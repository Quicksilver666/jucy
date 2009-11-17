package eu.jucy.op.ui;

import helpers.GH;

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import eu.jucy.op.StaticReplacement;
import uihelpers.ComplexListEditor;

import uihelpers.TableViewerAdministrator.ColumnDescriptor;


public class ReplacementsEditor extends ComplexListEditor<StaticReplacement> {

	
	@SuppressWarnings("unchecked")
	public ReplacementsEditor(String titleText, String prefID,Composite parent) {
		super(titleText, prefID, 
				Arrays.asList(new NameColumn(),new ReplacementColumn()), 
				parent,true, new RepSerializer());
	}

	@Override
	protected StaticReplacement getNewInputObject() {
		return newInputObject(getPage().getShell());
	}
	
	
	@Override
	protected void changeInputObject(StaticReplacement v) {
		changeObject(getPage().getShell(),v);
	}
	
	public static  StaticReplacement  newInputObject(Shell shell) {
		StaticReplacement sr = new StaticReplacement();
		InputDialog inputDialog = new InputDialog(shell,"Replacement",
				"Please provide name for the replacement!",
				"",new IInputValidator(){
					public String isValid(String newText) {
						if (newText.contains("]")|| newText.contains("%") || newText.contains("[")) {
							return "],[ and % are reserved characters"; 
						}
						if (GH.isNullOrEmpty(newText)) {
							return "Replacement may not be empty!";
						}
						return null;
					}
		});
		
		inputDialog.setBlockOnOpen(true);
		if (inputDialog.open() == Dialog.OK) {
			if (changeObject(shell,sr)) {
				sr.setName(inputDialog.getValue());
				return sr;
			}
		}
		return null;
	}
	
	public static boolean changeObject(Shell shell,StaticReplacement v) {
		InputDialog inputDialog2 = new InputDialog(shell,"Replacement",
				"Please provide with what the string should be replaced",
				v.getReplacement(),new IInputValidator() {
					public String isValid(String newText) {
						return null;
					}
		});
		inputDialog2.setBlockOnOpen(true);
		if (inputDialog2.open() == Dialog.OK) {
			v.setReplacement(inputDialog2.getValue());
			return true;
		}
		
		return false;
	}




	public static class RepSerializer implements IPrefSerializer<StaticReplacement> {
		public String[] serialize(StaticReplacement t) {
			return new String[] {t.getName(),t.getReplacement()};
		}

		public StaticReplacement unSerialize(String[] all) {
			return new StaticReplacement(all[0],all[1]);
		}
	}
	
	public static class NameColumn extends ColumnDescriptor<StaticReplacement> {

		public NameColumn() {
			super(80, "Name");
		}
		@Override
		public String getText(StaticReplacement x) {
			return "%["+x.getName()+"]";
		}
	}
	
	public static class ReplacementColumn extends ColumnDescriptor<StaticReplacement> {

		public ReplacementColumn() {
			super(200, "Replacement");
		}
		@Override
		public String getText(StaticReplacement x) {
			return x.getReplacement();
		}
	}
	
}
