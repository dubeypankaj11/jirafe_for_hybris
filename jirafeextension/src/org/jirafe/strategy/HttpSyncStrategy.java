/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.Config;

import java.util.List;

import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.webservices.JirafeOutboundClient;
import org.jirafe.webservices.JirafeOutboundClient.TransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Syncs {@code JirafeDataModel} to the Jirafe endpoints via the http protocol.
 * 
 * @author Larry Ramponi
 * 
 */
public class HttpSyncStrategy implements JirafeDataSyncStrategy
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpSyncStrategy.class);
	private static final String NOT_AUTHORIZED = "NOT_AUTHORIZED";

	private static final String MAX_RETRY = "jirafe.jirafeDataSync.authFailureLimit";

	private JirafeOutboundClient jirafeOutboundClient;
	private ModelService modelService;

	// following are injected via cronjob
	protected SessionService sessionService;
	protected FlexibleSearchService flexibleSearchService;

	/**
	 * 
	 */
	@Override
	public void sync(final List<JirafeDataModel> syncData)
	{
		TransactionResult result;
		int authFailureCount = 0;
		int successCount = 0;
		int failureCount = 0;

		LOG.info("JirafeData sync : starting syncing {} items", Integer.valueOf(syncData.size()));

		for (final JirafeDataModel model : syncData)
		{
			try
			{
				if (LOG.isDebugEnabled())
				{
					LOG.debug("JirafeData sync : attempting to sync type {}, pk={}", model.getType(), model.getPk());
				}
				result = jirafeOutboundClient.putMessage(model.getData(), model.getType(), model.getIsRemove());
				switch (result.status)
				{
					case SUCCESS:
						model.setStatus(JirafeDataStatus.ACCEPTED);
						successCount++;
						if (LOG.isDebugEnabled())
						{
							LOG.debug("JirafeData sync : successfully sync'd item.");
						}
						break;
					case FAILURE:
						model.setStatus(JirafeDataStatus.REJECTED);
						model.setErrors(result.errors.toString());
						failureCount++;
						if (LOG.isDebugEnabled())
						{
							LOG.debug("JirafeData sync : failed to sync item.");
						}
						break;
					default:
						model.setStatus(JirafeDataStatus.NOT_AUTHORIZED);
						model.setErrors(NOT_AUTHORIZED);
						authFailureCount++;
						if (LOG.isDebugEnabled())
						{
							LOG.debug("JirafeData sync : failed to sync item - NOT_AUTHORIZED");
						}
				}
				modelService.save(model);

			}
			catch (final Exception e)
			{
				LOG.error("JirafeData sync : exception occurred while syncing item.", e);
				failureCount++;
			}

			if (authFailureCount >= Config.getInt(MAX_RETRY, 5))
			{
				LOG.error("JirafeData sync : Cancelling processing, reached max auth failures ({})",
						Integer.valueOf(authFailureCount));
				throw new RuntimeException("JirafeData sync : Cancelling processing, reached max auth failures");
			}
		}

		LOG.info("JirafeData sync : completed syncing items. Success={}, Failure={}, AuthFailures={}", new Integer[]
		{ successCount, failureCount, authFailureCount });
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
	 * @param sessionService
	 *           the sessionService to set
	 */
	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	/**
	 * @param flexibleSearchService
	 *           the flexibleSearchService to set
	 */
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

}
