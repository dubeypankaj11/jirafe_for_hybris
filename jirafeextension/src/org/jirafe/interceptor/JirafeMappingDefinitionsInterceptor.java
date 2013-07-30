/**
 * 
 */
package org.jirafe.interceptor;

import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;

import org.jirafe.model.data.JirafeMappingDefinitionsModel;


/**
 * Intercepts Jirafe data mapping events. Allows for loading mappings during runtime via impex, ws, etc..
 * 
 * @author Larry Ramponi
 * 
 */
public class JirafeMappingDefinitionsInterceptor implements ValidateInterceptor
{

	private JirafeInterceptorLoader interceptorLoader;

	/**
	 * Loads a data mapping intercepter as it gets loaded to the database.
	 */
	@Override
	public void onValidate(final Object model, final InterceptorContext ctx) throws InterceptorException
	{
		interceptorLoader.registerInterceptor(((JirafeMappingDefinitionsModel) model).getType());
	}

	public void setJirafeInterceptorLoader(final JirafeInterceptorLoader interceptorLoader)
	{
		this.interceptorLoader = interceptorLoader;
	}

}
