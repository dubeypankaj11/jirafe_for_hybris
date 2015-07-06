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

import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dao.JirafeSyncDataDao;
import org.jirafe.dto.JirafeTempDataModelFactory;
import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.cronjob.JirafeCatalogSyncCronJobModel;
import org.jirafe.model.data.JirafeCatalogSyncDataModel;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.strategy.JirafeDataSyncStrategy;
import org.jirafe.strategy.JirafeDataSyncStrategy.AuthenticationException;
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
	protected JirafeTempDataModelFactory jirafeTempDataModelFactory;
	@Resource
	protected JirafeSyncDataDao jirafeSyncDataDao;
	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	private static final String dataQuery = //
	"SELECT {" + ItemSyncTimestampModel.PK + "},{" + ItemSyncTimestampModel.TARGETITEM + "} " + //
			"FROM {" + ItemSyncTimestampModel._TYPECODE + "} " + //
			"WHERE {" + ItemSyncTimestampModel.TARGETITEM + "} IS NOT NULL " + //
			"AND  ({" + ItemModel.MODIFIEDTIME + "} > ?lastModified " + //
			" OR   {" + ItemModel.MODIFIEDTIME + "} = ?lastModified " + //
			"  AND {" + ItemModel.PK + "} > ?lastPK) " + //
			"ORDER BY {" + ItemModel.MODIFIEDTIME + "} ASC, {" + ItemModel.PK + "} ASC";

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
		final int batchSize;
		List<ItemSyncTimestampModel> data;
		final FlexibleSearchQuery query;

		batchSize = Config.getInt("jirafe.cronjob.batchSize", 10);

		final JirafeCatalogSyncDataModel header = jirafeSyncDataDao.get();

		query = new FlexibleSearchQuery(JirafeCatalogSyncJobPerformable.dataQuery);
		query.setCount(batchSize);
		LOG.debug("Starting JirafeCatalogSyncCronJob job, batchSize={}", batchSize);

		Date lastModified = header.getLastModified();
		PK lastPK = header.getLastPK();
		JirafeDataStatus exitStatus = JirafeDataStatus.ACCEPTED;
		try
		{
			while ((data = getCatalogSyncData(query, lastModified, lastPK)) != null)
			{
				LOG.debug("Start performing batch, start={}/{}", lastModified, lastPK);
				for (final ItemSyncTimestampModel item : data)
				{
					lastModified = item.getModifiedtime();
					lastPK = item.getPk();
					final ItemModel itemModel = item.getTargetItem();
					final String itemType = itemModel.getItemtype();
					final String mappedType = jirafeMappingsDao.getMappedType(itemModel);
					if (mappedType == null)
					{
						LOG.debug("Skipping unmapped type {}", itemType);
						continue;
					}
					if (!jirafeMappingsDao.filter(mappedType, itemModel))
					{
						LOG.debug("Ignoring {} (filtered)", itemModel);
						continue;
					}
					final List<JirafeDataModel> jirafeDataModels = jirafeTempDataModelFactory.fromItemModel(itemModel);
					if (jirafeDataModels != null)
					{
						syncStrategy.sync(jirafeDataModels);
					}
				}
			}
			syncStrategy.flush();
		}
		catch (final AuthenticationException e)
		{
			exitStatus = JirafeDataStatus.NOT_AUTHORIZED;
		}
		catch (final Exception e)
		{
			exitStatus = JirafeDataStatus.REJECTED;
		}

		header.setStatus(exitStatus);
		header.setLastModified(lastModified);
		header.setLastPK(lastPK);
		modelService.save(header);

		LOG.debug("Finished JirafeCatalogSyncCronJob job, exit status = {}.", exitStatus);

		return new PerformResult(exitStatus == JirafeDataStatus.ACCEPTED ? CronJobResult.SUCCESS : CronJobResult.FAILURE,
				CronJobStatus.FINISHED);
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
