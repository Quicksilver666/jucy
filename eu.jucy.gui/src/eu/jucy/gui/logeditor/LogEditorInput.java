package eu.jucy.gui.logeditor;

import uc.IHub;


import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditorInput;
import eu.jucy.gui.texteditor.LabelViewer;


public class LogEditorInput extends UCEditorInput {

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
	
	

	
	public String getName() {
		if (hub == null) {
			return Lang.SystemLog;
		} else {
			return hub.getName()+" - Feed";
		}
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
