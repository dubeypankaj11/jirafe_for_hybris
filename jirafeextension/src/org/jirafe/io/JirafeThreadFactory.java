/**
 * 
 */
package org.jirafe.io;

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.Tenant;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.util.Config;

import java.util.concurrent.ThreadFactory;


/**
 * @author Larry Ramponi
 * 
 */
public class JirafeThreadFactory implements ThreadFactory
{

	private final Tenant currentTenant;

	private final String jirafeUser = Config.getString("jirafe.security.userName", "jirafeuser");

	public JirafeThreadFactory()
	{
		currentTenant = Registry.getCurrentTenant();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(final Runnable runnable)
	{
		return new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					Registry.setCurrentTenant(currentTenant);
					// Run as admin user, remove restrictions
					JaloSession.getCurrentSession().setUser(
							JaloSession.getCurrentSession().getUserManager().getUserByLogin(jirafeUser));
					JaloSession.getCurrentSession().setTimeout(-1);
					JaloSession.getCurrentSession().activate();
					runnable.run();
				}

				finally
				{
					JaloSession.deactivate();
					Registry.unsetCurrentTenant();
				}
			}
		};
	}
}
