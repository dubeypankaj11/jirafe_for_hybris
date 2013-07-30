/**
 * 
 */
package org.jirafe.converter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
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

	private static final Logger log = LoggerFactory.getLogger(JirafeJsonConverter.class);
	static final String DEFINITION_ERROR = "Failed to load mapping definition for <%s>, please check your configuration or disable interceptor for this type.";

	public String toJson(final JirafeDataDto jirafeDataDto) throws JirafeConvertException
	{
		return toJson(toMap(jirafeDataDto));
	}

	public Map<String, Object> toMap(final JirafeDataDto jirafeDataDto) throws JirafeConvertException
	{
		return toMap(jirafeDataDto, null);
	}

	public Map<String, Object> toMap(final JirafeDataDto jirafeDataDto, Iterable keyset) throws JirafeConvertException
	{
		final Map target = new HashMap();
		final Map definition = getDefinitionMap(jirafeDataDto.getJirafeTypeCode());
		if (definition == null)
		{
			throw new JirafeConvertException(String.format(DEFINITION_ERROR, jirafeDataDto.getJirafeTypeCode()));
		}
		if (keyset == null)
		{
			keyset = definition.keySet();
		}
		new JirafeModelToMapConverter(jirafeDataDto).convert(jirafeDataDto.getItemModel(), definition, target, keyset);
		return target;
	}

	public String toJson(final Map map)
	{
		return gson.toJson(map);
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
