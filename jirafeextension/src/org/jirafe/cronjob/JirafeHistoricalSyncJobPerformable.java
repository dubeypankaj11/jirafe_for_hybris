/**
 *
 */
package org.jirafe.cronjob;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.util.Config;

import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dao.JirafePagerDao;
import org.jirafe.dao.JirafePagerDao.BadArgument;
import org.jirafe.dto.JirafeTempDataModelFactory;
import org.jirafe.model.cronjob.JirafeHistoricalSyncCronJobModel;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.strategy.JirafeDataSyncStrategy;
import org.jirafe.strategy.JirafeDataSyncStrategy.AuthenticationException;
import org.jirafe.webservices.JirafeHistoricalStatusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author dbrand
 *
 */
public class JirafeHistoricalSyncJobPerformable extends AbstractJobPerformable<JirafeHistoricalSyncCronJobModel>
{
	private final static Logger log = LoggerFactory.getLogger(JirafeHistoricalSyncJobPerformable.class);

	@Resource(name = "jirafeHistoricalSyncStrategy")
	protected JirafeDataSyncStrategy jirafeHistoricalSyncStrategy;
	@Resource
	JirafePagerDao jirafePagerDao;
	@Resource
	protected JirafeTempDataModelFactory jirafeTempDataModelFactory;
	@Resource
	JirafeHistoricalStatusClient jirafeHistoricalStatusClient;
	@Resource
	private JirafeMappingsDao jirafeMappingsDao;
	@Resource
	private JirafeJsonConverter jirafeJsonConverter;

	protected int pageLimit = Config.getInt("jirafe.cronjob.batchSize", 100);

	@Override
	public boolean isAbortable()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable#perform(de.hybris.platform.cronjob.model.CronJobModel
	 * )
	 */
	@Override
	public PerformResult perform(final JirafeHistoricalSyncCronJobModel jirafeHistoricalSyncCronJobModel)
	{
		final String siteName = jirafeHistoricalSyncCronJobModel.getSiteName();
		String pageToken = jirafeHistoricalSyncCronJobModel.getPageToken();
		String types = jirafeHistoricalSyncCronJobModel.getTypes();

		log.debug(String.format("Historical sync: siteName=%s, pageToken=%s, types=%s", //
				siteName, pageToken, types));

		try
		{
			final String status = jirafeHistoricalStatusClient.getStatus(siteName);
			if (!"in-process".equals(status))
			{
				if (!"ready".equals(status))
				{
					log.error("Account status changed, aborting.");
					return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
				}
				jirafeHistoricalStatusClient.putStatus(siteName, "in-process");
			}

			while (!StringUtils.isEmpty(types))
			{
				if (clearAbortRequestedIfNeeded(jirafeHistoricalSyncCronJobModel))
				{
					log.error("Aborted by request.");
					return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
				}

				final String type = StringUtils.substringBefore(types, ",");

				ItemModel lastResult = null;
				final FlexibleSearchQuery query;
				try
				{
					query = jirafePagerDao.buildQuery(type, null, null, pageLimit, pageToken);
				}
				catch (final BadArgument e)
				{
					throw new IllegalStateException(e);
				}
				final SearchResult results = flexibleSearchService.search(query);
				if (results.getCount() > 0)
				{
					for (final ItemModel itemModel : (List<ItemModel>) results.getResult())
					{
						lastResult = itemModel;
						if (!jirafeMappingsDao.filter(type, itemModel))
						{
							log.debug("Ignoring {} (filtered)", itemModel);
							continue;
						}
						if (StringUtils.indexOfAny(siteName, jirafeJsonConverter.getSites(itemModel)) < 0)
						{
							log.debug("Skipping {} (wrong site)", itemModel);
							continue;
						}
						log.debug("Syncing {}", itemModel);
						final JirafeDataModel jirafeDataModel = jirafeTempDataModelFactory.fromItemModel(type, itemModel, siteName);
						jirafeHistoricalSyncStrategy.sync(Collections.singletonList(jirafeDataModel));
					}
					jirafeHistoricalSyncStrategy.flush();
				}
				pageToken = jirafePagerDao.buildPageToken(results, lastResult);
				jirafeHistoricalSyncCronJobModel.setPageToken(pageToken);
				if (pageToken == null)
				{
					types = StringUtils.substringAfter(types, ",");
					jirafeHistoricalSyncCronJobModel.setTypes(types);
				}
				modelService.save(jirafeHistoricalSyncCronJobModel);
			}
			jirafeHistoricalStatusClient.putStatus(siteName, "complete");
			return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
		}
		catch (final AuthenticationException e)
		{
			log.error("Authentication or communication failure during historical sync, will retry later", e);
			return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
		}
		catch (final Exception e)
		{
			log.error("Exception caught, aborting sync", e);
			return new PerformResult(CronJobResult.ERROR, CronJobStatus.ABORTED);
		}
	}

	public String resumeConditionString(final JirafeHistoricalSyncCronJobModel jirafeHistoricalSyncCronJobModel)
	{
		return String.format("JirafeHistoricalSync-%s", jirafeHistoricalSyncCronJobModel.getSiteName());
	}

}
