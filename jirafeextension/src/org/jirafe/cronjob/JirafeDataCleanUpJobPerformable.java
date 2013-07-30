/**
 * 
 */
package org.jirafe.cronjob;

import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.util.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.cronjob.JirafeDataCleanUpCronJobModel;
import org.jirafe.model.data.JirafeDataModel;


/**
 * Cron job for cleaning up the jirafedata table.
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeDataCleanUpJobPerformable extends JirafeBaseJobPerformable<JirafeDataCleanUpCronJobModel>
{
	private static final String cleanUpJobQuery = "SELECT {" + JirafeDataModel.PK + "} FROM {" + JirafeDataModel._TYPECODE
			+ "} WHERE {" + JirafeDataModel.STATUS + "} IN (?statuses)";
	private static final String jobName = "jirafeDataCleanUpJob";
	private static final String cleanUpStatusesProp = "jirafe.cronjob.cleanUp.statuses";

	private ModelService modelService;

	/**
	 * @param jobName
	 */
	protected JirafeDataCleanUpJobPerformable()
	{
		super(JirafeDataCleanUpJobPerformable.jobName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.cronjob.JirafeBaseJobPerformable#perform(java.util.List)
	 */
	@Override
	protected void perform(final List<JirafeDataModel> data)
	{
		LOG.info("About to remove {} {} jirafe data items.", data.size(), Config.getString(cleanUpStatusesProp, "ACCEPTED"));
		modelService.removeAll(data);
		LOG.info("Removed {} {} jirafe data items.", data.size(), Config.getString(cleanUpStatusesProp, "ACCEPTED"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.cronjob.JirafeBaseJobPerformable#getQuery()
	 */
	@Override
	protected String getQuery()
	{
		return JirafeDataCleanUpJobPerformable.cleanUpJobQuery;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jirafe.cronjob.JirafeBaseJobPerformable#setQueryParams(de.hybris.platform.servicelayer.search.FlexibleSearchQuery
	 * )
	 */
	@Override
	protected void setQueryParams(final FlexibleSearchQuery query)
	{
		final Set<JirafeDataStatus> dataStatuses;
		final String[] propStatuses;

		propStatuses = Config.getString(cleanUpStatusesProp, "ACCEPTED").split(",");
		dataStatuses = new HashSet<JirafeDataStatus>(propStatuses.length);

		for (final String status : propStatuses)
		{
			dataStatuses.add(JirafeDataStatus.valueOf(status));
		}

		query.addQueryParameter("statuses", dataStatuses);
	}

	/**
	 * Returns 0 in this case, since we should always start at 0 since records are getting removed.
	 */
	@Override
	protected int getStart(final int start, final int batchSize)
	{
		return 0;
	}

	/**
	 * 
	 */
	@Override
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

}
