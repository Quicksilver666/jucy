package eu.jucy.gui.texteditor;

import helpers.GH;
import helpers.PreferenceChangedAdapter;




import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;


import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PaintObjectEvent;
import org.eclipse.swt.custom.PaintObjectListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import eu.jucy.gui.GUIPI;




import uc.IHub;
import uc.IUser;
import uc.database.DBLogger;
import uc.database.ILogEntry;
import uihelpers.SUIJob;




/**
 * viewer that will help handling a styled text
 * @author Quicksilver
 *
 */
public class StyledTextViewer {
	
	private static final Logger logger = LoggerFactory.make();
	
	
	public static final int HISTORY = 150;
	private final StyledText text;
	private final IHub hub;
	
	private final List<Message> messages = new LinkedList<Message>();
	

	
	private final List<ITextModificator> modificators = new CopyOnWriteArrayList<ITextModificator>();
	
	private PreferenceChangedAdapter pfca;
	
	private final boolean pm;
	private final IUser usr;
	

	private final List<ObjectPoint<Image>> imagePoints = new ArrayList<ObjectPoint<Image>>();
	private final List<ObjectPoint<Control>> controlPoints = new ArrayList<ObjectPoint<Control>>();
	
	private final SortedMap<Integer,ObjectPoint<? extends Object>> allPointsByX = new TreeMap<Integer,ObjectPoint<? extends Object>>();
	
	public StyledTextViewer(StyledText styledtext,IHub hub, boolean pm,IUser usr) {
		this(styledtext,hub,pm,usr,Long.MAX_VALUE);
	}
	
	
	public StyledTextViewer(StyledText styledtext,IHub hub, boolean pm,IUser usr,long loadbeforeTime) {
		this.pm = pm;
		this.usr = usr;
		Assert.isTrue(pm ^ usr == null);
		this.text = styledtext;
		this.hub = hub;
		text.addModifyListener(new ModifyListener() {//moves the text downwards
			public void modifyText(final ModifyEvent e) {
				new SUIJob(text) {
					@Override
					public void run() {
						if (!text.isFocusControl()) {
							text.setSelection(text.getCharCount());
							text.redraw();
						}
					}
				}.schedule();
			}
		});

		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				int start = e.start;
				int replaceCharCount = e.end - e.start;
				int newCharCount = e.text.length();

				//start removal
				SortedMap<Integer,ObjectPoint<? extends Object>> toRemoveM = allPointsByX.subMap(Integer.valueOf(e.start), Integer.valueOf(e.end));
				for (Iterator<Map.Entry<Integer,ObjectPoint<? extends Object>>> it = toRemoveM.entrySet().iterator(); it.hasNext();) {
					Map.Entry<Integer,ObjectPoint<?>> entry = it.next();
					ObjectPoint<?> op = entry.getValue();
					if (op.obj instanceof Control) {
						((Control)op.obj).dispose();
						controlPoints.remove(op);
					} else {
						imagePoints.remove(op);
					}
					it.remove();
				}
				//start modification
				Map<Integer,ObjectPoint<? extends Object>> toAdjust = new HashMap<Integer,ObjectPoint<? extends Object>>(allPointsByX.tailMap(Integer.valueOf(start)));
				for (ObjectPoint<? extends Object> point:toAdjust.values()) {
					Integer oldKey = point.x;
					point.x += newCharCount - replaceCharCount;
					if (allPointsByX.get(oldKey) == point) {
						allPointsByX.remove(oldKey);
					}
					
					allPointsByX.put(Integer.valueOf(point.x), point);
				}
			}
		});
	    text.addPaintObjectListener(new PaintObjectListener() {
	    	public void paintObject(PaintObjectEvent event) {
	    		GC gc = event.gc;
	    		StyleRange style = event.style;
	    		int start = style.start;
	    	//	logger.debug("start: "+start);
	    		ObjectPoint<?> p = allPointsByX.get(Integer.valueOf(start));
	    		
	    		//for (ObjectPoint<Image> p: imagePoints) {
	    		//	logger.debug("p.x : "+p.x+ "  "+p.obj.getClass().getSimpleName());
	    		if (p != null) {
	  //  			logger.debug("asc:"+event.ascent+" bind:"+event.bulletIndex+" dsc:"+event.descent+
	   // 					"  x:"+event.x+" y:"+event.y+" class:"+p.obj.getClass().getSimpleName());
	    			if (p.obj instanceof Image ) {
	    				Image image = (Image)p.obj;
	    				int x = event.x;
	    				int y = event.y + event.ascent - style.metrics.ascent;
	    				gc.drawImage(image, x, y);
	    			} else if (p.obj instanceof Control) {
	    				Control c = ((Control)p.obj);
	    	            int x = event.x; // + ObjectPoint.MARGIN;
	    	            int y = event.y + event.ascent - style.metrics.ascent;
	    	            if (x >= 0 && y >= 0) {
	    	            	c.setLocation(x, y);
	    	            	if (!c.isVisible()) {
	    	            		c.setVisible(true); 
	    	            	}
	    	            } else {
	    	            	c.setLocation(0, 0);
	    	            	if (c.isVisible()) {
	    	            		c.setVisible(false);
	    	            	}
	    	            }
	    			}
	    			
	    		}
	    
	    	}
	    });
	    text.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				try {
					int beginningPos = text.getOffsetAtLocation(new Point(0,0));
					int bottomLine = text.getLineIndex(text.getClientArea().height);
					int endPos= text.getOffsetAtLine(bottomLine)+text.getLine(bottomLine).length();
		
					for (ObjectPoint<Control> p: controlPoints) {
						boolean newState =  beginningPos <= p.x && p.x <= endPos;
						if (newState != p.obj.isVisible()) {
							p.obj.setVisible(newState); 
						}
					}
				} catch (IllegalArgumentException iae) {
					logger.info("iea: "+iae);
				}

			}
	    });
	    
	    
	    text.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
	    });
	    
		refreshSettings();
		loadOldMessages(loadbeforeTime);
		
	}
	
	private void loadOldMessages(long loadBefore) {
		if (pm) {
			DBLogger entity = new DBLogger(usr);
			List<ILogEntry> logentry =  entity.loadLogEntrys(HISTORY, 0);
			//we have a problem here... if this was just opened by a message.. the message is already logged..
			//so we will receive that  message as old message.. -> remove last old message if it is way to new..
			if (!logentry.isEmpty() && System.currentTimeMillis()-logentry.get(0).getDate() < 1000 ) {
				logentry.remove(0);
			}
			

		//	Collections.reverse(logentry);
			String s = "";
			int totalLines = 0;
			for (ILogEntry le: logentry) {
				if (le.getDate() < loadBefore && totalLines < HISTORY) {
					OldMessage om = new OldMessage(le.getMessage(),new Date(le.getDate()));
					messages.add(om);
					String mes =  om.toString();
					totalLines += GH.getOccurences(mes, '\n'); 
					s = mes+s;
				}
			}
		

			text.setText(s); 
			//set all grey..	
			StyleRange sr = new StyleRange();
			sr.start = 0;
			sr.length = s.length();
			sr.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
			text.setStyleRange(sr);
			
		}
	}
	
	ObjectPoint<Image> addImage(Image image,int offset,int length) {
		List<StyleRange> ranges = new ArrayList<StyleRange>();
		ObjectPoint<Image> ip = ObjectPoint.create(offset, length, image, ranges);
		
		if (allPointsByX.remove(Integer.valueOf(offset)) != null) {
			imagePoints.remove(ip);
		}
		imagePoints.add(ip);
		allPointsByX.put(Integer.valueOf(offset), ip);
		for (StyleRange range:ranges) {
			text.setStyleRange(range);
		}
		return ip;
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	ObjectPoint<Control> addControl(Control control, int offset,float ascentPerc) {
		List<StyleRange> ranges = new ArrayList<StyleRange>();
		ObjectPoint<Control> op = ObjectPoint.create(offset, ascentPerc, control, ranges);
		
		ObjectPoint<Control> old = (ObjectPoint<Control>)allPointsByX.remove(Integer.valueOf(offset));
		if (old != null) {
			controlPoints.remove(old);
			old.obj.dispose();
		}
		
		controlPoints.add(op);
		allPointsByX.put(Integer.valueOf(op.x), op);
		
		for (StyleRange range:ranges) {
			text.setStyleRange(range);
		}
		return op;
	 }

	
	
	private void refreshSettings() {
		unloadModificators();
		loadModificators();
		
		for (ITextModificator mod: modificators) {
			mod.init(text,this ,hub);
		}
	}
	
	private void unloadModificators() {
		for (ITextModificator textmod: modificators) {
			textmod.dispose();
		}
		if (pfca != null) {
			pfca.dispose();
		}
		modificators.clear();
	}
	
	private void loadModificators() {
    	IExtensionRegistry reg = Platform.getExtensionRegistry();
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(ITextModificator.ExtensionpointID);

		List<String> allIds = new ArrayList<String>();
		
		for (IConfigurationElement element : configElements) {
			String id = element.getAttribute("id");
			logger.debug("loading: "+id);
			String fullid = GUIPI.IDForTextModificatorEnablement(id);
			allIds.add(fullid);
			if (GUIPI.getBoolean(fullid)) {
				try {
					modificators.add( (ITextModificator)element.createExecutableExtension("class"));
				} catch (CoreException e) {
					logger.error("Can't load TextModificator "+id,e);
				} 
			}
		}
		
		pfca = new PreferenceChangedAdapter(GUIPI.get(),allIds.toArray(new String[]{})) {

			@Override
			public void preferenceChanged(String preference,String oldValue, String newValue) {
				new SUIJob() {
					@Override
					public void run() {

						refreshSettings();
						refresh();
					}
				}.scheduleIfNotRunning(500,this);
			}
		}; 
	}

	
	/**
	 * clears the Text and deletes all messages
	 */
	public void clear() {
		text.setText("");
		messages.clear();
	}
	
	/**
	 * adds message to the end of the StyledText
	 * @param message  What is to be added
	 * @param received  The time the message was received
	 * @param usr A user that can be associated with the message
	 */
	public void addMessage(String message,IUser usr,Date received) {
		Message m = new Message(message,usr,received);
		messages.add(m);
		if (messages.size() > HISTORY) {
			messages.remove(0);
		}
		appendMessage(m);
		
	}
	
	private void appendMessage(Message m) {
		if (!text.isDisposed()) {
			int start = text.getCharCount();
			String message = m.toString();
			if (!GH.isEmpty(message)) {
				text.append(message);
				m.setStyleRanges(start, message);
			}
		}
	}
	
	/**
	 * refreshes the styled text
	 * used when timeStamps change
	 */
	public void refresh() {
		text.setText("");
		for (Message m: messages) {
			appendMessage(m);
		}
	}
	
	public void dispose() {
		unloadModificators();
	}

	
	
	public class Message {


		protected final String message;
		private final IUser usr;
		private final Date received;
		
		private Message(String message,IUser usr,Date received) {
			this.message = message;
			this.usr = usr;
			this.received = received;
		}
		private Message(String message,Date received) {
			this.message = message;
			this.usr = null;
			this.received = received;
		}
		
		
		public String toString() {
			String message = this.message;
			for (ITextModificator mod: modificators) {
				message = mod.modifyMessage(message, this, pm);
				if (message == null) {
					break;
				}
			}
			if (message == null) {
				return ""; //ignore message..
			}
			
			return "\n"+message.replace("\n<", "\n- <").replace("\n[", "\n- [");  //replace helps showing the difference with pasted text..
		}
		
		/**
		 * currently only checks for URLs may be more later..
		 * @param start - start of the message in the text
		 * @return all Styled texts that should be applied
		 */
		protected void setStyleRanges(int start,String renderedMessage) {
			//String s	= renderedMessage; //the result of toString();
			
			List<StyleRange> ranges = new ArrayList<StyleRange>();
			List<ObjectPoint<Image>> images = new ArrayList<ObjectPoint<Image>>();
			for (ITextModificator mod:modificators) {
				mod.getStyleRange(renderedMessage, start, this,ranges,images);
			}
			
			imagePoints.addAll(images);
			for (ObjectPoint<Image> op:images) {
				allPointsByX.put(Integer.valueOf(op.x), op);
			}
			for (StyleRange range:ranges) {
				text.setStyleRange(range);
			}
		}
		
		public String getMessage() {
			return message;
		}

		public IUser getUsr() {
			return usr;
		}

		public Date getReceived() {
			return received;
		}
		
	}
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("[dd.MM. HH:mm]");
	
	private class OldMessage extends Message {
		
		private OldMessage(String message,Date received) {
			super(message,received);
		}
		
		
		
		@Override
		public String toString() {
			String message = sdf.format(getReceived()) +this.message;
			return "\n"+message.replace("\n<", "\n- <").replace("\n[", "\n- [");
		}



		protected void setStyleRanges(int start,String renderedMessage) {
			StyleRange sr = new StyleRange();
			sr.start = start;
			sr.length = renderedMessage.length();
			sr.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
			text.setStyleRange(sr);
		}
	}
	
	

}
