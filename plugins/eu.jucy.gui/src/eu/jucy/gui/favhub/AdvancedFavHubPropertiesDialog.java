package eu.jucy.gui.favhub;

import java.util.ArrayList;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import eu.jucy.gui.Lang;
import eu.jucy.gui.favhub.IFavHubAdvanced.ICompControl;

import uc.FavHub;

public class AdvancedFavHubPropertiesDialog extends Dialog {

	private static Logger logger = LoggerFactory.make();
	
	private final FavHub modify;
	private final List<ICompControl>  controls = new ArrayList<ICompControl>();
	
	public AdvancedFavHubPropertiesDialog(Shell parent,FavHub modify) {
		super(parent);
		this.modify = modify;
		setBlockOnOpen(true); 
	}
	
	/**
	 * Create contents of the dialog
	 */
	protected Composite  createDialogArea(Composite parent) {
		
		Composite composite = (Composite) super.createDialogArea(parent);

		getShell().setText(Lang.AdvancedFavHubProperties); 
		

		
    	IExtensionRegistry reg = Platform.getExtensionRegistry();
    	
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(IFavHubAdvanced.ExtensionPointID);
		
		
		CTabFolder tabFolder = new CTabFolder(composite,SWT.TOP|SWT.BORDER);
		tabFolder.setSimple(false);
		
		for (IConfigurationElement element : configElements) {
			try {
				CTabItem item = new CTabItem(tabFolder, SWT.NONE);
				item.setText(element.getAttribute("tab_name"));
				IFavHubAdvanced fha = (IFavHubAdvanced) element.createExecutableExtension("class");
				Composite comp = new Composite(tabFolder,SWT.NONE);
				ICompControl icc = fha.fillComposite(comp, modify);
				controls.add(icc);
				item.setControl(comp);
			} catch (CoreException e) {
				logger.warn(e, e);
			}
		}
	
		return composite;
	}

	@Override
	protected void okPressed() {
		for (ICompControl icc:controls) {
			icc.okPressed(modify);
		}
		super.okPressed();
	}
		
	
}
