/**
 * 
 */
package org.jirafe.webservices;

import java.util.List;
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

	public TransactionResult putBatch(final String batch, final String site)
	{

		final Map result = jirafeOAuth2Session.putMessage(batch, "batch", site);

		return analizeResult(result);
	}

	public TransactionResult putMessage(final String message, final String type, final String site)
	{

		final String endPoint = jirafeMappingsDao.getEndPointName(type);

		final Map result = jirafeOAuth2Session.putMessage(message, endPoint, site);

		return analizeResult(result);
	}

	protected TransactionResult analizeResult(final Map result)
	{
		if (result == null)
		{
			return new TransactionResult(STATUS.NOT_AUTHORIZED);
		}
		return new TransactionResult(STATUS.SUCCESS, result);
	}

	public static class TransactionResult
	{
		public TransactionResult analyzeRow(final String type, final int rowNum)
		{
			final Map<String, Map> row = ((List<Map>) errors.get(type)).get(rowNum);
			Map errors = row.get("errors");
			if (errors == null)
			{
				errors = row.get("error_type");
			}
			if (errors != null)
			{
				return new TransactionResult(STATUS.FAILURE, errors);
			}
			return new TransactionResult(STATUS.SUCCESS);
		}

		protected TransactionResult(final STATUS status)
		{
			this.status = status;
		}

		protected TransactionResult(final STATUS status, final Map errors)
		{
			this.status = status;
			this.errors = errors;
		}

		public STATUS status;
		public Map errors;
	}
}
