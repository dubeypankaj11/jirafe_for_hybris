/**
 * 
 */
package org.jirafe.strategy;

import java.util.List;

import org.jirafe.model.data.JirafeDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Useful for local development or anywhere it isn't necessary to sync data, but view what is happening in logs. Will
 * only log out data and not actually send to Jirafe.
 * 
 * @author Larry Ramponi
 * 
 */
public class LoggingSyncStrategy implements JirafeDataSyncStrategy
{
	private static final Logger log = LoggerFactory.getLogger(LoggingSyncStrategy.class);

	/**
	 * Iterates each {@code JirafeDataModel} and logs out details.
	 */
	@Override
	public void sync(final List<JirafeDataModel> syncData)
	{
		log.debug("JirafeData sync : start");
		for (final JirafeDataModel jirafeDataModel : syncData)
		{
			log.debug("JirafeData : pk={}, typePk={}, type={}", new Object[]
			{ jirafeDataModel.getPk(), jirafeDataModel.getTypePK(), jirafeDataModel.getType() });
		}
		log.debug("JirafeData sync : complete");
	}

	@Override
	public void flush()
	{
		log.debug("JirafeData sync : flush");
	}

}
