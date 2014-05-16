/**
 * 
 */
package org.jirafe.hmc.administration;

import de.hybris.platform.cluster.PingBroadcastHandler;
import de.hybris.platform.cms2.model.site.CMSSiteModel;
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.enumeration.EnumerationValueModel;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.hmc.jalo.ConfigConstants;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;
import de.hybris.platform.hmc.webchips.Window;
import de.hybris.platform.hmc.webchips.event.WindowOpenEvent;
import de.hybris.platform.jdbcwrapper.HybrisDataSource;
import de.hybris.platform.print.hmc.ZipFileDownloadWindow;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.util.Config;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jirafe.constants.JirafeextensionConstants;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.webservices.JirafeHeartBeatClient;
import org.springframework.context.ApplicationContext;


/**
 * @author dbrand
 * 
 */
public class StatusDisplayChip extends DisplayChip
{
	private static final Logger LOG = Logger.getLogger(StatusDisplayChip.class.getName());

	public final String REFRESH = "REFRESH";
	public final String EXPORT = "EXPORT";
	public final String TEST_CONNECTION = "TEST_CONNECTION";

	private final ApplicationContext applicationContext = Registry.getApplicationContext();
	private final FlexibleSearchService flexibleSearchService = (FlexibleSearchService) applicationContext
			.getBean("flexibleSearchService");
	private final CMSSiteService cmsSiteService = ((CMSSiteService) applicationContext.getBean("cmsSiteService"));

	private final JirafeMappingsDao jirafeMappingsDao = (JirafeMappingsDao) Registry.getApplicationContext().getBean(
			"jirafeMappingsDao");
	private final JirafeHeartBeatClient jirafeHeartBeatClient = (JirafeHeartBeatClient) applicationContext
			.getBean("jirafeHeartBeatClient");

	private final DisplayState displayState;

	/**
	 * @param displayState
	 * @param parent
	 */
	public StatusDisplayChip(final DisplayState displayState, final Chip parent)
	{
		super(displayState, parent, "status");
		this.displayState = displayState;
	}

	public String testConnection(final String siteName)
	{
		return jirafeHeartBeatClient.ping(siteName) ? "ok" : "fail";
	}

	public List<Object> querySyncStatus(final String siteName)
	{
		final FlexibleSearchQuery query = new FlexibleSearchQuery(
				"select {type}, {status}, count(*) from {JirafeData} where {site} = ?siteName group by {type},{status}", //
				Collections.singletonMap("siteName", siteName));
		query.setResultClassList(Arrays.asList(String.class, EnumerationValueModel.class, Long.class));
		return flexibleSearchService.search(query).getResult();
	}

	public List<List<String>> getBasicInfo()
	{
		final List<List<String>> ret = new LinkedList<List<String>>();

		final HybrisDataSource ds = Registry.getCurrentTenant().getMasterDataSource();

		ret.add(Arrays.asList("Hybris version", Config.getString("build.version", "Not available")));
		ret.add(Arrays.asList("Jirafe for Hybris version", JirafeextensionConstants.RELEASE_VERSION));
		ret.add(Arrays.asList("Database version", ds.getDatabaseName() + " " + ds.getDatabaseVersion()));
		ret.add(Arrays.asList("Database driver version", ds.getDriverVersion()));
		ret.add(Arrays.asList("Interceptors enabled", Config.getString("jirafe.interceptors.enabled", "false")));

		return ret;
	}

	public List<List<String>> getProperties()
	{
		final List<List<String>> ret = new LinkedList<List<String>>();
		for (final Entry<String, String> parameter : Config.getAllParameters().entrySet())
		{
			if (parameter.getKey().startsWith("jirafe."))
			{
				ret.add(Arrays.asList(parameter.getKey(), parameter.getValue()));
			}
		}
		return ret;
	}

	private List<String> getActiveNodes()
	{
		final List<String> nodes = new LinkedList();
		for (final PingBroadcastHandler.NodeInfo node : PingBroadcastHandler.getInstance().getNodes())
		{
			nodes.add(Integer.toString(node.getNodeID()));
		}
		return nodes;
	}

	public List<List<String>> getCronjobStatus()
	{
		final List<List<String>> ret = new LinkedList<List<String>>();

		final List<String> nodes = getActiveNodes();
		final List<Object> cronjobs = flexibleSearchService.search(
				"select {pk} from {" + CronJobModel._TYPECODE + "} where {nodeID} in (" + StringUtils.join(nodes, ",") + ")")
				.getResult();
		for (final Object obj : cronjobs)
		{
			final CronJobModel cronjob = (CronJobModel) obj;
			if (cronjob.getCode().startsWith("jirafe"))
			{
				ret.add(Arrays.asList(cronjob.getCode().toString(), //
						cronjob.getActive().toString(), //
						cronjob.getStartTime() + " - " + cronjob.getEndTime()));
			}
		}

		return ret;
	}

	public List<List<String>> getConnectionStatus()
	{
		final List<List<String>> ret = new LinkedList<List<String>>();

		final Collection<CMSSiteModel> cmsSites = cmsSiteService.getSites();
		if (cmsSites == null || cmsSites.size() <= 0)
		{
			return null;
		}
		final List<String> siteNames = Arrays.asList(getConnectionConfig().getSiteNames());
		for (final CMSSiteModel cmsSite : cmsSites)
		{
			final String siteName = cmsSite.getUid();
			final String siteId = getConnectionConfig().getSiteId(siteName);
			ret.add(Arrays.asList(siteName, //
					siteId != null ? siteId : "", //
					siteNames.contains(siteName) ? testConnection(siteName) : "unmonitored"));
		}

		return ret;
	}

	public Map<String, List<List<String>>> getSyncStatus()
	{
		final Map<String, List<List<String>>> ret = new LinkedHashMap<String, List<List<String>>>();

		final List<String> siteNames = Arrays.asList(getConnectionConfig().getSiteNames());
		for (final String siteName : siteNames)
		{
			List<List<String>> rows = null;
			final List<Object> syncStatuses = querySyncStatus(siteName);
			if (syncStatuses.size() > 0 && (Long) ((List<Object>) syncStatuses.get(0)).get(2) > 0)
			{
				for (final Object obj : syncStatuses)
				{
					final List<Object> dbRow = (List<Object>) obj;
					final String type = (String) dbRow.get(0);
					final EnumerationValueModel status = (EnumerationValueModel) dbRow.get(1);
					final long count = (Long) dbRow.get(2);
					if (rows == null)
					{
						rows = new LinkedList<List<String>>();
					}
					rows.add(Arrays.asList(type, //
							status.getCode(), //
							Long.toString(count)));
				}
			}
			ret.put(siteName, rows);
		}

		return ret;
	}

	private void putEntry(final ZipOutputStream zos, final String name, final String content) throws Exception
	{
		zos.putNextEntry(new ZipEntry(name));
		try
		{
			zos.write(content.getBytes("UTF8"));
		}
		finally
		{
			zos.closeEntry();
		}
	}

	private void putRows(final StringBuilder sb, final List<List<String>> rows)
	{
		for (final List<String> row : rows)
		{
			sb.append(StringUtils.join(row, '\t')).append('\n');
		}
	}

	private String formatSystemStatus()
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("\n\nBasic info\n\n");
		putRows(sb, getBasicInfo());

		sb.append("\n\nCronjob status\n\n");
		putRows(sb, getCronjobStatus());

		sb.append("\n\nConnection status\n\n");
		putRows(sb, getConnectionStatus());

		sb.append("\n\nSynchronization status\n");
		final Map<String, List<List<String>>> syncStatus = getSyncStatus();
		for (final String siteName : syncStatus.keySet())
		{
			sb.append('\n').append(siteName).append('\n');
			final List<List<String>> rows = syncStatus.get(siteName);
			if (rows == null)
			{
				sb.append("No data found.\n");
				continue;
			}
			putRows(sb, rows);
		}

		sb.append("\n\nProperties\n\n");
		putRows(sb, getProperties());

		return sb.toString();
	}

	public byte[] getConfigurationZip() throws Exception
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			final ZipOutputStream zos = new ZipOutputStream(baos);
			try
			{
				putEntry(zos, "00README.txt", formatSystemStatus());
				for (final String type : jirafeMappingsDao.getAllMappedTypes())
				{
					putEntry(zos, "datamaps/" + type + "-filter.groovy", jirafeMappingsDao.loadFilter(type));
					putEntry(zos, "datamaps/" + type + ".json", jirafeMappingsDao.loadDefinition(type));
				}
			}
			finally
			{
				zos.close();
			}
			return baos.toByteArray();
		}
		finally
		{
			baos.close();
		}
	}

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	{
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public void processEvents(final Map<String, List<String>> events)
	{
		super.processEvents(events);
		if (events.containsKey(EXPORT))
		{
			try
			{
				final byte[] content = getConfigurationZip();
				final String now = formatter.format(new Date());
				final Window window = new ZipFileDownloadWindow(displayState, "Export Configuration", content, //
						"Jirafe-" + getConnectionConfig().getSiteIds().iterator().next() + "-" + now + ".zip", "application/zip");
				final WindowOpenEvent woe = new WindowOpenEvent(window, ConfigConstants.getInstance().WINDOW_ORGANIZER);
				woe.setHeight(25);//it's percentage not pixels !
				woe.setWidth(25);
				window.open(woe);
			}
			catch (final Exception e)
			{
				postErrorMessage(e.getMessage());
				return;
			}
		}
		else if (events.containsKey(REFRESH))
		{
			// nothing special to do
		}
		else
		{
			return;
		}
	}

}
