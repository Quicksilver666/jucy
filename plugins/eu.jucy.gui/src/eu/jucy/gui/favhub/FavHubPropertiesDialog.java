package eu.jucy.gui.favhub;

import helpers.GH;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;



import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.settings.ValidNickChecker;
import uc.FavHub;

public class FavHubPropertiesDialog extends Dialog {

	private static final int ADVANCED_BUTTON_ID =  42;
	
	private Text userDescriptionText;
	private Text passwordText;
	private Text nickText;
	private Text hubDescriptionText;
	private Text hubAddressText;
	private Text hubNameText;
	private Text emailText;
	protected FavHub result = null;
	
	private FavHub working;

	private final ValidNickChecker validNick = new ValidNickChecker(false);
	private final ValidNickChecker validOther = new ValidNickChecker(true);

	/**
	 * Create the dialog
	 * @param parent
	 */
	public FavHubPropertiesDialog(Shell parent) {
		this(parent, null);	
	}
	
	public FavHubPropertiesDialog(Shell parent,FavHub modify) {
		super(parent);
		working = modify;
		setBlockOnOpen(true); 
	}
	
	

	
	
	

	/**
	 * Create contents of the dialog
	 */
	protected Composite  createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		getShell().setText(Lang.FavHubProperties); 
	

		final Group hubGroup = new Group(composite, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		hubGroup.setLayout(gridLayout);
		hubGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		hubGroup.setText(Lang.Hub);

		final Label label = new Label(hubGroup, SWT.RIGHT);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.widthHint = 100;
		label.setLayoutData(gridData);
		label.setText(Lang.Name);

		hubNameText = new Text(hubGroup, SWT.BORDER);
		final GridData gridData_6 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_6.widthHint = 200;
		hubNameText.setLayoutData(gridData_6);

		final Label label_1 = new Label(hubGroup, SWT.NONE);
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_1.widthHint = 100;
		label_1.setLayoutData(gridData_1);
		label_1.setAlignment(SWT.RIGHT);
		label_1.setText(Lang.Address);

		hubAddressText = new Text(hubGroup, SWT.BORDER);
		final GridData gridData_7 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_7.widthHint = 200;
		hubAddressText.setLayoutData(gridData_7);

		final Label label_2 = new Label(hubGroup, SWT.NONE);
		final GridData gridData_2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_2.widthHint = 100;
		label_2.setLayoutData(gridData_2);
		label_2.setAlignment(SWT.RIGHT);
		label_2.setText(Lang.Description);

		hubDescriptionText = new Text(hubGroup, SWT.BORDER);
		final GridData gridData_8 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_8.widthHint = 200;
		hubDescriptionText.setLayoutData(gridData_8);

		final Group identificationGroup = new Group(composite, SWT.NONE);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		identificationGroup.setLayout(gridLayout_1);
		identificationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		identificationGroup.setText(Lang.Identification);

		final Label label_3 = new Label(identificationGroup, SWT.NONE);
		final GridData gridData_3 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_3.widthHint = 100;
		label_3.setLayoutData(gridData_3);
		label_3.setAlignment(SWT.RIGHT);
		label_3.setText(Lang.Nick);

		nickText = new Text(identificationGroup, SWT.BORDER);
		nickText.setTextLimit(32);
		final GridData gridData_9 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_9.widthHint = 200;
		nickText.setLayoutData(gridData_9);
		nickText.addKeyListener(validNick);
		

		final Label label_4 = new Label(identificationGroup, SWT.NONE);
		final GridData gridData_4 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_4.widthHint = 100;
		label_4.setLayoutData(gridData_4);
		label_4.setAlignment(SWT.RIGHT);
		label_4.setText(Lang.Password);
		

		passwordText = new Text(identificationGroup, SWT.BORDER| SWT.PASSWORD);
		passwordText.setTextLimit(80);
		final GridData gridData_10 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_10.widthHint = 200;
		passwordText.setLayoutData(gridData_10);

		final Label label_5 = new Label(identificationGroup, SWT.NONE);
		final GridData gridData_5 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_5.widthHint = 100;
		label_5.setLayoutData(gridData_5);
		label_5.setAlignment(SWT.RIGHT);
		label_5.setText(Lang.UserDescription);

		userDescriptionText = new Text(identificationGroup, SWT.BORDER);
		userDescriptionText.setTextLimit(80);
		final GridData gridData_11 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_11.widthHint = 200;
		userDescriptionText.setLayoutData(gridData_11);
		userDescriptionText.addKeyListener(validOther);
		
		final Label label_6 = new Label(identificationGroup, SWT.NONE);
		final GridData gridData_12 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_12.widthHint = 100;
		label_6.setLayoutData(gridData_12);
		label_6.setAlignment(SWT.RIGHT);
		label_6.setText(Lang.EMail);

		emailText = new Text(identificationGroup, SWT.BORDER);
		emailText.setTextLimit(80);
		final GridData gridData_13 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_13.widthHint = 200;
		emailText.setLayoutData(gridData_13);
		emailText.addKeyListener(validOther);
		
		//
		
		if (working != null) {
			hubNameText.setText(working.getHubname());
			hubAddressText.setText(working.getHubaddy());
			hubDescriptionText.setText(working.getDescription());
			
			nickText.setText(working.getNick());
			passwordText.setText(working.getPassword());
			userDescriptionText.setText(working.getUserDescription());
			emailText.setText(working.getEmail());
		}

		return composite;
		
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent,ADVANCED_BUTTON_ID,Lang.Advanced,false); 
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	protected void okPressed() {
		String hubAddress = hubAddressText.getText();
		if (setResultIfValidForFHGeneration()) {
			
			result.setHubname(hubNameText.getText());
			result = result.setHubaddy(hubAddress);
			result.setDescription(hubDescriptionText.getText());
			
			result.setNick(nickText.getText());
			result.setPassword(passwordText.getText());
			result.setUserDescription(userDescriptionText.getText());
			result.setEmail(emailText.getText());
			super.okPressed();
		}
	}
	
	private boolean setResultIfValidForFHGeneration() {
		String hubAddress = hubAddressText.getText();
		if (GH.isNullOrEmpty(hubAddress)) {
			//no hubaddress
			MessageDialog.openError(getShell(), "Error", "Hub address empty!"); //TODO translate
			return false;
		}
		if (alreadyExists(hubAddress)) {
			//already exists
			MessageDialog.openError(getShell(), "Error", "Hub address already in use!"); //TODO translate
			return false;
		}
		if (!checkValidStrings()) {
			//check nick / userdescription and email
			MessageDialog.openError(getShell(), "Error", "Check nick/user description/email field!"); //TODO translate
			return false;
		}
			
		if (working == null) {
			working = new FavHub(hubAddress);
		}
		result = working;
		return true;
		
		
	}
	

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (buttonId == ADVANCED_BUTTON_ID && (working != null || setResultIfValidForFHGeneration())) {
			AdvancedFavHubPropertiesDialog afhpd = 
				new AdvancedFavHubPropertiesDialog(getShell(),working != null? working: result);
			afhpd.open();
			
		} else if (buttonId == CANCEL) {
			result = null;
		}
	}

	private boolean checkValidStrings() {
		return 		validNick.checkString(nickText.getText(),33) 
				&& 	validOther.checkString(userDescriptionText.getText(),32) 
				&& 	validOther.checkString(emailText.getText(),32);
	}
	

	private boolean alreadyExists(String hubAddress) {
		if (working != null && working.getSimpleHubaddy().equals(new FavHub(hubAddress).getSimpleHubaddy())) {
			return false;
		}
		return ApplicationWorkbenchWindowAdvisor.get().getFavHubs().contains(hubAddress);
		
	}
	
	

	public FavHub getResult() {
		return result;
	}
	
}
