/**
 * 
 */
package org.jirafe.cronjob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.servicelayer.ServicelayerTest;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.internal.model.ServicelayerJobModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.jirafe.model.cronjob.JirafeDataSyncCronJobModel;
import org.junit.Before;
import org.junit.Test;

import com.enterprisedt.util.debug.Logger;


/**
 * @author Larry Ramponi
 * 
 */
public class JirafeDataSyncJobPerformableTest extends ServicelayerTest
{
	private static final Logger LOG = Logger.getLogger(JirafeDataSyncJobPerformableTest.class);

	@Resource
	CronJobService cronJobService;

	@Resource
	ModelService modelService;

	@Resource
	FlexibleSearchService flexibleSearchService;

	List<ServicelayerJobModel> servicelayerJobModelList = Collections.EMPTY_LIST;
	ServicelayerJobModel servicelayerJobModel = null;
	JirafeDataSyncCronJobModel jirafeDataSyncCronJobModel;

	@Before
	public void setUp()
	{
		final ServicelayerJobModel sjm = new ServicelayerJobModel();
		sjm.setSpringId("jirafeDataSyncJob");

		try
		{
			servicelayerJobModel = flexibleSearchService.getModelByExample(sjm);
		}
		catch (final ModelNotFoundException e)
		{
			servicelayerJobModel = modelService.create(ServicelayerJobModel.class);
			servicelayerJobModel.setSpringId("jirafeDataSyncJob");
			servicelayerJobModel.setCode("jirafeDataSyncJob");
			modelService.save(servicelayerJobModel);
		}

		jirafeDataSyncCronJobModel = modelService.create(JirafeDataSyncCronJobModel.class);
		jirafeDataSyncCronJobModel.setActive(Boolean.TRUE);
		jirafeDataSyncCronJobModel.setJob(servicelayerJobModel);
		modelService.save(jirafeDataSyncCronJobModel);
	}

	@Test
	public void testPerformJob()
	{
		//Check if there is an instance of myJobPerformable
		assertNotNull("***************No performable with springID *jirafeDataSyncJob* found perhaps you have to "
				+ "Update your JunitTenant to let create an instance!", servicelayerJobModel);
	}

	@Test
	public void testExecuteThePerformable()
	{
		//Check if setup works correctly
		assertNotNull("***************The in set up created CronJob is null?", jirafeDataSyncCronJobModel);

		//Perform the CronJob once for the test
		cronJobService.performCronJob(jirafeDataSyncCronJobModel);

		//Wait for the result to be written
		try
		{
			Thread.sleep(2000);
		}
		catch (final InterruptedException e)
		{
			e.printStackTrace();
		}

		LOG.info("*************** lets wait 2 seconds for the result  ***************");

		//Test if the job was executed successfully, if it fails here then try to extend the time
		assertEquals("*************** The perfromable has not finished successfull or more wait is required on this mashine!",
				CronJobResult.SUCCESS, jirafeDataSyncCronJobModel.getResult());

	}
}
