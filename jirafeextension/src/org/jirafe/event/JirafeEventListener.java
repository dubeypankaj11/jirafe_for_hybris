/**
 * 
 */
package org.jirafe.event;

import de.hybris.platform.servicelayer.event.events.AbstractEvent;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Larry Ramponi
 * 
 */
public class JirafeEventListener extends AbstractEventListener<AbstractEvent>
{


	private final static Logger LOG = LoggerFactory.getLogger(JirafeEventListener.class);

	private Set<String> eventTypes;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.hybris.platform.servicelayer.event.impl.AbstractEventListener#onEvent(de.hybris.platform.servicelayer.event
	 * .events.AbstractEvent)
	 */
	@Override
	protected void onEvent(final AbstractEvent event)
	{
		//if(this.eventTypes.contains(event.getSource().))
		LOG.debug(String.format("intercepted event %s", event.toString()));
		return;
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
