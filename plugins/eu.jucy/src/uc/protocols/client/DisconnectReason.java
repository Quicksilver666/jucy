/**
 * 
 */
package uc.protocols.client;

import uc.protocols.ADCStatusMessage;

public enum DisconnectReason  {
	
	USERQUIT("User quit"),
	NOSLOTS("No Slots available"),
	DOWNLOADFINISHED("Download finished"),
	UPLOADFINISHED("Upload finished"),
	CLIENTTOOOLD("Client too old",true),
	CONNECTIONTIMEOUT("Connection timeout"),
	FILENOTAVAILABLE("File Not Available",true),
	FILEREMOVED("File was removed"),
	UNKNOWN("Unknown"),
	CLOSEDBYUSER("Closed by User"),
	ILLEGALSTATEERROR("protocol is in an illegal state"), 
	NICKUNKNOWN("Nick is unknown",true);
	
	private final String readableReason;
	private final boolean error;
	
	private DisconnectReason(String readableReason) {
		this(readableReason,false);
	}
	
	/**
	 * 
	 * @param readableReason
	 * @param isError if the disconnect reason is at the same time an error
	 */
	private DisconnectReason(String readableReason, boolean isError) {
		this.error = isError;
		this.readableReason = readableReason;
	}
	
	public String toString() {
		return readableReason;
	}

	public boolean isError() {
		return error;
	}
	
	/**
	 * @return status messages for ADC sending error message
	 */
	public ADCStatusMessage getADCMessage() {
		 return new ADCStatusMessage(readableReason,ADCStatusMessage.FATAL,getTypeCode());
	}
	
	private int getTypeCode() {
		switch(this) {
		case NOSLOTS: return 53;
		case FILENOTAVAILABLE: return 51;
		default:
			throw new IllegalStateException("wrong code found");
		}
		
	}
}