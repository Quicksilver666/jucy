package eu.jucy.op.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import eu.jucy.op.Activator;
import eu.jucy.op.CheckState;
import eu.jucy.op.OperatorPlugin;

import uc.IUser;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;




public class CheckedUsersColumnDecorator extends ColumnDescriptor<IUser> {

	private final OperatorPlugin op = Activator.getOPPlugin();
	
	
	public CheckedUsersColumnDecorator() {
		super(100, "Checked State");
	}

	@Override
	public String getText(IUser t) {
		CheckState cs = op.getCheckState(t);
		if (cs == null) {
			return "";
		}
		return cs.toString();
	}

	@Override
	public Color getForeground(IUser t) {
		
		switch (op.getCheckState(t)) {
		case CHECKED:
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN);
		case CHECKING:
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
		case SCHEDULED_FOR_CHECK:
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
		default: 
			return null;
		}
	}
}
