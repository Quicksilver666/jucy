package eu.jucy.gui;



import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import uc.DCClient.ILogEventListener;
import uihelpers.SUIJob;

import eu.jucy.gui.logeditor.LogEditor;
import eu.jucy.gui.logeditor.LogEditorInput;
import eu.jucy.gui.logeditor.OpenLogEditorHandler;

/**
 * 
 * A class to manage logging events that go to the gui..
 * 
 * 
 * @author Quicksilver
 */
public class GuiAppender extends AppenderSkeleton implements ILogEventListener {
	
	private static final int KEPT_MESSAGES = 100;
	
	private static final int MessagesShownInTooltip = 10;
	
	private SimpleDateFormat dateFormat;
	
	private boolean timeStamps;
	
	private IStatusLineManager statusline;
	private IWorkbenchWindow window;
	private SUIJob errorClear = null;
	private Layout severe = new PatternLayout("%-5p %F Line:%L \t\t %m%n");
	//private final Control c; //for the ToolTip 
	
	private final List<LoggingEvent> lastMessages = new LinkedList<LoggingEvent>();
	
	
	private static class SingletonHolder {
		private static final GuiAppender singleton = new GuiAppender();
	}
	
	public static GuiAppender get() {
		return SingletonHolder.singleton;
	}
	
	private GuiAppender() {
		
		new PreferenceChangedAdapter(GUIPI.get(),GUIPI.timeStampFormat) {
		
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				new SUIJob() {
					@Override
					public void run() {
						refreshSettings();
					}
				}.schedule();
		
			}
		};

		refreshSettings();
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
	/**
	 * very hacky method to make setting the StatusLineToolTip possible
	 *  
	 *  c.getChildren()[0] is instanceof CLabel 
	 * @param tooltip
	 */
	public  void setToolTipToStatusLine(String tooltip) {
		Control c = getToolTipControl() ;
		if (c != null) {
			c.setToolTipText(tooltip);
		}
	}
	
	private Control getToolTipControl() {
		if (statusline instanceof StatusLineManager) {
			StatusLineManager slm = (StatusLineManager)statusline;
			Composite c = (Composite)slm.createControl(null);
			if (c.getChildren().length > 0) {
				return c.getChildren()[0];
			}
		}
		return null;
	}
	
	/**
	 * registers listener so doubleclick will open LogEditor
	 * 
	 * @param statusline - the statusline where we can log to
	 */
	public void initialize(IStatusLineManager statusline) {
		this.statusline= statusline;
		this.window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Control c = getToolTipControl();
		if (c != null) {
			c.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					OpenLogEditorHandler.openSystemLogEditor();
				}
			});
		}
		LoggerFactory.addAppender(this);
		ApplicationWorkbenchWindowAdvisor.get().addLogEventListener(this);
	}
	
	
	private final Logger logger = LoggerFactory.make();
	
	private static final Level GUI = new Level(Level.INFO.toInt(),"GUI",Level.INFO.getSyslogEquivalent()){
		private static final long serialVersionUID = 7946573916583631244L;
		} ;
	
	public void logEvent(String event) {
		logger.log(GUI, event);
	}

	private void appendLE(final LoggingEvent event) {
		new SUIJob() {
			public void run() {
				lastMessages.add(event);
				while (lastMessages.size() > KEPT_MESSAGES) {
					lastMessages.remove(0);
				} 

				if (window == null) {
					return;
				}
				IWorkbenchPage page = window.getActivePage();
				if (page == null) {
					return;
				}
				LogEditor le =(LogEditor)page.findEditor(new LogEditorInput());

				if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
					statusline.setErrorMessage(severe.format(event));  //event.getRenderedMessage());

					if (le == null) {
						OpenLogEditorHandler.openSystemLogEditor();
						if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
							MessageDialog.openError(window.getShell(), event.getLevel().toString(), severe.format(event));
						}
					}

					if (errorClear != null) {
						errorClear.cancel();
					}
					errorClear = new SUIJob() { // "clear Error"
						public void run() {
							errorClear = null;
							statusline.setErrorMessage(null);
						}

					};
					errorClear.schedule(1000 * 60 *5);
				} else {
					statusline.setMessage(getMessage(event));
				}

				String tooltip = null;
				for (int i = Math.max(lastMessages.size()-MessagesShownInTooltip, 0); i < lastMessages.size(); i++) {
					if (tooltip == null) {
						tooltip = getMessage(lastMessages.get(i));
					} else {
						tooltip += "\n"+getMessage(lastMessages.get(i));
					}
				}
				setToolTipToStatusLine(tooltip);


				if (le != null) {
					le.append(event);
				}
			}
		}.schedule();

	}
	
	/**
	 * 
	 * when a LoggingEvent is received this will print it to the
	 * StatusLine
	 * 
	 * also the event will be redirected to an OpenLoggingEditor
	 * if the event has at least Level Warn the editor will be 
	 * opened if it is not already open
	 */
	@Override
	protected void append(final LoggingEvent event) {
		if (event.getLevel().isGreaterOrEqual(Level.INFO) || event.getLevel().equals(GUI)) {
			appendLE(event);
		}
	}

	@Override
	public void close() {}

	@Override
	public boolean requiresLayout() {
		// we don't need a layout.. or we do the layout later..
		return false;
	}

	/**
	 * @return messages received in the past ..
	 * up to the last 100 messages
	 */
	public List<LoggingEvent> getLastMessages() {
		return Collections.unmodifiableList(lastMessages);
	}
	
	
	private String getMessage(LoggingEvent event) {
		return (timeStamps? dateFormat.format(new Date(event.timeStamp)) : "" )
				+event.getRenderedMessage();
	}

}
