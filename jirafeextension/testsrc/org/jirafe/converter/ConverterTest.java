/**
 * 
 */
package org.jirafe.converter;

import static org.junit.Assert.assertNotNull;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.ServicelayerTransactionalTest;

import javax.annotation.Resource;

import org.jirafe.dto.JirafeDataDto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author alex
 * 
 */
@IntegrationTest
public class ConverterTest extends ServicelayerTransactionalTest
{
	private final static Logger log = LoggerFactory.getLogger(ConverterTest.class);

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;

	@Test
	public void testConvert() throws JirafeConvertException
	{
		final ItemModel model = createTestOrder();

		assertNotNull(jirafeJsonConverter.toJson(new JirafeDataDto("Order", model).getMap()));
	}

	ItemModel createTestOrder()
	{
		final OrderModel order = new OrderModel();

		return order;
	}

}
