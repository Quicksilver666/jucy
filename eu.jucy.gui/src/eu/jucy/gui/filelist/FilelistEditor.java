package eu.jucy.gui.filelist;



import helpers.SizeEnum;


import java.text.Collator;
import java.util.Arrays;
import java.util.List;




import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;

import org.eclipse.jface.viewers.ISelectionChangedListener;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;


import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;



import eu.jucy.gui.FindHandler;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.ISearchableEditor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.DownloadableColumn.ExactSize;
import eu.jucy.gui.DownloadableColumn.FileColumn;
import eu.jucy.gui.DownloadableColumn.Path;
import eu.jucy.gui.DownloadableColumn.Size;
import eu.jucy.gui.DownloadableColumn.TTHRoot;
import eu.jucy.gui.DownloadableColumn.Type;
import eu.jucy.gui.itemhandler.DownloadableHandlers.DownloadHandler;



import uc.files.IDownloadable;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.filelist.FileList;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;
import uc.files.filelist.IFileListItem;
import uihelpers.IconManager;
import uihelpers.SUIJob;
import uihelpers.SelectionProviderIntermediate;
import uihelpers.TableViewerAdministrator;

public class FilelistEditor extends UCEditor implements ISearchableEditor {
	
	
	
	private CLabel containedSize;
	private CLabel containedFiles;
	public static final String ID = "eu.jucy.Filelist";
	
	private volatile FileList fileList;
	
	private String lastSearch;
	private int searchIndex = -1;
	private List<IFileListItem> found;

	
	private Table table;
	private Tree tree;
	private TreeViewer treeViewer;
	private TableViewer tableViewer;
	
	private Composite composite;

	private CLabel totalsize;
	private CLabel totalFiles;
	
	private TableViewerAdministrator<IDownloadable> tva;
	
	private final SelectionProviderIntermediate spi = new SelectionProviderIntermediate();

	
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {	
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		parent.setLayout(gridLayout);

		composite = new Composite(parent, SWT.NONE);
		final SashForm sashForm = new SashForm(composite, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tree = new Tree(sashForm, SWT.BORDER);



		table = new Table(sashForm, SWT.BORDER|SWT.FULL_SELECTION | SWT.MULTI);
		table.setHeaderVisible(true);	
		
		
		sashForm.setWeights(new int[] {168, 329 });

	
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = 487;
		
		composite.setLayoutData(gridData);
		composite.setLayout(new GridLayout());

		final Composite composite_1 = new Composite(parent, SWT.BORDER);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.verticalSpacing = 0;
		gridLayout_1.marginWidth = 0;
		gridLayout_1.marginHeight = 0;
		gridLayout_1.horizontalSpacing = 1;
		gridLayout_1.numColumns = 7;
		
	//	RowLayout rowLayout = new RowLayout();
	//	rowLayout.wrap = false;
		
		composite_1.setLayout(gridLayout_1);
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		composite_1.setLayoutData(gridData_1);

		final CLabel label = new CLabel(composite_1, SWT.BORDER);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	//	label.setLayoutData(new RowData());
		label.setText(getList().getGenerator());

//		final CLabel label_1 = new CLabel(composite_1, SWT.BORDER);
//		final GridData gridData_2 = new GridData(SWT.FILL, SWT.FILL, false, false);
//		gridData_2.widthHint = 100;
//		label_1.setLayoutData(gridData_2);
		
		int labelWidth= 100;
		GC gc = new GC(composite_1);
		int cWidth = gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();
		labelWidth = (int)(Math.max(Lang.Size.length()+10, Lang.Files.length()+8)*cWidth*1.05d);
		
	

		totalFiles = new CLabel(composite_1, SWT.BORDER);
		final GridData gridData_5 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_5.widthHint = labelWidth;
		totalFiles.setLayoutData(gridData_5);
	

		totalsize = new CLabel(composite_1, SWT.BORDER);
		final GridData gridData_4 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_4.widthHint = labelWidth;
		totalsize.setLayoutData(gridData_4);
	

		containedFiles = new CLabel(composite_1, SWT.BORDER);
		final GridData gridData_3 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_3.widthHint = labelWidth;
		containedFiles.setLayoutData(gridData_3);
	

		containedSize = new CLabel(composite_1, SWT.BORDER);
		final GridData gridData_6 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_6.widthHint = labelWidth;
		containedSize.setLayoutData(gridData_6);
	

		final Button findButton = new Button(composite_1, SWT.NONE);
		findButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				executeCommand(FindHandler.ID);
			}
		});
		findButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		findButton.setText(Lang.Find);
		

		final Button nextButton = new Button(composite_1, SWT.NONE);
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				next();
			}
		});
		nextButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nextButton.setText(Lang.Next);
		
		
		createColumns();
		
		treeViewer = new TreeViewer(tree);
		FilelistTreeProvider ftrp = new FilelistTreeProvider();
		treeViewer.setContentProvider(ftrp);
		treeViewer.setLabelProvider(ftrp);
		treeViewer.addFilter(new FolderFilter() ); //filter that shows only the folders..
		treeViewer.setSorter(new ViewerSorter() {
			private final Collator col = Collator.getInstance();
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return col.compare(((FileListFolder)e1).getName(), 
								((FileListFolder)e2).getName());
			
			}
			
		});
		treeViewer.setInput(getList());
		
		tableViewer = new TableViewer(table);
		FilelistTableProvider ftap= new FilelistTableProvider();
		tableViewer.setContentProvider(ftap);
		
		
		tva = new TableViewerAdministrator<IDownloadable>(tableViewer,
				Arrays.asList(new FileColumn(), new Type(),new Size(),new TTHRoot(),new ExactSize(),new Path()),
				GUIPI.fileListTable,0);

		
		tva.apply();
		
		setControlsForFontAndColour(tableViewer.getTable(),treeViewer.getTree());
		
		addListener();
		
		//makeActions();
		
		spi.addViewer(tableViewer);
		spi.addViewer(treeViewer);
		getSite().setSelectionProvider(spi);
		createContextPopup(tableViewer);
		createContextPopup(treeViewer);

		
		
		new SUIJob(tree) {
			@Override
			public void run() {
				treeViewer.refresh();
				updateTotal();
			//	setTotalsize(getList().getSharesize());
			//	setTotalFiles(getList().getNumberOfFiles());
				
				if (!getList().isCompleted()) {
					schedule(1000);
				} else {
					treeViewer.expandToLevel(2);
					FileList list = getList();
					IDownloadable in= ((FilelistEditorInput)getEditorInput()).getInitialSelection();
					FileListFile select = null;
					IDownloadable input = list.getRoot();
					if ( in != null) {
						IDownloadable parentFolder = list.getRoot().getByPath(in.getOnlyPath());
						if (in.isFile()) {
							select = list.search( ((IDownloadableFile)in).getTTHRoot() );
							if (parentFolder == null && select != null) {
								parentFolder = select.getParent();
							}
						}
						if (parentFolder != null) {
							input = parentFolder;
						}
					}
					tableViewer.setInput(input);
					if (select != null) {
						tableViewer.setSelection(new StructuredSelection(select), true);
					}
				}
				
			}
		}.schedule();
		
//		DCClient.execute(new Runnable() {
//			private final SUIJob refresher = new SUIJob() {
//				@Override
//				public void run() {
//					if(!tree.isDisposed()) {
//						treeViewer.refresh();
//						setTotalsize(getList().getSharesize());
//						setTotalFiles(getList().getNumberOfFiles());
//					}
//					
//				}
//			};
//			
//			public void run() {	
//				if (!getList().isCompleted()) {
//					ApplicationWorkbenchWindowAdvisor.get().getSchedulerDir().schedule(
//							this, 1, TimeUnit.SECONDS);
//					if (!table.isDisposed()) {
//						
//						refresher.schedule();
//					}
//					return;
//				}
//				//a last refresh to ensure we are up to date
//				refresher.schedule();
//
//				//expand some items for a nicer view ..
//				
//				new SUIJob() {
//					@Override
//					public void run() {
//						if (!table.isDisposed()) {
//							treeViewer.expandToLevel(2);
//							FileList list = getList();
//							IDownloadable in= ((FilelistEditorInput)getEditorInput()).getInitialSelection();
//							IDownloadable input = list.getRoot();
//							if ( in != null) {
//								IDownloadable parentFolder = list.getRoot().getByPath(in.getOnlyPath());
//								if (parentFolder != null) {
//									input = parentFolder;
//								}
//							}
//							tableViewer.setInput(input);
//						}
//					}
//				}.schedule();			
//				
//			}
//		}); 
//		
		
	}
	
	
	public void dispose(){
	//	disposeActions();
		fileList = null;
		super.dispose();
	}
	
	


	public void next() {
		if (found != null && !found.isEmpty()) {
			searchIndex++;
			IFileListItem current = found.get(searchIndex % found.size());
			tableViewer.setInput(current.getParent());
			tableViewer.setSelection(new StructuredSelection(current), true);
		}
	}




	public void search(String searchstring) {
		if (!searchstring.equals(lastSearch)) {
			lastSearch = searchstring;
			searchIndex = -1;
			found = getList().search(lastSearch);
		}
		next();
	}



	private synchronized FileList getList() {
		if (fileList == null ) {
			fileList = ((FilelistEditorInput)getEditorInput()).getFilelistDescriptor().getFilelist();
		}
		return fileList;
	}
	
	private void createColumns() {
		final TreeColumn newColumnTreeColumn = new TreeColumn(tree, SWT.NONE);
		newColumnTreeColumn.setWidth(1000);
		
	}
	
	
	private void addListener(){
		//add a listener in the tree  on selection .. show that folder in the table..
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				IStructuredSelection selection =
					(IStructuredSelection) event.getSelection();

				Object selectedFolder = selection.getFirstElement();
				tableViewer.setInput(selectedFolder);
		      }
		    });
		
		// a listener that expands a folder
		treeViewer.addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();

				Object selectedFolder = selection.getFirstElement();
				if (selectedFolder instanceof FileListFolder) {
					treeViewer.expandToLevel(selectedFolder, 1);
				}
				
			}
		 });
		
		 //add a listener for doubleclicks in the table on a folder --> go one deeper.. ( and show it BACK in the tree)
		 tableViewer.addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();

				Object selected = selection.getFirstElement();
				if (selected instanceof FileListFolder) {
					tableViewer.setInput(selected);
				} else if (selected instanceof IDownloadableFile) {
					executeCommand(DownloadHandler.ID);
				}

			}
		 });

	}
	
	private void updateTotal() {
		totalsize.setText(Lang.Size+": "+SizeEnum.getReadableSize(getList().getSharesize()));
		totalFiles.setText(Lang.Files+": "+getList().getNumberOfFiles());
		totalFiles.getParent().layout();
	}
	
//	void setTotalsize(long total){
//		
//	}
//	
//	void setTotalFiles(int total){
//		totalFiles.setText(Lang.Files+": "+total);
//	}
//	
	private void setContained(FileListFolder folder) {
		containedFiles.setText(""+Lang.Files+": "+folder.getContainedFiles() );
		containedSize.setText(Lang.Size+": "+SizeEnum.getReadableSize(folder.getContainedSize()));
		containedFiles.getParent().layout();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		tree.setFocus();
	}

	class FilelistTableProvider implements IStructuredContentProvider  {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof FileListFolder){
				FileListFolder folder = (FileListFolder)inputElement;
				setContained(folder);
				treeViewer.expandToLevel(folder, 1);
				
				List<IFileListItem> children = folder.getChildren();
//				if (folder.getParent() != null) {
//					children.add(0, new FileListFolderUp(folder.getParent())); 
//				}
				return  children.toArray();
			}
			return new Object[0];
		}


		public void dispose() {
		}


		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
		}
	}
	
//	public static class FileListFolderUp extends FileListFolder {
//
//		private final FileListFolder copy;
//		
//		public FileListFolderUp( FileListFolder copy) {
//			super(copy.getFilelist(), copy.getParent(), "..");
//			this.copy = copy;
//		}
//
//		public AbstractDownloadQueueEntry download() {
//			return copy.download();
//		}
//
//		public AbstractDownloadQueueEntry download(File target) {
//			return copy.download(target);
//		}
//
//		public FileListFolder getChildPerName(String foldernameOfTheChild) {
//			return copy.getChildPerName(foldernameOfTheChild);
//		}
//
//		public List<IFileListItem> getChildren() {
//			return copy.getChildren();
//		}
//
//		public int getContainedFiles() {
//			return copy.getContainedFiles();
//		}
//
//		public long getContainedSize() {
//			return copy.getContainedSize();
//		}
//
//		public List<FileListFile> getFiles() {
//			return copy.getFiles();
//		}
//
//		public List<FileListFolder> getSubfolders() {
//			return copy.getSubfolders();
//		}
//
//	}
	
	
	public static class FilelistTreeProvider implements ITreeContentProvider , ILabelProvider  {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return IconManager.get().getFolderIcon();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if(element instanceof FileListFolder){
				FileListFolder ff=((FileListFolder)element);
				if (ff.getParent() == null) { //if it is the root folder ... show the username
					return ff.getFilelist().getUsr().getNick();
				} else {
					return ff.getName();  //fielname
				}
				
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof FileListFolder) {
				return ((FileListFolder)parentElement).getChildren().toArray();
			}
			return new Object[]{};
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof FileListFolder) {
				return ((FileListFolder)element).getParent();
			}
			return ((FileListFile)element).getParent();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			return ((FileListFolder)element).hasSubfolders();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return new Object[]{ ((FileList)inputElement).getRoot()};
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {}
		
		
		
	}
	
	public static class FolderFilter extends ViewerFilter {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return  element instanceof FileListFolder;
		}
		
		
	}


	
	
}
