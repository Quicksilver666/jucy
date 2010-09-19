package uc.protocols;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.regex.Pattern;


import uc.crypto.TigerHashValue;

public interface IProtocolCommand<T extends ConnectionProtocol> {

	/**
	 * pattern describing a nick..
	 */
	public static final String NMDCNICK = "(?:[^\\s\\$<>]+)";
	
	public static final String BASE32CHAR = "[A-Z2-7]";
	
	public static final String TEXT = "(?:[^|]*)";
	
	public static final String TEXT_NOSPACE = "(?:[^\\x20]*)";
	public static final String TEXT_NONEWLINE_NOSPACE = "(?:[^\\x20\\x0C\\x0A]*)";
	public static final String ADCTEXT = TEXT_NOSPACE;
	public static final String TEXT_NODOLLAR = "(?:[^$]*)";;
	
	
	public static final String ESCAPED_PIPE = "(?:\\Q&#124;\\E)";
	
	public static final String BYTE 	= "(?:(?:25[0-5])|(?:2[0-4]\\d)|(?:[01]?\\d\\d?))"; 
	public static final String IPv4 	=  "(?:(?:"+BYTE+"\\.){3}"+BYTE+")";
	public static final String TWOHEXBYTES =  "(?:[a-fA-F0-9]{0,4})";
	public static final String IPv6		= 
		"(?:"+TWOHEXBYTES+"(?::"+TWOHEXBYTES+"){0,5}(?(?::"+TWOHEXBYTES+"){2}|(?::"+IPv4+")))"; //simple IPv6 with embedded IPv4
	
		
	   
	public static final String PORT	= "(?:(?:6553[0-5])|(?:655[0-2]\\d)|(?:65[0-4]\\d\\d)|(?:6[0-4]\\d{3})|(?:[1-5]?\\d{1,4}))";

	public static final String FILESIZE = "(?:\\d{1,18})"; //nearly a long ... though should be enough for any kind of files..
	
	public static final String SHORT =   "(?:(?:3276[0-7])|(?:327[0-5]\\d)|(?:32[0-6]\\d\\d)|(?:3[01]\\d{3})|(?:[0-2]?\\d{1,4}))";  //0 - 2^15-1  = 32767
	
	/**
	 * positive integer
	 */
	public static final String INT	= "(?:(?:214748364[0-7])|(?:21474836[0-3]\\d)" 
										+"|(?:2147483[0-5]\\d{2})"	+"|(?:214748[0-2]\\d{3})"
										+"|(?:21474[0-7]\\d{4})"	+"|(?:2147[0-3]\\d{5})"
										+"|(?:214[0-6]\\d{6})"		+"|(?:21[0-3]\\d{7})"
										+"|(?:20\\d{8})"			+"|(?:[01]?\\d{1,9}))";
	
										
	//192 Bit digest -> 39 chars in base32 though the last char has less possibilities only QYAI
	public static final String TTH 	= TigerHashValue.TTHREGEX;
	public static final String CID 	= TTH;
	/**
	 * hashvalue in base32 with some type prefix like sha256/ or TTH/
	 */
	public static final String HASH_WITH_TYPE = "(?:\\w+/[A-Z2-7]+)";
	
	public static final String COMPRESSION = "(?:(?: ZL1)|(?: BZ2)|(?:))";
	
	
	public static final String SID = "(?:[A-Z2-7]{4})";
	
	public  static final Pattern space = Pattern.compile(" "); 


	
//	public static final String FileChar = "[^\\|\\?\\*<\":>/]";
	/**
	 * only allows windows filenames.
	 */
	//public static final String FILENAME = "[^\\|\\?\\*<\">/]{1,255}";
	
	/**
	 * 
	 * @return a prefix representing the beginning of 
	 * the string.. usually everything before the first space char
	 */
	String getPrefix();
	
	/**
	 * command should try to handle the received string command
	 * @param command - the string representing the command
	 * @throws IOException - if some exception occurs.
	 */
	<K extends T> void handle(K k,String command) throws ProtocolException , IOException;
	
	/**
	 * 
	 * @param command
	 * @return if the provided string can be parsed by this command
	 */
	boolean matches(String command);
	
}
