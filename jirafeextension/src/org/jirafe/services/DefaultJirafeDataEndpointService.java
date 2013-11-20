/**
 * 
 */
package org.jirafe.services;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.util.Config;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.jirafe.converter.JirafeConvertException;
import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.converter.JirafeModelToMapConverter;
import org.jirafe.converter.UTCFormatter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.webservices.OAuth2ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;


/**
 * @author dbrand
 * 
 */
@Component("jirafeDataEndpointService")
public class DefaultJirafeDataEndpointService implements JirafeDataEndpointService
{
	private final static Logger log = LoggerFactory.getLogger(DefaultJirafeDataEndpointService.class);

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;

	@Resource
	private FlexibleSearchService flexibleSearchService;

	@Resource
	private UserService userService;

	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	@Resource
	private OAuth2ConnectionConfig connectionConfig;

	static String to_date1 = "";
	static String to_date2 = "";
	{
		if (Config.isOracleUsed())
		{
			to_date1 = "to_timestamp(";
			to_date2 = ", '" + Config.getString("jirafe.oracleFormatString", "YYYY-MM-DD\"T\"HH24:MI:SS.FF3") + "')";
		}
	}

	String dbDate(final Map params, final String key, final String time, final String desc) throws BadArgument
	{
		try
		{
			params.put(key, UTCFormatter.toLocal(time));
		}
		catch (final ParseException e)
		{
			throw new BadArgument(desc, time);
		}
		return to_date1 + "?" + key + to_date2;
	}

	private String badArg(final String name, final String val)
	{
		final Gson gson = new Gson();
		final HashMap<String, Object> data = new HashMap<String, Object>();
		final String msg = String.format("Invalid %s: %s", name, val);
		log.error(msg);
		data.put("success", false);
		data.put("error_type", msg);
		return gson.toJson(data, Map.class);
	}

	class BadArgument extends Exception
	{
		public final String message;

		public BadArgument(final String name, final String val)
		{
			message = badArg(name, val);
		}
	}

	private final String buildQuery(final Map params, final String type, final String startTime, final String endTime,
			final int pageLimit, final String pageToken) throws BadArgument
	{
		final StringBuilder query = new StringBuilder( //
				"SELECT {" + ItemModel.PK + "} FROM {" + type + "} ");
		if (startTime != null || endTime != null || pageToken != null)
		{
			query.append("WHERE ");
			String ptTime;
			if (pageToken != null)
			{
				try
				{
					final String pt[] = pageToken.split("/");
					ptTime = pt[0];
					params.put("ptPK", pt[1]);
				}
				catch (final ArrayIndexOutOfBoundsException e)
				{
					throw new BadArgument("page token", pageToken);
				}
				query.append( //
				"({" + ItemModel.MODIFIEDTIME + "} > " + dbDate(params, "ptTime", ptTime, "page token") + " OR" + //
						" {" + ItemModel.MODIFIEDTIME + "} = " + to_date1 + "?ptTime" + to_date2 + " AND" + //
						" {" + ItemModel.PK + "} > ?ptPK) ");
			}
			else if (startTime != null)
			{
				query.append("{" + ItemModel.MODIFIEDTIME + "} >= " + dbDate(params, "startTime", startTime, "start time") + " ");
			}
			if (endTime != null)
			{
				if (pageToken != null || startTime != null)
				{
					query.append("AND ");
				}
				query.append("{" + ItemModel.MODIFIEDTIME + "} <= " + dbDate(params, "endTime", endTime, "end time") + " ");
			}
		}
		query.append("ORDER BY {" + ItemModel.MODIFIEDTIME + "}, {" + ItemModel.PK + "} ASC");
		return query.toString();
	}

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
			return badArg("page limit", strPageLimit);
		}
		final Map<String, Object> params = new HashMap<String, Object>();

		final String type = normalizeType(argType);

		final String query;
		try
		{
			query = buildQuery(params, type, startTime, endTime, pageLimit, pageToken);
		}
		catch (final BadArgument e)
		{
			return e.message;
		}
		log.info("Generated query: {}", query);
		log.info("... params: {}", params);

		final FlexibleSearchQuery fQuery = new FlexibleSearchQuery(query, params);
		fQuery.setCount(pageLimit);
		fQuery.setNeedTotal(true);
		//userService.setCurrentUser(userService.getUserForUID(Config.getString("jirafe.security.userName", "jirafeuser")));
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
			return badArg("pk", pk);
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
		final Gson gson = new Gson();
		final HashMap<String, Object> data = new HashMap<String, Object>();

		final SearchResult<ItemModel> searchResult = flexibleSearchService.search(fQuery);

		final ArrayList<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
		ItemModel lastResult = null;
		try
		{
			final LinkedHashMap<String, Object> map = gson.fromJson(strMap, LinkedHashMap.class);
			for (final ItemModel result : searchResult.getResult())
			{
				if (jirafeMappingsDao.filter(type, result))
				{
					if (map != null)
					{
						final Map target = new HashMap();
						new JirafeModelToMapConverter(null).convert(result, map, target);
						response.add(target);
					}
					else
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
				lastResult = result;
			}
			data.put("success", true);
			data.put("response", response);
			final int requested = searchResult.getRequestedCount();
			final int total = searchResult.getTotalCount();
			if (requested > 0 && total > requested)
			{
				final String nextPage = UTCFormatter.format(lastResult.getModifiedtime()) + "/" + lastResult.getPk();
				data.put("page_token", nextPage);
			}
		}
		catch (final Exception e)
		{
			log.error("Failed to generate json due to: {}", e.getMessage());
			data.put("success", false);
			data.put("error_type", e.getMessage());
		}

		return jirafeJsonConverter.toJson(data);
	}

}
