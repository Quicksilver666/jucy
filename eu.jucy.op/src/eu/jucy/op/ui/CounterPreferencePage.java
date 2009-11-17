package eu.jucy.op.ui;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.PI;

public class CounterPreferencePage extends UCPrefpage {


	public CounterPreferencePage() {
		super(PI.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		CounterFieldEditor cfe = new CounterFieldEditor("Counters",PI.counters,getFieldEditorParent());
		addField(cfe);
	}

}
