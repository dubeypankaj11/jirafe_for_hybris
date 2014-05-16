/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Calendar;

import javax.annotation.Resource;

import org.jirafe.model.data.JirafeOAuthConfigModel;
import org.springframework.stereotype.Repository;


/**
 * @author alex
 * 
 */
@Repository("jirafeOAuth2SessionConfigDao")
public class JirafeOAuth2SessionConfigDaoImpl implements JirafeOAuth2SessionConfigDao
{
	@Resource
	private FlexibleSearchService flexibleSearchService;
	@Resource
	private ModelService modelService;


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.dao.JirafeOAuth2SessionConfigDao#getSessionConfig()
	 */
	@Override
	public JirafeOAuthConfigModel getSessionConfig()
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("SELECT {pk} FROM {").append(JirafeOAuthConfigModel._TYPECODE).append("} ");

		final SearchResult<JirafeOAuthConfigModel> result = flexibleSearchService.search(builder.toString());

		if (result == null || result.getResult() == null || result.getResult().isEmpty())
		{
			return null;
		}

		return result.getResult().get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jirafe.dao.JirafeOAuth2SessionConfigDao#setSessionConfig(org.jirafe.model.data.JirafeOAuth2SessionConfigModel)
	 */
	@Override
	public JirafeOAuthConfigModel saveSessionConfig(final JirafeOAuthConfigModel model)
	{
		model.setTimestamp(Calendar.getInstance().getTime());
		modelService.save(model);
		return model;
	}

}
