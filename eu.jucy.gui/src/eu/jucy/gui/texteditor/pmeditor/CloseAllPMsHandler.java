package eu.jucy.gui.texteditor.pmeditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class CloseAllPMsHandler extends AbstractHandler implements IHandler {

	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		for (IEditorReference editor : window.getActivePage().getEditorReferences()) {
			IEditorPart edi =editor.getEditor(false);
			if (edi instanceof PMEditor) {
				edi.getEditorSite().getPage().closeEditor(edi, false);
			}
		}
		
		return null;
	}

}
