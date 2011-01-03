package eu.jucy.gui;

import java.util.List;

import org.eclipse.jface.action.IContributionItem;

/**
 * methods any UCEditor must have
 * @author Quicksilver
 *
 */
public interface IUCEditor {

	/**
	 * the topic to be shown in the top of the client
	 * when this editor is active
	 */
	String getTopic();
	
	
	void registerTopicChangedListener(ITopicChangedListener tcl);
	
	
	void unregisterTopicChangedListener(ITopicChangedListener tcl);
	
	
	void fireTopicChangedListeners();
	
	public static interface ITopicChangedListener  {
		
		void topicChanged(IUCEditor editor);
	}
	
	/**
	 * 
	 * 
	 * contribution items used for the top tab
	 * @param list for adding all items the Editor wants on the menu of its tab
	 */
	void getContributionItems(List<IContributionItem> items);
	
	/**
	 * called right before tabmenu is shown..
	 */
	void tabMenuBeforeShow();
	
	/**
	 * called when ever the current part gets activated...
	 */
	void partActivated();
	
}
