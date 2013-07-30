/**
 * 
 */
package org.jirafe.dto;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.util.WebSessionFunctions;

import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


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
	private final Boolean isRemove;
	private final Date creationTime;

	public JirafeDataDto(final String jirafeTypeCode, final ItemModel itemModel, final Boolean isRemove)
	{
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
		this.isRemove = isRemove;
		this.creationTime = new Date();
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

	/**
	 * @return the isRemove
	 */
	public Boolean getIsRemove()
	{
		return isRemove;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj)
	{
		if (!(obj instanceof JirafeDataDto))
		{
			return false;
		}
		return getItemModel().equals(((JirafeDataDto) obj).getItemModel())
				&& getIsRemove().equals(((JirafeDataDto) obj).getIsRemove());
	}
}
