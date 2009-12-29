
package uc;


import helpers.PrefConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


import org.osgi.service.prefs.Preferences;

import uc.crypto.HashValue;
import uc.crypto.Tiger;
import uihelpers.ComplexListEditor.IPrefSerializer;

/**
 * FavHub stores persistent information about a hub.
 * 
 * @author Quicksilver
 *
 */
public class FavHub implements Comparable<FavHub> {
	
	private int order; // the order in which this hub should be displayed and started..
	private boolean autoconnect = false; //auto-connect on startup
	private String hubname="";
	private String hubaddy;
	private String description="";
	private String nick="";
	private String password="";
	private String userDescription="";
	private String email="";
	private boolean chatOnly; //set the hub to chat-only
	private int[] weights = new int[] {300,100}; //The weights of the SashForm of this hub
	private String charset = "";  //empty for default
	
	private boolean showJoins = false,showFavJoins = false;




	private final Map<String,String> info = new HashMap<String,String>();
	
	

	private FavHub(int order , String hubaddy) {
		this.order =	order;
		this.hubaddy =	hubaddy;
	}
	
	public FavHub(String hubaddy) {
		this(-1,hubaddy);
	}

	
	/**
	 * loads a single hub
	 * @param p - the node where all of the hubs information is stored
	 */
	static FavHub loadHub(Preferences p) {
		FavHub fh = new FavHub(new Integer(p.name()), p.get(PI.favHubhubaddy, "unknown"));
		fh.autoconnect = p.getBoolean(PI.favHubAutoconnect, false);
		fh.chatOnly = p.getBoolean(PI.favHubchatOnly, false);
		fh.hubname = p.get(PI.favHubhubname, "");
		fh.email = p.get(PI.eMail, "");
		fh.description = p.get(PI.description, "");
		fh.nick	=	p.get(PI.nick, "");
		fh.password = p.get(PI.favHubpassword,"" );
		fh.userDescription = p.get(PI.favHubuserDescription, "");
		fh.setWeights( p.get(PI.favHubweights, fh.getWeights() ) );
		fh.info.putAll( PrefConverter.asMap(p.get(PI.favInfo, "")));
		fh.charset = p.get(PI.favCharset, "");

		return fh;
	}
	
	/*
	 * loads a single hub
	 * @param p - the node where all of the hubs information is stored
	 *
	static FavHub loadHub(String s) {
		String[] data = PrefConverter.asArray(s);
		FavHub fh = new FavHub(Integer.parseInt(data[0]), data[1]); // p.get(PI.favHubhubaddy, "unknown"));
		fh.autoconnect = Boolean.parseBoolean(data[2]); //   p.getBoolean(PI.favHubAutoconnect, false);
		fh.chatOnly = Boolean.parseBoolean(data[3]);// p.getBoolean(PI.favHubchatOnly, false);
		fh.hubname = data[4]; // p.get(PI.favHubhubname, "");
		fh.email = data[5]; //p.get(PI.eMail, "");
		fh.description =  data[6]; //p.get(PI.description, "");
		fh.nick	= data[7];//	p.get(PI.nick, "");
		fh.password =  data[8]; //p.get(PI.favHubpassword,"" );
		fh.userDescription = data[9]; // p.get(PI.favHubuserDescription, "");
		fh.setWeights(data[10]); // p.get(PI.favHubweights, fh.getWeights() ) );
		fh.info.putAll( PrefConverter.asMap(data[11]));   //p.get(PI.favInfo, "")));
		fh.charset =  data[12];// p.get(PI.favCharset, "");
		return fh;
	} */
	
	
	/*String store() {
		String[] s = new String[13];
		s[0] = ""+order;
		s[1] = hubaddy;
		s[2] = ""+autoconnect;
		s[3] = ""+chatOnly;
		s[4] = hubname;
		s[5] = email;
		s[6] = description;
		s[7] = nick;
		s[8] = password;
		s[9] = userDescription;
		s[10] = getWeights();
		s[11]= PrefConverter.asString(info);
		s[12]= charset;
		
		return PrefConverter.asString(s);
	} */
	/*
	void store(Preferences p) {
		p.putBoolean(PI.favHubAutoconnect, autoconnect);
		p.put(PI.favHubhubaddy, hubaddy);
		p.putBoolean(PI.favHubchatOnly, chatOnly);
		p.put(PI.favHubhubname, hubname);
		p.put(PI.eMail, email);
		p.put(PI.description, description);
		p.put(PI.nick, nick);
		p.put(PI.favHubpassword, password);
		p.put(PI.favHubuserDescription, userDescription);
		p.put(PI.favHubweights, getWeights());
		p.put(PI.favInfo, PrefConverter.asString(info));
		p.put(PI.favCharset, charset);
		
	} */
	
	
	
	private String getWeights() {
		return weights[0]+";"+weights[1];
	}
	
	/**
	 * @return the weights of the SashForm in the HubEditor
	 */
	public int[] weights() { 
		return new int[]{weights[0],weights[1]};
	}
	
	/**
	 * sets the weights of the SashForm for storing ..
	 * @param weights
	 */
	public void setWeights(int[] weights) {
		this.weights = new int[]{weights[0],weights[1]};
	}
	
	private void setWeights(String weight) {
		String[] a	= weight.split(Pattern.quote(";"));
		if (a.length >= 2) {
			weights[0]	= Integer.parseInt(a[0]);
			weights[1]	= Integer.parseInt(a[1]);
		}
	}
	/**
	 * @return if hub should be started on startup
	 */
	public boolean isAutoconnect() {
		return autoconnect;
	}
	/**
	 * @param autoconnect  if the hub should be started on startup
	 */
	public void setAutoconnect(boolean autoconnect) {
		this.autoconnect = autoconnect;
	}
	/**
	 * @return the chatOnly
	 */
	public boolean isChatOnly() {
		return chatOnly;
	}
	/**
	 * @param chatOnly the chatOnly to set
	 */
	public void setChatOnly(boolean chatOnly) {
		this.chatOnly = chatOnly;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		if (description == null) {
			description = "";
		}
		this.description = description;
	}
	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return the hubaddy
	 */
	public String getHubaddy() {
		return hubaddy;
	}
	

	/**
	 * @param hubaddy the hubaddy to set
	 */
	public void setHubaddy(String hubaddy) {
		this.hubaddy = hubaddy;
	}
	/**
	 * @return the hubname
	 */
	public String getHubname() {
		return hubname;
	}
	/**
	 * @param hubname the hubname to set
	 */
	public void setHubname(String hubname) {
		if (hubname == null) {
			hubname = "";
		}
		this.hubname = hubname;
	}
	/**
	 * @return the nick
	 */
	public String getNick() {
		return nick;
	}
	/**
	 * @param nick the nick to set
	 */
	public void setNick(String nick) {
		this.nick = nick;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the userDescription
	 */
	public String getUserDescription() {
		return userDescription;
	}
	/**
	 * @param userDescription the userDescription to set
	 */
	public void setUserDescription(String userDescription) {
		this.userDescription = userDescription;
	}
	/**
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}
	/**
	 *  may only be called by FavHubs.. to preserve the consistence
	 *  of the order in FavHubs..
	 * @param order the order to set
	 */
	void setOrder(int order) {
		this.order = order;
	}
	
	public void connect(DCClient dcc) {
		dcc.getHub(this, true);
	}
	
	/**
	 * check if the favHub is open in given client
	 * @param dcc - dccclient to be checked
	 * @return true if hub is open 
	 */
	public boolean isConnected(DCClient dcc) {
		return dcc.isRunningHub(this);
	}
	
	/**
	 *   
	 * @param up if true hub will be started sooner
	 * (priority will have a lower integer-value actually)
	 * 
	 */
	public void changePriority(boolean up,IFavHubs fh) {
		if (isFavHub(fh)) {
			fh.changeOrder(this, up);
		}
	}
	
	/**
	 * true if this hub was already added to favhubs of that dcclient
	 */
	public boolean isFavHub(IFavHubs favHubs) {
		return favHubs.contains(this);
	}

	/**
	 * adds this hub to the Favorites
	 * 
	 * @return true if the hub was added - 
	 * false if the hub was already a Favorite
	 */
	public boolean addToFavHubs(IFavHubs favHubs) {
		if (!isFavHub(favHubs) && !favHubs.contains(hubaddy)) { 
			favHubs.addToFavorites(this);
			return true;
		} else {
			return false;
		}
	}
	
	public void removeFromFavHubs(IFavHubs favHubs) {
		if (isFavHub(favHubs)) {
			favHubs.removeFromFavorites(this);
		}
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 *
	@Override
	public int hashCode() {
		return order;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 *
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FavHub other = (FavHub) obj;
		if (hubaddy == null) {
			if (other.hubaddy != null)
				return false;
		} else if (!(hubaddy.hashCode() == other.hubaddy.hashCode()))
			return false;

		return true;
	} */
	
	
	

	public int compareTo(FavHub o) {
		return Integer.valueOf(order).compareTo(o.order);
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hubaddy == null) ? 0 : hubaddy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FavHub other = (FavHub) obj;
		if (hubaddy == null) {
			if (other.hubaddy != null)
				return false;
		} else if (!hubaddy.equals(other.hubaddy))
			return false;
		return true;
	}

	/**
	 * 
	 * delegate method to key value map
	 * to store additional info with the FavHub
	 * 
	 * retrieves value
	 */
	public String get(String key) {
		return info.get(key);
	}

	/**
	 * 
	 * delegate method to key value map
	 * to store additional info with the FavHub
	 * 
	 * stores a value... deletes if the value is null
	 */
	public String put(String key, String value) {
		if (value == null) {
			return remove(key);
		} 
		return info.put(key, value);
	}

	/**
	 * delegate method to key value map
	 * to store additional info with the FavHub
	 * 
	 * removes a value
	 */
	public String remove(String key) {
		return info.remove(key);
	}
	/**
	 * 
	 * @return name of the charset to be used..
	 * empty means default (utf-8 for adc, windows-1252 nmdc )
	 */
	public String getCharset() {
		return charset;
	}
	
	public boolean isShowJoins() {
		return showJoins;
	}

	public void setShowJoins(boolean showJoins) {
		this.showJoins = showJoins;
	}

	public boolean isShowFavJoins() {
		return showFavJoins;
	}

	public void setShowFavJoins(boolean showFavJoins) {
		this.showFavJoins = showFavJoins;
	}

	public void setCharset(String encoding) {
		this.charset = encoding;
	}
	
	/**
	 * id that can be used for that hub
	 * @return id only modified by mc flag
	 * used solyly to create ids for DBLoggers..
	 */
	public HashValue getEntityID(boolean mc) {
		String hubaddy = getHubaddy(); 
		if (hubaddy.indexOf("://") != -1) {
			hubaddy = hubaddy.substring(hubaddy.indexOf(':')+3);
		}
		
		return Tiger.tigerOfString((mc?"mc://":"feed://")+hubaddy);
	}
	
	public static class FavHubTranslater implements IPrefSerializer<FavHub> {

		public String[] serialize(FavHub t) {
			return new String[]{
			 ""+t.order,
			t.hubaddy,
			""+t.autoconnect,
			""+t.chatOnly,
			t.hubname,
			t.email,
			t.description,
			t.nick,
			t.password,
			t.userDescription,
			t.getWeights(),
			PrefConverter.asString(t.info),
			t.charset,
			""+t.showJoins,
			""+t.showFavJoins};
		}

		public FavHub unSerialize(String[] data) {
			FavHub fh = new FavHub(Integer.parseInt(data[0]), data[1]); 
			fh.autoconnect = Boolean.parseBoolean(data[2]);
			fh.chatOnly = Boolean.parseBoolean(data[3]);
			fh.hubname = data[4]; 
			fh.email = data[5]; 
			fh.description =  data[6]; 
			fh.nick	= data[7];
			fh.password =  data[8]; 
			fh.userDescription = data[9]; 
			fh.setWeights(data[10]); 
			fh.info.putAll( PrefConverter.asMap(data[11]));   
			fh.charset =  data[12];
			if (data.length > 14) {
				fh.showJoins = Boolean.parseBoolean(data[13]);
				fh.showFavJoins = Boolean.parseBoolean(data[14]);
			}
			
			return fh;
		}
		
	}
	
}