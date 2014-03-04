/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.rest.search.ConfigParseUtil;
import org.searchisko.api.rest.search.SemiParsedFilterConfig;
import org.searchisko.api.rest.search.SemiParsedFilterConfigSupportSuppressed;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * This service is configured by new {@link org.searchisko.api.model.QuerySettings.Filters} using CDI event.
 *
 * @author Lukas Vlcek
 */
@Named
@RequestScoped
public class ParsedFilterConfigService {

	@Inject
	protected ConfigService configService;

	private Map<String, SemiParsedFilterConfig> semiParsedFilters = null;

	/**
	 * @param filters to use to prepare relevant parsed filter configurations into request scope
	 */
	protected void prepareParsedFilters(@Observes QuerySettings.Filters filters) {
		Map<String, SemiParsedFilterConfig> semiParsedFilters = new LinkedHashMap<>();
		Map<String, Object> filtersConfig = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS);

		// collect parsed filter configurations that are relevant to filters required by client
		for (String filterCandidateKey : filters.getFilterCandidatesKeys()) {
			if (filtersConfig.containsKey(filterCandidateKey)) {
				// get filter types for filterCandidateKey and check all types are the same
				Object filterConfig = filtersConfig.get(filterCandidateKey);
				SemiParsedFilterConfig parsedFilterConfig = ConfigParseUtil.parseFilterType(filterConfig, filterCandidateKey); // TODO get from cache
				semiParsedFilters.put(filterCandidateKey, parsedFilterConfig);
			}
		}

		// iterate over all collected filters and drop those that are suppressed
		for (String filterName : semiParsedFilters.keySet()) {
			// parsed filters could have been removed in the meantime so we check if it is still present
			if (semiParsedFilters.containsKey(filterName)) {
				SemiParsedFilterConfig parsedFilterConfig = semiParsedFilters.get(filterName);
				if (parsedFilterConfig instanceof SemiParsedFilterConfigSupportSuppressed) {
					List<String> suppressed = ((SemiParsedFilterConfigSupportSuppressed)parsedFilterConfig).getSuppressed();
					if (suppressed != null) {
						for (String suppress : suppressed) {
							if (semiParsedFilters.containsKey(suppress)) {
								semiParsedFilters.remove(suppress);
							}
						}
					}
				}
			}
		}
		this.semiParsedFilters = semiParsedFilters;
	}

	/**
	 * Provide all valid filter configurations relevant to current request.
	 * Keys of returned map represent filter names of filter configurations for current request.
	 *
	 * @return map of {@link org.searchisko.api.rest.search.SemiParsedFilterConfig}s for current request
	 * @throws java.lang.RuntimeException if new request filters haven't been observed yet
	 */
	public Map<String, SemiParsedFilterConfig> getFilterConfigsForRequest() {
		if (this.semiParsedFilters == null) {
			// this should not happen if request filters have been dispatched correctly before
			throw new RuntimeException("Parsed filters not initialized. New request filters haven't been observed" +
					" yet, this can be internal implementation error.");
		}
		return this.semiParsedFilters;
	}

	/**
	 *
	 * @param fieldName
	 * @return
	 */
	public Set<String> getFilterNamesForDocumentField(String fieldName) {
		Set<String> filterNames = new HashSet<>();
		for (SemiParsedFilterConfig c : this.semiParsedFilters.values()) {
			if (c.getFieldName().equals(fieldName)) {
				filterNames.add(c.getFilterName());
			}
		}
		return filterNames;
	}
}
