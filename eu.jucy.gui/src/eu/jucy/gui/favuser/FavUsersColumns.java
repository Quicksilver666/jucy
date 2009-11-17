package eu.jucy.gui.favuser;


import java.util.Comparator;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.graphics.Image;


import eu.jucy.gui.Lang;
import uc.IUser;
import uc.User;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class FavUsersColumns extends ColumnDescriptor<IUser> {

	private static Logger logger = LoggerFactory.make();
	
	
	protected FavUsersColumns(int defaultColumnSize, String columnName, int style) {
		super(defaultColumnSize, columnName, style);
	}

	protected FavUsersColumns(int defaultColumnSize, String columnName) {
		super(defaultColumnSize, columnName);
	}

	
	@Override
	public Comparator<IUser> getComparator() {
		return new Comparator<IUser>() {
			public int compare(IUser o1, IUser o2) {
				int i = Boolean.valueOf(o1.isFavUser()).compareTo(o2.isFavUser());
				if (i != 0) {
					return -i;
				} else {
					return getText(o1).compareTo(getText(o2));
				}
			}
		};
	}

	@Override
	public Image getImage(IUser usr) {
		return null;
	}

	
	public static class FUNick extends FavUsersColumns {
		private final CheckboxTableViewer table;
		
		public FUNick(CheckboxTableViewer tab) {
			super(100,Lang.AutoGrantSlot
					+" / "+Lang.Nick );
			
			this.table = tab;
			table.addCheckStateListener(new ICheckStateListener() {
				
				public void checkStateChanged(CheckStateChangedEvent e) {
					User usr = (User)e.getElement();
					logger.debug("changed AutograntSlot: "+usr.getNick());
					usr.setAutoGrantSlot(e.getChecked());
					table.refresh(usr);
				}
			});
		}

		@Override
		public String getText(IUser usr) {
			return usr.getNick();
		}

		@Override
		public Image getImage(final IUser usr) {
			new SUIJob() {

				@Override
				public void run() {
					table.setChecked(usr, usr.isAutograntSlot());
				}	
			}.schedule();
			
			return null;
		}
		
	}
	
	

	


}
