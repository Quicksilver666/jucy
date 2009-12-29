package eu.jucy.gui.texteditor.hub;





import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


import helpers.GH;
import helpers.SizeEnum;

import logger.LoggerFactory;


import org.apache.log4j.Logger;




import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.swt.custom.StyledText;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;


import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;


import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import eu.jucy.gui.Application;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UserColumns.Connection;
import eu.jucy.gui.UserColumns.Description;
import eu.jucy.gui.UserColumns.Email;
import eu.jucy.gui.UserColumns.Nick;
import eu.jucy.gui.UserColumns.Shared;
import eu.jucy.gui.UserColumns.Tag;
import eu.jucy.gui.itemhandler.CommandDoubleClickListener;
import eu.jucy.gui.itemhandler.UCContributionItem.HubContributionItem;
import eu.jucy.gui.itemhandler.UserHandlers.GetFilelistHandler;
import eu.jucy.gui.sounds.AePlayWave;
import eu.jucy.gui.sounds.IAudioKeys;
import eu.jucy.gui.texteditor.LabelViewer;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.UCTextEditor;
import eu.jucy.gui.texteditor.SendingWriteline.HubSendingWriteline;
import eu.jucy.gui.texteditor.pmeditor.PMEditor;
import eu.jucy.language.LanguageKeys;





import uc.FavHub;
import uc.IUser;
import uc.protocols.ConnectionProtocol;
import uc.protocols.ConnectionState;
import uc.protocols.hub.Hub;
import uc.protocols.hub.IHubListener;
import uc.protocols.hub.PrivateMessage;


import uihelpers.DelayedUpdate;
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
public class HubEditor extends UCTextEditor implements IHubListener {
	
	private static final int MAXTABLENGTH = 20; 

	private static final Logger logger = LoggerFactory.make();
	
	public static final String ID = "eu.jucy.hub";
	
	
	public static final Image 	green ,
								yellow,
								red	,
								nickCalled;     
		
	static {
		//load the images that show if the hub is connected or not.. 
		green	= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.LEDLIGHT_GREEN).createImage();
		yellow	= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.LEDLIGHT_YELLOW).createImage();
		red		= AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.LEDLIGHT_RED).createImage();     
		nickCalled = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.NICK_CALLED).createImage();    
		
	}


	
	private Hub hub;
	
	
	private boolean messagesWaiting = false;
	private boolean nickWasCalled = false;

	
	private TableViewerAdministrator<IUser> tva;
	
	private StyledTextViewer textViewer;
	
	private LabelViewer labelViewer;
	
	private DelayedUpdate<IUser> updater;
	



	

	
	

	
	private StyledText hubText;
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
		hubText = new StyledText(sashForm, SWT.V_SCROLL | SWT.READ_ONLY  | SWT.WRAP);
		
		hubText.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
			}

			/**
			 * when the hubText is resized.. that means the sashForm probably changed
			 * therefore store size if it Users are visible
			 */
			public void controlResized(ControlEvent e) {
				if (tableViewer.getControl().isVisible()) {
					getInput().setWeights(sashForm.getWeights());
				}
			}
		});
		
		sashForm.setLayout(new FillLayout());
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		tableViewer= new TableViewer(sashForm, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER );
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		tableViewer.setUseHashlookup(true);
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
	
		writeline.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		writeline.setText("");
	
		

		filterText = new Text(composite_1, SWT.BORDER);
		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setFilter(filterText.getText());
			}
		});
		final GridData gridData_2 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gridData_2.widthHint = 80;
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
		gridLayout_1.marginWidth = 1;
		gridLayout_1.marginHeight = 1;
		gridLayout_1.horizontalSpacing = 2;
		gridLayout_1.numColumns = 4;
		
		composite_2.setLayout(gridLayout_1);
		final GridData gridData_3 = new GridData(SWT.FILL, SWT.FILL, true, false);
		composite_2.setLayoutData(gridData_3);
		
		feedLabel = new CLabel(composite_2, SWT.BORDER);
		feedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		

		userLabel = new CLabel(composite_2, SWT.BORDER);
		userLabel.setLayoutData(new GridData(80, SWT.DEFAULT));

		sharesizeLabel = new CLabel(composite_2, SWT.BORDER);
		
		
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_1.widthHint = 60;
		sharesizeLabel.setLayoutData(gridData_1);

		final Button button = new Button(composite_2, SWT.TOGGLE);
		button.addSelectionListener(new SelectionAdapter() {
			int[] weights = sashForm.getWeights();
			
			public void widgetSelected(final SelectionEvent e) {
				if (tableViewer.getControl().isVisible()){
					tableViewer.getControl().setVisible(false);   //set usertable invisible
					weights = sashForm.getWeights();                // store the weigths of the sashform
					sashForm.setWeights(new int[] {300, 0 });     // make only HubTextpart visible.. 
				} else {	
					tableViewer.getControl().setVisible(true); //set visible
					sashForm.setWeights(weights);                //and old weights
					weights = null;
				}
			}
		});
		final GridData gridData = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gridData.heightHint = 10;
		gridData.widthHint = 10;
		button.setLayoutData(gridData);
		//

		setText(hubText);
		
		this.hub = ApplicationWorkbenchWindowAdvisor.get().getHub(getInput(),false);   //here retrieve the hub  
		
		labelViewer = new LabelViewer(feedLabel,hub);
		textViewer = new StyledTextViewer(hubText,hub,false,null);
		
		sendingWriteline = new HubSendingWriteline(writeline,getHub().getUserPrefix(),getHub(),this);
		
		updater = new DelayedUpdate<IUser>(tableViewer) {
			protected void updateDone() {
				userLabel.setText(getHub().getUsers().size()+" "+Lang.Users );
				sharesizeLabel.setText(SizeEnum.getReadableSize(getHub().getTotalshare()));
			}
		};
		
		tableViewer.setInput(getHub());
		getHub().registerHubListener(this);//register the listeners..
		
	
		
		makeActions(); //create menu and actions
		logger.debug("created HubEditor partControl");
		
	//	setTitleImage();
		
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
	}
	




	
	
	@Override
	public void partActivated() {
		super.partActivated();
		messagesWaiting = false;
		nickWasCalled = false;
		setTitleImage();
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
			if (ConnectionState.LOGGEDIN.equals(hub.getState())) {
				if (getInput().isShowJoins() || (getInput().isShowFavJoins() && uce.getChanged().isFavUser())) {
					statusMessage(String.format(Lang.UserJoins,uce.getChanged().getNick()), 0);
				} 
			}
			break;
		case CHANGED:
			updater.change(uce.getChanged());
			break;
		case DISCONNECTED:
		case QUIT:
			updater.remove(uce.getChanged());
			if (ConnectionState.LOGGEDIN.equals(hub.getState())) {
				if (getInput().isShowJoins() || (getInput().isShowFavJoins() && uce.getChanged().isFavUser())) {
					statusMessage(String.format(Lang.UserParts,uce.getChanged().getNick()), 0);
				} 
			}
			break;
		}
	}


	
	/* (non-Javadoc)
	 * @see UC.listener.IMCReceived#mcReceived(java.lang.String)
	 */
	public void statusMessage(final String message,  int severity) {
		appendText( "*** " +message,null);			
		changeLabel(message,severity);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see UC.protocols.hub.IMCReceivedListener#mcReceived(java.lang.String)
	 */
	public void mcReceived(String message) {
		appendText(message, null );
	}


	private void changeLabel(final String message, final int severity) {
		new SUIJob(feedLabel) {
			public void run() {
				FeedType ft;
				switch(severity) {
				case 1: 
					ft = FeedType.WARN;
				break;
				case 2: 
					ft = FeedType.ERROR;
				break;
				default: 
					ft = FeedType.NONE;
				}
				labelViewer.addFeedMessage(ft,message);
		
			}
			
		}.schedule();
	}
	

	/* (non-Javadoc)
	 * @see UC.listener.IMCReceived#mcReceived(UC.datastructures.User, java.lang.String)
	 */
	public void mcReceived(final IUser sender, final String message) {
		appendText("<"+sender.getNick()+"> "+message,sender);
	}
	
	/**
	 * 
	 * @param text
	 * @param usr - who sent the message... may be null
	 */
	public void appendText(final String text,final IUser usr){
		SUIJob job = new SUIJob(hubText) {
			public void run() {
				textViewer.addMessage(text,usr,new Date()); 
				boolean activeEditor = HubEditor.this.getSite().getPage().getActiveEditor() == HubEditor.this;

				//notify because of nick found
				if (!hub.getSelf().equals(usr) && text.contains(hub.getSelf().getNick())
						&& usr != null && ! (usr.isOp() && usr.getShared() == 0)) { //here check if it is not is a bot..
					
					if (!activeEditor) {
						nickWasCalled = true;
					}
					if (GUIPI.getBoolean(GUIPI.addSoundOnNickinMC)) {
						AePlayWave.playWav(IAudioKeys.BLIP);
					}
					if (GUIPI.getBoolean(GUIPI.showToasterMessagesNickinMC)) {
						ToasterUtil.showMessage(text,GUIPI.getInt(GUIPI.toasterTime));
					}
				}
				if (!activeEditor) {
					messagesWaiting = true;
					setTitleImage();
				}
			}
		};
		job.scheduleOrRun();
	}
	
	private void setTitleImage() {
		if (messagesWaiting) {
			if (nickWasCalled) {
				setTitleImage(nickCalled);
			} else {
				setTitleImage(getHub().getState() == ConnectionState.LOGGEDIN ? 
					newMessage: newMessageOffline);
			}
		} else {
			switch (getHub().getState()){
			case CONNECTED:
				setTitleImage(yellow);
				break;
			case LOGGEDIN:
				setTitleImage(green);
				break;
			case CONNECTING:
			case CLOSED:
				setTitleImage(red);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see UC.listener.IPMReceivedListener#pmReceived(UC.datastructures.User, UC.datastructures.User, java.lang.String)
	 */
	public void pmReceived(final PrivateMessage pm) {
		new SUIJob() {
			public void run() {
				PMEditor.addPM(pm);
			}
			
		}.schedule();
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
		return (text+(hub.getHubaddy() != null ? " ("+hub.getHubaddy()+")" : "" )).trim();
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
				if (newStatus != ConnectionState.DESTROYED) {
					setTitleImage();
				}
				switch (newStatus) {
				case CONNECTING:
					logger.debug("statchanged connecting "+hub.getHubaddy());
					statusMessage(String.format(LanguageKeys.ConnectingTo,hub.getHubaddy()),0);
					break;
				case CONNECTED:
					statusMessage(LanguageKeys.Connected,0);
					break;
				case CLOSED:
					statusMessage(LanguageKeys.Disconnected,0);
					break;
				case DESTROYED:
					dispose();
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
				labelViewer.addFeedMessage(ft, message);
			}

		}.schedule();
	}

	public Hub getHub() {
		return hub;
	}
	
	
	private FavHub getInput() {
		return ((HubEditorInput)getEditorInput()).getFavHub();
	}
	
	
	public void dispose() {
		
		getHub().unregisterHubListener(this);
		//if this is the last Hubeditor on this hub.. 
		if (!anotherEditorIsOpenOnHub()) {
			getHub().close();//disconnect and no reconnect;
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
