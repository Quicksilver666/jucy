package eu.jucy.gui.favhub;

import java.io.IOException;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.itemhandler.DownloadableHandlers.RemoveDownloadableFromQueueHandler;

import uc.FavHub;
import uc.IFavHubs;

public abstract class FavHubHandlers extends AbstractHandler {

	private static final Logger logger = LoggerFactory.make();
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IStructuredSelection sel = (IStructuredSelection)HandlerUtil.getCurrentSelection(event);
		if (!sel.isEmpty()) {
			FavHub fh = (FavHub)sel.getFirstElement();
			
			FavHubEditor fhe = (FavHubEditor)HandlerUtil.getActiveEditorChecked(event);
			
			run(fh,fhe.getFavHubs(),event);
		}
		return null;
	}
	
	protected abstract void run(FavHub fh,IFavHubs favHubs,ExecutionEvent event);
	
	
	public static class ChangeFHPropertiesHandler extends FavHubHandlers {
		
		public static final String COMMAND_ID = "eu.jucy.gui.fh.properties";
			
		@Override
		protected void run(FavHub fh,IFavHubs favHubs, ExecutionEvent event) {
			FavHubPropertiesDialog diag = new FavHubPropertiesDialog(HandlerUtil.getActiveShell(event),fh);
			if (diag.open() == Dialog.OK) {
				try {
					ApplicationWorkbenchWindowAdvisor.get().getFavHubs().store();
				} catch(IOException ioe) {
					logger.warn(ioe,ioe);
				}
			}
		}
	}
	
	public static class CreateFavHubsHandler extends AbstractHandler {

		public static final String COMMAND_ID = "eu.jucy.gui.fh.new";
		
		public Object execute(ExecutionEvent event) throws ExecutionException {
			FavHubPropertiesDialog fhp = new FavHubPropertiesDialog(HandlerUtil.getActiveShell(event));
			if (fhp.open() == Dialog.OK) {
				FavHub fh = fhp.getResult();
				if (fh != null) {
					logger.debug("Adding FavHub: "+fh.getHubaddy());
					fh.addToFavHubs(ApplicationWorkbenchWindowAdvisor.get().getFavHubs());
				}
			}
			return null;
		}
	}
	
	public static class OpenHubHandler extends FavHubHandlers {
		public static final String COMMAND_ID = "eu.jucy.gui.fh.connect";
		@Override
		protected void run(FavHub fh,IFavHubs favHubs, ExecutionEvent event) {
			fh.connect(ApplicationWorkbenchWindowAdvisor.get());
		}
	}
	
	public static class MoveUpHandler extends FavHubHandlers {

		public static final String COMMAND_ID = "eu.jucy.gui.fh.moveup";
		@Override
		protected void run(FavHub fh,IFavHubs favHubs, ExecutionEvent event) {
			fh.changePriority(true,favHubs);
		}
	}
	
	public static class MoveDownHandler extends FavHubHandlers {

		public static final String COMMAND_ID = "eu.jucy.gui.fh.movedown";
			
		@Override
		protected void run(FavHub fh,IFavHubs favHubs, ExecutionEvent event) {
			fh.changePriority(false,favHubs);
		}
	}
	
	public static class RemoveHandler extends FavHubHandlers {
		public static final String COMMAND_ID = RemoveDownloadableFromQueueHandler.COMMAND_ID;
			
		@Override
		protected void run(FavHub fh,IFavHubs favHubs, ExecutionEvent event) {
			fh.removeFromFavHubs(favHubs);
		}
	}
}
