/**
 * 
 */
package org.jirafe.strategy;

import org.jirafe.dto.JirafeDataDto;


/**
 * Persistence strategy interface.
 * 
 * @author Larry Ramponi
 * 
 */
public interface JirafeDataPersistStrategy
{
	public void persist(JirafeDataDto jirafeDataDto);
}
