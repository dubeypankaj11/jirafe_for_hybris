/**
 * 
 */
package org.jirafe.webservices;

import de.hybris.platform.core.Registry;
import de.hybris.platform.util.Config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.interceptor.JirafeDefaultInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;


/**
 * @author alex
 * 
 */
@Component("jirafeHeartBeatClient")
public class JirafeHeartBeatClient
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeHeartBeatClient.class);

	@Resource
	private JirafeOAuth2Session jirafeOAuth2Session;
	protected final Gson gson = new Gson();

	public void ping()
	{
		try
		{
			jirafeOAuth2Session.putMessage(constractMessage(), "heartbeat");
		}
		catch (final Exception e)
		{
			LOG.error("Got error while sending ping {}", e.getMessage(), e);
		}
	}

	protected String constractMessage()
	{
		final String instanceId = String.format("<%s>:<%s>", Config.getString("cluster.id", "0"), Registry.getCurrentTenant()
				.getTenantID());
		final Map params = new HashMap<String, String>();
		params.put("client_id", jirafeOAuth2Session.getConnectionConfig().getClientId());
		params.put("site_id", jirafeOAuth2Session.getConnectionConfig().getSiteId());
		params.put("is_enabled", Boolean.valueOf(Config.getBoolean(JirafeDefaultInterceptor.IS_ENABLED, true)));
		params.put("instance_id", instanceId);
		params.put("message", "Hello");
		return gson.toJson(params);
	}
}
