package eu.jucy.notepad.editors;

import helpers.GH;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import eu.jucy.gui.OpenEditorHandler;

public class OpenNotepad extends OpenEditorHandler {
	
	public static final String NOTEPAD_PARM = "NOTEPAD_PARM";
	
	public OpenNotepad() {
		super(Notepad.ID, new NotepadInput());
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String s = event.getParameter(NOTEPAD_PARM);
		if (!GH.isNullOrEmpty(s)) {
			this.input = new NotepadInput(s);
		}
		
		return super.execute(event);
	}
	
	
	
}
