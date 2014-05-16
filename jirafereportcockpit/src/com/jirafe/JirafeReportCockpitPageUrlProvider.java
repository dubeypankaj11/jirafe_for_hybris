package com.jirafe;

import de.hybris.platform.util.Config;

/**
 * Provides URL to view for Jirafe report cockpit.
 *
 * @author Cedric Chantepie
 */
public class JirafeReportCockpitPageUrlProvider {
    /**
     * Returns view URL.
     */
    public String getUrl() {
    	return Config.getString("reportcockpit.dashboard.url", "https://hybris.partners.jirafe.com");
    } // end of getUrl
} // end of class JirafeReportCockpitPageUrlProvider
