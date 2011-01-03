package uc.protocols;

import uc.LanguageKeys;




public enum ConnectionState {

	CONNECTING(LanguageKeys.Connecting,false), // the socket is just about to open (before connect..)
	CONNECTED(LanguageKeys.Connected,true),  // Connection was established login process will begin
	LOGGEDIN(LanguageKeys.LoggedIn,true),   // the login procedure is done
	TRANSFERSTARTED(true),	// transfer is running  - only for client-client-protocol
	TRANSFERFINISHED(LanguageKeys.TransferFinished,true),	// transfer started but is now finished
	CLOSED(LanguageKeys.Closed,false), 	// the connection was closed 
	DESTROYED(false);  //the connection was closed.. and is not reconnectable.. for example used when the protocol is garbage collected 
	
	
	private final String translated;
	private final boolean open;
	
	ConnectionState(boolean open) {
		this.open = open;
		translated = this.name();
	}
	
	ConnectionState(String iLanguageKey,boolean open) {
		this.open = open;
		this.translated = iLanguageKey;
	}
	
	
	/**
	 * 
	 * @return true if the connection is open and running
	 */
	public boolean isOpen() {
		return open;
	}
	
	public String toString() {
		return translated;
	}
}
