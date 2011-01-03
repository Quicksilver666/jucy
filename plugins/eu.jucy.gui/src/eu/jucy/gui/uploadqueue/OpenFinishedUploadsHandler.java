package eu.jucy.gui.uploadqueue;


import eu.jucy.gui.OpenEditorHandler;

public class OpenFinishedUploadsHandler extends OpenEditorHandler {

	public OpenFinishedUploadsHandler() {
		super(FinishedTransfersEditor.ID, new FinishedTransfersEditorInput(true));
	}

}
