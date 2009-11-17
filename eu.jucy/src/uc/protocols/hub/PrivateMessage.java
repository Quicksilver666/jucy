package uc.protocols.hub;

import uc.IUser;

public class PrivateMessage {
	
	private final IUser from;
	
	private final IUser sender;
	
	private final String message;
	private final boolean me;
	
	private final long timeReceived;

	public PrivateMessage(IUser from, IUser sender, String message, boolean me) {
		super();
		this.from = from;
		this.sender = sender;
		this.message = message;
		this.timeReceived = System.currentTimeMillis();
		this.me = me;
	}

	/**
	 * the owner of the window..
	 * @return
	 */
	public IUser getFrom() {
		return from;
	}

	/**
	 * 
	 * @return the one standing in the brackets .. <%[userNI]>
	 */
	public IUser getSender() {
		return sender;
	}

	public String getMessage() {
		return message;
	}

	public long getTimeReceived() {
		return timeReceived;
	}
	
	public boolean fromEqualsSender() {
		return from.equals(sender);
	}
	
	public String toString() {
		if (me) {
			return "*"+ (sender != null? sender.getNick()+" ":"")+getMessage()+"*";
		} else {
			return (sender != null? "<"+sender.getNick()+"> ":"")+getMessage();
		}
	}
	

}
