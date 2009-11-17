package eu.jucy.gui.uploadqueue;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.jucy.gui.Lang;

public class UploadQueueEditorInput implements IEditorInput {

	
	public boolean exists() {
		return false;
	}

	
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	
	public String getName() {
		return Lang.UploadQueue;
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
