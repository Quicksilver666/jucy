package eu.jucy.gui.itemhandler;

import helpers.GH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logger.LoggerFactory;



import org.apache.log4j.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionService;

import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;



import eu.jucy.gui.ReplaceLine;
import eu.jucy.gui.settings.UserCommands;
import eu.jucy.gui.texteditor.UCTextEditor;

import uc.Command;
import uc.IHub;
import uc.IUser;
import uc.protocols.SendContext;


public abstract class UCContributionItem extends CompoundContributionItem implements IWorkbenchContribution {

	
	
	private static final Logger logger = LoggerFactory.make();
	private static final String SelfDefinedCommandID = "eu.jucy.gui.selfdefinedcommand"; 
	public static final String SD_USER = SelfDefinedCommandID+".user" ;
	public static final String SD_DOWNLOADABLE =SelfDefinedCommandID  +".downloadable";
	public static final String SD_HUB = SelfDefinedCommandID + ".hub";
	
	public static final String SEND = "SEND";
	
	
	protected final int where;
	protected final String commandID;
	private final Object allwaysSelected;
	
	protected UCContributionItem(int where,String commandid) {
		this(where,commandid,null);
	}
	
	protected UCContributionItem(int where,String commandid,Object allwaysSelected) {
		this.where = where;
		this.commandID = commandid;
		this.allwaysSelected = allwaysSelected;
	}
	
	
	protected ISelectionService ss;
	protected IServiceLocator serviceLocator;
	
	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
		ss = (ISelectionService)serviceLocator.getService(ISelectionService.class);
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		logger.debug("getContributionItems() called "+where);
		ISelection sel;
		if (allwaysSelected == null) {
			sel = ss.getSelection();
		} else {
			sel = new StructuredSelection(allwaysSelected);
		}
		List<IHub> hubs = UserHandlers.filterHubs(sel);
		
		if (!hubs.isEmpty()) {
			boolean multiusers = UserHandlers.filter(sel).size() > 1;
			List<Command> userCommands = UserCommands.loadCommandAndAddHubCommnds(hubs,multiusers, where);
			return addCommands(userCommands);
		}

		return new IContributionItem[]{};
	}
	
	
	public IContributionItem[] addCommands(List<Command> coms) {
		
		Map<List<String>,IMenuManager> alreadyCreated = new HashMap<List<String>,IMenuManager>();
		List<IContributionItem> itemsAdded = new ArrayList<IContributionItem>();
		
		for (Command com:coms) {
			IMenuManager target = null;
			//first make sure parent menumanagers exist
			String[] paths = com.getPaths();
			for (int i = 0;i < paths.length-1; i++) {
				String[] sub = subarray(paths,i+1);
				target = alreadyCreated.get(Arrays.asList(sub));
				if (target == null) {
					target = new MenuManager(sub[sub.length-1]);
					alreadyCreated.put(Arrays.asList(sub), target);
					IMenuManager parent = alreadyCreated.get(Arrays.asList(subarray(sub,sub.length-1)));
					if (parent == null) {
						//then we have a toplevel menu
						logger.debug("adding Menu to Toplevel: "+sub[sub.length-1]);
						itemsAdded.add(target);
					} else {
						logger.debug("addind Menu to parent: "+sub[sub.length-1]);
						parent.add(target);
						
					}
				} else {
				//	logger.debug("Menu already created.");
				}
			}
			IContributionItem con = createFromCommand(com);
			if (target == null) {
				logger.debug("addind contrib to Toplevel: "+com.getName()+" "+con);
				itemsAdded.add(con);
			} else {
				logger.debug("addind contrib to parent: "+com.getName()+ " "+con);
				target.add(con);
			}
		}
		return itemsAdded.toArray(new IContributionItem[]{});
	}
	
	

	private IContributionItem createFromCommand(Command com) {
		if (com.isSeparator()) {
			return new Separator();
		} else {
			CommandContributionItemParameter ccip = 
				new CommandContributionItemParameter(serviceLocator,null,commandID, SWT.PUSH);
			ccip.label = com.getName();
			ccip.parameters = Collections.singletonMap(SEND, com.getCommand());
			//TODO user by ID parameter needed here.. 
			
			CommandContributionItem cci = new CommandContributionItem(ccip);
			
			return cci;
		}
	}
	
	
	private static String[] subarray(String[] arr,int length) {
		String[] s = new String[length];
		System.arraycopy(arr, 0, s, 0, length);
		return s;
	}
	
	public static class UserCommandsContributionItem extends UCContributionItem {
		public UserCommandsContributionItem() {
			super(Command.USER,SD_USER);
		}
		
		public UserCommandsContributionItem(IUser usr) {
			super(Command.USER,SD_USER,usr);
		}
	}
	
	public static class DownloadableSearchContributionItem extends UCContributionItem {
		public DownloadableSearchContributionItem() {
			super(Command.SEARCH,SD_DOWNLOADABLE);
		}
	}
	
	public static class DownloadableFileListContributionItem extends UCContributionItem {
		public DownloadableFileListContributionItem() {
			super(Command.FILELIST,SD_DOWNLOADABLE);
		}
	}
	
	public static class HubContributionItem extends UCContributionItem {
		private final IHub hub;
		public HubContributionItem() {
			this(null);
		}
		
		public HubContributionItem(IHub hub) {
			super(Command.HUB,SD_HUB);
			this.hub = hub;
		}

		@Override
		protected IContributionItem[] getContributionItems() {
			IPartService ips = (IPartService) serviceLocator.getService(IPartService.class);
			logger.debug("getContribs()");
			IHub hub = this.hub;
			if (hub == null && ips.getActivePart() instanceof UCTextEditor) {
				 hub = ((UCTextEditor)ips.getActivePart()).getHub();
			}
			if (hub != null) {
				List<Command> userCommands = UserCommands.loadCommandAndAddHubCommnds(
						Collections.singletonList(hub),false, where);
				logger.debug("size: "+userCommands.size());
				return addCommands(userCommands);
			}
			
			return new IContributionItem[]{};
		}
	}
	
	public static class HubSelfDefinedCommandHandler extends AbstractHandler {
		
		public HubSelfDefinedCommandHandler(){}
		
		public Object execute(ExecutionEvent event) throws ExecutionException {
			IEditorPart part = HandlerUtil.getActiveEditor(event);
			if (part instanceof UCTextEditor) {
				IHub hub = ((UCTextEditor)part).getHub();
				String command = event.getParameter(SEND); 
				Map<String,String> reps = ReplaceLine.get().replaceLines(command);
				if (!GH.isNullOrEmpty(command) && reps != null) {
					hub.sendRaw(command,new SendContext(reps));
				}
			}
			return null;
		}
	}

}
