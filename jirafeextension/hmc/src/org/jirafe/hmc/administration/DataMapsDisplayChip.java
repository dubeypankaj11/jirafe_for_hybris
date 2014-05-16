/**
 *
 */
package org.jirafe.hmc.administration;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.hmc.HMCHelper;
import de.hybris.platform.hmc.generic.ClipChip;
import de.hybris.platform.hmc.generic.ItemAcceptor;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;
import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.type.TypeManager;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.util.WebSessionFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * @author dbrand
 * 
 */
public class DataMapsDisplayChip extends DisplayChip implements ClipChip, ItemAcceptor
{
	private static final Logger LOG = Logger.getLogger(DataMapsDisplayChip.class.getName());

	private final ModelService modelService = (ModelService) Registry.getApplicationContext().getBean("modelService");
	private final FlexibleSearchService flexibleSearchService = (FlexibleSearchService) Registry.getApplicationContext().getBean(
			"flexibleSearchService");
	private final JirafeMappingsDao jirafeMappingsDao = (JirafeMappingsDao) Registry.getApplicationContext().getBean(
			"jirafeMappingsDao");

	public JirafeMappingsDao getJirafeMappingsDao()
	{
		return jirafeMappingsDao;
	}

	private final JirafeJsonConverter jirafeJsonConverter = (JirafeJsonConverter) Registry.getApplicationContext().getBean(
			"jirafeJsonConverter");

	Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public final String LOAD_MAP = "LOAD_MAP";
	public final String TEST_MAP = "TEST_MAP";
	public final String SAVE_MAP = "SAVE_MAP";
	public final String PK_PICKER = "PK_PICKER";

	public String type;
	public String pk;
	public String siteName;
	public String filter;
	public String filterOutput;
	public String dataMap;
	public String output;

	/**
	 * @param displayState
	 * @param parent
	 */
	public DataMapsDisplayChip(final DisplayState displayState, final Chip parent)
	{
		super(displayState, parent, "datamaps");
		try
		{
			final Item item = displayState.getItemHistory().get(0);

			type = (String) item.getAttribute("type");

			refresh();
		}
		catch (final Exception e)
		{
			// just leave the rest uninitialized
		}
	}

	@Override
	protected void refresh()
	{
		final ItemModel model;
		try
		{
			filterOutput = output = "";

			if (StringUtils.isEmpty(dataMap))
			{
				dataMap = jirafeMappingsDao.loadDefinition(type);
				if (dataMap == null)
				{
					filter = dataMap = "";
					postErrorMessage("Type invalid or no map found for type.");
				}
				filter = jirafeMappingsDao.loadFilter(type);
			}

			if (StringUtils.isEmpty(pk))
			{
				// Grab the pk of the most recently changed instance for this type - a good starting point for testing the map
				final FlexibleSearchQuery query = new FlexibleSearchQuery("select {pk} from {" + type
						+ "} order by {modifiedtime} desc");
				query.setCount(1);
				pk = ((ItemModel) flexibleSearchService.search(query).getResult().get(0)).getPk().toString();
			}

			model = modelService.get(PK.fromLong(Long.parseLong(pk)));

			if (StringUtils.isEmpty(siteName))
			{
				siteName = getConnectionConfig().getSiteNames()[0];
			}

			final JirafeDataDto jirafeDataDto = new JirafeDataDto(type, model, siteName);
			filterOutput = Boolean.toString(jirafeJsonConverter.filter(type, model, filter));
			final Map map = jirafeJsonConverter.toMap(jirafeDataDto, dataMap);
			output = gson.toJson(map);
			if (map.containsKey("__errors__"))
			{
				postErrorMessage("Errors in the following fields (see log for additional info): " + (String) map.get("__errors__"));
			}
		}
		catch (final Exception e)
		{
			postErrorMessage(e.toString());
		}
	}

	private void pkPicker()
	{
		final HashMap map = new HashMap();
		map.put("site", siteName);
		HMCHelper.openItemSearch(TypeManager.getInstance().getComposedType(type), this, null, false, map, null);
	}

	@Override
	public void processEvents(final Map<String, List<String>> events)
	{
		final HttpServletRequest request = WebSessionFunctions.getCurrentHttpServletRequest();
		super.processEvents(events);

		siteName = request.getParameter("siteName");
		type = request.getParameter("type");
		pk = request.getParameter("pk");
		filter = request.getParameter("filter");
		dataMap = request.getParameter("dataMap");

		if (events.containsKey(LOAD_MAP))
		{
			dataMap = null;
			refresh();
		}
		else if (events.containsKey(TEST_MAP))
		{
			refresh();
		}
		else if (events.containsKey(SAVE_MAP))
		{
			final String endPointName = jirafeMappingsDao.getEndPointName(type);
			jirafeMappingsDao.updateFilter(type, filter);
			jirafeMappingsDao.updateDefinition(type, dataMap, endPointName);
		}
		else if (events.containsKey(PK_PICKER))
		{
			pkPicker();
		}
		else
		{
			return;
		}
		//		if ((Boolean) result.get("success"))
		//		{
		//			postInfoMessage("Request processed successfully.");
		//		}
		//		else
		//		{
		//			postErrorMessage((String) result.get("error"));
		//		}
	}

	@Override
	public void initialize()
	{
		// YTODO Auto-generated method stub

	}

	@Override
	public boolean isInitialized()
	{
		// YTODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.hmc.generic.ItemAcceptor#allowsMultipleSelection()
	 */
	@Override
	public boolean allowsMultipleSelection()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.hmc.generic.ItemAcceptor#setSelectedItems(java.util.List)
	 */
	@Override
	public void setSelectedItems(final List pks)
	{
		if (pks.size() != 1)
		{
			return;
		}
		pk = ((Item) pks.get(0)).getPK().toString();
	}

}
