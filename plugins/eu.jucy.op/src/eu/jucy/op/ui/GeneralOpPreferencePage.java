package eu.jucy.op.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.OPI;

public class GeneralOpPreferencePage extends UCPrefpage {

	public GeneralOpPreferencePage() {
		super(OPI.PLUGIN_ID);
	}


	@Override
	protected void createFieldEditors() {
		
		BooleanFieldEditor checkUsers = new BooleanFieldEditor(
				OPI.checkUsers,"Check users in enabled hubs",getFieldEditorParent());
		addField(checkUsers);
		
		StringFieldEditor protectedUsers = new StringFieldEditor(
				OPI.protectedUsersRegEx,"Protected users Regexp",getFieldEditorParent());
		addField(protectedUsers);
		
		IntegerFieldEditor parallelChecks = new IntegerFieldEditor(
				OPI.parallelChecks,"Number of parallel Filelist checks",getFieldEditorParent());
		parallelChecks.setValidRange(1, 100);
		addField(parallelChecks);

	}

}
