/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.util.Config;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jirafe.enums.JirafeDataStatus;
import org.jirafe.model.data.JirafeDataModel;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Uses the hybris {@code ModelService} to do persist actions of the {@code JirafeDataModel}.
 * 
 * @author Larry Ramponi
 * 
 */
public class ModelJirafeDataDao implements JirafeDataDao
{

	@Autowired
	private ModelService modelService;
	@Autowired
	private FlexibleSearchService flexibleSearchService;

	private static final String queryString = //
	"SELECT {" + JirafeDataModel.PK + "} " + //
			"FROM {" + JirafeDataModel._TYPECODE + "} " + //
			"WHERE {" + JirafeDataModel.TYPEPK + "} = ?pk " + //
			"AND {" + JirafeDataModel.SITE + "} = ?site " + //
			"AND {" + JirafeDataModel.STATUS + "} IN (?status) " + //
			"ORDER BY {" + JirafeDataModel.TIMESTAMP + "} DESC";


	/**
	 * 
	 */
	@Override
	public JirafeDataModel save(final String jirafeTypeCode, final String pk, final String json, final String site,
			final Boolean isRemove)
	{
		final Set trackAll = new HashSet(Arrays.asList(StringUtils.split(
				Config.getString("jirafe.jirafeDataSync.trackAll", "Cart,Order"), ',')));

		JirafeDataModel model = new JirafeDataModel();
		if (!trackAll.contains(jirafeTypeCode))
		{
			try
			{
				final FlexibleSearchQuery query = new FlexibleSearchQuery(queryString);
				query.setStart(0);
				query.setCount(1);
				query.addQueryParameter("pk", pk);
				query.addQueryParameter("site", site);

				final Set includedTypes = new HashSet();
				includedTypes.add(JirafeDataStatus.valueOf("NEW"));
				includedTypes.add(JirafeDataStatus.valueOf("NOT_AUTHORIZED"));
				query.addQueryParameter("status", includedTypes);

				final JirafeDataModel existing = (JirafeDataModel) flexibleSearchService.search(query).getResult().get(0);
				// We'll update the existing item
				model = existing;
				model.setStatus(JirafeDataStatus.NEW);
			}
			catch (final Exception e)
			{
				// No problem, create a new one
			}
		}
		model.setType(jirafeTypeCode);
		model.setTypePK(pk);
		model.setData(json);
		model.setSite(site);
		model.setIsRemove(isRemove);
		model.setTimestamp(Calendar.getInstance().getTime());
		modelService.save(model);
		return model;
	}

}
