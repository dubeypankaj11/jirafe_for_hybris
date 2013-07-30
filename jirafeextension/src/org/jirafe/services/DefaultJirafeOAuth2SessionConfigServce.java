/**
 * 
 */
package org.jirafe.services;


/**
 * @author alex
 * 
 */
public class DefaultJirafeOAuth2SessionConfigServce implements JirafeOAuth2SessionConfigServce
{


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeOAuth2SessionConfigServce#getRefreshToken()
	 */
	@Override
	public String getRefreshToken()
	{
		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeOAuth2SessionConfigServce#setRefreshToken(java.lang.String)
	 */
	@Override
	public void setRefreshToken(final String refreshToken)
	{
		// YTODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeOAuth2SessionConfigServce#setBeingFetched(boolean)
	 */
	@Override
	public void setBeingFetched(final boolean isBeingFetched)
	{
		// YTODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jirafe.webservices.JirafeOAuth2SessionConfigServce#isBeingFetched()
	 */
	@Override
	public boolean isBeingFetched()
	{
		// YTODO Auto-generated method stub
		return false;
	}
}
