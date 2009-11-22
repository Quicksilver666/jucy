package eu.jucy.gui.favuser;





import java.util.Arrays;
import java.util.HashSet;




import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Table;




import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.UCEditor;
import eu.jucy.gui.UserColumns.HubName;
import eu.jucy.gui.UserColumns.LastSeen;
import eu.jucy.gui.UserColumns.Nick;
import eu.jucy.gui.UserColumns.SlotUntil;
import eu.jucy.gui.favuser.FavUsersColumns.FUNick;
import eu.jucy.gui.texteditor.hub.HubEditor;



import uc.IUser;
import uc.Population;
import uc.listener.IUserChangedListener;



import uihelpers.SUIJob;
import uihelpers.SelectionProviderIntermediate;
import uihelpers.TableViewerAdministrator;

public class FavUserEditor extends UCEditor implements IUserChangedListener {

	public static final String ID = "eu.jucy.gui.favuser";
	
	private CheckboxTableViewer tableViewerFav;
	private TableViewerAdministrator<IUser> tvaFav;
	
	private TableViewer tableViewerSlot;
	private TableViewerAdministrator<IUser> tvaSlot;
	

	
	private final Population pop;
	
	public FavUserEditor() {
		pop = ApplicationWorkbenchWindowAdvisor.get().getPopulation();
	}

	private final SelectionProviderIntermediate spi = new SelectionProviderIntermediate();


	@SuppressWarnings("unchecked")
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		
		ExpandBar bar = new ExpandBar (parent, SWT.V_SCROLL);

		Table table = new Table(bar,SWT.CHECK | SWT.SINGLE|SWT.FULL_SELECTION |SWT.HIDE_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION );
		table.setHeaderVisible(true);
		
		tableViewerFav = new CheckboxTableViewer( table );
		
		spi.addViewer(tableViewerFav);
		
		
		tvaFav = new TableViewerAdministrator<IUser>(tableViewerFav,
				Arrays.asList(new FUNick(tableViewerFav),new HubName(),new LastSeen(),new SlotUntil()),
						GUIPI.favUsersTable,TableViewerAdministrator.NoSorting);
		
		tvaFav.apply();
		
		tableViewerFav.setContentProvider(new FavUserProvider(true));
		
		
		Table table2 = new Table(bar, SWT.SINGLE|SWT.FULL_SELECTION |SWT.HIDE_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION );
		table2.setHeaderVisible(true);
		
		tableViewerSlot = new TableViewer( table2 );

		tvaSlot = new TableViewerAdministrator<IUser>(tableViewerSlot,
				Arrays.asList(new Nick(),new HubName(),new LastSeen(),new SlotUntil()),
						GUIPI.favUsersSlotTable,TableViewerAdministrator.NoSorting);
		
		tvaSlot.apply();
		
		tableViewerSlot.setContentProvider(new FavUserProvider(false));
		spi.addViewer(tableViewerSlot);

		
		createContextPopup(HubEditor.ID,tableViewerFav);
		createContextPopup(HubEditor.ID, tableViewerSlot);
		
		
		
		pop.registerUserChangedListener(this);
		tableViewerFav.setInput(pop);
		tableViewerSlot.setInput(pop);
		
		ExpandItem item0 = new ExpandItem (bar, SWT.NONE, 0);
		item0.setText("Fav Users");
		item0.setHeight(Math.max(table.computeSize(SWT.DEFAULT, SWT.DEFAULT).y,250));
		item0.setControl(table);
		item0.setExpanded(true);
		
		ExpandItem item1 = new ExpandItem (bar, SWT.NONE, 1);
		item1.setText("Slots Granted");
		item1.setHeight(Math.max(table2.computeSize(SWT.DEFAULT, SWT.DEFAULT).y,250));
		item1.setControl(table2);
		item1.setExpanded(true);
		
		getSite().setSelectionProvider(spi);
		setControlsForFontAndColour(tableViewerFav.getTable());
	}


	
	public void setFocus() {
		tableViewerFav.getControl().setFocus();
	}



	/**
	 * updates fav users that have changed..
	 */
	public void changed(final UserChangeEvent uce) {
		switch(uce.getDetail()) {
		case UserChangeEvent.FAVUSER_ADDED:
			new SUIJob() {
				@Override
				public void run() {
					if (!tableViewerFav.getTable().isDisposed()) {
						tableViewerFav.add(uce.getChanged());
						if (uce.getChanged().hasCurrentlyAutogrant()) {
							tableViewerSlot.remove(uce.getChanged());
						}
					}
				}
			}.schedule();
			break;
		case UserChangeEvent.SLOT_GRANTED:
			new SUIJob() {
				@Override
				public void run() {
					if (!tableViewerSlot.getTable().isDisposed()) {
						if (uce.getChanged().isFavUser()) {
							tableViewerFav.refresh(uce.getChanged());
						} else {
							tableViewerSlot.add(uce.getChanged());
						}
					}
				}
			}.schedule();
			break;
		case UserChangeEvent.FAVUSER_REMOVED:
			new SUIJob() {
				@Override
				public void run() {
					if (!tableViewerFav.getTable().isDisposed()) {
						tableViewerFav.remove(uce.getChanged());
						if (uce.getChanged().hasCurrentlyAutogrant()) {
							tableViewerSlot.add(uce.getChanged());
						}
					}
				}
			}.schedule();
			break;
		case UserChangeEvent.SLOTGRANT_REVOKED:
			new SUIJob() {
				@Override
				public void run() {
					if (!tableViewerSlot.getTable().isDisposed()) {
						if (uce.getChanged().isFavUser()) {
							tableViewerFav.refresh(uce.getChanged());
						} else {
							tableViewerSlot.remove(uce.getChanged());
						}
					}
				}
			}.schedule();
		break;
		default:
			if (uce.getChanged().isFavUser()) {
				new SUIJob() {
					@Override
					public void run() {
						if (!tableViewerFav.getTable().isDisposed()) {
							tableViewerFav.refresh(uce.getChanged());
						}
					}
				}.schedule();
			} else if (uce.getChanged().hasCurrentlyAutogrant()) {
				new SUIJob() {
					@Override
					public void run() {
						if (!tableViewerSlot.getTable().isDisposed()) {
							tableViewerSlot.refresh(uce.getChanged());
						}
					}
				}.schedule();
			}
		}
		
	}
	

/*
	public void update(Observable o, Object arg) {
		StatusObject so = (StatusObject)arg;
		
		switch(so.getType()) {
		case ADDED:
			tableViewer.add(so.getValue());
			break;
		case REMOVED:
			tableViewer.remove(so.getValue());
			break;
		}
	} */




	





	public void dispose() {
	//	pop.deleteObserver(this);
		pop.unregisterUserChangedListener(this);
//		for (UserActions aua: actions) {
//			aua.dispose();
//		}
		super.dispose();
	}



	public static class FavUserProvider implements IStructuredContentProvider {

		private final boolean fav;
		/**
		 * if true this will provide favusers..
		 * if false users with slotgrants..
		 * @param fav
		 */
		public FavUserProvider(boolean fav) {
			this.fav = fav;
		}
		public void dispose() {}

		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		
		public Object[] getElements(Object inputElement) {
			Population pop = (Population)inputElement;
			if (fav) {
				return pop.getFavUsers().toArray();
			} else {
				HashSet<IUser> u = new HashSet<IUser>(pop.getSlotGranted());
				u.removeAll(pop.getFavUsers());
				return u.toArray();
			}
		}
	}
	
}
