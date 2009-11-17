package eu.jucy.gui.texteditor;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


import eu.jucy.gui.GUIPI;
import eu.jucy.gui.logeditor.LogEditor;
import eu.jucy.gui.logeditor.LogEditorInput;
import eu.jucy.gui.logeditor.OpenLogEditorHandler;

import uc.IHub;
import uc.protocols.hub.IFeedListener.FeedType;
import uihelpers.SUIJob;



public class LabelViewer {
	
	private static Logger logger = LoggerFactory.make();
	
	static {
		logger.setLevel(Level.INFO);
	}
	
	private static final int HISTORY = 150;
	
	private final CLabel label;
	
	private SimpleDateFormat dateFormat;
	
	private boolean timeStamps;
	
	private final List<Message> messages = new LinkedList<Message>();
	
	private final IHub hub;
	
	private PreferenceChangedAdapter listener;
	
	/**
	 * constructor that will additionally open an FeedEditor window on DoubleClick
	 * So specially used for the Feed label below the hub window
	 * 
	 * @param label
	 * @param hub
	 */
	public LabelViewer(CLabel label, IHub hub) {
		this.label = label;
		this.hub = hub;
		refreshSettings();
		listener = new PreferenceChangedAdapter(GUIPI.get(),GUIPI.timeStampFormat,GUIPI.timeStamps) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				new SUIJob() {
					@Override
					public void run() {
						refreshSettings();
						refresh();
					}
				}.schedule();
			}
		};
		
		if (hub != null) {
			label.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					logger.debug("Open Log editor called");
					OpenLogEditorHandler.openFeedLogEditor(LabelViewer.this, LabelViewer.this.hub);
				}
				
			});
		}
		label.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				listener.dispose();
			}
		});
	}
	
	/**
	 * constructor for normal Label viewer
	 * 
	 * @param label
	 */
	public LabelViewer(CLabel label) {
		this(label,null);
	}
	
	private void refreshSettings() {
		String format = GUIPI.get(GUIPI.timeStampFormat);
		if (format == null || GH.isEmpty(format)) {
			dateFormat = new SimpleDateFormat(); 
		} else {
			dateFormat = new SimpleDateFormat(format);
		}
		timeStamps = GUIPI.getBoolean(GUIPI.timeStamps);
	}
	
	private void appendMessage(Message m) {
		if (!label.isDisposed()) {
			String meslabel = m.toString();
			if (meslabel.indexOf('\n') != -1) {
				meslabel = meslabel.substring(0, meslabel.indexOf('\n'));
			}
			label.setText(meslabel);
			label.setForeground(m.getMessageColor());
			
			String tooltip = null;
			for (int i = Math.max(0, messages.size()-5); i < messages.size();i++) {
				Message mes = messages.get(i);
				if (tooltip == null) {
					tooltip = mes.toString();
				} else {
					tooltip += "\n"+mes.toString();
				}
			}
			label.setToolTipText(tooltip);
			appendIfOpen(m);
		}
	}
	
	/**
	 * adds message to the StyledText
	 * @param message - what is to be added
	 */
	public void addMessage(String message) {
		addFeedMessage( FeedType.NONE , message);
	}
	
	public void addFeedMessage(final FeedType ft, final String message) {
		Message m = new Message(this,message,ft);
		messages.add(m);
		if (messages.size() > HISTORY) {
			messages.remove(0);
		}
		appendMessage(m);
	}
	
	
	
	/**
	 * refreshes the styled text
	 * used when timeStamps change
	 */
	public void refresh() {
		if (label.isDisposed()) {
			return;
		}
		label.setText("");
		LogEditor le = getLogEditorIfOpen();
		if (le != null) {
			le.clear();
		}
		for (Message m: messages) {
			appendMessage(m);
		}
	}

	private void appendIfOpen(Message m) {
		LogEditor le = getLogEditorIfOpen();
		if (le != null) {
			le.appendFeed(m);
		}
	}
	
	/**
	 * 
	 * @return the mathcing logeditor if open .. null otherwise
	 */
	private LogEditor getLogEditorIfOpen() {
		if (hub == null) {
			return null;
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		return (LogEditor)page.findEditor(new LogEditorInput(this,hub));
	}
	
	
	
	public static class Message {
		private final String message;
		private final Date received;
		private final FeedType ft;
		private final LabelViewer label;
		private Message(LabelViewer label,String message, FeedType ft) {
			this.label = label;
			this.message = message;
			this.ft = ft;
			this.received = new Date();
		}
		
		public String toString() {
			return (label.timeStamps? label.dateFormat.format(received) : "" )+ft+message;
		}
		
		public Color getMessageColor() {
			Display display = label.label.getDisplay();
			switch(ft) {
			case GUI:
				return display.getSystemColor(SWT.COLOR_DARK_CYAN);
			case KICK:
				return display.getSystemColor(SWT.COLOR_BLUE);
			case ACTION:
			case EVENT:
				return display.getSystemColor(SWT.COLOR_DARK_GREEN);
			case ERROR:
			case FATAL:
			case WARN:
				return display.getSystemColor(SWT.COLOR_DARK_RED);
			case NONE:
			case REPORT:
				return display.getSystemColor(SWT.COLOR_BLACK);
			default: 
				throw new IllegalStateException();
			}
		}
		
	}




	public List<Message> getMessages() {
		return Collections.unmodifiableList(messages);
	}
}
