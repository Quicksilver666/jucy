package eu.jucy.gui.favhub;




import helpers.IObservable;
import helpers.Observable.IObserver;


import java.util.Arrays;


import logger.LoggerFactory;

import org.apache.log4j.Logger;


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
	private TableViewerAdministrator<FavHub> tva;
	private IFavHubs favHubs = ApplicationWorkbenchWindowAdvisor.get().getFavHubs();

	
	public IFavHubs getFavHubs() {
		return favHubs;
	}


	public void createPartControl(Composite parent) {
		
		parent.setLayout(new GridLayout());

		table = new Table(parent,SWT.CHECK | SWT.SINGLE|SWT.FULL_SELECTION |SWT.HIDE_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION );
		
		
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
		createContextPopup(tableViewer);
		

		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		comp.setLayout(new RowLayout());
		
		final Composite composite = new Composite(comp, SWT.NONE);
		composite.setLayoutData(new RowData());
		
		FillLayout fillLayout = new FillLayout();
		fillLayout.spacing = 5;
		composite.setLayout(fillLayout);
		
		for (String command:new String[]{
				CreateFavHubsHandler.COMMAND_ID,ChangeFHPropertiesHandler.COMMAND_ID,
				RemoveHandler.COMMAND_ID,MoveUpHandler.COMMAND_ID,
				MoveDownHandler.COMMAND_ID,OpenHubHandler.COMMAND_ID}) {
			
			Button button = new Button(composite, SWT.NONE);
			CommandButton.setCommandToButton(command, button, getSite(),false);
		}
	
		tableViewer.setInput(favHubs);
		favHubs.addObserver(this);
		logger.debug("created FavHub editor");
		setControlsForFontAndColour(tableViewer.getTable());
	}
	
	
	public String getTopic() {
		return getPartName();
	}

	

	public void update(IObservable<FavHub> o, FavHub arg) {
		new SUIJob() {
			public void run() {
				logger.debug("refreshing FavHubs");
				tableViewer.refresh(); 
			}
			
		}.schedule();
	}

	
	public void setFocus() {
		table.setFocus();
	}
	
	public void dispose(){
		favHubs.deleteObserver(this);
		favHubs.store();

		super.dispose();
	}

	
	public static class FavHubContentProvider implements IStructuredContentProvider {

		
		public void dispose() {}

		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		public Object[] getElements(Object inputElement) {
			IFavHubs hubs = (IFavHubs)inputElement;
			return hubs.getFavHubs().toArray();
		}
		
	}

}
