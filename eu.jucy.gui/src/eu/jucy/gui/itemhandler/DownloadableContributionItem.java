package eu.jucy.gui.itemhandler;

import java.util.ArrayList;
import java.util.Collections;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import uc.FavFolders;
import uc.FavFolders.FavDir;
import uihelpers.IconManager;
import eu.jucy.gui.itemhandler.DownloadableHandlers.DownloadToRecommendedDir;
import eu.jucy.gui.itemhandler.DownloadableHandlers.DownloadToRecommendedPath;

public class DownloadableContributionItem extends CompoundContributionItem implements IWorkbenchContribution {

	private static final Logger logger = LoggerFactory.make();
	
	
	private IServiceLocator serviceLocator;
	
	@Override
	protected IContributionItem[] getContributionItems() {
		ArrayList<IContributionItem> contribs = new ArrayList<IContributionItem>();
		for (FavDir favDir: FavFolders.getFavDirs()) {
			CommandContributionItemParameter ccip = 
				new CommandContributionItemParameter(serviceLocator, null, DownloadToRecommendedDir.ID,SWT.PUSH);
			ccip.parameters = Collections.singletonMap(DownloadToRecommendedPath.TargetPath, favDir.getDirectory().getPath());
			ccip.label = favDir.getName();
			if (favDir.getDirectory().isDirectory()) {
				ccip.icon =  IconManager.get().getImageDescriptorFromFile(favDir.getDirectory()); 
			}
			logger.debug("parameter: "+ ccip.parameters.get(DownloadToRecommendedPath.TargetPath));
			CommandContributionItem cci = new CommandContributionItem(ccip);
			contribs.add(cci);
		}
		return contribs.toArray(new IContributionItem[]{});
	}

	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}


	

}
