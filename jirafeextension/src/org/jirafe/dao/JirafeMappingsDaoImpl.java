/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.search.exceptions.FlexibleSearchException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.type.TypeService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.model.data.JirafeMappingDefinitionsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author alex
 * 
 */

public class JirafeMappingsDaoImpl implements JirafeMappingsDao
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeMappingsDaoImpl.class);

	private FlexibleSearchService flexibleSearchService;
	private ModelService modelService;
	private TypeService typeService;

	@Resource(name = "sessionService")
	private SessionService sessionService;

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;

	final static private String queryByType = new StringBuilder("SELECT {").append(JirafeMappingDefinitionsModel.PK).append("} ")//
			.append("FROM {").append(JirafeMappingDefinitionsModel._TYPECODE).append("} ")//
			.append("WHERE {").append(JirafeMappingDefinitionsModel.TYPE).append("}=?type ")//
			.append("ORDER BY {").append(JirafeMappingDefinitionsModel.PK).append("} ASC").toString();

	final static private String queryGetAll = new StringBuilder("SELECT {").append(JirafeMappingDefinitionsModel.PK).append("} ")//
			.append("FROM {").append(JirafeMappingDefinitionsModel._TYPECODE).append("} ")//
			.append("ORDER BY {").append(JirafeMappingDefinitionsModel.PK).append("} ASC").toString();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeMappingsDao#loadDefinition(java.lang.String)
	 */
	@Override
	public String loadDefinition(final String type)
	{
		final JirafeMappingDefinitionsModel model = getByType(type);

		if (model == null)
		{
			return null;
		}

		return model.getDefinition();
	}

	@Override
	public String getMappedType(final ItemModel model)
	{
		final List<JirafeMappingDefinitionsModel> allDefinitions = getAllDefinitions();
		if (allDefinitions == null)
		{
			return null;
		}
		for (final JirafeMappingDefinitionsModel def : allDefinitions)
		{
			final String type = def.getType();
			// if (modelService.create(type).getClass().isInstance(model))
			if (typeService.isAssignableFrom(type, model.getItemtype()))
			{
				return type;
			}
		}
		return null;
	}

	@Override
	public List<String> getAllMappedTypes()
	{
		final List<JirafeMappingDefinitionsModel> definitions = getAllDefinitions();
		final List<String> types = new ArrayList(definitions.size());
		for (final JirafeMappingDefinitionsModel definition : definitions)
		{
			types.add(definition.getType());
		}
		return types;
	}

	public JirafeMappingDefinitionsModel getByType(final String type)
	{
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put(JirafeMappingDefinitionsModel.TYPE, type);

		final SearchResult<JirafeMappingDefinitionsModel> result;


		result = flexibleSearchService.search(queryByType, params);


		if (result == null || result.getResult() == null || result.getResult().isEmpty())
		{
			return null;
		}

		return result.getResult().get(0);
	}

	@Override
	public List<JirafeMappingDefinitionsModel> getAllDefinitions()
	{
		try
		{
			final SearchResult<JirafeMappingDefinitionsModel> result = flexibleSearchService.search(queryGetAll);
			if (result == null || result.getResult() == null || result.getResult().isEmpty())
			{
				return null;
			}
			return result.getResult();
		}
		catch (final FlexibleSearchException e)
		{
			LOG.error("Failed to load Jirafe mapping definitions. System not updated?");
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeMappingsDao#updateDefinition(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean updateDefinition(final String type, final String json, final String endPoint)
	{
		boolean isNew = false;
		JirafeMappingDefinitionsModel model = getByType(type);
		if (model == null)
		{
			isNew = true;
			model = new JirafeMappingDefinitionsModel();
			model.setType(type);
		}
		model.setEndPointName(endPoint);
		model.setDefinition(json);
		model.setTimestamp(Calendar.getInstance().getTime());

		modelService.save(model);
		return isNew;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeMappingsDao#deleteDefinition(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean deleteDefinition(final String type)
	{
		final JirafeMappingDefinitionsModel model = getByType(type);
		if (model == null)
		{
			return false;
		}
		modelService.remove(model);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeMappingsDao#loadFilter(java.lang.String)
	 */
	@Override
	public String loadFilter(final String type)
	{
		final JirafeMappingDefinitionsModel model = getByType(type);

		if (model == null)
		{
			return null;
		}

		return model.getFilter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeMappingsDao#updateFilter(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateFilter(final String type, final String filter)
	{
		JirafeMappingDefinitionsModel model = getByType(type);
		if (model == null)
		{
			model = new JirafeMappingDefinitionsModel();
			model.setType(type);
		}
		model.setFilter(filter);
		model.setTimestamp(Calendar.getInstance().getTime());

		modelService.save(model);
	}

	/**
	 * Returns TRUE if the model should be intercepted, FALSE if it should be ignored.
	 * 
	 * @param itemModel
	 */
	@Override
	public boolean filter(final String type, final ItemModel itemModel)
	{
		return jirafeJsonConverter.filter(type, itemModel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeMappingsDao#loadEndPoint(java.lang.String)
	 */
	@Override
	public String getEndPointName(final String type)
	{
		final JirafeMappingDefinitionsModel model = getByType(type);

		if (model != null)
		{
			final String endPoint = model.getEndPointName();
			if (endPoint != null)
			{
				return endPoint;
			}
		}

		return type.toLowerCase();
	}

	/**
	 * @param flexibleSearchService
	 *           the flexibleSearchService to set
	 */
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
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
	 * @param typeService
	 *           the typeService to set
	 */
	public void setTypeService(final TypeService typeService)
	{
		this.typeService = typeService;
	}
}
