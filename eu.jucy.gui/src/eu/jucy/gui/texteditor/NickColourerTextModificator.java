package eu.jucy.gui.texteditor;

import helpers.PreferenceChangedAdapter;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import uc.IHub;
import uc.IUser;
import uihelpers.SUIJob;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;

public class NickColourerTextModificator implements ITextModificator {

	public static final String ID = "eu.jucy.gui.nickcolouring";
	
	private static Color ownNickCol;
	private static Font  ownNickFont;
	
	private static Color opNickCol;
	private static Font  opNickFont;
	
	private static Color favNickCol;
	private static Font  favNickFont;
	
	private static Color normalNickCol;
	private static Font  normalNickFont;
	
	private static boolean active;
	
	

	static {
		loadFontsAndColours();
		
		new PreferenceChangedAdapter(GUIPI.get(),
				GUIPI.ownNickCol,GUIPI.ownNickFont,GUIPI.opNickCol,GUIPI.normalNickCol,
				GUIPI.opNickFont,GUIPI.favNickCol,GUIPI.favNickFont,GUIPI.normalNickFont,GUIPI.IDForTextModificatorEnablement(ID) ) {

			private volatile boolean changeRunning = false;
			
			@Override
			public void preferenceChanged(String preference, String oldValue,
					String newValue) {
				
				if (!changeRunning) {
					changeRunning = true;
					new SUIJob() {
						@Override
						public void run() {
							changeRunning = false;
							loadFontsAndColours();
						}
					}.schedule(500);
				}
			}
			
		};
		
	}
	
	private static void loadFontsAndColours() {
		ownNickCol = GUIPI.getColor(GUIPI.ownNickCol);
		opNickCol = GUIPI.getColor(GUIPI.opNickCol);
		favNickCol = GUIPI.getColor(GUIPI.favNickCol);
		normalNickCol = GUIPI.getColor(GUIPI.normalNickCol);
		ownNickFont = GUIPI.getFont(GUIPI.ownNickFont);
		opNickFont = GUIPI.getFont(GUIPI.opNickFont);
		favNickFont = GUIPI.getFont(GUIPI.favNickFont);
		normalNickFont = GUIPI.getFont(GUIPI.normalNickFont);
		active = GUIPI.getBoolean(GUIPI.IDForTextModificatorEnablement(ID));
	}
	
	public NickColourerTextModificator() {
	}



	public void getStyleRange(String message, int startpos, Message original,
			List<StyleRange> ranges, List<ObjectPoint<Image>> images) {
		IUser usr = original.getUsr();
		if (usr != null) {
			int nickstart = message.indexOf('<')+1+startpos;
			int length = usr.getNick().length();
			StyleRange sr = null;
			if (usr.getHub().getSelf().equals(usr)) {
				sr = getStyleRange(ownNickCol,ownNickFont,nickstart,length);
			} else if (usr.isFavUser()) {
				sr = getStyleRange(favNickCol,favNickFont,nickstart,length);
			} else if (usr.isOp()) {
				sr = getStyleRange(opNickCol,opNickFont,nickstart,length);
			} else {
				sr = getStyleRange(normalNickCol,normalNickFont,nickstart,length);
			}
			if (sr != null) {
				ranges.add(sr);
			}
			int currentpos = message.indexOf('>')+1;
			String ownNick = usr.getHub().getSelf().getNick();
			while (-1 != (currentpos = message.indexOf(ownNick, currentpos)) ) {
				StyleRange sr2 = getStyleRange(ownNickCol,ownNickFont,currentpos+startpos,ownNick.length());
				ranges.add(sr2);
				currentpos+= ownNick.length();
			}
		}
	}
	
	
	/**
	 * for usercolumn to find out if nickcolouring is active
	 * 
	 * @return
	 */
	public static boolean isActive() {
		return active;
	}

	
	
	public static Color getColor(IUser usr) {
		if (usr.getHub().getSelf().equals(usr)) {
			return ownNickCol;
		} else if (usr.isFavUser()) {
			return favNickCol;
		} else if (usr.isOp()) {
			return opNickCol; 
		} else {
			return normalNickCol;
		}
	}
	public static Font getFont(IUser usr) {
		if (usr.getHub().getSelf().equals(usr)) {
			return ownNickFont;
		} else if (usr.isFavUser()) {
			return favNickFont;
		} else if (usr.isOp()) {
			return opNickFont; 
		} else {
			return normalNickFont;
		}
	}
	
	
	
	private StyleRange getStyleRange(Color col, Font font, int nickstart,int length) {
		StyleRange sr = new StyleRange();
		sr.font = font;
		sr.foreground = col;
		sr.start = nickstart;
		sr.length = length;
		return sr;
	}
	
	public void dispose() {}

	public void init(StyledText st,StyledTextViewer viewer, IHub hub) {}

	public String modifyMessage(String message, Message original, boolean pm) {
		return message;
	}

}
