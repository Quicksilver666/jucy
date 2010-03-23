/**
 * 
 */
package eu.jucy.gui.texteditor;

import java.util.SortedMap;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;


import uc.IUser;


/**
 * Completes user's names by Tab .. presents window with choices..
 * 
 * TODO may be also complete commands
 * 
 * @author Quicksilver
 *
 */
public class UserNameCompleter extends TextContentAdapter implements IContentProposalProvider {

	private static final Logger logger = LoggerFactory.make();
	
	private final SortedMap<String,IUser> users;
	
	public UserNameCompleter(SortedMap<String,IUser> users) {
		this.users = users;
	}
	
	/**
	 * modified insert function .. 
	 */
	@Override
	public void insertControlContents(Control control, String text,int cursorPosition) {
		Text t = (Text)control;
		int space = t.getText().lastIndexOf(' ', t.getCaretPosition()-1 );
		
		logger.debug("current cursor:"+t.getCaretPosition()+
				" currentPos: "+cursorPosition+" space: "+space +" textlenght: "+t.getText().length());
		
		t.setSelection(space+1 ,t.getCaretPosition());
		
		logger.debug("current Selection:"+t.getSelection() );
		super.insertControlContents(control, text, cursorPosition);
	}
	

	public IContentProposal[] getProposals(String contents, int position) {
		try {
			String prefix = getPrefix(contents ,position);

			logger.debug("prefix:"+prefix+" content:"+contents +" position: "+position);
			int length = prefix.length();
			if (length != 0) {
				
				String lastkey = prefix.substring(0, length-1)+
									(char)(prefix.charAt(length-1)+1);
			
				logger.debug("lastkey:"+lastkey);
				
				return UserContentProposal.create(
						users.subMap(prefix, lastkey).values());
			
			} 
			
		} catch(Exception pe) {
			logger.error(pe,pe);
		}
		
		return new IContentProposal[]{};
	}
	
	private static String getPrefix(String contents ,int cursor) {
		int begin = contents.lastIndexOf(' ', cursor-1);
		if (begin == -1) {
			begin = 0;
		}
		logger.debug("prefix: "+begin+" "+cursor);
		return contents.substring(begin, cursor).toLowerCase().trim();
	}
	
}