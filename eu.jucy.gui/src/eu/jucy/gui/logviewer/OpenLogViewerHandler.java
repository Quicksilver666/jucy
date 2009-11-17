package eu.jucy.gui.logviewer;



import eu.jucy.gui.OpenEditorHandler;

public class OpenLogViewerHandler extends OpenEditorHandler {

	public OpenLogViewerHandler() {
		super(LogViewerEditor.ID, new LogViewerEditorInput());
	}

}
