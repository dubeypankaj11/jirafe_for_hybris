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
import org.jirafe.model.data.JirafeCatalogSyncDataModel;
import org.jirafe.model.data.JirafeDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @author dbrand
 * 
 */
@Component
public class JirafeCatalogDataModelFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(JirafeCatalogDataModelFactory.class);

	@Resource
	private JirafeJsonConverter jirafeJsonConverter;
	@Resource
	private JirafeMappingsDao jirafeMappingsDao;

	public List<JirafeDataModel> fromItemModel(final JirafeCatalogSyncDataModel header, final ItemModel itemModel)
	{
		final String itemType = itemModel.getItemtype();
		final String mappedType = jirafeMappingsDao.getMappedType(itemModel);
		if (mappedType == null)
		{
			LOG.debug("Skipping unmapped type {}", itemType);
			return null;
		}
		Map map;
		try
		{
			map = jirafeJsonConverter.toMap(new JirafeDataDto(mappedType, itemModel));
		}
		catch (final JirafeConvertException e)
		{
			LOG.error("Catalog sync: failed to map item {}.", itemModel.getPk());
			LOG.debug("", e);
			return null;
		}
		final String json = jirafeJsonConverter.toJson(map);
		final String[] sites = jirafeJsonConverter.getSites(map);
		final List<JirafeDataModel> ret = new LinkedList<JirafeDataModel>();
		for (final String site : sites)
		{
			final JirafeCatalogDataModel jirafeCatalogDataModel = new JirafeCatalogDataModel(header);
			jirafeCatalogDataModel.setSite(site);
			jirafeCatalogDataModel.setType(mappedType);
			jirafeCatalogDataModel.setData(json);
			ret.add(jirafeCatalogDataModel);
		}

		return ret;
	}
}
