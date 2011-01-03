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
	
	private Button showJoinsButton;
	private Button showFavJoinsButton;
	private Button showChatterJoinsButton;
	
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
		chatOnlyButton.setText(Lang.ChatOnly);
		chatOnlyButton.setToolTipText(Lang.ChatOnlyDescription);
		chatOnlyButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		chatOnlyButton.setSelection(favHub.isChatOnly());
		
		showJoinsButton = new Button(parent,SWT.CHECK);
		showJoinsButton.setText(Lang.ShowJoins);
		showJoinsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		showJoinsButton.setSelection(favHub.isShowJoins());
		
		showFavJoinsButton = new Button(parent,SWT.CHECK);
		showFavJoinsButton.setText(Lang.ShowFavJoins);
		showFavJoinsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		showFavJoinsButton.setSelection(favHub.isShowFavJoins());
		
		showChatterJoinsButton = new Button(parent,SWT.CHECK);
		showChatterJoinsButton.setText(Lang.ShowChatterJoins);
		showChatterJoinsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		showChatterJoinsButton.setSelection(favHub.isShowRecentChatterJoins());
		
		
		return new ICompControl() {
			public void okPressed(FavHub favHub) {
				String charset = charsetCombo.getText();
				favHub.setCharset(charset.equals(DEFAULT)? "" : charset);
				favHub.setChatOnly(chatOnlyButton.getSelection());
				favHub.setShowJoins(showJoinsButton.getSelection());
				favHub.setShowFavJoins(showFavJoinsButton.getSelection());
				favHub.setShowRecentChatterJoins(showChatterJoinsButton.getSelection());
			}
		};
		
	}

}
