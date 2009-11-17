package eu.jucy.gui.favhub;

import java.util.Comparator;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;


import eu.jucy.gui.Lang;


import uc.FavHub;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class FavHubColumns extends ColumnDescriptor<FavHub> {
	
	private static final Logger logger = LoggerFactory.make();
	

	protected FavHubColumns(int defaultWidth,String columnName) {
		this(defaultWidth,columnName,SWT.LEAD);
	}
	protected FavHubColumns(int defaultWidth,String columnName, int style) {
		super(defaultWidth,columnName,style);
	}

	/**
	 * no comparator either.. as FavHubs sorting has the meaning of priority..
	 */
	@Override
	public Comparator<FavHub> getComparator() {
		return null;
	}

	/**
	 * no image for FavHubs
	 */
	@Override
	public Image getImage(FavHub x) {
		return null;
	}
	
	
	/**
	 * first column .. contains code for handling the Checkbox
	 *
	 */
	public static class FavHubName extends FavHubColumns {
		
		private final CheckboxTableViewer table;
		
		
		public FavHubName(CheckboxTableViewer table) {
			super(120,Lang.AutoConnect+" / "+Lang.Name);
			this.table = table;
			
			table.addCheckStateListener(new ICheckStateListener() {
				
				public void checkStateChanged(CheckStateChangedEvent e) {
					FavHub fh = (FavHub)e.getElement();
					fh.setAutoconnect(e.getChecked());
					logger.debug("checked state changed: "+fh.isAutoconnect());
				}
			});
			
		}

		/**
		 * we don't need a image .. but we use this 
		 * as a hack to set the selection in the table...
		 * ugly ugly..
		 */
		@Override
		public Image getImage(final FavHub hub) {
			new SUIJob() {

				@Override
				public void run() {
					table.setChecked(hub, hub.isAutoconnect());
				}
				
			}.schedule();
			
			logger.debug("checked state set: "+hub.isAutoconnect());
			return null;
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getHubname();
		}
		
	}
	
	
	public static class Description  extends  FavHubColumns {
		
		public Description() {
			super(200,Lang.Description);
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getDescription();
		}
	}
	
	public static class Nick extends FavHubColumns {
		
		public Nick() {
			super(100,Lang.Nick);
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getNick();
		}
	}
	
	public static class Password extends FavHubColumns {
		
		public Password() {
			super(80,Lang.Password);
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getPassword().replaceAll(".", "*");
		}
	}
	
	public static class Address extends FavHubColumns {
		
		public Address() {
			super(130,Lang.Address);
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getHubaddy();
		}
		
	}
	
	public static class UserDescription extends FavHubColumns {
		
		public UserDescription() {
			super(130,Lang.UserDescription);
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getUserDescription();
		}
	}
	
	public static class Email extends FavHubColumns {
		
		public Email() {
			super(100, Lang.EMail);
		}

		@Override
		public String getText(FavHub hub) {
			return hub.getEmail();
		}
		
	}
	
	public static class ChatOnly extends FavHubColumns {
		
		public ChatOnly() {
			super(40,Lang.ChatOnly);
		}

		@Override
		public String getText(FavHub hub) {
			return (hub.isChatOnly()? Lang.Yes: Lang.No);
		}
	}
	
	
	
	
}
