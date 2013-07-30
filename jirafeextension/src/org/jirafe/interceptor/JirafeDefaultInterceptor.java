/**
 * 
 */
package org.jirafe.interceptor;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.RemoveInterceptor;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.util.Config;

import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.dto.JirafeDataDto;
import org.jirafe.strategy.JirafeDataPersistStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author alex
 * 
 */
public class JirafeDefaultInterceptor implements ValidateInterceptor, RemoveInterceptor
{

	private final static Logger LOG = LoggerFactory.getLogger(JirafeDefaultInterceptor.class);
	public static final String IS_ENABLED = "jirafe.interceptors.enabled";

	private final JirafeDataPersistStrategy persistStrategy;
	private final String jirafeTypeCode;
	private final JirafeMappingsDao jirafeMappingsDao;
	private final ModelService modelService;

	/**
	 * Constructor that accepts required objects.
	 * 
	 * @param persistStrategy
	 * @param jirafeTypeCode
	 * @param jirafeMappingsDao
	 */
	public JirafeDefaultInterceptor(final JirafeDataPersistStrategy persistStrategy, final String jirafeTypeCode,
			final JirafeMappingsDao jirafeMappingsDao, final ModelService modelService)
	{
		this.jirafeTypeCode = jirafeTypeCode;
		this.persistStrategy = persistStrategy;
		this.jirafeMappingsDao = jirafeMappingsDao;
		this.modelService = modelService;
	}

	/**
	 * Handles both all intercepter events.
	 * 
	 * @param model
	 * @param isRemove
	 */
	protected void onIntercept(final Object model, final Boolean isRemove)
	{

		if (!isEnabled())
		{
			LOG.debug("Jirafe interceptors disabled, update ignored..");
			return;
		}

		final ItemModel itemModel = (ItemModel) model;

		if (isRemove)
		{
			// Remove events aren't needed and are causing too much trouble.
			// Remove until further notice.
			LOG.debug("Not persisting remove event on item {}", itemModel.getClass().getName());
			return;
		}

		if (modelService.isRemoved(itemModel))
		{
			LOG.debug("Intercepted removed item {}", itemModel.getClass().getName());
			return;
		}
		LOG.debug("Intercepted {}", itemModel.getClass().getName());

		if (!jirafeMappingsDao.filter(jirafeTypeCode, itemModel, isRemove))
		{
			LOG.debug("Ignoring {}", itemModel.getClass().getName());
			return;
		}
		LOG.debug("Calling {}", persistStrategy.getClass().getName());

		persistStrategy.persist(new JirafeDataDto(this.jirafeTypeCode, itemModel, isRemove));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.servicelayer.interceptor.ValidateInterceptor#onValidate(java.lang.Object,
	 * de.hybris.platform.servicelayer.interceptor.InterceptorContext)
	 */
	@Override
	public void onValidate(final Object model, final InterceptorContext ctx) throws InterceptorException
	{
		onIntercept(model, Boolean.FALSE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.servicelayer.interceptor.RemoveInterceptor#onRemove(java.lang.Object,
	 * de.hybris.platform.servicelayer.interceptor.InterceptorContext)
	 */
	@Override
	public void onRemove(final Object model, final InterceptorContext ctx) throws InterceptorException
	{
		onIntercept(model, Boolean.TRUE);
	}

	/**
	 * Returns true if data should be intercepted.
	 * 
	 * @return
	 */
	protected boolean isEnabled()
	{
		return Config.getBoolean(IS_ENABLED, true);
	}
}
