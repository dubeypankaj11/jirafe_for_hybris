/**
 * 
 */
package org.jirafe.dto;

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.util.WebSessionFunctions;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.jirafe.converter.JirafeConvertException;
import org.jirafe.converter.JirafeJsonConverter;


/**
 * Encapsulates the data necessary to save a {@code JirafeDataModel}.
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeDataDto
{
	private final String jirafeTypeCode;
	private final ItemModel itemModel;
	private final Cookie[] cookies;
	private final Date creationTime;

	private final Map<String, Object> map;

	public <T extends ItemModel> JirafeDataDto(final String jirafeTypeCode, final T itemModel) throws JirafeConvertException
	{
		final JirafeJsonConverter jirafeJsonConverter = (JirafeJsonConverter) Registry.getApplicationContext().getBean(
				"jirafeJsonConverter");

		this.jirafeTypeCode = jirafeTypeCode;
		this.itemModel = itemModel;
		//this.cookies = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getCookies();
		final HttpServletRequest servletRequest = WebSessionFunctions.getCurrentHttpServletRequest();
		if (servletRequest == null)
		{
			this.cookies = null;
		}
		else
		{
			this.cookies = servletRequest.getCookies();
		}
		this.creationTime = new Date();
		this.map = jirafeJsonConverter.toMap(this);
	}

	/**
	 * @return the jirafeTypeCode
	 */
	public String getJirafeTypeCode()
	{
		return jirafeTypeCode;
	}

	/**
	 * @return the itemModel
	 */
	public ItemModel getItemModel()
	{
		return itemModel;
	}

	/**
	 * @return the creationTime
	 */
	public Date getCreationTime()
	{
		return creationTime;
	}

	/**
	 * @return the cookies
	 */
	public Cookie[] getCookies()
	{
		return cookies;
	}

	public Map<String, Object> getMap()
	{
		return map;
	}

}
