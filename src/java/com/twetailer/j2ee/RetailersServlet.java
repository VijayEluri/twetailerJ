package com.twetailer.j2ee;

import java.util.List;
import java.util.logging.Logger;

import domderrien.jsontools.JsonArray;
import domderrien.jsontools.JsonObject;

import com.google.appengine.api.users.User;
import com.twetailer.DataSourceException;
import com.twetailer.dto.Retailer;

@SuppressWarnings("serial")
public class RetailersServlet extends BaseRestlet {
	private static final Logger log = Logger.getLogger(RetailersServlet.class.getName());

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected String createResource(JsonObject parameters, User loggedUser) throws DataSourceException {
		return null;
	}

	@Override
	protected void deleteResource(String resourceId, User loggedUser) throws DataSourceException {
	}

	@Override
	protected JsonObject getResource(JsonObject parameters, String resourceId, User loggedUser) throws DataSourceException {
		return null;
	}

	@Override
	protected JsonArray selectResources(JsonObject parameters) throws DataSourceException {
		return null;
	}

	@Override
	protected void updateResource(JsonObject parameters, String resourceId, User loggedUser) throws DataSourceException {
	}
	
	public List<Retailer> getRetailers(String key, Object value) {
	    return null;
	}
}