package eu.jucy.gui.uploadqueue;

import java.util.Comparator;

import logger.LoggerFactory;
import helpers.SizeEnum;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.Lang;
import uc.files.UploadQueue.TransferRecord;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class FinishedTransfersColumn extends ColumnDescriptor<TransferRecord> {

	private static Logger logger = LoggerFactory.make(); 
	
	static {
		logger.setLevel(Level.DEBUG);
	}

	public FinishedTransfersColumn(int defaultColumnSize, String columnName) {
		super(defaultColumnSize, columnName, SWT.LEAD);
	}
	
	

	public FinishedTransfersColumn(int defaultColumnSize, String columnName,int style) {
		super(defaultColumnSize, columnName, style);
	}



	@Override
	public Image getImage(TransferRecord x) {
		return null;
	}
	

	
	public static class NameTransfCol  extends  FinishedTransfersColumn {

		public NameTransfCol() {
			super(300, Lang.FileCol);
		}

		@Override
		public String getText(TransferRecord x) {
			return x.getName();
		}
	}
	
	public static class SizeCol  extends  FinishedTransfersColumn {

		public SizeCol() {
			super(80, Lang.Size,SWT.TRAIL);
		}

		@Override
		public String getText(TransferRecord x) {
			return  SizeEnum.getReadableSize(x.getSize());
		}

		@Override
		public Comparator<TransferRecord> getComparator() {
			return new Comparator<TransferRecord>() {
				public int compare(TransferRecord arg0, TransferRecord arg1) {
					return Long.valueOf(arg0.getSize()).compareTo(arg1.getSize());
				}
			};
		}
		
		
	}
	
	public static class DurationCol  extends  FinishedTransfersColumn {

		public DurationCol() {
			super(60, Lang.Duration);
		}

		@Override
		public String getText(TransferRecord x) {
			return SizeEnum.toDurationString(x.getTimeNeeded()/1000);
		}
		
		@Override
		public Comparator<TransferRecord> getComparator() {
			return new Comparator<TransferRecord>() {
				public int compare(TransferRecord arg0, TransferRecord arg1) {
					return Long.valueOf(arg0.getTimeNeeded()).compareTo(arg1.getTimeNeeded());
				}
			};
		}
	}
	
	public static class SpeedCol  extends  FinishedTransfersColumn {

		public SpeedCol() {
			super(80, Lang.Speed);
		}

		@Override
		public String getText(TransferRecord x) {
			return SizeEnum.toSpeedString(x.getTimeNeeded(), x.getSize());
		}
		
		@Override
		public Comparator<TransferRecord> getComparator() {
			return new Comparator<TransferRecord>() {
				public int compare(TransferRecord arg0, TransferRecord arg1) {
					float speed	= (float)arg0.getSize()  / (float)arg0.getTimeNeeded();
					float speed2= (float)arg1.getSize()  / (float)arg1.getTimeNeeded();
					return Float.valueOf(speed).compareTo(speed2);
				}
				
			};
		}
	}
	
	public static class IPCol  extends  FinishedTransfersColumn {

		public IPCol() {
			super(90, Lang.IP);
		}

		@Override
		public String getText(TransferRecord x) {
			return x.getTargetIP().getHostAddress();
		}
		
	}
	
	public static class StartedCol  extends  FinishedTransfersColumn {

		public StartedCol() {
			super(110, Lang.Started);
		}

		@Override
		public String getText(TransferRecord x) {
			return GuiHelpers.dateToString(x.getStarttime());
		}
		
		@Override
		public Comparator<TransferRecord> getComparator() {
			return new Comparator<TransferRecord>() {
				public int compare(TransferRecord arg0, TransferRecord arg1) {
					return arg0.getStarttime().compareTo(arg1.getStarttime());
				}
				
			};
		}
	}
	
	public static class FinishedCol  extends  FinishedTransfersColumn {

		public FinishedCol() {
			super(110, Lang.Finished);
		}

		@Override
		public String getText(TransferRecord x) {
			return GuiHelpers.dateToString(x.getEndTime());
		}
		
		@Override
		public Comparator<TransferRecord> getComparator() {
			return new Comparator<TransferRecord>() {
				public int compare(TransferRecord arg0, TransferRecord arg1) {
					return arg0.getEndTime().compareTo(arg1.getEndTime());
				}
				
			};
		}
	}
	
	public static class PathTransfCol  extends  FinishedTransfersColumn {

		public PathTransfCol() {
			super(300, Lang.Path);
		}

		@Override
		public String getText(TransferRecord x) {
			if (x.getFile() != null) {
				return x.getFile().getParent();
			}
			return "";
		}
	}
	
	

}
