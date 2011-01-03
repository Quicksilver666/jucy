package eu.jucy.op.ui;

import java.util.Arrays;
import java.util.List;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;


import eu.jucy.op.CounterFactory;
import uihelpers.ComplexListEditor;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;


public class CounterFieldEditor extends ComplexListEditor<CounterFactory> {

	protected CounterFieldEditor(String titleText, String prefID,Composite parent) {
		super(titleText, prefID, 
				Arrays.asList(new CounterName(),new CounterPriority(),new CounterPerFile()), 
						parent, true, new CounterFactoryTranslator());
	}


	@Override
	protected void changeInputObject(CounterFactory v) {
		CounterDialog diag = new CounterDialog(getPage().getShell(),v);
		diag.setBlockOnOpen(true);
		diag.open();
	}

	@Override
	protected CounterFactory getNewInputObject() {
		CounterDialog diag = new CounterDialog(getPage().getShell(),new CounterFactory());
		diag.setBlockOnOpen(true);
		
		if (diag.open() == Dialog.OK) {
			return diag.getCounter();
		}
		return null;
	}
	
	public static List<CounterFactory> loadCFFromString(String s) {
		return ComplexListEditor.parseString(s, new CounterFactoryTranslator());
	}
	
	public static class CounterFactoryTranslator implements IPrefSerializer< CounterFactory > {

		public String[] serialize(CounterFactory t) {
			return t.toStringAR();
		}

		public CounterFactory unSerialize(String[] all) {
			return CounterFactory.fromStrinngAR(all);
		}
		
	}
	
	public static abstract class CounterColumn extends ColumnDescriptor<CounterFactory> {

		public CounterColumn(int defaultColumnSize, String columnName, int style) {
			super(defaultColumnSize, columnName, style);
		}

		@Override
		public Image getImage(CounterFactory x) {
			return null;
		}

	}
	
	public static class CounterName extends  CounterColumn {

		public CounterName() {
			super(120, "Name", SWT.LEAD);
		}

		@Override
		public String getText(CounterFactory x) {
			return x.getName();
		}
	}
	
	public static class CounterPriority extends  CounterColumn {

		public CounterPriority() {
			super(60, "Priotrity", SWT.LEAD);
		}

		@Override
		public String getText(CounterFactory x) {
			return ""+x.getPriority();
		}
	}
	public static class CounterPerFile extends  CounterColumn {

		public CounterPerFile() {
			super(60, "Per File", SWT.LEAD);
		}

		@Override
		public String getText(CounterFactory x) {
			return x.isPerFile()? "YES" : "NO";
		}
	}
	

}
