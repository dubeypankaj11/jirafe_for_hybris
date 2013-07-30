/**
 * 
 */
package org.jirafe.dao;

import org.jirafe.model.data.JirafeOAuthConfigModel;


/**
 * @author alex
 * 
 */
public interface JirafeOAuth2SessionConfigDao
{

	JirafeOAuthConfigModel getSessionConfig();

	JirafeOAuthConfigModel saveSessionConfig(JirafeOAuthConfigModel model);


}
