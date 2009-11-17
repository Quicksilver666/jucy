package eu.jucy.gui.uploadqueue;

import helpers.IObservable;
import helpers.StatusObject;
import helpers.Observable.IObserver;


import java.util.Arrays;


import logger.LoggerFactory;


import org.apache.log4j.Logger;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;




import uc.files.UploadQueue.UploadInfo;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.UserColumns.NameUserCol;
import eu.jucy.gui.texteditor.hub.HubEditor;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.FirstRequestCol;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.LastRequestCol;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.NameRequestedCol;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.PositionCol;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.RequestsReceivedCol;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.SlotReceivedCol;
import eu.jucy.gui.uploadqueue.UploadQueueColumns.TotalSizeCol;

public class UploadQueueEditor extends UCEditor implements IObserver<StatusObject> {

	private static Logger logger = LoggerFactory.make(); 
	
	static {
		//logger.setLevel(Platform.inDevelopmentMode()? Level.DEBUG:Level.INFO);
	}
	
	public static final String ID = "eu.jucy.gui.UploadQueue";
	
	private Table table;
	private TableViewer tableViewer;
	private TableViewerAdministrator<UploadInfo> tva;
	
//	private final List<IWorkbenchAction> actions = new ArrayList<IWorkbenchAction>();
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite arg0) {
		final GridLayout gridLayout = new GridLayout();
		arg0.setLayout(gridLayout);

		tableViewer = new TableViewer(arg0, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tva = new TableViewerAdministrator<UploadInfo>(tableViewer,
				Arrays.asList(NameUserCol.<UploadInfo>get(),new NameRequestedCol(),new TotalSizeCol(),
						new FirstRequestCol(),new RequestsReceivedCol(),new LastRequestCol(), 
						new SlotReceivedCol(),new PositionCol()),
						GUIPI.uploadQueueTable,5);
		tva.apply();
		
		tableViewer.setContentProvider(new UploadQueueProvider());
		tableViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				UploadInfo ui = (UploadInfo)element;
				long milisecondssincelast = System.currentTimeMillis()- ui.getLastRequest().getTime();
				return milisecondssincelast < 1000*60*10; //only show users with requests in the last 10 minutes..
			}
			
		});
		
		//makeActions();
		getSite().setSelectionProvider(tableViewer);
		createContextPopup(HubEditor.ID, tableViewer);
		ApplicationWorkbenchWindowAdvisor.get().getUpQueue().addObserver(this);
		tableViewer.setInput(ApplicationWorkbenchWindowAdvisor.get().getUpQueue());
		
		
		new SUIJob() {

			@Override
			public void run() {
				if (!table.isDisposed()) {
					tableViewer.refresh();
					schedule(60*1000);
				}
				//return Status.OK_STATUS;
			}
			
		}.schedule(60*1000);
		
		setControlsForFontAndColour(tableViewer.getTable());
	}
	
/*	private void makeActions() {
		final IWorkbenchWindow window = getSite().getWorkbenchWindow();
		PathMenuManager menuManager = new PathMenuManager();
		actions.addAll(UserActions.createUserActions(tableViewer, window, menuManager,false));
		
		Menu menu = menuManager.createContextMenu(tableViewer.getControl());
    	tableViewer.getControl().setMenu(menu);
	} */

	@Override
	public void dispose() {
		ApplicationWorkbenchWindowAdvisor.get().getUpQueue().deleteObserver(this);


		
		super.dispose();
	}



	@Override
	public void setFocus() {
		table.setFocus();
	}


	public void update(IObservable<StatusObject> arg0, final StatusObject st) {
		//final StatusObject st = (StatusObject)arg1;
		if (st.getValue() instanceof UploadInfo) {
			new SUIJob() {
				@Override
				public void run() {
					logger.debug("found: "+st.getType()+"  "+ ((UploadInfo)st.getValue()).getUser().toString());
					if (!table.isDisposed()){
						switch(st.getType()) {
						case ADDED:
							tableViewer.add(st.getValue());
							break;
						case CHANGED:
							tableViewer.refresh(st.getValue());
							break;
						case REMOVED:
							tableViewer.remove(st.getValue());
							break;
						}
					}
	
				
				}
				
			}.schedule();
		}
		
	}

	
	
}
