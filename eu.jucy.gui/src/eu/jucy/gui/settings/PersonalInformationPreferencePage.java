package eu.jucy.gui.settings;



import org.eclipse.jface.preference.ComboFieldEditor;

import org.eclipse.jface.preference.StringFieldEditor;


import eu.jucy.gui.Lang;
import uc.PI;

public class PersonalInformationPreferencePage extends
		UCPrefpage {

	public static final String ID = "de.du-hub.settings.PersonalInformation" ;

	
	
	
	private final ValidNickChecker checker = new ValidNickChecker(true);
	

	
	@Override
	protected void createFieldEditors() {
		StringFieldEditor nick = new StringFieldEditor(PI.nick,
				Lang.Nick,
				getFieldEditorParent()) {

					@Override
					protected boolean doCheckState() {
						return checker.checkString(getStringValue(),33);
					}
			
		};
		nick.setTextLimit(32);
		nick.setEmptyStringAllowed(false);
		addField(nick);
		
		StringFieldEditor eMail = new StringFieldEditor(PI.eMail,
				Lang.EMail,
				getFieldEditorParent()){

			@Override
			protected boolean doCheckState() {
				return checker.checkString(getStringValue(),33);
			}
	
		};
		addField(eMail);
		
		
		StringFieldEditor description = new StringFieldEditor(PI.description,
				Lang.Description,
				getFieldEditorParent()){

			@Override
			protected boolean doCheckState() {
				return checker.checkString(getStringValue(),32);
			}
	
		};
		addField(description);
		
		String[][] pairs = new String[][]{
				{"0.005", "0.005"}, 
				{"0.01", "0.01"} ,
				{"0.02", "0.02"},
				{"0.05", "0.05"},
				{"0.1", "0.1"} ,
				{"0.2", "0.2"},
				{"0.5", "0.5"},
				{"1", "1"} ,
				{"2", "2"},
				{"5", "5"},
				{"10", "10"} ,
				{"20", "20"},
				{"50", "50"},
				{"100", "100"}};
		
		ComboFieldEditor lineSpeed = new ComboFieldEditor(PI.connection,				
				Lang.LineSpeed,
				pairs,
				getFieldEditorParent());
		
		addField(lineSpeed);
		
		StringFieldEditor defaultAFKMessage = new StringFieldEditor(PI.defaultAFKMessage,
				Lang.DefaultAwayMessage,
				getFieldEditorParent());
		addField(defaultAFKMessage);
		
		
	}


}
