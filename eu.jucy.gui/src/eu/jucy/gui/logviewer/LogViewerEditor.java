package eu.jucy.gui.logviewer;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;



import uc.PI;
import uc.database.DBLogger;
import uc.database.IDatabase;
import uc.database.ILogEntry;
import uihelpers.CommandButton;
import uihelpers.TableViewerAdministrator;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;


import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.ISearchableEditor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.logviewer.LogViewerHandlers.ExportAllLogs;

public class LogViewerEditor extends UCEditor implements ISearchableEditor {

	public static final String ID =  "eu.jucy.gui.logviewer";
	private static Logger logger = LoggerFactory.make();
	
	private static final int hitsPerPage = 500;
	
	private TableViewer tableViewer;
	private Table table;
	private StyledText styledText;
	private DateTime calendar;
	
	private TableViewerAdministrator<DBLogger> tva;
	
	private DBLogger dbLogger;
	
//	private List<LogViewerActions> actions;
	
	private Label countLabel;
	
	private Composite linkComp;
	
	private int page = 0;
	
	private int totalcount;
	
	
	
	private String lastSearch;
	private int lastHit = -1;
	
	@Override
	public void createPartControl(Composite parent) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		parent.setLayout(gridLayout);
		
		tableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER| SWT.V_SCROLL );
		table = tableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		
		tva = new TableViewerAdministrator<DBLogger>(
				tableViewer,
				Collections.singletonList(new LogEntityNameCol()),
				GUIPI.logViewerTable,0);
		
		tva.apply();
		
		final Composite comp  = new Composite(parent,SWT.BORDER);
		final GridLayout gridLayout_0 = new GridLayout();
		gridLayout_0.numColumns = 1;
		comp.setLayout(gridLayout_0);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
		
		
		styledText = new StyledText(comp, SWT.BORDER| SWT.V_SCROLL | SWT.WRAP);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		final Composite belowText  = new Composite(comp,SWT.BORDER);
		final GridLayout gridLayout_b = new GridLayout();
		gridLayout_b.numColumns = 3;
		belowText.setLayout(gridLayout_b);
		belowText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		countLabel = new Label(belowText,SWT.NONE);
		countLabel.setLayoutData(new GridData(100, SWT.DEFAULT));
		Label distLabel = new Label(belowText,SWT.NONE);
		distLabel.setLayoutData(new GridData(100, SWT.DEFAULT));
		
		linkComp = new Composite(belowText,SWT.NONE);
		
		linkComp.setLayout(new RowLayout());
		/*final GridLayout gridLayout_l = new GridLayout();
		gridLayout_l.numColumns = 3;
		belowText.setLayout(gridLayout_l); */
		
		linkComp.setLayoutData(new GridData(SWT.LEFT,SWT.FILL,true,false));
		
		
		

		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 2;
		composite.setLayout(gridLayout_1);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		final Button pruneButton = new Button(composite, SWT.NONE);
		pruneButton.setText(Lang.PruneAllOlderThan);


		calendar = new DateTime(composite, SWT.DATE);

		pruneButton.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				Calendar cal = Calendar.getInstance();
				cal.set(calendar.getYear(), calendar.getMonth(), calendar.getDay());
				ApplicationWorkbenchWindowAdvisor.get().getDatabase().pruneLogentrys(null, cal.getTime());
				page = 0;
				update(true);
			}
		});
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -6); //prune all older than 6 Months as default..
		calendar.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		
		final Button exportAll = new Button(composite,SWT.NONE);
		CommandButton.setCommandToButton(ExportAllLogs.COMMAND_ID, exportAll, getSite(), false);
		
//		exportAll.setText("Export All Logs");
//		exportAll.addSelectionListener (new SelectionAdapter () {
//			public void widgetSelected (SelectionEvent e) {
//				Export.exportAll();
//			}
//		});

		tableViewer.setContentProvider(new LogViewerContentProvider());
		
		tableViewer.setInput(ApplicationWorkbenchWindowAdvisor.get().getDatabase());
		
		tableViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection iss = (IStructuredSelection)selection;
					if (!iss.isEmpty()) {
						dbLogger = (DBLogger)iss.getFirstElement();
						page = 0; //update page..
						update(false);
					}
				}
			}
		});
		
		tableViewer.getTable().getColumn(0).pack();
	//	makeActions();
		getSite().setSelectionProvider(tableViewer);
		createContextPopup(tableViewer);
		
		setControlsForFontAndColour(tableViewer.getTable(),styledText,calendar);
		
		logger.debug("Editor Created");
	}
	

	
	
	void setFilter(final DBLogger logger) {
		tableViewer.addFilter(new DBViewerFilter(logger));
	}
	
	void removeFilter(DBLogger logger) {
		if (!tableViewer.getTable().isDisposed()) {
			tableViewer.removeFilter(new DBViewerFilter(logger));
		}
	}
	
	private static class DBViewerFilter extends ViewerFilter {

		private final DBLogger dbLogger;
		
		public DBViewerFilter(DBLogger dbLogger) {
			super();
			this.dbLogger = dbLogger;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement,Object element) {
			return !dbLogger.equals(element);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((dbLogger == null) ? 0 : dbLogger.hashCode());
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DBViewerFilter other = (DBViewerFilter) obj;
			if (dbLogger == null) {
				if (other.dbLogger != null)
					return false;
			} else if (!dbLogger.equals(other.dbLogger))
				return false;
			return true;
		}

		
	}
	
	
	@Override
	public void dispose() {
//		for (LogViewerActions action: actions) {
//			action.dispose();
//		}
//		actions.clear();
		
		super.dispose();
	}

	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}
	
	private void createLinks() {
		for (Control c : linkComp.getChildren()) {
			c.dispose();
		}
		List<String> s = new ArrayList<String>(Arrays.asList("˂˂","-5","˂","</A>  "+(page+1)+"/"+getNrOFPages()+"  <A>","˃","+5","˃˃")); //these are no normal < > chars !!
		List<Integer> pageInt = new ArrayList<Integer>(Arrays.asList(0,page-5,page-1,page,page+1,page+5,getNrOFPages()-1));
		
		for (int i=0; i < s.size(); i++) {
			final Link link = new Link(linkComp, SWT.NONE);
		    link.setText(" <A>"+s.get(i)+"</A> ");
		    link.setData(pageInt.get(i));
		    
			link.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					int page = (Integer)link.getData();
					if (page < 0) {
						page = 0;
					}
					if (page >= getNrOFPages()) {
						page = getNrOFPages()-1;
					}
					changeToPage(page);
					
				}
			});
		}
	    
	    linkComp.layout();
	    linkComp.pack();

	   
	   // link.setSize(140, 40);
	}
	
	private void changeToPage(int page) {
		if (this.page != page) {
			this.page = page;
			update(false);
		}
	}
	
	
	private int getNrOFPages() {
		return totalcount/hitsPerPage  +  (totalcount % hitsPerPage == 0 ? 0:1) ;
	}
	

	public void update(boolean both) {
		if (both) {
			tableViewer.refresh();
		}
		
		styledText.setText("");
		if (dbLogger != null) {
			List<ILogEntry> logs = dbLogger.loadLogEntrys(hitsPerPage,hitsPerPage * page);
			StringBuilder text = new StringBuilder();
			SimpleDateFormat sdf = new SimpleDateFormat(PI.get(PI.logTimeStamps));
			for (ILogEntry log:logs) {
				text.append(sdf.format(new Date(log.getDate())))
						.append(log.getMessage())
						.append('\n');
				
			}
			styledText.setText(text.toString());
			
			totalcount = dbLogger.countLogEntrys();
			countLabel.setText(String.format(Lang.TotalMessages, totalcount));
			countLabel.pack();
			//offsetLabel.setText("Page: "+(page+1)+"/"+getNrOFPages());
			//offsetLabel.pack();
			
			createLinks();
		}
	}
	
	
	
	public void next() {
		if (lastSearch != null) {
			String text = styledText.getText().toLowerCase();
			lastHit = text.indexOf(lastSearch, lastHit+1);
			if (lastHit != -1) {
				styledText.setSelection(lastHit, lastHit+ lastSearch.length());
			}
		}
	}

	public void search(String searchString) {
		searchString = searchString.toLowerCase();
		if (!searchString.equals(lastSearch)) {
			lastSearch = searchString;
			lastHit = -1;
		} 
		
		next();
	}




	public static class LogViewerContentProvider implements  IStructuredContentProvider  {
		
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IDatabase) {
				return ((IDatabase)inputElement).getLogentitys().toArray();
			}
			throw new IllegalStateException();
		}

		
		public void dispose() {}

		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	}
	
	public static class LogEntityNameCol extends ColumnDescriptor<DBLogger> {
		public LogEntityNameCol() {
			super(200, Lang.Name);
		}

		@Override
		public String getText(DBLogger x) {
			return x.getName();
		}
	}
	
	

}
