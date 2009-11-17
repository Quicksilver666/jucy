package eu.jucy.gui.settings;

import org.eclipse.jface.preference.BooleanFieldEditor;

import eu.jucy.gui.Lang;

import uc.PI;

public class MiscPage extends UCPrefpage {



	public MiscPage() {
	}

	@Override
	protected void createFieldEditors() {
		
		BooleanFieldEditor bfe = new BooleanFieldEditor(PI.checkForUpdates,Lang.AutomaticallyCheckForUpdates,getFieldEditorParent());
		addField(bfe);
		
		BooleanFieldEditor bfe2 = new BooleanFieldEditor(PI.autoSearchForAlternates,Lang.AutomaticallySearchForAlternates,getFieldEditorParent());
		addField(bfe2);
		
		BooleanFieldEditor bfe3 = new BooleanFieldEditor(
				PI.autoFollowRedirect,Lang.AutomaticallyFollowRedirects,getFieldEditorParent());
		addField(bfe3);
	}

}
