package eu.jucy.gui.texteditor;

import java.util.List;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;

import eu.jucy.gui.texteditor.StyledTextViewer.Message;


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
	 * 
	 * @param message - the message to Modify as it is currently
	 * @param original - user and time stamp.. also the umodified text..
	 * @return the message string possibly modified 
	 *  null if the message should be ignored and not printed 
	 */
	String modifyMessage(String message,Message original, boolean pm);
	
	
	/**
	 * it is guaranteed that this is called immediately after modifyMessage()
	 * before modifyMessage is called on any other message
	 * 
	 * @param message the message that should be modified with styles
	 * now with time stamps and all modifications that might have taken place
	 * 
	 * @param startpos the starting position for the message in the text
	 * @param originalMessage is the Message how it was before modifyMessage was called on it..
	 * @return ranges all style ranges this plugin wants to place on the message.
	 * @return images  - all images that should be added to the message..
	 * corresponding styleRange must be created @see{ImagePoint.java}
	 * and may be other stuff...
	 */
	void getStyleRange(String message, int startpos,Message original, List<StyleRange> ranges,List<ObjectPoint<Image>> images);
	
	
	/**
	 * calles when the TextModificator should cease to exist. / is disabled..
	 */
	void dispose();
	
}
