/**
 * 
 */
package org.jirafe.webservices;

import de.hybris.platform.cms2.model.site.CMSSiteModel;
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;
import de.hybris.platform.core.Registry;
import de.hybris.platform.util.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

	private String clientId;
	private String clientSecret;
	private String username;
	private String password;
	private String accessToken;

	private String authServerUrl;
	private String authServerAuthorize;
	private String authServerAccessToken;
	private String[] blacklistedSiteIds;

	private String siteId;
	private String[] siteNames;
	private Set<String> siteIds;
	private HashMap<String, String> siteIdMap;

	private String eventApiUrl;
	private int timeOut;

	final static String SITE_PREFIX = "jirafe.site.id.";

	public OAuth2ConnectionConfig()
	{
		loadConfig();
	}

	public void loadConfig()
	{
		clientId = Config.getString("jirafe.outboundConnectionConfig.client_id", "");
		clientSecret = Config.getString("jirafe.outboundConnectionConfig.client_secret", "");
		username = Config.getString("jirafe.outboundConnectionConfig.username", "");
		password = Config.getString("jirafe.outboundConnectionConfig.password", "");
		accessToken = Config.getString("jirafe.outboundConnectionConfig.access_token", null);

		authServerUrl = Config.getString("jirafe.outboundConnectionConfig.auth_server_url", "");
		authServerAuthorize = Config.getString("jirafe.outboundConnectionConfig.auth_server_authorize", "");
		authServerAccessToken = Config.getString("jirafe.outboundConnectionConfig.auth_server_access_token", "");
		blacklistedSiteIds = StringUtils.split(Config.getString("jirafe.site.ids.blacklist", ""), ",");

		siteId = null;
		siteNames = null;
		siteIds = null;
		siteIdMap = null;

		eventApiUrl = Config.getString("jirafe.outboundConnectionConfig.event_api_url", "");
		timeOut = Config.getInt("jirafe.outboundConnectionConfig.time_out", 30000);

		final String s = Config.getString("jirafe.site.id", null);
		if (!StringUtils.isEmpty(s) && !ArrayUtils.contains(blacklistedSiteIds, s))
		{
			siteId = s;
		}
	}

	public void initSiteIdMap()
	{
		final HashMap<String, String> siteIdMap = new HashMap<String, String>();
		final CMSSiteService cmsSiteService = (CMSSiteService) Registry.getApplicationContext().getBean("cmsSiteService");
		for (final CMSSiteModel cmsSiteModel : cmsSiteService.getSites())
		{
			final String siteName = cmsSiteModel.getUid();
			if (!cmsSiteModel.getActive().booleanValue())
			{
				log.info("Skipping  inactive site {}", siteName);
				continue;
			}
			String siteId = Config.getParameter(SITE_PREFIX + siteName);
			if (siteId == null)
			{
				siteId = this.siteId;
				if (siteId == null)
				{
					log.warn("No site id supplied for site {}, site will not be monitored", siteName);
					continue;
				}
			}
			if (ArrayUtils.contains(blacklistedSiteIds, siteId))
			{
				log.error("Skipping blacklisted site {}={}", siteName, siteId);
				continue;
			}
			log.info("Initializing site map: {}={}", siteName, siteId);
			siteIdMap.put(siteName, siteId);
		}
		this.siteIds = new HashSet(siteIdMap.values());
		this.siteNames = siteIdMap.keySet().toArray(new String[0]);
		this.siteIdMap = siteIdMap;
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
	public Set<String> getSiteIds()
	{
		if (siteIds == null)
		{
			initSiteIdMap();
		}
		return siteIds;
	}

	/**
	 * @return the sites
	 */
	public String[] getSiteNames()
	{
		if (siteIds == null)
		{
			initSiteIdMap();
		}
		return siteNames;
	}

	/**
	 * @return the siteId given either a siteId or siteName
	 */
	public String getSiteId(final String site)
	{
		if (!StringUtils.isEmpty(site))
		{
			if (getSiteIds().contains(site))
			{
				return site;
			}
			return getSiteIdMap().get(site);
		}
		return siteId;
	}

	public String getSiteNameFromId(final String siteId)
	{
		final HashMap<String, String> siteIdMap = getSiteIdMap();
		for (final String siteName : siteIdMap.keySet())
		{
			if (siteIdMap.get(siteName).equals(siteId))
			{
				return siteName;
			}
		}
		return null;
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
