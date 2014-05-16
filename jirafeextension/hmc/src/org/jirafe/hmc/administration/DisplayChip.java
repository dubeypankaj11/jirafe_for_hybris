/**
 * 
 */
package org.jirafe.hmc.administration;

import de.hybris.platform.core.Registry;
import de.hybris.platform.hmc.webchips.AbstractChip;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jirafe.webservices.OAuth2ConnectionConfig;

import com.google.gson.Gson;


/**
 * @author dbrand
 * 
 */
public class DisplayChip extends AbstractChip
{
	private static final Logger LOG = Logger.getLogger(DisplayChip.class.getName());

	public OAuth2ConnectionConfig getConnectionConfig()
	{
		return ((OAuth2ConnectionConfig) Registry.getApplicationContext().getBean("connectionConfig"));
	}

	private final String uri;

	/**
	 * @param displayState
	 * @param parent
	 */
	public DisplayChip(final DisplayState displayState, final Chip parent, final String uri)
	{
		super(displayState, parent);
		this.uri = uri;
	}

	@Override
	public String getJSPURI()
	{
		return "/ext/jirafeextension/" + uri + ".jsp";
	}

	@Override
	public void processEvents(final Map<String, List<String>> events)
	{
		if (LOG.isDebugEnabled())
		{
			final Gson gson = new Gson();
			LOG.debug("Got events: " + gson.toJson(events));
		}
	}

}
