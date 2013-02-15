/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.annotations.header.AccessControlAllowOrigin;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.persistence.service.ContentPersistenceService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API for Content
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@RequestScoped
@Path("/content/{type}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProviderAllowed
public class ContentRestService extends RestServiceBase {

	@Inject
	protected ProviderService providerService;

	@Inject
	protected ContentPersistenceService contentPersistenceService;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
    @AccessControlAllowOrigin
	public Object getAllContent(@PathParam("type") String type, @QueryParam("from") Integer from,
			@QueryParam("size") Integer size, @QueryParam("sort") String sort) {
		if (type == null || type.isEmpty()) {
			return createRequiredFieldResponse("type");
		}
		try {
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
				return createBadFieldDataResponse("type");
			}

			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			SearchRequestBuilder srb = new SearchRequestBuilder(getSearchClientService().getClient());
			srb.setIndices(indexName);
			srb.setTypes(indexType);

			srb.setQuery(QueryBuilders.matchAllQuery());

			if (from != null) {
				srb.setFrom(from);
			}
			if (size != null) {
				srb.setSize(size);
			}
			if (sort != null) {
				if (sort.equalsIgnoreCase(SortOrder.ASC.name())) {
					srb.addSort(DcpContentObjectFields.DCP_UPDATED, SortOrder.ASC);
				} else if (sort.equalsIgnoreCase(SortOrder.DESC.name())) {
					srb.addSort(DcpContentObjectFields.DCP_UPDATED, SortOrder.DESC);
				}
			}

			final SearchResponse response = srb.execute().actionGet();

			return new ESDataOnlyResponse(response, DcpContentObjectFields.DCP_CONTENT_ID);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@GET
	@Path("/{contentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
    @AccessControlAllowOrigin
	public Object getContent(@PathParam("type") String type, @PathParam("contentId") String contentId) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			return createRequiredFieldResponse("contentId");
		}
		if (type == null || type.isEmpty()) {
			return createRequiredFieldResponse("type");
		}
		try {
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
				return createBadFieldDataResponse("type");
			}

			String dcpContentId = providerService.generateDcpId(type, contentId);

			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			GetResponse getResponse = getSearchClientService().getClient().prepareGet(indexName, indexType, dcpContentId)
					.execute().actionGet();

			if (!getResponse.exists()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			return createResponse(getResponse);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@POST
	@Path("/{contentId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@ProviderAllowed
	public Object pushContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			Map<String, Object> content) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			return createRequiredFieldResponse("contentId");
		}
		if (type == null || type.isEmpty()) {
			return createRequiredFieldResponse("type");
		}
		if (content == null || content.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("Some content for pushing must be defined").build();
		}
		try {
			Map<String, Object> provider = providerService.findProvider(getProvider());
			Map<String, Object> typeDef = ProviderService.extractContentType(provider, type);
			if (typeDef == null) {
				return createBadFieldDataResponse("type");
			}

			String dcpContentId = providerService.generateDcpId(type, contentId);

			// check search subsystem configuration
			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			// fill some normalized fields - should be last step to avoid changing them via preprocessors
			content.put(DcpContentObjectFields.DCP_CONTENT_PROVIDER, getProvider());
			content.put(DcpContentObjectFields.DCP_CONTENT_ID, contentId);
			content.put(DcpContentObjectFields.DCP_CONTENT_TYPE, type);
			content.put(DcpContentObjectFields.DCP_ID, dcpContentId);
			content.put(DcpContentObjectFields.DCP_TYPE, ProviderService.extractDcpType(typeDef, type));
			content.put(DcpContentObjectFields.DCP_UPDATED, new Date());
			// Copy distinct data from content to normalized fields
			content.put(DcpContentObjectFields.DCP_TAGS, content.get("tags"));

			// Run preprocessors to manipulate other fields
			providerService.runPreprocessors(type, ProviderService.extractPreprocessors(typeDef, type), content);

			if (ProviderService.extractPersist(typeDef)) {
				contentPersistenceService.store(dcpContentId, type, content);
			}

			// TODO EXTERNAL_TAGS - add external tags for this document into dcp_tags field

			// Push to search subsystem
			IndexResponse ir = getSearchClientService().getClient().prepareIndex(indexName, indexType, dcpContentId)
					.setSource(content).execute().actionGet();
			Map<String, String> retJson = new LinkedHashMap<String, String>();
			if (ir.version() > 1) {
				retJson.put("status", "update");
				retJson.put("message", "Content was updated successfully.");
			} else {
				retJson.put("status", "insert");
				retJson.put("message", "Content was inserted successfully.");
			}
			return Response.ok(retJson).build();
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@DELETE
	@Path("/{contentId}")
	@ProviderAllowed
	public Object deleteContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			@QueryParam("ignore_missing") String ignoreMissing) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			return createRequiredFieldResponse("contentId");
		}
		if (type == null || type.isEmpty()) {
			return createRequiredFieldResponse("type");
		}
		try {
			Map<String, Object> provider = providerService.findProvider(getProvider());
			Map<String, Object> typeDef = ProviderService.extractContentType(provider, type);
			if (typeDef == null) {
				return createBadFieldDataResponse("type");
			}

			String dcpContentId = providerService.generateDcpId(type, contentId);

			if (ProviderService.extractPersist(typeDef)) {
				contentPersistenceService.delete(dcpContentId, type);
			}

			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			DeleteResponse dr = getSearchClientService().getClient().prepareDelete(indexName, indexType, dcpContentId)
					.execute().actionGet();

			if (dr.isNotFound() && !Boolean.parseBoolean(ignoreMissing)) {
				return Response.status(Status.NOT_FOUND).entity("Content not found to be deleted.").build();
			} else {
				return Response.ok("Content deleted successfully.").build();
			}
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}
}
