package eu.jucy.connectiondebugger;


import java.util.Arrays;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import eu.jucy.connectiondebugger.CryptoInfo.CryptoInfoEntry;

import uihelpers.TableViewerAdministrator;

public class CryptoComposite extends Composite {

	
	private TableViewer tableViewer;
	private TableViewerAdministrator<CryptoInfoEntry> tva;
	
	private StyledText text ;
	
	

	public CryptoComposite(Composite parent, int style) {
		super(parent, style);
		
		GridLayout gd = new GridLayout();
		gd.numColumns = 2;
		
		this.setLayout( gd);
	
		
		tableViewer = new TableViewer(this,SWT.BORDER|SWT.SINGLE);
		GridData gridData = new GridData(SWT.FILL,SWT.FILL,false,true );
		getTable().setLayoutData(gridData);
	//	getTable().setHeaderVisible(true);
		tableViewer.setContentProvider(new CryptoContentProvider());
		
		tva = new TableViewerAdministrator<CryptoInfoEntry>(tableViewer,
				Arrays.asList(new CryptoInfoColumn()),DebuggerView.ID+".crypto",
				TableViewerAdministrator.NoSorting,false);
		tva.apply();
		GridData gridData2 = new GridData(SWT.FILL,SWT.FILL,true,true );
		text = new StyledText(this, SWT.BORDER|SWT.V_SCROLL);
		text.setLayoutData(gridData2);
		
		tableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel =  (IStructuredSelection)event.getSelection();
				if (sel.getFirstElement() instanceof CryptoInfoEntry) {
					CryptoInfoEntry cie = (CryptoInfoEntry)sel.getFirstElement();
					text.setText(cie.getInfo());
				}
			}
		});
		
	}
	
	public StyledText getText() {
		return text;
	}
	
	public Table getTable() {
		return tableViewer.getTable();
	}
	
	public void setCryptoInfo(CryptoInfo ci) {
		tableViewer.setInput(ci);
		getTable().getColumn(0).pack();
		getTable().pack();
	}
	
	private static class CryptoContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof CryptoInfo) {
				return ((CryptoInfo)inputElement).getCryptoInfo().toArray();
			} else {
				return new Object[]{};
			}
		}
		
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	}

}
