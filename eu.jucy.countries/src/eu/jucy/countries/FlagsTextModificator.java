package eu.jucy.countries;

import geoip.GEOIP;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;

import uc.DCClient;
import uc.IHub;
import uc.IUser;

import eu.jucy.gui.texteditor.ITextModificator;
import eu.jucy.gui.texteditor.ObjectPoint;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;



public class FlagsTextModificator implements ITextModificator {

	
	private static final char  FLAGCHAR = (char)17;
	

	
	public FlagsTextModificator() {}


	public void init(StyledText st,StyledTextViewer viewer, IHub hub) {
		DCClient.execute(new Runnable() {
			public void run() {
				GEOIP.get(); //force initialisation
			}
		});
	}
	

	public void dispose() {}


	public void getStyleRange(String message, int startpos,
			Message originalMessage, List<StyleRange> ranges, List<ObjectPoint<Image>> images) {
		int i = message.indexOf(FLAGCHAR);
		if (i != -1) {
			ObjectPoint<Image> p = ObjectPoint.create(startpos+i , 1,  
					FlagStorage.get().getFlag(originalMessage.getUsr(),false,true), ranges);
			images.add(p);
		}
		
	}
	

	public String modifyMessage(String message, Message original, boolean pm) {
		if (message.indexOf(FLAGCHAR) >= 0) {
			message = message.replace(FLAGCHAR, ' ');
		}
		IUser user = original.getUsr();
		if (user != null && user.getIp() != null) {
			return FLAGCHAR+message;
		}
		
		return message;
	}

	
}
