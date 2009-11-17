package eu.jucy.gui;

import helpers.GH;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import uc.FavHub;

public class QuickConnectHandler extends AbstractHandler implements IHandler {


	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		InputDialog dialog = new InputDialog(window.getShell(),Lang.QuickConnect,
				Lang.Address,"",(IInputValidator)null);
		dialog.setBlockOnOpen(true);
		
		if (dialog.open() != Dialog.CANCEL) {
			String address = dialog.getValue();
			if (!GH.isEmpty(address)) {
				new FavHub(address).connect(ApplicationWorkbenchWindowAdvisor.get());
			}
		}
		
		return null;
	}

}
