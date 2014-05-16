/**
 * 
 */
package org.jirafe.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;

import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;


/**
 * @author dbrand
 * 
 */
@Component
public class JirafeScriptPool
{
	private static final Logger log = LoggerFactory.getLogger(JirafeScriptPool.class);

	@Resource
	JirafeJsonConverter jirafeJsonConverter;

	private ConcurrentLinkedQueue<Script> scriptPool = new ConcurrentLinkedQueue<Script>();

	public Script acquireScript()
	{
		final Script script = scriptPool.poll();
		if (script != null)
		{
			return script;
		}
		return newScript();
	}

	public void releaseScript(final Script script)
	{
		if (log.isDebugEnabled())
		{
			log.debug("{} items in pool", scriptPool.size());
		}
		final ConcurrentLinkedQueue<Script> returnPool = (ConcurrentLinkedQueue<Script>) script.getProperty("__scriptPool__");
		returnPool.add(script);
	}

	private Script newScript()
	{
		final Script script;
		final GroovyShell shell = new GroovyShell();
		try
		{
			final Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("Mapper.groovy"), "UTF-8");
			try
			{
				script = shell.parse(reader);
			}
			finally
			{
				reader.close();
			}
		}
		catch (final CompilationFailedException e)
		{
			log.error("Failed to load Mapper.groovy: ", e);
			return null;
		}
		catch (final IOException e)
		{
			log.error("Failed to load Mapper.groovy: ", e);
			return null;
		}
		final Binding binding = script.getBinding();
		binding.setVariable("dateFormat", UTCFormatter.getUTCFormatter());
		binding.setVariable("model", null);
		binding.setVariable("jirafeJsonConverter", jirafeJsonConverter);
		binding.setVariable("jirafe", script);
		// For backward compatibility
		binding.setVariable("jirafeModelToMapConverter", script);
		binding.setVariable("__mapCache__", new HashMap<String, Map>());
		binding.setVariable("__scriptPool__", scriptPool);
		return script;
	}

	public void reset()
	{
		scriptPool = new ConcurrentLinkedQueue<Script>();
	}

}
