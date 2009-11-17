package eu.jucy.adlsearch.ui;



import eu.jucy.adlsearch.ADLFileListProcessor;
import eu.jucy.gui.settings.UCPrefpage;

public class ADLPreferencePage extends UCPrefpage {


	
	
	
	public ADLPreferencePage() {
		super(ADLFileListProcessor.PLUGIN_ID);
	}
	
	@Override
	protected void createFieldEditors() {
		ADLFieldEditor adlFe = new ADLFieldEditor(Lang.ADLSearch,ADLFileListProcessor.adlID,getFieldEditorParent());
		addField(adlFe);
	}

}
