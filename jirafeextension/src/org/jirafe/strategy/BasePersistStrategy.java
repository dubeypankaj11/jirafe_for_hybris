/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.PK;
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
			log.debug("{}: itemModel is null, skipping...");
			return;
		}
		final PK pk = itemModel.getPk();
		if (pk == null)
		{
			log.debug("{}: pk is null, skipping...");
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

			final String typeCode = jirafeDataDto.getJirafeTypeCode();
			final String site = jirafeDataDto.getSite();

			jirafeDataDao.save(typeCode, pk.toString(), jsonRepresentation, site, false);
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
