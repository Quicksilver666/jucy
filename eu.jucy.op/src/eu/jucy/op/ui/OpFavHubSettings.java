package eu.jucy.op.ui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import uc.FavHub;
import uihelpers.TableViewerAdministrator;
import eu.jucy.gui.favhub.IFavHubAdvanced;
import eu.jucy.op.PI;
import eu.jucy.op.StaticReplacement;
import eu.jucy.op.ui.ReplacementsEditor.NameColumn;
import eu.jucy.op.ui.ReplacementsEditor.ReplacementColumn;

public class OpFavHubSettings implements IFavHubAdvanced {

	public OpFavHubSettings() {}

	
	@SuppressWarnings("unchecked")
	public ICompControl fillComposite(Composite fill, final FavHub favHub) {
		//add layout to fill
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		gridLayout_1.horizontalSpacing = 5;
		fill.setLayout(gridLayout_1);
		
		
		final Button checkbox = new Button(fill,SWT.CHECK);
		checkbox.setSelection( Boolean.parseBoolean(favHub.get(PI.fh_checkUsers)));
		final Label checkHub = new Label(fill,SWT.NONE);
		checkHub.setText("Check this hub");
		
		final Label staticReplacementsLabel = new Label(fill,SWT.NONE);
		staticReplacementsLabel.setText("Static Replacements:");
		staticReplacementsLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,2,1));
		
		
		final List<StaticReplacement> replacements = StaticReplacement.loadReplacement(favHub);
		
		final TableViewer tableViewer = new TableViewer(fill,SWT.BORDER| SWT.SINGLE|SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        
        tableViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return ((List<?>)inputElement).toArray();
			}

			public void dispose() {}

			public void inputChanged(Viewer viewer, Object oldInput,Object newInput) {}
        	
        });
        
        tableViewer.setInput(replacements);
        
        TableViewerAdministrator<StaticReplacement> tva = new TableViewerAdministrator<StaticReplacement>(
        		tableViewer,Arrays.asList(new NameColumn(),new ReplacementColumn()),PI.staticReplacementTable,0);
        tva.apply();
        
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true,2,1);
        data.minimumHeight = 200;
        data.minimumWidth = 500;
        table.setLayoutData(data);
        
        Composite composite_1 = new Composite(fill,SWT.NONE);
        GridData data2 = new GridData(SWT.FILL, SWT.FILL, false, false,2,1);
        composite_1.setLayoutData(data2);
        
        composite_1.setLayout(new FillLayout());
        
        final Button addButton = new Button(composite_1, SWT.NONE);
        addButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		add(tableViewer,favHub,replacements);
        	}
        });
        addButton.setText("Add");

        final Button changeButton = new Button(composite_1, SWT.NONE);
        changeButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		change(tableViewer,favHub,replacements);
        	}
        });
        changeButton.setText("Change");

        final Button removeButton = new Button(composite_1, SWT.NONE);
        removeButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		remove(tableViewer,favHub,replacements);
        	}
        });
        removeButton.setText("Remove");
        
		
		return new ICompControl() {
			public void okPressed(FavHub favHub) {
				favHub.put(PI.fh_checkUsers,""+checkbox.getSelection());
				StaticReplacement.storeReplacements(favHub, replacements);
			}
		};
	}
	
	private void add(TableViewer tv,FavHub fh,List<StaticReplacement> replacements) {
		StaticReplacement sr = ReplacementsEditor.newInputObject(tv.getTable().getShell());
		if (sr != null && !replacements.contains(sr)) {
			replacements.add(sr);
			tv.add(sr);
		}
	}
	
	private void change(TableViewer tv,FavHub fh,List<StaticReplacement> replacements) {
		StaticReplacement sr = getSelection(tv);
		if (sr != null) {
			if (ReplacementsEditor.changeObject(tv.getTable().getShell(), sr)) {
				tv.refresh(sr);
			}
		}
	}
	
	private void remove(TableViewer tv,FavHub fh,List<StaticReplacement> replacements) {
		StaticReplacement sr = getSelection(tv);
		if (sr != null) {
			replacements.remove(sr);
			tv.remove(sr);
		}
	}
	
	private StaticReplacement getSelection(TableViewer tableViewer) {
		IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
		if (!sel.isEmpty()) {
			return (StaticReplacement)sel.getFirstElement();
		}
		return null;
	}
	
	
	
	

}
