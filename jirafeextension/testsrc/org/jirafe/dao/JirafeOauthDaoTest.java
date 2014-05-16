/**
 * 
 */
package org.jirafe.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.servicelayer.ServicelayerTransactionalTest;
import de.hybris.platform.servicelayer.model.ModelService;

import javax.annotation.Resource;

import org.jirafe.model.data.JirafeOAuthConfigModel;
import org.junit.Test;


/**
 * @author alex
 * 
 */
@IntegrationTest
public class JirafeOauthDaoTest extends ServicelayerTransactionalTest
{

	@Resource
	JirafeOAuth2SessionConfigDao jirafeOAuth2SessionConfigDao;
	@Resource
	private ModelService modelService;

	@Test
	public void testSaveAndLoad()
	{
		// Need to run both in one test so no rollback in between
		testSave();
		testLoad();
	}

	public void testSave()
	{

		JirafeOAuthConfigModel model = modelService.create(JirafeOAuthConfigModel.class);

		model.setClientId("1dc8cfa27935d6440219");
		model.setSiteId("d440bcb6-fc18-40fc-af08-20ad41f4f250");
		model.setClientSecret("3be4f33dacaedcfb3ab2160c5406dfd3adc74a8d");
		model.setRefreshToken("testToken");

		model = jirafeOAuth2SessionConfigDao.saveSessionConfig(model);

		assertNotNull(model);
		assertNotNull(model.getTimestamp());
	}

	public void testLoad()
	{
		final JirafeOAuthConfigModel model = jirafeOAuth2SessionConfigDao.getSessionConfig();

		assertNotNull(model);
		assertNotNull(model.getTimestamp());
		assertEquals(model.getClientId(), "1dc8cfa27935d6440219");
	}

}
