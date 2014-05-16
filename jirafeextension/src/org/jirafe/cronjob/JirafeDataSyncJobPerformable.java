/**
 * 
 */
package org.jirafe.cronjob;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.util.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.cronjob.JirafeDataSyncCronJobModel;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.strategy.JirafeDataSyncStrategy;
import org.jirafe.strategy.JirafeDataSyncStrategy.AuthenticationException;


/**
 * Cron job that reads data from the jirafedata type and uses a {@code JirafeDataSyncStrategy} to sync data.
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeDataSyncJobPerformable extends JirafeBaseJobPerformable<JirafeDataSyncCronJobModel>
{

	private static final String jobName = "jirafeDataSyncJob";
	private static final String begQuery = //
	"SELECT {" + JirafeDataModel.PK + "},{" + JirafeDataModel.TYPE + "},{" + JirafeDataModel.DATA + "} " + //
			"FROM {" + JirafeDataModel._TYPECODE + "} " + //
			"WHERE {" + JirafeDataModel.STATUS + "} IN (?status) ORDER BY ";
	private static final String endQuery = "{" + JirafeDataModel.TIMESTAMP + "} ASC";

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
	protected void perform(final List<JirafeDataModel> data) throws AuthenticationException
	{
		syncStrategy.sync(data);
		// Need to sync each batch since the query depends on updated status
		syncStrategy.flush();
	}

	@Override
	protected void flush() throws AuthenticationException
	{
		syncStrategy.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.cronjob.JirafeBaseJobPerformable#getQuery()
	 */
	@Override
	protected String getQuery()
	{
		final StringBuilder orderBy = new StringBuilder();
		final String order = Config.getString("jirafe.jirafeDataSync.order", "Order,Cart,Employee,Customer,Category,Product");
		int idx = 0;
		if (!StringUtils.isEmpty(order))
		{
			orderBy.append("(CASE {" + JirafeDataModel.TYPE + "} ");
			for (final String type : StringUtils.split(order, ','))
			{
				orderBy.append(String.format("WHEN '%s' THEN %d ", type, ++idx));
			}
			orderBy.append(String.format("ELSE %d END) ASC, ", ++idx));
		}
		return begQuery + orderBy.toString() + endQuery;
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
