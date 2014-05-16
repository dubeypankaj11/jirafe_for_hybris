/**
 * 
 */
package org.jirafe.hmc.administration;

import de.hybris.platform.hmc.TreeLeafChip;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;

import org.apache.log4j.Logger;


/**
 * @author dbrand
 * 
 */
public class SyncTreeNodeChip extends TreeLeafChip
{
	private static final Logger LOG = Logger.getLogger(SyncTreeNodeChip.class.getName());

	private final SyncDisplayChip displayChip;

	/**
	 * @param displayState
	 * @param parent
	 */
	public SyncTreeNodeChip(final DisplayState displayState, final Chip parent)
	{
		super(displayState, parent);
		displayChip = new SyncDisplayChip(displayState, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.hmc.AbstractExplorerMenuTreeNodeChip#getDisplayChip(de.hybris.platform.hmc.webchips.Chip)
	 */
	@Override
	protected Chip getDisplayChip(final Chip chip)
	{
		return displayChip;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.hmc.AbstractTreeNodeChip#getIcon()
	 */
	@Override
	public String getIcon()
	{
		return "images/icons/t_undefined.gif";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.hmc.AbstractTreeNodeChip#getName()
	 */
	@Override
	public String getName()
	{
		return getLocalizedString("sync.treenode.name");
	}

}
