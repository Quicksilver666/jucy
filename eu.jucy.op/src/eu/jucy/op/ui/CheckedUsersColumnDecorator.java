package eu.jucy.op.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import eu.jucy.op.OperatorPlugin;

import uc.IUser;
import uihelpers.TableViewerAdministrator.TableColumnDecorator;



public class CheckedUsersColumnDecorator extends TableColumnDecorator<IUser> {

	@Override
	public Color getForeground(IUser t, Color parentcolor) {
		
		switch (OperatorPlugin.get().getCheckState(t)) {
		case CHECKED:
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN);
		case CHECKING:
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
		case SCHEDULED_FOR_CHECK:
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
		default: 
			return parentcolor;
		}
	}
}
