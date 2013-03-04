package eu.jucy.gui.downloadqueue;




import helpers.IObservable;
import helpers.PrefConverter;
import helpers.SizeEnum;
import helpers.StatusObject;
import helpers.Observable.IObserver;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


import logger.LoggerFactory;


import org.apache.log4j.Logger;


import org.eclipse.jface.viewers.ISelectionChangedListener;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;


import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;




import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.ISearchableEditor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQAdded;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQDownloaded;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQErrors;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQExactSize;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQFile;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQPath;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQPriority;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQSize;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQStatus;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQTTHRoot;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQUsers;
import eu.jucy.gui.itemhandler.DownloadQueueHandlers;






import uc.crypto.HashValue;
import uc.files.downloadqueue.DownloadQueue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.DownloadQueueFolder;
import uihelpers.IconManager;
import uihelpers.SUIJob;
import uihelpers.SelectionProviderIntermediate;
import uihelpers.TableViewerAdministrator;


public class DownloadQueueEditor extends UCEditor implements IObserver<StatusObject> , ISearchableEditor  {

	public static final String ID	=	"eu.jucy.DownloadQueue";
	
	private static final Logger logger = LoggerFactory.make();
	

	private static final String OverrideFilePrefix = "TARGET:";
	
	
	private Image showLeftIcon;

	private CLabel totalSizeLabel;
	private CLabel nrOfFilesLabel;
	private CLabel sizeLabel;
	private CLabel nrOfSelectedItemsLabel;
	private Table table;
	private Tree tree;
	private Composite composite;
	
	private TreeViewer treeViewer;
	private TableViewer tableViewer;
	
	private final AtomicBoolean arInProgress = new AtomicBoolean();
	private TableViewerAdministrator<AbstractDownloadQueueEntry> tva;
	
//	private List<Action> actions = new ArrayList<Action>();
//	private Action remove;
//	private IHandlerActivation handRemove;
//	private IHandlerActivation handUp;
//	private IHandlerActivation handDown;

	private SelectionProviderIntermediate spi = new SelectionProviderIntermediate();
	
	
	
	@Override
	public void createPartControl(Composite parent) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 1;
		gridLayout.marginWidth = 2;
		gridLayout.marginHeight = 1;
		gridLayout.horizontalSpacing = 3;
		parent.setLayout(gridLayout);

		composite = new Composite(parent, SWT.NONE);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 487;
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout());
		
		
		final SashForm sashForm = new SashForm(composite, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		
		tree = new Tree(sashForm, SWT.BORDER);

		final TreeColumn newColumnTreeColumn = new TreeColumn(tree, SWT.NONE);
		newColumnTreeColumn.setWidth(200);
	

		table = new Table(sashForm, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		table.setHeaderVisible(true);
		

		sashForm.setWeights(new int[] { 120, 300 });
		
		


		final Composite composite_1 = new Composite(parent, SWT.BORDER);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 5;
		gridLayout_1.verticalSpacing = 1;
		gridLayout_1.marginWidth = 2;
		gridLayout_1.marginHeight = 1;
		gridLayout_1.horizontalSpacing = 3;
		
		composite_1.setLayout(gridLayout_1);
		
		
		showLeftIcon = 	 AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.SHOW_LEFT_SIDEVIEW).createImage(); 
		
		ToolBar toolBar = new ToolBar (composite_1, SWT.HORIZONTAL | SWT.SHADOW_OUT | SWT.FLAT);
		final ToolItem scroll = new ToolItem (toolBar, SWT.CHECK);
		scroll.setImage(showLeftIcon);
		scroll.addSelectionListener(new SelectionAdapter() {
			private int[] sashweights = null;
			public void widgetSelected(final SelectionEvent e) {
				if (!scroll.getSelection()) {
					sashForm.setWeights(sashweights);
					tree.setVisible(true);
				} else {
					sashweights = sashForm.getWeights();
					tree.setVisible(false);
					sashForm.setWeights(new int[]{0,400});
				}
				sashForm.layout(true);
			}
		});
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
//		Label placeholder = new Label(composite_1,SWT.NONE);
//		placeholder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		

//		final Button showSidebarCheckbox = new Button(composite_1, SWT.CHECK);
//		showSidebarCheckbox.addSelectionListener(new SelectionAdapter() {
//			private int[] sashweights = null;
//			public void widgetSelected(final SelectionEvent e) {
//				if (showSidebarCheckbox.getSelection()) {
//					sashForm.setWeights(sashweights);
//					tree.setVisible(true);
//				} else {
//					sashweights = sashForm.getWeights();
//					tree.setVisible(false);
//					sashForm.setWeights(new int[]{0,400});
//				}
//				sashForm.layout(true);
//			}
//		});
//		showSidebarCheckbox.setSelection(true);
//		showSidebarCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		showSidebarCheckbox.setText(Lang.ShowSidebar);

		nrOfSelectedItemsLabel = new CLabel(composite_1, SWT.NONE);
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_1.widthHint = 100;
		nrOfSelectedItemsLabel.setLayoutData(gridData_1);

		sizeLabel = new CLabel(composite_1, SWT.NONE);
		final GridData gridData_2 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_2.widthHint = 100;
		sizeLabel.setLayoutData(gridData_2);

		nrOfFilesLabel = new CLabel(composite_1, SWT.BORDER);
		final GridData gridData_3 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		nrOfFilesLabel.setLayoutData(gridData_3);

		totalSizeLabel = new CLabel(composite_1, SWT.BORDER);
		final GridData gridData_4 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		totalSizeLabel.setLayoutData(gridData_4);
		
		treeViewer = new TreeViewer(tree);	
		DownloadQueueTreeManager dqtp = new DownloadQueueTreeManager();
		treeViewer.setContentProvider(dqtp);
		treeViewer.setLabelProvider(dqtp);
		treeViewer.addFilter(new DownloadQueueFolderFilter());
		treeViewer.setComparator(dqtp);
		treeViewer.setAutoExpandLevel(2);
		
		DQProgressPainter.addToTable(table,3);
		tableViewer = new TableViewer(table);
		DownloadQueueTableProvider dqTablep = new DownloadQueueTableProvider();
		tableViewer.setContentProvider(dqTablep);
		tableViewer.addFilter(new DownloadQueueEntryFilter());
		
		tva = new TableViewerAdministrator<AbstractDownloadQueueEntry>(tableViewer,
				Arrays.asList(new DQFile(),new DQStatus(),new DQSize(),
						new DQDownloaded(),new DQPriority(),new DQUsers(),
						new DQPath(),new DQExactSize(),new DQErrors(),
						new DQAdded(), new DQTTHRoot()),
						GUIPI.downloadQueueTable,0);
		tva.apply();
		
		spi.addViewer(treeViewer);
		spi.addViewer(tableViewer);
		getSite().setSelectionProvider(spi);
		createContextPopup(tableViewer);
		createContextPopup(treeViewer);
		

		
		treeViewer.setInput(getDQ().getRoot());
		tableViewer.setInput(getDQ().getRoot());
		
		updateLabels();
		addListener();
		pack();
		setControlsForFontAndColour(treeViewer.getTree(),tableViewer.getTable());
		
		logger.debug("current column width: "+tree.getColumn(0).getWidth());
		
		
	

		   
		DragSource ds = new DragSource(table, DND.DROP_MOVE);
	    ds.setTransfer(new Transfer[] { TextTransfer.getInstance() });
	    ds.addDragListener(new DragSourceAdapter() {
	    	public void dragSetData(DragSourceEvent event) {
	    		IStructuredSelection isl = (IStructuredSelection)tableViewer.getSelection();
	    		List<String> paths = new ArrayList<String>();
	    		for (Object o: isl.toArray()) {
	    			if (o instanceof AbstractDownloadQueueEntry) {
	    				paths.add(((AbstractDownloadQueueEntry)o).getID().toString());
	    			} 
	    		}
	    		event.data = PrefConverter.asString(paths.toArray(new String[]{}));
	    	}
	    });
	    
	    DragSource ds2 = new DragSource(tree, DND.DROP_MOVE);
	    
	    ds2.setTransfer(new Transfer[] { TextTransfer.getInstance() });
	    ds2.addDragListener(new DragSourceAdapter() {
	    	public void dragSetData(DragSourceEvent event) {
	    		IStructuredSelection isl = (IStructuredSelection)treeViewer.getSelection();
	    		
	    		List<String> paths = new ArrayList<String>();
	    		Object o = isl.getFirstElement();
	    		if (o instanceof DownloadQueueFolder) {
	    			DownloadQueueFolder dqf = (DownloadQueueFolder)o;
	    			paths.add(OverrideFilePrefix+dqf.getShownPath().getParent());
	    			
	    			for (AbstractDownloadQueueEntry adqe: dqf.getAllDQEChildren()) {
	    				paths.add(adqe.getID().toString());
	    			}
	    		}
	    		
	    		event.data = PrefConverter.asString(paths.toArray(new String[]{}));
	    	}
	    });
	    
	    
	    DropTarget dt = new DropTarget(tree, DND.DROP_MOVE);
	    dt.setTransfer(new Transfer[] { TextTransfer.getInstance() });
	    dt.addDropListener (new DropTargetAdapter() {
	    	public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
				if (event.item != null) {
					event.feedback |= DND.FEEDBACK_SELECT;
				}
	    	}
	    	
			@Override
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}
				String[] files = PrefConverter.asArray((String)event.data);
				File overridePrefix = null;
				List<AbstractDownloadQueueEntry> adqeList = new ArrayList<AbstractDownloadQueueEntry>();
				for (String s : files) {
					if (HashValue.isHash(s)) {
						HashValue h = HashValue.createHash(s);
						AbstractDownloadQueueEntry adqe = getDQ().get(h);
						if (adqe != null) {
							adqeList.add(adqe);
						}
					} else if (s.startsWith(OverrideFilePrefix)) {
						overridePrefix = new File(s.substring(OverrideFilePrefix.length()));
					}
				}
				
				
				Object o = ((TreeItem)event.item).getData();
				if (o instanceof DownloadQueueFolder) {
					DownloadQueueFolder dqf = (DownloadQueueFolder)o;
					DownloadQueueHandlers.move(dqf, adqeList, overridePrefix);
				} else {
					event.detail = DND.DROP_NONE;
				}
			}
	    	
	    });
	}
	
	private void updateLabels() {
		totalSizeLabel.setText(	"  "+
				String.format(Lang.TotalSize,SizeEnum.getReadableSize(
						getDQ().getTotalSize())) +
								"  ");
		
		nrOfFilesLabel.setText(	"  " +
				String.format(Lang.TotalFiles,getDQ().getTotalNrOfFiles())
								+"  ");
		
		
		totalSizeLabel.getParent().layout();
	}
	

	
	private void addListener() {
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection =
					(IStructuredSelection) event.getSelection();

				Object selectedFolder = selection.getFirstElement();
				pack();
				logger.debug("current column selection width: "+tree.getColumn(0).getWidth());
				tableViewer.setInput(selectedFolder);
			}
		});
		treeViewer.addTreeListener(new ITreeViewerListener() {

			
			public void treeCollapsed(TreeExpansionEvent event) {
				pack();
				logger.debug("current column collapsed width: "+tree.getColumn(0).getWidth());
			}

			
			public void treeExpanded(TreeExpansionEvent event) {
				pack();
				logger.debug("current column expanded width: "+tree.getColumn(0).getWidth());
			}

		});
		getDQ().addObserver(this);
	}

	private DownloadQueue getDQ() {
		return ((DownloadQueueEditorInput)getEditorInput()).getDQ();
	}
	
//	private void makeActions() {
//		MenuManager tableMenu = new MenuManager();
//		MenuManager treeMenu = new MenuManager();
//		IWorkbenchWindow window = getEditorSite().getWorkbenchWindow();
//		if (!actions.isEmpty()) {
//			throw new IllegalStateException();
//		}
//		
//		actions.add(new SearchForAlternatesAction(window,tableViewer,treeViewer));
//		actions.add(new CopyTTHToClipboardAction(window,tableViewer,treeViewer));
//		actions.add(new CopyMagnetToClipboardAction(window,tableViewer,treeViewer));
//		actions.add(new MoveRenameAction(window,tableViewer,treeViewer));
//		for (Action a: actions) {
//			tableMenu.add(a);
//			if (a instanceof AbstractDownloadQueueActions) {
//				treeMenu.add(a);
//			}
//		}
//		
//		List<? extends Action> prio = SetPriorityAction.getAll(window,tableViewer,treeViewer);
//		MenuManager setPrio = new MenuManager(Lang.SetPriority);
//		MenuManager setPrioTree = new MenuManager(Lang.SetPriority);
//		for (Action a : prio) {
//			setPrio.add(a);
//			setPrioTree.add(a);
//			actions.add(a);
//		}
//		setPrio.add(new Separator());
//		setPrioTree.add(new Separator());
//		
//		IHandlerService hservice = (IHandlerService) getSite().getService(IHandlerService.class);
//		// change priority action
//		Action up = new ChangePriorityAction(window,true,tableViewer,treeViewer);
//		Action down = new ChangePriorityAction(window,false,tableViewer,treeViewer);
//		IHandler hUp = new ActionHandler(up);
//		IHandler hDown = new ActionHandler(down);
//		handUp = hservice.activateHandler(up.getId(), hUp);
//		handDown = hservice.activateHandler(down.getId(), hDown);
//		
//		setPrio.add(up);
//		setPrioTree.add(up);
//		setPrio.add(down);
//		setPrioTree.add(down);
//		actions.add(up);
//		actions.add(down);
//		//cp end
//		
//		tableMenu.add(setPrio);
//		treeMenu.add(setPrioTree);
//		
//		
//		
//		remove = new RemoveAction(window,tableViewer,treeViewer);
//		IHandler hRemove = new ActionHandler(remove);
//		handRemove = hservice.activateHandler(RemoveAction.ID, hRemove);
//		
//		
//	
//		tableMenu.add(remove);
//		treeMenu.add(remove);
//		actions.add(remove);
//		
//		tableMenu.add(new Separator()); 
//		
//		new DQUserActionMenuListener(tableViewer,window,tableMenu);
//		
//		
//    	Menu menu = tableMenu.createContextMenu(tableViewer.getControl());
//    	Menu menu2 = treeMenu.createContextMenu(treeViewer.getControl());
//    	
//    	tableMenu.updateAll(true);
//    	
//    	tableViewer.getControl().setMenu(menu);
//    	treeViewer.getControl().setMenu(menu2);
//    	
//    	logger.debug("DQ created");
//    	
//    	
//	}
	
	private void pack() {
		new SUIJob(treeViewer.getTree()) {
			public void run() {
				Tree tree = treeViewer.getTree();
				TreeColumn col = tree.getColumn(0);
				col.pack();
				if (col.getWidth() < tree.getBounds().width) {
					logger.debug("setting new Column width after pack: "+tree.getBounds().width);
					col.setWidth(tree.getBounds().width);
				}
			}
		}.schedule();
	}
	
	
	public void setFocus() {
		table.setFocus();
	}

	
	
	public void dispose() {
		//getEditorSite().getKeyBindingService().unregisterAction(remove);
//		IHandlerService hservice = (IHandlerService) getSite().getService(IHandlerService.class);
//		hservice.deactivateHandler(handUp);
//		hservice.deactivateHandler(handDown);
//		hservice.deactivateHandler(handRemove);
//		
//		for (Action a:actions) {
//			((IWorkbenchAction)a).dispose();
//		}
		
		if (showLeftIcon != null) {
			showLeftIcon.dispose();
		}
		
		getDQ().deleteObserver(this);
		super.dispose();
	}


	

	
	public String getTopic() {
		return getPartName();
	}

	
	public void update(IObservable<StatusObject> o, final StatusObject status) {
		switch(status.getType()) {
		case ADDED:
		case REMOVED:
			if (arInProgress.compareAndSet(false, true)) {
				new SUIJob(table) {
					public void run() {
						arInProgress.set(false);
						tableViewer.refresh();
						treeViewer.refresh();
						updateLabels();
					}
				}.schedule();
			}
			break;
		case CHANGED:
			new SUIJob(table) {
				public void run() {
					tableViewer.update(status.getValue(), null);
				}
				
			}.schedule();
			break;
		}
		

	}


	public void next() {}
	
	public void search(String searchstring) {
		tableViewer.setInput( getDQ().search(searchstring));
	}





	public static class DownloadQueueTableProvider implements IStructuredContentProvider {

		
		public DownloadQueueTableProvider(){
		}
		
		
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof DownloadQueueFolder) {
				DownloadQueueFolder dqf =(DownloadQueueFolder)inputElement;
				if (dqf.oneFolderChildNothingElse() && dqf.getParent() != null) {
					return getElements(dqf.getChildren()[0]);
				} else {
					return dqf.getChildren() ;
				}
			}
			if (inputElement instanceof List<?>) {
				return ((List<?>)inputElement).toArray();
			}
			return new Object[0];
		}

		
		public void dispose() {}

		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
		
			
	}
	
	public static class DownloadQueueTreeManager extends ViewerComparator implements ITreeContentProvider , ILabelProvider {
		
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return IconManager.get().getFolderIcon();
		}
		
		
		public String getText(Object element) {
			DownloadQueueFolder dqf = ((DownloadQueueFolder)element);
			if (dqf.oneFolderChildNothingElse() ) {
				return dqf.getName()+java.io.File.separator+getText(dqf.getChildren()[0]);
			}
			
			return dqf.getName();
		}
		

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			return getText(e1).compareTo(getText(e2));
		}


		public Object[] getChildren(Object parentElement) {
			DownloadQueueFolder dqf= (DownloadQueueFolder)parentElement;
			if (dqf.oneFolderChildNothingElse() && dqf.getParent() != null) {
				return getChildren(dqf.getChildren()[0]);
			} else {
				return dqf.getChildren();
			}
		}
		
		public Object getParent(Object element) {
			return null;
		}
		
		public boolean hasChildren(Object element) {
			DownloadQueueFolder dqf = (DownloadQueueFolder)element;
			if (dqf.oneFolderChildNothingElse() && dqf.getParent() != null) {
				Object[] children =dqf.getChildren();
				if (children.length == 0) {  //concurrency problem.. if folder is removed after check above..
					return false;
				}
				return hasChildren(children[0]);
			} else {
				return dqf.hasChildFolders();
			}
		}
		
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}


		
		public void addListener(ILabelProviderListener listener) {}
		
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		
		public void removeListener(ILabelProviderListener listener) {}


		
		public void dispose() {}


		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
		
		
	}
	
	public static class DownloadQueueFolderFilter extends ViewerFilter {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return  element instanceof DownloadQueueFolder;
		}
	}
	
	public static class DownloadQueueEntryFilter extends ViewerFilter {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return  element instanceof AbstractDownloadQueueEntry;
		}
	}
	
	
}
