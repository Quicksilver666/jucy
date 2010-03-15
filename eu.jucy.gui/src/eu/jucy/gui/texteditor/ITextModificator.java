package eu.jucy.gui.texteditor;

import java.util.List;


import org.eclipse.swt.custom.StyledText;


import eu.jucy.gui.texteditor.StyledTextViewer.Message;
import eu.jucy.gui.texteditor.StyledTextViewer.TextReplacement;


import uc.IHub;

public interface ITextModificator {

	public static final String ExtensionpointID = "eu.jucy.gui.TextModificator";
	
	/**
	 * initialises the plugin for a certain StyledText and hub ..
	 * (this plugin is not a singleton)
	 * 
	 * @param st - the styled text where everything will be put
	 * @param viewer - the styled Textviewer that uses this textModificator..
	 * @param hub - the hub which is responsible for this
	 * 
	 */
	void init(StyledText st ,StyledTextViewer viewer, IHub hub);
	

	
	/**
	 * request modificators for the message ..
	 * 
	 * @param original - original unmodified message
	 * @param pm - if the message was received in pm or not
	 * @param replacement list to add replacements to
	 */
	void getMessageModifications(Message original,boolean pm,List<TextReplacement> replacement);

	
	/**
	 * calles when the TextModificator should cease to exist. / is disabled..
	 */
	void dispose();
	
}
