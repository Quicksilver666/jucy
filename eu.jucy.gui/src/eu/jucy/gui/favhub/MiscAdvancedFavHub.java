package eu.jucy.gui.favhub;

import helpers.GH;

import java.nio.charset.Charset;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.jucy.gui.Lang;



import uc.FavHub;


public class MiscAdvancedFavHub implements IFavHubAdvanced {

	private Combo charsetCombo;
	
	private Button chatOnlyButton;
	
	private static final String DEFAULT = "Default";
	
	public MiscAdvancedFavHub() {
	}

	public ICompControl fillComposite(Composite parent, FavHub favHub) {
		final GridLayout gridLayout_1 = new GridLayout();
		parent.setLayout(gridLayout_1);
		
		final Label codepageChooseLabel = new Label(parent, SWT.NONE);
		codepageChooseLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		codepageChooseLabel.setText(Lang.OverrideProtocolCodepage);
		
		charsetCombo = new Combo(parent, SWT.READ_ONLY);
		charsetCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		charsetCombo.setVisibleItemCount(20);
		
		charsetCombo.add(DEFAULT);
		// st the comparisson enums to the combo
		for (String charsetName:  Charset.availableCharsets().keySet()) { 
			charsetCombo.add(charsetName);
		}
		//set At least as test

		charsetCombo.setText(GH.isEmpty(favHub.getCharset())?DEFAULT: favHub.getCharset());
		
		
		chatOnlyButton = new Button(parent,SWT.CHECK);
		chatOnlyButton.setText("Chat Only");
		chatOnlyButton.setToolTipText("Disables sharing, searching and Up-/Downloading in this hub, \nhub is no longer counted in description");
		chatOnlyButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		chatOnlyButton.setSelection(favHub.isChatOnly());
		
		
		
		
		return new ICompControl() {
			public void okPressed(FavHub favHub) {
				favHub.setChatOnly(chatOnlyButton.getSelection());
				String charset = charsetCombo.getText();
				favHub.setCharset(charset.equals(DEFAULT)? "" : charset);
			}
		};
		
	}

}
