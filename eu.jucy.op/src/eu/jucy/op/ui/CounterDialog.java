package eu.jucy.op.ui;

import java.util.Arrays;

import helpers.GH;
import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import uihelpers.TableViewerAdministrator;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

import eu.jucy.op.CounterFactory;
import eu.jucy.op.PI;
import eu.jucy.op.CounterFactory.CounterAction;

public class CounterDialog extends TrayDialog {

	private static final Logger logger = LoggerFactory.make();
	
	private Table table;
	private Text commentText;
	private Text counterNameText;
	private Spinner prioritySpinner;
	private Button perFileButton;	
	private TableViewer tableViewer;
	

	private final CounterFactory counter;
	


	public CounterDialog(IShellProvider parentShell,CounterFactory counter) {
		super(parentShell);
		this.counter = counter;
		setHelpAvailable(true);
	}

	public CounterDialog(Shell parentShell,CounterFactory counter) {
		super(parentShell);
		this.counter = counter;
		setHelpAvailable(true);
	}

	
	@Override
	protected Control createDialogArea(Composite parent) {
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,"eu.jucy.op.OpADLSearch");
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);

        final Label counternameLabel = new Label(composite, SWT.NONE);
        counternameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        counternameLabel.setText("Counter Name");

        counterNameText = new Text(composite, SWT.BORDER);
        counterNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        counterNameText.setText(counter.getName());

        final Label commentLabel = new Label(composite, SWT.NONE);
        commentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        commentLabel.setText("Comment");

        commentText = new Text(composite, SWT.BORDER);
        commentText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        commentText.setText(counter.getComment());

        perFileButton = new Button(composite, SWT.CHECK);
        perFileButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        perFileButton.setText("Per File");
        perFileButton.setSelection( counter.isPerFile());

        prioritySpinner = new Spinner(composite, SWT.BORDER);
        prioritySpinner.setMaximum(255);
        prioritySpinner.setSelection(counter.getPriority());

        final Label priorityLabel = new Label(composite, SWT.NONE);
        priorityLabel.setLayoutData(new GridData());
        priorityLabel.setText("Priority");

        tableViewer = new TableViewer(composite, SWT.BORDER| SWT.SINGLE|SWT.FULL_SELECTION);
      
        tableViewer.setContentProvider(new CP());
        
        table = tableViewer.getTable();
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true,2,1);
        data.minimumHeight = 120;
        data.minimumWidth = 285;
        table.setLayoutData(data);
        
        
        TableViewerAdministrator<CounterAction> tva = 
        	new TableViewerAdministrator<CounterAction>(tableViewer,
        			Arrays.asList(new MinColumn(),new MaxColumn(),new RawColumn()),
        			PI.counterActionsTable,0,false);
        
        tva.apply();
        
        tableViewer.setInput(counter);

        final Composite composite_1 = new Composite(composite, SWT.NONE);
        composite_1.setLayout(new FillLayout());
        composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

        final Button addButton = new Button(composite_1, SWT.NONE);
        addButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		add();
        	}
        });
        addButton.setText("Add");

        final Button changeButton = new Button(composite_1, SWT.NONE);
        changeButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		change();
        	}
        });
        changeButton.setText("Change");

        final Button removeButton = new Button(composite_1, SWT.NONE);
        removeButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		remove();
        	}
        });
        removeButton.setText("Remove");
        
        return composite;
	
	}
	
	private void add() {
		ActionDialog ad = new ActionDialog(getShell(),new CounterAction());
		ad.setBlockOnOpen(true);
		if (ad.open() == Dialog.OK ) {
			CounterAction ca = ad.getAction();
			counter.add(ca);
			tableViewer.add(ca);
		}
	}
	
	private void remove() {
		CounterAction ca = getSelection();
		if (ca != null) {
			counter.remove(ca);
			tableViewer.remove(ca);
		}
	}
	
	private void change() {
		CounterAction ca = getSelection();
		if (ca != null) {
			ActionDialog ad = new ActionDialog(getShell(),ca);
			ad.setBlockOnOpen(true);
			ad.open();
			tableViewer.refresh(ca);
		}
	}
	
	private CounterAction getSelection() {
		IStructuredSelection sel = (IStructuredSelection)tableViewer.getSelection();
		if (!sel.isEmpty()) {
			return (CounterAction)sel.getFirstElement();
		}
		return null;
	}
	
	
	
	@Override
	protected void okPressed() {
		try {
			if (GH.isNullOrEmpty(counterNameText.getText())) {
				MessageDialog.openInformation(getParentShell(),"Info", "Counter name may not be empty");
				return;
			}
			counter.setName(counterNameText.getText());
			counter.setComment(commentText.getText());
			counter.setPerFile(perFileButton.getSelection());
			counter.setPriority(prioritySpinner.getSelection());
			
		} catch(RuntimeException re) {
			logger.warn(re,re);
		}
		
		super.okPressed();
	}
	
	public CounterFactory getCounter() {
		return counter;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Counter");
	}
	
	
	private static class CP implements IStructuredContentProvider {

		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		public Object[] getElements(Object inputElement) {
			return ((CounterFactory)inputElement).getAllActions().toArray();
		}
		
	}
	
	private static abstract class CounterActionsColumn extends ColumnDescriptor<CounterAction> {

		public CounterActionsColumn(int defaultColumnSize, String columnName,int style) {
			super(defaultColumnSize, columnName, style);
		}
		
	}
	
	public static class MinColumn extends CounterActionsColumn {

		public MinColumn() {
			super(40, "Min Count", SWT.LEAD);
		}

		@Override
		public String getText(CounterAction x) {
			return ""+x.getMinCount();
		}
	}
	
	public static class MaxColumn extends CounterActionsColumn {

		public MaxColumn() {
			super(40, "Max Count", SWT.LEAD);
		}

		@Override
		public String getText(CounterAction x) {
			return ""+x.getMaxCount();
		}
	}
	
	public static class RawColumn extends CounterActionsColumn {
		public RawColumn() {
			super(200, "Raw", SWT.LEAD);
		}

		@Override
		public String getText(CounterAction x) {
			return x.getRaw();
		}
	}
	
	
}
