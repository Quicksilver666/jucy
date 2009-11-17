package eu.jucy.gui.settings;



import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;


import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.texteditor.ITextModificator;



public class TextModificationsPrefPage extends UCPrefpage {

	private static Logger logger = LoggerFactory.make();

	public TextModificationsPrefPage() {
		super(Application.PLUGIN_ID);
	}
	
	@Override
	protected void createFieldEditors() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
    	
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(ITextModificator.ExtensionpointID);
	
		
		for (IConfigurationElement element : configElements) {
			try {
				String fullID = GUIPI.IDForTextModificatorEnablement(element.getAttribute("id"));
				BooleanFieldEditor bfe = new BooleanFieldEditor(fullID,element.getAttribute("name"),getFieldEditorParent());
				addField(bfe);
			} catch (Exception e) {
				logger.warn("Element: "+element.getAttribute("id"),e);
			}
		}
		
	}


}
