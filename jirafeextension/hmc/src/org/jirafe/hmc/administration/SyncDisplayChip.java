/**
 * 
 */
package org.jirafe.hmc.administration;

import de.hybris.platform.core.Registry;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jirafe.services.JirafeHistoricalSyncService;


/**
 * @author dbrand
 * 
 */
public class SyncDisplayChip extends DisplayChip
{
	private static final Logger LOG = Logger.getLogger(SyncDisplayChip.class.getName());

	/**
	 * @param displayState
	 * @param parent
	 */
	public SyncDisplayChip(final DisplayState displayState, final Chip parent)
	{
		super(displayState, parent, "sync");
	}

	public final String REFRESH = "REFRESH";
	public final String START_SYNC = "START_SYNC";
	public final String STOP_SYNC = "STOP_SYNC";
	private final JirafeHistoricalSyncService historicalSyncService = (JirafeHistoricalSyncService) Registry
			.getApplicationContext().getBean("jirafeHistoricalSyncService");

	public String getHistoricalSyncStatus(final String siteName)
	{
		try
		{
			return (String) historicalSyncService.status(siteName, null).get("status");
		}
		catch (final Exception e)
		{
			return e.getMessage();
		}
	}

	@Override
	public void processEvents(final Map<String, List<String>> events)
	{
		super.processEvents(events);
		Map<String, Object> result;
		if (events.containsKey(START_SYNC))
		{
			result = historicalSyncService.start(events.get(START_SYNC).get(0), null);
		}
		else if (events.containsKey(STOP_SYNC))
		{
			result = historicalSyncService.stop(events.get(STOP_SYNC).get(0), null);
		}
		else
		{
			return;
		}
		if ((Boolean) result.get("success"))
		{
			postInfoMessage("Request processed successfully.");
		}
		else
		{
			postErrorMessage((String) result.get("error"));
		}
	}
}
