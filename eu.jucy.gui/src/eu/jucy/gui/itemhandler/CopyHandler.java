package eu.jucy.gui.itemhandler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.GuiHelpers;

public class CopyHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection)HandlerUtil.getCurrentSelection(event);
		String s = sel.getFirstElement().toString();
		GuiHelpers.copyTextToClipboard(s);
		
		return null;
	}

}
