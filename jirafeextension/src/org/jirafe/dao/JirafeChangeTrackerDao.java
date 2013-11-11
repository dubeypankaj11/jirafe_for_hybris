/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.Registry;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.util.HashMap;
import java.util.Map;

import org.jirafe.model.data.JirafeChangeTrackerModel;


/**
 * @author dbrand
 * 
 */
public class JirafeChangeTrackerDao
{
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
		final JirafeChangeTrackerModel jirafeChangeTrackerModel = modelService.create(JirafeChangeTrackerModel.class);
		jirafeChangeTrackerModel.setContainerPK(containerPK);
		jirafeChangeTrackerModel.setEntryPK(entryPK);
		jirafeChangeTrackerModel.setAttribute(attribute);
		jirafeChangeTrackerModel.setValue(value);
		try
		{
			modelService.save(jirafeChangeTrackerModel);
		}
		catch (final ModelSavingException e)
		{
			// Ignore the duplicate key exceptions - we only want the oldest one
		}
	}

	public Map<PK, Map<String, Object>> load()
	{
		final Map<PK, Map<String, Object>> ret = new HashMap<PK, Map<String, Object>>();
		final JirafeChangeTrackerModel example = new JirafeChangeTrackerModel();
		example.setContainerPK(containerPK);
		for (final JirafeChangeTrackerModel jirafeChangeTrackerModel : flexibleSearchService.getModelsByExample(example))
		{
			final PK pk = jirafeChangeTrackerModel.getEntryPK();
			Map map = ret.get(pk);
			if (map == null)
			{
				map = new HashMap<String, String>();
				ret.put(pk, map);
			}
			map.put(jirafeChangeTrackerModel.getAttribute(), jirafeChangeTrackerModel.getValue());
			modelService.remove(jirafeChangeTrackerModel);
		}
		return ret;
	}

}
