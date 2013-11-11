/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.model.ItemModel;

import java.util.Map;

import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeDataDao;
import org.jirafe.dto.JirafeDataDto;
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

	protected void doPersist(final JirafeDataDto jirafeDataDto)
	{
		Map mapRepresentation;
		String jsonRepresentation;
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
			final String[] sites = jirafeJsonConverter.getSites(mapRepresentation);
			log.debug("JsonConverter returned: {} {}", sites, jsonRepresentation);
			if (sites != null)
			{
				for (final String site : sites)
				{
					jirafeDataDao.save(jirafeDataDto.getJirafeTypeCode(), itemModel.getPk().toString(), jsonRepresentation, site,
							false);
				}
			}
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
