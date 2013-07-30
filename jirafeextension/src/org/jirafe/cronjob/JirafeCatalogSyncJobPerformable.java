/**
 * 
 */
package org.jirafe.cronjob;

import de.hybris.platform.catalog.model.ItemSyncTimestampModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.util.Config;

import java.util.Date;
import java.util.List;

import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.cronjob.JirafeCatalogSyncCronJobModel;
import org.jirafe.model.data.JirafeCatalogSyncDataModel;
import org.jirafe.strategy.JirafeCatalogSyncStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cron job that reads data from the jirafe data type and uses a {@code CatalogSyncCronJobSyncStrategy} to sync data.
 * 
 * @author Larry Ramponi
 * @author Dave Brand
 * 
 */
public class JirafeCatalogSyncJobPerformable extends AbstractJobPerformable<JirafeCatalogSyncCronJobModel>
{
	protected static final Logger LOG = LoggerFactory.getLogger(JirafeCatalogSyncJobPerformable.class);

	protected FlexibleSearchService flexibleSearchService;
	private static final String MAX_RETRY = "jirafe.jirafeDataSync.authFailureLimit";

	private static final String headerQuery = "SELECT {" + JirafeCatalogSyncDataModel.PK + "}, {"
			+ JirafeCatalogSyncDataModel.LASTMODIFIED + "}, {" + JirafeCatalogSyncDataModel.LASTPK + "} FROM {"
			+ JirafeCatalogSyncDataModel._TYPECODE + "}";

	private static final String dataQuery = "SELECT {" + ItemSyncTimestampModel.PK + "},{" + ItemSyncTimestampModel.TARGETITEM
			+ "} FROM {" + ItemSyncTimestampModel._TYPECODE + "} WHERE ({" + ItemModel.MODIFIEDTIME + "} > ?lastModified OR {"
			+ ItemModel.MODIFIEDTIME + "} = ?lastModified AND {" + ItemModel.PK + "} > ?lastPK) ORDER BY {" + ItemModel.MODIFIEDTIME
			+ "} ASC, {" + ItemModel.PK + "} ASC";

	private JirafeCatalogSyncStrategy syncStrategy;

	/**
	 * @param syncStrategy
	 *           the syncStrategy to set
	 */
	public void setJirafeCatalogSyncStrategy(final JirafeCatalogSyncStrategy syncStrategy)
	{
		this.syncStrategy = syncStrategy;
	}

	/**
	 * Perform method of the {@code AbstractJobPerformable}. Iterates over the jirafe data and passes the list into the
	 * {@code perform} method of this class.
	 */
	@Override
	public PerformResult perform(final JirafeCatalogSyncCronJobModel cronJob)
	{
		int start;
		final int batchSize;
		List<ItemSyncTimestampModel> data;
		final FlexibleSearchQuery query;

		int successCount = 0;
		int failureCount = 0;
		int authFailureCount = 0;

		final int maxRetry = Config.getInt(MAX_RETRY, 5);

		start = 0;
		batchSize = Config.getInt("jirafe.cronjob.batchSize", 10);

		final List<Object> result = flexibleSearchService.search(
				new FlexibleSearchQuery(JirafeCatalogSyncJobPerformable.headerQuery)).getResult();
		final JirafeCatalogSyncDataModel header;
		if (result != null && result.size() > 0)
		{
			header = (JirafeCatalogSyncDataModel) result.get(0);
		}
		else
		{
			header = new JirafeCatalogSyncDataModel();
			header.setLastModified(new Date());
			header.setLastPK(PK.BIG_PK);
		}
		Date lastModified = header.getLastModified();
		PK lastPK = header.getLastPK();

		query = new FlexibleSearchQuery(JirafeCatalogSyncJobPerformable.dataQuery);

		query.setCount(batchSize);

		LOG.debug("Starting JirafeCatalogSyncCronJob job, batchSize={}", batchSize);

		boolean abort = false;
		while ((data = getCatalogSyncData(query, lastModified, lastPK)) != null)
		{
			LOG.debug("Start performing batch, start={}/{}", lastModified, lastPK);
			for (final ItemSyncTimestampModel item : data)
			{
				final ItemModel itemModel = item.getTargetItem();
				int authFails = 0;
				for (;;)
				{
					final JirafeDataStatus status = syncStrategy.sync(itemModel);
					switch (status)
					{
						case ACCEPTED:
							successCount++;
							break;
						case REJECTED:
							failureCount++;
							break;
						case NOT_AUTHORIZED:
							authFailureCount++;
							if (++authFails < maxRetry)
							{
								continue;
							}
							abort = true;
							break;
					}
					break;
				}
				if (abort)
				{
					break;
				}
				lastModified = item.getModifiedtime();
				lastPK = item.getPk();
			}
			if (abort)
			{
				break;
			}
		}
		header.setStatus(abort ? JirafeDataStatus.NOT_AUTHORIZED : JirafeDataStatus.ACCEPTED);
		header.setLastModified(lastModified);
		header.setLastPK(lastPK);
		modelService.save(header);

		LOG.info("JirafeCatalogSyncData sync : completed syncing items. Success={}, Failure={}, AuthFailures={}", new Integer[]
		{ successCount, failureCount, authFailureCount });
		if (abort)
		{
			LOG.error("JirafeCatalogSyncData sync : Cancelling processing, reached max auth failures ({})",
					Integer.valueOf(authFailureCount));
			return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
		}
		LOG.debug("Finished performing batch, start={}, batchSize={}", start, batchSize);

		LOG.debug("Finished JirafeCatalogSyncCronJob job.");

		return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}

	/**
	 * Returns a list of jirafe data to be processed.
	 * 
	 * @return
	 */
	protected List<ItemSyncTimestampModel> getCatalogSyncData(final FlexibleSearchQuery query, final Date lastModified,
			final PK lastPK)
	{
		final SearchResult<ItemSyncTimestampModel> results;

		query.addQueryParameter("lastModified", lastModified);
		query.addQueryParameter("lastPK", lastPK);
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
