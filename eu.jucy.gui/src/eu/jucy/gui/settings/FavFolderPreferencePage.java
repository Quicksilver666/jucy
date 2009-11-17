package eu.jucy.gui.settings;

import uc.PI;


public class FavFolderPreferencePage extends UCPrefpage {


	@Override
	protected void createFieldEditors() {
		FavoriteDirsFieldEditor fdfe = new FavoriteDirsFieldEditor("FavFolders",PI.favDirs,getFieldEditorParent());
		addField(fdfe);
	}

}
