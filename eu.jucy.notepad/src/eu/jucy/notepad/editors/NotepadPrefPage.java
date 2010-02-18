package eu.jucy.notepad.editors;

import org.eclipse.jface.preference.IntegerFieldEditor;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.notepad.Activator;
import eu.jucy.notepad.NPI;

public class NotepadPrefPage extends UCPrefpage {

	
	
	public NotepadPrefPage() {
		super(NPI.PLUGIN_ID);
	}


	@Override
	protected void createFieldEditors() {
		IntegerFieldEditor nrNotepads = 
			new IntegerFieldEditor(NPI.NR_OF_NOTEPADS, "How many Notepads?", getFieldEditorParent());
		nrNotepads.setValidRange(0, Activator.MAX_NOTEPADS);
		addField(nrNotepads);

	}

}
