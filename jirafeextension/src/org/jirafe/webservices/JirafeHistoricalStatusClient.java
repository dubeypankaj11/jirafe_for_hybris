/**
 *
 */
package org.jirafe.webservices;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.webservices.JirafeOAuth2Session.InvalidSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;


/**
 * @author dbrand
 * 
 */
@Component
public class JirafeHistoricalStatusClient
{
	private final static Logger log = LoggerFactory.getLogger(JirafeHistoricalStatusClient.class);

	@Resource
	private JirafeOAuth2Session jirafeOAuth2Session;
	@Resource
	private OAuth2ConnectionConfig connectionConfig;

	protected final Gson gson = new Gson();

	// 'disable', 'ready', 'in-process', 'complete', 'connection failed'
	public String getStatus(final String site)
	{
		Map<String, Object> message;
		try
		{
			message = jirafeOAuth2Session.getMessage("accounts/historical/status", connectionConfig.getSiteId(site));
		}
		catch (final InvalidSite e)
		{
			message = null;
		}
		if (message == null)
		{
			return "connection failed";
		}
		return (String) message.get("historical_status");
	}

	public void putStatus(final String site, final String status) throws Exception
	{
		final String siteId = connectionConfig.getSiteId(site);
		final Map<String, Object> message = new HashMap<String, Object>();
		message.put("site_id", siteId);
		message.put("historical_status", status);
		final Map ret = jirafeOAuth2Session.putMessage(message, "accounts/historical/status", siteId);
		if (ret == null)
		{
			throw new Exception("Failed to update historical status.");
		}
		if (!(Boolean) ret.get("success"))
		{
			throw new Exception((String) ret.get("message"));
		}
	}

}
