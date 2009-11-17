package eu.jucy.gui.settings;

import org.eclipse.jface.preference.ColorFieldEditor;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;


public class FileColouringPrefpage extends UCPrefpage {

	public FileColouringPrefpage() {
		super(Application.PLUGIN_ID);
	}
	
	@Override
	protected void createFieldEditors() {
		String[] pref = new String[]{GUIPI.fileInDownloadCol,GUIPI.fileInShareCol,GUIPI.fileMultiUserCol,GUIPI.fileDefaultCol};
		String[] lang = new String[]{Lang.FileInDownloadCol,Lang.FileInShareCol,Lang.FileMultiUserCol,Lang.FileDefaultCol};
		for (int i =0 ; i < pref.length ; i++) {
			ColorFieldEditor cfe = new ColorFieldEditor(pref[i],lang[i],getFieldEditorParent());
			addField(cfe);
		}
	}

}
