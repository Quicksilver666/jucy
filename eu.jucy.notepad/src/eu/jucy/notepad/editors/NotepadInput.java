package eu.jucy.notepad.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class NotepadInput implements IEditorInput {

	
	public boolean exists() {
		return false;
	}

	
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	
	public String getName() {
		return Messages.getString("NotepadInput"); //$NON-NLS-1$
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
