package eu.jucy.op.ui;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.OPI;

public class CounterPreferencePage extends UCPrefpage {


	public CounterPreferencePage() {
		super(OPI.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		CounterFieldEditor cfe = new CounterFieldEditor("Counters",OPI.counters,getFieldEditorParent());
		addField(cfe);
	}

}
