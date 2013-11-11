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

import javax.annotation.Resource;

import org.jirafe.dto.JirafeCatalogDataModelFactory;
import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.cronjob.JirafeCatalogSyncCronJobModel;
import org.jirafe.model.data.JirafeCatalogSyncDataModel;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.strategy.JirafeDataSyncStrategy;
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

	@Resource
	protected FlexibleSearchService flexibleSearchService;
	@Resource
	protected JirafeCatalogDataModelFactory jirafeCatalogDataModelFactory;

	private static final String MAX_RETRY = "jirafe.jirafeDataSync.authFailureLimit";

	private static final String headerQuery = "SELECT {" + JirafeCatalogSyncDataModel.PK + "}, {"
			+ JirafeCatalogSyncDataModel.LASTMODIFIED + "}, {" + JirafeCatalogSyncDataModel.LASTPK + "} FROM {"
			+ JirafeCatalogSyncDataModel._TYPECODE + "}";

	private static final String dataQuery = "SELECT {" + ItemSyncTimestampModel.PK + "},{" + ItemSyncTimestampModel.TARGETITEM
			+ "} FROM {" + ItemSyncTimestampModel._TYPECODE + "} WHERE ({" + ItemModel.MODIFIEDTIME + "} > ?lastModified OR {"
			+ ItemModel.MODIFIEDTIME + "} = ?lastModified AND {" + ItemModel.PK + "} > ?lastPK) ORDER BY {" + ItemModel.MODIFIEDTIME
			+ "} ASC, {" + ItemModel.PK + "} ASC";

	private JirafeDataSyncStrategy syncStrategy;

	/**
	 * @param syncStrategy
	 *           the syncStrategy to set
	 */
	public void setJirafeDataSyncStrategy(final JirafeDataSyncStrategy syncStrategy)
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

		query = new FlexibleSearchQuery(JirafeCatalogSyncJobPerformable.dataQuery);
		query.setCount(batchSize);
		LOG.debug("Starting JirafeCatalogSyncCronJob job, batchSize={}", batchSize);

		Date lastModified = header.getLastModified();
		PK lastPK = header.getLastPK();
		while ((data = getCatalogSyncData(query, lastModified, lastPK)) != null)
		{
			LOG.debug("Start performing batch, start={}/{}", lastModified, lastPK);
			for (final ItemSyncTimestampModel item : data)
			{
				final ItemModel itemModel = item.getTargetItem();
				final List<JirafeDataModel> jirafeDataModels = jirafeCatalogDataModelFactory.fromItemModel(header, itemModel);
				if (jirafeDataModels != null)
				{
					syncStrategy.sync(jirafeDataModels);
				}
				lastModified = item.getModifiedtime();
				lastPK = item.getPk();
			}
		}
		syncStrategy.flush();

		header.setStatus(JirafeDataStatus.ACCEPTED);
		header.setLastModified(lastModified);
		header.setLastPK(lastPK);
		modelService.save(header);

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

}
