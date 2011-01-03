package eu.jucy.op.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;

import uihelpers.ComplexListEditor;

import eu.jucy.adlsearch.ADLSearchEntry;
import eu.jucy.adlsearch.ui.ADLFieldEditor;
import eu.jucy.op.OpADLEntry;



/**
 * 
 * Implementation of the advanced tableField Editor ..
 * as already used for normal ADL search.
 * 
 * @author Quicksilver
 *
 */
public class OpADLFieldEditor extends ADLFieldEditor {

	public OpADLFieldEditor(String titleText, String prefID,Composite parent) {
		super (titleText,prefID, parent,new OpADLTranslator());
		
	}
	
	@Override
	protected OpADLEntry getNewInputObject() {
		OpADLDialog diag = new OpADLDialog(getPage().getShell(),new OpADLEntry());
		diag.setBlockOnOpen(true);
		
		if (diag.open() == Dialog.OK) {
			return diag.getAdlEntry();
		}
		return null;
	}

	
	@Override
	protected void changeInputObject(ADLSearchEntry v) {
		OpADLEntry op = (OpADLEntry)v;
		OpADLDialog diag = new OpADLDialog(getPage().getShell(),op);
		diag.setBlockOnOpen(true);
		diag.open();
	}
	
	
	/**
	 * 
	 * @param s - string containing all OP ADL entry information
	 * @return a list with all OpADL entries.
	 */
	public static List<OpADLEntry> LoadOPADLFromString(String s) {
		List<ADLSearchEntry> entries = ComplexListEditor.parseString(s, new OpADLTranslator());
		
		List<OpADLEntry> opEntries = new ArrayList<OpADLEntry>();
		for (ADLSearchEntry e: entries) {
			if (e instanceof OpADLEntry) {
				opEntries.add((OpADLEntry)e);
			} else {
				throw new IllegalStateException();
			}
		}
		return opEntries;
	}



	/**
	 * Translator between OpADLEntry and preferences
	 */
	public static class OpADLTranslator implements IPrefSerializer<ADLSearchEntry> {

		public String[] serialize(ADLSearchEntry t) {
			OpADLEntry ent = (OpADLEntry)t;
			return ent.toStringAR();
		}

		public OpADLEntry unSerialize(String[] all) {
			return OpADLEntry.fromStringAR(all);
		}
		
	}
}
