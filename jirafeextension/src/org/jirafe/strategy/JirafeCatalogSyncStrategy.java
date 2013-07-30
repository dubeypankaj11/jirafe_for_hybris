/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.model.ItemModel;

import org.jirafe.enums.JirafeDataStatus;


/**
 * @author Larry Ramponi
 * @author Dave Brand
 * 
 */
public interface JirafeCatalogSyncStrategy
{

	public JirafeDataStatus sync(ItemModel model);

}
