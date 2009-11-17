package eu.jucy.gui.texteditor.hub;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import eu.jucy.gui.texteditor.NickColourerTextModificator;



import uc.IUser;
import uihelpers.TableViewerAdministrator.TableColumnDecorator;



public class NickColourDecorator extends TableColumnDecorator<IUser> {

	
	@Override
	public Font getFont(IUser t, Font parentfont) {
		return NickColourerTextModificator.getFont(t);
	}

	@Override
	public Color getForeground(IUser t, Color parentcolor) {
		return NickColourerTextModificator.getColor(t);
	}

}
