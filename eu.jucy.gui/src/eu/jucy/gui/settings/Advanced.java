package eu.jucy.gui.settings;


import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;


import uc.PI;

import eu.jucy.gui.Lang;


public class Advanced extends UCPrefpage {
	
	public Advanced() {
		super(PI.PLUGIN_ID,"eu.jucy.gui.help.Preferences_Advanced");
	}

	@Override
	protected void createFieldEditors() {
		StringFieldEditor includes = new StringFieldEditor(PI.includeFiles,Lang.IncludedFiles ,50 ,
				getFieldEditorParent());
		addField(includes);	
		
		StringFieldEditor excludes = new StringFieldEditor(PI.excludedFiles,Lang.ExcludedFiles ,50, 
				getFieldEditorParent());
		addField(excludes);	
		
		IntegerFieldEditor maxHashSpeed= new IntegerFieldEditor(PI.maxHashSpeed,
				Lang.MaxHashSpeed,
				getFieldEditorParent());
		
		maxHashSpeed.setValidRange(0, 100000);
		addField(maxHashSpeed);
		
		IntegerFieldEditor filelistRefreshInterval= new IntegerFieldEditor(PI.filelistRefreshInterval,
				Lang.FilelistRefreshInterval,
				getFieldEditorParent());
		filelistRefreshInterval.setValidRange(5, 60*6); //max six hours..
		addField(filelistRefreshInterval);
		
		
		StringFieldEditor bindAddress = new StringFieldEditor(PI.bindAddress,"Bind address (empty for default)" , 
				getFieldEditorParent());
		addField(bindAddress);	
		
		
	}

	
}
