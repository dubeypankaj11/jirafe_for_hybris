/**
 *
 */
package org.jirafe.dao;

import de.hybris.platform.core.PK;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.jirafe.model.data.JirafeCatalogSyncDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


/**
 * @author dbrand
 * 
 */
@Repository
public class JirafeSyncDataDao
{
	protected static final Logger LOG = LoggerFactory.getLogger(JirafeSyncDataDao.class);

	@Resource
	protected FlexibleSearchService flexibleSearchService;

	private static final String query = //
	"SELECT " + //
			"{" + JirafeCatalogSyncDataModel.PK + "}, " + //
			"{" + JirafeCatalogSyncDataModel.LASTMODIFIED + "}, " + //
			"{" + JirafeCatalogSyncDataModel.LASTPK + "} " + //
			"FROM {" + JirafeCatalogSyncDataModel._TYPECODE + "}";

	public JirafeCatalogSyncDataModel get()
	{
		final List<Object> result = flexibleSearchService.search(new FlexibleSearchQuery(query)).getResult();

		final JirafeCatalogSyncDataModel header;
		if (result != null && result.size() > 0)
		{
			header = (JirafeCatalogSyncDataModel) result.get(0);
		}
		else
		{
			header = new JirafeCatalogSyncDataModel();
			header.setLastModified(new Date());
			header.setLastPK(PK.BIG_PK);
		}

		return header;
	}
}
