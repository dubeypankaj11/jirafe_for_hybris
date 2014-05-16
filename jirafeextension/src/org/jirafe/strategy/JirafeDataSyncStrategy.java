/**
 * 
 */
package org.jirafe.strategy;

import java.util.List;

import org.jirafe.model.data.JirafeDataModel;


/**
 * @author Larry Ramponi
 * 
 */
public interface JirafeDataSyncStrategy
{
	public class AuthenticationException extends Exception
	{
		// Authentication or other transient communication failure
	}

	public void sync(List<JirafeDataModel> syncData) throws AuthenticationException;

	public void flush() throws AuthenticationException;

}
