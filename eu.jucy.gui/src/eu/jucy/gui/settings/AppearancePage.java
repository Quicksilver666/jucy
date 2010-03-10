package eu.jucy.gui.settings;

import java.text.SimpleDateFormat;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;


public class AppearancePage extends UCPrefpage {
	

	public AppearancePage() {
		super(Application.PLUGIN_ID);
	}

	
	@Override
	protected void createFieldEditors() {
		FontFieldEditor ffe = new FontFieldEditor(
				GUIPI.editorFont,Lang.SelectFont, getFieldEditorParent() );
		addField(ffe);
		
		ColorFieldEditor window = new ColorFieldEditor(GUIPI.windowColor,Lang.WindowBackgroundColour,getFieldEditorParent());
		addField(window);
		
		ColorFieldEditor windowfont = new ColorFieldEditor(GUIPI.windowFontColor,Lang.FontColour,getFieldEditorParent());
		addField(windowfont);
		
	
		
		ColorFieldEditor download1 = new ColorFieldEditor(GUIPI.downloadColor1,Lang.PrimaryDownloadColour,getFieldEditorParent());
		addField(download1);
		
		ColorFieldEditor download2 = new ColorFieldEditor(GUIPI.downloadColor2,Lang.SecondaryDownloadColour,getFieldEditorParent());
		addField(download2);
		
		ColorFieldEditor upload1 = new ColorFieldEditor(GUIPI.uploadColor1,Lang.PrimaryUploadColour,getFieldEditorParent());
		addField(upload1);
		
		ColorFieldEditor upload2 = new ColorFieldEditor(GUIPI.uploadColor2,Lang.SecondaryUploadColour,getFieldEditorParent());
		addField(upload2);
		
		
		BooleanFieldEditor askBeforeShutdown = new BooleanFieldEditor(GUIPI.askBeforeShutdown,
				Lang.AskBeforeShutdown,getFieldEditorParent());
		addField(askBeforeShutdown);
		
		BooleanFieldEditor minimizeToTray = new BooleanFieldEditor(GUIPI.minimizeToTray,Lang.MinimizeToTray,getFieldEditorParent());
		addField(minimizeToTray);
		
		BooleanFieldEditor minimizeOnStart = new BooleanFieldEditor(GUIPI.minimizeOnStart, 
				Lang.MinimizeOnStart,getFieldEditorParent());
		addField(minimizeOnStart);
		
		BooleanFieldEditor setAwayOnMinimize = new BooleanFieldEditor(GUIPI.setAwayOnMinimize,Lang.SetAwayMessageOnMinimize,getFieldEditorParent());
		addField(setAwayOnMinimize);
		
		
		
		BooleanFieldEditor alternativeTabstyle = new BooleanFieldEditor(GUIPI.alternativePresentation,Lang.UseAlternativeTabs,getFieldEditorParent());
		addField(alternativeTabstyle);
		
		
		StringFieldEditor timestamps = new StringFieldEditor(GUIPI.timeStampFormat, Lang.TimeStamps,getFieldEditorParent()) {
			
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
		
		addField(timestamps);

	}

	

}
