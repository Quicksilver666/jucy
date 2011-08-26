package uc.user;

import helpers.IPrefSerializer;
import helpers.PrefConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;



import uc.IUser;
import uc.IUserChangedListener;
import uc.PI;
import uc.IStoppable.IStartable;
import uc.crypto.HashValue;

public class StoredPM implements IUserChangedListener ,IStartable {
	
	/**
	 * delete messages after that amount of time..
	 * keep for 2 months..
	 */
	private static final long DELETION_TIME = 1000L * 3600L * 24L * 60L;  
	private static final byte MAX_MESSAGES = 5;
	
	private final Map<HashValue,List<Message>> userToMessages = 
		Collections.synchronizedMap(new HashMap<HashValue, List<Message>>());
	
	private final Population pop;
	
	public StoredPM(Population pop) {
		this.pop = pop;
	}
	
	

	
	public void stop() {
		pop.unregisterUserChangedListener(this);
	}


	public void start() {
		load();
		pop.registerUserChangedListener(this);
	}


	
	private static class Message {
		private final HashValue userID;
		private final String message;
		private final boolean me;
		private final long date;
		
		public Message(HashValue userID, String message, boolean me, long date) {
			super();
			this.userID = userID;
			this.message = message;
			this.me = me;
			this.date = date;
		}
		
		public void send(IUser usr) {
			SimpleDateFormat sdf = new SimpleDateFormat("[dd.MM. HH:mm]");
			usr.sendPM(sdf.format(new Date(date))+" "+message, me,false);
		}
		
		public boolean shouldBeDeleted() {
			return System.currentTimeMillis()- date > DELETION_TIME;
		}

	}
	
	public static class MessageTranslater implements IPrefSerializer<Message> {
		
		public String[] serialize(Message t) {
			return new String[]{
				t.userID.toString(),
				t.message,
				""+t.me,
				""+t.date
			};
		}
		
		public Message unSerialize(String[] all) {
			return new Message(HashValue.createHash(all[0]),
					all[1],
					Boolean.parseBoolean(all[2]),
					Long.parseLong(all[3]) );
		}
		
	}
	
	private void load() {
		List<Message> messages = PrefConverter.parseString(PI.get(PI.storedPMs), new MessageTranslater());
		for (Message mes:messages) {
			addPM(mes);
		}
	}
	
	private void store() {
		List<Message> mesList = new ArrayList<Message>();
		for (List<Message> list:userToMessages.values()) {
			mesList.addAll(list);
		}
		String s = PrefConverter.createList(mesList, new MessageTranslater());
		PI.put(PI.storedPMs, s);
	}
	
	
	/**
	 * stores a private message to be sent when the user comes online again..
	 * 
	 * @param target
	 * @param message
	 * @param me
	 * @return  true if successful... number of PMs per user is limited..
	 * otherwise sending of PMs on connect will trigger spam controls..
	 */
	public boolean storePM(IUser target,String message,boolean me) {
		Message mes = new Message(target.getUserid(),message,me,System.currentTimeMillis());
		boolean ret = addPM(mes);
		store();
		return ret;
	}

	private boolean addPM(Message mes) {
		if (!mes.shouldBeDeleted()) {
			List<Message> messageList = userToMessages.get(mes.userID);
			if (messageList == null) {
				messageList = new CopyOnWriteArrayList<Message>();
				userToMessages.put(mes.userID, messageList);
			}
			if (messageList.size() < MAX_MESSAGES) {
				messageList.add(mes);
				return true;
			}
		}
		return false;
	}

	public void changed(UserChangeEvent uce) {
		if (uce.getType() == UserChange.CONNECTED && 
				userToMessages.containsKey(uce.getChanged().getUserid())) {
			List<Message> messageList = userToMessages.remove(uce.getChanged().getUserid());
			if (messageList != null) {
				for (Message mes:messageList) {
					mes.send(uce.getChanged());
				}
				store();
			}
		}
	}
	
	

}
