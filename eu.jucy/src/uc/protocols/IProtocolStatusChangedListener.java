package uc.protocols;


public interface IProtocolStatusChangedListener {



	/**
	 * @param newStatus - what new status the connection has..
	 * @param cp - the connection protocol currently associated with this connection
	 */
	void statusChanged(ConnectionState newStatus,ConnectionProtocol cp);
}
