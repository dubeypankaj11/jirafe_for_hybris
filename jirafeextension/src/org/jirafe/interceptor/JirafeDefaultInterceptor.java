/**
 * 
 */
package org.jirafe.interceptor;

import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.user.EmployeeModel;
import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.JaloObjectNoLongerValidException;
import de.hybris.platform.search.restriction.SearchRestrictionService;
import de.hybris.platform.servicelayer.exceptions.AttributeNotSupportedException;
import de.hybris.platform.servicelayer.i18n.daos.LanguageDao;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.RemoveInterceptor;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.util.Config;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jirafe.converter.JirafeJsonConverter;
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
	 * Check if the intercept included changes
	 * 
	 * @param itemModel
	 * @param isRemove
	 * @param ctx
	 * @return true if there are no changes
	 * 
	 *         If debug is enabled, log the changed attributes.
	 * 
	 */
	protected boolean noChanges(final ItemModel itemModel, final boolean isRemove, final InterceptorContext ctx)
	{
		Map<String, Set<Locale>> dirtyAttributes = ctx.getDirtyAttributes(itemModel);

		// Discard validations with nothing changed.
		if (!isRemove && dirtyAttributes.size() <= 0)
		{
			return true;
		}

		if (!LOG.isDebugEnabled())
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
				// All the source attributes are changing, in a sense
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
			try
			{
				LOG.debug(String.format("dirty attribute: %s %s->%s", att, orig, modelService.getAttributeValue(itemModel, att)));
			}
			catch (final AttributeNotSupportedException e)
			{
				// Just skip it
			}
		}
		return false;
	}

	/**
	 * Wrap the interceptor with an exception catch-all
	 * 
	 * @param model
	 */
	protected void onIntercept(final Object model, final boolean isRemove, final InterceptorContext ctx)
	{
		try
		{
			onInterceptInternal(model, isRemove, ctx);
		}
		catch (final Exception e)
		{
			LOG.error("Unexpected exception in Jirafe interceptor (ignored, but please report)", e);
		}
	}

	/**
	 * Handles all intercepter events.
	 * 
	 * @param model
	 */
	protected void onInterceptInternal(final Object model, final boolean isRemove, final InterceptorContext ctx)
	{

		if (!isEnabled())
		{
			LOG.debug("Jirafe interceptors disabled, update ignored..");
			return;
		}

		final ItemModel itemModel = (ItemModel) model;
		final SessionService sessionService = (SessionService) Registry.getApplicationContext().getBean("sessionService");
		final LanguageDao languageDao = (LanguageDao) Registry.getApplicationContext().getBean("languageDao");

		final UserService userService = (UserService) Registry.getApplicationContext().getBean("userService");
		final SearchRestrictionService searchRestrictionService = (SearchRestrictionService) Registry.getApplicationContext()
				.getBean("searchRestrictionService");
		final EmployeeModel jirafeUser = (EmployeeModel) sessionService.executeInLocalView(new SessionExecutionBody()
		{
			@Override
			public Object execute()
			{
				searchRestrictionService.disableSearchRestrictions();
				return userService.getUserForUID("jirafeuser", EmployeeModel.class);
			}
		});
		sessionService.executeInLocalView(new SessionExecutionBody()
		{
			@Override
			public void executeWithoutResult()
			{
				try
				{
					LOG.debug("Intercepted {}", itemModel);
					sessionService.setAttribute("language", languageDao.findLanguagesByCode("en").get(0));

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

					// No much else we can do if the item's disappeared
				}
				catch (final Exception e)
				{
					LOG.error("Failed to map intercepted object {} due to: ", itemModel, e);
				}
			}
		}, jirafeUser);
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
