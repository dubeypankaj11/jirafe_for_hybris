/**
 * 
 */
package org.jirafe.web.controllers.ui;

import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.services.JirafeHistoricalSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;


/**
 * @author dbrand
 * 
 */
@Controller
public class JirafeHistoricalSyncController
{
	private final static Logger log = LoggerFactory.getLogger(JirafeHistoricalSyncController.class);

	@Resource
	JirafeHistoricalSyncService jirafeHistoricalSyncService;

	private final static Gson gson = new Gson();

	@RequestMapping(method = RequestMethod.GET, produces = "application/json", value = "/historicalsync/{siteName}/{command}")
	@ResponseBody
	public String request(@PathVariable final String siteName, @PathVariable final String command)
	{
		final Map map = jirafeHistoricalSyncService.request(siteName, command, null);
		return gson.toJson(map);
	}
}
