package eu.jucy.gui.downloadqueue;

import helpers.GH;
import helpers.SizeEnum;

import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;


import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.Lang;
import eu.jucy.gui.Priority;
import eu.jucy.language.LanguageKeys;


import uc.IUser;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uihelpers.IconManager;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class DownloadQueueColumns extends ColumnDescriptor<AbstractDownloadQueueEntry> {

	protected DownloadQueueColumns(int defaultColumnSize, String columnName,int style) {
		super(defaultColumnSize, columnName, style);
	}

	@Override
	public Image getImage(AbstractDownloadQueueEntry x) {
		return null;
	}

	
	public static class DQFile extends DownloadQueueColumns {

		public DQFile() {
			super(150, Lang.FileCol, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry x) {
			return x.getFileName();
		}

		@Override
		public Image getImage(AbstractDownloadQueueEntry element) {
			String filename = ((AbstractDownloadQueueEntry)element).getFileName();
			return IconManager.get().getIconByFilename(filename);
		}
		
		
	}
	
	
	public static class DQStatus extends DownloadQueueColumns {

		public DQStatus() {
			super(80, Lang.Status, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			boolean downloadrunning = dqe.getNrOfRunningDownloads() > 0; // .isDownloading();
			int uploadersOnline = 0;
			for (IUser u: dqe.getUsers()) {
				if (u.isOnline()) {
					uploadersOnline++;
				}
			}
			
			int totalUploaders = dqe.getUsers().size();
		
			if (totalUploaders == 0) {
				return Lang.NoUsers;
			} else if (downloadrunning) {
				return Lang.Downloading+"...";
			} else if (totalUploaders == 1) {
				if (uploadersOnline == 0) {
					return LanguageKeys.UserOffline;
				} else {
					return Lang.UserOnline;
				}
			} else if(uploadersOnline == 0 ) {
				return String.format(Lang.AllXUsersOffline,totalUploaders);
			} else {
				return String.format(Lang.XOfYUsersOnline,uploadersOnline, totalUploaders);  
			}
		}
	}
	
	public static class DQSize extends DownloadQueueColumns {

		public DQSize() {
			super(80, Lang.Size, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			return SizeEnum.getReadableSize( dqe.getSize());
		}

		@Override
		public Comparator<AbstractDownloadQueueEntry> getComparator() {
			return new Comparator<AbstractDownloadQueueEntry>() {

				public int compare(AbstractDownloadQueueEntry o1,
						AbstractDownloadQueueEntry o2) {
					return Long.valueOf(o1.getSize()).compareTo(o2.getSize());
				}
				
			};
		}
	}
	
	public static class DQDownloaded extends DownloadQueueColumns {

		public DQDownloaded() {
			super(150, Lang.Downloaded, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			long downloaded= dqe.getDownloadedBytes();
			return SizeEnum.getReadableSize(downloaded)+"("+GuiHelpers.toPercentString(downloaded, dqe.getSize()) +")";
		}
	}
	
	public static class DQPriority  extends DownloadQueueColumns {

		public DQPriority() {
			super(40, Lang.Priority, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			int priority = dqe.getPriority();
			return Priority.getPriority(priority).toString()+" ("+priority+")";
		}
	}
	
	public static class DQUsers extends DownloadQueueColumns {

		public DQUsers() {
			super(50, Lang.Users, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			String ret="";
			for (IUser usr: dqe.getUsers()) {
				ret+= ";"+usr.getNick();
			}
			if (GH.isEmpty(ret)) {
				return "";
			} else {
				return ret.substring(1);
			}
		}
		
	}
	
	public static class DQPath extends DownloadQueueColumns {

		public DQPath() {
			super(80, Lang.Path, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			return dqe.getFolder().toString();
		}
		
	}
	
	public static class DQExactSize extends DownloadQueueColumns {

		public DQExactSize() {
			super(80, Lang.ExactSize, SWT.TRAIL);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			return SizeEnum.getExactSharesize(dqe.getSize());
		}

		@Override
		public Comparator<AbstractDownloadQueueEntry> getComparator() {
			return new Comparator<AbstractDownloadQueueEntry>() {
				public int compare(AbstractDownloadQueueEntry o1,
						AbstractDownloadQueueEntry o2) {
					return Long.valueOf(o1.getSize()).compareTo(o2.getSize());
				}
				
			};
		}
		
	}
	
	public static class DQErrors extends DownloadQueueColumns {

		public DQErrors() {
			super(40, Lang.Errors, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			return ""; //TODO showing errors for dqe in the gui
		}
	}
	
	
	public static class DQAdded extends DownloadQueueColumns {

		public DQAdded() {
			super(60, Lang.Added, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			return GuiHelpers.dateToString( dqe.getAdded() );
		}	
	}
	
	public static class DQTTHRoot extends DownloadQueueColumns {

		public DQTTHRoot() {
			super(120, Lang.TTHRoot, SWT.LEAD);
		}

		@Override
		public String getText(AbstractDownloadQueueEntry dqe) {
			return dqe.getID().toString();
		}
	}
	
	

}
