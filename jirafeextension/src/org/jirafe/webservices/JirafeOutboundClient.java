/**
 * 
 */
package org.jirafe.webservices;

import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.dao.JirafeMappingsDao;


/**
 * @author alex
 * 
 */
public class JirafeOutboundClient
{
	@Resource
	private JirafeOAuth2Session jirafeOAuth2Session;
	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	public enum STATUS
	{
		SUCCESS, FAILURE, NOT_AUTHORIZED
	}

	public TransactionResult putMessage(final String message, final String type, final boolean isRemove)
	{

		String endPoint = jirafeMappingsDao.getEndPointName(type);
		if (endPoint == null)
		{
			endPoint = type.toLowerCase();
		}
		final Map result = jirafeOAuth2Session.putMessage(message, endPoint, isRemove);

		return analizeResult(result);
	}

	protected TransactionResult analizeResult(final Map result)
	{
		if (result == null)
		{
			return new TransactionResult(STATUS.NOT_AUTHORIZED);
		}
		Object errors = result.get("errors");
		if (errors == null)
		{
			errors = result.get("error_type");
		}
		if (errors != null)
		{
			return new TransactionResult(STATUS.FAILURE, errors);
		}
		return new TransactionResult(STATUS.SUCCESS);
	}

	public static class TransactionResult
	{
		protected TransactionResult(final STATUS status)
		{
			this.status = status;
		}

		protected TransactionResult(final STATUS status, final Object errors)
		{
			this.status = status;
			this.errors = errors;
		}

		public STATUS status;
		public Object errors;
	}
}
