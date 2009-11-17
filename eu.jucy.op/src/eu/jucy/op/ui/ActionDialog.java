package eu.jucy.op.ui;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import uihelpers.ComboBoxViewer;

import eu.jucy.op.CounterFactory;
import eu.jucy.op.PI;
import eu.jucy.op.CounterFactory.CounterAction;

public class ActionDialog extends TrayDialog {

	private Button noUpperBoundButton;
	private Button noLowerboundButton;
	private Button openFileListButton;
	private Button breakAfterActionButton;
	private Spinner maxPointsSpinner;
	private Spinner minPointsSpinner;
	private Spinner incrementByWhatSpinner;
	
	private Combo incrementCounterCombo;
	private ComboBoxViewer<CounterFactory> comboViewer;
	
	private Text rawText;
	private static final Logger logger = LoggerFactory.make();
	
	private final CounterAction action;
	


	public ActionDialog(Shell parentShell,CounterAction  action) {
		super(parentShell);
		this.action = action;
		setHelpAvailable(true);
	}

	public ActionDialog(IShellProvider parentShell,CounterAction  action) {
		super(parentShell);
		this.action = action;
		setHelpAvailable(true);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,"eu.jucy.op.OpADLSearch");
        final GridLayout gridLayout = new GridLayout();
        composite.setLayout(gridLayout);

        final Composite composite_1 = new Composite(composite, SWT.BORDER);
        final GridLayout gridLayout_1 = new GridLayout();
        gridLayout_1.numColumns = 3;
        composite_1.setLayout(gridLayout_1);
        composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        minPointsSpinner = new Spinner(composite_1, SWT.BORDER);
        minPointsSpinner.setMinimum(-100000);
        minPointsSpinner.setMaximum(100000);
        minPointsSpinner.setSelection(action.getMinCount());
        

        final Label minPointsForLabel = new Label(composite_1, SWT.NONE);
        minPointsForLabel.setText("Min Count");

        noLowerboundButton = new Button(composite_1, SWT.CHECK);
        noLowerboundButton.setText("No Lower Bound");
        noLowerboundButton.setSelection(action.isNoLowerBound());
        noLowerboundButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateSpinner();
			}
        });
        
        
        maxPointsSpinner = new Spinner(composite_1, SWT.BORDER);
        maxPointsSpinner.setMinimum(-100000);
        maxPointsSpinner.setMaximum(100000);
        maxPointsSpinner.setSelection(action.getMaxCount());
        
        
        final Label maxCountLabel = new Label(composite_1, SWT.NONE);
        maxCountLabel.setText("Max Count");

        noUpperBoundButton = new Button(composite_1, SWT.CHECK);
        noUpperBoundButton.setText("No Upper Bound");
        noUpperBoundButton.setSelection(action.isNoUpperBound());
        noUpperBoundButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateSpinner();
			}
        });
        
        updateSpinner();

        breakAfterActionButton = new Button(composite_1, SWT.CHECK);
        breakAfterActionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        breakAfterActionButton.setText("Break after Action execution");
        breakAfterActionButton.setSelection(action.isBreakAfterExecution());
        new Label(composite_1, SWT.NONE);

        final Group actionGroup = new Group(composite, SWT.NONE);
        final GridLayout gridLayout_2 = new GridLayout();
        gridLayout_2.numColumns = 2;
        actionGroup.setLayout(gridLayout_2);
        actionGroup.setText("Actions");
        actionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        final Label rawLabel = new Label(actionGroup, SWT.NONE);
        rawLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        rawLabel.setText("Raw");

        rawText = new Text(actionGroup, SWT.BORDER);
        rawText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        rawText.setText(action.getRaw());

        openFileListButton = new Button(actionGroup, SWT.CHECK);
        openFileListButton.setText("Open FileList");
        openFileListButton.setSelection(action.isOpenFileList());
        new Label(actionGroup, SWT.NONE);

        final Label incrementCounterLabel = new Label(actionGroup, SWT.NONE);
        incrementCounterLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        incrementCounterLabel.setText("Increment Counter");

        final Label incrementByLabel = new Label(actionGroup, SWT.NONE);
        incrementByLabel.setText("Increment By");

        incrementCounterCombo = new Combo(actionGroup, SWT.READ_ONLY);
        incrementCounterCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        
        comboViewer = new ComboBoxViewer<CounterFactory>(incrementCounterCombo,PI.getCounterFactories(),true);
        
        incrementByWhatSpinner = new Spinner(actionGroup, SWT.BORDER);
        incrementByWhatSpinner.setMinimum(-10000);
        incrementByWhatSpinner.setMaximum(10000);
        incrementByWhatSpinner.setSelection(action.getIncrementByWhat());
        comboViewer.selectByString(action.getIncrementCounter());
        
        
        return composite;
    	
    	
	}
	
	private void updateSpinner() {
		maxPointsSpinner.setEnabled(!noUpperBoundButton.getSelection());
		minPointsSpinner.setEnabled(!noLowerboundButton.getSelection());
	}
	
	@Override
	protected void okPressed() {
		try {
			action.setOpenFileList(openFileListButton.getSelection());
			action.setBreakAfterExecution(breakAfterActionButton.getSelection());
			action.setMaxCount(maxPointsSpinner.getSelection());
			action.setNoUpperBound(noUpperBoundButton.getSelection());
			action.setMinCount(minPointsSpinner.getSelection());
			action.setNoLowerBound(noLowerboundButton.getSelection());
			action.setIncrementByWhat(incrementByWhatSpinner.getSelection());
			action.setIncrementCounter(comboViewer.getSelectedString());
			action.setRaw(rawText.getText());

		} catch(RuntimeException re) {
			logger.warn(re,re);
		}
		
		super.okPressed();
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Action");
	}

	public CounterAction getAction() {
		return action;
	}
	protected Button getNoLowerboundButton() {
		return noLowerboundButton;
	}
	protected Button getNoUpperBoundButton() {
		return noUpperBoundButton;
	}

}
