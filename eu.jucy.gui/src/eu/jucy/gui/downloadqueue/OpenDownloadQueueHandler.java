/**
 * 
 */
package eu.jucy.gui.downloadqueue;

import eu.jucy.gui.OpenEditorHandler;

public class OpenDownloadQueueHandler extends OpenEditorHandler {
	
	public OpenDownloadQueueHandler() {
		super(DownloadQueueEditor.ID, new DownloadQueueEditorInput());
	}
	
}