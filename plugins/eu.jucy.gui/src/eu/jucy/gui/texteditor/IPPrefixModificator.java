package eu.jucy.gui.texteditor;

import java.net.InetAddress;
import java.util.List;

import org.eclipse.swt.custom.StyledText;

import uc.IHub;
import uc.IUser;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;
import eu.jucy.gui.texteditor.StyledTextViewer.TextReplacement;

public class IPPrefixModificator implements ITextModificator {

	public void init(StyledText st, StyledTextViewer viewer, IHub hub) {}

	public void getMessageModifications(Message original, boolean pm,List<TextReplacement> replacement) {
		IUser usr = original.getUsr();
		if (usr != null) { 
			InetAddress use = usr.getIp() != null ? usr.getIp():usr.getI6();
			if (use != null) {
				replacement.add(new TextReplacement(1, 0, "["+use.getHostAddress()+"]" ));
			}
		}
	}

	public void dispose() {}

}
