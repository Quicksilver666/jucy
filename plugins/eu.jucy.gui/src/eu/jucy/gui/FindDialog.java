package eu.jucy.gui;

import helpers.GH;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for searching through 
 * ISearchableEditor 
 * 
 * @author Quicksilver
 *
 */
public class FindDialog extends Dialog {

	
	private static final int FIND_ID = 42;
	
	private Text text;
	
	private final ISearchableEditor ise;

	public FindDialog(Shell parentShell, ISearchableEditor ise) {
		super(parentShell);
		this.ise = ise;
		
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, false);
		createButton(parent, FIND_ID, Lang.Find, true);
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
	}
	
	

	@Override
	protected void buttonPressed(int buttonId) {
		switch(buttonId) {
		case IDialogConstants.NEXT_ID:
			ise.next();
			break;
		case FIND_ID:
			if (!GH.isEmpty(text.getText())) {
				ise.search(text.getText());
			}
			break;
		case IDialogConstants.CLOSE_ID:
			super.buttonPressed(IDialogConstants.CANCEL_ID);
			break;
		default:
			super.buttonPressed(buttonId);
			break;
		}
		
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite)super.createDialogArea(parent);
		text = new Text(comp,SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		getShell().setText(Lang.Find);
		
		return comp;
	}
	
	

	
}
