package com.jirafe.components;

import de.hybris.platform.cockpit.session.impl.BaseUICockpitPerspective;
import de.hybris.platform.cockpit.session.impl.TemplateListEntry;

/**
 * @author Cedric Chantepie
 */
public class JirafeReportCockpitPerspective extends BaseUICockpitPerspective {
    @Override
    public boolean canCreate(final TemplateListEntry entry) {
        return false;
    }
}
