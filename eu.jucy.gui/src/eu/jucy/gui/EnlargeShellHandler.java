package eu.jucy.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;



/**
 * un-minimizes the shell
 * 
 * @author Quicksilver
 *
 */
public class EnlargeShellHandler extends AbstractHandler {

	public static final String CommandID = "eu.jucy.gui.maximizeGUI";
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchWindow[] windows;
		if (window == null) {
			windows = PlatformUI.getWorkbench().getWorkbenchWindows();	//may work better... might return non active windows..
			if (windows.length == 0) {
				throw new ExecutionException("no Windows found");
			} else {
				window = windows[0];
			}
		}
	
		
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
