/**
 * 
 */
package org.jirafe.dao;

import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Calendar;

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


	/**
	 * 
	 */
	@Override
	public JirafeDataModel save(final String jirafeTypeCode, final String pk, final String json, final Boolean isRemove)
	{
		final JirafeDataModel model;

		model = new JirafeDataModel();
		model.setType(jirafeTypeCode);
		model.setTypePK(pk);
		model.setData(json);
		model.setIsRemove(isRemove);
		model.setTimestamp(Calendar.getInstance().getTime());
		modelService.save(model);
		return model;
	}

}
