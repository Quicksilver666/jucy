package eu.jucy.ui.smileys;

import java.io.File;

import helpers.GH;

import org.eclipse.jface.preference.FileFieldEditor;

import eu.jucy.gui.settings.UCPrefpage;

public class SmileyPrefPage extends UCPrefpage {



	public SmileyPrefPage() {
		super(SmileysPI.PLUGIN_ID);
	}



	@Override
	protected void createFieldEditors() {
		FileFieldEditor ffe = new FileFieldEditor(SmileysPI.SMILEYS_PATH, 
				Lang.SMSmileyZipFile, getFieldEditorParent()) {

			@Override
			protected boolean checkState() {
				String path = getTextControl().getText();
				if (!GH.isNullOrEmpty(path)) {
					File f = new File(path);
					return f.isFile() && SmileyTextModificator.isValidZipFile(f);
				} else {
					return true;
				}
			}
			
		};
		ffe.setEmptyStringAllowed(true);
		ffe.setFileExtensions(new String[]{"*.zip"});
		addField(ffe);

	}

}
