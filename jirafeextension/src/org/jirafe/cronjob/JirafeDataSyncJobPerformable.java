/**
 * 
 */
package org.jirafe.cronjob;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.cronjob.JirafeDataSyncCronJobModel;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.strategy.JirafeDataSyncStrategy;


/**
 * Cron job that reads data from the jirafedata type and uses a {@code JirafeDataSyncStrategy} to sync data.
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeDataSyncJobPerformable extends JirafeBaseJobPerformable<JirafeDataSyncCronJobModel>
{

	private static final String jobName = "jirafeDataSyncJob";
	private static final String query = "SELECT {" + JirafeDataModel.PK + "},{" + JirafeDataModel.TYPE + "},{"
			+ JirafeDataModel.DATA + "} FROM {" + JirafeDataModel._TYPECODE + "} WHERE {" + JirafeDataModel.STATUS
			+ "} IN (?status) ORDER BY {" + JirafeDataModel.TIMESTAMP + "} ASC";

	private JirafeDataSyncStrategy syncStrategy;

	/**
	 * @param jobName
	 */
	protected JirafeDataSyncJobPerformable()
	{
		super(JirafeDataSyncJobPerformable.jobName);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.cronjob.JirafeBaseJobPerformable#perform(java.util.List)
	 */
	@Override
	protected void perform(final List<JirafeDataModel> data)
	{
		syncStrategy.sync(data);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.cronjob.JirafeBaseJobPerformable#getQuery()
	 */
	@Override
	protected String getQuery()
	{
		return JirafeDataSyncJobPerformable.query;
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
		final Set includedTypes = new HashSet();
		includedTypes.add(JirafeDataStatus.valueOf("NEW"));
		includedTypes.add(JirafeDataStatus.valueOf("NOT_AUTHORIZED"));

		query.addQueryParameter("status", includedTypes);
	}

	/**
	 * 
	 * @param syncStrategy
	 */
	public void setJirafeDataSyncStrategy(final JirafeDataSyncStrategy syncStrategy)
	{
		this.syncStrategy = syncStrategy;
	}
}
