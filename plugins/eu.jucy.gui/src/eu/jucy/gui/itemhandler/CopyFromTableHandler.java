package eu.jucy.gui.itemhandler;

import logger.LoggerFactory;
import helpers.GH;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.GuiHelpers;

public class CopyFromTableHandler extends AbstractHandler {
	
	private static Logger logger = LoggerFactory.make();
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		logger.info("executed copy");
		IStructuredSelection sel = (IStructuredSelection)HandlerUtil.getCurrentSelectionChecked(event);
		String s = GH.concat(sel.toList(), "\n");
		if (!GH.isNullOrEmpty(s)) {
			GuiHelpers.copyTextToClipboard(s);
		}
		return null;
	}

}
