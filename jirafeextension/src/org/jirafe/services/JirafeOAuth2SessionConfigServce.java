/**
 * 
 */
package org.jirafe.services;

/**
 * @author alex
 * 
 */
public interface JirafeOAuth2SessionConfigServce
{
	String getRefreshToken();

	void setRefreshToken(String refreshToken);

	void setBeingFetched(boolean isBeingFetched);

	boolean isBeingFetched();
}
