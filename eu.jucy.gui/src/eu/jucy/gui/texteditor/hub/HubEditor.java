package eu.jucy.gui.texteditor.hub;





import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import helpers.GH;
import helpers.SizeEnum;

import logger.LoggerFactory;


import org.apache.log4j.Logger;




import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;


import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;


import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import eu.jucy.gui.Application;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UserColumns.Connection;
import eu.jucy.gui.UserColumns.Description;
import eu.jucy.gui.UserColumns.Email;
import eu.jucy.gui.UserColumns.Nick;
import eu.jucy.gui.UserColumns.Shared;
import eu.jucy.gui.UserColumns.Tag;
import eu.jucy.gui.downloadsview.DownloadsView;
import eu.jucy.gui.itemhandler.CommandDoubleClickListener;
import eu.jucy.gui.itemhandler.UCContributionItem.HubContributionItem;
import eu.jucy.gui.itemhandler.UserHandlers.GetFilelistHandler;
import eu.jucy.gui.sounds.AePlayWave;
import eu.jucy.gui.sounds.IAudioKeys;
import eu.jucy.gui.texteditor.LabelViewer;
import eu.jucy.gui.texteditor.MessageType;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.UCTextEditor;
import eu.jucy.gui.texteditor.SendingWriteline.HubSendingWriteline;
import eu.jucy.gui.texteditor.StyledTextViewer.MyStyledText;
import eu.jucy.gui.texteditor.pmeditor.PMEditor;
import eu.jucy.gui.transferview.TransferColumns;
import eu.jucy.gui.transferview.TransfersView;






import uc.FavHub;
import uc.IHasUser;
import uc.IHub;
import uc.IUser;
import uc.IUserChangedListener;
import uc.LanguageKeys;
import uc.protocols.ConnectionProtocol;
import uc.protocols.ConnectionState;
import uc.protocols.hub.FeedType;
import uc.protocols.hub.Hub;
import uc.protocols.hub.IHubListener;
import uc.protocols.hub.PrivateMessage;


import uihelpers.DelayedTableUpdater;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;
import uihelpers.ToasterUtil;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;



/**
 * Show hub to the user (Mainchat, Userlist and a line to write messages)
 * 
 * @author Quicksilver
 *
 */
public class HubEditor extends UCTextEditor implements IHubListener,IUserChangedListener {
	
	private static final int MAXTABLENGTH = 20; 

	private static final Logger logger = LoggerFactory.make();
	
	public static final String ID = "eu.jucy.hub";
	
	
	public static final Image 	GREEN_LED ,
								YELLOW_LED,
								RED_LED	,
								GREY_LED,
								GREEN_ENC_LED,
								GREENLKEYP_LED,
								NICK_CALLED,
								SCROLL_LOCK,
								SHOW_SIDEVIEW;     
		
	static {
		//load the images that show if the hub is connected or not.. 
		GREEN_LED	= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.LEDLIGHT_GREEN).createImage();
		YELLOW_LED	= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.LEDLIGHT_YELLOW).createImage();
		RED_LED		= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.LEDLIGHT_RED).createImage();    
		GREY_LED 	= new Image(null,RED_LED,SWT.IMAGE_GRAY);
		GREEN_ENC_LED = GuiHelpers.addCornerIcon(GREEN_LED, TransferColumns.UserColumn.ENC_ICON);
		GREENLKEYP_LED =  GuiHelpers.addCornerIcon(GREEN_LED, TransferColumns.UserColumn.ENCKEYP_ICON);
		NICK_CALLED = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.NICK_CALLED).createImage();   
		SCROLL_LOCK = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.SCROLL_LOCK).createImage();   
		SHOW_SIDEVIEW = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.SHOW_RIGHT_SIDEVIEW).createImage();   
		
	}


	
	private Hub hub;
	
	
	
	private boolean nickWasCalled = false;


	
	private TableViewerAdministrator<IUser> tva;
	
	//private StyledTextViewer textViewer;
	
	
	private DelayedTableUpdater<IUser> updater;
	

	private ISelectionListener transfersListener;

	

	
	private MyStyledText hubText;
	private TableViewer tableViewer;
	private CLabel feedLabel;
	private Text filterText;
	private Text writeline;
	private CLabel sharesizeLabel;
	private CLabel userLabel;
	private Table table;
	private SashForm sashForm;
	

	
	public HubEditor() {}
	

	@Override
	public void createPartControl(Composite parent) {
		final GridLayout gridLayoutx = new GridLayout();
		gridLayoutx.verticalSpacing = 2;
		gridLayoutx.marginWidth = 2;
		gridLayoutx.marginHeight = 4;
		gridLayoutx.horizontalSpacing = 2;
		gridLayoutx.numColumns = 1;
		parent.setLayout(gridLayoutx);
		
		
		sashForm = new SashForm(parent, SWT.NONE);
		hubText = new MyStyledText(sashForm, SWT.V_SCROLL | SWT.READ_ONLY  | SWT.WRAP);
		
		hubText.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
			}

			/**
			 * when the hubText is resized.. that means the sashForm probably changed
			 * therefore store size if it Users are visible
			 */
			public void controlResized(ControlEvent e) {
				if (table.isVisible()) {
					getInput().setWeights(sashForm.getWeights());
				}
			}
		});
		
		sashForm.setLayout(new FillLayout());
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		
		tableViewer= new TableViewer(sashForm, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER  );
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
	//	tableViewer.setUseHashlookup(true);
		tableViewer.setContentProvider(new UserContentProvider());
		
		
		List<ColumnDescriptor<IUser>> columns = new ArrayList<ColumnDescriptor<IUser>>();
		columns.addAll(Arrays.asList(new Nick(),new Shared(),new Description(),new Tag(),new Connection(),new Email()));
		
		tva = new TableViewerAdministrator<IUser>(
				tableViewer,
				columns,
				GUIPI.hubWindowUserTable,0);
				
		
		tva.apply();

		
		spi.addViewer(tableViewer); //->texteditor deleagte
		
		sashForm.setWeights(getInput().weights());
		
		final Composite composite_1 = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 2;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 2;
		gridLayout.horizontalSpacing = 2;
		gridLayout.numColumns = 2;
		composite_1.setLayout(gridLayout);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		writeline = new Text(composite_1,  SWT.MULTI | SWT.BORDER);
	
		writeline.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		writeline.setText("");
	
		

		filterText = new Text(composite_1, SWT.BORDER | SWT.SEARCH| SWT.ICON_SEARCH);
		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setFilter(filterText.getText());
			}
		});
		filterText.setMessage("<"+Lang.FilterUserlist+">"); 
		final GridData gridData_2 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData_2.widthHint = 120;
		filterText.setLayoutData(gridData_2);

		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 1;
		gridLayout.numColumns = 4;
		composite_1.setLayout(gridLayout);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		final Composite composite_2 = new Composite( parent , SWT.BORDER);

		
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.verticalSpacing = 1;
		gridLayout_1.marginWidth = 2;
		gridLayout_1.marginHeight = 1;
		gridLayout_1.horizontalSpacing = 3;
		gridLayout_1.numColumns = 4;
		
		composite_2.setLayout(gridLayout_1);
		final GridData gridData_3 = new GridData(SWT.FILL, SWT.FILL, true, false);
		composite_2.setLayoutData(gridData_3);
		
		feedLabel = new CLabel(composite_2, SWT.BORDER);
		feedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


		userLabel = new CLabel(composite_2, SWT.BORDER);
		userLabel.setLayoutData(new GridData(80, SWT.DEFAULT));

		sharesizeLabel = new CLabel(composite_2, SWT.BORDER);
		sharesizeLabel.setLayoutData(new GridData(60, SWT.DEFAULT));
		
		
		ToolBar toolBar = new ToolBar (composite_2, SWT.HORIZONTAL | SWT.SHADOW_OUT | SWT.FLAT);
		final ToolItem scroll = new ToolItem (toolBar, SWT.CHECK);
		scroll.setImage(SCROLL_LOCK);
		scroll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				textViewer.setScrollLock(!textViewer.isScrollLock());
			}
		});
		
		ToolItem showUserlist = new ToolItem (toolBar, SWT.CHECK |SWT.BORDER);
		showUserlist.setImage(SHOW_SIDEVIEW);
		showUserlist.addSelectionListener(new SelectionAdapter() {
			int[] weights = sashForm.getWeights();
			public void widgetSelected(SelectionEvent e) {
				if (table.isVisible()){
					table.setVisible(false);   
					weights = sashForm.getWeights();             
					sashForm.setWeights(new int[] {300, 0 });  
				} else {	
					table.setVisible(true); 
					try {
						sashForm.setWeights(weights);  
					} catch(IllegalArgumentException iae) {
						sashForm.setWeights(new int[]{300,100});
					}
					weights = null;
				}
			}
		});

		
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		


//		final Button button = new Button(composite_2, SWT.TOGGLE);
//		button.addSelectionListener(new SelectionAdapter() {
//			int[] weights = sashForm.getWeights();
//			
//			public void widgetSelected(final SelectionEvent e) {
//				if (table.isVisible()){
//					table.setVisible(false);   //set usertable invisible
//					weights = sashForm.getWeights();                // store the weigths of the sashform
//					sashForm.setWeights(new int[] {300, 0 });     // make only HubTextpart visible.. 
//				} else {	
//					table.setVisible(true); //set visible
//					try {
//						sashForm.setWeights(weights);                //and old weights
//					} catch(IllegalArgumentException iae) {
//						sashForm.setWeights(new int[]{300,100});
//					}
//					weights = null;
//				}
//			}
//		});
//		final GridData gridData = new GridData(SWT.RIGHT, SWT.FILL, false, false);
//		gridData.heightHint = 10;
//		gridData.widthHint = 10;
//		button.setLayoutData(gridData);
		//

		setText(hubText);
		
		
		this.hub = ApplicationWorkbenchWindowAdvisor.get().getHub(getInput(),false);   //here retrieve the hub  
		
		feedLabelViewer = new LabelViewer(feedLabel,hub);
		textViewer = new StyledTextViewer(hubText,hub,false);
		hubText.setViewer(textViewer);
		
		sendingWriteline = new HubSendingWriteline(writeline,hub.getUserPrefix(),hub,this);
		
		updater = new DelayedTableUpdater<IUser>(tableViewer) {
			protected void updateDone() {
				userLabel.setText(hub.getUsers().size()+" "+Lang.Users );
				sharesizeLabel.setText(SizeEnum.getReadableSize(hub.getTotalshare()));
			}
		};
		
		
		hub.registerHubListener(this);//register the listeners..
		tableViewer.setInput(getHub());
		hub.registerUserChangedListener(this);
	
		
		makeActions(); //create menu and actions
		logger.debug("created HubEditor partControl");
		
		
		setControlsForFontAndColour(hubText,tableViewer.getTable(),writeline,filterText );

	}


	
	@Override
	public void getContributionItems(List<IContributionItem> items) {
		super.getContributionItems(items);
		CommandContributionItemParameter ccip = 
			new CommandContributionItemParameter(getSite(), null, ReconnectHandler.COMMAND_ID,SWT.PUSH);
		
		ccip.icon = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.RECONNECT_SMALL);
		CommandContributionItem cci = new CommandContributionItem(ccip);
		items.add(cci);
		items.add(new Separator());
		HubContributionItem hci = new HubContributionItem(getHub());
		hci.initialize(getSite());
		items.add(hci);
	}
	
	/**
	 * creates actions and some listeners that create or handle actions 
	 * may only be called once refresh is done 
	 * with refreshActions();
	 */
	private void makeActions() { 
		logger.debug("makeActions()");
		
		//refreshActions();
		createContextPopup(tableViewer);
		makeTextActions();
		
		tableViewer.addDoubleClickListener(new CommandDoubleClickListener(GetFilelistHandler.COMMAND_ID));
		
		tus.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection iss = (IStructuredSelection)event.getSelection();
				if (iss.getFirstElement() instanceof IUser) {
					tableViewer.setSelection(new StructuredSelection(iss.getFirstElement()), true);
				}	
			}
		});
		
		transfersListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part,ISelection selection) {
				if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IHasUser) {
					IHasUser transfer = (IHasUser)((IStructuredSelection)selection).getFirstElement();
					if (getHub().equals(transfer.getUser().getHub())) {
						tableViewer.setSelection(new StructuredSelection(transfer.getUser()), true);
					}
				}
			}
		};
		
		ISelectionService sel = getSite().getWorkbenchWindow().getSelectionService();
		sel.addPostSelectionListener(TransfersView.ID,transfersListener);
		sel.addPostSelectionListener(DownloadsView.VIEW_ID, transfersListener);
		
	}
	




	
	
	@Override
	public void partActivated() {
		nickWasCalled = false;
		super.partActivated();
	}
	



	private void setFilter(final String filter) {
		tableViewer.resetFilters();
		if (filter != null  && filter.length() >= 3) {
			final String filterString = filter.toLowerCase();
			tableViewer.addFilter(new ViewerFilter(){
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					IUser u = (IUser)element;
					return 	u.getNick().toLowerCase().contains(filterString) 			||
					 		u.getDescription().toLowerCase().contains(filterString) 	||
					 		u.getEMail().toLowerCase().contains(filterString)			||
					 		u.getTag().toLowerCase().contains(filterString);
				}
			});
			tableViewer.refresh();
		}
	}
	
	
	public void setFocus() {
		writeline.setFocus();
	}
	
	/**
	 * new change version ..
	 * makes changes in Bulk so it needs less CPU..
	 */
	public void changed(UserChangeEvent uce) {	
		switch(uce.getType()) {
		case CONNECTED:
			updater.add(uce.getChanged());
			if (shouldJoinPartBeShown(uce.getChanged())) {
				 showJoinsParts(uce.getChanged(),true);
			}
			break;
		case CHANGED:
			updater.change(uce.getChanged());
			break;
		case DISCONNECTED:
		case QUIT:
			updater.remove(uce.getChanged());
			if (shouldJoinPartBeShown(uce.getChanged())) {
				showJoinsParts(uce.getChanged(),false);
			}
			break;
		}
	}
	
	private boolean shouldJoinPartBeShown(IUser usr) {
		FavHub fav = getInput();
		return 		!logInRecent()   
				&& 	ConnectionState.LOGGEDIN.equals(hub.getState()) 
				&&	(	fav.isShowJoins() 
					|| (fav.isShowFavJoins() && usr.isFavUser()) 
					|| (fav.isShowRecentChatterJoins() && contains(usr)))
				;
	}

	private boolean logInRecent() {
		long lastlogin = hub.getLastLogin();
		return lastlogin == 0 ||  System.currentTimeMillis() - lastlogin < 10000 ;
	}

	@Override
	public void storedPM(IUser usr,String message, boolean me) {
		statusMessage(String.format(Lang.StoredPMUser, usr.getNick(),message),0);//  "stored PM for "+usr.getNick()+": "+message,0); 
	}


	/*
	 * (non-Javadoc)
	 * @see UC.protocols.hub.IMCReceivedListener#mcReceived(java.lang.String)
	 */
	public void mcReceived(String message) {
		appendText(message, null ,MessageType.CHAT);
	}



	

	/* (non-Javadoc)
	 * @see UC.listener.IMCReceived#mcReceived(UC.datastructures.User, java.lang.String)
	 */
	public void mcReceived(IUser sender, String message,boolean me) {
		String mes;
		if (me) { 
			mes = "*"+sender.getNick()+" "+message+"*";
		} else {
			mes = "<"+sender.getNick()+"> "+message;
		}
		appendText(mes,sender,MessageType.CHAT);
		
	}
	
	/**
	 * 
	 * @param text
	 * @param usr - who sent the message... may be null
	 */
	@Override
	public void appendText(final String text,final IUser usr,final long date,MessageType type) {
		super.appendText(text,usr,date,type);
		SUIJob job = new SUIJob(hubText) {
			public void run() {
				//notify because of nick found
				if ( usr != null 
						&& !hub.getSelf().equals(usr)
						&& !usr.isBot()
						&& text.contains(hub.getSelf().getNick())) { 
					
					if (!isActiveEditor()) {
						nickWasCalled = true;
					}
					if (GUIPI.getBoolean(GUIPI.addSoundOnNickinMC)) {
						AePlayWave.playWav(IAudioKeys.BLIP);
					}
					if (GUIPI.getBoolean(GUIPI.showToasterMessagesNickinMC)) {
						ToasterUtil.showMessage(text,GUIPI.getInt(GUIPI.toasterTime));
					}
				}
				if ( !isActiveEditor() && !logInRecent() 
						&& !messagesWaiting && (usr == null || !usr.isBot()) ) {
					
					messagesWaiting = true;
					setTitleImage();
				}
			}
		};
		job.scheduleOrRun();
	}
	
	protected void setTitleImage() {
		ConnectionState state = hub.getState();
		if (messagesWaiting && state != ConnectionState.DESTROYED) {
			if (nickWasCalled) {
				setTitleImage(NICK_CALLED);
			} else {
				setTitleImage(hub.getState() == ConnectionState.LOGGEDIN ? 
					newMessage: newMessageOffline);
			}
		} else {
			switch (state){
			case CONNECTED:
				setTitleImage(YELLOW_LED);
				break;
			case LOGGEDIN:
				if (hub.isFingerPrintUsed()) {
					setTitleImage(GREENLKEYP_LED);
				} else if (hub.isEncrypted()) {
					setTitleImage(GREEN_ENC_LED);
				} else {
					setTitleImage(GREEN_LED);
				}
				break;
			case CONNECTING:
				setTitleImage(RED_LED);
				break;
			case CLOSED:
			case DESTROYED:
				setTitleImage(hub.checkReconnect()?RED_LED:GREY_LED);
				break;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see UC.listener.IPMReceivedListener#pmReceived(UC.datastructures.User, UC.datastructures.User, java.lang.String)
	 */
	public void pmReceived(final PrivateMessage pm) {
		PMEditor.addPM(pm);
	}
	

	/* (non-Javadoc)
	 * @see UC.listener.IHubnameChangedListener#hubnameChanged(java.lang.String)
	 */
	public void hubnameChanged(final String hubname, final String topic) {
		new SUIJob() {
			public void run() {
				setPartName(getShortText());
				setTitleToolTip(getFullHubText());
				fireTopicChangedListeners();
			}
		}.schedule();
	}
	
	private String getFullHubText() {
		String text = hub.getHubname();
		if (!GH.isEmpty(hub.getTopic())) {
			text += " - " + hub.getTopic();
		}
		FavHub fh = hub.getFavHub();
		return (text+( " ("+fh.getSimpleHubaddy()+")"  )).trim();
	}
	
	private String getShortText() {	
		String modified = getFullHubText();
		if (modified.length() > MAXTABLENGTH ) {
			modified = modified.substring(0, MAXTABLENGTH-3)+"...";
		}
		return modified;
	}
	
	

	/**
	 * will set the Light indicating if the hub is online offline or in login process.. 
	 * 
	 */
	public void statusChanged(final ConnectionState newStatus, ConnectionProtocol cp) {
		new SUIJob() {
			public void run() {
				setTitleImage();
				switch (newStatus) {
				case CONNECTING:
					logger.debug("statchanged connecting "+hub.getFavHub().getSimpleHubaddy());
					statusMessage(String.format(Lang.ConnectingTo,hub.getFavHub().getSimpleHubaddy()),0);
					break;
				case CONNECTED:
					statusMessage(LanguageKeys.Connected,0);
					break;
				case CLOSED:
					statusMessage(LanguageKeys.Disconnected,0);
					break;
				case DESTROYED:
					new SUIJob(hubText) {
						int count = 6;
						public void run() {
							if (count > 0) {
								statusMessage(String.format("Closing in %d s",count*5),0);
								count--;
								schedule(5000);
							} else {
								HubEditor.this.getSite().getPage().closeEditor(HubEditor.this, false);
							}
							
						}
					}.schedule(1000);
					
					break;
				}
			}
			
		}.schedule();
	}
	
	
	public void clear() {
		hubText.setText("");
	}
	

	/*
	 * (non-Javadoc)
	 * @see UC.protocols.hub.IFeedListener#feedReceived(UC.protocols.hub.IFeedListener.FeedType, java.lang.String)
	 */
	public void feedReceived(final FeedType ft, final String message) {
		logger.debug("received message: "+ft+" "+message);
		new SUIJob(feedLabel) { 
			public void run() {
				feedLabelViewer.addFeedMessage(ft, message);
			}
		}.schedule();
	}

	

	
	public void redirectReceived(FavHub target) {
		statusMessage(String.format(Lang.RedirectReceived,target.getSimpleHubaddy()),0);
	}


	public IHub getHub() {
		return hub;
	}
	
	
	private FavHub getInput() {
		return ((HubEditorInput)getEditorInput()).getFavHub();
	}
	
	
	public void dispose() {
		
		hub.unregisterHubListener(this);
		hub.unregisterUserChangedListener(this);
		//if this is the last Hubeditor on this hub.. 
		if (!anotherEditorIsOpenOnHub() && getHub().getState() != ConnectionState.DESTROYED) {
			hub.close();//disconnect and no reconnect;
		}
		if (transfersListener != null) {
			ISelectionService sel= getSite().getWorkbenchWindow().getSelectionService();
			sel.removePostSelectionListener(TransfersView.ID,transfersListener);
			sel.removePostSelectionListener(DownloadsView.VIEW_ID, transfersListener);
		}
		
		super.dispose();

	}
	
	private boolean anotherEditorIsOpenOnHub() {
		IWorkbenchPage page = getSite().getPage();
		
		IEditorReference[] refs=page.findEditors(getEditorInput(), HubEditor.ID, IWorkbenchPage.MATCH_INPUT|IWorkbenchPage.MATCH_ID);
		
		for (IEditorReference ref: refs) {
			if (ref.getEditor(false) != this ) {
				return true;
			}
		}
		
		return false;
	}
		
	public String getTopic() {
		return getFullHubText();
	}


	public static class UserContentProvider implements  IStructuredContentProvider  {

		public Object[] getElements(Object inputElement) {
			return ((Hub)inputElement).getUsers().values().toArray();
		}
		
		public void dispose() {}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	}
}
