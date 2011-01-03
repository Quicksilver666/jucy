/**
 * 
 */
package eu.jucy.gui.transferview;

import org.eclipse.swt.SWT;

import uc.IUser;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.IFileTransfer;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine;
import eu.jucy.gui.Lang;

public class CompColumn extends TransferColumns {

	public CompColumn() {
		super(30, Lang.Ratio, SWT.LEAD);
	}

	@Override
	protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
		return ft != null? String.format("%.2f", ft.getCompressionRatio()): "";
	}

	@Override
	protected String getText(ClientProtocolStateMachine ccspm,
			IUser usr, FileTransferInformation last) {
		return "";
	}
}