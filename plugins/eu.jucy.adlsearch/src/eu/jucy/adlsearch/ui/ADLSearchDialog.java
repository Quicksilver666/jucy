package eu.jucy.adlsearch.ui;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import logger.LoggerFactory;
import helpers.GH;
import helpers.SizeEnum;


import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Text;

import eu.jucy.adlsearch.ADLSearchEntry;
import eu.jucy.adlsearch.Lang;
import eu.jucy.adlsearch.ADLSearchEntry.ADLSearchType;

public class ADLSearchDialog extends Dialog {

	private static final Logger logger = LoggerFactory.make();
	

	
	private Button downloadmatchesButton;
	private Button activeButton;
	private Text maxSizeText;
	private Text minSizeText;
	private Combo sizeTypeCombo;
	private Combo searchTypeCombo;
	private Text targetFolderText;
	private Text searchStringText;
	
	private Button regexpButton;
	private Button caseSensitiveRegexpButton;
	private Text regExpTesterText;
	
	private final ADLSearchEntry adlEntry;



	
	public ADLSearchDialog(IShellProvider parentShell,ADLSearchEntry adlEntry) {
		super(parentShell);
		this.adlEntry = adlEntry;
	}

	public ADLSearchDialog(Shell parentShell,ADLSearchEntry adlEntry) {
		super(parentShell);
		this.adlEntry = adlEntry;
	}



	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Lang.ADL_ADLEntry);
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);

        final Composite composite_1 = new Composite(composite, SWT.NONE);
        composite_1.setLayout(new GridLayout());

        final Label searchstringLabel = new Label(composite_1, SWT.NONE);
        searchstringLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        searchstringLabel.setText(Lang.ADL_SearchString);

        searchStringText = new Text(composite_1, SWT.BORDER);
        searchStringText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
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

        final Label targetFolderLabel = new Label(composite_1, SWT.NONE);
        targetFolderLabel.setText(Lang.ADL_TargetFolder);

        targetFolderText = new Text(composite_1, SWT.BORDER);
        targetFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        targetFolderText.setText(adlEntry.getTargetFolder());

        final Composite composite_2 = new Composite(composite, SWT.NONE);
        composite_2.setLayout(new GridLayout());
        composite_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        final Label searchTypeLabel = new Label(composite_2, SWT.NONE);
        searchTypeLabel.setText(Lang.ADL_SearchType);

        searchTypeCombo = new Combo(composite_2, SWT.READ_ONLY);
        searchTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        for (ADLSearchType st:ADLSearchType.values()) {
        	searchTypeCombo.add(st.toString());
        	searchTypeCombo.setData(st.toString(),st);
        }
        searchTypeCombo.setText( adlEntry.getSearchType().toString() );

        final Label sizeTypeLabel = new Label(composite_2, SWT.NONE);
        sizeTypeLabel.setText(Lang.ADL_SizeType);

        sizeTypeCombo = new Combo(composite_2, SWT.READ_ONLY);
        sizeTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
   
        for (SizeEnum se:SizeEnum.values()) {
        	 sizeTypeCombo.add(se.name());
        	 sizeTypeCombo.setData(se.name(), se);
        }
        

        activeButton = new Button(composite_2, SWT.CHECK);
        activeButton.setText(Lang.ADL_Active );
        activeButton.setSelection(adlEntry.isActive());

        downloadmatchesButton = new Button(composite_2, SWT.CHECK);
        downloadmatchesButton.setText(Lang.ADL_DownloadMatches);
        downloadmatchesButton.setSelection(adlEntry.isDownloadMatches());
        
        regexpButton = new Button(composite_2, SWT.CHECK);
        regexpButton.setText(Lang.ADL_REGEXP);
        regexpButton.setToolTipText(Lang.ADL_REGEXPTT);
        regexpButton.setSelection(adlEntry.isRegExp());
        regexpButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				caseSensitiveRegexpButton.setEnabled(regexpButton.getSelection());
			}
        });

       	caseSensitiveRegexpButton = new Button(composite_2, SWT.CHECK);
        caseSensitiveRegexpButton.setText(Lang.ADL_CASESENSITIVE);
        caseSensitiveRegexpButton.setSelection(adlEntry.isCaseSensitive());
        caseSensitiveRegexpButton.setEnabled(regexpButton.getSelection());
        
        
        final Group regexpTesterGroup = new Group(composite, SWT.NONE);
        regexpTesterGroup.setText(Lang.ADL_REGEXPTESTER);
        final GridLayout gridLayout_3 = new GridLayout();
        gridLayout_3.numColumns = 2;
        regexpTesterGroup.setLayout(gridLayout_3);
        regexpTesterGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

        regExpTesterText = new Text(regexpTesterGroup, SWT.BORDER);
        regExpTesterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        regExpTesterText.setText("");
        
        final Button matchButton = new Button(regexpTesterGroup, SWT.NONE);
        matchButton.setText(Lang.ADL_REGEXPMATCH);
        matchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				boolean caseSensitive = caseSensitiveRegexpButton.getSelection();
				Pattern p = Pattern.compile(searchStringText.getText(),
						Pattern.DOTALL |(caseSensitive?  0:Pattern.UNICODE_CASE|Pattern.CASE_INSENSITIVE));
				
				boolean matches = p.matcher(regExpTesterText.getText()).matches();
				MessageDialog.openInformation(getParentShell(),Lang.ADL_REGEXPTESTER, 
						matches? Lang.ADL_REGEXPMATCH_SUCC : Lang.ADL_REGEXPMATCH_FAIL );
			}
		});
        
        setSizes();
        
        return composite;
	
	
	}

	@Override
	protected void okPressed() {
		boolean allok = true;
		try {
			adlEntry.setSearchString(searchStringText.getText());
			adlEntry.setActive(activeButton.getSelection());
			adlEntry.setDownloadMatches(downloadmatchesButton.getSelection());
			
			Object o = searchTypeCombo.getData(searchTypeCombo.getText());
			adlEntry.setSearchType((ADLSearchType)o);
			
			adlEntry.setMinSize( parseSize(minSizeText.getText()));
			adlEntry.setMaxSize( parseSize(maxSizeText.getText()));
			
			adlEntry.setTargetFolder(targetFolderText.getText());
			adlEntry.setCaseSensitive(caseSensitiveRegexpButton.getEnabled());	
			adlEntry.setRegExp(regexpButton.getSelection());
			
			if (adlEntry.isRegExp()) { // test validity..
				Pattern.compile(adlEntry.getSearchString());
			}
		} catch(PatternSyntaxException pse) {
			MessageDialog.openWarning(getShell(), "Error", pse.toString());
			allok  = false;
		} catch(RuntimeException re) {
			logger.error(re,re);
			allok  = false;
		}
		if (allok) {
			super.okPressed();
		}
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

	public ADLSearchEntry getAdlEntry() {
		return adlEntry;
	}
	
	
	

}
