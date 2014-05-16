/**
 * 
 */
package org.jirafe.webservices;

import java.util.HashMap;
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

	public TransactionResult put(final String batch, final String endPoint, final String site)
	{
		Map result;
		try
		{
			result = jirafeOAuth2Session.putMessage(batch, endPoint, site);
			return analizeResult(result);
		}
		catch (final Exception e)
		{
			return new TransactionResult(STATUS.FAILURE, e.getMessage());
		}
	}

	public TransactionResult putBatch(final String batch, final String site)
	{
		return put(batch, "batch", site);
	}

	public TransactionResult putMessage(final String message, final String type, final String site)
	{
		final String endPoint = jirafeMappingsDao.getEndPointName(type);

		return put(message, endPoint, site);
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
			if (status == STATUS.FAILURE)
			{
				return this;
			}
			final Map<String, Object> row = ((List<Map>) errors.get(type)).get(rowNum);
			final Map errors = (Map) row.get("errors");
			if (errors != null)
			{
				return new TransactionResult(STATUS.FAILURE, errors);
			}
			final String error = (String) row.get("error_type");
			if (error != null)
			{
				return new TransactionResult(STATUS.FAILURE, error);
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

		protected TransactionResult(final STATUS status, final String error)
		{
			this.status = status;
			errors = new HashMap(1);
			errors.put("unknown", error);
		}

		public STATUS status;
		public Map errors;
	}
}
