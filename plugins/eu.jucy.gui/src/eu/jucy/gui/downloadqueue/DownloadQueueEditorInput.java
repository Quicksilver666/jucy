package eu.jucy.gui.downloadqueue;


import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditorInput;

import uc.files.downloadqueue.DownloadQueue;

public class DownloadQueueEditorInput extends UCEditorInput {

	
	public DownloadQueueEditorInput() {}
	
	public DownloadQueue getDQ(){
		return ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue();
	}
	

	public String getName() {
		return Lang.DownloadQueue;
	}


}
