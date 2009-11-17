package eu.jucy.op.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.PI;

public class GeneralOpPreferencePage extends UCPrefpage {

	public GeneralOpPreferencePage() {
		super(PI.PLUGIN_ID);
	}


	@Override
	protected void createFieldEditors() {
		
		BooleanFieldEditor checkUsers = new BooleanFieldEditor(
				PI.checkUsers,"Check users in enabled hubs",getFieldEditorParent());
		addField(checkUsers);
		
		StringFieldEditor protectedUsers = new StringFieldEditor(
				PI.protectedUsersRegEx,"Protected users Regexp",getFieldEditorParent());
		addField(protectedUsers);
		
		IntegerFieldEditor parallelChecks = new IntegerFieldEditor(
				PI.parallelChecks,"Number of parallel Filelist checks",getFieldEditorParent());
		parallelChecks.setValidRange(1, 100);
		addField(parallelChecks);

	}

}
