package eu.jucy.gui.uploadqueue;

import eu.jucy.gui.OpenEditorHandler;

public class OpenFinishedDownloadsHandler extends OpenEditorHandler {

	public OpenFinishedDownloadsHandler() {
		super(FinishedTransfersEditor.ID2, new FinishedTransfersEditorInput(false));
	}
	
}
