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
	 * 
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @param pageLimit
	 * @param pageToken
	 */
	String getData(String type, String startTime, String endTime, String pageLimit, String pageToken);
}
