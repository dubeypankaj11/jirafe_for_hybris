/**
 * 
 */
package org.jirafe.dao;

import org.jirafe.model.data.JirafeDataModel;


/**
 * Interface for all dao implementation of the {@code JirafeDataModel}
 * 
 * @author Larry Ramponi
 * 
 */
public interface JirafeDataDao
{
	/**
	 * 
	 * @param jirafeTypeCode
	 * @param pk
	 * @param json
	 * @param site
	 * @param isRemove
	 * @return JirafeDataModel
	 */
	public JirafeDataModel save(String jirafeTypeCode, String pk, String json, String site, Boolean isRemove);
}
