package eu.jucy.gui.texteditor;

import helpers.GH;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import logger.LoggerFactory;


import org.apache.log4j.Logger;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCMessageEditor;
import eu.jucy.gui.search.OpenSearchEditorHandler;


import uc.DCClient;
import uc.FavHub;
import uc.IUser;
import uc.PI;
import uc.protocols.hub.Hub;

/**
 * Interprets commands given from CommandLine
 * 
 * @author Quicksilver
 *
 */
public class CommandInterpreter {

	private static final Logger logger = LoggerFactory.make();
	
	private static final Map<String,Command> mapping = new HashMap<String,Command>();
	
	static {
		Command.values();
	}
	
	private final Hub he;
	private final UCMessageEditor editor;
	private final IUser usr;
	
	public CommandInterpreter(Hub he, UCMessageEditor editor) {
		this.he = he;
		this.editor = editor;
		usr = null;
	}
	
	public CommandInterpreter(IUser usr, UCMessageEditor editor) {
		he = (Hub)usr.getHub();
		this.editor = editor;
		this.usr = usr;
	}
	
	
	public boolean isCommand(String line) {
		if (!line.startsWith("/")) {
			logger.debug("message starts not with /");
			return false;
		}
		int i = line.indexOf(' ');
		if (i == -1) {
			i = line.length();
		}
		
		String command = line.substring(1, i);
		command = command.toUpperCase();
		Command com = mapping.get(command);
		logger.debug("command found"+com+ " string searched: "+command+"  length: "+mapping.size());
		return com != null;
		
		
	}
	
	public void executeCommand(String line) {
		int i = line.indexOf(' ');
		if (i == -1) {
			i = line.length();
		}
		
		String command = line.substring(1, i);
		command = command.toUpperCase();
		Command com = mapping.get(command);
		com.execute(line, he,usr, editor);
	}
	
	private static enum Command {
		
		AWAY,BACK,UC,REFRESH,ME,PM,SEARCH,SLOTS,REBUILD,PRUNEHASHES,JOIN,CLEAR,TS,CLOSE,GETLIST,FAVORITES,FAV,PASS,HELP;
		
	
		Command() {
			mapping.put(name(), this);
		}
		
		public void execute(String line,final Hub hub,IUser usr, UCMessageEditor editor) {
			
			switch(this) {
			case UC:
				List<String> phrases = Arrays.asList(
						"I don't even know what to say. \nThis client is so simple yet so feature complete.    -- Todi",  //said as fun when still thinking the client didn't exist and was a April Fools joke
						"I know my client is secure against\nbuffer overflows. What about yours?",
						"My client is LG-Proof®. What about yours?",
						"Actually, I agree with Todi. Great work qs!    -- Nev",
						"Leave out all the rest!   -- Linkin Park",
						"My client loads old PMs from the log \nwhen opening a PM window. \nWhat about yours?",
						"Downloading the same file from multiple users?\nSure you can do that!\nBut my client does not need 1000 segments for that!"
						);
				
				
				hub.sendMM("\n"+phrases.get(GH.nextInt(phrases.size()))+"\n<"+DCClient.LONGVERSION+"> http://jucy.eu",false);
				break;
			case CLEAR:
				editor.clear();
				break;
			case CLOSE:
				editor.dispose();
				break;
			case ME:
				int i = line.indexOf(' ');
				if (i != -1) {
					String send = line.substring(i+1);
					if (!GH.isEmpty(send)) {
						if (usr != null) {
							usr.sendPM(send,true);
						} else {
							hub.sendMM(send, true);
						}
					}
				}
				break;
			case FAV:
			case FAVORITES:
				if (usr != null) {
					if (!usr.isFavUser()) {
						usr.setFavUser(true);
					}
				} else if (!hub.getFavHub().isFavHub(ApplicationWorkbenchWindowAdvisor.get().getFavHubs())) {
					FavHub fh= hub.getFavHub();
					fh.setHubname(hub.getHubname());
					fh.setDescription(hub.getTopic());
					fh.addToFavHubs(ApplicationWorkbenchWindowAdvisor.get().getFavHubs());
					hub.statusMessage(Lang.HubAddedToFavorites,0 );
				} else {
					hub.statusMessage(Lang.HubIsAlreadyFavorite,0);
				}
				break;
			case GETLIST:
				String nick = GetFirstWord(line);
				
				IUser user = hub.getUserByNick(nick);
				if (user != null) {
					user.downloadFilelist();
					
					break;
				}
				break;
			case JOIN:
				String addy = GetFirstWord(line);
				if (!GH.isEmpty(addy)) {
					ApplicationWorkbenchWindowAdvisor.get().getHub(new FavHub(addy),true);
				}
				break;
			case PM:
				String receiverNick = GetFirstWord(line);
				
				IUser usrByNick = hub.getUserByNick(receiverNick);
				String[] splits = line.split(Pattern.quote(" "),3);
				if (usrByNick != null && splits.length == 3) {
					usrByNick.sendPM(splits[2],false);
				}
				
				break;
			case REBUILD:
				ApplicationWorkbenchWindowAdvisor.get().rebuildFilelist();
				break;
			case PRUNEHASHES:
				DCClient.execute(new Runnable() {
					public void run() {
						int i = DCClient.get().pruneHashes();
						hub.statusMessage(String.format(
								"Deleted %d unused hashes", i), 0);
					}
				});
				break;
			case REFRESH:
				ApplicationWorkbenchWindowAdvisor.get().refreshFilelist();
				break;
			case TS:
				GUIPI.put(GUIPI.timeStamps,!GUIPI.getBoolean(GUIPI.timeStamps));
				break;
			case SLOTS:
				String number = GetFirstWord(line);
				if (number.matches("\\d+")) {
					int num = Integer.parseInt(number);
					if (num > 0) {
						PI.put(PI.slots,num );
					}
				}
				break;
			case SEARCH: // /SEARCH <string>
				String search = GetAllAfterCommand(line).trim();
				OpenSearchEditorHandler.openSearchEditor(editor.getSite().getWorkbenchWindow(), search);
				break;
			case PASS:
				String passwd = GetFirstWord(line).trim();
				if (!GH.isEmpty(passwd)) {
					hub.sendPassword(passwd);
				}
				break;
			case AWAY:
				String awaymsg = GetAllAfterCommand(line);
				if (GH.isEmpty(awaymsg.trim())) {
					ApplicationWorkbenchWindowAdvisor.get().setAway(true);
				} else {
					ApplicationWorkbenchWindowAdvisor.get().setAway(awaymsg);
				}
				break;
			case BACK:
				ApplicationWorkbenchWindowAdvisor.get().setAway(false);
				break;
			case HELP:
				hub.statusMessage("/away <msg>, /me <msg>, /back, /refresh, /slots #, /uc, /rebuild, /pruneHashes, /join <hub-ip>,"
						+" /clear, /ts, /close, /fav, /pm <user> [message], /getList <user> ",0);
				break;
			}
		}
		
		/**
		 * first word.. null if not found
		 * @param command where to SEARCH for
		 * @return the first word (/command firstWord rest)
		 */
		private static String GetFirstWord(String command) {
			String[] splits = command.split(Pattern.quote(" "),3);
			if (splits.length >=2 ) {
				return splits[1];
			}
			return "";
		}
		
		private static String GetAllAfterCommand(String command) {
			String[] splits = command.split(Pattern.quote(" "),2);
			if (splits.length == 2) {
				return splits[1];
			}
			return "";
		}
	}
	
	
	
}
