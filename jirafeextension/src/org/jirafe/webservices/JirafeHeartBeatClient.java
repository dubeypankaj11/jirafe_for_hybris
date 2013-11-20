/**
 * 
 */
package org.jirafe.webservices;

import de.hybris.platform.core.Registry;
import de.hybris.platform.util.Config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.constants.JirafeextensionConstants;
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
		final String instanceId = String.format("<%s>:<%s>", Config.getString("cluster.id", "0"), Registry.getCurrentTenant()
				.getTenantID());
		final Map params = new HashMap<String, String>();
		params.put("version", JirafeextensionConstants.RELEASE_VERSION);
		params.put("client_id", jirafeOAuth2Session.getConnectionConfig().getClientId());
		params.put("is_enabled", Boolean.valueOf(Config.getBoolean(JirafeDefaultInterceptor.IS_ENABLED, true)));
		params.put("instance_id", instanceId);
		params.put("message", "Hello");
		final OAuth2ConnectionConfig connectionConfig = jirafeOAuth2Session.getConnectionConfig();
		for (final String siteId : connectionConfig.getSiteIds())
		{
			params.put("site_id", siteId);
			final String message = gson.toJson(params);
			try
			{
				jirafeOAuth2Session.putMessage(message, "heartbeat", connectionConfig.getSiteNameFromId(siteId));
			}
			catch (final Exception e)
			{
				LOG.error("Got error while sending ping {}", e.getMessage(), e);
			}
		}
	}

}
