package eu.jucy.connectiondebugger;

import eu.jucy.connectiondebugger.CryptoInfo.CryptoInfoEntry;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public class CryptoInfoColumn extends ColumnDescriptor<CryptoInfoEntry> {

	public CryptoInfoColumn() {
		super(200, "Crypto Info");
	}

	@Override
	public String getText(CryptoInfoEntry x) {
		return x.getType();
	}

	
	
}
