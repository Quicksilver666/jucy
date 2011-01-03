package eu.jucy.gui.logeditor;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


import uihelpers.SUIJob;

import eu.jucy.gui.GUIPI;
import eu.jucy.gui.GuiAppender;
import eu.jucy.gui.UCMessageEditor;
import eu.jucy.gui.texteditor.LabelViewer;
import eu.jucy.gui.texteditor.LabelViewer.Message;

public class LogEditor extends UCMessageEditor {

	public static final String ID = "eu.jucy.gui.logeditor";
	
	private StyledText styledText;
	private SimpleDateFormat dateFormat;
	private boolean timeStamps;
	private PreferenceChangedAdapter pca; 
	
	private Layout severe = new PatternLayout("%-5p %F Line:%L \t\t %m%n");

	
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		styledText = new StyledText(parent, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY | SWT.MULTI | SWT.BORDER);
		
		styledText.addModifyListener(new ModifyListener() { //scrolls down
			public void modifyText(ModifyEvent e) {
				styledText.setSelection(styledText.getCharCount());
			}
		});
		setText(styledText);

		
		
		refreshSettings();
		
		if (getLabelViewer() == null) {
			for (LoggingEvent e:GuiAppender.get().getLastMessages()) {
				append(e);
			}
		} else {
			LabelViewer lab = getLabelViewer();
			for (Message m:lab.getMessages()) {
				appendFeed(m);
			}
		}
		

		
		pca = new PreferenceChangedAdapter(GUIPI.get(),GUIPI.timeStampFormat,GUIPI.timeStamps) {
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
	

	private LabelViewer getLabelViewer() {
		return  ((LogEditorInput)getEditorInput()).getSource();
	}
	
	
	@Override
	public void clear() {
		styledText.setText("");
	}

	public void dispose() {
		if (pca != null) {
			pca.dispose();
		}
	}


	@Override
	public void setFocus() {
		styledText.setFocus();
	}

	
	
	public void append(LoggingEvent event) {
		boolean error = event.getLevel().isGreaterOrEqual(Level.WARN);
		boolean heavyError = event.getLevel().isGreaterOrEqual(Level.ERROR);
		boolean fatalError = event.getLevel().isGreaterOrEqual(Level.FATAL);
		
		int startpos = styledText.getCharCount();
		
		String text = "\n"+ (error?severe.format(event): 
										(timeStamps? 
												dateFormat.format(new Date(event.timeStamp)) 
												: "" )+event.getRenderedMessage());
		
		Display display = styledText.getDisplay(); 
		
		styledText.append(text);
		
		StyleRange styleRange = new StyleRange();
		styleRange.start = startpos;
		styleRange.length = text.length();
		styleRange.fontStyle = error ? SWT.BOLD: SWT.NORMAL;
		
		styleRange.foreground =error ?display.getSystemColor(SWT.COLOR_RED):styledText.getForeground() ;
		if (heavyError) {
			styleRange.background = display.getSystemColor(SWT.COLOR_BLACK);
		}
		if (fatalError) {
			styleRange.underline = true;
		}
		styledText.setStyleRange(styleRange);
	}
	
	/**
	 * 
	 * @param mes - a feed message
	 */
	public void appendFeed(Message mes) {
		int startpos = styledText.getCharCount();
		String text = "\n"+ mes.toString();
		styledText.append(text);
		StyleRange styleRange = new StyleRange();
		styleRange.start = startpos;
		styleRange.length = text.length();
		styleRange.foreground = mes.getMessageColor();
		styledText.setStyleRange(styleRange);
	}

}
