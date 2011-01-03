package eu.jucy.gui.downloadqueue;



import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.Block;
import uc.files.downloadqueue.FileDQE;
import uc.files.transfer.AbstractFileInterval;
import uc.files.transfer.IFileTransfer;
import uihelpers.SUIJob;
import eu.jucy.gui.downloadqueue.DownloadQueueColumns.DQDownloaded;
import eu.jucy.gui.transferview.UCProgressPainter;

public class DQProgressPainter implements Listener {


	
	private static final DQDownloaded col = new DQDownloaded();
	

	
	private final int column;
	
	private DQProgressPainter(int column) {
		super();
		this.column = column;
	
		
	}

	public static void addToTable(final Table table, int column) {
		table.addListener(SWT.PaintItem, new DQProgressPainter(column));
		new SUIJob(table) { 
			@Override
			public void run() {
				table.redraw();
				schedule(500);
			}
		}.schedule(1000);
	}

	public void handleEvent(Event event) {
		if (event.index != column) {
			return;
		}
		TableItem ti = (TableItem)event.item;
		Object o = ti.getData();
	
		if (o instanceof AbstractDownloadQueueEntry) {
			drawADQE((AbstractDownloadQueueEntry)o,ti.getParent(),event.gc,event,null);
		}
			
	}
	
	public static void drawADQE(AbstractDownloadQueueEntry adqe,Control c,GC gc,Event event,IFileTransfer markSpecial) {
		String s;
		if ( adqe instanceof FileDQE) {
			FileDQE fdqe = (FileDQE)adqe;
			s = col.getText(fdqe);
			Device display = gc.getDevice();

			
			Rectangle rect = new Rectangle(event.x,event.y,Math.max(gc.getClipping().width,event.width),event.height);//gc.getClipping();
			int numberOfBlocks = fdqe.getNrOfBlocks();
			for (int i = 0; i < numberOfBlocks;i++) {
				Block b = fdqe.getBlock(i);
				Color col = null;
				boolean minSize = false;
				switch(b.getState()) {
				case EMPTY: 
					col = display.getSystemColor(SWT.COLOR_GRAY); 
					break;
				case FINISHED: 	
					col = UCProgressPainter.downloadColor; 
					break;
				case WRITEINPROGRESS:  
					minSize = true;
					col = display.getSystemColor(SWT.COLOR_RED); 
					break;
				case UNVERIFIED:  
					col = display.getSystemColor(SWT.COLOR_BLUE); 
					break;
				default: throw new IllegalStateException();
				}
				
				int start  = i*rect.width / numberOfBlocks;
				int length =  ((i+1)*rect.width / numberOfBlocks)-start;
				if (length == 0 && minSize) { //minimum one pixel width for some drawn stuff..
					start--;
					length = 1;
				}
				gc.setBackground(col);
				gc.fillRectangle(rect.x+start, rect.y, length, rect.height);
			}
			if (numberOfBlocks == 0) {
				gc.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
				gc.fillRectangle(rect);
			}
			
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
			gc.drawRectangle(rect);
			
			gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.drawString(s,rect.x+5,rect.y+1,true);
		//	gc.setForeground(c.getForeground());
		//	gc.drawRectangle(rect);
			
			gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
			gc.drawRectangle(rect.x,rect.y+1,rect.width, rect.height-3); //accenting Rectangle
			
			
			if (markSpecial != null) {
				AbstractFileInterval afi =  markSpecial.getFileInterval();
				long totalLength = afi.getTotalLength();
				int start =(int)  (afi.getStartpos() * rect.width / totalLength);
				int length =(int) (afi.length() * rect.width / totalLength);
				gc.setForeground(c.getForeground());
				gc.drawRoundRectangle(rect.x+start, rect.y + 1, length, rect.height-2,3,3);
				
			}
			
			//gc.drawRoundRectangle(rect.x,rect.y,rect.width,rect.height,3,3);

			
		} else {
			s = col.getText(adqe);
			UCProgressPainter.drawString(s,c,gc,event);
		}
	}

	
	
	
}
