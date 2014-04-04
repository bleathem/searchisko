/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import java.util.*;

import org.searchisko.api.util.QuerySettingsParser;

/**
 * Search Query settings sent from client to search method.
 *
 * @author Lukas Vlcek
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class QuerySettings {

	public static class Filters {

		Map<String, List<String>> acknowledgedFilterCandidates = new HashMap<>();

		/**
		 * Return all values for all url parameters which are relevant to specific field name.
		 * For example if there are two different filters defined for the same document field
		 * then this method returns all values for both url params.
		 *
		 * @param fieldName
		 * @return list of param values
		 */
//		public List<String> valuesForField(String fieldName) {
			// TODO: !
//			return new ArrayList<>();
//		}

		// should be used only in tests
		public void acknowledgeUrlFilterCandidate(String urlParamName, String... values) {
			acknowledgeUrlFilterCandidate(urlParamName, Arrays.asList(values));
		}

		/**
		 * Remember urlParamName and all provided values.
		 *
		 * @param urlParamName name of URL parameter
		 * @param values
		 */
		public void acknowledgeUrlFilterCandidate(String urlParamName, List<String> values) {
			acknowledgedFilterCandidates.put(urlParamName, values);
		}

		/**
		 * Forgets previously remembered urlParamName and its values.
		 *
		 * @param urlParamName
		 */
		public void forgetUrlFilterCandidate(String urlParamName) {
			if (acknowledgedFilterCandidates.containsKey(urlParamName)) {
				acknowledgedFilterCandidates.remove(urlParamName);
			}
		}

		public Set<String> getFilterCandidatesKeys() {
			return acknowledgedFilterCandidates.keySet();
		}

		/**
		 * Returns all URL parameter values as provided by the client.
		 *
		 * @param urlParamName
		 * @return
		 */
		public List<String> getFilterCandidateValues(String urlParamName) {
			return acknowledgedFilterCandidates.get(urlParamName);
		}

		/**
		 * Returns a distinct set of URL values for all provided URL parameters.
		 *
		 * @param urlParamName
		 * @return
		 */
		public Set<String> getFilterCandidateValues(Set<String> urlParamName) {
			Set<String> distinctValues = new HashSet<>();
			for (String paramName: urlParamName) {
				for (String value : acknowledgedFilterCandidates.get(paramName)) {
					distinctValues.add(value);
				}
			}
			return distinctValues;
		}

		public String getFirstValueForFilterCandidate(String urlParamName) {
			if (acknowledgedFilterCandidates.containsKey(urlParamName) &&
					acknowledgedFilterCandidates.get(urlParamName).size() > 0) {
				return acknowledgedFilterCandidates.get(urlParamName).get(0);
			} else {
				return null;
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Filter candidates [");
			boolean first = true;
			for (String key : acknowledgedFilterCandidates.keySet()) {
				if (!first) sb.append(", ");
				sb.append(key).append("=").append(acknowledgedFilterCandidates.get(key));
				first = false;
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private Filters filters;

	/**
	 * Fulltext query
	 */
	private String query;

	public static final String QUERY_KEY = "query";

	private boolean queryHighlight = false;

	public static final String QUERY_HIGHLIGHT_KEY = "query_highlight";

	/**
	 * List of fields in response
	 */
	private List<String> fields;

	public static final String FIELDS_KEY = "field";

	private Set<String> facets;

	public static final String FACETS_KEY = "facet";

	/**
	 * Results Paging - start index
	 */
	private Integer from = null;

	public static final String FROM_KEY = "from";

	/**
	 * Results Paging - count of returned results
	 */
	private Integer size = null;

	public static final String SIZE_KEY = "size";

	/**
	 * Sorting of results
	 */
	private SortByValue sortBy;

	public static final String SORT_BY_KEY = "sortBy";

	public void setFilters(Filters filters) {
		this.filters = filters;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setSortBy(SortByValue sortBy) {
		this.sortBy = sortBy;
	}

	public Filters getFilters() {
		return filters;
	}

	/**
	 * Get filters, init object if not initialized yet
	 *
	 * @return filters, never null
	 */
	public Filters getFiltersInit() {
		if (filters == null)
			filters = new Filters();
		return filters;
	}

	/**
	 * Get search query
	 *
	 * @return normalized query or null
	 * @see QuerySettingsParser#normalizeQueryString(String)
	 */
	public String getQuery() {
		return query;
	}

	public SortByValue getSortBy() {
		return sortBy;
	}

	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	public void addField(String value) {
		if (value == null)
			return;
		if (fields == null)
			fields = new ArrayList<String>();
		fields.add(value);
	}

	public boolean isQueryHighlight() {
		return queryHighlight;
	}

	public void setQueryHighlight(boolean queryHighlight) {
		this.queryHighlight = queryHighlight;
	}

	public Set<String> getFacets() {
		return facets;
	}

	public void addFacet(String value) {
		if (value == null)
			return;
		if (facets == null)
			facets = new LinkedHashSet<>();
		facets.add(value);
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getFrom() {
		return from;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public void clearFacets() {
		if (facets != null)
			facets.clear();
	}

	public void clearFields() {
		if (fields != null)
			fields.clear();
	}

	@Override
	public String toString() {
		return "QuerySettings [filters=" + filters + ", query=" + query + ", queryHighlight=" + queryHighlight + ", field="
				+ fields + ", facet=" + facets + ", sortBy=" + sortBy + "]";
	}

}
