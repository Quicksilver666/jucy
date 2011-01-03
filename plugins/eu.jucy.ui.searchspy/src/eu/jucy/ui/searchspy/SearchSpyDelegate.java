package eu.jucy.ui.searchspy;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;



public class SearchSpyDelegate implements IWorkbenchWindowActionDelegate {

	
	private IWorkbenchWindow window;

	
	public void dispose() {}

	
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	
	public void run(IAction action) {
		SearchSpyEditorInput sei = new SearchSpyEditorInput();
		try {
			window.getActivePage().openEditor(sei, SearchSpyEditor.ID, true); 	
		} catch(PartInitException pie) {
			MessageDialog.openError(window.getShell(), "Error", "Error opening SearchSpy editor:" + pie.getMessage());
		}

	}

	
	public void selectionChanged(IAction action, ISelection selection) {}

}
