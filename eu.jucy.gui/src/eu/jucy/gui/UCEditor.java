package eu.jucy.gui;





import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.action.IContributionItem;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import uihelpers.SUIJob;





public abstract class UCEditor extends EditorPart implements IUCEditor {

	private static final Logger logger = LoggerFactory.make();
	
	
	
	private CopyOnWriteArrayList<ITopicChangedListener> topicListeners = 
		new CopyOnWriteArrayList<ITopicChangedListener>();
	
	private UCWorkbenchPart part = new UCWorkbenchPart();
	
	
	protected UCEditor() {
	}
	
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}



	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}


	protected void setControlsForFontAndColour(Control... receivingChange) {
		part.setControlsForFontAndColour(receivingChange);
	}



	public String getTopic() {
		return getPartName();
	}


	public void registerTopicChangedListener(ITopicChangedListener tcl) {
		topicListeners.addIfAbsent(tcl);
	}

	public void unregisterTopicChangedListener(ITopicChangedListener tcl) {
		topicListeners.remove(tcl);
	}


	public void fireTopicChangedListeners() {
		new SUIJob() {
			@Override
			public void run() {
				for (ITopicChangedListener tcl:topicListeners) {
					tcl.topicChanged(UCEditor.this);
				}
			}
		}.schedule();
	}
	
	
	public void tabMenuBeforeShow() {
		logger.debug("tab menu to be shown");
	}

	public void getContributionItems(List<IContributionItem> items) {
		CommandContributionItemParameter ccip = 
			new CommandContributionItemParameter(getSite(), null, CloseEditorHandler.COMMAND_ID,SWT.PUSH);
		ccip.icon = AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.STOPICON);

		CommandContributionItem cci = new CommandContributionItem(ccip);
		
		items.add(cci);
	}

	public void dispose() {
		super.dispose();
		topicListeners.clear();
		part.dispose();
	}
	
	

	/**
	 * normal initialisation setting part by editor input. 
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName( input.getName());
		
	}

	
	/**
	 * creates a context popup menu on the viewer and registers it for this site
	 * with the default ID 
	 * @param viewer
	 */
	protected void createContextPopup(Viewer viewer) {
		createContextPopup(getSite().getId(), viewer);
	}
	
	/**
	 * creates a context popup menu on the viewer and registers it for this site
	 * with the provided ID 
	 * @param id
	 * @param viewer
	 */
	protected void createContextPopup(String id,Viewer viewer) {
		UCWorkbenchPart.createContextPopups(getSite(),id,viewer);
	}
	

	

	/**
	 * does nothing..
	 */
	public void partActivated() {}



	/**
	 * little helper executes given command, logs failures
	 * 
	 * @param commandID
	 */
	public void executeCommand(String commandID) {
		IHandlerService hs = (IHandlerService)getSite().getService(IHandlerService.class);

		try {
			hs.executeCommand(commandID, null);
		} catch (Exception e) {
			logger.warn(e,e);
		} 
	}


}
