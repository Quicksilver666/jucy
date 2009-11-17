package eu.jucy.gui.itemhandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.downloadqueue.DownloadQueue;
import uihelpers.IconManager;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.itemhandler.DownloadableHandlers.DownloadToRecommendedPath;

public class RecommendedDownloadsContributionItem extends CompoundContributionItem implements IWorkbenchContribution {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG); 
	
	private IServiceLocator serviceLocator;
	
	
	@Override
	protected IContributionItem[] getContributionItems() {

		IStructuredSelection iss= (IStructuredSelection)((ISelectionService)serviceLocator.getService(ISelectionService.class)).getSelection();
		ArrayList<IContributionItem> contribs = new ArrayList<IContributionItem>();


		Object o = iss.getFirstElement();
		logger.debug("downloadable: "+o.getClass().getName()+"  size: "+iss.size());

		if (o instanceof IHasDownloadable && ((IHasDownloadable)o).getDownloadable() != null) {
			IDownloadable idf = ((IHasDownloadable)o).getDownloadable();
			DownloadQueue dq = ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue();
			for (File target:dq.getPathRecommendation(idf)) {
				logger.debug("adding recommendation: "+target);
				CommandContributionItemParameter ccip = 
					new CommandContributionItemParameter(serviceLocator, null,
							DownloadToRecommendedPath.ID,SWT.PUSH);

				ccip.parameters = Collections.singletonMap(
						DownloadToRecommendedPath.TargetPath, target.getPath());
				ccip.label = target.getPath();
				
				ccip.icon = IconManager.get().getImageDescriptorFromFile(target);
				

				CommandContributionItem cci = new CommandContributionItem(ccip);
				contribs.add(cci);

			}
		}

		
		return contribs.toArray(new IContributionItem[]{});
	}

	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
		
	}


	

}
