/**
 * 
 */
package org.jirafe.hmc.administration;

import de.hybris.platform.hmc.TreeLeafChip;
import de.hybris.platform.hmc.webchips.AbstractChip;
import de.hybris.platform.hmc.webchips.Chip;
import de.hybris.platform.hmc.webchips.DisplayState;

import org.apache.log4j.Logger;


/**
 * @author dbrand
 * 
 */
public class StatusTreeNodeChip extends TreeLeafChip
{
	private static final Logger LOG = Logger.getLogger(StatusTreeNodeChip.class.getName());

	private final AbstractChip displayChip;

	/**
	 * @param displayState
	 * @param parent
	 */
	public StatusTreeNodeChip(final DisplayState displayState, final Chip parent)
	{
		super(displayState, parent);
		displayChip = new StatusDisplayChip(displayState, this);
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
		return "images/search.gif";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.hybris.platform.hmc.AbstractTreeNodeChip#getName()
	 */
	@Override
	public String getName()
	{
		return getLocalizedString("status.treenode.name");
	}

}
