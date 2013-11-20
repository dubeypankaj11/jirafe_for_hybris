/**
 * 
 */
package org.jirafe.converter;

import de.hybris.platform.core.model.ItemModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.webservices.OAuth2ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;


/**
 * @author alex
 * 
 */
public class JirafeJsonConverter
{
	final private Gson gson = new Gson();

	private JirafeMappingsDao jirafeMappingsDao;

	@Resource
	OAuth2ConnectionConfig connectionConfig;

	private static final Logger log = LoggerFactory.getLogger(JirafeJsonConverter.class);
	static final String DEFINITION_ERROR = "Failed to load mapping definition for <%s>, please check your configuration or disable interceptor for this type.";

	public Map<String, Object> toMap(final JirafeDataDto jirafeDataDto) throws JirafeConvertException
	{
		return toMap(jirafeDataDto, null);
	}

	private Map<String, Object> toMap(final JirafeDataDto jirafeDataDto, final Iterable keyset) throws JirafeConvertException
	{
		return toMap(jirafeDataDto, jirafeDataDto.getItemModel(), keyset);
	}

	private Map<String, Object> toMap(final JirafeDataDto jirafeDataDto, final ItemModel itemModel, Iterable keyset)
			throws JirafeConvertException
	{
		final Map target = new HashMap();
		final String type = jirafeMappingsDao.getMappedType(itemModel);
		final Map definition = getDefinitionMap(type);
		if (definition == null)
		{
			throw new JirafeConvertException(String.format(DEFINITION_ERROR, type));
		}
		if (keyset == null)
		{
			keyset = definition.keySet();
		}
		new JirafeModelToMapConverter(jirafeDataDto).convert(itemModel, definition, target, keyset);
		return target;
	}

	public String[] getSites(final ItemModel itemModel) throws JirafeConvertException
	{
		final List<String> keys = new ArrayList<String>(1);
		keys.add("__sites__");
		final Map map = toMap(null, itemModel, keys);
		final Object sites = map.get("__sites__");
		// We accept either a string or an array or list of strings
		if (sites instanceof String)
		{
			// Special case "*" - all sites
			if (sites.equals("*"))
			{
				return connectionConfig.getSiteNames();
			}
			return new String[]
			{ (String) sites };
		}
		if (sites instanceof List)
		{
			final List<String> s = (List<String>) sites;
			return s.toArray(new String[s.size()]);
		}
		return (String[]) sites;
	}

	private <T> T stripSpecials(final T object)
	{
		if (object instanceof List)
		{
			return (T) stripSpecials((List) object);
		}
		if (object instanceof Map)
		{
			return (T) stripSpecials((Map) object);
		}
		return object;
	}

	private <T> List<T> stripSpecials(final List<T> list)
	{
		List<T> ret = list;
		for (int i = 0; i < list.size(); ++i)
		{
			final T v0 = list.get(i);
			final T v1 = stripSpecials(v0);
			if (v0 != v1)
			{
				if (ret == list)
				{
					ret = new ArrayList<T>(list);
				}
				ret.set(i, v1);
			}
		}
		return ret;
	}

	private Map<String, Object> stripSpecials(final Map<String, Object> map)
	{
		Map<String, Object> ret = new LinkedHashMap<String, Object>(map);
		for (final String key : map.keySet())
		{
			final Object v0 = map.get(key);
			Object v1 = null;
			if (!key.startsWith("__") || key.equals("__errors__"))
			{
				v1 = stripSpecials(v0);
			}
			if (v0 != v1)
			{
				if (ret == map)
				{
					ret = new HashMap<String, Object>(map);
				}
				if (v1 != null)
				{
					ret.put(key, v1);
				}
				else
				{
					ret.remove(key);
				}
			}
		}
		return ret;
	}

	public String toJson(final Object object)
	{
		final Object stripped = stripSpecials(object);

		return gson.toJson(stripped);
	}

	public Map getDefinitionMap(final String type)
	{
		final String jsonDefinition = jirafeMappingsDao.loadDefinition(type);
		if (jsonDefinition == null)
		{
			log.error(String.format(DEFINITION_ERROR, type));
			return null;
		}
		return gson.fromJson(jsonDefinition, LinkedHashMap.class);
	}

	public void setJirafeMappingsDao(final JirafeMappingsDao jirafeMappingsDao)
	{
		this.jirafeMappingsDao = jirafeMappingsDao;
	}

}
