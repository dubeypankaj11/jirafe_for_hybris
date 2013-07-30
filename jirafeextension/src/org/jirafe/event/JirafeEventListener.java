/**
 * 
 */
package org.jirafe.event;

import de.hybris.platform.servicelayer.event.events.AbstractPersistenceEvent;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;

import java.util.Set;


/**
 * @author Larry Ramponi
 * 
 */
public class JirafeEventListener extends AbstractEventListener<AbstractPersistenceEvent>
{

	private Set<String> eventTypes;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.hybris.platform.servicelayer.event.impl.AbstractEventListener#onEvent(de.hybris.platform.servicelayer.event
	 * .events.AbstractEvent)
	 */
	@Override
	protected void onEvent(final AbstractPersistenceEvent event)
	{
		//if(this.eventTypes.contains(event.getSource().))
		System.out.println("event=" + event);
	}

	/**
	 * Adds a type to listen on.
	 * 
	 * @param type
	 */
	public void addEventType(final String type)
	{
		this.eventTypes.add(type);
	}

	/**
	 * Sets the event types.
	 * 
	 * @param eventTypes
	 */
	public void setEventTypes(final Set<String> eventTypes)
	{
		this.eventTypes = eventTypes;
	}


}
