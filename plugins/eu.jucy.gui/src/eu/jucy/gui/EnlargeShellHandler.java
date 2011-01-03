package eu.jucy.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import uc.DCClient;



/**
 * un-minimizes the shell
 * 
 * @author Quicksilver
 *
 */
public class EnlargeShellHandler extends AbstractHandler {

	public static final String COMMAND_ID = "eu.jucy.gui.maximizeGUI";
	
	public static final String PARAM_MAXIMIZE = "eu.jucy.gui.maximize";
	
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
	
		boolean maximize = Boolean.parseBoolean(event.getParameter(PARAM_MAXIMIZE));
		
		if (maximize) {
			Shell shell = window.getShell();
			boolean iconify = GUIPI.getBoolean(GUIPI.minimizeToTray);
			if (!shell.isVisible() || !iconify) {
				shell.setVisible(true);
				shell.setMinimized(false);
				shell.setActive();
				shell.setFocus();
				if (GUIPI.getBoolean(GUIPI.setAwayOnMinimize)) {
					ApplicationWorkbenchWindowAdvisor.get().setAway(false);
				}
			} 
		} else {
			mininmize(window);
		}
		
		return null;
	}
	
    private static void mininmize(IWorkbenchWindow window) {
    	
    	Shell shell = window.getShell();
    	if (!shell.getMinimized()) {
    		shell.setMinimized(true);
    	}
    	if (GUIPI.getBoolean(GUIPI.minimizeToTray) && shell.isVisible()) {
			shell.setVisible(false);
		}
    	DCClient dcc = ApplicationWorkbenchWindowAdvisor.get();
		if (!dcc.isAway() && GUIPI.getBoolean(GUIPI.setAwayOnMinimize)) {
			dcc.setAway(true);
		}
    }

}
