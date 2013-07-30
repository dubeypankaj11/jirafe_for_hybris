/**
 * 
 */
package org.jirafe.webservices;

import de.hybris.platform.util.Config;


/**
 * @author alex
 * 
 */
public class OAuth2ConnectionConfig
{
	private String clientId;
	private String clientSecret;
	private String username;
	private String password;
	private String accessToken;

	private String siteId;

	private String authServerUrl;
	private String authServerAuthorize;
	private String authServerAccessToken;

	private String eventApiUrl;

	private int timeOut;

	public OAuth2ConnectionConfig(final String clientId, //
			final String clientSecret,//
			final String username,//
			final String password,//
			final String accessToken,//
			final String siteId,//
			final String authServerUrl,//
			final String authServerAuthorize,//
			final String authServerAccessToken,//
			final String eventApiUrl, //
			final int timeOut)
	{
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
		this.accessToken = accessToken;
		this.siteId = siteId;
		this.authServerUrl = authServerUrl;
		this.authServerAuthorize = authServerAuthorize;
		this.authServerAccessToken = authServerAccessToken;
		this.eventApiUrl = eventApiUrl;
		this.timeOut = timeOut;
	}

	public OAuth2ConnectionConfig()
	{
		this(Config.getString("jirafe.outboundConnectionConfig.client_id", ""),//
				Config.getString("jirafe.outboundConnectionConfig.client_secret", ""),//
				Config.getString("jirafe.outboundConnectionConfig.username", ""),//
				Config.getString("jirafe.outboundConnectionConfig.password", ""),//
				Config.getString("jirafe.outboundConnectionConfig.access_token", null),//
				Config.getString("jirafe.outboundConnectionConfig.site_id", ""),//
				Config.getString("jirafe.outboundConnectionConfig.auth_server_url", ""),//
				Config.getString("jirafe.outboundConnectionConfig.auth_server_authorize", ""),//
				Config.getString("jirafe.outboundConnectionConfig.auth_server_access_token", ""),//
				Config.getString("jirafe.outboundConnectionConfig.event_api_url", ""),//
				Config.getInt("jirafe.outboundConnectionConfig.time_out", 30000));
	}

	/**
	 * @return the clientId
	 */
	public String getClientId()
	{
		return clientId;
	}

	/**
	 * @param clientId
	 *           the clientId to set
	 */
	public void setClientId(final String clientId)
	{
		this.clientId = clientId;
	}

	/**
	 * @return the clientSecret
	 */
	public String getClientSecret()
	{
		return clientSecret;
	}

	/**
	 * @param clientSecret
	 *           the clientSecret to set
	 */
	public void setClientSecret(final String clientSecret)
	{
		this.clientSecret = clientSecret;
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username
	 *           the username to set
	 */
	public void setUsername(final String username)
	{
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @param password
	 *           the password to set
	 */
	public void setPassword(final String password)
	{
		this.password = password;
	}

	/**
	 * @return the access token
	 */
	public String getAccessToken()
	{
		return accessToken;
	}

	/**
	 * @param accessToken
	 *           the access token to set
	 */
	public void setAccessToken(final String accessToken)
	{
		this.accessToken = accessToken;
	}

	/**
	 * @return the siteId
	 */
	public String getSiteId()
	{
		return siteId;
	}

	/**
	 * @param siteId
	 *           the siteId to set
	 */
	public void setSiteId(final String siteId)
	{
		this.siteId = siteId;
	}

	/**
	 * @return the authServerUrl
	 */
	public String getAuthServerUrl()
	{
		return authServerUrl;
	}

	/**
	 * @param authServerUrl
	 *           the authServerUrl to set
	 */
	public void setAuthServerUrl(final String authServerUrl)
	{
		this.authServerUrl = authServerUrl;
	}

	/**
	 * @return the authServerAuthorize
	 */
	public String getAuthServerAuthorize()
	{
		return authServerAuthorize;
	}

	/**
	 * @param authServerAuthorize
	 *           the authServerAuthorize to set
	 */
	public void setAuthServerAuthorize(final String authServerAuthorize)
	{
		this.authServerAuthorize = authServerAuthorize;
	}

	/**
	 * @return the authServerAccessToken
	 */
	public String getAuthServerAccessToken()
	{
		return authServerAccessToken;
	}

	/**
	 * @param authServerAccessToken
	 *           the authServerAccessToken to set
	 */
	public void setAuthServerAccessToken(final String authServerAccessToken)
	{
		this.authServerAccessToken = authServerAccessToken;
	}

	/**
	 * @return the eventApiUrl
	 */
	public String getEventApiUrl()
	{
		return eventApiUrl;
	}

	/**
	 * @param eventApiUrl
	 *           the eventApiUrl to set
	 */
	public void setEventApiUrl(final String eventApiUrl)
	{
		this.eventApiUrl = eventApiUrl;
	}

	/**
	 * @return the timeOut
	 */
	public int getTimeOut()
	{
		return timeOut;
	}

	/**
	 * @param timeOut
	 *           the timeOut to set
	 */
	public void setTimeOut(final int timeOut)
	{
		this.timeOut = timeOut;
	}

}
