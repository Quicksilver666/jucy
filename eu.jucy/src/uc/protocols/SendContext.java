package uc.protocols;

import helpers.SizeEnum;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import uc.IHub;
import uc.IUser;
import uc.crypto.HashValue;
import uc.files.IDownloadable;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.MagnetLink;
import uc.files.filelist.FileListFolder;
import uc.protocols.hub.AbstractADCHubCommand;
import uc.protocols.hub.INFField;



/**
 * specifies a context in which a command is sent
 * and allows the string to be formatted with this context..
 * 

 * 
 * @author quicksilver
 *
 */
public class SendContext {


	private static final Map<String,String> DateMappings = Collections.synchronizedMap(new HashMap<String,String>());
	
	static {
		String[] normalMappings = new String[]{"a","A","b","B","c","d","H","I","j","m","M","p","S","y","Y"};
		for (String normal : normalMappings) {
			DateMappings.put("%"+normal, "%t"+normal);
		}
		DateMappings.put("%x", "%tF");
		DateMappings.put("%X", "%tR");
		DateMappings.put("%z", "");
		DateMappings.put("%Z", "");
	}
	
	
	protected static final Pattern replace = Pattern.compile(".*?(%\\[(.+?)\\]).*"); 
	
	private static final Pattern datematcher = Pattern.compile(".*?(%[aAbBcdHIjmMpSxXyYzZ]).*");
	
	
	/**
	 * the hub and with it, the user we represent our self is always
	 * available
	 */
	private IHub hub;
	
	/**
	 * the other user..
	 */
	private IUser user;
	
	
	private IDownloadable fileOrFolder;
	
	protected Map<String,String> replacements;

	
	/**
	 * empty send context.. if none is available
	 */
	public SendContext() {}
	
	public SendContext(Map<String,String> replacements) {
		this.replacements = replacements;
	}

	/**
	 * create a send Context with a set other user..
	 * @param other
	 */
	public SendContext(IUser other,Map<String,String> replacements) {
		this(replacements);
		this.user = other;
	}
	
	public SendContext(IDownloadable fileOrFolder,Map<String,String> replacements) {
		this(fileOrFolder,fileOrFolder.getUser(),replacements);
	}
	
	public SendContext(IDownloadable fileOrFolder,IUser user,Map<String,String> replacements) {
		this.fileOrFolder = fileOrFolder;
		this.user = user;
		this.replacements = replacements;
	}
	
	

	
	/**
	 * 
	 * @param command - a command that is to be sent..
	 * @return the command formatted with date and time stuff
	 * and with %[userNI]/ %[myNI] and alike stuff that is usual in 
	 * DC
	 */
	public String format(String command) {
		if (replacements != null) {
			for (Entry<String,String> e : replacements.entrySet()) {
				String repl = hub.isNMDC()? DCProtocol.doReplaces(e.getValue()):
											AbstractADCCommand.doReplaces(e.getValue());
				command = command.replace(e.getKey(), repl);
			}
		}
		
		Matcher m = replace.matcher(command);
		int currentpos = 0;
		
		while(m.find(currentpos)) {
			String formatStringfound = m.group(2);
			Formatter f = Formatter.parse(formatStringfound ); 
			String replacement = f.getReplacement(this, formatStringfound);
			command = command.replace(m.group(1), replacement);
			currentpos = m.start(1)+1;
		}

		
		return formatDate(command);
	}
	
	
	/**
	 * method needed as the java date format is different than
	 * the POSIX/UNIX one..
	 */
	private String formatDate(String command) {
		Matcher m = datematcher.matcher(command);
		int currentpos = 0 ;
		
		Calendar c = null;
		while (m.find(currentpos)) {
			if (c == null) {
				c = Calendar.getInstance();
			}
			String dateSpec = m.group(1);
			String replaceSpec = DateMappings.get(dateSpec);
			String replaceString = String.format(replaceSpec, c);
			
			command = command.replace(dateSpec, replaceString);
			
			currentpos = m.start(1)+1;
		}
		return command;
		
	}

	public void setHub(IHub hub) {
		this.hub = hub;
	}
	
	
	/*
	 * just a testfunction..
	 *
	public static void main(String[] args) {
		SendContext sc = new SendContext();
		sc.user = Population.get().get("schnüffi", TigerHashValue.SELFHASH);
		
		String command= "hello says %[userNI]";
		Matcher m = replace.matcher(command);
		System.out.println(m.matches());
		System.out.println(m.group(1));
		System.out.println(m.group(2));
		System.out.println(sc.format(command));
	} */
	

	public IUser getUser() {
		return user;
	}

	public IHub getHub() {
		return hub;
	}
	
	public IDownloadable getFile() {
		return fileOrFolder;
	}
	/*private static abstract class Formatter<T> {
		
		private final String replacePrefix;
		/**
		 * formatters replace
		 * @param replace what the formatter does replace  for example 
		 * "userNI" for a formatter that does replace ""
		 *
		public Formatter(String replaceprefix) {
			this.replacePrefix = replaceprefix;
		}
		public String getReplacePrefix() {
			return replacePrefix;
		}
		
		public abstract String format(String command,T t); 
	}*/
	
	
	private static enum Formatter {
		userI6(true,getINFFields()), //default for all inf
		myI6(false,getINFFields()),  
		
		nick(FormatterType.USER),
		mynick(FormatterType.MY),
		ip(FormatterType.USER),
		
		userTAG(FormatterType.USER,"tag"),
		myTAG(FormatterType.MY),

		description(FormatterType.USER),
		email(FormatterType.USER),
		share(FormatterType.USER),
		userSSshort(FormatterType.USER,"shareshort"),
		mySSshort(FormatterType.MY),
		userCID(FormatterType.USER,"cid"),
		myCID(FormatterType.MY,"mycid"),
		userSID(FormatterType.USER),
		mySID(FormatterType.MY),

		
		fileFN(FormatterType.FILE,"file"),
		filePath(FormatterType.FILE),
		Directory(FormatterType.FILE),
		fileSI(FormatterType.FILE,"fileSIsize","filesize"),
		fileSIshort(FormatterType.FILE,"filesizeshort"),
		fileTR(FormatterType.FILE,"tth"),
		fileMN(FormatterType.FILE),
		type(FormatterType.FILE),
		
		hubNI(FormatterType.HUB),
		hubDE(FormatterType.HUB),
		hubVE(FormatterType.HUB),
		
		//line(FormatterType.OTHER), .. line is not allowed here..
		DEFAULT(FormatterType.OTHER);
		
		
		private final FormatterType formatterType;
		
		private final String[] alternateNames;
		
		private Formatter(FormatterType t,String... alternateNames) {
			formatterType = t;
			this.alternateNames = alternateNames;
		}
		
		private Formatter(boolean user,INFField... alternateNames) {
			formatterType = user? FormatterType.USER : FormatterType.MY;
			this.alternateNames = new String[alternateNames.length];
			for (int i = 0 ; i < alternateNames.length; i++) {
				this.alternateNames[i] =formatterType.name().toLowerCase()+alternateNames[i].name();
			}
		} 
		
		private static INFField[] getINFFields() {
			Set<INFField> fields = new HashSet<INFField>(Arrays.asList(INFField.values()));
			fields.remove( INFField.PD );	//these fields are not used/allowed
			fields.remove( INFField.RF );
			fields.remove( INFField.TO );
			
			return fields.toArray(new INFField[0]);
		}
		
		private static final Map<String,Formatter> formatters = new HashMap<String,Formatter>();
		
		static {
			for (Formatter f:Formatter.values()) {
				formatters.put(f.name(), f);
				for (String alternate :f.alternateNames) {
					formatters.put(alternate, f);
				}
			}
		}
		
		/**
		 * 
		 * @param formatstring the string between the brackets .. so userNI for %[userNI]
		 * @return the matching formatter..
		 */
		public static Formatter parse(String formatstring) {
			int i = formatstring.indexOf(':');
			if (i != -1) {
				formatstring = formatstring.substring(0, i);
			}
			
			Formatter f  = formatters.get(formatstring);
			if (f != null) {	
				return f;
			} else {
				return DEFAULT;
			}
		}

		private String getReplacement(SendContext sc,String formatString) {
			IUser usr = null;
			IDownloadable file =  sc.getFile();
			switch(formatterType) {
			case USER:
				usr = sc.getUser();
				if ( usr == null) {
					return "%["+formatString+"]";
				}
				break;
			case MY:
				if (sc.getHub() == null || (usr =sc.getHub().getSelf()) == null) {
					return "%["+formatString+"]";
				} 
				break;
			case FILE:
				if (file == null) {
					return "%["+formatString+"]";
				}
				break;
			case HUB:
				if (sc.getHub() == null) {
					return "%["+formatString+"]";
				}
				break;
			}
			
			switch (this) {
			case nick:
			case mynick:
				return usr.getNick();
			case ip:
				InetAddress ia = usr.getIp();
				if (ia == null) {
					return "%["+formatString+"]";
				} else {
					return ia.getHostAddress(); 
				}
			case userTAG:
			case myTAG:
				return usr.getTag();
			case description:
				return usr.getDescription();
			case email:
				return usr.getEMail();
			case share:
				return ""+usr.getShared();
			case userSSshort:
			case mySSshort:
				return SizeEnum.getReadableSize(usr.getShared());
			case userCID:
			case myCID:
				HashValue cid = usr.getCID();
				if (cid != null) {
					return cid.toString();
				} 
				break;
			case userSID:
			case mySID:
				int sid = usr.getSid();
				if (sid != -1) {
					return AbstractADCHubCommand.SIDToStr(usr.getSid());
				}
				break;
			case userI6: 
			case myI6:
				String inf = formatString.substring(formatString.length()-2);
				INFField inff = INFField.parse(inf);
				if (inff != null) {
					String prop = inff.getProperty(usr);
					if (prop != null) {
						return prop;
					}
				} 
				break;
				
				//end User stuff
				//start File stuff
			case fileFN:
				return file.getName();
			case Directory:
				return file.getOnlyPath();
			case filePath:
				return file.getPath();
			case fileSI:
				if (file.isFile()) {
					return ""+ ((IDownloadableFile)file).getSize();
				}
				if (file instanceof FileListFolder) {
					return ""+ ((FileListFolder)file).getContainedSize();
				}
				return "";
			case fileSIshort:
				if (file.isFile()) {
					return SizeEnum.getReadableSize(((IDownloadableFile)file).getSize());
				}
				if (file instanceof FileListFolder) {
					return SizeEnum.getReadableSize(((FileListFolder)file).getContainedSize());
				}
				return "";
				
			case fileTR:
				if (file.isFile()) {
					return ((IDownloadableFile)file).getTTHRoot().toString();
				} else {
					return "";
				}
			case fileMN:
				if (file.isFile()) {
					return new MagnetLink((IDownloadableFile)file).toString();
				} else {
					return "";
				}
			case type:
				if (file.isFile()) {
					return "File";
				} else {
					return "Directory";
				}
			case hubNI:
				return sc.getHub().getName();
			case hubDE:
				return sc.getHub().getTopic();
			case hubVE:
				return sc.getHub().getVersion();
			case DEFAULT:
				return "%["+formatString+"]";
			}
			
			return "%["+formatString+"]";
			
		
		}
		
	}
	
	private static enum FormatterType {
		USER,MY,HUB,FILE,OTHER;
	}




}
