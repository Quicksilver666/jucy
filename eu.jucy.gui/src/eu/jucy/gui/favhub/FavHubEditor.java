package eu.jucy.gui.favhub;




import helpers.IObservable;
import helpers.Observable.IObserver;

import java.io.IOException;
import java.util.Arrays;


import logger.LoggerFactory;

import org.apache.log4j.Logger;


import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;





import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.favhub.FavHubColumns.Address;
import eu.jucy.gui.favhub.FavHubColumns.ChatOnly;
import eu.jucy.gui.favhub.FavHubColumns.Description;
import eu.jucy.gui.favhub.FavHubColumns.Email;
import eu.jucy.gui.favhub.FavHubColumns.FavHubName;
import eu.jucy.gui.favhub.FavHubColumns.Nick;
import eu.jucy.gui.favhub.FavHubColumns.Password;
import eu.jucy.gui.favhub.FavHubColumns.UserDescription;
import eu.jucy.gui.favhub.FavHubHandlers.ChangeFHPropertiesHandler;
import eu.jucy.gui.favhub.FavHubHandlers.CreateFavHubsHandler;
import eu.jucy.gui.favhub.FavHubHandlers.MoveDownHandler;
import eu.jucy.gui.favhub.FavHubHandlers.MoveUpHandler;
import eu.jucy.gui.favhub.FavHubHandlers.OpenHubHandler;
import eu.jucy.gui.favhub.FavHubHandlers.RemoveHandler;
import eu.jucy.gui.itemhandler.CommandDoubleClickListener;





import uc.FavHub;
import uc.IFavHubs;
import uihelpers.CommandButton;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;

/**
 * very ugly class..
 * 
 * lots of code mingled with GUI code.. needs rewrite / refactoring
 * 
 * also if no TableViewer is used..it should be easy and possible 
 * to use the native CheckBoxes in the table.. 
 * 
 * @author Quicksilver
 *
 */
public class FavHubEditor extends UCEditor implements IObserver<FavHub> {

	
	
	public static final String ID = "eu.jucy.FavHub";
	
	public static final String OPEN_FAVHUBS_COMMAND_ID = "eu.jucy.gui.favhub.FavHub";
	
	private static final Logger logger = LoggerFactory.make();
	
	
	private Table table;
	private CheckboxTableViewer tableViewer;
//	private OpenHubAction openHubAction;
//	private CreateFavHubAction createFavHubAction;
//	private ChangePropertiesAction changePropertiesAction;
	
	
	
	private TableViewerAdministrator<FavHub> tva;

	
	private IFavHubs favHubs = ApplicationWorkbenchWindowAdvisor.get().getFavHubs();

	
	public IFavHubs getFavHubs() {
		return favHubs;
	}


	public void createPartControl(Composite parent) {
		
		parent.setLayout(new GridLayout());

		table = new Table(parent,SWT.CHECK | SWT.SINGLE|SWT.FULL_SELECTION |SWT.HIDE_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION );
	/*	table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				logger.debug("doubleClick received. ");
				connect();
			}
		}); */
		
		
		tableViewer = new CheckboxTableViewer( table );
		tableViewer.addDoubleClickListener(new CommandDoubleClickListener( OpenHubHandler.COMMAND_ID));
		

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		
		
		
		tva = new TableViewerAdministrator<FavHub>(tableViewer,
				Arrays.asList(new FavHubName(tableViewer), new Description(),new Nick(),
						new Password(),new Address(),new UserDescription(), 
						new Email(),new ChatOnly()),
						GUIPI.favHubsTable,TableViewerAdministrator.NoSorting,false);
		
		
		FavHubContentProvider fh = new FavHubContentProvider();
		tableViewer.setContentProvider(fh);
		tva.apply();
		
	
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				Integer a = ((FavHub)e1).getOrder(); 
				Integer b = ((FavHub)e2).getOrder(); 
				return a.compareTo(b);
			}
		});
	
		
		getSite().setSelectionProvider(tableViewer);
		

//		makeActions();
		
		//createPopUpMenu();
		createContextPopup(tableViewer);
		

		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		comp.setLayout(new RowLayout());
		
		final Composite composite = new Composite(comp, SWT.NONE);
		composite.setLayoutData(new RowData());
		
		FillLayout fillLayout = new FillLayout();
		fillLayout.spacing = 5;
		composite.setLayout(fillLayout);
		
		final Button newButton = new Button(composite, SWT.NONE);
		CommandButton.setCommandToButton(CreateFavHubsHandler.COMMAND_ID, newButton, getSite(),false);
		
//		newButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				create();
//			}
//		});
//		
//		newButton.setText(createFavHubAction.getText());

		final Button propertiesButton = new Button(composite, SWT.NONE);
		CommandButton.setCommandToButton(ChangeFHPropertiesHandler.COMMAND_ID, propertiesButton, getSite(),false);
		
//		propertiesButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				changeProperties();
//			}
//		});
//		propertiesButton.setText(changePropertiesAction.getText());
		
		
		

		final Button removeButton = new Button(composite, SWT.NONE);
		CommandButton.setCommandToButton(RemoveHandler.COMMAND_ID, removeButton, getSite(),false);
//		removeButton.setText(Lang.Remove);
//		removeButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				remove();
//			}
//		});

		final Button moveUpButton = new Button(composite, SWT.NONE);
		CommandButton.setCommandToButton(MoveUpHandler.COMMAND_ID, moveUpButton, getSite(),false);
		
//		moveUpButton.setText(Lang.MoveUp);
//		moveUpButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				move(true);
//			}
//		});

		final Button moveDownButton = new Button(composite, SWT.NONE);
		CommandButton.setCommandToButton(MoveDownHandler.COMMAND_ID, moveDownButton, getSite(),false);
//		moveDownButton.setText(Lang.MoveDown);
//		moveDownButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				move(false);
//			}
//		});
		final Button connectButton = new Button(composite, SWT.NONE);
		CommandButton.setCommandToButton(OpenHubHandler.COMMAND_ID, connectButton, getSite(),false);
		
//		connectButton.setText(openHubAction.getText());
//		connectButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				connect();
//			}
//		});
		//
		
	
	
		tableViewer.setInput(favHubs);
		favHubs.addObserver(this);
		logger.debug("created FavHub editor");
		setControlsForFontAndColour(tableViewer.getTable());
	}
	
	
	public String getTopic() {
		return getPartName();
	}



//	private void makeActions() {
//		IWorkbenchWindow window = getSite().getWorkbenchWindow();
//		openHubAction = new OpenHubAction(tableViewer);
//		createFavHubAction = new CreateFavHubAction(window);
//		changePropertiesAction = new ChangePropertiesAction(window,tableViewer);
//	}
	
	
//	private void createPopUpMenu() {
//		final Menu menu = new Menu(table);
//		
//		
//		final MenuItem menuItem = new MenuItem(menu, SWT.NONE);
//		menuItem.setText(openHubAction.getText());
//		menuItem.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				connect();
//			}
//		});
//		menuItem.setAccelerator(SWT.ALT | 'C');
//		
//
//		final MenuItem menuItem_1 = new MenuItem(menu, SWT.NONE);
//		menuItem_1.setText(createFavHubAction.getText());
//		
//		menuItem_1.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				create();
//			}
//		});
//		menuItem_1.setAccelerator(createFavHubAction.getAccelerator());
//		
//		
//		final MenuItem menuItem_2 = new MenuItem(menu, SWT.NONE);
//		menuItem_2.setText(changePropertiesAction.getText());
//		menuItem_2.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				changeProperties();
//			}
//		});
//		menuItem_2.setAccelerator(changePropertiesAction.getAccelerator());
//		
//
//		final MenuItem menuItem_3 = new MenuItem(menu, SWT.NONE);
//		menuItem_3.setText(Lang.MoveUp);
//		menuItem_3.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				move(true);
//			}
//		});
//		
//		final MenuItem menuItem_4 = new MenuItem(menu, SWT.NONE);
//		menuItem_4.setText(Lang.MoveDown);
//		menuItem_4.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				move(false);
//			}
//		});
//		
//
//		new MenuItem(menu, SWT.SEPARATOR);
//
//		final MenuItem menuItem_5 = new MenuItem(menu, SWT.NONE);
//		menuItem_5.setText(Lang.Remove);
//		menuItem_5.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(final SelectionEvent e) {
//				remove();
//			}
//		});
//		table.setMenu(menu);
//	}
	
//	private void remove() {
//		FavHub fh = getCurrent();
//		
//		if (fh != null) {
//			fh.removeFromFavHubs(favHubs);
//			//tableViewer.remove(fh);
//			//tableViewer.refresh();
//		}
//	}
//	
//	private void move(boolean up) {
//		FavHub fh = getCurrent();
//		
//		if (fh != null) {
//			fh.changePriority(up,favHubs);
//			//tableViewer.refresh();
//		}
//	}
//
//	public void changeProperties() {
//		if (changePropertiesAction.isEnabled()) {
//			changePropertiesAction.run();
//		}
//	}
//	
//	
//	private void connect(){
//		if (openHubAction.isEnabled()) {
//			openHubAction.run();
//		}
//	}
//	
//	private FavHub getCurrent() {
//		ISelection selection = tableViewer.getSelection();
//		if (selection instanceof IStructuredSelection && !((IStructuredSelection)selection).isEmpty() )
//			return (FavHub)((IStructuredSelection)selection).getFirstElement();
//		
//		return null;
//	}
//	
//	private void create() {
//		createFavHubAction.run();
//		tableViewer.refresh();
//	}

	
	
	
	
	public void update(IObservable<FavHub> o, FavHub arg) {
		new SUIJob() {
			public void run() {
				logger.debug("refreshing FavHubs");
				tableViewer.refresh(); 
//				return Status.OK_STATUS;
			}
			
		}.schedule();
		
	}

	
	public void setFocus() {
		table.setFocus();
	}
	
	public void dispose(){
		favHubs.deleteObserver(this);
		try {
			favHubs.store();
		} catch(final IOException ioe) {
			new SUIJob() {
				public void run() {
					MessageDialog.openError(getWindow().getShell(), "Error", ioe.toString());
				}
			}.schedule();
		}
		
//	
//		changePropertiesAction.dispose();
//		createFavHubAction.dispose();
//		openHubAction.dispose();

		super.dispose();
	}

	
	public static class FavHubContentProvider implements IStructuredContentProvider {

		
		public void dispose() {
		}

		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		
		public Object[] getElements(Object inputElement) {
			IFavHubs hubs = (IFavHubs)inputElement;
			return hubs.getFavHubs().toArray();
		}
		
	}

}
