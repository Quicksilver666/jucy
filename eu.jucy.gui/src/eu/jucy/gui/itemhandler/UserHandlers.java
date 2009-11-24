package eu.jucy.gui.itemhandler;

import helpers.GH;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;


import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.ReplaceLine;


import eu.jucy.gui.filelist.FilelistHandler;
import eu.jucy.gui.texteditor.pmeditor.PMEditor;
import eu.jucy.gui.texteditor.pmeditor.PMEditorInput;


import uc.DCClient;
import uc.IHasUser;
import uc.IHub;
import uc.IUser;
import uc.IHasUser.IMultiUser;
import uc.crypto.HashValue;
import uc.files.IDownloadable;
import uc.files.downloadqueue.AbstractDownloadQueueEntry.IDownloadFinished;
import uc.protocols.SendContext;

public abstract class UserHandlers extends AbstractHandler {
	
	public static final String USER_BY_ID = "USER_BY_ID";
	public static final String BY_ID_POSTFIX = "BYUSERID";
	
	protected UserHandlers() {}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		String userID = event.getParameter(USER_BY_ID);
		if (userID != null) {
			HashValue user = HashValue.createHash(userID);
			IUser usr = ApplicationWorkbenchWindowAdvisor.get().getPopulation().get(user);
			if (usr != null) {
				doWithUsers(Collections.singletonList(usr),event);
			}
		} else {
			List<IUser> users = filter(HandlerUtil.getCurrentSelection(event));
			doWithUsers(users, event);
		}
		
		
		return null;
	}
	
	public static List<IHub> filterHubs(ISelection sel) {
		List<IHub> hubs = new ArrayList<IHub>();
		for (IUser usr: filter(sel)) {
			if (usr.getHub() !=null && !hubs.contains(usr.getHub())) {
				hubs.add(usr.getHub());
			}
		}
		return hubs;
		
	}
	
	/**
	 * filters from a userselection every user..
	 * no user is taken twice!
	 * 
	 * @param sel - a selection of IHasUser/IMultiUserObjects
	 * @return set of all users
	 */
	public static List<IUser> filter(ISelection sel) {
		List<IUser> users = new ArrayList<IUser>();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection)sel;
			Object[] items = selection.toArray();
		
			for (Object item: items) {
				if (item instanceof IMultiUser) {
					for (IUser u: ((IMultiUser)item).getIterable()) {
						if (!users.contains(u)) {
							users.add(u);
						}
					}
				} else if (item instanceof IHasUser) {
					IUser u = ((IHasUser)item).getUser();
					if (!users.contains(u)) {
						users.add(u);
					}
				}
			}
		}
		return users;
	}
	
	protected void doWithUsers(List<IUser> users,ExecutionEvent event) {
		for (IUser user: users) {
			doWithUser(user,event);
		}
	}
	
	protected void doWithUser(IUser usr,ExecutionEvent event) {}

	
	public static class GetFilelistHandler extends UserHandlers {

		public static final String COMMAND_ID = "eu.jucy.gui.getfilelist";
	//	public static final String PARM_COMMAND_ID = "eu.jucy.gui.getfilelistBYUSERID";
		




		protected void doWithUser(final IUser usr,ExecutionEvent event){
			final IDownloadable id = getDownloadableForUsr(usr, HandlerUtil.getCurrentSelection(event));
			usr.downloadFilelist().addDoAfterDownload(new IDownloadFinished() {

				public void finishedDownload(File f) {
					FilelistHandler.openFilelist(usr,id);
				}

				@Override
				public boolean equals(Object obj) {
					return this.getClass().equals(obj.getClass());
				}

				@Override
				public int hashCode() {
					return this.getClass().hashCode();
				}
				
			});	
		}
		
		private static IDownloadable getDownloadableForUsr(IUser usr,ISelection selection) {
			if (selection instanceof StructuredSelection) {
				for (Object o: ((StructuredSelection)selection).toArray()) {
					if (o instanceof IDownloadable) {
						IDownloadable id = (IDownloadable)o;
						for (IUser dl: id.getIterable()) {
							if (dl.equals(usr)) {
								return id;
							}
						}
					} 
				}
			}
			return null;
		}
		
	}
	
	
	public static class BrowseFilelistHandler extends UserHandlers {
		public static final String COMMAND_ID = "eu.jucy.gui.browsefilelist";
		protected void doWithUser(IUser usr,ExecutionEvent event) {
			FilelistHandler.openFilelist(usr);
		}
	}
	
	
	public static class MatchQueueHandler extends UserHandlers { //Lang.MatchQueue
		
		public static final String COMMAND_ID = "eu.jucy.gui.matchqueue";
		
		protected void doWithUser(final IUser usr,ExecutionEvent event) {
			usr.downloadFilelist().addDoAfterDownload( new IDownloadFinished() { 
				public void finishedDownload(File f) { 
					usr.getFilelistDescriptor().getFilelist().match();
				}
				
				@Override
				public boolean equals(Object obj) {
					return this.getClass().equals(obj.getClass());
				}

				@Override
				public int hashCode() {
					return this.getClass().hashCode();
				}
			}); 
		}
	}
	
	
	public static class SendPMHandler extends UserHandlers { //Lang.SendPrivateMessage
		public static final String COMMAND_ID = "eu.jucy.gui.sendpm";
//		public static final String PARM_COMMAND_ID = "eu.jucy.gui.sendpmBYUSERID";
		
		protected void doWithUser(IUser usr,ExecutionEvent event) {
			if (usr.getHub() != null) {
				PMEditor.openPMEditor(new PMEditorInput(usr));
			}
		}	
	}
	
	public static class AddToFavouritesHandler extends UserHandlers { //Lang.AddToFavorites
		
		public static final String COMMAND_ID = "eu.jucy.gui.addtofavourites";
		
		protected void doWithUser(IUser usr,ExecutionEvent event) {
			usr.setFavUser(true);
		}	
	}
	
	public static class RemoveFromFavouritesHandler extends UserHandlers { //Lang.RemoveFromFavorites
		public static final String COMMAND_ID = "eu.jucy.gui.removefromfavourites";
		@Override
		protected void doWithUser(IUser usr,ExecutionEvent event) {
			usr.setFavUser(false);
		}
	}

	public static class GrantExtraSlotHandler extends  UserHandlers { //Lang.GrantExtraSlot
		public static final String COMMAND_ID = "eu.jucy.gui.grantextraslot";
		protected void doWithUser(IUser usr,ExecutionEvent event) {
			usr.increaseAutograntSlot(1000L*60*60*6);//grant a slot for quarter of a day
		}	
	}
	
	public static class RevokeSlotHandler extends  UserHandlers { //Lang.GrantExtraSlot
		public static final String COMMAND_ID = "eu.jucy.gui.revokeextraslot";
		protected void doWithUser(IUser usr,ExecutionEvent event) {
			usr.revokeSlot();
		}	
	}
	
	public static class RemoveUserFromQueueHandler extends UserHandlers { //Lang.RemoveUserFromDQ
		
		public static final String COMMAND_ID = "eu.jucy.gui.removeuserfromqueue";
	//	public static final String PARM_COMMAND_ID = "eu.jucy.gui.removeuserfromqueueBYUSERID";
		
		protected void doWithUser(final IUser usr,ExecutionEvent event) {
			DCClient.execute(new Runnable() {
				public void run() {
					usr.removeFromDownloadQueue();
				}
			});
		}	
	}
	

	public static class CopyNickToClipboardHandler extends UserHandlers { //Lang.CopyNickToClipboard
		public static final String COMMAND_ID = "eu.jucy.gui.copynicktoclipboard";
		
		protected void doWithUser(IUser usr,ExecutionEvent event){
			GuiHelpers.copyTextToClipboard(usr.getNick());
		}	
	}
	
	public static class UserCommandHandler extends UserHandlers {

		@Override
		protected void doWithUsers(List<IUser> users,ExecutionEvent event) {
			String command = event.getParameter(UCContributionItem.SEND); 
			command = ReplaceLine.get().replaceLines(command);
			if (!GH.isNullOrEmpty(command)) {
				for (IUser usr:users) {
					usr.getHub().sendRaw(command, new SendContext(usr));
				}
			}
			
		}	
		
	}

}
