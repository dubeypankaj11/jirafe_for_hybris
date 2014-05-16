/**
 *
 */
package org.jirafe.dto;

import de.hybris.platform.core.model.ItemModel;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.jirafe.converter.JirafeConvertException;
import org.jirafe.converter.JirafeJsonConverter;
import org.jirafe.dao.JirafeMappingsDao;
import org.jirafe.model.data.JirafeDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @author dbrand
 * 
 */
@Component
public class JirafeTempDataModelFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(JirafeTempDataModelFactory.class);

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;
	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	public JirafeDataModel fromItemModel(final String mappedType, final ItemModel itemModel, final String site)
	{
		try
		{
			final Map map = jirafeJsonConverter.toMap(new JirafeDataDto(mappedType, itemModel, site));
			final String json = jirafeJsonConverter.toJson(map);
			final JirafeTempDataModel jirafeTempDataModel = new JirafeTempDataModel();
			jirafeTempDataModel.setSite(site);
			jirafeTempDataModel.setType(mappedType);
			jirafeTempDataModel.setData(json);
			return jirafeTempDataModel;
		}
		catch (final JirafeConvertException e)
		{
			LOG.error("failed to map item {}", itemModel.getPk());
			LOG.debug("", e);
			return null;
		}
	}

	public List<JirafeDataModel> fromItemModel(final ItemModel itemModel)
	{
		final String itemType = itemModel.getItemtype();
		final String mappedType = jirafeMappingsDao.getMappedType(itemModel);
		if (mappedType == null)
		{
			LOG.debug("Skipping unmapped type {}", itemType);
			return null;
		}
		final List<JirafeDataModel> ret = new LinkedList<JirafeDataModel>();
		try
		{
			final String[] sites = jirafeJsonConverter.getSites(itemModel);
			for (final String site : sites)
			{
				ret.add(fromItemModel(mappedType, itemModel, site));
			}
			return ret;
		}
		catch (final JirafeConvertException e)
		{
			LOG.error("failed to map item {}", itemModel.getPk());
			LOG.debug("", e);
			return null;
		}
	}
}
