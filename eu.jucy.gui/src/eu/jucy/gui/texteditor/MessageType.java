package eu.jucy.gui.texteditor;

import org.eclipse.swt.graphics.Color;

import eu.jucy.gui.GUIPI;

public enum MessageType {
	
	OLD(GUIPI.oldMessageCol),
	CHAT(null),
	STATUS(GUIPI.statusMessageCol),
	JOINPART(GUIPI.joinPartMessageCol);
	
	
	private final String prefID;
	
	private MessageType(String prefID) {
		this.prefID = prefID;
	}
	public Color getColour() {
		if (prefID == null) {
			return null;
		}
		return GUIPI.getColor(prefID);
	}
}
