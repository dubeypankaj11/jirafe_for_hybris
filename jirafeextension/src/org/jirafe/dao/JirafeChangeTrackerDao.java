/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.Tenant;
import de.hybris.platform.persistence.property.PersistenceManager;
import de.hybris.platform.persistence.property.TypeInfoMap;
import de.hybris.platform.persistence.property.TypeInfoMap.PropertyColumnInfo;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.tx.Transaction;
import de.hybris.platform.util.Config;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jirafe.model.data.JirafeChangeTrackerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author dbrand
 * 
 */
public class JirafeChangeTrackerDao
{
	private static final Logger log = LoggerFactory.getLogger(JirafeChangeTrackerDao.class);

	final PK containerPK;
	final ModelService modelService;
	final FlexibleSearchService flexibleSearchService;

	/**
	 * @param containerPK
	 */
	public JirafeChangeTrackerDao(final PK containerPK)
	{
		super();
		this.containerPK = containerPK;
		modelService = (ModelService) Registry.getApplicationContext().getBean("modelService");
		flexibleSearchService = (FlexibleSearchService) Registry.getApplicationContext().getBean("flexibleSearchService");
	}

	public void save(final PK entryPK, final String attribute, final Object value)
	{
		final JirafeChangeTrackerModel jirafeChangeTrackerModel = new JirafeChangeTrackerModel();
		jirafeChangeTrackerModel.setContainerPK(containerPK);
		jirafeChangeTrackerModel.setEntryPK(entryPK);
		jirafeChangeTrackerModel.setAttribute(attribute);
		jirafeChangeTrackerModel.setValue(value);
		final Transaction tx = Transaction.current();
		final boolean wasRollbackOnly = tx.isRollbackOnly();
		// Number of times to retry (mostly for deadlocks)
		final int retry0 = Config.getInt("jirafe.deadlock.count", 20);
		int retry = retry0;
		for (;;)
		{
			try
			{
				modelService.save(jirafeChangeTrackerModel);
			}
			catch (final ModelSavingException e)
			{
				// Detach the model that failed so it doesn't get saved later
				modelService.detach(jirafeChangeTrackerModel);
				// Ignore the duplicate key exceptions - we only want the oldest one
				if (!wasRollbackOnly && tx.isRollbackOnly())
				{
					// Transaction has been marked readonly (for no good reason), try to fix it
					try
					{
						final Method m = tx.getClass().getMethod("clearRollbackOnly");
						m.invoke(tx);
						log.debug("Transaction rollback has been successfully cleared");
					}
					catch (final Exception e1)
					{
						log.error("Rolled back due to ModelSavingException and {} has no clearRollbackOnly() to fix it", //
								tx.getClass().getName());
						log.error("", e1);
					}
				}
				if (e.getMessage().toLowerCase().contains("deadlock"))
				{
					// Retry a few times if it was a deadlock
					if (--retry >= 0)
					{
						try
						{
							Thread.sleep(Config.getLong("jirafe.deadlock.interval", 50L));
						}
						catch (final InterruptedException e1)
						{
							// the wait is optional anyway
						}
						continue;
					}
					log.debug("", e);
				}
			}
			break;
		}
		if (retry != retry0)
		{
			log.debug("{} retries left", retry);
		}
	}

	private String createDeleteString(final int howMany)
	{
		final PersistenceManager persistenceManager = Registry.getPersistenceManager();
		final TypeInfoMap persistenceInfo = persistenceManager.getPersistenceInfo("JirafeChangeTracker");
		final String tableName = persistenceInfo.getItemTableName();
		final PropertyColumnInfo typeInfo = persistenceInfo.getInfoForCoreProperty(JirafeChangeTrackerModel.PK);
		final String columnName = typeInfo.getColumnName();
		final String query = "DELETE FROM " + tableName + //
				" WHERE " + columnName + " IN (" + StringUtils.repeat("?", ",", howMany) + ")";
		return query;
	}

	private void removeAll(final List<PK> toDelete) throws SQLException
	{
		// This should simply be a call to modelService.removeAll(), but the performance was abysmal.
		// Instead of a single sql delete, it generated one per row.
		// Worse, it also generated an additional delete on aclentries and 2 on props *per row*.
		Connection connection = null;
		PreparedStatement statement = null;
		try
		{
			final Tenant tenant = Registry.getCurrentTenant();
			connection = tenant.getDataSource().getConnection();
			statement = connection.prepareStatement(createDeleteString(toDelete.size()));
			// PK.getLong() not in Hybris v4
			// statement.setLong(1, pk.getLong().longValue());
			int parameterIndex = 0;
			for (final PK pk : toDelete)
			{
				statement.setLong(++parameterIndex, Long.parseLong(pk.toString()));
			}
			statement.executeUpdate();

			// Support recommends clearing the cache after the JDBC updates,
			// ... but there shouldn't be any cached change trackers anyway.
			//tenant.getCache().clear();
		}
		finally
		{
			if (statement != null)
			{
				statement.close();
			}
			if (connection != null)
			{
				connection.close();
			}
		}
	}

	public Map<PK, Map<String, Object>> load()
	{
		final Map<PK, Map<String, Object>> ret = new HashMap<PK, Map<String, Object>>();

		final JirafeChangeTrackerModel example = new JirafeChangeTrackerModel();
		example.setContainerPK(containerPK);
		final List<JirafeChangeTrackerModel> jirafeChangeTrackerModels = flexibleSearchService.getModelsByExample(example);

		if (jirafeChangeTrackerModels.size() <= 0)
		{
			return ret;
		}

		final List<PK> toDelete = new LinkedList<PK>();
		for (final JirafeChangeTrackerModel jirafeChangeTrackerModel : jirafeChangeTrackerModels)
		{
			final PK pk = jirafeChangeTrackerModel.getEntryPK();
			Map map = ret.get(pk);
			if (map == null)
			{
				map = new HashMap<String, String>();
				ret.put(pk, map);
			}
			map.put(jirafeChangeTrackerModel.getAttribute(), jirafeChangeTrackerModel.getValue());
			toDelete.add(jirafeChangeTrackerModel.getPk());
		}
		// Number of times to retry (mostly for deadlocks)
		final int retry0 = Config.getInt("jirafe.deadlock.count", 20);
		int retry = retry0;
		for (;;)
		{
			try
			{
				removeAll(toDelete);
			}
			catch (final SQLException e)
			{
				if (--retry > 0)
				{
					try
					{
						Thread.sleep(Config.getLong("jirafe.deadlock.interval", 50L));
					}
					catch (final InterruptedException e1)
					{
						// the wait is optional anyway
					}
					continue;
				}
				// We got the data but the cleanup failed?
				log.error("", e);
			}
			break;
		}
		if (retry != retry0)
		{
			log.debug("{} retries left", retry);
		}
		return ret;
	}

}
