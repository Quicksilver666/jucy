package eu.jucy.gui.texteditor;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.text.SimpleDateFormat;
import java.util.List;

import org.eclipse.swt.custom.StyledText;


import eu.jucy.gui.GUIPI;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;
import eu.jucy.gui.texteditor.StyledTextViewer.TextReplacement;

import uc.IHub;
import uihelpers.SUIJob;


public class DateStampTextModificator implements ITextModificator {

	public static final String ID = "eu.jucy.gui.DateStamp";
	
	
	private static SimpleDateFormat dateFormat;
	
	static {
		refreshSettings();
		new PreferenceChangedAdapter(GUIPI.get(),GUIPI.timeStampFormat) {
			@Override
			public void preferenceChanged(String preference,String oldValue, String newValue) {
				new SUIJob() {
					@Override
					public void run() {
						refreshSettings();
					}

				}.schedule();
			}
		};
	}
	
	private static void refreshSettings() {
		String format = GUIPI.get(GUIPI.timeStampFormat);
		if (GH.isNullOrEmpty(format)) {
			dateFormat = new SimpleDateFormat(); 
		} else {
			dateFormat = new SimpleDateFormat(format);
		}
	}
	
	
	
//	
//	public void getStyleRange(String message, int startpos, Message original,
//			List<StyleRange> ranges, List<ObjectPoint<Image>> images) {
//
//	}




	public void init(StyledText st,StyledTextViewer viewer, IHub hub) {}

	
	
//	public String modifyMessage(String message, Message original, boolean pm) {
//		return dateFormat.format(original.getReceived()) +message;
//	}
	
	public void getMessageModifications(Message original, boolean pm,List<TextReplacement> replacement) {
		replacement.add(new TextReplacement(1, 0, dateFormat.format(original.getReceived())));
	}




	public void dispose() {}
}
