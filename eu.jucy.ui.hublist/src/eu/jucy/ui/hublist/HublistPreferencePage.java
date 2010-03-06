package eu.jucy.ui.hublist;



import uihelpers.SemikolonListFieldEditor;



import eu.jucy.gui.settings.UCPrefpage;


public class HublistPreferencePage extends UCPrefpage {

	public HublistPreferencePage() {
		super(HublistPI.PLUGIN_ID);
	}
	
	@Override
	protected void createFieldEditors() {
		
		SemikolonListFieldEditor hublists= new SemikolonListFieldEditor(HublistPI.hublistServers,
				Lang.ConfigurePublicHubLists,
				getFieldEditorParent(),
				Lang.Hublist,
				Lang.EnterAddressOfTheHublist);
		addField(hublists);

	}


}
