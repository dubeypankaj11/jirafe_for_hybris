/**
 * 
 */
package org.jirafe.interceptor;

import de.hybris.platform.servicelayer.event.events.AfterSessionCreationEvent;
import de.hybris.platform.servicelayer.event.impl.AbstractEventListener;
import de.hybris.platform.servicelayer.interceptor.InterceptorRegistry;
import de.hybris.platform.servicelayer.interceptor.impl.DefaultInterceptorRegistry;
import de.hybris.platform.servicelayer.interceptor.impl.InterceptorMapping;
import de.hybris.platform.servicelayer.model.ModelService;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.model.data.JirafeMappingDefinitionsModel;
import org.jirafe.strategy.JirafeDataPersistStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author alex
 * 
 */
public class JirafeInterceptorLoader extends AbstractEventListener<AfterSessionCreationEvent>
{
	private final static Logger LOG = LoggerFactory.getLogger(JirafeInterceptorLoader.class);
	private boolean loaded = false;
	private final Object _interceptorRegistryLock = new Object();
	private InterceptorRegistry _interceptorRegistry;

	@Resource(name = "jirafeDataPersistStrategy")
	private JirafeDataPersistStrategy persistStrategy;

	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	@Resource
	ModelService modelService;

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;

	@Override
	protected void onEvent(final AfterSessionCreationEvent event)
	{
		if (loaded)
		{
			return;
		}
		loaded = true;
		LOG.info("Initializing interceptors");

		final List<JirafeMappingDefinitionsModel> definitions = jirafeMappingsDao.getAllDefinitions();

		if (definitions == null)
		{
			LOG.info("No mapping definitions found, therefore no interceptors will be loaded.");
		}
		else
		{
			LOG.info("Loading {} definitions.", definitions.size());
			for (final JirafeMappingDefinitionsModel definition : definitions)
			{
				registerInterceptor(definition.getType());
			}
		}

		LOG.info("Done initializing interceptors");

	}

	public void registerInterceptor(final String type)
	{
		final DefaultInterceptorRegistry interceptorRegistry;
		final InterceptorMapping mapping;

		interceptorRegistry = ((DefaultInterceptorRegistry) getInterceptorRegistry());
		mapping = new InterceptorMapping();

		// Set values using a mapping object, allows for setting order.
		mapping.setTypeCode(type);
		mapping.setInterceptor(new JirafeDefaultInterceptor(persistStrategy, type, jirafeMappingsDao, jirafeJsonConverter));
		mapping.setOrder(Integer.MAX_VALUE);
		mapping.setReplacedInterceptors(Collections.EMPTY_LIST);

		try
		{
			LOG.info("Initializing interceptor for <{}>", type);
			interceptorRegistry.registerInterceptor(mapping);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to load interceptor for type <{}>", type, e);
		}
	}

	public void unregisterInterceptor(final String type)
	{
		final DefaultInterceptorRegistry interceptorRegistry;
		final InterceptorMapping mapping;

		interceptorRegistry = ((DefaultInterceptorRegistry) getInterceptorRegistry());
		mapping = new InterceptorMapping();

		// Set values using a mapping object, allows for setting order.
		mapping.setTypeCode(type);
		// I tried with and without the following and the interceptor doesn't get removed
		//mapping.setInterceptor(new JirafeDefaultInterceptor(persistStrategy, type, jirafeMappingsDao, modelService));
		//mapping.setOrder(Integer.MAX_VALUE);
		//mapping.setReplacedInterceptors(Collections.EMPTY_LIST);

		try
		{
			LOG.info("Removing interceptor for <{}>", type);
			// unregisterInterceptor is not available in Hybris 4
			final Method method = interceptorRegistry.getClass().getDeclaredMethod("unregisterInterceptor", mapping.getClass());
			method.invoke(interceptorRegistry, mapping);
			LOG.warn("Interceptor for type <{}> has been removed, restart server to complete operation", type);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to remove interceptor for type <{}>, restart server to complete operation", type);
			LOG.debug("Exception was: ", e);
		}
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DMI_UNSUPPORTED_METHOD", justification = "because I know better")
	public InterceptorRegistry getInterceptorRegistry()
	{
		if (_interceptorRegistry == null)
		{
			synchronized (_interceptorRegistryLock)
			{
				if (_interceptorRegistry == null)
				{
					_interceptorRegistry = lookupInterceptorRegistry();
				}
			}
		}
		return _interceptorRegistry;
	}

	/**
	 * Must be overwritten! Use &lt;lookup-method&gt; in spring.
	 */
	public InterceptorRegistry lookupInterceptorRegistry()
	{
		throw new UnsupportedOperationException(
				"please override JirafeInterceptorLoader.lookupInterceptorRegistry() or use <lookup-method>");
	}

	/**
	 * @param persistStrategy
	 *           the persistStrategy to set
	 */
	public void setPersistStrategy(final JirafeDataPersistStrategy persistStrategy)
	{
		this.persistStrategy = persistStrategy;
	}
}
