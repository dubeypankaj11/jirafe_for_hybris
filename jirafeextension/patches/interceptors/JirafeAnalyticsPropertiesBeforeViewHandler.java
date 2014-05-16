/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2013 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 *
 *
 */
package org.jirafe.interceptors;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;
import de.hybris.platform.util.Config;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jirafe.webservices.OAuth2ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;


public class JirafeAnalyticsPropertiesBeforeViewHandler implements JirafeBeforeViewHandler
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeAnalyticsPropertiesBeforeViewHandler.class);

	@Resource
	CMSSiteService cmsSiteService;
	@Resource
	private OAuth2ConnectionConfig connectionConfig;

	@Override
	public void beforeView(final HttpServletRequest request, final HttpServletResponse response, final ModelAndView modelAndView)
	{
		final String siteName = getSiteName(request);

		if (siteName != null)
		{
			modelAndView.addObject("jirafe2SiteId", connectionConfig.getSiteId(siteName));
			modelAndView.addObject("jirafe2ApiUrl", Config.getString("jirafe.api.url", null));
		}
	}

	protected String getSiteName(final HttpServletRequest request)
	{
		String siteName = (String) request.getAttribute("org.jirafe.siteName");

		if (StringUtils.isEmpty(siteName))
		{
			final String queryString = request.getQueryString();
			final StringBuffer fullURL = request.getRequestURL();
			if (queryString != null)
			{
				fullURL.append('?').append(queryString);
			}
			try
			{
				siteName = cmsSiteService.getSiteForURL(new URL(fullURL.toString())).getUid();
				request.getSession().setAttribute("org.jirafe.siteName", siteName);
			}
			catch (CMSItemNotFoundException e)
			{
				// Without a site id, we don't need to hear about it
				LOG.debug("No site id match for request {}, ignoring.", fullURL);
				siteName = null;
			}
			catch (MalformedURLException e)
			{
				// Without a site id, we don't need to hear about it
				LOG.debug("No site id match for request {}, ignoring.", fullURL);
				siteName = null;
			}
		}
		return siteName;
	}
}
