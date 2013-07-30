/**
 * 
 */
package org.jirafe.strategy;

import org.jirafe.dto.JirafeDataDto;


/**
 * Synchronous strategy for persisting {@code JirafeData}. Runs in the same thread as the parent.
 * 
 * @author Larry Ramponi
 * 
 */
public class SynchronousPersistStrategy extends BasePersistStrategy
{

	@Override
	public void persist(final JirafeDataDto jirafeDataDto)
	{
		doPersist(jirafeDataDto);
	}
}
