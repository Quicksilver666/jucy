/**
 * 
 */
package eu.jucy.gui.logeditor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import uc.IHub;

import eu.jucy.gui.OpenEditorHandler;
import eu.jucy.gui.texteditor.LabelViewer;

public class OpenLogEditorHandler extends OpenEditorHandler {
	
	public OpenLogEditorHandler() {
		super(LogEditor.ID, new LogEditorInput());
	}
	
	public static void openSystemLogEditor() {
		OpenLogEditor(new LogEditorInput());
	}
	
	public static void openFeedLogEditor(LabelViewer viewer,IHub hub) {
		OpenLogEditor(new LogEditorInput(viewer,hub));
	}
	
	private static void OpenLogEditor(LogEditorInput lei) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {
			window.getActivePage().openEditor(lei, LogEditor.ID);
		} catch(PartInitException pie) {
			MessageDialog.openError(window.getShell(), "Error", "Error opening LogEditor:" + pie.getMessage());
		}
	}
}