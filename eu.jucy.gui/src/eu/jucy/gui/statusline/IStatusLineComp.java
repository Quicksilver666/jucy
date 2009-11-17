package eu.jucy.gui.statusline;

public interface IStatusLineComp {

	void setText();
	
	void pack();
	
	/**
	 * 
	 * @return the number of characters to be put into the label
	 */
	int getNumberOfCharacters();
}
