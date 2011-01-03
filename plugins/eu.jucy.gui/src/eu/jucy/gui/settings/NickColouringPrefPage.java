package eu.jucy.gui.settings;


import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;

import uc.DCClient;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;


public class NickColouringPrefPage extends UCPrefpage {
	

	
	public NickColouringPrefPage() {
		super(Application.PLUGIN_ID);
	}

	@Override
	protected void createFieldEditors() {
		
		addEditors(GUIPI.ownNickCol,Lang.OwnNick, GUIPI.ownNickFont, Lang.OwnNick );
		addEditors(GUIPI.opNickCol,Lang.OperatorNicks, GUIPI.opNickFont, Lang.OperatorNicks );
		addEditors(GUIPI.favNickCol,Lang.FavouritesNicks, GUIPI.favNickFont, Lang.FavouritesNicks );
		addEditors(GUIPI.normalNickCol,Lang.NormalNicks,GUIPI.normalNickFont,Lang.NormalNicks);
		
		BooleanFieldEditor joinParts = new BooleanFieldEditor(GUIPI.colourJoinParts, Lang.ColourJoinParts, getFieldEditorParent());
		addField(joinParts);
	}

	private void addEditors(String colourID,String colourdesc,String fontID,String fontdsc) {
		ColorFieldEditor cfe = new ColorFieldEditor(colourID,colourdesc,getFieldEditorParent());
		addField(cfe);
		FontFieldEditor ffe = new FontFieldEditor(fontID,fontdsc,DCClient.LONGVERSION,getFieldEditorParent());
		addField(ffe);
		
	}
}
