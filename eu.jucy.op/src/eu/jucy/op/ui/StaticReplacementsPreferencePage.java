package eu.jucy.op.ui;

import eu.jucy.gui.settings.UCPrefpage;
import eu.jucy.op.PI;

public class StaticReplacementsPreferencePage extends UCPrefpage {

	public StaticReplacementsPreferencePage() {
		super(PI.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		ReplacementsEditor reps = new ReplacementsEditor("Static replacememnt",
				PI.staticReplacements,getFieldEditorParent());
		addField(reps);
	}

}
