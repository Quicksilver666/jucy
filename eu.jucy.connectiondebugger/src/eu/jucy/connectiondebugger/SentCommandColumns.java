package eu.jucy.connectiondebugger;

import java.text.SimpleDateFormat;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class SentCommandColumns extends ColumnDescriptor<SentCommand> {

	protected SentCommandColumns(int defaultColumnSize, String columnName) {
		super(defaultColumnSize, columnName);
	}
	
	public static class DateCol extends SentCommandColumns {
		
		private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		protected DateCol() {
			super(120, "Date");
		}

		@Override
		public String getText(SentCommand x) {
			return sdf.format(x.getTimeReceived());
		}

		@Override
		public Comparator<SentCommand> getComparator() {
			return new Comparator<SentCommand>() {
				public int compare(SentCommand o1, SentCommand o2) {
					return o1.getTimeReceived().compareTo(o2.getTimeReceived());
				}
			};
		}

		@Override
		public Color getBackground(SentCommand x) {
			return Display.getCurrent().getSystemColor(x.isIncoming()?SWT.COLOR_DARK_BLUE:SWT.COLOR_DARK_YELLOW);
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
			if (x instanceof ReceivedCommand && !((ReceivedCommand)x).isWellFormed()) {
				return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
			}
			return null;
		}
		
		
	}
	
	
	
	
}
