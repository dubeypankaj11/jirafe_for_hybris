/**
 * 
 */
package org.jirafe.converter;

/**
 * @author alex
 * 
 */
public class JirafeConvertException extends Exception
{
	public JirafeConvertException()
	{
		super();
	}

	public JirafeConvertException(final String msg)
	{
		super(msg);
	}

	public JirafeConvertException(final Exception e)
	{
		super(e);
	}

	public JirafeConvertException(final String msg, final Exception e)
	{
		super(msg, e);
	}
}
