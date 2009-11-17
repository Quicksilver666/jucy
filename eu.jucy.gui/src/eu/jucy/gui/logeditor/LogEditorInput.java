package eu.jucy.gui.logeditor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import uc.IHub;


import eu.jucy.gui.Lang;
import eu.jucy.gui.texteditor.LabelViewer;


public class LogEditorInput implements IEditorInput {

private final LabelViewer source;
private final IHub hub;
	
	/**
	 * null for the default logeditor
	 * @param source
	 */
	public LogEditorInput(LabelViewer source, IHub hub) {
		this.source = source;
		this.hub = hub;
	}
	
	public LogEditorInput() {
		source = null;
		hub = null;
	}
	
	
	public boolean exists() {
		return false;
	}

	
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	
	public String getName() {
		if (hub == null) {
			return Lang.SystemLog;
		} else {
			return hub.getName()+" - Feed";
		}
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

	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final LogEditorInput other = (LogEditorInput) obj;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	public LabelViewer getSource() {
		return source;
	}


	
	


}
