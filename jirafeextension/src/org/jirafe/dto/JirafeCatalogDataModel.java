/**
 * 
 */
package org.jirafe.dto;

import org.jirafe.model.data.JirafeCatalogSyncDataModel;
import org.jirafe.model.data.JirafeDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author dbrand
 * 
 */
public class JirafeCatalogDataModel extends JirafeDataModel
{
	private static final Logger LOG = LoggerFactory.getLogger(JirafeCatalogDataModel.class);

	private final JirafeCatalogSyncDataModel header;

	/**
	 * @param header
	 */
	public JirafeCatalogDataModel(final JirafeCatalogSyncDataModel header)
	{
		super();
		this.header = header;
	}

	public void save()
	{
		// Don't persist these, just update header
		header.setLastModified(getModifiedtime());
		header.setLastPK(getPk());
	}
}
