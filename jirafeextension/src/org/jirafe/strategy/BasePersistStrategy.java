/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.model.ItemModel;

import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeDataDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.webservices.JirafeOAuth2Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base persistence strategy for implementations to extend. Provides basic saving mechanism plus iterating through
 * filters.
 * 
 * @author Larry Ramponi
 * 
 */
public abstract class BasePersistStrategy implements JirafeDataPersistStrategy
{
	private final static Logger log = LoggerFactory.getLogger(BasePersistStrategy.class);

	private JirafeDataDao jirafeDataDao;
	private JirafeJsonConverter jirafeJsonConverter;
	@Resource
	private JirafeOAuth2Session jirafeOAuth2Session;

	protected void doPersist(final JirafeDataDto jirafeDataDto)
	{
		Map mapRepresentation;
		String jsonRepresentation;

		if (jirafeOAuth2Session.getConnectionConfig().getSiteId(jirafeDataDto.getSite()) == null)
		{
			return;
		}

		final ItemModel itemModel = jirafeDataDto.getItemModel();

		if (itemModel == null)
		{
			return;
		}

		try
		{
			log.debug("About to call JsonConverter");
			mapRepresentation = jirafeDataDto.getMap();
			jsonRepresentation = jirafeJsonConverter.toJson(mapRepresentation);
		}
		catch (final Exception e)
		{
			log.error("Failed to convert to json, model of type {} due to : {}", jirafeDataDto.getJirafeTypeCode(), e);
			return;
		}

		try
		{
			log.debug("JsonConverter returned: {}", jsonRepresentation);
			jirafeDataDao.save(jirafeDataDto.getJirafeTypeCode(), itemModel.getPk().toString(), jsonRepresentation,
					jirafeDataDto.getSite(), false);
		}
		catch (final Exception e)
		{
			log.error("Failed to persist jirafeDataDao due to: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param jirafeDataDao
	 */
	public void setJirafeDataDao(final JirafeDataDao jirafeDataDao)
	{
		this.jirafeDataDao = jirafeDataDao;
	}

	public void setJirafeJsonConverter(final JirafeJsonConverter jirafeJsonConverter)
	{
		this.jirafeJsonConverter = jirafeJsonConverter;
	}

}
