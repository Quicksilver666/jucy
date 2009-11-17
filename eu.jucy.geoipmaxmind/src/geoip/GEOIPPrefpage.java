package geoip;


import org.eclipse.jface.preference.BooleanFieldEditor;

import eu.jucy.gui.settings.UCPrefpage;

public class GEOIPPrefpage extends UCPrefpage  {

	
	
	public GEOIPPrefpage() {
		super(GEOPref.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		BooleanFieldEditor useCityInformation = new BooleanFieldEditor(
				GEOPref.countryOnly,
				"Use only country-level information (City-level information uses about 30 MiB more RAM)",
				getFieldEditorParent());
		
		addField(useCityInformation);
		
	}

	
	
}
