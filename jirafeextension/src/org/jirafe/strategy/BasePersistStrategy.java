/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.jalo.JaloObjectNoLongerValidException;
import de.hybris.platform.servicelayer.model.ModelService;

import javax.annotation.Resource;

import org.jirafe.converter.JirafeConvertException;
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

	private final static String emptyJson = "{}";
	private final static String errorJson = "{'error':'%s'}";

	private JirafeDataDao jirafeDataDao;
	private JirafeJsonConverter jirafeJsonConverter;

	@Resource(name = "modelService")
	protected ModelService modelService;

	protected void doPersist(final JirafeDataDto jirafeDataDto)
	{
		String jsonRepresentation;
		final ItemModel itemModel = jirafeDataDto.getItemModel();

		if (itemModel == null || modelService.isRemoved(itemModel))
		{
			return;
		}

		try
		{
			log.debug("About to call JsonConverter");
			jsonRepresentation = jirafeJsonConverter.toJson(jirafeDataDto);
		}
		catch (final JaloObjectNoLongerValidException e)
		{
			// It's ok to lose a cart
			if (itemModel instanceof CartModel)
			{
				// We'll probably downgrade this to a debug message
				log.info(e.getMessage());
				return;
			}
			throw e;
		}
		catch (final JirafeConvertException e)
		{
			log.error("Failed to generate json due to: {}", e.getMessage());
			return;
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
					jirafeDataDto.getIsRemove());
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
