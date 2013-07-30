/**
 * 
 */
package org.jirafe.converter;

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jirafe.dto.JirafeDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;


/**
 * @author alex
 * 
 */
public class JirafeModelToMapConverter
{

	private static final String PATH_ATTRIBUTE = "_path";
	private static final Logger log = LoggerFactory.getLogger(JirafeModelToMapConverter.class);

	/** The model service. */
	private final ModelService modelService;

	private final GroovyShell shell;
	private final JirafeJsonConverter jirafeJsonConverter;

	public JirafeModelToMapConverter(final JirafeDataDto jirafeDataDto)
	{
		super();
		modelService = (ModelService) Registry.getApplicationContext().getBean("modelService");
		final Binding binding = new Binding();
		binding.setVariable("cookies", jirafeDataDto.getCookies());
		binding.setVariable("dateFormat", UTCFormatter.getUTCFormatter());
		jirafeJsonConverter = (JirafeJsonConverter) Registry.getApplicationContext().getBean("jirafeJsonConverter");
		binding.setVariable("jirafeModelToMapConverter", this);
		binding.setVariable("modelService", modelService);
		shell = new GroovyShell(binding);
	}

	public void convert(final ItemModel model, final Map definition, final Map target, final Iterable keyset)
	{
		parseMap(definition, target, model, keyset);
	}

	public void convert(final ItemModel model, final Map definition, final Map target)
	{
		parseMap(definition, target, model, definition.keySet());
	}

	public Map<String, Object> toMap(final ItemModel model, final String type, Iterable keyset) throws JirafeConvertException
	{
		final Map target = new HashMap();
		final Map definition = jirafeJsonConverter.getDefinitionMap(type);
		if (definition == null)
		{
			throw new JirafeConvertException(String.format(JirafeJsonConverter.DEFINITION_ERROR, type));
		}
		if (keyset == null)
		{
			keyset = definition.keySet();
		}
		convert(model, definition, target, keyset);
		return target;
	}

	protected void parseMap(final Map definition, final Map target, final ItemModel model, final Iterable keyset)
	{
		if (log.isDebugEnabled())
		{
			log.debug("Using definition: {}", definition);
		}
		final Iterator keys = keyset.iterator();
		while (keys.hasNext())
		{
			final Object key = keys.next();
			final Object value = definition.get(key);

			if (value instanceof String)
			{
				//ready to get data
				populate(target, key, value, model);
			}
			if (value instanceof List)
			{
				//iterate over list
				final List list = new ArrayList();
				if (parseList((List) value, list, model, key.toString()))
				{
					target.put(key, list);
				}
			}
			if (value instanceof Map)
			{
				//dig into map
				final Map level = new HashMap();
				target.put(key, level);
				parseMap((Map) value, level, model, ((Map) value).keySet());
			}
		}
	}

	protected boolean parseList(final List list, final List targetList, final ItemModel model, final String key)
	{

		//get _path
		final String path;
		try
		{
			path = ((Map) list.get(0)).get(PATH_ATTRIBUTE).toString();
		}
		catch (final Exception e)
		{
			log.error("Path attribute is missing or invalid for list <{}>", key);
			return false;
		}
		final Map map = (Map) list.get(1);

		//get the child list
		final List<ItemModel> childItems;
		try
		{
			final Object o = getChild(path, model);
			if (o == null)
			{
				// The array is missing, skip it
				return false;
			}
			if (!(o instanceof List))
			{
				// Handle sets and maybe other similar types
				childItems = new ArrayList<ItemModel>();
				childItems.addAll((Set) o);
			}
			else
			{
				childItems = (List<ItemModel>) o;
			}
		}
		catch (final Exception e)
		{
			log.error("Failed to create list <{}> due to: {}", key, e);
			return false;
		}

		// lose the empty lists
		if (childItems.size() <= 0)
		{
			return false;
		}

		// iterate over childItems and pass child
		for (final ItemModel childItem : childItems)
		{
			final Map target = new HashMap();
			targetList.add(target);
			parseMap(map, target, childItem, map.keySet());
		}

		return true;
	}

	protected void populate(final Map map, final Object key, final Object valueKey, final ItemModel model)
	{
		final Object value = getChild(valueKey, model);
		log.debug(String.format("Updating <%s> by <%s> with <%s>", key, valueKey, value));

		map.put(key, value);
	}

	protected Object getChild(final Object valueKey, final ItemModel model)
	{
		final ValueKey key = new ValueKey(valueKey.toString());
		if (StringUtils.isEmpty(key.getKey()) && StringUtils.isEmpty(key.getGroovyCode()))
		{
			log.warn("Empty ValueKey, returning null (check object mappings)");
			return null;
		}
		if (StringUtils.isEmpty(key.getKey()))
		{
			return getChildValue(model, "model", key.getGroovyCode());
		}
		final String[] path = key.getKey().split("[.]");
		ItemModel child = model;
		Object value;
		for (int i = 0; i < path.length; i++)
		{
			if (i == path.length - 1)
			{
				value = modelService.getAttributeValue(child, path[i]);
				if (StringUtils.isEmpty(key.getGroovyCode()))
				{
					return value;
				}
				return getChildValue(value, path[i], key.getGroovyCode());
			}
			child = modelService.getAttributeValue(child, path[i]);
		}
		return null;
	}

	protected Object getChildValue(final Object value, final String key, final String groovyCode)
	{
		log.debug("Binding model as {}", key);
		shell.setVariable(key, value);

		log.debug("About to eval <{}>", groovyCode);
		return shell.evaluate(groovyCode);
	}

	private static class ValueKey
	{
		private String key;
		private String groovyCode;

		private ValueKey(final String valueKey)
		{
			if (valueKey.contains("@{"))
			{
				key = StringUtils.substringBefore(valueKey, "@{");
				groovyCode = StringUtils.substringAfter(StringUtils.substringBeforeLast(valueKey, "}"), "@{");
			}
			else
			{
				key = valueKey;
			}

		}

		/**
		 * @return the key
		 */
		public String getKey()
		{
			return key;
		}

		/**
		 * @return the groovyCode
		 */
		public String getGroovyCode()
		{
			return groovyCode;
		}

	}
}