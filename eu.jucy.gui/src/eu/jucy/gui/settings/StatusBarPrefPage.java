package eu.jucy.gui.settings;

import org.eclipse.jface.preference.BooleanFieldEditor;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;

public class StatusBarPrefPage extends UCPrefpage {

	
	public StatusBarPrefPage() {
		super(Application.PLUGIN_ID);
	}
	
	@Override
	protected void createFieldEditors() {
		
		String[] prefs = new String[] {
				GUIPI.shareSizeContrib,GUIPI.hubsContrib,
				GUIPI.slotsContrib,GUIPI.downContrib,
				GUIPI.upContrib,GUIPI.downSpeedContrib,
				GUIPI.upSpeedContrib,GUIPI.connectionStatusContrib};
		
		String[] labels = new String[] {Lang.ShowSharesize,Lang.ShowHubs,
				Lang.ShowSlots,Lang.ShowDownTotal,
				Lang.ShowUpTotal,Lang.ShowDownSpeed,
				Lang.ShowUpSpeed,Lang.ShowConnStatus};
		
		for (int i =0 ; i < prefs.length; i++) {
			BooleanFieldEditor bfe = new BooleanFieldEditor(prefs[i],labels[i],getFieldEditorParent());
			addField(bfe);
		}
		

	}

}
