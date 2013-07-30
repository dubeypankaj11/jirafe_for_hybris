/**
 * 
 */
package org.jirafe.strategy;

/**
 * Generic exception throw from persistence errors around the {@code JirafeDataModel}.
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeDataPersistException extends Exception
{

	public JirafeDataPersistException()
	{
		super();
	}

	public JirafeDataPersistException(final String msg)
	{
		super(msg);
	}

	public JirafeDataPersistException(final Exception e)
	{
		super(e);
	}

	public JirafeDataPersistException(final String msg, final Exception e)
	{
		super(msg, e);
	}
}
