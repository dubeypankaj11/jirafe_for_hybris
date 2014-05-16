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
	@Resource
	private OAuth2ConnectionConfig connectionConfig;

	protected final Gson gson = new Gson();

	public boolean ping()
	{
		boolean success = true;
		for (final String siteName : connectionConfig.getSiteNames())
		{
			success = ping(siteName) && success;
		}
		return success;
	}

	public boolean ping(final String siteName)
	{
		final String siteId = connectionConfig.getSiteId(siteName);

		final String instanceId = String.format("<%s>:<%s>", Config.getString("cluster.id", "0"), Registry.getCurrentTenant()
				.getTenantID());
		final Map params = new HashMap<String, String>();
		params.put("version", JirafeextensionConstants.RELEASE_VERSION);
		params.put("client_id", jirafeOAuth2Session.getConnectionConfig().getClientId());
		params.put("is_enabled", Boolean.valueOf(Config.getBoolean(JirafeDefaultInterceptor.IS_ENABLED, true)));
		params.put("instance_id", instanceId);
		params.put("message", "Hello");
		params.put("site_id", siteId);
		final String message = gson.toJson(params);
		try
		{
			final Map ret = jirafeOAuth2Session.putMessage(message, "heartbeat", connectionConfig.getSiteNameFromId(siteId));
			if (ret == null)
			{
				return false;
			}
			final Object success = ret.get("success");
			if (success == null)
			{
				return false;
			}
			return (Boolean) success;
		}
		catch (final Exception e)
		{
			LOG.error("Got error while sending ping {}", e.getMessage(), e);
			return false;
		}
	}

}
