package eu.jucy.gui.itemhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.services.IServiceLocator;

import eu.jucy.gui.Application;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.itemhandler.UserHandlers.AddToFavouritesHandler;
import eu.jucy.gui.itemhandler.UserHandlers.BrowseFilelistHandler;
import eu.jucy.gui.itemhandler.UserHandlers.CopyNickToClipboardHandler;
import eu.jucy.gui.itemhandler.UserHandlers.GetFilelistHandler;
import eu.jucy.gui.itemhandler.UserHandlers.GrantExtraSlotHandler;
import eu.jucy.gui.itemhandler.UserHandlers.MatchQueueHandler;
import eu.jucy.gui.itemhandler.UserHandlers.RemoveFromFavouritesHandler;
import eu.jucy.gui.itemhandler.UserHandlers.RemoveUserFromQueueHandler;
import eu.jucy.gui.itemhandler.UserHandlers.RevokeSlotHandler;
import eu.jucy.gui.itemhandler.UserHandlers.SendPMHandler;


import uc.IHasUser;
import uc.IUser;
import uc.IHasUser.IMultiUser;

public class UserContributionItem extends CompoundContributionItem implements
		IWorkbenchContribution {

	private final IUser usr;
	private IServiceLocator serviceLocator;
	
	private static final ImageDescriptor filelist,sendPM,fav;
	
	static {
		filelist = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.FILELIST);
		sendPM = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.SENDPM);
		fav = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.FAVUSER);
	}
	
	public UserContributionItem() {
		this(null);
	}
	
	public UserContributionItem(IUser usr) {
		this.usr = usr;
		
	}
	
	
	@Override
	protected IContributionItem[] getContributionItems() {
		ArrayList<IContributionItem> contribs = new ArrayList<IContributionItem>();

		List<IUser> selectedUsers = new ArrayList<IUser>();
		if (usr == null) {
			ISelectionService selserv = (ISelectionService)serviceLocator.getService(ISelectionService.class);
			ISelection sel = selserv.getSelection();
			if (sel instanceof IStructuredSelection) {
				for (Object o : ((IStructuredSelection)sel).toArray()) {
					if (o instanceof IMultiUser) {
						selectedUsers.addAll(((IMultiUser)o).getIterable());
					} else if (o instanceof IHasUser) {
						selectedUsers.add(((IHasUser)o).getUser());
					} else if (o instanceof IUser) {
						selectedUsers.add((IUser)o);
					}
				}
			} 
		} else {
			selectedUsers.add(usr);
		}
		
		if (!selectedUsers.isEmpty()) {
			contribs.add(create(GetFilelistHandler.COMMAND_ID, filelist)); 
			
			boolean allHaveFilelist = true;
			for (IUser usr:selectedUsers) {
				allHaveFilelist = allHaveFilelist && usr.hasDownloadedFilelist();
			}
			if (allHaveFilelist) {
				contribs.add(create(BrowseFilelistHandler.COMMAND_ID, null ));
			}
			
			contribs.add(create(MatchQueueHandler.COMMAND_ID, null ));
			
			if (selectedUsers.size() == 1) {
				IUser sel = selectedUsers.get(0);
			
				if (sel.getHub() != null) {
					contribs.add(create(SendPMHandler.COMMAND_ID, sendPM  )); 
				}
			
				if (!sel.isFavUser()) {
					contribs.add(create(AddToFavouritesHandler.COMMAND_ID, fav )); 
				} else {
					contribs.add(create(RemoveFromFavouritesHandler.COMMAND_ID, fav )); 
				}
				
				if (!sel.hasCurrentlyAutogrant()) {
					contribs.add(create(GrantExtraSlotHandler.COMMAND_ID,null));
				} else {
					contribs.add(create(RevokeSlotHandler.COMMAND_ID,null));
				}
				
				contribs.add(new Separator());
				if (sel.nrOfFilesInQueue() > 0) {
					contribs.add(create(RemoveUserFromQueueHandler.COMMAND_ID,null));
				} 
				
				contribs.add(create(CopyNickToClipboardHandler.COMMAND_ID,null));
			}
		}
		
		return contribs.toArray(new IContributionItem[]{});
	}

	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}
	
	private IContributionItem create(String commandId,ImageDescriptor icon) {
		boolean byID = usr != null;
		if (byID) {
			commandId += UserHandlers.BY_ID_POSTFIX;
		}
		CommandContributionItemParameter ccip = 
			new CommandContributionItemParameter(serviceLocator, null,
					commandId,SWT.PUSH);
		ccip.icon = icon;
		if (byID) {
			ccip.parameters = Collections.singletonMap(
					UserHandlers.USER_BY_ID,usr.getUserid().toString());
		}
		return new CommandContributionItem(ccip);
	}

}
