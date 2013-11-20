/**
 * 
 */
package org.jirafe.interceptor;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.JaloObjectNoLongerValidException;
import de.hybris.platform.servicelayer.exceptions.AttributeNotSupportedException;
import de.hybris.platform.servicelayer.i18n.daos.LanguageDao;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.RemoveInterceptor;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.daos.UserDao;
import de.hybris.platform.util.Config;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeChangeTrackerDao;
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
	private final JirafeJsonConverter jirafeJsonConverter;

	/**
	 * Constructor that accepts required objects.
	 * 
	 * @param persistStrategy
	 * @param jirafeTypeCode
	 * @param jirafeMappingsDao
	 */
	public JirafeDefaultInterceptor(final JirafeDataPersistStrategy persistStrategy, final String jirafeTypeCode,
			final JirafeMappingsDao jirafeMappingsDao, final JirafeJsonConverter jirafeJsonConverter)
	{
		this.jirafeTypeCode = jirafeTypeCode;
		this.persistStrategy = persistStrategy;
		this.jirafeMappingsDao = jirafeMappingsDao;
		this.jirafeJsonConverter = jirafeJsonConverter;
	}

	/**
	 * Handle dirty attribute records
	 * 
	 * For AbstractOrderEntryModel (cart and order line items), write out change records. If debug is enabled, format
	 * change records for log.
	 * 
	 * Finally, return true if there are no changes.
	 * 
	 * @param itemModel
	 * @param isRemove
	 * @param ctx
	 * @return
	 */
	protected boolean noChanges(final ItemModel itemModel, final boolean isRemove, final InterceptorContext ctx)
	{
		Map<String, Set<Locale>> dirtyAttributes = ctx.getDirtyAttributes(itemModel);

		// Discard validations with nothing changed.
		if (!isRemove && dirtyAttributes.size() <= 0)
		{
			return true;
		}

		if (!(itemModel instanceof AbstractOrderEntryModel) && !LOG.isDebugEnabled())
		{
			return false;
		}

		final ModelService modelService = ctx.getModelService();
		// ItemModelContext not available in Hybris 4, so use introspection
		//   mctx = itemModel.getItemModelContext();
		Object mctx;
		try
		{
			mctx = itemModel.getClass().getMethod("getItemModelContext").invoke(itemModel);
		}
		catch (final Exception e)
		{
			mctx = null;
		}
		Item source;
		try
		{
			source = (Item) modelService.getSource(itemModel);
			if (isRemove)
			{
				try
				{
					dirtyAttributes = source.getAllAttributes();
				}
				catch (final Exception e)
				{
					LOG.debug("While trying to getAllAttributes for {} (being removed)", itemModel, e);
				}
			}
		}
		catch (final IllegalStateException e)
		{
			source = null;
		}

		// Use the product pk rather than the entry pk since the entry pk won't be assigned
		// until after the entry is persisted the first time.
		PK productPK = null;

		final JirafeChangeTrackerDao jirafeChangeTrackerDao;
		if (itemModel instanceof AbstractOrderEntryModel)
		{
			final AbstractOrderEntryModel abstractOrderEntryModel = (AbstractOrderEntryModel) itemModel;
			productPK = abstractOrderEntryModel.getProduct().getPk();
			final PK containerPK = abstractOrderEntryModel.getOrder().getPk();
			jirafeChangeTrackerDao = new JirafeChangeTrackerDao(containerPK);
			try
			{
				// We don't get modifiedtime so force it here
				jirafeChangeTrackerDao.save(productPK, "modifiedtime", source == null ? null : source.getAttribute("modifiedtime"));
			}
			catch (final Exception e)
			{
				LOG.debug("", e);
			}
		}
		else
		{
			jirafeChangeTrackerDao = null;
		}
		for (final String att : dirtyAttributes.keySet())
		{
			// Empirically, sometimes the value is in
			//		mctx.getOriginalValue(att)
			// and other times it's in
			// 	ctx.getModelService().getSource(model)).getAttribute(att)
			// Fortunately, if both are present they always seem to agree.
			Object orig = null;
			try
			{
				orig = dirtyAttributes.get(att);
				if (orig == null && mctx != null)
				{
					// ItemModelContext not available in Hybris 4, so use introspection
					//   orig = mctx.getOriginalValue(att);
					orig = mctx.getClass().getMethod("getOriginalValue", String.class).invoke(mctx, att);
				}
			}
			catch (final Exception e1)
			{
				// LOG.debug("", e1);
			}
			if (orig == null)
			{
				try
				{
					orig = source.getAttribute(att);
				}
				catch (final Exception e2)
				{
					// LOG.debug("", e2);
					orig = null;
				}
			}
			if (LOG.isDebugEnabled())
			{
				try
				{
					LOG.debug(String.format("dirty attribute: %s %s->%s", att, orig, modelService.getAttributeValue(itemModel, att)));
				}
				catch (final AttributeNotSupportedException e)
				{
					// Just skip it
				}
			}
			if (productPK != null)
			{
				jirafeChangeTrackerDao.save(productPK, att.toLowerCase(), orig);
			}
		}
		return false;
	}

	/**
	 * Handles all intercepter events.
	 * 
	 * @param model
	 */
	protected void onIntercept(final Object model, final boolean isRemove, final InterceptorContext ctx)
	{

		if (!isEnabled())
		{
			LOG.debug("Jirafe interceptors disabled, update ignored..");
			return;
		}

		final ItemModel itemModel = (ItemModel) model;
		final SessionService sessionService = (SessionService) Registry.getApplicationContext().getBean("sessionService");
		final UserDao userDao = (UserDao) Registry.getApplicationContext().getBean("userDao");
		final LanguageDao languageDao = (LanguageDao) Registry.getApplicationContext().getBean("languageDao");
		sessionService.executeInLocalView(new SessionExecutionBody()
		{
			@Override
			public void executeWithoutResult()
			{
				try
				{
					LOG.debug("Intercepted {}", itemModel);
					sessionService.setAttribute("language", languageDao.findLanguagesByCode("en").get(0));
					LOG.debug("Language set to {}", ((LanguageModel) sessionService.getAttribute("language")).getIsocode());

					if (noChanges(itemModel, isRemove, ctx))
					{
						LOG.debug("Ignoring {} (no changes)", itemModel);
						return;
					}

					if (isRemove)
					{
						LOG.debug("Ignoring {} (remove)", itemModel);
						return;
					}

					if (!jirafeMappingsDao.filter(jirafeTypeCode, itemModel))
					{
						LOG.debug("Ignoring {} (filtered)", itemModel);
						return;
					}

					LOG.debug("Calling {}", persistStrategy.getClass().getName());

					for (final String site : jirafeJsonConverter.getSites(itemModel))
					{
						persistStrategy.persist(new JirafeDataDto(jirafeTypeCode, itemModel, site));
					}
				}
				catch (final JaloObjectNoLongerValidException e)
				{
					LOG.debug("Intercepted removed item {}", e.getMessage());

					// It's ok to lose a cart
					if (!(itemModel instanceof CartModel))
					{
						throw e;
					}
				}
				catch (final Exception e)
				{
					LOG.error("Failed to map intercepted object {} due to: ", itemModel, e);
				}
			}
		}, userDao.findUserByUID("jirafeuser"));

		LOG.debug("Language reverted to {}", ((LanguageModel) sessionService.getAttribute("language")).getIsocode());
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
		onIntercept(model, false, ctx);
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
		onIntercept(model, true, ctx);
	}

	/**
	 * Returns true if data should be intercepted.
	 */
	protected boolean isEnabled()
	{
		return Config.getBoolean(IS_ENABLED, true);
	}

}
