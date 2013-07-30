/**
 * 
 */
package org.jirafe.filter;

import de.hybris.platform.core.model.ItemModel;


/**
 * Filter interface for persistence strategies. Meant to check if a model should be persisted.
 * 
 * @author Larry Ramponi
 * 
 */
public interface JirafeFilter
{

	public boolean filter(ItemModel itemModel);
}
