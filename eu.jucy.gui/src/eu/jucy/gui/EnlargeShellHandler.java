package eu.jucy.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;



/**
 * un-minimizes the shell
 * 
 * @author Quicksilver
 *
 */
public class EnlargeShellHandler extends AbstractHandler {

	public static final String CommandID = "eu.jucy.gui.maximizeGUI";
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = //HandlerUtil.getActiveWorkbenchWindowChecked(event);
		PlatformUI.getWorkbench().getWorkbenchWindows()[0];	//may work better... might return non active windows..

		
		Shell shell = window.getShell();
		boolean iconify = GUIPI.getBoolean(GUIPI.minimizeToTray);
		if (!shell.isVisible() ||!iconify ) {
			shell.setVisible(true);
			shell.setMinimized(false);
			shell.setActive();
			shell.setFocus();
			if (GUIPI.getBoolean(GUIPI.setAwayOnMinimize)) {
				ApplicationWorkbenchWindowAdvisor.get().setAway(false);
			}
		} 
		
		return null;
	}

}
