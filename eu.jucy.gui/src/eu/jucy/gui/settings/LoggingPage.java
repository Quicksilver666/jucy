package eu.jucy.gui.settings;



import java.text.SimpleDateFormat;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;


import eu.jucy.gui.Lang;
import uc.PI;



public class LoggingPage extends UCPrefpage {



	public LoggingPage() {}

	@Override
	protected void createFieldEditors() {
		
		StringFieldEditor timeStamp = new StringFieldEditor(PI.logTimeStamps,Lang.TimeStamps,getFieldEditorParent()) {
			@Override
			protected boolean checkState() {
				try {
					new SimpleDateFormat(getStringValue());
				} catch(Exception e) {
					return false;
				}
				return super.checkState();
			}
		};
		addField(timeStamp);
		
		BooleanFieldEditor logMC = new BooleanFieldEditor(PI.logMainChat,Lang.LogMainchat,getFieldEditorParent());
		addField(logMC);
		
		BooleanFieldEditor logPM = new BooleanFieldEditor(PI.logPM,Lang.LogPM,getFieldEditorParent());
		addField(logPM);
		
		BooleanFieldEditor logFeed = new BooleanFieldEditor(PI.logFeed,Lang.LogFeed,getFieldEditorParent());
		addField(logFeed);
	}

}
