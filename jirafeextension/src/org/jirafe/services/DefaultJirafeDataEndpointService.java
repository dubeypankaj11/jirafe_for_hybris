/**
 * 
 */
package org.jirafe.services;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.user.UserService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.converter.JirafeConvertException;
import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.converter.UTCFormatter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
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
		final StringBuilder query = new StringBuilder("SELECT {" + ItemModel.PK + "} FROM {" + type + "} ");
		if (startTime != null || endTime != null || pageToken != null)
		{
			query.append("WHERE ");
			if (pageToken != null)
			{
				try
				{
					final String pt[] = pageToken.split("/");
					params.put("ptTime", UTCFormatter.toLocal(pt[0]));
					params.put("ptPK", pt[1]);
				}
				catch (final ArrayIndexOutOfBoundsException e)
				{
					throw new BadArgument("page token", pageToken);
				}
				catch (final ParseException e)
				{
					throw new BadArgument("page token", pageToken);
				}
				query.append("({" + ItemModel.MODIFIEDTIME + "} > ?ptTime OR {" + ItemModel.MODIFIEDTIME + "} = ?ptTime AND {"
						+ ItemModel.PK + "} > ?ptPK) ");
			}
			else if (startTime != null)
			{
				try
				{
					params.put("startTime", UTCFormatter.toLocal(startTime));
				}
				catch (final ParseException e)
				{
					throw new BadArgument("start time", startTime);
				}
				query.append("{" + ItemModel.MODIFIEDTIME + "} >= ?startTime ");
			}
			if (endTime != null)
			{
				try
				{
					params.put("endTime", UTCFormatter.toLocal(endTime));
				}
				catch (final ParseException e)
				{
					throw new BadArgument("end time", endTime);
				}
				query.append("AND {" + ItemModel.MODIFIEDTIME + "} <= ?endTime ");
			}
		}
		query.append("ORDER BY {" + ItemModel.MODIFIEDTIME + "}, {" + ItemModel.PK + "} ASC");
		return query.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeDataEndpointService#getDate(java.lang.String, java.util.Date, java.util.Date,
	 * int, java.lang.String)
	 */
	@Override
	public String getData(final String argType, final String startTime, final String endTime, final String strPageLimit,
			final String pageToken)
	{
		final Gson gson = new Gson();
		final HashMap<String, Object> data = new HashMap<String, Object>();
		final int pageLimit;
		try
		{
			pageLimit = Integer.parseInt(strPageLimit);
		}
		catch (final NumberFormatException e)
		{
			return badArg("page limit", strPageLimit);
		}
		final Map<String, Object> params = new HashMap<String, Object>();

		// Accept random case and convert to capital first letter plus lower case
		// We don't check if it's a supported type here, but the toMap call below will
		// block any unsupported types from being returned.
		final String type = Character.toUpperCase(argType.charAt(0)) + argType.substring(1).toLowerCase();

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
		final SearchResult<ItemModel> searchResult = flexibleSearchService.search(fQuery);
		final int total = searchResult.getTotalCount();

		final ArrayList<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
		ItemModel lastResult = null;
		try
		{
			for (final ItemModel result : searchResult.getResult())
			{
				if (jirafeMappingsDao.filter(type, result, false))
				{
					response.add(jirafeJsonConverter.toMap(new JirafeDataDto(type, result, false)));
				}
				lastResult = result;
			}
			data.put("success", true);
			data.put("response", response);
			if (total > pageLimit)
			{
				final String nextPage = UTCFormatter.format(lastResult.getModifiedtime()) + "/" + lastResult.getPk();
				data.put("page_token", nextPage);
			}
		}
		catch (final JirafeConvertException e)
		{
			log.error("Failed to generate json due to: {}", e.getMessage());
			data.put("success", false);
			data.put("error_type", e.getMessage());
		}

		return gson.toJson(data, Map.class);
	}
}
