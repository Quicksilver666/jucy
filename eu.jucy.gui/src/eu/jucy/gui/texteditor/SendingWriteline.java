package eu.jucy.gui.texteditor;



import helpers.GH;


import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArrayList;


import logger.LoggerFactory;
import org.apache.log4j.Logger;


import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;



import eu.jucy.gui.texteditor.hub.HubEditor;
import eu.jucy.gui.texteditor.pmeditor.PMEditor;


import uc.DCClient;
import uc.IUser;
import uc.IUser.PMResult;
import uc.protocols.hub.Hub;
import uihelpers.SUIJob;


public abstract class SendingWriteline  {

	private static final Logger logger = LoggerFactory.make();
	
	private static final int HISTORYTYPED = 20;
	
	public static boolean lastKeyWasEnter;
	public static long lastKeyPressed;
	
	/**
	 * 
	 * @param timedifmillis  how much difference should be at least between typing and opening a popup..
	 * @return  true if last key pressed was enter ... or nothing was typed for provided time..
	 */
	public static boolean checkOpenPopUpExecution(long timedifmillis) {
		return lastKeyWasEnter || lastTypingOccurred(timedifmillis);
	}
	
	public static boolean lastTypingOccurred(long timedifmillis) {
		return lastKeyPressed + timedifmillis < System.currentTimeMillis();
	}
	
	
	private final Text writeline;
	

	/**
	 * sent messages stuff .. 
	 * store for messages sent recently..
	 * -> navigateing..
	 */
	private final List<String> sentMessages = new CopyOnWriteArrayList<String>();
	
	private ListIterator<String> ctrlUpPosition;
	//if thats tool ong ago -> reset ctrlUpPosition
	private long recentCTRLUpPressedTime = System.currentTimeMillis(); 
	private String savedText; //save text that was currently in the writeline before up and down were pressed..

	/**
	 * com-variable to determine if text can be sent to hub
	 * by pressing enter.. only when no completion proposal is open
	 * text should be sent..
	 */
	private boolean proposalClosed = true;
	
	private final CommandInterpreter interpreter;
	
	private boolean ctrlPressed = false;
	


	/**
	 * 
	 * @param line - the Text where the user types
	 * @param users - the users needed for completion (kept in the HubEditor)
	 */
	public SendingWriteline(Text line,SortedMap<String,IUser> users, CommandInterpreter inter) {
		this.writeline = line;
		this.interpreter = inter;
		
		UserNameCompleter unc = new UserNameCompleter(users);
		
		ContentProposalAdapter cpa = new ContentProposalAdapter(
				writeline,unc,unc, 
				KeyStroke.getInstance(KeyStroke.NO_KEY,SWT.TAB ),null); 
		cpa.setPopupSize(new Point(300,200));

		//disables sending while proposal pop-up is open
		cpa.addContentProposalListener(new IContentProposalListener2() {

	
			public void proposalPopupClosed(ContentProposalAdapter adapter) {
				new SUIJob() { //done in UIjob so it is postponed until the key listener is called (Enter Key that was used for closing the proposal)
					@Override
					public void run() {
						proposalClosed = true;
					}
				}.schedule();
				logger.debug("proposal popup closed");
			}

			public void proposalPopupOpened(ContentProposalAdapter adapter) {
				proposalClosed = false;
				logger.debug("proposal popup opened");
			}
		});
		writeline.addKeyListener(new KeyAdapter() {
			
			public void keyPressed(final KeyEvent e) {
				lastKeyWasEnter = false;
				lastKeyPressed = System.currentTimeMillis();
				
				if (e.keyCode == SWT.CTRL) {
					ctrlPressed = true;
				}
				//logger.info("ctrl up or down pressed "+e.keyCode+ "  "+SWT.ARROW_UP);
				if (proposalClosed && ctrlPressed && (e.keyCode == SWT.ARROW_UP|| e.keyCode == SWT.ARROW_DOWN)) {
					ctrlAndUpOrDownPressed(e.keyCode == SWT.ARROW_UP );
					//logger.info("ctrl up or down pressed");
					e.doit = false;
					return;
				}
				
				if (proposalClosed && !ctrlPressed
					&& (e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.CR)) {
					lastKeyWasEnter = true;
			
					e.doit = false; //else the 'CR' would just be in the way and represent a newline
					
					String sendText = writeline.getText();
					if (!GH.isEmpty(sendText.trim())) {
						if (interpreter.isCommand(sendText)) {
							interpreter.executeCommand(sendText);
							writeline.setText("");
							logger.debug("line is command. "+sendText);
						} else {
							send(sendText);
							sentMessages.add(0,sendText);
							if (sentMessages.size() > HISTORYTYPED) {
								sentMessages.remove(sentMessages.size()-1);
							}
							logger.debug("line is chat message "+sendText);
							writeline.setText("");
						}
					}
				} 
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (SWT.CTRL == e.keyCode) {
					ctrlPressed = false;
				}
			}
			
		});
		writeline.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				ctrlPressed = false;
			}
			
		});
	}
	

	/**
	 * moves to the next sentMessage ..
	 * if nothing is pressed for more than 10 secs  we forget where we were..
	 * @param up -  if true move up else down..
	 */
	private void ctrlAndUpOrDownPressed(boolean up) {
		if (ctrlUpPosition == null || System.currentTimeMillis()-recentCTRLUpPressedTime > 10000) {
			ctrlUpPosition = sentMessages.listIterator();
			savedText = writeline.getText();
		}
		
		if (up?ctrlUpPosition.hasNext(): ctrlUpPosition.hasPrevious()) {
			writeline.setText(up ? ctrlUpPosition.next(): ctrlUpPosition.previous());
		} else if (!up) {  //if its down and has no previous -> set original text
			writeline.setText(savedText);
		}
		
		recentCTRLUpPressedTime  = System.currentTimeMillis();
	}
	
	
	public abstract void send(String s);



	public Text getWriteline() {
		return writeline;
	}

	
	public static class HubSendingWriteline extends SendingWriteline {

		private final Hub hub;
		public HubSendingWriteline(Text line, SortedMap<String, IUser> users, Hub hub,HubEditor he) {
			super(line, users, new CommandInterpreter(hub,he));
			this.hub = hub;
		}

		@Override
		public void send(final String s) {
			DCClient.execute(new Runnable() {
				public void run() {
					hub.sendMM(s, false);
				}
			});
		}
		
	}
	
	public static class UserSendingWriteline extends SendingWriteline {
		private final IUser usr;
		private final PMEditor pme;

		public UserSendingWriteline(Text line, SortedMap<String, IUser> users, IUser other, PMEditor pme) {
			super(line, users, new CommandInterpreter(other,pme));
			this.usr = other;
			this.pme = pme;
		}

		@Override
		public void send(final String s) {
			DCClient.execute(new Runnable() {
				public void run() {
					PMResult pmres = usr.sendPM(s, false, true);
					if (pmres == PMResult.STORED) {
						pme.storedPM(usr,s, false);
					}
				}
			});
		}
	}
	
	
	
	
	
}
