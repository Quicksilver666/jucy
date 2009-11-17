package eu.jucy.gui.itemhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import uc.IUser;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UserColumns;
import eu.jucy.gui.itemhandler.DownloadQueueHandlers.ReaddSourceHandler;
import eu.jucy.gui.itemhandler.DownloadQueueHandlers.RemoveSourceHandler;
import eu.jucy.gui.itemhandler.UserHandlers.GetFilelistHandler;
import eu.jucy.gui.itemhandler.UserHandlers.RemoveUserFromQueueHandler;
import eu.jucy.gui.itemhandler.UserHandlers.SendPMHandler;

public class DownloadQueueContributionItem extends CompoundContributionItem
		implements IWorkbenchContribution {

	private static final Logger logger = LoggerFactory.make();
	
	private IServiceLocator serviceLocator;
	
	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

	
	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> topLevel = new ArrayList<IContributionItem>();

		ISelectionService selserv = (ISelectionService)serviceLocator.getService(ISelectionService.class);
		List<AbstractDownloadQueueEntry> entries = DownloadQueueHandlers.getSelected(selserv.getSelection(),true);
		logger.debug("menu is about to show");
		
		if (entries.size() == 1) {
			AbstractDownloadQueueEntry adqe = entries.get(0);
			MenuManager reAddUser = new MenuManager(Lang.ReAddUserToFile);
			MenuManager getFilelist = new MenuManager(Lang.GetFilelist);
			MenuManager sendPM = new MenuManager(Lang.SendPrivateMessage);
			MenuManager removeSource = new MenuManager(Lang.RemoveUserFromFile);
			MenuManager removeUserFromQueue = new MenuManager(Lang.RemoveUserFromDQ);
			
			if (!adqe.getUsers().isEmpty()) {
				topLevel.add(getFilelist);
				topLevel.add(sendPM);
				topLevel.add(removeUserFromQueue);
				
				removeSource.add(create(null,RemoveSourceHandler.COMMAND_ID));
				
				removeSource.add(new Separator());
				
				topLevel.add(removeSource);
			}
			if (!adqe.getRemovedUsers().isEmpty()) {
				reAddUser.add(create(null,ReaddSourceHandler.COMMAND_ID));
				reAddUser.add(new Separator());
				topLevel.add(reAddUser);
			}	
			
			for (IUser usr:adqe.getUsers()) {
			
				getFilelist.add(create(usr,GetFilelistHandler.COMMAND_ID+UserHandlers.BY_ID_POSTFIX));
				if (usr.getHub() != null) {
					sendPM.add(create(usr,SendPMHandler.COMMAND_ID+UserHandlers.BY_ID_POSTFIX));
				}
				removeSource.add(create(usr,RemoveSourceHandler.COMMAND_ID));
				removeUserFromQueue.add(create(usr,RemoveUserFromQueueHandler.COMMAND_ID+UserHandlers.BY_ID_POSTFIX));
				
				logger.debug("added user: "+usr);
			}
			
			for (IUser usr: adqe.getRemovedUsers()) {
				reAddUser.add(create(usr,ReaddSourceHandler.COMMAND_ID));
			}
		}
		
		
		return topLevel.toArray( new IContributionItem[]{});
	}

	private IContributionItem create(IUser usr,String commandId) {
		CommandContributionItemParameter ccip = 
			new CommandContributionItemParameter(serviceLocator, null,
					commandId,SWT.PUSH);
		if (usr != null) {
			ccip.parameters = Collections.singletonMap(
					UserHandlers.USER_BY_ID,usr.getUserid().toString()); 
			ccip.label = usr.getNick();
		
			ccip.icon = ImageDescriptor.createFromImage(UserColumns.Nick.GetUserImage(usr));
		} else {
			ccip.parameters = Collections.singletonMap(
					UserHandlers.USER_BY_ID,null); //all users
			ccip.label = Lang.All;
		}
		
		return new CommandContributionItem(ccip);
	}

}
