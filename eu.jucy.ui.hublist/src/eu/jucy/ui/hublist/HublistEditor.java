package eu.jucy.ui.hublist;


import java.net.URL;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.xml.sax.SAXException;

import uc.DCClient;
import uihelpers.SUIJob;
import uihelpers.StandardContentAndLabelProvider;
import uihelpers.TableColumnSorter;

import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditor;
import eu.jucy.hublist.Column;
import eu.jucy.hublist.HubList;
import eu.jucy.hublist.HublistHub;
import eu.jucy.language.LanguageKeys;
import eu.jucy.ui.hublist.HublistHubActions.AddToFavoritesAction;
import eu.jucy.ui.hublist.HublistHubActions.ConnectAction;
import eu.jucy.ui.hublist.HublistHubActions.CopyAddressAction;


public class HublistEditor extends UCEditor {
	
	private static Logger logger = LoggerFactory.make();

	
	public static final String ID = "eu.jucy.ui.hublist";

	private Label usersLabel;
	private Label hubLabel;
	private Combo hublistsCombo;
	//private Combo anyCombo;
	private Text filterText;
	private Table table;
	
	private TableViewer tableViewer;
	
	private HublistHubActions   
		addToFavoritesAction,
		connectAction,
		copyAddressAction;	





	
	
	@Override
	public String getTopic() {
		return getPartName();
	}



	@Override
	public void createPartControl(Composite parent) {
	
		parent.setLayout(new GridLayout());

		table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));



		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);

		final Group filterGroup = new Group(composite, SWT.NONE);
		filterGroup.setText("Filter");
		filterGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		filterGroup.setBounds(5, 6,189, 47);
		final GridLayout gridLayout_2 = new GridLayout();
		gridLayout_2.verticalSpacing = 0;
		gridLayout_2.marginWidth = 0;
		gridLayout_2.marginHeight = 0;
		gridLayout_2.horizontalSpacing = 0;
		gridLayout_2.numColumns = 2;
		filterGroup.setLayout(gridLayout_2);

		filterText = new Text(filterGroup, SWT.BORDER);
		final GridData gridData_2 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData_2.widthHint = 150;
		filterText.setLayoutData(gridData_2);

		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				new SUIJob() {
					@Override
					public void run() {
						setFilter(filterText.getText());
					}	
				}.scheduleIfNotRunning(700,HublistEditor.this); 
				
			}
		});
		//TODO internationalization
		final Group configuredPublicHubGroup = new Group(composite, SWT.NONE);
		configuredPublicHubGroup.setText("Configured Public Hub List");
		configuredPublicHubGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final GridLayout gridLayout_3 = new GridLayout();
		gridLayout_3.marginHeight = 0;
		gridLayout_3.marginWidth = 2;
		configuredPublicHubGroup.setLayout(gridLayout_3);

		hublistsCombo = new Combo(configuredPublicHubGroup, SWT.NONE);
		hublistsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		hublistsCombo.setItems(HublistPI.getHublists());
		hublistsCombo.select(0);
		hublistsCombo.setVisibleItemCount(15);

		final Button button = new Button(composite, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadHublist();
			}
		});
		button.setLayoutData(new GridData());
		button.setText(LanguageKeys.LoadHublist);

		final Composite composite_1 = new Composite(composite, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.verticalSpacing = 0;
		gridLayout_1.marginWidth = 0;
		gridLayout_1.marginHeight = 0;
		gridLayout_1.horizontalSpacing = 0;
		gridLayout_1.numColumns = 3;
		composite_1.setLayout(gridLayout_1);

		final Label label = new Label(composite_1, SWT.BORDER);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		

		hubLabel = new Label(composite_1, SWT.BORDER);
		final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData.widthHint = 80;
		hubLabel.setLayoutData(gridData);
		

		usersLabel = new Label(composite_1, SWT.BORDER);
		final GridData gridData_1 = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData_1.widthHint = 80;
		usersLabel.setLayoutData(gridData_1);
		
		//
		
		
		tableViewer = new TableViewer(table);
		tableViewer.setUseHashlookup(true);
		PublicHubsProvider provider = new PublicHubsProvider();
		tableViewer.setContentProvider(provider);
		tableViewer.setLabelProvider(provider);
		
		addActions();
		
		new SUIJob() {
			@Override
			public void run() {
				loadHublist();
			}
			
		}.schedule(); 
		setControlsForFontAndColour(tableViewer.getTable(),filterText,hublistsCombo);
	}
	
	private void addActions() {
		addToFavoritesAction 	= new AddToFavoritesAction();
		tableViewer.addPostSelectionChangedListener(addToFavoritesAction);
		connectAction 			= new ConnectAction();
		tableViewer.addPostSelectionChangedListener(connectAction);
		copyAddressAction 		= new CopyAddressAction();
		tableViewer.addPostSelectionChangedListener(copyAddressAction);
		
		tableViewer.addDoubleClickListener(new IDoubleClickListener(){
			public void doubleClick(DoubleClickEvent event ){
				connectAction.run();
			}
		});
		MenuManager hublistmenu = new MenuManager("hublist");
		hublistmenu.add(connectAction);
		hublistmenu.add(addToFavoritesAction);
		hublistmenu.add(copyAddressAction);
		hublistmenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		
    	Menu menu = hublistmenu.createContextMenu(tableViewer.getControl());
    	tableViewer.getControl().setMenu(menu);
    	
    	getSite().registerContextMenu(ID, hublistmenu, tableViewer);
		
	}
	
	private void setFilter(final String filterString){
		//tableViewer.resetFilters();
		if (filterString != null  && filterString.length() >= 3) { 
			logger.debug("adding filter");
			tableViewer.resetFilters();
			tableViewer.addFilter(new ViewerFilter() {
			//	Set<HublistHub> contained = hublist.search(filterString);
					
				public boolean select(Viewer viewer, java.lang.Object parentElement, java.lang.Object element) {
								
					HublistHub hub = (HublistHub)element;
				//	return contained.contains(hub);
					
					return 	hub.getAttribute(Column.HUBNAME).contains(filterString) ||
							hub.getAttribute(Column.DESCRIPTION).contains(filterString) ||
							hub.getAttribute(Column.ADDRESS).contains(filterString); 
				

				}
			});
			//tableViewer.refresh();
		} else {
			tableViewer.resetFilters();
		}
	}
	

	@Override
	public void setFocus() {
		table.setFocus();
	}
	
	void loadHublist() {
		String hubListAddy = hublistsCombo.getText();
		
		try {
			synchronized(this) {
				final URL url = new URL(hubListAddy);
				logger.debug(url.toString());
		
			
				Runnable r = new Runnable() {
					public void run() {
						HubList hl;
						synchronized(HublistEditor.this) {
							hl = new HubList(url);
						}
						try {
							hl.load(true);
						} catch(Exception ioe) {
							handleErrorOnOpeningHublist(ioe);
						} 
						handleHubListLoaded(hl);
					}
				};
				
				DCClient.execute(r);
			}
		} catch(Exception e) {
			handleErrorOnOpeningHublist(e);
		}
		
	}
	
	private void handleErrorOnOpeningHublist(final Exception e) {
		new SUIJob(table) {

			@Override
			public void run() {
				if (e instanceof SAXException) {
					String mes = e.getMessage();
					MessageDialog.openError(getSite().getShell(), "Error", "Error invalid Hublist. No valid XML found.\n" + (mes != null? mes: e.getClass().getSimpleName()));	
				} else {
					String mes = e.getMessage();
					MessageDialog.openError(getSite().getShell(), "Error", "Error loading Hublist:\n" + (mes != null? mes: e.getClass().getSimpleName()));	
				}
				logger.info(e,e);
				
			}
			
		}.schedule();
	}
	
	private void handleHubListLoaded(final HubList hl) {
		new SUIJob(table) {
			@Override
			public void run() {
				tableViewer.setInput(hl);
			}
			
		}.schedule();
	}
	
	class PublicHubsProvider extends StandardContentAndLabelProvider<HublistHub,HubList> {

		private List<Column> current;
		
		@Override
		protected Image getColumnImage(int columnIndex, HublistHub element) {
			return null;
		}

		@Override
		protected String getColumnText(int columnIndex, HublistHub element) {
			Column c = current.get(columnIndex);
			return element.getPresentableForColumn(c);
		}

		
		
		@Override
		public HublistHub[] getElementss(HubList inputElement) {
			return inputElement.getHubs().toArray(new HublistHub[]{});
		}

		@Override
		public void inputChangeds(Viewer viewer, HubList oldInput,HubList newInput) {
			if (newInput != null) {
				current = newInput.getColumns();
				setHubLabel(newInput.getNrOfHubs());
				setUsersLabel(newInput.getUserCount());
				
				for (TableColumn tc: table.getColumns() ) {
					tc.dispose();
				}
				
				for (Column c: current) {
					final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
					tableColumn.setWidth(c.getDefaultWidth());
					tableColumn.setText(c.getName());
					tableColumn.setMoveable(true);
					try {
						new TableColumnSorter<HublistHub>(tableViewer,tableColumn,c.getComparator());
					} catch(Exception e) {
						logger.error(e, e);
					}
				}
			}
		}
		
	}
	
	protected void setHubLabel(int hubs) {
		hubLabel.setText(Lang.Hubs+": "+hubs);
	}
	protected void setUsersLabel(long users) {
		usersLabel.setText(Lang.Users+": "+users);
	}

}
