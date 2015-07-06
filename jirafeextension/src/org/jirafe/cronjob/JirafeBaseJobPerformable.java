/**
 *
 */
package org.jirafe.cronjob;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.util.Config;

import java.util.List;

import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.strategy.JirafeDataSyncStrategy.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cron job that reads data from the jirafe data type and uses a {@code JirafeDataSyncStrategy} to sync data.
 *
 * @author Larry Ramponi
 *
 */
public abstract class JirafeBaseJobPerformable<T extends CronJobModel> extends AbstractJobPerformable<T>
{
	protected static final Logger LOG = LoggerFactory.getLogger(JirafeBaseJobPerformable.class);

	protected String jobName;
	protected FlexibleSearchService flexibleSearchService;

	/**
	 * Constructor that sets the job name.
	 *
	 * @param jobName
	 */
	protected JirafeBaseJobPerformable(final String jobName)
	{
		this.jobName = jobName;
	}

	/**
	 * Abstract method for job to implement for specific functionality.
	 *
	 * @param data
	 * @throws AuthenticationException
	 */
	protected abstract void perform(List<JirafeDataModel> data) throws AuthenticationException;

	/**
	 * Returns the query used to get the jirafe data.
	 *
	 * @return
	 */
	protected abstract String getQuery();

	/**
	 * Method that allows concrete class to set query params before it is executed.
	 *
	 * @param query
	 */
	protected abstract void setQueryParams(FlexibleSearchQuery query);

	/**
	 * Perform method of the {@code AbstractJobPerformable}. Iterates over the jirafe data and passes the list into the
	 * {@code perform} method of this class.
	 */
	@Override
	public synchronized PerformResult perform(final T cronJob)
	{
		final int batchSize;
		List<JirafeDataModel> data;
		final FlexibleSearchQuery query;

		batchSize = Config.getInt("jirafe.cronjob.batchSize", 10);

		query = new FlexibleSearchQuery(getQuery());

		// Allow extending class to set params.
		// Do this here so its only set one time.
		setQueryParams(query);

		LOG.debug("Starting {} job, batchSize={}", jobName, batchSize);

		CronJobResult result = CronJobResult.SUCCESS;
		try
		{
			while ((data = getJirafeData(batchSize, query)) != null)
			{
				LOG.debug("Start performing batch, batchSize={}", batchSize);
				perform(data);
				LOG.debug("Finished performing batch, batchSize={}", batchSize);
			}
			flush();
		}
		catch (final AuthenticationException e)
		{
			result = CronJobResult.FAILURE;
			LOG.warn("Authentication or communication failure, will retry later", e);
		}
		catch (final Exception e)
		{
			result = CronJobResult.ERROR;
			LOG.error("Exception caught, aborting job", e);
		}

		LOG.debug("Finished {} job, result = {}.", jobName, result);

		return new PerformResult(result, CronJobStatus.FINISHED);
	}

	/**
	 * @throws AuthenticationException
	 *
	 */
	protected void flush() throws AuthenticationException
	{
		// Do nothing by default, override if needed
	}

	/**
	 * Returns a list of jirafe data to be processed.
	 *
	 * @return
	 */
	protected List<JirafeDataModel> getJirafeData(final int batchSize, final FlexibleSearchQuery query)
	{
		final SearchResult<JirafeDataModel> results;

		query.setCount(batchSize);

		results = flexibleSearchService.search(query);

		if (results == null || results.getCount() <= 0)
		{
			return null;
		}
		else
		{
			return results.getResult();
		}
	}

	/**
	 *
	 */
	@Override
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

}
