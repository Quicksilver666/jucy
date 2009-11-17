/**
 * 
 */
package eu.jucy.gui.favhub;

import eu.jucy.gui.OpenEditorHandler;

public class OpenFavHubsHandler extends OpenEditorHandler {
	public OpenFavHubsHandler() {
		super(FavHubEditor.ID, new FavHubEditorInput());
	}
}