package eu.jucy.gui.settings;



import org.eclipse.jface.preference.IntegerFieldEditor;

import eu.jucy.gui.Lang;


import uc.PI;

public class SharingPreferencePage extends UCPrefpage {
	
	public SharingPreferencePage(){
		super(PI.PLUGIN_ID);
	}
	@Override
	protected void createFieldEditors() {
		SharedDirsFieldEditor sdfe= new SharedDirsFieldEditor();
		sdfe.doFillIntoGrid(getFieldEditorParent(), 2);
		addField(sdfe);
		
		IntegerFieldEditor uplimit = new IntegerFieldEditor(PI.uploadLimit,
				Lang.UploadLimit,
				getFieldEditorParent());
		uplimit.setValidRange(0, Integer.MAX_VALUE/1024);
		addField( uplimit );	
	}
	

}
