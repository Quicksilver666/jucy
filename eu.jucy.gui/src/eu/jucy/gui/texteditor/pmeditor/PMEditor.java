package eu.jucy.gui.texteditor.pmeditor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import logger.LoggerFactory;



import org.apache.log4j.Logger;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;




import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;

import eu.jucy.gui.UserColumns.Nick;

import eu.jucy.gui.itemhandler.UserContributionItem;
import eu.jucy.gui.itemhandler.UCContributionItem.UserCommandsContributionItem;
import eu.jucy.gui.sounds.AePlayWave;
import eu.jucy.gui.sounds.IAudioKeys;
import eu.jucy.gui.texteditor.LabelViewer;
import eu.jucy.gui.texteditor.SendingWriteline;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.UCTextEditor;
import eu.jucy.gui.texteditor.SendingWriteline.UserSendingWriteline;
import eu.jucy.gui.texteditor.StyledTextViewer.MyStyledText;
import eu.jucy.gui.texteditor.hub.HubEditor;
import eu.jucy.gui.texteditor.hub.HubEditorInput;




import uc.IHasUser;
import uc.IHub;
import uc.IUser;
import uc.IUserChangedListener;
import uc.LanguageKeys;
import uc.Population;
import uc.protocols.hub.Hub;
import uc.protocols.hub.PrivateMessage;
import uihelpers.SUIJob;
import uihelpers.ToasterUtil;


public class PMEditor extends UCTextEditor implements  IUserChangedListener,IHasUser {

	private static final Logger logger = LoggerFactory.make();
	
	public static final String ID = "eu.jucy.PM";
	
	
	private volatile boolean userOnline = true;
	private boolean firstMessage = true;
	
	private CLabel feedLabel;
	private Text text;
	private MyStyledText pmText;
	

	
	


	@Override
	public void createPartControl(Composite parent) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 4;
		gridLayout.marginWidth = 2;
		gridLayout.verticalSpacing = 2;
		gridLayout.horizontalSpacing = 2;
		parent.setLayout(gridLayout);

		pmText = new MyStyledText(parent, SWT.BORDER| SWT.V_SCROLL | SWT.READ_ONLY  | SWT.WRAP);
		pmText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		Hub hub = (Hub)getHub();
		textViewer = new StyledTextViewer(pmText,hub,true,getUser(), 
				((PMEditorInput)getEditorInput()).getTime());
		pmText.setViewer(textViewer);
		
		text = new Text(parent,  SWT.MULTI | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sendingWriteline = new UserSendingWriteline(text,hub.getUserPrefix(), getUser(),this);


		feedLabel = new CLabel(parent, SWT.BORDER);
		feedLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		feedLabelViewer = new LabelViewer(feedLabel);
		
		setText(pmText);
		makeTextActions();
		
		ApplicationWorkbenchWindowAdvisor.get().getPopulation()
			.registerUserChangedListener(this, getUser().getUserid());
		
		setTitleImage( Nick.getUserImage(getUser(),true));
		
		setControlsForFontAndColour(text,pmText);
		
	}

	


	@Override
	protected void userRemovedFromRecentChatted(IUser usr) {
		getPop().unregisterUserChangedListener(this, usr.getUserid());
	}


	@Override
	protected void userAddedToRecentChatted(IUser usr) {
		getPop().registerUserChangedListener(this, usr.getUserid());
	}



	private Population getPop() {
		return ApplicationWorkbenchWindowAdvisor.get().getPopulation();
	}

	public void setFocus() {
		text.setFocus();
	}
	
	public void dispose() {
		//textViewer.dispose();
		openPMEditors--;
		getPop().unregisterUserChangedListener(this, getUser().getUserid());
		super.dispose();
	}
	
	@Override
	public void getContributionItems(List<IContributionItem> items) {
		super.getContributionItems(items);

		items.add(new Separator());
		UserContributionItem uci = new UserContributionItem(getUser());
		uci.initialize(getSite());
		items.add(uci);
		
		items.add(new Separator());
		UserCommandsContributionItem ucci = new UserCommandsContributionItem(getUser());
		ucci.initialize(getSite());
		items.add(ucci);
	}
	

	/* (non-Javadoc)
	 * @see UC.listener.IPMReceivedListener#pmReceived(UC.datastructures.User, UC.datastructures.User, java.lang.String)
	 */

	public void pmReceived(PrivateMessage pm) {
		if (getUser().equals(pm.getFrom())) { //if this PM is for us... show it
			final String completeMessage =  pm.toString();
			
			appendText(completeMessage, pm.getSender(),pm.getTimeReceived(),true);
			
			
			//show toaster message if wished
			
			if (!isActiveEditor() || firstMessage) { 
				final boolean chatroom = !pm.fromEqualsSender();
				if (GUIPI.getBoolean( chatroom ? 
						GUIPI.showToasterMessagesChatroom : 
						GUIPI.showToasterMessages)) {
					
					new SUIJob() {
						public void run() {
							ToasterUtil.showMessage(completeMessage,GUIPI.getInt(GUIPI.toasterTime));
						}
					}.schedule();
				}
				if (GUIPI.getBoolean(chatroom? 
						GUIPI.addSoundOnChatroomMessage :
						GUIPI.addSoundOnPM)) {
					AePlayWave.playWav(IAudioKeys.BLIP);
				}
			}
			firstMessage = false;
			
			//change image to absent.. if needed..
			if (!messagesWaiting && !isActiveEditor()) {
				messagesWaiting = true;
				setTitleImage();
			}
		}
	}

	/* (non-Javadoc)
	 * @see UC.listener.IUserChangedListener#changed(UC.datastructures.User)
	 */
	public void changed(UserChangeEvent uce) {
		final UserChange type = uce.getType();
		if (!type.equals(UserChange.CHANGED)) {
			boolean joined = type == UserChange.CONNECTED;
			if (uce.getChanged().equals(getUser())) {
				userOnline = joined;
				statusMessage(type.toString(),0);
				fireTopicChangedListeners();
			} else {
				if (getHub().getFavHub().isShowRecentChatterJoins()) {
					showJoinsParts(uce.getChanged(), joined);
				}
			}
		}	
	}
	
	
	
	protected void setTitleImage() {
		new SUIJob(this.text) {
			public void run() {
				if (messagesWaiting) {
					setTitleImage(userOnline?newMessage:newMessageOffline);
				} else {
					setTitleImage( Nick.getUserImage(getUser(),true));
				}
			}	
		}.scheduleOrRun();
	}
	
	@Override
	public void appendText(String text, IUser usr,long received,boolean chatMessage) {
		super.appendText(text, usr, received,chatMessage);
	}
	
	@Override
	public void statusMessage(final String message,  int severity) {
		super.statusMessage(message, severity);
		setTitleImage();
	}
	
	@Override
	public void storedPM(IUser usr,String message, boolean me) {
		statusMessage(String.format(Lang.StoredPM, message), 0); 
	}


	public IUser getUser() {
		return ((PMEditorInput)getEditorInput()).getOther();
	}

	
	@Override
	public IHub getHub() {
		return getUser().getHub();
	}


	public String getTopic() {
		IHub hub = getHub();
		if (hub != null) {
			return getUser().getNick()+" - "+ hub.getName();
		} else {
			return getUser().getNick()+" - "+ LanguageKeys.UserOffline;
		}
	}

	
	public void clear() {
		textViewer.clear();
	}
	
	
	public static PMEditor openPMEditor(final PMEditorInput pme) {
		try {
		
			IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			IEditorPart activeBefore = page.getActiveEditor();
			logger.debug("editoropening..: foregound: "+GUIPI.getBoolean(GUIPI.openPMInForeground));
			PMEditor pmei = (PMEditor)page.openEditor(
					pme, 
					PMEditor.ID, 
					GUIPI.getBoolean(GUIPI.openPMInForeground));  
			
			if (!GUIPI.getBoolean(GUIPI.openPMInForeground) && activeBefore != null) {
				page.activate(activeBefore);
			}
	
			return pmei;
			
		} catch (PartInitException pie) {
			MessageDialog.openError(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(),
					"Error", "Error opening PMEditor:" + pie.getMessage());
			throw new IllegalStateException(pie);
		}
	}
	
	private static final List<PrivateMessage> pair = 
		Collections.synchronizedList(new ArrayList<PrivateMessage>()); 
	
	private static int openPMEditors = 0;
	private static final int MAX_PMEDITORS_OPEN = 30; //security against ressource usage... 30 pms at most..
	
	
	public static void addPM(PrivateMessage pm) {
		pair.add(pm);
		if (pair.size() <= 3) { // -> maximum 3 SUIJobs running here in parallel
			new SUIJob() {
				@Override
				public void run() {
					boolean showInMainchat = openPMEditors > MAX_PMEDITORS_OPEN || GUIPI.getBoolean(GUIPI.showPMsInMC);
					IWorkbenchPage page = getWindow().getActivePage();
					boolean mayOpen =!showInMainchat && (SendingWriteline.checkOpenPopUpExecution(1500) || !GUIPI.getBoolean(GUIPI.openPMInForeground));
					synchronized(pair) {
						for (Iterator<PrivateMessage> it = pair.iterator(); it.hasNext();) {
							PrivateMessage current = it.next();
						
							PMEditorInput pmi = new PMEditorInput(current.getFrom(), current.getTimeReceived());
							PMEditor pme = (PMEditor)page.findEditor(pmi);
								
							if ( pme == null && mayOpen) { 
								openPMEditors++;
								pme = PMEditor.openPMEditor(pmi);
							}  
							if (pme != null) {
								it.remove();
								pme.pmReceived(current);
							} else if (showInMainchat) {
								it.remove();
								String s = String.format("[PM: %s]<%s> %s",current.getFrom().getNick(),current.getSender().getNick(),current.getMessage());
								HubEditorInput hei = new HubEditorInput(current.getFrom().getHub().getFavHub()); 
								HubEditor he =  (HubEditor)page.findEditor(hei);
								if (he != null) {
									he.appendText(s, current.getSender(),true);
								}
								
							}
						}	
						if (!pair.isEmpty()) {
							schedule(400);
						}
					}
				}
					
			}.schedule();
		}
		
	}
	

	



}
