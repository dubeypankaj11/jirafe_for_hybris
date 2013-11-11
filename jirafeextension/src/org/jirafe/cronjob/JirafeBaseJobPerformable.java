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
	 */
	protected abstract void perform(List<JirafeDataModel> data);

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
	public PerformResult perform(final T cronJob)
	{
		int start;
		final int batchSize;
		List<JirafeDataModel> data;
		final FlexibleSearchQuery query;

		start = 0;
		batchSize = Config.getInt("jirafe.cronjob.batchSize", 10);

		query = new FlexibleSearchQuery(getQuery());

		// Allow extending class to set params.  
		// Do this here so its only set one time.
		setQueryParams(query);

		LOG.debug("Starting {} job, batchSize={}", jobName, batchSize);

		while ((data = getJirafeData(start, batchSize, query)) != null)
		{
			LOG.debug("Start performing batch, start={}, batchSize={}", start, batchSize);
			perform(data);
			LOG.debug("Finished performing batch, start={}, batchSize={}", start, batchSize);
			start = getStart(start, batchSize);
		}
		flush();

		LOG.debug("Finished {} job.", jobName);

		return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}

	/**
	 * 
	 */
	protected void flush()
	{
		// Do nothing by default, override if needed
	}

	/**
	 * Returns a list of jirafe data to be processed.
	 * 
	 * @return
	 */
	protected List<JirafeDataModel> getJirafeData(final int start, final int batchSize, final FlexibleSearchQuery query)
	{
		final SearchResult<JirafeDataModel> results;

		query.setStart(start);
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
	 * Returns the start of where to return results. Separating it out in a method allows an extending class to add
	 * custom logic.
	 * 
	 * @param start
	 * @return
	 */
	protected int getStart(final int start, final int batchSize)
	{
		return start + batchSize;
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
