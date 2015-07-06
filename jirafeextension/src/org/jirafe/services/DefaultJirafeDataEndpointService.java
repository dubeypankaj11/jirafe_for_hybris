/**
 *
 */
package org.jirafe.services;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.jirafe.converter.JirafeConvertException;
import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dao.JirafePagerDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.webservices.OAuth2ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @author dbrand
 *
 */
@Component("jirafeDataEndpointService")
public class DefaultJirafeDataEndpointService implements JirafeDataEndpointService
{
	private final static Logger log = LoggerFactory.getLogger(DefaultJirafeDataEndpointService.class);

	@Resource
	JirafePagerDao jirafePagerDao;

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;

	@Resource
	private FlexibleSearchService flexibleSearchService;

	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	@Resource
	private OAuth2ConnectionConfig connectionConfig;

	private String normalizeType(final String argType)
	{
		// Accept random case and convert to capital first letter plus lower case
		// We don't check if it's a supported type here, but the toMap call below will
		// block any unsupported types from being returned.
		return Character.toUpperCase(argType.charAt(0)) + argType.substring(1).toLowerCase();
	}

	private boolean validPK(final String pk)
	{
		return pk.matches("^\\d+$");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeDataEndpointService#getDate(java.lang.String, java.util.Date, java.util.Date,
	 * int, java.lang.String)
	 */
	@Override
	public String getData(final String argType, String siteId, final String startTime, final String endTime,
			final String strPageLimit, final String pageToken)
	{
		if (StringUtils.isEmpty(siteId))
		{
			siteId = connectionConfig.getSiteId();
		}
		final int pageLimit;
		try
		{
			if (strPageLimit == null)
			{
				// Default the size to something very safe
				pageLimit = 10;
			}
			else
			{
				pageLimit = Integer.parseInt(strPageLimit);
			}
		}
		catch (final NumberFormatException e)
		{
			return jirafePagerDao.badArg("page limit", strPageLimit);
		}
		final String type = normalizeType(argType);

		final FlexibleSearchQuery fQuery;
		try
		{
			fQuery = jirafePagerDao.buildQuery(type, startTime, endTime, pageLimit, pageToken);
		}
		catch (final JirafePagerDao.BadArgument e)
		{
			return e.message;
		}
		return queryAndReturn(fQuery, type, siteId, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeDataEndpointService#getDate(java.lang.String, java.util.Date, java.util.Date,
	 * int, java.lang.String)
	 */
	@Override
	public String getData(final String argType, final String pk, final String strMap)
	{
		final String type = normalizeType(argType);
		if (!validPK(pk))
		{
			return jirafePagerDao.badArg("pk", pk);
		}
		final String query = //
		"SELECT {" + ItemModel.PK + "} FROM {" + type + "} WHERE {" + ItemModel.PK + "} = ?pk";
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("pk", pk);
		final FlexibleSearchQuery fQuery = new FlexibleSearchQuery(query, params);
		return queryAndReturn(fQuery, type, null, strMap);
	}

	private String siteFilter(final String siteId, final ItemModel itemModel) throws JirafeConvertException
	{
		if (siteId == null)
		{
			// Default site
			return connectionConfig.getSiteNameFromId(connectionConfig.getSiteId());
		}
		final String[] mapSites = jirafeJsonConverter.getSites(itemModel);
		for (final String mapSite : mapSites)
		{
			final String mapSiteId = connectionConfig.getSiteId(mapSite);
			if (siteId.equals(mapSiteId))
			{
				return mapSite;
			}
		}
		return null;
	}

	private String queryAndReturn(final FlexibleSearchQuery fQuery, final String type, final String siteId, final String strMap)
	{
		final HashMap<String, Object> data = new HashMap<String, Object>();

		final SearchResult<ItemModel> searchResult = flexibleSearchService.search(fQuery);

		final ArrayList<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
		ItemModel lastResult = null;
		try
		{
			for (final ItemModel result : searchResult.getResult())
			{
				lastResult = result;
				if (!StringUtils.isEmpty(strMap))
				{
					final JirafeDataDto jirafeDataDto = new JirafeDataDto(type, result, connectionConfig.getSiteNameFromId(siteId));
					final Map target = jirafeJsonConverter.toMap(jirafeDataDto, strMap);
					response.add(target);
				}
				else
				{
					if (jirafeMappingsDao.filter(type, result))
					{
						final String site = siteFilter(siteId, result);
						if (site != null)
						{
							final JirafeDataDto jirafeDataDto = new JirafeDataDto(type, result, site);
							final Map<String, Object> target = jirafeJsonConverter.toMap(jirafeDataDto);
							response.add(target);
						}
					}
				}
			}
			data.put("success", true);
			data.put("response", response);
			final String nextPage = jirafePagerDao.buildPageToken(searchResult, lastResult);
			if (nextPage != null)
			{
				data.put("page_token", nextPage);
			}
		}
		catch (final Exception e)
		{
			log.error("Failed to generate json due to: ", e);
			data.put("success", false);
			data.put("error_type", e.getMessage());
		}

		return jirafeJsonConverter.toJson(data);
	}

}
