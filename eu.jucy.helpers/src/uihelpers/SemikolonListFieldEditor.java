package uihelpers;

import helpers.GH;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.Composite;

public class SemikolonListFieldEditor extends ListEditor {
	private final String dialogtitel, 
					dialogmessage;

	public SemikolonListFieldEditor(String name, String labelText, Composite parent,String dialogtitel, String dialogmessage){
		super(name,labelText,parent);
		this.dialogtitel = dialogtitel;
		this.dialogmessage=dialogmessage;
	}
	
	@Override
	protected String createList(String[] items) {
		return GH.concat(Arrays.asList(items), ";", "");
	}

	@Override
	protected String getNewInputObject() {
		InputDialog create=new InputDialog(getShell()
				, dialogtitel
				, dialogmessage
				, ""
				, null) ;
		create.setBlockOnOpen(true);
		int ret= create.open();
		if (ret == InputDialog.OK) {
			return create.getValue();
		}
		return null;
	}

	@Override
	protected String[] parseString(String stringList) {
		return stringList.split(Pattern.quote(";"));
	}

}
