package eu.jucy.ui.translation;

import java.util.ArrayList;
import java.util.List;




import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.google.api.translate.Language;


import eu.jucy.gui.settings.UCPrefpage;

public class TranslationPrefPage extends UCPrefpage implements
		IWorkbenchPreferencePage {



	public TranslationPrefPage() {
		super( TransPI.PLUGIN_ID);
	}


	@Override
	protected void createFieldEditors() {
		
		
		List<String[]> all = new ArrayList<String[]>();
	
		for (Language l:Language.values()) {
			all.add(new String[]{l.name(),l.name()});
		}
		
		
		
		ComboFieldEditor sourceLanguage = new ComboFieldEditor(TransPI.sourceLanguage,				
				Lang.SourceLanguage,
				all.toArray(new String[2][all.size()]),
				getFieldEditorParent());
		
		addField(sourceLanguage);
		
		all.remove(0); //remove auto detect -> no possible value for target..
		
		ComboFieldEditor targetLanguage = new ComboFieldEditor(TransPI.targetLanguage,				
				Lang.TargetLanguage,
				all.toArray(new String[2][all.size()]),
				getFieldEditorParent());
		
		addField(targetLanguage);

	}

}
