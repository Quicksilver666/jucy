package eu.jucy.gui.downloadqueue;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;

import uc.files.downloadqueue.DownloadQueue;

public class DownloadQueueEditorInput implements IEditorInput {

	
	public DownloadQueueEditorInput() {}
	
	public DownloadQueue getDQ(){
		return ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue();
	}
	
	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return Lang.DownloadQueue;
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


	@Override
	public int hashCode() {
		return getClass().hashCode();
	}


	@Override
	public boolean equals(Object obj) {
		return getClass().equals(obj.getClass());
	}
	
	
	
	

}
