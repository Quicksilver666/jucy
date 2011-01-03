package eu.jucy.gui.uploadqueue;



import eu.jucy.gui.OpenEditorHandler;

public class OpenUploadQueueHandler extends OpenEditorHandler {

	public OpenUploadQueueHandler() {
		super(UploadQueueEditor.ID, new UploadQueueEditorInput());
	}

	
}
