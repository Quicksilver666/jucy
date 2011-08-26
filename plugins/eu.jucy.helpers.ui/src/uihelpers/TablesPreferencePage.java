package uihelpers;

import java.io.IOException;


import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;



public class TablesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {




	
	private final ScopedPreferenceStore preferences;
	
	
	public TablesPreferencePage() {
		super(GRID);
		preferences = new ScopedPreferenceStore(new InstanceScope(),TVAPI.PLUGIN_ID);
		setPreferenceStore(preferences);
	}


	@Override
	protected void createFieldEditors() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
    	
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(TableViewerAdministrator.ExtensionpointID);

		
		for (IConfigurationElement el : configElements) {
			try {
				if ("table_description".equals(el.getName())  ) {
					String tableName = el.getAttribute("name");
					String tableID = el.getAttribute("id");
					for (IConfigurationElement element : configElements) {
						if ("table".equals(element.getName()) && tableID.equals(element.getAttribute("id"))) {
							Label table = new Label(getFieldEditorParent(),SWT.NONE);
							table.setText(tableName);
							table.setLayoutData(new GridData(SWT.LEFT,SWT.CENTER,false,false,2,1));
							
							for (IConfigurationElement newColumns : element.getChildren("table_column")) {
								String fullID = TVAPI.IDForTableColumn(tableID,newColumns.getAttribute("id"),false);
								BooleanFieldEditor fieldEditor = new BooleanFieldEditor(fullID,newColumns.getAttribute("name"),getFieldEditorParent());
								addField(fieldEditor);
							}
							
							for (IConfigurationElement newColumns : element.getChildren("table_column_decorator")) {
								String fullID = TVAPI.IDForTableColumn(tableID,newColumns.getAttribute("id"),true);
								BooleanFieldEditor fieldEditor = new BooleanFieldEditor(fullID,newColumns.getAttribute("name"),getFieldEditorParent());
								addField(fieldEditor);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
	}

	public boolean performOk() {
		try {
			preferences.save();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		return super.performOk();
	}


	public void init(IWorkbench workbench) {}
	


	
}
