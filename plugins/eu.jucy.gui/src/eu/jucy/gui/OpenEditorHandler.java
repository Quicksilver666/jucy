package eu.jucy.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;



public abstract class OpenEditorHandler extends AbstractHandler {

	private final String editorID;
	protected IEditorInput input;
	
	
	protected OpenEditorHandler(String editorID,IEditorInput input) {
		this.editorID = editorID;
		this.input = input;
		Assert.isNotNull(input);
		Assert.isNotNull(editorID);
	}


	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		try{
			window.getActivePage().openEditor(input, editorID, true); 	
		} catch(PartInitException pie){
			MessageDialog.openError(window.getShell(), "Error", "Error opening "+input.getName()+":" + pie.getMessage());
		}
		return null;
	}
	
}
