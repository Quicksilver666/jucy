package eu.jucy.adlsearch.ui;


import java.util.Arrays;
import java.util.List;

import helpers.SizeEnum;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eu.jucy.adlsearch.ADLSearchEntry;

import uihelpers.ComplexListEditor;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public class ADLFieldEditor extends ComplexListEditor<ADLSearchEntry> {


	public ADLFieldEditor(String titleText, String prefID,Composite parent) {
		this(titleText, prefID, parent,  new ADLTranslator());
	}
	
	protected ADLFieldEditor(String titleText, String prefID,Composite parent,IPrefSerializer<ADLSearchEntry> entry) {
		super(titleText, prefID, 
				Arrays.asList(new SearchString(),new SearchTypeCol(),
						new TargetFolder(),new MinSize(),new MaxSize()), 
						parent, true, entry);
	}
	

	/*
	@Override
	protected ADLSearchEntry createItemFromString(String item) {
		return ItemFromString(item);
	}

	@Override
	protected String createStringFromItem(ADLSearchEntry v) {
		return asString(v.toStringAR());
	} */
	
	/*
	public static ADLSearchEntry ItemFromString(String item) {
		String[] ar = asArray(item);
		return ADLSearchEntry.fromString(ar);
	} */
	
	public static List<ADLSearchEntry> loadFromString(String s) {
		return ComplexListEditor.parseString(s, new ADLTranslator());
		/*
		List<ADLSearchEntry> ret = new ArrayList<ADLSearchEntry>();
		for (String item:loadList(s)) {
			ret.add(ItemFromString(item));
		}
		return ret; */
	}
	


	@Override
	protected ADLSearchEntry getNewInputObject() {
		ADLSearchDialog adld = new ADLSearchDialog(getPage().getShell(),new ADLSearchEntry());
		adld.setBlockOnOpen(true);
		if (adld.open() == Dialog.OK) {
			return adld.getAdlEntry();
		}
		return null;
	}
	

	@Override
	protected void changeInputObject(ADLSearchEntry v) {
		ADLSearchDialog adld = new ADLSearchDialog(getPage().getShell(),v);
		adld.setBlockOnOpen(true);
		adld.open();
	}

	public static class ADLTranslator implements IPrefSerializer<ADLSearchEntry> {

		public String[] serialize(ADLSearchEntry t) {
			return t.toStringAR();
		}

		public ADLSearchEntry unSerialize(String[] all) {
			return ADLSearchEntry.fromString(all);
		}
	}


	public static abstract class ADLColumn extends ColumnDescriptor<ADLSearchEntry> {

		public ADLColumn(int defaultColumnSize, String columnName, int style) {
			super(defaultColumnSize, columnName, style);
		}

		@Override
		public Image getImage(ADLSearchEntry x) {
			return null;
		}


		@Override
		public Color getForeground(ADLSearchEntry x) {
			if (x.isActive()) {
				return super.getForeground(x);
			} else {
				return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
			}
		}
	}
	

	

	public static class SearchString extends  ADLColumn {
		
		public SearchString() {
			super(120, Lang.SearchString, SWT.LEAD);
		}

		@Override
		public String getText(ADLSearchEntry x) {
			return x.getSearchString();
		}

	}
	
	public static class SearchTypeCol extends ADLColumn {

		public SearchTypeCol() {
			super(80, Lang.SearchType, SWT.LEAD);
		}

		@Override
		public String getText(ADLSearchEntry x) {
			return x.getSearchType().toString();
		}
	}
	
	public static class TargetFolder extends ADLColumn {

		public TargetFolder() {
			super(80, Lang.TargetFolder, SWT.LEAD);
		}

		@Override
		public String getText(ADLSearchEntry x) {
			return x.getTargetFolder();
		}
	}
	
	public static class MinSize extends ADLColumn {
		
		public MinSize() {
			super(60, Lang.MinSize, SWT.LEAD);
		}

		@Override
		public String getText(ADLSearchEntry x) {
			if (x.getMinSize() == -1) {
				return "";
			} else {
				return SizeEnum.getReadableSize(x.getMinSize());
			}
		}
	}
	
	public static class MaxSize extends ADLColumn {
		
		public MaxSize() {
			super(60, Lang.MaxSize, SWT.LEAD);
		}

		@Override
		public String getText(ADLSearchEntry x) {
			if (x.getMaxSize() == -1) {
				return "";
			} else {
				return SizeEnum.getReadableSize(x.getMaxSize());
			}
		}
	}
	
	
	
}
