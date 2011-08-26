package eu.jucy.gui.settings;

import java.io.IOException;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import uc.PI;

public abstract class UCPrefpage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	protected static final Logger logger =  LoggerFactory.make();
	
	
	private final ScopedPreferenceStore preferences;
	
	private final String showHelpID; 
	
	
	public UCPrefpage(String pluginID,String helpID) {
		super(GRID);
		preferences = new ScopedPreferenceStore(new InstanceScope(),pluginID);
		setPreferenceStore(preferences);
		this.showHelpID = helpID;
	}
	public UCPrefpage(String pluginID) {
		this(pluginID,null);
	}
	
	public UCPrefpage() {
		this(PI.PLUGIN_ID);
	}
	
	
	public boolean performOk() {
		try {
			preferences.save();
		} catch(IOException ioe){
			logger.warn(ioe,ioe);
		} catch(NullPointerException npe) {
			throw new RuntimeException("Bad NPE: "+getClass().getName(),npe) ;
		}
		return super.performOk();
	}


	public void init(IWorkbench workbench) {}


	
	
    protected void contributeButtons(Composite parent) {
    	if (showHelpID != null) {
    		createHelpControl(parent);
    		PlatformUI.getWorkbench().getHelpSystem().setHelp(getFieldEditorParent(),showHelpID); 
    	}
    }
    
    protected Control createHelpControl(Composite parent) {
		Image helpImage = JFaceResources.getImage(Dialog.DLG_IMG_HELP);
		if (helpImage != null) {
			return createHelpImageButton(parent, helpImage);
		}
		return createHelpLink(parent);
    }
    
	/*
	 * Creates a help link. This is used when there is no help image
	 * available.
	 */
	private Link createHelpLink(Composite parent) {
		Link link = new Link(parent, SWT.WRAP | SWT.NO_FOCUS);
        ((GridLayout) parent.getLayout()).numColumns++;
		link.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		link.setText("<a>"+IDialogConstants.HELP_LABEL+"</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.setToolTipText(IDialogConstants.HELP_LABEL);
		link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
				helpPressed();
            }
        });
		return link;
	}	
    
    /*
     * Creates a button with a help image. This is only used if there
     * is an image available.
     */
	private ToolBar createHelpImageButton(Composite parent, Image image) {
        ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.NO_FOCUS);
        ((GridLayout) parent.getLayout()).numColumns++;
		toolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		final Cursor cursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		toolBar.setCursor(cursor);
		toolBar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				cursor.dispose();
			}
		});		
        ToolItem item = new ToolItem(toolBar, SWT.NONE);
		item.setImage(image);
		item.setToolTipText(JFaceResources.getString("helpToolTip")); //$NON-NLS-1$
		item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
				helpPressed();
            }
        });
		return toolBar;
	}
	
	/*
	 * Called when the help control is invoked. This emulates the keyboard
	 * context help behavior (e.g. F1 on Windows). It traverses the widget
	 * tree upward until it finds a widget that has a help listener on it,
	 * then invokes a help event on that widget.
	 */
	private void helpPressed() {
		logger.debug("help Pressed..");
    	if (getShell() != null) {
	    	Control c = getFieldEditorParent();
	    	while (c != null) {
	    		if (c.isListening(SWT.Help)) {
	    			logger.debug("found listener");
	    			c.notifyListeners(SWT.Help, new Event());
	    			break;
	    		}
	    		c = c.getParent();
	    	}
    	}
	}

}
