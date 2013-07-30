/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.core.model.ItemModel;

import java.util.List;

import org.jirafe.model.data.JirafeMappingDefinitionsModel;


/**
 * @author alex
 * 
 */
public interface JirafeMappingsDao
{

	/**
	 * 
	 * @return All definitions
	 */
	List<JirafeMappingDefinitionsModel> getAllDefinitions();

	/**
	 * 
	 * @param type
	 * @return The definition
	 */
	String loadDefinition(String type);

	String getMappedType(final ItemModel model);

	/**
	 * 
	 * @param type
	 * @param json
	 * @return true if definition is new
	 */
	boolean updateDefinition(String type, String json, String endPoint);

	/**
	 * 
	 * @param type
	 * @return true if definition is new
	 */
	boolean deleteDefinition(String type);

	/**
	 * 
	 * @param type
	 * @param filter
	 */
	void updateFilter(String type, String filter);

	/**
	 * 
	 * @param type
	 * @return The filter
	 */
	String loadFilter(String type);

	public boolean filter(final String type, final ItemModel itemModel, boolean isRemove);

	/**
	 * 
	 * @param type
	 * @return The endPoint
	 */
	String getEndPointName(String type);

}
