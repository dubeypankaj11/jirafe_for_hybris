/**
 * 
 */
package org.jirafe.event;

import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.event.events.AbstractPersistenceEvent;


/**
 * @author Larry Ramponi
 * 
 */
public class JirafeEvent extends AbstractPersistenceEvent
{

	/**
	 * @param pk
	 */
	public JirafeEvent(final PK pk)
	{
		super(pk);
	}

}
