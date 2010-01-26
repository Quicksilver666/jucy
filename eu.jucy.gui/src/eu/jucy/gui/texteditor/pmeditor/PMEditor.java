package eu.jucy.gui.texteditor.pmeditor;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import eu.jucy.language.LanguageKeys;



import uc.IHub;
import uc.IUser;
import uc.listener.IUserChangedListener;
import uc.protocols.hub.Hub;
import uc.protocols.hub.PrivateMessage;
import uihelpers.SUIJob;
import uihelpers.ToasterUtil;


public class PMEditor extends UCTextEditor implements  IUserChangedListener {

	private static final Logger logger = LoggerFactory.make();
	
	public static final String ID = "eu.jucy.PM";
	
	
	private boolean messagesWaiting = false;
	private volatile boolean userOnline = true;
	private boolean firstMessage = true;
	
	private CLabel feedLabel;
	private Text text;
	private MyStyledText pmText;
	
	private LabelViewer labelViewer;
	private StyledTextViewer textViewer;



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
		labelViewer = new LabelViewer(feedLabel);
		
		setText(pmText);
		makeTextActions();
		
		ApplicationWorkbenchWindowAdvisor.get().getPopulation()
			.registerUserChangedListener(this, getUser().getUserid());
		
		setTitleImage( Nick.GetUserImage(getUser()));
		
		setControlsForFontAndColour(text,pmText);
		
	}


	@Override
	public void partActivated() {
		super.partActivated();
		messagesWaiting = false;
		setCurrentimage();
	}
	


	public void setFocus() {
		text.setFocus();
	}
	
	public void dispose() {
		//textViewer.dispose();
		ApplicationWorkbenchWindowAdvisor.get().getPopulation()
			.unregisterUserChangedListener(this, getUser().getUserid());
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
			
			if (pm.getSender() == null) {
				append(completeMessage,null,pm.getTimeReceived());
			} else {
				append(completeMessage,pm.getSender(),pm.getTimeReceived());
			}
			
			boolean editorActive = getSite().getPage().getActiveEditor() == this;
			//show toaster message if wished
			
			if (!editorActive || firstMessage) { 
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
			if (!messagesWaiting && !editorActive) {
				messagesWaiting = true;
				setCurrentimage();
			}
		}
	}

	/* (non-Javadoc)
	 * @see UC.listener.IUserChangedListener#changed(UC.datastructures.User)
	 */
	public void changed(UserChangeEvent uce) {
		final UserChange type = uce.getType();
		if (!type.equals(UserChange.CHANGED)) {
			userOnline = type == UserChange.CONNECTED;
			append("*** "+type+" ***",null,System.currentTimeMillis());
			new SUIJob(feedLabel) {
				public void run() {
					labelViewer.addMessage("*** "+type+" ***");
					setCurrentimage();
				}
			}.schedule();
			fireTopicChangedListeners();
		}
		
	}
	
	
	
	private void setCurrentimage() {
		if (messagesWaiting) {
			setTitleImage(userOnline?newMessage:newMessageOffline);
		} else {
			setTitleImage( Nick.GetUserImage(getUser()));
		}
	}
	
	private void append(final String text,final IUser usr,final long received) {
		new SUIJob(this.text) {
			public void run() {
				textViewer.addMessage(text,usr,new Date(received));
			}	
		}.schedule();		
	}
	

	
	
	@Override
	public void storedPM(String message, boolean me) {
		append("*** stored PM: "+message,null,System.currentTimeMillis()); //TODO internationalize
	}


	private IUser getUser() {
		return ((PMEditorInput)getEditorInput()).getOther();
	}

	
	@Override
	public IHub getHub() {
		return getUser().getHub();
	}


	public String getTopic() {
		IHub hub = getUser().getHub();
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
	
	private static final List<PrivateMessage> pair = Collections.synchronizedList(new ArrayList<PrivateMessage>()); 
	
	
	public static void addPM(PrivateMessage pm) {
		pair.add(pm);
		if (pair.size() <= 3) { // -> maximum 3 SUIJobs running here in parallel
			new SUIJob() {
				@Override
				public void run() {
					boolean showInMainchat = GUIPI.getBoolean(GUIPI.showPMsInMC);
					IWorkbenchPage page = getWindow().getActivePage();
					boolean mayOpen =!showInMainchat && (SendingWriteline.checkOpenPopUpExecution(1500) || !GUIPI.getBoolean(GUIPI.openPMInForeground));
					synchronized(pair) {
						for (Iterator<PrivateMessage> it = pair.iterator(); it.hasNext();) {
							PrivateMessage current = it.next();
						
							PMEditorInput pmi = new PMEditorInput(current.getFrom(), current.getTimeReceived());
							PMEditor pme = (PMEditor)page.findEditor(pmi);
								
							if ( pme == null && mayOpen) { 
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
									he.appendText(s, current.getSender());
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
