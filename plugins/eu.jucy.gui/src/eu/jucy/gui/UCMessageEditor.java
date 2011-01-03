package eu.jucy.gui;



import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;


public abstract class UCMessageEditor extends UCEditor implements ISearchableEditor {

	protected static final Image 	newMessage ,
									newMessageOffline;
	
	private StyledText text;
	

	

	static {
		newMessage = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.NEWPM).createImage();
		newMessageOffline= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.NEWPMOFFLINE).createImage();
		

	}
	
	public UCMessageEditor() {}

	
	private String lastSearch;
	private int lastHit = -1;
	

	
	/**
	 * should clear TextField in the editor
	 */
	public abstract void clear();
	
	/**
	 * the text used to present the messages
	 * @return
	 */
	public final StyledText getText() {
		return text;
	}

	
	/**
	 * must be called by any implementing.. class
	 * 
	 * @param text the text that represents the message..
	 */
	public void setText(StyledText text) {
		this.text = text;
		setControlsForFontAndColour(text);
	}
	
	

	public void next() {
		if (lastSearch != null) {
			String text = getText().getText().toLowerCase();
			lastHit = text.indexOf(lastSearch, lastHit+1);
			if (lastHit != -1) {
				getText().setSelection(lastHit, lastHit+ lastSearch.length());
			}
		}
	}

	public void search(String searchString) {
		searchString = searchString.toLowerCase();
		if (!searchString.equals(lastSearch)) {
			lastSearch = searchString;
			lastHit = -1;
		} 
		
		next();
	}
	
	
	
}
