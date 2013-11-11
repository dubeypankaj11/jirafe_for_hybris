/**
 * 
 */
package org.jirafe.services;



/**
 * @author dbrand
 * 
 */
public interface JirafeDataEndpointService
{
	/**
	 * @param type
	 * @param site YTODO
	 * @param startTime
	 * @param endTime
	 * @param pageLimit
	 * @param pageToken
	 */
	String getData(String type, String site, String startTime, String endTime, String pageLimit, String pageToken);

	/**
	 * @param type
	 * @param id
	 */
	String getData(String type, String pk, String map);
}
