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

	public void sync(List<JirafeDataModel> syncData);

}
