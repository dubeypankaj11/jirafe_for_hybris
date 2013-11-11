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
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
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
	private static final int MAX_RETRY = 1;

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
		client.getHttpConnectionManager().getParams().setConnectionTimeout(connectionConfig.getTimeOut());
	}

	public Map putMessage(final String message, final String type, final String site)
	{
		final String url = getUrl(type, site);
		LOG.debug("PUT <{}> message to {}", type, url);
		final PutMethod method = new PutMethod(url);

		try
		{
			method.setRequestEntity(new StringRequestEntity(message, "application/json", null));
		}
		catch (final UnsupportedEncodingException e)
		{
			LOG.error("Error encoding json message: {} ({})", e.getMessage(), url);
			return null;
		}
		return sendMessage(method);
	}

	protected Map sendMessage(final HttpMethodBase method)
	{
		return sendMessage(method, 0);
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

	protected Map sendMessage(final HttpMethodBase method, final int retry)
	{
		final String url = getMethodUrl(method);

		if (retry >= MAX_RETRY)
		{
			LOG.error("Retry limit reached ({})", url);
			return null;
		}

		try
		{
			method.setRequestHeader(getHeader());
			final int statusCode = client.executeMethod(method);
			LOG.debug("Got response code: {}", Integer.valueOf(statusCode));
			if (statusCode == 403)
			{
				invalidateToken();
				return sendMessage(method, retry + 1);
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

	protected String getUrl(final String type, final String site)
	{
		return connectionConfig.getEventApiUrl() + connectionConfig.getSiteId(site) + "/" + type;
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
