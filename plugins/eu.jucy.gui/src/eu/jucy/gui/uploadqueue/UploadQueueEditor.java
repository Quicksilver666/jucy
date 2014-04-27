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
import uihelpers.DelayedTableUpdater;
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
	private DelayedTableUpdater<UploadInfo> update;
	

	
	@Override
	public void createPartControl(Composite arg0) {
		logger.debug("create upqueue");
		GridLayout gridLayout = new GridLayout();
		arg0.setLayout(gridLayout);

		tableViewer = new TableViewer(arg0, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tva = new TableViewerAdministrator<UploadInfo>(tableViewer,
				Arrays.asList(NameUserCol.<UploadInfo>get(),new NameRequestedCol(),new TotalSizeCol(),
						new FirstRequestCol(),new RequestsReceivedCol(),new LastRequestCol(), 
						new SlotReceivedCol(),new PositionCol()),
						GUIPI.uploadQueueTable,7);
		tva.apply();
		
		update = new DelayedTableUpdater<UploadInfo>(tableViewer);
		
		tableViewer.setContentProvider(new UploadQueueProvider());
		tableViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,Object element) {
				UploadInfo ui = (UploadInfo)element;
				long milisecondssincelast = System.currentTimeMillis()- ui.getLastRequest().getTime();
				return milisecondssincelast < 1000*60*10; //only show users with requests in the last 10 minutes..
			}
			
		});
		
		
		getSite().setSelectionProvider(tableViewer);
		createContextPopup(HubEditor.ID, tableViewer);
		ApplicationWorkbenchWindowAdvisor.get().getUpQueue().addObserver(this);
		tableViewer.setInput(ApplicationWorkbenchWindowAdvisor.get().getUpQueue());
		
		
		new SUIJob(table) {
			@Override
			public void run() {
				update.clear();
				tableViewer.setInput(ApplicationWorkbenchWindowAdvisor.get().getUpQueue());
				schedule(10*1000);
			}
		}.schedule(10*1000);
		
		setControlsForFontAndColour(table);
	}


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
		if (st.getValue() instanceof UploadInfo) {
			update.put(st.getType(), (UploadInfo)st.getValue());
		}
	}
	
}
