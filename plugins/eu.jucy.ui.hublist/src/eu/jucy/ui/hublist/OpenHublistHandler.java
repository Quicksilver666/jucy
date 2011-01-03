package eu.jucy.ui.hublist;


import eu.jucy.gui.OpenEditorHandler;

public class OpenHublistHandler extends OpenEditorHandler {

	public OpenHublistHandler() {
		super(HublistEditor.ID, new HublistEditorInput());
	}

}
