/**
 * 
 */
package org.jirafe.cronjob;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;

import org.jirafe.model.cronjob.JirafeHeartbeatCronJobModel;
import org.jirafe.webservices.JirafeHeartBeatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Cron job that reads data from the jirafedata type and uses a {@code JirafeDataSyncStrategy} to sync data.
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeHeartbeatJobPerformable extends AbstractJobPerformable<JirafeHeartbeatCronJobModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(JirafeHeartbeatJobPerformable.class);

	@Autowired
	private JirafeHeartBeatClient jirafeHeartBeatClient;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable#perform(de.hybris.platform.cronjob.model.CronJobModel
	 * )
	 */
	@Override
	public PerformResult perform(final JirafeHeartbeatCronJobModel arg0)
	{
		LOG.debug("Starting jirafe heartbeat ping.");

		jirafeHeartBeatClient.ping();

		LOG.debug("Finished jirafe heartbeat ping.");
		return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}



	/**
	 * @param jirafeHeartBeatClient
	 *           the jirafeHeartBeatClient to set
	 */
	public void setJirafeHeartBeatClient(final JirafeHeartBeatClient jirafeHeartBeatClient)
	{
		this.jirafeHeartBeatClient = jirafeHeartBeatClient;
	}
}
