package eu.jucy.gui.texteditor;



import logger.LoggerFactory;

import org.apache.log4j.Logger;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;


import uc.IHub;
import uc.IUser;

import uihelpers.SelectionProviderIntermediate;


import eu.jucy.gui.UCMessageEditor;
import eu.jucy.gui.UCWorkbenchPart;
import eu.jucy.gui.texteditor.hub.ItemSelectionProvider;

/**
 * 
 * holds common functionality of Hub and PM editor
 * 
 * 
 * @author Quicksilver
 *
 */
public abstract class UCTextEditor extends UCMessageEditor {

	public static final String TEXT_POPUP_ID = "eu.jucy.gui.texteditor";
	
	private static final Logger logger = LoggerFactory.make();
	
	protected SendingWriteline sendingWriteline;
	
	protected TextUserSelectionprovider tus;
	
	protected final SelectionProviderIntermediate spi = new SelectionProviderIntermediate();
	
	public UCTextEditor() {}

	
	public SendingWriteline getSendingWriteline() {
		return sendingWriteline;
	}

	public Text getWriteline() {
		return sendingWriteline.getWriteline();
	}
	
	protected void makeTextActions() {
		tus = new TextUserSelectionprovider(getText(),getHub());

		spi.addSelectionProvider(getText(), tus);
		getSite().setSelectionProvider(spi);
		
		UCWorkbenchPart.createContextPopups(getSite(), TEXT_POPUP_ID, tus, getText());
	}
	
	public void storedPM(String message,boolean me) {
		
	}

	
	
	@Override
	public void partActivated() {
		super.partActivated();
		getText().redraw();
	}


	public abstract IHub getHub();
	
	
	/**
	 * selects user if a nick is under the mousepointer
	 * otherwise selects the text currently selected
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class TextUserSelectionprovider extends ItemSelectionProvider implements ISelectionProvider {
		
		private final StyledText text;
		private final IHub hub;
		private final Point mousepos = new Point(0,0);
		/**
		 * 
		 * @param text a text on which selections happen
		 * @param hub where the users appearing in that text are from
		 */
		public TextUserSelectionprovider(StyledText text,IHub hub) {
			this.text = text;
			this.hub = hub;
			addListeners();
			setSelection(null);
		}
		
		private void addListeners() {
			text.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					mousepos.x = e.x;
					mousepos.y = e.y;
					IUser usr = getUsrFromPosition(mousepos.x,mousepos.y);
					logger.debug("slection set to: "+ (usr!= null?usr.toString():"empty"));
					if (usr != null) {
						setSelection(usr);
					} else {
						setSelection(text.getSelectionText());
					}
					
				}
	    	});
		}
		
		
		/**
		 * gets user by MousePosition in styled Text..
		 * 
		 * @return null if none found
		 */
		protected IUser getUsrFromPosition(int x, int y) {
			int cursorPos ;
			try {
				cursorPos = text.getOffsetAtLocation(new Point(x,y));
			} catch (IllegalArgumentException iae) {
				return null; //break .. don't know how to do this diffently..
			}
			
			String tFound = text.getText();
			int space = tFound.lastIndexOf(' ', cursorPos);
			int smaller = tFound.lastIndexOf('<', cursorPos);
			int smaller2 =  tFound.lastIndexOf('\n', cursorPos);
			int begin = Math.max(space, smaller);
			begin = Math.max(smaller2, begin);
			
			int space2 = tFound.indexOf(' ', cursorPos);
			int bigger = tFound.indexOf('>', cursorPos);
			int bigger2 = tFound.indexOf('\n', cursorPos);
			int end = smallestNonnegative(space2,  bigger);
			end = smallestNonnegative(end, bigger2);
			
//			logger.debug("spacebegin:"+space+" <begin:"+smaller+"\n"
//						+" spaceend:"+space2+" >ende:"+bigger +"\nstart:"+begin+" end:"+end);
			
			if (begin != -1 && end != -1 && begin < end) {
				String nick = tFound.substring(begin+1, end);
				logger.debug("nick found: "+nick);
				return hub.getUserByNick(nick);
			}
			return null;
		}
		
		private static int smallestNonnegative(int a ,int b) {
			if (a < 0 || b < 0) {
				return Math.max(a, b);
			} else {
				return Math.min(a, b);
			}
		}
		
	}

}
