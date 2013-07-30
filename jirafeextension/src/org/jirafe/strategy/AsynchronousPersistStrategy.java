/**
 * 
 */
package org.jirafe.strategy;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.util.Config;

import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.jirafe.dto.JirafeDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


/**
 * Asynchronous implementation to persist data, freeing up the thread that is using it to continue. Helpful when being
 * called from an interceptor when non-blocking is preferred.
 * 
 * @author Larry Ramponi
 * 
 */
public class AsynchronousPersistStrategy extends BasePersistStrategy
{
	private final static Logger log = LoggerFactory.getLogger(AsynchronousPersistStrategy.class);
	private ThreadPoolTaskExecutor executor;
	private BlockingQueue<JirafeDataDto> queue;
	private Runnable persistTasks[];
	private final int threadCount = Config.getInt("jirafe.persistence.thread_count", 5);
	private final int waitTime = Config.getInt("jirafe.persistence.wait_time", 500);
	// Expire time is entered as minutes, but we want milliseconds
	private final long expireTime = (long) (Config.getDouble("jirafe.persistence.expire_time", 15) * 60000.0);

	@Override
	public void persist(final JirafeDataDto jirafeDataDto)
	{
		if (this.persistTasks == null)
		{
			init();
		}
		synchronized (queue)
		{
			if (!queue.contains(jirafeDataDto))
			{
				queue.add(jirafeDataDto);
			}
		}
	}

	/**
	 * Initializes the thread pool executor.
	 */
	public synchronized void init()
	{
		if (persistTasks != null)
		{
			// If the task is not null, another thread may have initialized.
			return;
		}
		// make sure enough threads are available
		if (executor.getCorePoolSize() < threadCount)
		{
			executor.setCorePoolSize(threadCount);
		}
		// get tenant from callers scope
		// If the perstsTasks is null, then it hasn't been initialized
		persistTasks = new Runnable[threadCount];
		for (int i = 0; i < threadCount; ++i)
		{
			persistTasks[i] = new PersistTask();
			executor.execute(persistTasks[i]);
		}
	}

	public void setBlockingQueue(final BlockingQueue<JirafeDataDto> queue)
	{
		this.queue = queue;
	}

	public void setExecutor(final ThreadPoolTaskExecutor executor)
	{
		this.executor = executor;
	}

	/**
	 * Runnable task.
	 * 
	 * @author Larry Ramponi
	 * 
	 */
	class PersistTask implements Runnable
	{
		@Override
		public void run()
		{
			JirafeDataDto jirafeDataDto;
			for (;;)
			{
				try
				{
					if ((jirafeDataDto = queue.take()) == null)
					{
						break;
					}
					log.debug("ASYNC - " + queue.size() + " items in queue.");
					final ItemModel itemModel = jirafeDataDto.getItemModel();
					if (modelService.isRemoved(itemModel))
					{
						log.debug("ASYNC - item {} was removed after being queued", itemModel.getPk());
						continue;
					}
					if ((new Date().getTime() - jirafeDataDto.getCreationTime().getTime()) > expireTime)
					{
						log.debug("ASYNC - item {} expired before save", itemModel.getPk());
						continue;
					}
					if (modelService.isModified(itemModel))
					{
						// Means that it hasn't been through the modelservice save process.
						// Add it back to the queue.
						// We hold it 
						log.debug("ASYNC - Item not ready, " + (waitTime > 0 ? "sleeping for " + waitTime + "ms" : ""));
						synchronized (queue)
						{
							if (!queue.contains(jirafeDataDto))
							{
								queue.add(jirafeDataDto);
							}
							if (waitTime > 0)
							{
								queue.wait(waitTime);
							}
						}
						continue;
					}
					try
					{
						// Do the persisting.
						doPersist(jirafeDataDto);
					}
					catch (final Exception e)
					{
						log.error("Failed to persist <{}> due to: {}", jirafeDataDto.getJirafeTypeCode(), e);
					}
				}
				catch (final InterruptedException e)
				{
					log.info("Thread interrupted, exiting.");
					//e.printStackTrace();
					break;
				}
				catch (final Throwable e)
				{
					// Log the error, but the thread must keep going
					log.error("Persistence exception: {}", e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

}
