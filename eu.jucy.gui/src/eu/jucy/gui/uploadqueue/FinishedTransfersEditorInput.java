package eu.jucy.gui.uploadqueue;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import uc.files.IUploadQueue;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;

public class FinishedTransfersEditorInput implements IEditorInput {

	private final boolean up;
	
	public FinishedTransfersEditorInput(boolean up) {
		this.up = up;
	}
	
	
	public boolean exists() {
		return false;
	}

	
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public IUploadQueue getInput() {
		return ApplicationWorkbenchWindowAdvisor.get().getUpDownQueue(up);
	}
	
	
	public String getName() {
		return up? Lang.FinishedUploads: Lang.FinishedDownloads;
	}

	
	public IPersistableElement getPersistable() {
		return null;
	}

	
	public String getToolTipText() {
		return getName();
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}

}
