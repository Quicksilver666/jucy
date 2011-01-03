package eu.jucy.connectiondebugger;

import helpers.GH;
import helpers.SizeEnum;

import java.text.SimpleDateFormat;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import eu.jucy.connectiondebugger.ConnectionDebugger.CommandStat;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.transferview.TransferColumns.UserColumn;


import uihelpers.TableViewerAdministrator.ColumnDescriptor;
import uihelpers.TableViewerAdministrator.NumberColumnDescriptor;

public abstract class SentCommandColumns extends ColumnDescriptor<SentCommand> {

	protected SentCommandColumns(int defaultColumnSize, String columnName) {
		super(defaultColumnSize, columnName);
	}
	
	public static class DateCol extends SentCommandColumns {
		
		public static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
		
		protected DateCol() {
			super(120, "Date");
		}

		@Override
		public String getText(SentCommand x) {
			return SDF.format(x.getTimeReceived());
		}

		@Override
		public Comparator<SentCommand> getComparator() {
			return new Comparator<SentCommand>() {
				public int compare(SentCommand o1, SentCommand o2) {
					return GH.compareTo(o1.getNanosReceived(),o2.getNanosReceived());
				}
			};
		}

		@Override
		public Image getImage(SentCommand x) {
			Boolean inc = x.isIncoming();
			if (inc == null) {
				return null;
			} else {
				return inc?UserColumn.DOWNLOAD_ICON:UserColumn.UPLOAD_ICON;
			}
		}

	}

	public static class CommandCol extends SentCommandColumns {
		protected CommandCol() {
			super(600, "Command");
		}

		@Override
		public String getText(SentCommand x) {
			return x.getCommand();
		}

		@Override
		public Color getBackground(SentCommand x) {
			if (x instanceof ReceivedCommand) {
				ReceivedCommand y = (ReceivedCommand)x;
				if (y.getCommandHandler().equals( ReceivedCommand.UNKNOWN)) {
					return Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
				}
				if (!y.isWellFormed()) {
					return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
				}
			}
			return null;
		}
		
		
	}
	
	public static class CommandName extends ColumnDescriptor<CommandStat> {

		public CommandName() {
			super(100, "Command");
		}

		@Override
		public String getText(CommandStat x) {
			return x.getCommandName();
		}	
	}
	public static class Frequency extends NumberColumnDescriptor<CommandStat> {
		public Frequency() {
			super(100,"Frequency");
		}
		@Override
		public long getNumber(CommandStat x) {
			return x.getFrequency();
		}
		
		@Override
		public String getTextFromNumber(long num) {
			return Long.toString(num);
			
		}
	}
	public static class TrafficTotal extends NumberColumnDescriptor<CommandStat> {
		private final ConnectionDebugger cd;
		public TrafficTotal(ConnectionDebugger cs) {
			super(150,"Traffic");
			cd = cs;
		}

		@Override
		public long getNumber(CommandStat x) {
			return x.getTrafficTotal();
		}

		@Override
		public String getTextFromNumber(long num) {
			return SizeEnum.getReadableSize(num)+"/"+GuiHelpers.toPercentString(num,cd.getTrafficTotal()); 
		}
	}
	
	public static class LastCommand extends ColumnDescriptor<CommandStat> {
		
		public LastCommand() {
			super(800,"Last");
		}

		@Override
		public String getText(CommandStat x) {
			return x.getLastCommand();
		}

	}
	
}
