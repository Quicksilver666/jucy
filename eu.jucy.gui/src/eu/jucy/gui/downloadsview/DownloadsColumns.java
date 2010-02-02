package eu.jucy.gui.downloadsview;

import helpers.SizeEnum;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UserColumns.Nick;
import eu.jucy.gui.texteditor.NickColourerTextModificator;
import eu.jucy.gui.transferview.TransferColumns.TimeLeftColumn;

import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.transfer.AbstractFileInterval;
import uc.files.transfer.IFileTransfer;
import uc.protocols.client.ClientProtocol;
import uihelpers.IconManager;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class DownloadsColumns extends ColumnDescriptor<Object> {

	public DownloadsColumns(int defaultColumnSize, String columnName, int style) {
		super(defaultColumnSize, columnName, style);
	}

	@Override
	public String getText(Object x) {
		if (x instanceof AbstractDownloadQueueEntry) {
			return getText((AbstractDownloadQueueEntry)x);
		} else if (x instanceof ClientProtocol) {
			x = ((ClientProtocol)x).getFileTransfer();
		} 
		if (x instanceof IFileTransfer) {
			return getText((IFileTransfer)x);
		}
		return null;
	}


	protected abstract String getText(AbstractDownloadQueueEntry adqe);
	
	protected abstract String getText(IFileTransfer ft);


	public static class Transferrer extends DownloadsColumns {

		
		public Transferrer() {
			super(250, Lang.FileCol + "/"+Lang.User, SWT.NONE);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			return adqe.getFileName();
		}

		@Override
		protected String getText(IFileTransfer ft) {
			return ft.getOther().getNick();
		}

		@Override
		public Font getFont(Object x) {
			if (x instanceof ClientProtocol && NickColourerTextModificator.isActive()) {
				return NickColourerTextModificator.getFont(((ClientProtocol)x).getUser());
			}
			return null;
		}

		@Override
		public Color getForeground(Object x) {
			if (x instanceof ClientProtocol && NickColourerTextModificator.isActive()) {
				return NickColourerTextModificator.getColor(((ClientProtocol)x).getUser());
			}
			return null;
		}

		@Override
		public Image getImage(Object x) {
			if (x instanceof ClientProtocol) {
				return Nick.getUserImage(((ClientProtocol)x).getUser());
			} else if (x instanceof AbstractDownloadQueueEntry) {
				return IconManager.get().getIconByFilename(
						((AbstractDownloadQueueEntry)x).getFileName() );
			}
			return null;
		}
		
	}
	
	public static class StatusCol extends DownloadsColumns {

		public StatusCol() {
			super(250, Lang.Status, SWT.NONE);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			return "";//GuiHelpers.toPercentString( adqe.getDownloadedBytes(), adqe.getSize())  ;
		}

		@Override
		protected String getText(IFileTransfer ft) {
			//AbstractFileInterval fi = ft.getFileInterval();
			return "";//GuiHelpers.toPercentString(fi.getRelativeCurrentPos(),fi.length());
		}
	}
	
	public static class SpeedCol extends DownloadsColumns {

		public SpeedCol() {
			super(80, Lang.Speed, SWT.TRAIL);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			long totalSpeed = 0;
			for (IFileTransfer ft : adqe.getRunningFileTransfers()) {
				totalSpeed+= ft.getSpeed();
			}
			return SizeEnum.toSpeedString(1000,totalSpeed );
		}

		@Override
		protected String getText(IFileTransfer ft) {
			return ft != null ? SizeEnum.toSpeedString(1000,ft.getSpeed() ): "";
		}
		
		
	}
	
	public static class TimeLeftCol extends DownloadsColumns {
		
		private TimeLeftColumn tlc = new TimeLeftColumn();
		
		public TimeLeftCol() {
			super(80, Lang.TimeLeft, SWT.TRAIL);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			return SizeEnum.timeEstimation( adqe.getTimeRemaining() );
		}

		@Override
		protected String getText(IFileTransfer ft) {
			return tlc.getText(null,ft,null);
		}
	}
	
	public static class TotalTimeLeftCol extends DownloadsColumns {
		
		
		
		public TotalTimeLeftCol() {
			super(100, Lang.TotalTimeLeft, SWT.TRAIL);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			
			//return SizeEnum.timeEstimation( adqe.getTimeRemaining() );
			return "";
		}

		@Override
		protected String getText(IFileTransfer ft) {
			//Total time for all files in queue
			return SizeEnum.timeEstimation( ft.getOther().sizeOfFilesInQueue() , ft.getSpeed());
		}
	}
	
	
	public static class FilesLeftCol extends DownloadsColumns {
		
		
		public FilesLeftCol() {
			super(60, Lang.FilesLeft, SWT.TRAIL);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			return ""+ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue().getTotalNrOfFiles();
		}

		@Override
		protected String getText(IFileTransfer ft) {
			return ""+ft.getOther().nrOfFilesInQueue();
		}
	}
	
	public static class SizeLeftCol extends DownloadsColumns {
		
		
		public SizeLeftCol() {
			super(80, Lang.SizeLeft, SWT.TRAIL);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			return SizeEnum.getReadableSize(adqe.getSize()- adqe.getDownloadedBytes());
		}

		@Override
		protected String getText(IFileTransfer ft) {
			AbstractFileInterval fi =ft.getFileInterval();
			return SizeEnum.getReadableSize(fi.length()-fi.getRelativeCurrentPos());  
		}
	}
	
	public static class TotalSizeLeftCol extends DownloadsColumns {
		
		
		public TotalSizeLeftCol() {
			super(100, Lang.TotalSizeLeft, SWT.TRAIL);
		}

		@Override
		protected String getText(AbstractDownloadQueueEntry adqe) {
			return SizeEnum.getReadableSize(
					ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue().getTotalSize());
		}

		@Override
		protected String getText(IFileTransfer ft) {
			return SizeEnum.getReadableSize(ft.getOther().sizeOfFilesInQueue());
		}
	}
	
}
