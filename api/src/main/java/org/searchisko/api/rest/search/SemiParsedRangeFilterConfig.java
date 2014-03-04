/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

/**
 * @author Lukas Vlcek
 */
public class SemiParsedRangeFilterConfig extends SemiParsedFilterConfigSupportSuppressed {

	public boolean definesGte() {
		return false;
	}

	public boolean definesLte() {
		return false;
	}

	public String getEnum() {
		return null;
	}
}
