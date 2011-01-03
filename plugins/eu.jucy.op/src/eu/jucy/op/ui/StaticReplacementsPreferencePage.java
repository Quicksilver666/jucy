package eu.jucy.op.ui;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.OPI;

public class StaticReplacementsPreferencePage extends UCPrefpage {

	public StaticReplacementsPreferencePage() {
		super(OPI.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		ReplacementsEditor reps = new ReplacementsEditor("Static replacememnt",
				OPI.staticReplacements,getFieldEditorParent());
		addField(reps);
	}

}
