package eu.jucy.op.ui;


import logger.LoggerFactory;
import helpers.GH;
import helpers.SizeEnum;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
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

import uc.files.search.SearchType;
import uihelpers.ComboBoxViewer;


import eu.jucy.adlsearch.ADLSearchEntry.ADLSearchType;
import eu.jucy.adlsearch.Lang;
import eu.jucy.op.CounterFactory;
import eu.jucy.op.OpADLEntry;
import eu.jucy.op.OPI;

public class OpADLDialog extends TrayDialog {

	private static final Logger logger = LoggerFactory.make();
	
	private Combo fileTypeCombo;
	private ComboBoxViewer<SearchType> fileTypeViewer;
	private Button breakAfterRawButton;
	
	
	
	private Text commentText;
	private Combo counterNameCombo;
	private ComboBoxViewer<CounterFactory> comboViewer;
	
	private Text rawCommandText;
	
	
	
	private Button activeButton;
	private Text maxSizeText;
	private Text minSizeText;
	private Combo sizeTypeCombo;
	private Combo searchTypeCombo;
	private Text searchStringText;
	private Spinner incrementBySpinner;

	
	private final OpADLEntry adlEntry;
	
	public OpADLDialog(Shell parentShell,OpADLEntry adlEntry) {
		super(parentShell);
		this.adlEntry = adlEntry;
		setHelpAvailable(true);
	}



	public OpADLDialog(IShellProvider parentShell,OpADLEntry adlEntry) {
		super(parentShell);
		this.adlEntry = adlEntry;
		setHelpAvailable(true);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent.getShell().setText("OP ADL Search Entry");
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,"eu.jucy.op.OpADLSearch");
        
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);

        final Composite composite_1 = new Composite(composite, SWT.NONE);
        composite_1.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        composite_1.setLayout(new GridLayout());

        final Label searchstringLabel = new Label(composite_1, SWT.NONE);
        searchstringLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        searchstringLabel.setText(Lang.ADL_SearchString);

        searchStringText = new Text(composite_1, SWT.BORDER);
        searchStringText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        searchStringText.setText(adlEntry.getSearchString());

        final Composite composite_3 = new Composite(composite_1, SWT.NONE);
        composite_3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        final GridLayout gridLayout_1 = new GridLayout();
        gridLayout_1.numColumns = 2;
        composite_3.setLayout(gridLayout_1);

        final Label minSizeLabel = new Label(composite_3, SWT.NONE);
        minSizeLabel.setText(Lang.ADL_MinSize);

        final Label maxSizeLabel = new Label(composite_3, SWT.NONE);
        maxSizeLabel.setText(Lang.ADL_MaxSize);

        minSizeText = new Text(composite_3, SWT.BORDER);
        minSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        maxSizeText = new Text(composite_3, SWT.BORDER);
        maxSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Composite composite_4 = new Composite(composite_1, SWT.NONE);
        composite_4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        composite_4.setLayout(new GridLayout());

        final Label fileTypeLabel = new Label(composite_4, SWT.NONE);
        fileTypeLabel.setText("File Type:");

        fileTypeCombo = new Combo(composite_4, SWT.READ_ONLY);
        fileTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        fileTypeCombo.setVisibleItemCount(10);
        fileTypeViewer = new ComboBoxViewer<SearchType>(fileTypeCombo,SearchType.values());
        fileTypeViewer.select(adlEntry.getFileType());
        


        final Composite composite_2 = new Composite(composite, SWT.NONE);
        composite_2.setLayout(new GridLayout());
        composite_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        final Label searchTypeLabel = new Label(composite_2, SWT.NONE);
        searchTypeLabel.setText(Lang.ADL_SearchType);

        searchTypeCombo = new Combo(composite_2, SWT.READ_ONLY);
        searchTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        
        for (ADLSearchType st:ADLSearchType.values()) {
        	searchTypeCombo.add(st.toString());
        	searchTypeCombo.setData(st.toString(),st);
        }
        searchTypeCombo.setText( adlEntry.getSearchType().toString() );

        final Label sizeTypeLabel = new Label(composite_2, SWT.NONE);
        sizeTypeLabel.setText(Lang.ADL_SizeType);

        sizeTypeCombo = new Combo(composite_2, SWT.READ_ONLY);
        sizeTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        
   
        for (SizeEnum se:SizeEnum.values()) {
        	 sizeTypeCombo.add(se.name());
        	 sizeTypeCombo.setData(se.name(), se);
        }
        

        activeButton = new Button(composite_2, SWT.CHECK);
        activeButton.setText(Lang.ADL_Active );
        activeButton.setSelection(adlEntry.isActive());





        final Group actionsWhenFoundGroup = new Group(composite, SWT.NONE);
        actionsWhenFoundGroup.setText("Actions");
        final GridLayout gridLayout_2 = new GridLayout();
        gridLayout_2.numColumns = 3;
        actionsWhenFoundGroup.setLayout(gridLayout_2);
        actionsWhenFoundGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

        final Label rawCommandLabel = new Label(actionsWhenFoundGroup, SWT.NONE);
        rawCommandLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        rawCommandLabel.setText("Send Raw Command");

        breakAfterRawButton = new Button(actionsWhenFoundGroup, SWT.CHECK);
        breakAfterRawButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        breakAfterRawButton.setText("break after Raw");
        breakAfterRawButton.setSelection(adlEntry.isBreakAfterRaw());

        rawCommandText = new Text(actionsWhenFoundGroup, SWT.BORDER);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        gd.minimumWidth = 300;
        rawCommandText.setLayoutData(gd);
        rawCommandText.setText(adlEntry.getRaw());

        final Label incrementCounterLabel = new Label(actionsWhenFoundGroup, SWT.NONE);
        incrementCounterLabel.setText("Increment Counter:");

        final Label label_1 = new Label(actionsWhenFoundGroup, SWT.NONE);
        label_1.setText("     ");

        final Label incrementByLabel = new Label(actionsWhenFoundGroup, SWT.NONE);
        incrementByLabel.setText("Increment By");

        counterNameCombo = new Combo(actionsWhenFoundGroup, SWT.BORDER| SWT.DROP_DOWN| SWT.READ_ONLY);
        counterNameCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        
        comboViewer = new ComboBoxViewer<CounterFactory>(counterNameCombo,OPI.getCounterFactories(),true);
        comboViewer.selectByString(adlEntry.getCounter() );
        

       	incrementBySpinner = new Spinner(actionsWhenFoundGroup, SWT.BORDER);
        incrementBySpinner.setMaximum(10000);
        incrementBySpinner.setMinimum(-10000);
        incrementBySpinner.setSelection( adlEntry.getIncrementBy());

        final Label commentLabel = new Label(actionsWhenFoundGroup, SWT.NONE);
        commentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        commentLabel.setText("Comment");

        commentText = new Text(actionsWhenFoundGroup, SWT.BORDER);
        commentText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        commentText.setText(adlEntry.getComment());

       
        
        
        setSizes();
        
        return composite;
	
	
	}
	
	@Override
	protected void okPressed() {
		try {
			adlEntry.setSearchString(searchStringText.getText());
			adlEntry.setActive(activeButton.getSelection());
	
			
			Object o = searchTypeCombo.getData(searchTypeCombo.getText());
			adlEntry.setSearchType((ADLSearchType)o);
			
			adlEntry.setFileType(fileTypeViewer.getSelected());
			adlEntry.setMinSize( parseSize(minSizeText.getText()));
			adlEntry.setMaxSize( parseSize(maxSizeText.getText()));
			
			adlEntry.setTargetFolder("<OP_ADL_SEARCH>");
			
			adlEntry.setIncrementBy(incrementBySpinner.getSelection());
			adlEntry.setCounter(comboViewer.getSelectedString());
			
			adlEntry.setComment(commentText.getText());
			adlEntry.setRaw(rawCommandText.getText());
			
			
			
		} catch(RuntimeException re) {
			logger.warn(re,re);
		}
		
		super.okPressed();
	}
	
	private long parseSize(String s) {
		if (GH.isEmpty(s)) {
			return -1;
		}
		try {
			long size = Long.parseLong(s);
			if (size >= 0) {
				SizeEnum se = getSelectedSize();
			
				return se.getInBytes(size);
			}
		} catch(NumberFormatException nfe) {
			return -1;
		}
		return -1;
	}
	
	private SizeEnum getSelectedSize() {
		String s = sizeTypeCombo.getText();
		SizeEnum se = (SizeEnum)sizeTypeCombo.getData(s);
		if (se == null) {
			se = SizeEnum.B;
		}
		return se;
	}
	
	private void setSizes() {
		long larger = Math.max(adlEntry.getMinSize(), adlEntry.getMaxSize());
		SizeEnum active = SizeEnum.getLargestEnumMatchingByteSize(larger);
		sizeTypeCombo.setText(active.name());
		
		minSizeText.setText(parse(adlEntry.getMinSize() ,active));
		maxSizeText.setText(parse(adlEntry.getMaxSize() ,active));
	}
	
	private String parse(long byteSize,SizeEnum used) {
		if (byteSize < 0) {
			return "";
		}
		return Long.toString(used.getSize(byteSize));
	}
	
	public OpADLEntry getAdlEntry() {
		return adlEntry;
	}

}
