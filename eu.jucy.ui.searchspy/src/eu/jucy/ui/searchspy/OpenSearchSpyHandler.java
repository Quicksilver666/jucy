package eu.jucy.ui.searchspy;

import eu.jucy.gui.OpenEditorHandler;

public class OpenSearchSpyHandler extends OpenEditorHandler {

	public OpenSearchSpyHandler() {
		super(SearchSpyEditor.ID, new SearchSpyEditorInput());
	}

}
