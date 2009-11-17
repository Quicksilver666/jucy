package eu.jucy.gui.itemhandler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.handlers.HandlerUtil;

import uc.DCClient;


import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine;
import uc.protocols.client.DisconnectReason;




public abstract class TransfersHandlers extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection)HandlerUtil.getCurrentSelection(event);
		if (selection != null) {
			Object o = selection.getFirstElement();
			if (o instanceof ClientProtocol) {
				run((ClientProtocol)o,event);
			} else if (o instanceof ClientProtocolStateMachine) {
				run((ClientProtocolStateMachine)o,event);
			} else {
				throw new IllegalStateException();
			}
		}
		return null;
	}
	
	protected void run(ClientProtocol cp,ExecutionEvent event) throws ExecutionException {}
	
	protected void run(ClientProtocolStateMachine cpsm,ExecutionEvent event) throws ExecutionException {}
	
	
	public static class CloseConnection extends TransfersHandlers {
		
		

		@Override
		protected void run(final ClientProtocol cp,ExecutionEvent event) {
			DCClient.execute(new Runnable() {
				public void run() {
					cp.disconnect(DisconnectReason.CLOSEDBYUSER);
				}
			});
			setEnabled(false);
		}
		
	}
	
	public static class ForceAttempt extends TransfersHandlers {

		@Override
		protected void run(ClientProtocolStateMachine cpsm,ExecutionEvent event) {
			cpsm.clearTime();
		}
	}
	

}
