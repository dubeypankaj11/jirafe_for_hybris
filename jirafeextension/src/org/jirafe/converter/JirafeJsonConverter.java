/**
 * 
 */
package org.jirafe.converter;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.webservices.OAuth2ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import groovy.lang.Binding;
import groovy.lang.Script;


/**
 * @author alex
 * 
 */
public class JirafeJsonConverter
{
	private static final Logger log = LoggerFactory.getLogger(JirafeJsonConverter.class);
	static final String DEFINITION_ERROR = "Failed to load mapping definition for <%s>, please check your configuration or disable interceptor for this type.";

	private JirafeMappingsDao jirafeMappingsDao;

	@Resource
	private ModelService modelService;
	@Resource
	private OAuth2ConnectionConfig connectionConfig;
	@Resource
	JirafeScriptPool jirafeScriptPool;

	final private Gson gson = new Gson();

	/**
	 * Returns TRUE if the model should be intercepted, FALSE if it should be ignored.
	 */
	public boolean filter(final String type, final ItemModel itemModel)
	{
		return filter(type, itemModel, null);
	}

	public boolean filter(final String type, final ItemModel itemModel, final String filter)
	{
		Boolean ret = Boolean.FALSE;
		final Script script = jirafeScriptPool.acquireScript();
		try
		{
			final Object expr;
			if (filter != null)
			{
				expr = compileToClosure(script, null, filter);
			}
			else
			{
				expr = getDefinitionMap(script, type).get("__filter__");
			}
			if (expr != null)
			{
				ret = (Boolean) script.invokeMethod("getValue", new Object[]
				{ itemModel, expr });
			}
			else
			{
				// No filter means all may pass
				ret = Boolean.TRUE;
			}
		}
		catch (final Exception e)
		{
			log.error("Failed to apply filter due to {}", e.getMessage(), e);
		}
		finally
		{
			jirafeScriptPool.releaseScript(script);
		}
		return ret.booleanValue();
	}

	public Map<String, Object> toMap(final JirafeDataDto jirafeDataDto) throws JirafeConvertException
	{
		final Script script = jirafeScriptPool.acquireScript();
		try
		{
			final Map definition = getDefinitionMap(script, jirafeDataDto.getJirafeTypeCode());
			if (definition == null)
			{
				throw new JirafeConvertException(String.format(DEFINITION_ERROR, jirafeDataDto.getJirafeTypeCode()));
			}
			return toMap(script, jirafeDataDto, definition);
		}
		finally
		{
			jirafeScriptPool.releaseScript(script);
		}
	}

	public Map<String, Object> toMap(final JirafeDataDto jirafeDataDto, final String strMap) throws JirafeConvertException
	{
		final Script script = jirafeScriptPool.acquireScript();
		try
		{
			final Map definition = compileMap(script, strMap);
			return toMap(script, jirafeDataDto, definition);
		}
		finally
		{
			jirafeScriptPool.releaseScript(script);
		}
	}

	private Map<String, Object> toMap(final Script script, final JirafeDataDto jirafeDataDto, final Map definition)
			throws JirafeConvertException
	{
		final Binding binding = script.getBinding();
		binding.setVariable("cookies", jirafeDataDto.getCookies());
		binding.setVariable("site", jirafeDataDto.getSite());
		binding.setVariable("modelService", modelService);
		final LinkedList<String> errors = new LinkedList<String>();
		binding.setVariable("errors", errors);
		binding.setVariable("context", new LinkedList<String>());
		final Map target = (Map) script.invokeMethod("toMap", new Object[]
		{ jirafeDataDto.getItemModel(), definition });
		if (errors.size() > 0)
		{
			target.put("__errors__", StringUtils.join(errors, ','));
		}
		return target;
	}

	public String[] getSites(final ItemModel itemModel) throws JirafeConvertException
	{
		Object sites = null;
		final String type = jirafeMappingsDao.getMappedType(itemModel);
		final Script script = jirafeScriptPool.acquireScript();
		try
		{
			final Object expr = getDefinitionMap(script, type).get("__sites__");
			sites = script.invokeMethod("getValue", new Object[]
			{ itemModel, expr });
		}
		catch (final Exception e)
		{
			log.error("Failed to get sites due to {}", e.getMessage(), e);
			sites = null;
		}
		finally
		{
			jirafeScriptPool.releaseScript(script);
		}
		// We accept either a string or an array or list of strings
		if (sites == null)
		{
			return new String[0];
		}
		List<String> sitesList;
		if (sites instanceof String)
		{
			// Special case "*" - all sites
			if (sites.equals("*"))
			{
				return connectionConfig.getSiteNames();
			}
			sitesList = Collections.singletonList((String) sites);
		}
		else if (sites instanceof List)
		{
			sitesList = (List<String>) sites;
		}
		else
		{
			sitesList = Arrays.asList((String[]) sites);
		}

		final List<String> ret = ListUtils.retainAll(sitesList, Arrays.asList(connectionConfig.getSiteNames()));
		return ret.toArray(new String[ret.size()]);
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

	private void compileFilter(final Script script, final Map<String, Object> map, final String type)
	{
		final String filter = jirafeMappingsDao.loadFilter(type);
		if (StringUtils.isEmpty(filter))
		{
			return;
		}
		compileExpression(script, map, "__filter__", null, filter);
	}

	private Map compileMap(final Script script, final String strMap)
	{
		final Map map = gson.fromJson(strMap, LinkedHashMap.class);
		compileMap(script, map);
		return map;
	}

	private void compileMap(final Script script, final Map<String, Object> map)
	{
		for (final String key : map.keySet())
		{
			final Object value = map.get(key);
			if (value instanceof Map)
			{
				compileMap(script, (Map<String, Object>) value);
			}
			else if (value instanceof List)
			{
				// The list currently must look like:
				//		[{"_path", "string"}, map]
				// We just need to cook the map
				final List list = (List) value;
				compileMap(script, (Map<String, Object>) list.get(1));
			}
			else
			{
				final String s = (String) value;
				final int firstAtsign = s.indexOf("@{");
				String param = null;
				String expr;
				if (firstAtsign < 0)
				{
					// Simple mapping
					expr = String.format("model.%s", s.replace(".", "?."));
				}
				else
				{
					final int lastBrace = s.lastIndexOf('}');
					if (firstAtsign > 0)
					{
						param = s.substring(0, firstAtsign);
					}
					expr = s.substring(firstAtsign + 2, lastBrace);
				}
				compileExpression(script, map, key, param, expr);
			}
		}
	}

	private Object compileToClosure(final Script script, final String param, final String expr)
	{
		String groovy = "{->";
		if (param != null)
		{
			// param may be a path. If it is, change the '.' to '?.' for safety
			// and assign the result to a local variable named by the last component
			final String[] path = StringUtils.split(param, '.');
			groovy += String.format("def %s=model.%s;", path[path.length - 1], StringUtils.join(path, "?."));
		}
		// Can't leave the imports inside the closures, move them to the end 
		final String[] lines = expr.split("\r*\n");
		final List<String> imports = new LinkedList<String>();
		for (final String line : lines)
		{
			if (line.matches("\\s*import\\s.*"))
			{
				imports.add(line);
			}
			else
			{
				groovy += line + "\n";
			}
		}
		groovy += "}\n" + StringUtils.join(imports, '\n');

		try
		{
			return script.evaluate(groovy);
		}
		catch (final CompilationFailedException e)
		{
			log.debug("Exception while evaluating: {}", groovy);
			log.error("", e);
			return null;
		}
	}

	private void compileExpression(final Script script, final Map map, final String key, final String param, final String expr)
	{
		final Object closure = compileToClosure(script, param, expr);
		if (closure != null)
		{
			map.put(key, closure);
		}
	}

	public Map getDefinitionMap(final Script script, final String type)
	{
		final Map<String, Map> mapCache = (Map<String, Map>) script.getProperty("__mapCache__");
		Map map = mapCache.get(type);

		if (map != null)
		{
			return map;
		}
		final String jsonDefinition = jirafeMappingsDao.loadDefinition(type);
		if (jsonDefinition == null)
		{
			log.error(String.format(DEFINITION_ERROR, type));
			return null;
		}
		map = gson.fromJson(jsonDefinition, LinkedHashMap.class);
		compileMap(script, map);
		compileFilter(script, map, type);
		mapCache.put(type, map);
		return map;
	}

	/**
	 * @param type
	 */
	public void invalidateDefinitionMap(final String type)
	{
		// Just zap the whole pool - it doesn't happen often
		jirafeScriptPool.reset();
	}

	public void setJirafeMappingsDao(final JirafeMappingsDao jirafeMappingsDao)
	{
		this.jirafeMappingsDao = jirafeMappingsDao;
	}

}
