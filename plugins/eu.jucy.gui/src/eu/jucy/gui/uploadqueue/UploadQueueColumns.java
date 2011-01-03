package eu.jucy.gui.uploadqueue;

import helpers.SizeEnum;

import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.Lang;

import uc.ISlotManager;
import uc.files.UploadQueue.UploadInfo;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class UploadQueueColumns extends ColumnDescriptor<UploadInfo> {

	


	public UploadQueueColumns(int defaultColumnSize, String columnName) {
		super(defaultColumnSize, columnName);
	}
	
	

	public UploadQueueColumns(int defaultColumnSize, String columnName,	int style) {
		super(defaultColumnSize, columnName, style);
	}



	@Override
	public Image getImage(UploadInfo x) {
		return null;
	}

	
	
	
	public static class NameRequestedCol extends UploadQueueColumns {

		public NameRequestedCol() {
			super(250, Lang.FileCol);
		}

		@Override
		public String getText(UploadInfo x) {
			return x.getRequested();
		}	
	}
	
	
	
	public static class TotalSizeCol extends UploadQueueColumns {

		public TotalSizeCol() {
			super(70, Lang.TotalDownloaded,SWT.TRAIL);
		}

		@Override
		public String getText(UploadInfo x) {
			return SizeEnum.getReadableSize(x.getUploadedTotal());
		}	
		
		@Override
		public Comparator<UploadInfo> getComparator() {
			return new Comparator<UploadInfo>() {
				public int compare(UploadInfo arg0, UploadInfo arg1) {
					return Long.valueOf(arg0.getUploadedTotal()).compareTo(arg1.getUploadedTotal());
				}
			};
		}
	}
	
	public static class FirstRequestCol  extends UploadQueueColumns {

		public FirstRequestCol() {
			super(110, Lang.FirstRequest);
		}

		@Override
		public String getText(UploadInfo x) {
			return GuiHelpers.dateToString(x.getFirstrequest());
		}
		
		@Override
		public Comparator<UploadInfo> getComparator() {
			return new Comparator<UploadInfo>() {
				public int compare(UploadInfo arg0, UploadInfo arg1) {
					return arg0.getFirstrequest().compareTo(arg1.getFirstrequest());
				}
			};
		}
	}
	
	public static class RequestsReceivedCol  extends UploadQueueColumns {

		public RequestsReceivedCol() {
			super(80, Lang.RequestsReceived,SWT.TRAIL);
		}

		@Override
		public String getText(UploadInfo x) {
			return String.valueOf(x.getNumberOfRequestsSinceThen());
		}
		
		@Override
		public Comparator<UploadInfo> getComparator() {
			return new Comparator<UploadInfo>() {
				public int compare(UploadInfo arg0, UploadInfo arg1) {
					return Integer.valueOf(arg0.getNumberOfRequestsSinceThen())
							.compareTo(arg1.getNumberOfRequestsSinceThen());
				}
			};
		}
	}
	
	
	public static class LastRequestCol  extends UploadQueueColumns {

		public LastRequestCol() {
			super(110, Lang.LastRequest);
		}

		@Override
		public String getText(UploadInfo x) {
			return GuiHelpers.dateToString(x.getLastRequest());
		}
		
		@Override
		public Comparator<UploadInfo> getComparator() {
			return new Comparator<UploadInfo>() {
				public int compare(UploadInfo arg0, UploadInfo arg1) {
					return arg0.getLastRequest().compareTo(arg1.getLastRequest());
				}
			};
		}
	}
	
	public static class SlotReceivedCol  extends UploadQueueColumns {

		public SlotReceivedCol() {
			super(50, Lang.SlotReceived,SWT.TRAIL);
		}

		@Override
		public String getText(UploadInfo x) {
			return x.isSlot()? Lang.Yes:Lang.No;
		}
	}
	
	public static class PositionCol  extends UploadQueueColumns {

		private final ISlotManager slotmanager;
		public PositionCol() {
			super(50, "Position",SWT.TRAIL);
			slotmanager = ApplicationWorkbenchWindowAdvisor.get().getSlotManager();
		}

		@Override
		public String getText(UploadInfo x) {
			return ""+slotmanager.getPositionInQueue(x.getUser());
		}

		@Override
		public Comparator<UploadInfo> getComparator() {
			return new Comparator<UploadInfo>() {
				public int compare(UploadInfo o1, UploadInfo o2) {
					int a = slotmanager.getPositionInQueue(o1.getUser());
					int b = slotmanager.getPositionInQueue(o2.getUser());
					return Integer.valueOf(a).compareTo(b);
				}
				
			};
		}
		
		
	}
	
	
	
	
	
}
