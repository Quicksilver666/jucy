package eu.jucy.notepad.editors;

import eu.jucy.gui.OpenEditorHandler;

public class OpenNotepad extends OpenEditorHandler {
	
	public OpenNotepad() {
		super(Notepad.ID, new NotepadInput());
	}
}
