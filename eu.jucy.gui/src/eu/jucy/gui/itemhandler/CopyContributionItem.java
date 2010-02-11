package eu.jucy.gui.itemhandler;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;


import eu.jucy.gui.itemhandler.UserHandlers.CopyNickToClipboardHandler;

import uc.IUser;
import uc.protocols.hub.INFField;


public class CopyContributionItem extends CompoundContributionItem implements
		IWorkbenchContribution {

	private static final Logger logger = LoggerFactory.make();
	
	private IServiceLocator serviceLocator;
	
	private IUser usr;
	
	public CopyContributionItem() {
		this(null);
	}
	
	public CopyContributionItem(IUser usr) {
		super();
		this.usr = usr;
	}


	@SuppressWarnings("unchecked")
	@Override
	protected IContributionItem[] getContributionItems() {
	//	List<IContributionItem> items = new ArrayList<IContributionItem>();
		IMenuManager manager = new MenuManager("Copy");
		String commandId = CopyNickToClipboardHandler.COMMAND_ID;
		boolean byID = usr != null;
		if (byID) {
			commandId += UserHandlers.BY_ID_POSTFIX;
		}
		List<INFField> fields = new ArrayList<INFField>();
		fields.add(INFField.NI);
		fields.add(INFField.I4);
		fields.add(INFField.I6);
		fields.add(INFField.SS);
		fields.add(INFField.ID);
		fields.add(INFField.DE);
		fields.add(INFField.VE);
		fields.add(INFField.US);
		fields.add(INFField.KP);
		fields.add(INFField.U4);
		
		
		for (INFField inf:fields) {
			CommandContributionItemParameter ccip = 
				new CommandContributionItemParameter(serviceLocator, null, commandId,SWT.PUSH);
			ccip.parameters = new HashMap<String,String>();
			ccip.parameters.put(CopyNickToClipboardHandler.USE_INF, inf.name());
			if (byID) {
				ccip.parameters.put(UserHandlers.USER_BY_ID,usr.getUserid().toString());
			}
			
			ccip.label = inf.toString();
			
			logger.debug("parameter: "+ ccip.parameters.get(CopyNickToClipboardHandler.USE_INF));
			CommandContributionItem cci = new CommandContributionItem(ccip);
			manager.add(cci);
		}
		
		return new IContributionItem[]{manager};
	}

	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

}
