package eu.jucy.gui.logviewer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class LogViewerEditorInput implements IEditorInput {

	
	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return "Log Viewer";
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

	public int hashcode() {
		return -1;
	}
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return getClass() == obj.getClass();
	}
}
