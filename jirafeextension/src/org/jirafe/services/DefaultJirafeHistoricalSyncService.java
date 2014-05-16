/**
 * 
 */
package org.jirafe.services;

import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.internal.model.ServicelayerJobModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.util.Config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.model.cronjob.JirafeHistoricalSyncCronJobModel;
import org.jirafe.webservices.JirafeHistoricalStatusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @author dbrand
 * 
 */
@Component("jirafeHistoricalSyncService")
public class DefaultJirafeHistoricalSyncService implements JirafeHistoricalSyncService
{
	private final static Logger log = LoggerFactory.getLogger(DefaultJirafeHistoricalSyncService.class);

	@Resource
	ModelService modelService;
	@Resource
	UserService userService;
	@Resource
	CronJobService cronJobService;
	@Resource
	FlexibleSearchService flexibleSearchService;
	@Resource
	JirafeHistoricalStatusClient jirafeHistoricalStatusClient;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.services.JirafeHistoricalSyncService#request(java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public Map request(final String siteName, final String command, final Map args)
	{
		final JirafeHistoricalSyncCronJobModel jirafeHistoricalSyncCronJobModel0 = new JirafeHistoricalSyncCronJobModel();
		jirafeHistoricalSyncCronJobModel0.setSiteName(siteName);

		JirafeHistoricalSyncCronJobModel jirafeHistoricalSyncCronJobModel = null;
		try
		{
			jirafeHistoricalSyncCronJobModel = flexibleSearchService.getModelByExample(jirafeHistoricalSyncCronJobModel0);
		}
		catch (final ModelNotFoundException e)
		{
			// Leave it null
		}

		final Map map = new HashMap();
		String error = null;
		String message = null;
		map.put("site", siteName);
		log.debug("HistoricalSync... site: {}, command: {}", siteName, command);

		final String accountStatus = jirafeHistoricalStatusClient.getStatus(siteName);

		try
		{
			if (accountStatus == null)
			{
				error = "Failed to retrieve account status, check server log for details";
			}
			else if (command.equals("start"))
			{
				if (!accountStatus.equals("ready") && !accountStatus.equals("in-process"))
				{
					error = String.format("Cannot start historical sync, account status is: %s", accountStatus);
				}
				else if (jirafeHistoricalSyncCronJobModel == null)
				{
					final ServicelayerJobModel sjm = new ServicelayerJobModel();
					sjm.setSpringId("jirafeHistoricalSyncJob");
					final ServicelayerJobModel servicelayerJobModel = flexibleSearchService.getModelByExample(sjm);

					final UserModel jirafeuser = userService.getUserForUID("jirafeuser");

					jirafeHistoricalSyncCronJobModel = modelService.create(JirafeHistoricalSyncCronJobModel.class);
					jirafeHistoricalSyncCronJobModel.setJob(servicelayerJobModel);
					jirafeHistoricalSyncCronJobModel.setSiteName(siteName);
					jirafeHistoricalSyncCronJobModel.setSessionUser(jirafeuser);
					jirafeHistoricalSyncCronJobModel.setSessionLanguage(jirafeuser.getSessionLanguage());
					jirafeHistoricalSyncCronJobModel.setSessionCurrency(jirafeuser.getSessionCurrency());
					final String types = Config.getString("jirafe.jirafeHistoricalSync.order",
							"Category,Product,Customer,Order,Employee");
					jirafeHistoricalSyncCronJobModel.setTypes(types);
					modelService.save(jirafeHistoricalSyncCronJobModel);
				}
				else
				{
					final CronJobStatus status = jirafeHistoricalSyncCronJobModel.getStatus();
					switch (status)
					{
						case RUNNING:
						case RUNNINGRESTART:
							error = String.format("Historical sync already running");
							break;
					}
				}
				if (error == null)
				{
					cronJobService.performCronJob(jirafeHistoricalSyncCronJobModel);
					message = String.format("Historical sync started for site '%s', types='%s'", siteName,
							jirafeHistoricalSyncCronJobModel.getTypes());
				}
			}
			else if (command.equals("status"))
			{
				// ok
			}
			else if (command.equals("stop"))
			{
				if (jirafeHistoricalSyncCronJobModel == null)
				{
					error = String.format("Historical sync not scheduled, nothing to stop");
				}
				else
				{
					cronJobService.requestAbortCronJob(jirafeHistoricalSyncCronJobModel);
					message = String.format("Historical sync terminate request submitted for site '%s'", siteName);
				}
			}
			else
			{
				error = String.format("Invalid command: %s", command);
			}
		}
		catch (final Exception e)
		{
			error = e.getLocalizedMessage();
		}

		if (error == null)
		{
			map.put("success", true);
			log.info(message);
			map.put("message", message);
			map.put("status", jirafeHistoricalSyncCronJobModel != null ? jirafeHistoricalSyncCronJobModel.getStatus().toString()
					: accountStatus);
		}
		else
		{
			map.put("success", false);
			log.error(error);
			map.put("error", error);
		}
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.services.JirafeHistoricalSyncService#start(java.lang.String, java.util.Map)
	 */
	@Override
	public Map start(final String siteName, final Map args)
	{
		return request(siteName, "start", args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.services.JirafeHistoricalSyncService#stop(java.lang.String, java.util.Map)
	 */
	@Override
	public Map stop(final String siteName, final Map args)
	{
		return request(siteName, "stop", args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.services.JirafeHistoricalSyncService#status(java.lang.String, java.util.Map)
	 */
	@Override
	public Map status(final String siteName, final Map args)
	{
		return request(siteName, "status", args);
	}

}
