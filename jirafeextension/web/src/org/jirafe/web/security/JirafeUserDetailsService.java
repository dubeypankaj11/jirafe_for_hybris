/**
 * 
 */
package org.jirafe.web.security;

import de.hybris.platform.core.model.user.UserGroupModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.user.UserService;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * @author alex
 * 
 */
@Service("jirafeUserDetailsService")
public class JirafeUserDetailsService implements UserDetailsService
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeUserDetailsService.class);


	@Resource(name = "userService")
	private UserService userService;

	@Resource(name = "configurationService")
	private ConfigurationService configurationService;

	private String userName;
	private String groupName;
	private String roleName;
	private String servicesRoleName;

	@PostConstruct
	public void init()
	{
		userName = configurationService.getConfiguration().getString("jirafe.security.userName", "jirafeuser");
		groupName = configurationService.getConfiguration().getString("jirafe.security.groupName", "jirafegroup");
		roleName = configurationService.getConfiguration().getString("jirafe.security.roleName", "ROLE_JIRAFE_ADMIN");
		servicesRoleName = configurationService.getConfiguration().getString("jirafe.security.roleName", "ROLE_JIRAFE_SERVICES");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
	 */
	@Override
	public UserDetails loadUserByUsername(final String userName) throws UsernameNotFoundException
	{
		LOG.debug("Loading user info for <{}>", userName);
		final UserModel user = userService.getUserForUID(userName);

		if (user == null)
		{
			LOG.debug("User <{}> not found", userName);
			return null;
		}

		if (user.isLoginDisabled())
		{
			LOG.debug("User <{}> has login disabled", userName);
			return null;
		}

		final List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
		final UserGroupModel groupToCheckFor = userService.getUserGroupForUID(groupName);
		if (userService.isMemberOfGroup(user, groupToCheckFor))
		{
			LOG.debug("User <{}> is a member of <{}>", userName, groupName);
			LOG.debug("Setting role for user <{}> to <{}>", userName, roleName);
			roles.add(new GrantedAuthority()
			{
				@Override
				public String getAuthority()
				{
					return roleName;
				}
			});
			if (userName.equals(this.userName))
			{
				LOG.debug("User <{}> is the special jirafe user", userName);
				LOG.debug("Setting role for user <{}> to <{}>", userName, servicesRoleName);
				roles.add(new GrantedAuthority()
				{
					@Override
					public String getAuthority()
					{
						return servicesRoleName;
					}
				});
			}
			else
			{
				LOG.debug("User <{}> is not the special jirafe user", userName);
			}
		}
		else
		{
			LOG.debug("User <{}> is NOT a member of <{}>", userName, groupName);
		}

		userService.setCurrentUser(user);

		return new User(userName, userService.getPassword(user), roles);
	}
}
