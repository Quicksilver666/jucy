package eu.jucy.op.ui;

import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.PI;

public class OpADLPreferencePage extends UCPrefpage implements
		IWorkbenchPreferencePage {

	public OpADLPreferencePage() {
		super(PI.PLUGIN_ID);
	}



	@Override
	protected void createFieldEditors() {
		OpADLFieldEditor oafe = new OpADLFieldEditor("OP ADL Search",PI.opADLEntries, getFieldEditorParent());
		addField(oafe);
	}

}
