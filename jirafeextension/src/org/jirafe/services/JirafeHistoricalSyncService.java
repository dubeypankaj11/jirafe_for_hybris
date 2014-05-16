/**
 * 
 */
package org.jirafe.services;

import java.util.Map;


/**
 * @author dbrand
 * 
 */
public interface JirafeHistoricalSyncService
{
	Map request(final String siteName, final String command, final Map args);

	Map start(final String siteName, final Map args);

	Map stop(final String siteName, final Map args);

	Map status(final String siteName, final Map args);
}
