package uc;


import helpers.GH;
import helpers.PrefConverter;

import java.util.regex.Pattern;




import uihelpers.ComplexListEditor.IPrefSerializer;




/**
 * a command that may either be created by the user from settings dialog
 * or send by the hub via $UserCommand
 * 
 * It represents a command for some pop-up menu in the GUI
 * 
 * @author Quicksilver
 *
 */
public class Command  {


	/**
	 * if true the command may be sent to multiple users at once..
	 */
	private boolean allowMulti = false;
	

	/**
	 * what should be sent to the hub
	 */
	private String command = "";


	
	public static final int HUB		= 1,
	USER	= 2,
	SEARCH	= 4,
	FILELIST= 8;

	/**
	 * path of the command separated by /
	 * last part is the name
	 */
	protected String path;
	
	/**
	 * modifier telling where this command should be applied
	 * 
	 * 1 = Hub 
	 * 2 = User 
	 * 4 = Search
	 * 8 = FileList 
	 */
	private int where;


	/**
	 * where the command is applicable
	 * "op" means where we are op
	 * empty String means everywhere..
	 * otherwise it is everywhere applicable
	 * where this string is contained in the DNS..
	 */
	private String hub;
	
	private boolean separator;

	public Command(String path, boolean allowMulti, int where,String command,String hub) {
		this(where,path,hub);
		this.allowMulti = allowMulti;
		if (command != null) {
			this.command = command;
		}
		separator = false;
	}
	
	/**
	 * Constructor for separator..
	 * 
	 * @param where - to what menus this applies
	 * @param path -  if no path set (empty) separator is added to lowest level
	 */
	public Command(int where,String path, String hub) {
		this.path = path;
		this.where = where;
		this.hub = hub;
		separator = true;
	}

	public Command() {
		this(0,"","");
	}
	
	public int getWhere() {
		return where;
	}


	public String getPath() {
		return path;
	}
	
	public String getParentPath() {
		int i = path.lastIndexOf('\\');
		if (i != -1) {
			return path.substring(0,i);
		} else {
			return "";
		}
	}

	public String[] getPaths() {
		if (separator && GH.isEmpty(path)) {
			return new String[]{};
		}
		return path.split(Pattern.quote("\\"));
	}
	
	
	public boolean matches(int place) {
		return (place & where) != 0;
	}
	
	public void setWhere(int place) {
		this.where = place;
	}
	
	public boolean matches(int place,IHub hub) {
		boolean correctPlace = matches(place);
		if (correctPlace) {
			if (hub == null) {
				return true;
			}
			
			String hubaddy =hub.getFavHub().getHubaddy().toLowerCase();
			if (this.hub.equals("op")) {
				return hub.getSelf().isOp();
			} else {
				//empty or matching hubaddy
				return hubaddy.contains(this.hub.toLowerCase());
			}
		}
		return false; 
	}

	/**
	 * 
	 * @param place - in which places this command should be deleted
	 * @return true if no place is left where
	 */
	public boolean delete(int place) {
		where = where & ~place ;
		return where == 0;
	}
	
	public String getHub() {
		return hub;
	}
	
	public void setHub(String hub) {
		if (hub == null) {
			throw new IllegalArgumentException();
		}
		this.hub = hub;
	}
	
	
	public String getName() {
		if (separator) {
			return "Separator";
		}
		String[] s = getPaths();
		return s[s.length-1];
	}
	

	/**
	 * may more then one user or item be affected..
	 * @return
	 */
	public boolean isAllowMulti() {
		return allowMulti;
	}

	/**
	 * 
	 * @return command to be sent... 
	 */
	public String getCommand() {
		return command;
	}



	public String toString() {
		return "Name: "+ getName()+" ParentPath:"+getParentPath()+" TotalPath:"+getPath();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		final Command other = (Command) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	public boolean isSeparator() {
		return separator;
	}

	public void setSeparator(boolean separator) {
		this.separator = separator;
	}

	public void setAllowMulti(boolean allowMulti) {
		this.allowMulti = allowMulti;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public static Command createFromString(String com) {
		return new CommandTranslater().unSerialize(PrefConverter.asArray(com));
	}
	
	public String toStoreString() {
		return PrefConverter.asString(new CommandTranslater().serialize(this));
	}

	
	public static class CommandTranslater implements IPrefSerializer<Command> {

		public String[] serialize(Command t) {
			if (!t.isSeparator()) {
				Command c= (Command)t;
				return new String[]{c.getPath(),
						Integer.toString(c.getWhere()),
						c.getHub(),
						c.getCommand(),
						Boolean.toString(c.isAllowMulti())};
			}
			return new String[]{t.getPath(),Integer.toString(t.getWhere()),
					t.getHub()};
		}

		public Command unSerialize(String[] all) {
			if (all.length == 3) {
				return new Command(Integer.valueOf(all[1]),all[0],all[2]);
			} else if (all.length == 5) {
				return new Command(all[0],Boolean.valueOf(all[4]),
						Integer.valueOf(all[1]),all[3],all[2]);
			}
			throw new IllegalArgumentException();
		}
	}
	
}
