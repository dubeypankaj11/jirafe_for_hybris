/**
 * 
 */
package org.jirafe.web.controllers;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;


/**
 * @author alex
 * 
 */
@Controller
public class JirafeUpdateMappingsController
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeUpdateMappingsController.class);
	private final static String SUCCESS = "{\"success\":true}";
	private final static String FAILURE = "{\"success\":false, \"error\":\"%s\"}";
	@Resource
	private JirafeUpdateMappingService jirafeUpdateMappingService;

	@RequestMapping(method = RequestMethod.PUT, produces = "application/json", value = "/updatemapping/{type}")
	@ResponseBody
	public String update(@PathVariable final String type, @RequestBody final String mapping, @RequestParam String endPoint)
	{
		LOG.debug("Got new mappings for type <{}>: \n{}", type, mapping);
		final Gson gson = new Gson();

		try
		{
			gson.fromJson(mapping, Map.class);
			if (endPoint == null)
			{
				endPoint = type.toLowerCase();
			}
			jirafeUpdateMappingService.updateDefinition(type, mapping, endPoint);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to update type <{}> due to {}", type, e);
			return String.format(FAILURE, e);
		}
		LOG.debug("Type <{}> updated successfully", type);
		return SUCCESS;
	}

	@RequestMapping(method = RequestMethod.DELETE, produces = "application/json", value = "/updatemapping/{type}")
	@ResponseBody
	public String update(@PathVariable final String type)
	{
		LOG.debug("Delete mappings for type <{}>", type);
		try
		{
			jirafeUpdateMappingService.deleteDefinition(type);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to update type <{}> due to {}", type, e);
			return String.format(FAILURE, e);
		}
		LOG.debug("Type <{}> updated successfully", type);
		return SUCCESS;
	}

	@RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/updatemapping/{type}")
	@ResponseBody
	public String get(@PathVariable final String type)
	{
		LOG.debug("Got get request for type <{}>", type);
		return jirafeUpdateMappingService.getDefinition(type);
	}
}
