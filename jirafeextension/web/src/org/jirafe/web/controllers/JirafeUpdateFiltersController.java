/**
 * 
 */
package org.jirafe.web.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.services.JirafeUpdateMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;


/**
 * @author alex
 * 
 */
@Controller
public class JirafeUpdateFiltersController
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeUpdateFiltersController.class);
	private final static String SUCCESS = "{\"success\":true}";
	private final static String FAILURE = "{\"success\":false, \"error\":\"%s\"}";
	@Resource
	private JirafeUpdateMappingService jirafeUpdateMappingService;

	@RequestMapping(method = RequestMethod.PUT, produces = "application/json", value = "/updatefilter/{type}")
	@ResponseBody
	public String update(@PathVariable final String type, @RequestBody final String mapping)
	{
		LOG.debug("Got new filter for type <{}>: \n{}", type, mapping);
		final Gson gson = new Gson();

		try
		{
			final Map map = gson.fromJson(mapping, Map.class);
			// note: casting to String instead of toString() as we may want to set filter to null - ie. remove it
			jirafeUpdateMappingService.updateFilter(type, (String) map.get("filter"));
		}
		catch (final Exception e)
		{
			LOG.error("Failed to update filter for type <{}> due to {}", type, e);
			return String.format(FAILURE, e);
		}
		LOG.debug("Filter for type <{}> updated successfully", type);
		return SUCCESS;
	}

	@RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/updatefilter/{type}")
	@ResponseBody
	public String get(@PathVariable final String type)
	{
		LOG.debug("Got get request for type <{}>", type);
		final Gson gson = new Gson();
		final HashMap<String, Object> ret = new HashMap<String, Object>();
		ret.put("filter", jirafeUpdateMappingService.getFilter(type));
		return gson.toJson(ret);
	}
}
