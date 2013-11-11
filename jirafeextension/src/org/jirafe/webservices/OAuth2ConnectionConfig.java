/**
 * 
 */
package org.jirafe.webservices;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.site.CMSSiteModel;
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;
import de.hybris.platform.core.Registry;
import de.hybris.platform.util.Config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @author alex
 * 
 */
@Component("connectionConfig")
public class OAuth2ConnectionConfig
{
	private static final Logger log = LoggerFactory.getLogger(OAuth2ConnectionConfig.class);

	private final String clientId;
	private final String clientSecret;
	private final String username;
	private final String password;
	private final String accessToken;

	private final String siteId;
	private String[] siteIds;
	private HashMap<String, String> siteIdMap;

	private final String authServerUrl;
	private final String authServerAuthorize;
	private final String authServerAccessToken;

	private final String eventApiUrl;

	private final int timeOut;

	final static String SITE_PREFIX = "jirafe.site.id.";

	public OAuth2ConnectionConfig()
	{
		clientId = Config.getString("jirafe.outboundConnectionConfig.client_id", "");
		clientSecret = Config.getString("jirafe.outboundConnectionConfig.client_secret", "");
		username = Config.getString("jirafe.outboundConnectionConfig.username", "");
		password = Config.getString("jirafe.outboundConnectionConfig.password", "");
		accessToken = Config.getString("jirafe.outboundConnectionConfig.access_token", null);
		authServerUrl = Config.getString("jirafe.outboundConnectionConfig.auth_server_url", "");
		authServerAuthorize = Config.getString("jirafe.outboundConnectionConfig.auth_server_authorize", "");
		authServerAccessToken = Config.getString("jirafe.outboundConnectionConfig.auth_server_access_token", "");
		eventApiUrl = Config.getString("jirafe.outboundConnectionConfig.event_api_url", "");
		timeOut = Config.getInt("jirafe.outboundConnectionConfig.time_out", 30000);

		siteId = Config.getString("jirafe.site.id", "");
	}

	public void initSiteIdMap()
	{
		final String[] blacklistedSiteIds = StringUtils.split(Config.getString("jirafe.site.ids.blacklist", ""), ",");
		siteIdMap = new HashMap<String, String>();
		final Map<String, String> hostToSite = Config.getParametersByPattern(SITE_PREFIX);
		final CMSSiteService cmsSiteService = (CMSSiteService) Registry.getApplicationContext().getBean("cmsSiteService");
		// Find site ids we've assigned them
		for (final String key : hostToSite.keySet())
		{
			final String siteId = hostToSite.get(key);
			if (ArrayUtils.contains(blacklistedSiteIds, siteId))
			{
				log.debug("Skipping blacklisted site {}={}", key, siteId);
				continue;
			}
			final String host = key.substring(SITE_PREFIX.length());
			final String spec = "http://" + host;
			try
			{
				final CMSSiteModel cmsSiteModel = cmsSiteService.getSiteForURL(new URL(spec));
				final String siteName = cmsSiteModel.getUid();
				log.debug("Assigning site id {} to {}", siteId, siteName);
				siteIdMap.put(siteName, siteId);
			}
			catch (final CMSItemNotFoundException e)
			{
				log.error(e.getMessage());
			}
			catch (final MalformedURLException e)
			{
				log.error(e.getMessage());
			}
		}
		// Warn about any missing site ids and default them to the default site id
		for (final CMSSiteModel cmsSiteModel : cmsSiteService.getSites())
		{
			log.debug("Checking site {}", cmsSiteModel.getUid());
			if (!cmsSiteModel.getActive().booleanValue())
			{
				continue;
			}
			final String siteName = cmsSiteModel.getUid();
			if (!siteIdMap.containsKey(siteName))
			{
				log.warn("Missing site ID for site {}, using default site ID", siteName);
				siteIdMap.put(siteName, siteId);
			}
		}

		siteIds = siteIdMap.keySet().toArray(new String[0]);
	}

	/**
	 * @return the clientId
	 */
	public String getClientId()
	{
		return clientId;
	}

	/**
	 * @return the clientSecret
	 */
	public String getClientSecret()
	{
		return clientSecret;
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @return the access token
	 */
	public String getAccessToken()
	{
		return accessToken;
	}

	/**
	 * @return the default siteId
	 */
	public String getSiteId()
	{
		return siteId;
	}

	/**
	 * @return the siteIds
	 */
	public String[] getSiteIds()
	{
		if (siteIds == null)
		{
			initSiteIdMap();
		}
		return siteIds;
	}

	/**
	 * @return the siteId
	 */
	public String getSiteId(final String site)
	{
		if (!StringUtils.isEmpty(site))
		{
			return getSiteIdMap().get(site);
		}
		return siteId;
	}

	/**
	 * @return the siteIds
	 */
	public HashMap<String, String> getSiteIdMap()
	{
		if (siteIdMap == null)
		{
			initSiteIdMap();
		}
		return siteIdMap;
	}

	/**
	 * @return the authServerUrl
	 */
	public String getAuthServerUrl()
	{
		return authServerUrl;
	}

	/**
	 * @return the authServerAuthorize
	 */
	public String getAuthServerAuthorize()
	{
		return authServerAuthorize;
	}

	/**
	 * @return the authServerAccessToken
	 */
	public String getAuthServerAccessToken()
	{
		return authServerAccessToken;
	}

	/**
	 * @return the eventApiUrl
	 */
	public String getEventApiUrl()
	{
		return eventApiUrl;
	}

	/**
	 * @return the timeOut
	 */
	public int getTimeOut()
	{
		return timeOut;
	}

}
