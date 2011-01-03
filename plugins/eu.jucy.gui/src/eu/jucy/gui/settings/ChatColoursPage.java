package eu.jucy.gui.settings;

import org.eclipse.jface.preference.ColorFieldEditor;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;


public class ChatColoursPage extends UCPrefpage {

	
	
	public ChatColoursPage() {
		super(Application.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		String[] prefs = new String[] {//GUIPI.chatMessageCol,
				GUIPI.joinPartMessageCol
				,GUIPI.statusMessageCol,GUIPI.oldMessageCol};
		String[] names = new String[] { //TODO internationalisation..
				GUIPI.joinPartMessageCol
				,GUIPI.statusMessageCol,GUIPI.oldMessageCol};
		for (int i=0; i < prefs.length;i++ ) {
			ColorFieldEditor window = new ColorFieldEditor(prefs[i],names[i],getFieldEditorParent());
			addField(window);
		}

	}

}
