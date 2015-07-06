/**
 *
 */
package org.jirafe.strategy;

import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.util.Config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeTempDataModel;
import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.data.JirafeDataModel;
import org.jirafe.webservices.JirafeOutboundClient;
import org.jirafe.webservices.JirafeOutboundClient.STATUS;
import org.jirafe.webservices.JirafeOutboundClient.TransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Syncs {@code JirafeDataModel} to the Jirafe endpoints via the http protocol.
 *
 * @author Larry Ramponi
 *
 */
public class HttpSyncStrategy implements JirafeDataSyncStrategy
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpSyncStrategy.class);
	private static final String NOT_AUTHORIZED = "NOT_AUTHORIZED";

	private static final String MAX_RETRY = "jirafe.jirafeDataSync.authFailureLimit";
	private static final String EVENT_API_MAX = "jirafe.jirafeDataSync.eventApiMax";

	private JirafeOutboundClient jirafeOutboundClient;
	private ModelService modelService;

	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	// following are injected via cronjob
	protected SessionService sessionService;
	protected FlexibleSearchService flexibleSearchService;

	class SiteData
	{
		// Combined length of all data in typeMap
		int len;
		Map<String, TypeData> typeMap;

		SiteData()
		{
			// Reserve space for the braces we'll need to surround the object with
			len = 0;
			typeMap = new HashMap<String, TypeData>();
		}

		void reinitialize()
		{
			len = 0;
			typeMap.clear();
		}

		int getLength()
		{
			// The calculated length + one per comma separator + braces
			return len + typeMap.size() - 1 + "{}".length();
		}

		boolean roomFor(final int len)
		{
			return getLength() + len <= Config.getInt(EVENT_API_MAX, 10000000);
		}
	}

	class TypeData
	{
		LinkedList<JirafeDataModel> models;
		String prefix;
		StringBuilder data;

		TypeData(final String type)
		{
			models = new LinkedList();
			prefix = "\"" + type + "\":[";
			data = new StringBuilder(prefix);
		}

		int getLength()
		{
			// Save a character for the close bracket
			return data.length() + 1;
		}
	}

	private final Map<String, SiteData> queue = new HashMap<String, SiteData>();

	/**
	 * @throws AuthenticationException
	 *
	 */
	@Override
	public void sync(final List<JirafeDataModel> syncData) throws AuthenticationException
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("JirafeData sync : starting syncing {} items", Integer.valueOf(syncData.size()));
		}

		for (final JirafeDataModel model : syncData)
		{
			if (LOG.isDebugEnabled())
			{
				LOG.debug("JirafeData sync : attempting to sync type {}, pk={}", model.getType(), model.getTypePK());
			}
			final String site = model.getSite();
			SiteData siteData = queue.get(site);
			if (siteData == null)
			{
				siteData = new SiteData();
				queue.put(site, siteData);
			}
			final String data = model.getData();
			final String type = jirafeMappingsDao.getEndPointName(model.getType());
			if (!siteData.roomFor(data.length() + 1))
			{
				flush(site);
			}
			TypeData typeData = siteData.typeMap.get(type);
			if (typeData == null)
			{
				typeData = new TypeData(type);
				siteData.len += typeData.getLength();
				siteData.typeMap.put(type, typeData);
			}
			else
			{
				typeData.data.append(",");
				siteData.len += 1;
			}
			typeData.models.add(model);
			typeData.data.append(data);
			siteData.len += data.length();
		}
	}

	private void flush(final String site) throws AuthenticationException
	{
		final SiteData siteData = queue.get(site);
		if (siteData == null)
		{
			return;
		}

		final Map<String, TypeData> typeMap = siteData.typeMap;
		if (typeMap.size() <= 0)
		{
			return;
		}

		int authFailureCount = 0;
		int successCount = 0;
		int failureCount = 0;

		final int maxRetries = Config.getInt(MAX_RETRY, 5);

		int len = "{}".length();
		for (final Entry<String, TypeData> entry : typeMap.entrySet())
		{
			len += entry.getValue().data.length();
		}
		final StringBuffer buf = new StringBuffer(len);
		buf.append("{");
		for (final Entry<String, TypeData> entry : typeMap.entrySet())
		{
			final TypeData typeData = entry.getValue();
			buf.append(typeData.data);
			buf.append("],");
		}

		// Delete the extra comma
		buf.deleteCharAt(buf.length() - 1);

		// End of the map
		buf.append("}");

		try
		{
			while (authFailureCount < maxRetries)
			{
				final String batchBuf = buf.toString();
				LOG.debug("Batch buffer: size is {}, calculated size is {}", batchBuf.length(), siteData.getLength());
				LOG.trace("Batch contents: {}", batchBuf);
				final TransactionResult result = jirafeOutboundClient.putBatch(batchBuf, site);
				LOG.trace("Got result: {} {}", result.status, result.errors);
				switch (result.status)
				{
					case SUCCESS:
					case FAILURE:
						for (final String type : typeMap.keySet())
						{
							final List<JirafeDataModel> models = typeMap.get(type).models;

							for (int i = 0; i < models.size(); ++i)
							{
								STATUS status;
								String errors;
								try
								{
									final TransactionResult lineResult = result.analyzeRow(type, i);
									status = lineResult.status;
									errors = lineResult.errors != null ? lineResult.errors.toString() : null;
								}
								catch (final Exception e)
								{
									status = STATUS.FAILURE;
									errors = e.toString();
								}
								final JirafeDataModel model = models.get(i);
								switch (status)
								{
									case SUCCESS:
										model.setStatus(JirafeDataStatus.ACCEPTED);
										successCount++;
										if (LOG.isDebugEnabled())
										{
											LOG.debug("JirafeData sync: successfully sync'd item {}/{}.", model.getType(), model.getTypePK());
										}
										break;
									case FAILURE:
										model.setStatus(JirafeDataStatus.REJECTED);
										model.setErrors(errors);
										failureCount++;
										LOG.error("JirafeData sync: failed to sync item {}/{}.", model.getType(), model.getTypePK());
										break;
								}
								save(model);
							}
						}
						break;
					default:
						final JirafeDataModel model0 = typeMap.values().iterator().next().models.get(0);
						model0.setStatus(JirafeDataStatus.NOT_AUTHORIZED);
						model0.setErrors(NOT_AUTHORIZED);
						authFailureCount++;
						if (LOG.isDebugEnabled())
						{
							LOG.debug("JirafeData sync : failed to sync - NOT_AUTHORIZED");
						}
						save(model0);
						continue;
				}
				break;
			}
		}
		catch (final Exception e)
		{
			LOG.error("JirafeData sync : exception occurred while syncing item.", e);
			failureCount++;
		}
		finally
		{
			siteData.reinitialize();
		}

		LOG.info("JirafeData sync : completed syncing items. Success={}, Failure={}, AuthFailures={}", //
				new Integer[]
				{ successCount, failureCount, authFailureCount });

		if (authFailureCount >= maxRetries)
		{
			LOG.error("JirafeData sync : Cancelling processing, reached max auth failures ({})", Integer.valueOf(authFailureCount));
			throw new AuthenticationException();
		}
	}

	private void save(final JirafeDataModel model)
	{
		if (!(model instanceof JirafeTempDataModel))
		{
			modelService.save(model);
		}
	}

	@Override
	public void flush() throws AuthenticationException
	{
		for (final String site : queue.keySet())
		{
			flush(site);
		}
	}

	/**
	 * @param jirafeOutboundClient
	 *           the jirafeOutboundClient to set
	 */
	public void setJirafeOutboundClient(final JirafeOutboundClient jirafeOutboundClient)
	{
		this.jirafeOutboundClient = jirafeOutboundClient;
	}

	/**
	 * @param modelService
	 *           the modelService to set
	 */
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}


	/**
	 * @param sessionService
	 *           the sessionService to set
	 */
	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	/**
	 * @param flexibleSearchService
	 *           the flexibleSearchService to set
	 */
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

}
