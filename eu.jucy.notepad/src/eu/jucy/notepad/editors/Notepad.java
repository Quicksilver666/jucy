package eu.jucy.notepad.editors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.osgi.service.prefs.BackingStoreException;

import uihelpers.SUIJob;

import eu.jucy.gui.UCMessageEditor;



public class Notepad extends UCMessageEditor {

	private static final String PLUGIN_ID = "eu.jucy.notepad" ;
	
	/**
	 * editor id
	 */
	public static final String ID = "eu.jucy.notepad"; //Editor id
	
	private static final String notepad0  = "notepad0"; // may be later add more notepads
	
	private StyledText text;
	private volatile String textstr = "";
	private volatile boolean textChanged = false;
	
	@Override
	public void createPartControl(Composite parent) {
		text = new StyledText(parent, SWT.V_SCROLL | SWT.WRAP| SWT.MULTI);
		
		text.addModifyListener(new ModifyListener() {//moves the text downwards
			public void modifyText(final ModifyEvent e) {
				textstr = text.getText();
				textChanged = true;
			}
		});
		text.setText(get());
		setText(text);
		Menu popupMenu = new Menu(text);
		MenuItem copyItem = new MenuItem(popupMenu, SWT.PUSH);
		copyItem.setText("Copy");
		copyItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				text.copy();
			}
		});
		MenuItem cutItem = new MenuItem(popupMenu, SWT.PUSH);
		cutItem.setText("Cut");
		cutItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				text.cut();
			}
		});
		new MenuItem(popupMenu, SWT.SEPARATOR);
		MenuItem pasteItem = new MenuItem(popupMenu, SWT.PUSH);
		pasteItem.setText("Paste");
		pasteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				text.paste();
			}
		});
		
		
		text.setMenu(popupMenu);
		
		
		new SUIJob() {

			@Override
			public void run() {
				if (!text.isDisposed()) {
					checkSave();
					schedule(10000);
				}	
			}
		}.schedule(1000);
	}
	
	

	@Override
	public void clear() {
		text.setText("");
	}



	private void checkSave() {
		if (textChanged) {
			textChanged = false;
			put(textstr);
		}
	}
	
	@Override
	public void setFocus() {
		text.setFocus();
	}

	public void dispose() {
		checkSave();
		super.dispose();
	}
	
	
	
	public static String get() {
		return new InstanceScope().getNode(PLUGIN_ID).get(notepad0, "");
	}
	
	public static boolean put(String value) {
		IEclipsePreferences is = new InstanceScope().getNode(PLUGIN_ID);
		is.put(notepad0, value);
		try {
			is.flush();
		} catch(BackingStoreException bse) {
			bse.printStackTrace();
			return false;
		}
		return true;
	}

}
