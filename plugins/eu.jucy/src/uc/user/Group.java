package uc.user;

import java.util.Map;

import uc.IUser;

/**
 * A group where a user can belong to
 * 
 *  ex
 *  1. FavUsers could become one such group
 *  2. user with slot granted another?
 *  3. maybe user in DownloadQueue also
 * 
 * @author Quicksilver
 * 
 */
public abstract class Group<K extends GroupInfo> {
	
	public static final String EXTENSIONPOINT_ID = "eu.jucy.user.Group";
	
	private String id;
	private String name;
	
	void init(String id) {
		this.id = id;
		//TODO set name
	}
	
	
	public String getName() {
		return name; 
	}
	
	/**
	 * 
	 * @param s stored info for this GroupInfo object
	 * @return an object made from s , null if the info has become invalid
	 * in meantime / the user is no longer part of the group due to some 
	 * time conditions
	 * 
	 */
	public abstract K unserialize(Map<String,String> s);
	public abstract Map<String,String> serializeInfo(K info);
	
	void infoChanged(K k) {
		
	}
	
	
	protected void userAddedToGroup(IUser usr) {}
	protected void userRemovedFromGroup(IUser usr){}
	
	
	public K getGroupInfo(IUser usr) {
		return null;
	}
	
	public boolean inGroup(IUser usr) {
		return false;
	}
	

}
