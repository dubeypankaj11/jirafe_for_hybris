/**
 *
 */
package org.jirafe.webservices;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;


/**
 * @author alex
 *
 */
@Component("jirafeOAuth2Session")
public class JirafeOAuth2Session
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeOAuth2Session.class);

	private HttpClient client;
	private String accessToken;
	private final Gson gson = new Gson();

	@Resource
	private OAuth2ConnectionConfig connectionConfig;

	@PostConstruct
	public void init()
	{
		accessToken = connectionConfig.getAccessToken();
		client = new HttpClient(new MultiThreadedHttpConnectionManager());
		if (StringUtils.isNotBlank(connectionConfig.getProxyHost()))
		{
			final ProxyHost proxy = new ProxyHost(connectionConfig.getProxyHost(), connectionConfig.getProxyPort());
			client.getHostConfiguration().setProxyHost(proxy);
		}
		client.getHttpConnectionManager().getParams().setConnectionTimeout(connectionConfig.getTimeOut());
	}

	public Map getMessage(final String type, final String site) throws InvalidSite
	{
		final String url = getUrl(type, site, false);
		if (url == null)
		{
			final String msg = String.format("No endpoint for site %s", site);
			LOG.error(msg);
			throw new InvalidSite(msg);
		}

		LOG.debug("GET <{}> message from {}", type, url);
		final GetMethod method = new GetMethod(url);

		return sendMessage(method);
	}

	public Map putMessage(final Map message, final String type, final String site) throws UnsupportedEncodingException,
			InvalidSite
	{
		return putMessage(gson.toJson(message), type, site);
	}

	public class InvalidSite extends Exception
	{
		public InvalidSite(final String message)
		{
			super(message);
		}
	}

	public Map putMessage(final String message, final String type, final String site) throws UnsupportedEncodingException,
			InvalidSite
	{
		final String url = getUrl(type, site, true);
		if (url == null)
		{
			final String msg = String.format("No endpoint for site %s, discarding message", site);
			LOG.error(msg + ": " + message);
			throw new InvalidSite(msg);
		}

		LOG.debug("PUT <{}> message to {}", type, url);
		final PutMethod method = new PutMethod(url);

		try
		{
			method.setRequestEntity(new StringRequestEntity(message, "application/json", null));
		}
		catch (final UnsupportedEncodingException e)
		{
			LOG.error("Error encoding json message: {} ({})", e.getMessage(), url);
			throw e;
		}
		return sendMessage(method);
	}

	private String getMethodUrl(final HttpMethodBase method)
	{
		String url;

		try
		{
			URI uri;
			uri = method.getURI();
			url = uri.getURI();
		}
		catch (final URIException e)
		{
			LOG.debug("", e);
			url = null;
		}

		return url;
	}

	protected Map sendMessage(final HttpMethodBase method)
	{
		final String url = getMethodUrl(method);

		try
		{
			method.setRequestHeader(getHeader());
			final int statusCode = client.executeMethod(method);
			LOG.debug("Got response code: {}", Integer.valueOf(statusCode));
			if (statusCode == 403)
			{
				invalidateToken();
				return null;
			}

			if (statusCode != HttpStatus.SC_OK)
			{
				LOG.error("Failed to send message: {} ({})", method.getStatusLine(), url);
			}

			return getMapFromJson(method);
		}
		catch (final HttpException e)
		{
			LOG.error("Fatal protocol violation: {} ({})", e.getMessage(), url);
		}
		catch (final IOException e)
		{
			LOG.error("Fatal transport error: {} ({})", e.getMessage(), url);
		}
		finally
		{
			method.releaseConnection();
		}
		return null;
	}

	protected Header getHeader()
	{
		updateToken();
		return new Header("Authorization", String.format("Bearer %s", accessToken));
	}

	protected String getUrl(final String type, final String siteName, final boolean update)
	{
		final String siteId = connectionConfig.getSiteId(siteName);
		if (StringUtils.isEmpty(siteId))
		{
			return null;
		}
		String url, append;
		if (type.startsWith("accounts/"))
		{
			url = connectionConfig.getAuthServerUrl();
			append = type + "/" + siteId + "/";
			if (update)
			{
				append += "update/";
			}
		}
		else
		{
			url = connectionConfig.getEventApiUrl();
			append = siteId + "/" + type;
		}
		if (!url.endsWith("/"))
		{
			url += "/";
		}
		return url + append;
	}

	protected void updateToken()
	{
		if (accessToken == null)
		{
			if (!StringUtils.isEmpty(connectionConfig.getAccessToken()))
			{
				accessToken = connectionConfig.getAccessToken();
			}
			else if (!StringUtils.isEmpty(connectionConfig.getUsername()) && !StringUtils.isEmpty(connectionConfig.getPassword()))
			{
				LOG.debug("Authenticationg using username/password");
				final PostMethod method = new PostMethod(connectionConfig.getAuthServerUrl()
						+ connectionConfig.getAuthServerAccessToken());
				method.setParameter("client_id", connectionConfig.getClientId());
				method.setParameter("client_secret", connectionConfig.getClientSecret());

				method.setParameter("username", connectionConfig.getUsername());
				method.setParameter("password", connectionConfig.getPassword());
				method.setParameter("grant_type", "password");

				final String url = getMethodUrl(method);
				try
				{
					final int statusCode = client.executeMethod(method);
					LOG.debug("Got response code: {}", Integer.valueOf(statusCode));
					if (statusCode != HttpStatus.SC_OK)
					{
						LOG.error("Authorization call failed: {} ({})", method.getStatusLine(), url);
					}

					final Map authResponse = getMapFromJson(method);

					if (authResponse.containsKey("access_token"))
					{
						accessToken = (String) authResponse.get("access_token");
					}
				}
				catch (final HttpException e)
				{
					LOG.error("Fatal protocol violation: {} ({})", e.getMessage(), url);
				}
				catch (final IOException e)
				{
					LOG.error("Fatal transport error: {} ({})", e.getMessage(), url);
				}
				finally
				{
					method.releaseConnection();
				}
			}
		}
	}

	protected void invalidateToken()
	{
		accessToken = null;
	}

	protected Map getMapFromJson(final HttpMethodBase method) throws IOException
	{
		final StringWriter writer = new StringWriter();
		IOUtils.copy(method.getResponseBodyAsStream(), writer, method.getResponseCharSet());
		final String jsonResponse = writer.toString();
		LOG.debug("Got response: {}", jsonResponse);

		try
		{
			final Map map = gson.fromJson(jsonResponse, Map.class);
			LOG.debug("Parsed response: {}", map);
			return map;
		}
		catch (final JsonParseException e)
		{
			LOG.error("Invalid JSON received: {} ({})", jsonResponse, getMethodUrl(method));
			return null;
		}
	}

	/**
	 * @return the connectionConfig
	 */
	public OAuth2ConnectionConfig getConnectionConfig()
	{
		return connectionConfig;
	}
}
