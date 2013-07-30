/**
 * 
 */
package org.jirafe.web.controllers;

import javax.annotation.Resource;

import org.jirafe.services.JirafeDataEndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * @author dbrand
 * 
 */
@Controller
public class JirafeDataEndpointsController
{

	private final static Logger LOG = LoggerFactory.getLogger(JirafeDataEndpointsController.class);
	private final static String FAILURE = "{\"success\":false, \"error\": \"%s\"}";

	@Resource
	private JirafeDataEndpointService jirafeDataEndpointService;

	@RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/dataendpoint/{type}")
	@ResponseBody
	public String get(@PathVariable final String type,
			@RequestParam(value = "start_time", required = false) final String startTime,
			@RequestParam(value = "end_time", required = false) final String endTime,
			@RequestParam(value = "page_limit", required = false) final String pageLimit,
			@RequestParam(value = "page_token", required = false) final String pageToken)
	{
		LOG.info(String.format(
				"Got new data endpoint request for <%s>: \nstart_time=%s, end_time=%s, page_limit=%s, page_token=%s", type,
				startTime, endTime, pageLimit, pageToken));
		try
		{
			return jirafeDataEndpointService.getData(type, startTime, endTime, pageLimit, pageToken);
		}
		catch (final Exception e)
		{
			LOG.error("failed to retrieve data <{}> due to {}", type, e);
			return String.format(FAILURE, e);
		}
	}
}
