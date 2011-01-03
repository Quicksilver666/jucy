package eu.jucy.gui.texteditor;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;
import eu.jucy.gui.settings.UCPrefpage;

public class TexteditorPrefPage extends UCPrefpage {

	public TexteditorPrefPage() {
		super(Application.PLUGIN_ID);
	}


	@Override
	protected void createFieldEditors() {
		
		BooleanFieldEditor openPMInForeground = new BooleanFieldEditor(GUIPI.openPMInForeground,
				Lang.OpenPMinForeground,getFieldEditorParent());
		addField(openPMInForeground);
		
		BooleanFieldEditor redirectPmToMainchat = new BooleanFieldEditor(GUIPI.showPMsInMC,
				Lang.ShowPMInMainchat,getFieldEditorParent());
		addField(redirectPmToMainchat);
		
		
		String[] fieldName = new String[]{GUIPI.showToasterMessages,GUIPI.showToasterMessagesChatroom,GUIPI.showToasterMessagesNickinMC};
		String[] popupText = new String[]{Lang.ShowPOPUPonPM,Lang.ShowPOPUPonChatroom, Lang.ShowPOPUPonNickinMC};
		String[] fieldNameSound = new String[]{GUIPI.addSoundOnPM,GUIPI.addSoundOnChatroomMessage,GUIPI.addSoundOnNickinMC};
		String[] soundText = new String[]{Lang.AddSoundNotificationToPM,Lang.AddSoundNotificationToChatroom,Lang.AddSoundOnNickInMainchat};
	
		
		for (int i = 0; i < fieldName.length; i++) {
			addField(new BooleanFieldEditor(fieldName[i],popupText[i],getFieldEditorParent()));
			addField(new BooleanFieldEditor(fieldNameSound[i],soundText[i],getFieldEditorParent()));
		}
		
		IntegerFieldEditor ife = new IntegerFieldEditor(GUIPI.toasterTime, Lang.PopupDissappearanceTime, getFieldEditorParent());
		ife.setValidRange(100, 60*1000);
		addField(ife);
		
		
		/*
		BooleanFieldEditor showToasterMessages = new BooleanFieldEditor(GUIPI.showToasterMessages,
				Lang.ShowPOPUPonPM,getFieldEditorParent());
		addField(showToasterMessages);
		
		BooleanFieldEditor addSoundOnPM = new BooleanFieldEditor(GUIPI.addSoundOnPM,
				Lang.AddSoundNotificationToPopup,getFieldEditorParent());
		addField(addSoundOnPM);
		*/
		

	}

}
