package eu.jucy.gui.itemhandler;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import uc.IUser;
import uc.Population;
import uihelpers.SUIJob;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.filelist.FilelistEditor;
import eu.jucy.gui.filelist.FilelistEditorInput;

public class FilelistHandler extends AbstractHandler {

	public static void openFilelist(final IUser usr) {
		if (usr != null && usr.getFilelistDescriptor() != null) {
			new SUIJob() {
				public void run(){
					FilelistEditorInput fei = new FilelistEditorInput(usr.getFilelistDescriptor());
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					try {
						window.getActivePage().openEditor(fei,FilelistEditor.ID);

					} catch(PartInitException pie) {
						MessageDialog.openError(window.getShell(), "Error", "Error opening Filelisteditor:" + pie.getMessage());
					}
				}
			}.schedule();
		}
	}
	

	public static final String OPEN_OWN_FILELIST = "OPEN_OWN_FILELIST"; 

	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		
	
		//if no user is provided .. open a fileDialog to open a stored filelist..
		boolean ownfl = Boolean.parseBoolean(event.getParameter(OPEN_OWN_FILELIST));
		IUser usr = null;
		if (!ownfl) {
		
			FileDialog fd = new FileDialog(HandlerUtil.getActiveShellChecked(event),SWT.OPEN);
			fd.setFilterExtensions(new String[]{"*.xml.bz2","*.xml"});
			fd.setFilterPath(uc.PI.getFileListPath().getPath());
		
			//fd.setFilterPath(string)
			String file = fd.open();
			if (file != null) {
				File f = new File(file);
				if (f.isFile()) {
					Population pop = ApplicationWorkbenchWindowAdvisor.get().getPopulation();
					usr = pop.getUserForFilelistfile(f);
				}
			}
		} else {
			usr = ApplicationWorkbenchWindowAdvisor.get().getFilelistself();
		}
		openFilelist(usr);
		
		
		return null;
	}
	
	public static class RefreshFilelistHandler extends AbstractHandler {

		public Object execute(ExecutionEvent event) throws ExecutionException {
			ApplicationWorkbenchWindowAdvisor.get().refreshFilelist();
			return null;
		}
		
	}


}
