package eu.jucy.connectiondebugger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.texteditor.hub.HubEditor;

import uc.IHasUser;
import uc.IHub;
import uc.IUser;

public class OpenDebuggerViewHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection)HandlerUtil.getCurrentSelectionChecked(event);
		
		if (sel.size() == 1 && sel.getFirstElement() instanceof IHasUser) {
			IUser usr =((IHasUser)sel.getFirstElement()).getUser();
			 openDebugger(usr,usr.getUserid().toString(),
					 HandlerUtil.getActiveWorkbenchWindowChecked(event));
		}
		return null;
	}
	
	public static class OpenDebuggerViewHubHandler extends AbstractHandler {

		public Object execute(ExecutionEvent event) throws ExecutionException {
			IEditorPart part = HandlerUtil.getActiveEditor(event);
			if (part instanceof HubEditor) {
				IHub hub = ((HubEditor)part).getHub();
				openDebugger(hub,hub.getFavHub().getHubaddy(),part.getSite().getWorkbenchWindow());
			}
			return null;
		}
	}
	
	
	public static void openDebugger(Object o,String id,IWorkbenchWindow window) {
		IWorkbenchPage page = window.getActivePage();
		boolean exists	= page.findViewReference(DebuggerView.ID,id) != null;
		if (exists) {
			DebuggerView.addInput(id, o);
		}
		
		try {
			page.showView(DebuggerView.ID,id,
					exists? IWorkbenchPage.VIEW_ACTIVATE : IWorkbenchPage.VIEW_CREATE );
		      
		} catch (PartInitException e){
		     MessageDialog.openError(window.getShell(),"Error" ,e.toString());
		}
	}

}
