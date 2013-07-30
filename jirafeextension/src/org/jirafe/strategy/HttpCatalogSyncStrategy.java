/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;

import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.webservices.JirafeOutboundClient;
import org.jirafe.webservices.JirafeOutboundClient.TransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Syncs {@code JirafeCatalogSyncDataModel} to the Jirafe endpoints via the http protocol.
 * 
 * @author Larry Ramponi
 * @author Dave Brand
 * 
 */
public class HttpCatalogSyncStrategy implements JirafeCatalogSyncStrategy
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpCatalogSyncStrategy.class);

	private JirafeOutboundClient jirafeOutboundClient;
	private ModelService modelService;

	private JirafeJsonConverter jirafeJsonConverter;
	private JirafeMappingsDao jirafeMappingsDao;

	/**
	 * @return
	 * 
	 */
	@Override
	public JirafeDataStatus sync(final ItemModel model)
	{
		TransactionResult result;
		final String itemType = model.getItemtype();

		if (LOG.isDebugEnabled())
		{
			LOG.debug("JirafeCatalogSyncData sync : attempting to sync type {}, pk={}", itemType, model.getPk());
		}
		try
		{
			final String mappedType = jirafeMappingsDao.getMappedType(model);
			if (mappedType == null)
			{
				LOG.debug("Skipping unmapped type {}", itemType);
				return JirafeDataStatus.ACCEPTED;
			}
			final String json = jirafeJsonConverter.toJson(new JirafeDataDto(mappedType, model, false));
			result = jirafeOutboundClient.putMessage(json, mappedType, false);
			switch (result.status)
			{
				case SUCCESS:
					if (LOG.isDebugEnabled())
					{
						LOG.debug("JirafeCatalogSyncJobPerformable sync : successfully sync'd item {}.", model.getPk());
					}
					return JirafeDataStatus.ACCEPTED;
				case FAILURE:
					if (LOG.isDebugEnabled())
					{
						LOG.debug("JirafeCatalogSyncJobPerformable sync : failed to sync item {}.", model.getPk());
					}
					return JirafeDataStatus.REJECTED;
				default:
					if (LOG.isDebugEnabled())
					{
						LOG.debug("JirafeCatalogSyncData sync : failed to sync item - NOT_AUTHORIZED");
					}
					return JirafeDataStatus.NOT_AUTHORIZED;
			}
		}
		catch (final Exception e)
		{
			LOG.error("Failed to process catalog sync due to : {}", e.toString());
			return JirafeDataStatus.REJECTED;
		}
	}

	/**
	 * @param jirafeOutboundClient
	 *           the jirafeOutboundClient to set
	 */
	public void setJirafeOutboundClient(final JirafeOutboundClient jirafeOutboundClient)
	{
		this.jirafeOutboundClient = jirafeOutboundClient;
	}

	/**
	 * @param modelService
	 *           the modelService to set
	 */
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	/**
	 * @param jirafeJsonConverter
	 *           the jirafeJsonConverter to set
	 */
	public void setJirafeJsonConverter(final JirafeJsonConverter jirafeJsonConverter)
	{
		this.jirafeJsonConverter = jirafeJsonConverter;
	}

	/**
	 * @param jirafeMappingsDao
	 *           the jirafeMappingsDao to set
	 */
	public void setJirafeMappingsDao(final JirafeMappingsDao jirafeMappingsDao)
	{
		this.jirafeMappingsDao = jirafeMappingsDao;
	}

}
