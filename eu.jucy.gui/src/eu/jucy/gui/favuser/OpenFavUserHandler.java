/**
 * 
 */
package eu.jucy.gui.favuser;

import eu.jucy.gui.OpenEditorHandler;

public class OpenFavUserHandler extends OpenEditorHandler {
	public OpenFavUserHandler() {
		super(FavUserEditor.ID, new FavUserEditorInput());
	}
}