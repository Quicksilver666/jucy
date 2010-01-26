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
		int[] speeds= new int[] {64,96,128,256,512,768,1000,
				1500,2000,2500,4000,5000,10000,25000,50000,100000}; 
		
		String[][] pairs2 = new String[speeds.length][2];
		
		for (int i=0 ; i < speeds.length; i++) {
			if (speeds[i] > 1000) {
				String s ;
				if ((speeds[i]*1000/(1024*1024*8)) >= 1 ) {
					s = (speeds[i]*1000/(1024*1024*8))+"MiB/s)";
				} else {
					s = (speeds[i]*1000/(1024*8))+"KiB/s)";
				}
				pairs2[i][0] = (speeds[i]/1000)+(speeds[i]%1000 != 0 ?".5":"" ) +"Mbps ("+ s;
			} else {
				pairs2[i][0] = speeds[i]+"kbps ("+ (speeds[i]*1000/ (1024*8))+"KiB/s)";
			}
			pairs2[i][1] = ""+ speeds[i] * 1000/8;
		}
		
		
		ComboFieldEditor lineSpeed = new ComboFieldEditor(PI.connectionNew,				
				Lang.LineSpeed,
				pairs2,
				getFieldEditorParent());
		
		addField(lineSpeed);
		
		StringFieldEditor defaultAFKMessage = new StringFieldEditor(PI.defaultAFKMessage,
				Lang.DefaultAwayMessage,
				getFieldEditorParent());
		addField(defaultAFKMessage);

	}
	



}
