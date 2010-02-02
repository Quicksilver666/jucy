package eu.jucy.gui.transferview;



import helpers.GH;
import helpers.PreferenceChangedAdapter;
import helpers.SizeEnum;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import uc.files.transfer.AbstractFileInterval;
import uc.files.transfer.IFileTransfer;
import uc.protocols.ConnectionState;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine;
import uc.protocols.client.ClientProtocolStateMachine.CPSMState;
import uihelpers.SUIJob;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.GuiHelpers;

public class UCProgressPainter implements Listener {

	
	public static Color downloadColor;
	public static Color downloadColor2;
	private static Color uploadColor;
	private static Color uploadColor2;
	
	private static Color backgroundColor; 

	private final Table table; // used for back and foreground colours
	
	static {
		new PreferenceChangedAdapter(GUIPI.get(),GUIPI.downloadColor1,
				GUIPI.downloadColor2,GUIPI.uploadColor1,GUIPI.uploadColor2) {
			
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				refreshColors();
			}
		};
		refreshColors();
	}
	
	private static void refreshColors() {
		downloadColor 	= GUIPI.getColor(GUIPI.downloadColor1);
		downloadColor2 	= GUIPI.getColor(GUIPI.downloadColor2);
		uploadColor 	= GUIPI.getColor(GUIPI.uploadColor1);
		uploadColor2 	= GUIPI.getColor(GUIPI.uploadColor2);
		if (backgroundColor == null) {
			backgroundColor	= new Color(null,210,210,210);
		}
	}
	

	public static void addToTable(final Table table) {
		table.addListener(SWT.PaintItem, new UCProgressPainter(table));
		new SUIJob(table) { 
			@Override
			public void run() {
				if (table.getItemCount() != 0) {
					table.redraw();
				}
				schedule(500);
			}
		}.schedule(1000);
	}
	
	private UCProgressPainter(Table table) {
		this.table = table;
	}

	public static void drawString(String message,Control c, GC gc,Event event) {
		Color oldbackground = gc.getBackground();
		Color oldforeground = gc.getForeground();
		
		Rectangle rect = new Rectangle(event.x,event.y,Math.max(gc.getClipping().width,event.width),event.height);//gc.getClipping();
		gc.setBackground(c.getBackground());
		gc.setForeground(c.getForeground()); 
		gc.fillRectangle(rect);
		
		int pos = (rect.height - gc.getFontMetrics().getHeight()) /2;
		gc.drawString(message,rect.x+5, rect.y+pos,true);
		
		
		gc.setForeground(oldforeground);
		gc.setBackground(oldbackground);
		
	}
	
	public void handleEvent(Event event) {
		if (event.index != 2) {
			return;
		}
		TableItem ti = (TableItem)event.item;
		Object o = ti.getData();
		
		if (o instanceof ClientProtocol) {
			ClientProtocol cp= (ClientProtocol)o;
//			if (event.height != event.gc.getClipping().height && event.gc.getClipping().height != 0) {
//				System.out.println("heigth: "+event.height+" "+event.gc.getClipping().height);
//				System.out.println("width: "+event.width+" "+event.gc.getClipping().width);
//				System.out.println("x: "+event.x+" "+event.gc.getClipping().x);
//				System.out.println("y: "+event.y+" "+event.gc.getClipping().y);
//				System.out.println(cp.getUser().getNick());
//			}
			switch (cp.getState()) {
			case TRANSFERSTARTED:
				IFileTransfer ft = cp.getFileTransfer();
				if (ft != null) {
					drawFileTransfer(ft,event.gc,event);
				}
				break;
			case DESTROYED:
			case CLOSED:
				String reason = cp.getDisconnectReason();
				drawString(reason != null? reason : "idle" ,table, event.gc,event);
				break;
				
			case TRANSFERFINISHED:
				drawString(ConnectionState.TRANSFERFINISHED.toString(),table,event.gc,event);
				break;
			case LOGGEDIN: //LoggedIn is equivalent to connected in this context..
				drawString(ConnectionState.CONNECTED.toString(),table,event.gc,event);
				break;
			default:
				drawString(cp.getState().toString(),table,event.gc,event);
				break;
			}
		} else if (o instanceof ClientProtocolStateMachine) {
			ClientProtocolStateMachine cpsm =(ClientProtocolStateMachine)o; 
			String s;
			if (cpsm.getState() == CPSMState.CLOSED) {
				s = cpsm.getCurrent().getDisconnectReason();
			} else {
				s = cpsm.getState().toString();
			}
			if (s == null) {
				s = "";
			}
			if (!GH.isEmpty(s)) {
				s += " ("+cpsm.getSleepCounter()+")";
			}
			
			drawString(s,table,event.gc,event);
			
		}
		
	}
	
	public static void drawFileTransfer(IFileTransfer ft,GC gc,Event event) {
		
		Rectangle rect =  new Rectangle(event.x,event.y,Math.max(gc.getClipping().width,event.width),event.height); //  gc.getClipping();
	
		if (rect.height == 0  && rect.width == 0) {
			return;
		}
		boolean downloading = !ft.isUpload();
		AbstractFileInterval fi = ft.getFileInterval();
	//	long totalLength = ft.getFileInterval().getTotalLength();
		long length;//	= ft.getFileInterval().getShownLength(); //determine the length
		long current;
		if (downloading) {
			length = fi.length();
			current = fi.getRelativeCurrentPos();
		} else {
			length = fi.getTotalLength();
			current = fi.getCurrentPosition();
		}
		
		if (length <= 0) {
			length = 1;
		}

		int divider =(int)((current*rect.width) / length);
		
//		Color oldforeground = gc.getForeground();
//		Color oldbackground = gc.getBackground();
			
		//"%sDownloaded %s (%s) in %s"
		String d = "%sDownloaded %s (%s) in %s",u = "%sUploaded %s (%s) in %s";
		
		String downloadString = String.format(downloading?d:u, 
				ft.getCompression().toTransferViewString(),
				SizeEnum.getReadableSize(current),
				GuiHelpers.toPercentString(current, length),
				SizeEnum.toDurationString((System.currentTimeMillis()-ft.getStartTime().getTime())/1000 ));
			
		
		/*	ft.getCompression().toTransferViewString()
				+(downloading ? "Downloaded " : "Uploaded ")
				+ SizeEnum.getReadableSize(current)
				+" ("+GuiHelpers.toPercentString(current, length)+")"
				+" in "+
				SizeEnum.toDurationString((System.currentTimeMillis()-ft.getStartTime().getTime())/1000 ); */
			
		//draw the background
		gc.setBackground(backgroundColor);
		gc.fillRectangle(rect);
		
			
		//draw the downloadString on the background..
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(rect);
		
		int pos = (rect.height - gc.getFontMetrics().getHeight()) /2;
		gc.drawString(downloadString,rect.x+5,rect.y+pos,true);
			
		//draw rectangle with color for transferred bytes.. 
		gc.setBackground(downloading ? downloadColor2 :uploadColor2 );
		gc.setForeground(downloading ? downloadColor :uploadColor);
		//set Clipping so that we don't draw to much of the font
		Rectangle old = gc.getClipping();
		gc.setClipping( old.x,  old.y, divider ,old.height);
			

		gc.fillGradientRectangle(rect.x,  rect.y, rect.width, rect.height, true);
		
		gc.setForeground(backgroundColor);
//		if ( ft.getNameOfTransferred().contains("119")) {
//			System.out.println(rect+" : "+ ft.getOther().getNick());
//		}
		gc.drawRectangle(rect.x+1,rect.y+1,rect.width-1, rect.height-3); //accenting Rectangle
		

		//then draw the String in white in the clipping region set to the colored part of the bar
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.drawString(downloadString,rect.x+5,rect.y+1,true);
			
	//	gc.setForeground(oldforeground);
	//	gc.setBackground(oldbackground);
		
	}

}
