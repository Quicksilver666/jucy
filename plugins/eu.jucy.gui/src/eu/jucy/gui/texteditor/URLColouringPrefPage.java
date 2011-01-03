package eu.jucy.gui.texteditor;


import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;

import uc.DCClient;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;
import eu.jucy.gui.settings.UCPrefpage;

public class URLColouringPrefPage extends UCPrefpage {

	public URLColouringPrefPage() {
		super(Application.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new ColorFieldEditor(GUIPI.urlModCol,Lang.URLColour,getFieldEditorParent()));
		
		addField(new FontFieldEditor(GUIPI.urlModFont,Lang.URLFont,DCClient.LONGVERSION,getFieldEditorParent()));
		
		//addField(new BooleanFieldEditor(GUIPI.urlModUnderline,Lang.URLUnderline,getFieldEditorParent()));
		
	}

}
