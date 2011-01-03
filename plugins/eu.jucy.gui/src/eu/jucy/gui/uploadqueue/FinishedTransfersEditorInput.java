package eu.jucy.gui.uploadqueue;


import uc.files.IUploadQueue;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditorInput;

public class FinishedTransfersEditorInput extends UCEditorInput {

	private final boolean up;
	
	public FinishedTransfersEditorInput(boolean up) {
		this.up = up;
	}
	


	public IUploadQueue getInput() {
		return ApplicationWorkbenchWindowAdvisor.get().getUpDownQueue(up);
	}
	
	
	public String getName() {
		return up? Lang.FinishedUploads: Lang.FinishedDownloads;
	}




	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (up ? 1231 : 1237);
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FinishedTransfersEditorInput other = (FinishedTransfersEditorInput) obj;
		if (up != other.up)
			return false;
		return true;
	}
	
	

}
