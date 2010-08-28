package eu.jucy.gui.search;

import helpers.GH;
import helpers.IObservable;
import helpers.SizeEnum;
import helpers.StatusObject;
import helpers.Observable.IObserver;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.ISearchableEditor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.DownloadableColumn.ExactSize;
import eu.jucy.gui.DownloadableColumn.FileColumn;
import eu.jucy.gui.DownloadableColumn.Path;
import eu.jucy.gui.DownloadableColumn.SlotsSearchColumn;
import eu.jucy.gui.DownloadableColumn.TTHRoot;
import eu.jucy.gui.DownloadableColumn.Type;
import eu.jucy.gui.DownloadableColumn.UserWrapper;
import eu.jucy.gui.UserColumns.Connection;
import eu.jucy.gui.UserColumns.HubName;
import eu.jucy.gui.UserColumns.IPColumn;
import eu.jucy.gui.itemhandler.DownloadableHandlers.DownloadHandler;



import eu.jucy.gui.Application;
import uc.FavHub;
import uc.IHub;
import uc.crypto.HashValue;
import uc.files.IDownloadable;
import uc.files.MultiUserAbstractDownloadable;
import uc.files.search.ComparisonEnum;
import uc.files.search.FileSearch;
import uc.files.search.ISearchResult;
import uc.files.search.SearchType;
import uc.protocols.hub.Hub;
import uihelpers.ComboBoxViewer;
import uihelpers.DelayedTreeUpdater;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;

/**
 * An Editor providing the possibility to the user to search and 
 * view the corresponding search results
 * 
 * @author Quicksilver
 *
 */
public class SearchEditor extends UCEditor implements IObserver<StatusObject> , ISearchableEditor {

	private static final Logger logger = LoggerFactory.make();
	
	public static final String searchMenuID = "eu.jucy.gui.search";
	
	
	
	
	static final List<SearchInfo> PAST_SEARCHES = new ArrayList<SearchInfo>();

	public static final String ID = "eu.jucy.Search";
		
	private TreeViewer treeViewer;
	private volatile FileSearch current;
	
	private Image showLeftIcon;
	
	private Tree searchResultTable;
	
	private Table hubsToSearch;
	private ComboBoxViewer<SearchType> fileTypeComboViewer;

	private ComboBoxViewer<SizeEnum> unitComboViewer;
	
	private Text sizeText;

	private ComboBoxViewer<ComparisonEnum> minmaxsizeComboViewer;
	private Text searchForText;
	
	private TableViewerAdministrator<IDownloadable> tva;
	private DelayedTreeUpdater<IDownloadable> update;
	


	private Button onlyWhereIAmOpButton;

	private Button onlyUsersWithFreeSlotsButton;
	
	private Button invertButton;
	
	private Label itemCountLabel;
	private Label fileCountLabel;
	
	private boolean useCustomCancelImage = false;
	private Image image = null;
	private ToolItem item;
	
	private boolean proposalClosed = true;

	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite parent) {
		final GridLayout gridLayout_1 = new GridLayout();
		parent.setLayout(gridLayout_1);

		final SashForm sashForm = new SashForm(parent, SWT.NONE);

		final Composite sideBarComposite = new Composite(sashForm, SWT.BORDER);
		final GridLayout gridLayout_2 = new GridLayout();
		gridLayout_2.numColumns = 1;
		sideBarComposite.setLayout(gridLayout_2);


		Group comp = new Group(sideBarComposite,SWT.NONE);
		comp.setText(Lang.SearchFor);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		final GridLayout gridLayout_search = new GridLayout();
		gridLayout_search.numColumns = 2;
		comp.setLayout(gridLayout_search);

		
		searchForText = new Text(comp, SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH | SWT.BORDER);
		searchForText.setMessage(String.format("%-25s", Lang.EnterSearch));
		useCustomCancelImage = (searchForText.getStyle() & SWT.ICON_CANCEL) == 0; //if cancel style does not work on text..
		if (useCustomCancelImage ) { 
			image = AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.STOPICON).createImage();
			ToolBar toolBar = new ToolBar (comp, SWT.FLAT);
			item = new ToolItem (toolBar, SWT.PUSH);
			item.setImage(image);
			item.setEnabled(false);
			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					cancel();
				}
			});
			toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		} 

		searchForText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				if ((e.keyCode == SWT.KEYPAD_CR ||e.keyCode  == SWT.CR) && proposalClosed) {
					String search = searchForText.getText().trim();
					e.doit = false;
					if (search.length() > 2) {
						startSearch( search );
					}
				}
			}
		});
		searchForText.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail == SWT.ICON_CANCEL) {
					cancel();
				} 
			}
		});
		SearchInfoProvider sip = new SearchInfoProvider();
		ContentProposalAdapter cpa = new ContentProposalAdapter(
				searchForText,sip,sip, 
				KeyStroke.getInstance(KeyStroke.NO_KEY,SWT.ARROW_DOWN ),null); 
		cpa.setPopupSize(new Point(500, 400));
		
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
		

	
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, useCustomCancelImage? 1 : 2 , 1);
		searchForText.setLayoutData(gd);
	
		
		final Label distanceholder = new Label(sideBarComposite, SWT.NONE);
		distanceholder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		
		Group sizeComp = new Group(sideBarComposite,SWT.NONE);
		sizeComp.setText(Lang.Size);
		sizeComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		final GridLayout gridLayout_size = new GridLayout();
		gridLayout_size.numColumns = 3;
		
		sizeComp.setLayout(gridLayout_size);
		

		Combo minmaxsizeCombo = new Combo(sizeComp, SWT.READ_ONLY);
		minmaxsizeCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		minmaxsizeComboViewer = new ComboBoxViewer<ComparisonEnum>(minmaxsizeCombo,ComparisonEnum.values());
		minmaxsizeComboViewer.select(ComparisonEnum.ATLEAST);


		sizeText = new Text(sizeComp, SWT.BORDER);
		sizeText.setTextLimit(15);
		
		sizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


		
		Combo unitCombo = new Combo(sizeComp, SWT.READ_ONLY);
		unitComboViewer = new ComboBoxViewer<SizeEnum>(unitCombo,SizeEnum.values());
		unitComboViewer.select(SizeEnum.B);


		unitCombo.setVisibleItemCount(SizeEnum.values().length);

		
		unitCombo.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		

		final Label distanceholder2 = new Label(sideBarComposite, SWT.NONE);
		distanceholder2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		
		Group fileType = new Group(sideBarComposite,SWT.NONE);
		fileType.setText(Lang.FileType);
		final GridLayout gridLayout_fileType= new GridLayout();
		gridLayout_fileType.numColumns = 1;
		fileType.setLayout(gridLayout_fileType);
		fileType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		//create combo for choosing the filetype of the search and fill it with the matching Enums
		Combo fileTypeCombo = new Combo(fileType, SWT.READ_ONLY);  
		fileTypeCombo.setVisibleItemCount(SearchType.values().length);
		fileTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		fileTypeComboViewer = new ComboBoxViewer<SearchType>(fileTypeCombo,SearchType.getNMDCSearchTypes());
		fileTypeComboViewer.select(SearchType.ANY);
		

		final Label distanceholder3 = new Label(sideBarComposite, SWT.NONE);
		distanceholder3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));


		Group searchOptions = new Group(sideBarComposite,SWT.NONE);
		final GridLayout gridLayout_options = new GridLayout();
		gridLayout_options.numColumns = 1;
		searchOptions.setLayout(gridLayout_options);
		searchOptions.setText(Lang.SearchOptions);
		searchOptions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		onlyUsersWithFreeSlotsButton = new Button(searchOptions, SWT.CHECK);
		onlyUsersWithFreeSlotsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		onlyUsersWithFreeSlotsButton.setText(Lang.OnlyUsersWithFreeSlots);
		

		onlyWhereIAmOpButton = new Button(searchOptions, SWT.CHECK);
		onlyWhereIAmOpButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		onlyWhereIAmOpButton.setText(Lang.OnlyWhereIAmOp);
		
		
		invertButton = new Button(searchOptions, SWT.PUSH);
		invertButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		invertButton.setText(Lang.InvertSelection);
		invertButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (TableItem ti:hubsToSearch.getItems()) {
					ti.setChecked(!ti.getChecked());
				}
			}
		});

		Composite comptable= new Composite(sideBarComposite,SWT.NONE);
		comptable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comptable.setLayout(new FillLayout());
		
		
		hubsToSearch = new Table(comptable, SWT.BORDER | SWT.CHECK);
		TableColumn tc = new TableColumn(hubsToSearch,SWT.NONE);

		Map<FavHub,Hub> hubs = ApplicationWorkbenchWindowAdvisor.get().getHubs();
		List<FavHub> fHubs = new ArrayList<FavHub>(hubs.keySet());
		Collections.sort(fHubs);
		for (FavHub e: fHubs ) {
			IHub hub = hubs.get(e);
			if (hub != null && !hub.getFavHub().isChatOnly()) {
				TableItem ti = new TableItem(hubsToSearch,SWT.LEAD);
				ti.setData(hub);
				
				String name = hub.getName();
				if (GH.isNullOrEmpty(name)) {
					name = hub.getFavHub().getSimpleHubaddy();
				}
				ti.setText(name);
				
				ti.setChecked(true);
				
			}
		}
		tc.pack();
		hubsToSearch.pack();
		comptable.pack();
		


		treeViewer = new TreeViewer(sashForm, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		searchResultTable = treeViewer.getTree();
		searchResultTable.setHeaderVisible(true);
		
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				executeCommand(DownloadHandler.ID);
			}
		});
		
		
		tva = new TableViewerAdministrator<IDownloadable>(treeViewer,
				Arrays.asList(new FileColumn(),UserWrapper.createUserColumn(), new Type(),
						new ExactSize(),new SlotsSearchColumn(),new Path(),
						new UserWrapper(new Connection()),new UserWrapper(new HubName()),
						new UserWrapper(new IPColumn()),new TTHRoot())
				,GUIPI.searchEditorTable,TableViewerAdministrator.NoSorting);
		
		
		tva.apply();
		
		onlyUsersWithFreeSlotsButton.addSelectionListener(new SelectionAdapter() {
			boolean onlyUsersWithFreeSlots = false;
			ViewerFilter filter = new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement,Object element) {
					if (onlyUsersWithFreeSlots) {
						if (element instanceof ISearchResult) {
							ISearchResult sr = (ISearchResult)element;
							return sr.getAvailabelSlots() > 0;
						}
					} 
					return true;
				}
			};
			{
				treeViewer.addFilter(filter);
			}
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				onlyUsersWithFreeSlots = onlyUsersWithFreeSlotsButton.getSelection();
				treeViewer.refresh();
			}
		});
		
		
		SearchTreeProvider stp = new SearchTreeProvider();
		treeViewer.setContentProvider(stp);
		
		
	
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sashForm.setWeights(new int[] {130, 430 });

		final Composite composite = new Composite(parent, SWT.BORDER);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 1;
		gridLayout.marginHeight = 1;
		gridLayout.horizontalSpacing = 2;
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);

//		final Button showSearchOptions = new Button(composite, SWT.CHECK);
//		showSearchOptions.setSelection(true);
//		showSearchOptions.addSelectionListener(new SelectionAdapter() {
//			private int[] weights = new int[] {130, 430 };
//			private boolean enable = true;
//			public void widgetSelected(final SelectionEvent e) {
//				if (enable != showSearchOptions.getSelection()) {
//					enable = !enable;
//					if (showSearchOptions.getSelection()) {
//						sashForm.setWeights(weights);
//					} else {
//						weights = sashForm.getWeights();
//						sashForm.setWeights(new int[]{0,500});
//					}
//				}
//			}
//		});
//		
//		showSearchOptions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		showSearchOptions.setText(Lang.ShowSidebar);
		
		showLeftIcon = 	 AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.SHOW_LEFT_SIDEVIEW).createImage(); 
		ToolBar toolBar = new ToolBar (composite, SWT.HORIZONTAL | SWT.SHADOW_OUT | SWT.FLAT);
		final ToolItem showOptions = new ToolItem (toolBar, SWT.CHECK);
		showOptions.setImage(showLeftIcon);
		showOptions.addSelectionListener(new SelectionAdapter() {
			private int[] weights = null;
			public void widgetSelected(final SelectionEvent e) {
				if (!showOptions.getSelection()) {
					sashForm.setWeights(weights);
				} else {
					weights = sashForm.getWeights();
					sashForm.setWeights(new int[]{0,500});
				}
			}
		});
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		
		

		itemCountLabel = new Label(composite, SWT.BORDER);
		final GridData gridData = new GridData(SWT.LEFT, SWT.FILL, false, false);
		gridData.widthHint = 120;
		itemCountLabel.setLayoutData(gridData);

		fileCountLabel = new Label(composite, SWT.BORDER);
		final GridData gridData_1 = new GridData(SWT.LEFT, SWT.FILL, false, false);
		gridData_1.widthHint = 100;
		fileCountLabel.setLayoutData(gridData_1);
		//
		
		update = new DelayedTreeUpdater<IDownloadable>(treeViewer) {
			@Override
			protected void updateDone() {
				updateLabels();
			}
		};
		
	//	makeActions();
		getSite().setSelectionProvider(treeViewer);
		createContextPopup(searchMenuID, treeViewer);

		if (getInitialSearch() != null) {
			//set TTH as type..
			fileTypeComboViewer.select(SearchType.TTH); 
			String s = getInitialSearch().toString();
			searchForText.setText(s);
			startSearch(s);
		} else if (alternateInitialSearch() != null) {
			fileTypeComboViewer.select(SearchType.ANY); 
			String s = alternateInitialSearch();
			searchForText.setText(s);
			startSearch(s);
		}
			

		hubsToSearch.pack();

		
		sideBarComposite.pack();
		sashForm.pack();
		
		setControlsForFontAndColour(treeViewer.getControl(),searchForText,
				sizeText,unitCombo,minmaxsizeCombo,hubsToSearch);
	}
	
	
	
	
	private String lastSearch;
	private int searchIndex = -1;
	private List<ISearchResult> found;

	public void next() {
		if (found != null && !found.isEmpty()) {
			searchIndex++;
			ISearchResult current = found.get(searchIndex % found.size());
			treeViewer.setSelection(new StructuredSelection(current), true);
		}
	}


	public void search(String searchstring) {
		if (!searchstring.equals(lastSearch)) {
			lastSearch = searchstring;
			searchIndex = -1;
			if (current != null) {
				found = current.searchSubset(lastSearch);
			}
		}
		next();
		
	}







	private HashValue getInitialSearch() {
		return ((SearchEditorInput)getEditorInput()).getInitialsearch();
	}
	
	private String alternateInitialSearch() {
		return ((SearchEditorInput)getEditorInput()).getAlternate();
	}

	

	
	/**
	 * checks if the user has entered a max or minsize ..
	 * returns -1 if nothing is set in sizeText or set value is not an integer 
	 * 
	 * @return the max or minsize set
	 */
	private long getSearchSize(){
		long size = -1;
		//check if a max size is set
		String sizestring = sizeText.getText().trim();
		if (sizestring.length() > 0) {
			try {
				size = new Long(sizestring);
				if( size < 0 ) {
					size = -1;
				} else {
					size = unitComboViewer.getSelected().getInBytes(size); 
				}
			} catch(NumberFormatException nfe){
				size = -1;  //no legal number...
			}
		}
		return size;
	}

	
	/**
	 * reads in the values from the widgets and then sends a search to the hub
	 * @param searchString  a searchstring the user provided
	 */
	private void startSearch(String searchstring) {
		
		//unregister last search
		cancel();
		//create new search and register.. with the DCClient  then register this viewer with the search
		current = new FileSearch(searchstring,fileTypeComboViewer.getSelected()
				,minmaxsizeComboViewer.getSelected(),getSearchSize());
		
		ApplicationWorkbenchWindowAdvisor.get().register(current);
		current.addObserver(this);
		
		//add the search as input to the input provider..
		treeViewer.setInput(current);
		
		
		//search in the marked hubs
		Set<IHub> usedHubs = new HashSet<IHub>();
		boolean onlyHubsWhereOp = onlyWhereIAmOpButton.getSelection();
		for (TableItem ti : hubsToSearch.getItems()) {
			Object o = ti.getData();
			if (ti.getChecked() && o instanceof IHub ) {
				logger.debug("sending seach to hub: "+ ((IHub)o).getName());
				IHub hub =(IHub)o;
				if ( (!onlyHubsWhereOp || hub.getSelf().isOp()) && !hub.getFavHub().isChatOnly() ) {
					usedHubs.add(hub);
				}
			}
		}	
		ApplicationWorkbenchWindowAdvisor.get().search(current, usedHubs);
		setPartName(Lang.Search + " - "+searchstring);
		fireTopicChangedListeners();
		updateCancelImageIfNeeded();
	}
	
	private void cancel() {
		if (current != null) { // cancels current search and clears view
			PAST_SEARCHES.add(0,
					new SearchInfo(
							current.getSearchString(), 
							current.getNrOfResults(),
							current.getNrOfFiles(),
							current.getSearchType()));
			
			ApplicationWorkbenchWindowAdvisor.get().unregister(current);
			current.deleteObserver(this);
			current = null;
			update.clear();
			if (treeViewer.getContentProvider() != null) { //might not be set..
				treeViewer.setInput(new Object());
			}
			updateCancelImageIfNeeded();
			updateLabels();
		}
	}
	
	private void updateCancelImageIfNeeded() {
		if (useCustomCancelImage && !item.isDisposed()) {
			if (current == null) {
				item.setEnabled(false);
			} else {
				item.setEnabled(true);
				item.setImage(image);
			}
		}
	}
	
	private void updateLabels() {
		if (!itemCountLabel.isDisposed() && !fileCountLabel.isDisposed()) {
			if (current == null) {
				itemCountLabel.setText("");
				fileCountLabel.setText("");
			} else {
				itemCountLabel.setText( String.format(Lang.ReceivedResults, current.getNrOfResults()));
				fileCountLabel.setText(	String.format(Lang.DifferentFiles , current.getNrOfFiles()));
			}
		}
	}
	
	
	

	public void update(IObservable<StatusObject> o, StatusObject arg) {
		update.put(arg.getType(), (ISearchResult)arg.getValue(), arg.getDetailObject());
	}




	@Override
	public void setFocus() {
		searchForText.setFocus();
	}
	
	/**
	 * on disposing unregister any current search
	 * 
	 */
	public void dispose() {
		cancel();
		super.dispose();

		if (image != null) {
			image.dispose();
		}
		if (showLeftIcon != null) {
			showLeftIcon.dispose();
		}
	}
	
	
	
	
	@Override
	public String getTopic() {
		String text = getPartName();
		if (current != null)  {
			text += " - "+ current.getSearchString();
		}
		
		return text;
	}


	public static class SearchTreeProvider implements ITreeContentProvider {
		
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof FileSearch){
				FileSearch sr = (FileSearch)inputElement;
				return sr.getResults().toArray();
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof MultiUserAbstractDownloadable) {
				return ((MultiUserAbstractDownloadable)parentElement).getFiles().toArray();
			}
			
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return ((IDownloadable)element).nrOfUsers() > 1;
		}

		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	}
	
	


}
