/**
 * 
 */
package org.jirafe.services;



/**
 * @author alex
 * 
 */
public interface JirafeUpdateMappingService
{
	/**
	 * 
	 * @param type
	 * @param json
	 */
	void updateDefinition(String type, String json, String endPoint);

	/**
	 * 
	 * @param type
	 */
	void deleteDefinition(String type);

	/**
	 * 
	 * @param type
	 * @param filter
	 */
	void updateFilter(String type, String filter);

	/**
	 * @param type
	 * @return
	 */
	String getDefinition(String type);

	/**
	 * @param type
	 * @return
	 */
	String getFilter(String type);


}
