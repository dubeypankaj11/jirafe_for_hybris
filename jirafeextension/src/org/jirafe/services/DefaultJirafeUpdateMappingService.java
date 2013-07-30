/**
 * 
 */
package org.jirafe.services;

import javax.annotation.Resource;

import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.interceptor.JirafeInterceptorLoader;
import org.springframework.stereotype.Component;


/**
 * @author alex
 * 
 */
@Component("jirafeUpdateMappingService")
public class DefaultJirafeUpdateMappingService implements JirafeUpdateMappingService
{
	@Resource
	private JirafeMappingsDao jirafeMappingsDao;
	@Resource
	private JirafeInterceptorLoader jirafeInterceptorLoader;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeUpdateMappingService#updateDefinition(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateDefinition(final String type, final String json, final String endPoint)
	{
		final boolean isNew = jirafeMappingsDao.updateDefinition(type, json, endPoint);

		if (isNew)
		{
			jirafeInterceptorLoader.registerInterceptor(type);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeUpdateMappingService#deleteDefinition(java.lang.String)
	 */
	@Override
	public void deleteDefinition(final String type)
	{
		jirafeMappingsDao.deleteDefinition(type);
		jirafeInterceptorLoader.unregisterInterceptor(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.services.JirafeUpdateMappingService#updateFilter(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateFilter(final String type, final String filter)
	{
		jirafeMappingsDao.updateFilter(type, filter);

	}

	@Override
	public String getDefinition(final String type)
	{
		return jirafeMappingsDao.loadDefinition(type);
	}

	@Override
	public String getFilter(final String type)
	{
		return jirafeMappingsDao.loadFilter(type);
	}

}
