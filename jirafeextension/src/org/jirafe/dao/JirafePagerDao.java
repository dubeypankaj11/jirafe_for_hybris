/**
 *
 */
package org.jirafe.dao;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.util.Config;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.converter.UTCFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;


/**
 * @author dbrand
 * 
 */
@Component
public class JirafePagerDao
{
	private final static Logger log = LoggerFactory.getLogger(JirafePagerDao.class);

	@Resource
	private UserService userService;

	static String to_date1 = "";
	static String to_date2 = "";
	{
		if (Config.isOracleUsed())
		{
			to_date1 = "to_timestamp(";
			to_date2 = ", '" + Config.getString("jirafe.oracleFormatString", "YYYY-MM-DD\"T\"HH24:MI:SS.FF3") + "')";
		}
	}

	public String badArg(final String name, final String val)
	{
		final Gson gson = new Gson();
		final HashMap<String, Object> data = new HashMap<String, Object>();
		final String msg = String.format("Invalid %s: %s", name, val);
		log.error(msg);
		data.put("success", false);
		data.put("error_type", msg);
		return gson.toJson(data, Map.class);
	}

	public class BadArgument extends Exception
	{
		public final String message;

		public BadArgument(final String name, final String val)
		{
			message = badArg(name, val);
		}
	}

	public final FlexibleSearchQuery buildQuery(final String type, final String startTime, final String endTime,
			final int pageLimit, final String pageToken) throws BadArgument
	{
		final Map<String, Object> params = new HashMap<String, Object>();
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

		log.debug("Generated query: {}", query);
		log.debug("... params: {}", params);
		final FlexibleSearchQuery fQuery = new FlexibleSearchQuery(query.toString(), params);

		fQuery.setCount(pageLimit);
		fQuery.setNeedTotal(true);
		userService.setCurrentUser(userService.getUserForUID(Config.getString("jirafe.security.userName", "jirafeuser")));

		return fQuery;
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

	public String buildPageToken(final SearchResult<ItemModel> searchResult, final ItemModel lastResult)
	{
		if (searchResult == null || lastResult == null)
		{
			return null;
		}
		final int requested = searchResult.getRequestedCount();
		final int total = searchResult.getTotalCount();
		if (requested > 0 && total > requested)
		{
			return UTCFormatter.format(lastResult.getModifiedtime()) + "/" + lastResult.getPk();
		}
		return null;
	}

}
