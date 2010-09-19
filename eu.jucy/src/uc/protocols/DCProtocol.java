package uc.protocols;




import uc.FavHub;
import uc.crypto.HashValue;
import uc.crypto.Tiger;

import uc.protocols.hub.Hub;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public abstract class DCProtocol extends ConnectionProtocol {

	private static Logger logger = LoggerFactory.make(); 
	
	public static final String PROTOCOL_EXTENSIONPOINT = "eu.jucy.protocol";

	public static final String NMDC_CHARENCODING = "windows-1252";
	
	public static final Charset NMDC_CHARSET;
	
	public static final String ADC_CHARENCODING = "utf-8";
	
	public static final Charset ADC_CHARSET;
	
	static {
		NMDC_CHARSET = Charset.forName(NMDC_CHARENCODING);
		ADC_CHARSET = Charset.forName(ADC_CHARENCODING);
	}
	
	public static final Pattern NMDCCommand = Pattern.compile("([^|]+)\\|");
	public static final Pattern ADCCommand = Pattern.compile("([^\\n]+)\\n");
	
	public static final Pattern NMDC_PREFIX_PATTERN = Pattern.compile("(\\$\\S+)[^|]*");
	public static final Pattern ADC_PREFIX_PATTERN = Pattern.compile("[BCDEFHIU]([A-Z][A-Z0-9]{2})[^\\n]*");
	public static final Pattern NMDC_AND_ADC_PREFIX_PATTERN = Pattern.compile("[BCDEFHIU]?(\\$?\\S+)[^\\|\\n]*");
	
	public static final Pattern NMDCANDADCCommand = Pattern.compile("([^|\\n]+)[\\|\\n]");

	/**
	 * variable to signal if this hub is running NMDC protocol..
	 * true if NMDC protocol is running
	 * false if ADC protocol is running
	 * 
	 *  default is NMDC
	 */
	protected Boolean nmdc = true;

	public DCProtocol() {
		setCharset(NMDC_CHARSET);
	}
	public DCProtocol(int[] perfPref) {
		super(perfPref);
		setCharset(NMDC_CHARSET);
	}
	
	/**
	 * sets DC char-set 
	 * based on NMDC variable
	 */
	protected void setCharSet() {
		setCharset(isNMDC()? NMDC_CHARSET:ADC_CHARSET);
	}
	
	
	
	public void setProtocolNMDC(Boolean nmdc) {
	//	if (this.nmdc == null || !this.nmdc.equals(nmdc)) {
			this.nmdc = nmdc;
			setCharSet();
			setPrefixPattern();
	//	}
	}
	
	private void setPrefixPattern() {
		if (nmdc == null) {
			setPrefix(NMDC_AND_ADC_PREFIX_PATTERN);
		} else  {
			setPrefix(nmdc?NMDC_PREFIX_PATTERN: ADC_PREFIX_PATTERN);
		}
	}
	
	public boolean isNMDC() {
		return nmdc == null || nmdc;
	}
	
	public Boolean getNMDC() {
		return nmdc;
	}

	@Override
	public Pattern getCommandRegexPattern() {
		if (nmdc == null) {
			return NMDCANDADCCommand;
		}
		return nmdc ? NMDCCommand :ADCCommand;
	}
	
	/**
	 * stop byte per command..
	 */
	public int getCommandStopByte() {
		return nmdc == null||nmdc ? '|' : '\n';
	}
	
	
/*	public static void main(String[] args) throws UnsupportedEncodingException {
		
		Random rand = new Random();
		
		for (int i = 0 ; i < 5000 ; i ++) {
			String lock = "$Lock EXTENDEDPROTOCOL::This_hub_was_written_by_Yoshi::CTRL[";
			String seperating = "";
			for (int x = 0; x < 5; x++) {
				int num =rand.nextInt(256);
				lock += (char)num;
				seperating+= num+",";
			}
			lock += "] Pk=YnHub|"; 
			
			byte[] key = generateKey(lock,NMDCCHARENCODING).getBytes(NMDCCHARENCODING);
			byte[] key2 = generateKey(lock, NMDCCHARSET);
			
			boolean valid = key.length == key2.length;
			for (int s = 0 ; s < key.length && valid ; s++) {
				valid &= key[s] == key2[s];
			}
			
			if (!valid) {
				System.out.println("Invalid: "+seperating+"\n" 
						+ new String(key,NMDCCHARENCODING)+"\n"
						+ new String(key2,NMDCCHARENCODING)+"\n");
			}
			
		}
		
	} */


	// ------------------------------------------------------------------------------
	// ------ Lock part START
	// --------------------------------------------------------
	// ------------------------------------------------------------------------------
	// Mysterious method to Compute a Key String from a Lock String
	public static byte[] generateKey(String lockstr,Charset cs) throws UnsupportedEncodingException ,ProtocolException{ 
		int firstpos = lockstr.indexOf(' ');
		int lastpos = lockstr.indexOf(" Pk=");
		if (lastpos < 0) {
			lastpos = lockstr.indexOf(' ', firstpos+1);
		}
		if (firstpos < 0 || lastpos < 0 || firstpos >= lastpos) {
			throw new ProtocolException("Invalid Lock received: "+lockstr);
		}
		
		byte[] lock = lockstr.substring(firstpos+1, lastpos).getBytes(cs.name()); 

		byte[] key =  new byte[lock.length];

		for (int i = 1; i < lock.length; i++) {
			key[i] = (byte) ((lock[i] ^ lock[i - 1]) & 0xFF);
		}

		key[0] = (byte) ((((lock[0] ^ lock[lock.length - 1]) ^ lock[lock.length - 2]) ^ 5) & 0xFF);

		for (int i = 0; i < key.length; i++) {
			key[i] = (byte) ((((key[i] << 4) & 0xF0) | ((key[i] >> 4) & 0x0F)) & 0xFF);
		}
		
		
		
		byte[] dcnEncoded = dcnEncode(key,cs);
		
		
		byte[] completeKey = new byte[dcnEncoded.length + 6];
		completeKey[0] = '$';
		completeKey[1] = 'K';
		completeKey[2] = 'e';
		completeKey[3] = 'y';
		completeKey[4] = ' ';
		completeKey[completeKey.length-1] = '|';
		
		System.arraycopy(dcnEncoded, 0, completeKey, 5, dcnEncoded.length);
		
		
		return completeKey;
		
	}
	

	

	
	private static byte[] dcnEncode(byte[] lock,Charset cs) throws UnsupportedEncodingException {
		
		List<Byte> encoded = new ArrayList<Byte>();
		
		for (byte b: lock ) {
			if (b == 0 || b == 5 || b == 36 || b == 96 || b == 124 || b == 126 ) {
				String paddedDecimal= "/%DCN"+String.format("%03d", (int)b)+"%/";  
				
				for (byte dcn :  paddedDecimal.getBytes(cs.name()) ) {
					encoded.add(dcn);
				}
	
			} else {
				encoded.add(b);
			}
		}
		
		byte[] encodedBytes = new byte[encoded.size()];
		
		for (int i = 0; i < encodedBytes.length ; i++) {
			encodedBytes[i] = encoded.get(i);
		}
		
		return encodedBytes;
	}


	// ------------------------------------------------------------------------------
	// -------- Lockpart END
	// --------------------------------------------------------
	// ------------------------------------------------------------------------------

	
	
	public void onLogIn() throws IOException {
		super.onLogIn();
		loadCommands(false);
	}

	@Override
	public void beforeConnect() {
		super.beforeConnect();
		loadCommands(true);
	}
	
	
	/**
	 * 
	 * @param connect true for on connect
	 *   false for login
	 */
	private void loadCommands(boolean connect) {
		IExtensionRegistry reg = Platform.getExtensionRegistry();

		IConfigurationElement[] configElements = reg
				.getConfigurationElementsFor(PROTOCOL_EXTENSIONPOINT);

		for (IConfigurationElement element : configElements) {
			if (getClass().getName().equals(element.getAttribute("target"))
					&& Boolean.valueOf(element.getAttribute("nmdc")).equals(nmdc)
					&& Boolean.parseBoolean(element
							.getAttribute(connect ? "active_during_login"
									: "active_after_login"))) {

				IConfigurationElement[] commands = element
						.getChildren("command");
				for (IConfigurationElement command : commands) {

					try {
						@SuppressWarnings("unchecked")
						IProtocolCommand<? extends ConnectionProtocol> prot = 
							(IProtocolCommand<? extends ConnectionProtocol>) command
								.createExecutableExtension("commandClass");

						addCommand(prot);
					} catch (CoreException e) {
						logger.error(e, e);
					}

				}

			}
		}
	}
	
	
	public static String doReplaces(String toReplace) { // Replaces DC protocol
														// relevant chars
		return toReplace.replace("&#", "&amp;#").replace("$", "&#36;").replace(
				"|", "&#124;");
	}

	public static String reverseReplaces(String toReplace) {
		return toReplace.replace("&#124;", "|").replace("&#36;", "$").replace(
				"&amp;#", "&#");
	}

	/**
	 * convenience method reverses NMDC replaces in all strings of the array
	 * 
	 * @param toReplace -
	 *            the array to do the replaces..
	 * @return - an array containing the same string just reverse replaced
	 */
	public static String[] reverseReplaces(String[] toReplace) {
		for (int i = 0; i < toReplace.length; i++) {
			toReplace[i] = reverseReplaces(toReplace[i]);
		}
		return toReplace;
	}

	/*
	 * one-way function to compute a "artificial" uid for an nmdc user @param
	 * nick the nick of the user @param hubid the hubid of the user @return the
	 * unique userid
	 * 
	 * @deprecated use different method without int for hubid
	 * 
	 * 
	 * public static HashValue nickToUserID(String nick, int hubid){ return
	 * Tiger.tigerOfString(hubid+"$$"+nick); }
	 */
	
	public static HashValue nickToUserID(String nick, FavHub hub) {
		return Tiger.tigerOfString(hub.getSimpleHubaddy() + "$$" + nick);
	}

	public static HashValue nickToUserID(String nick, Hub hub) {
		return nickToUserID(nick, hub.getFavHub());
	}
	
	public static HashValue CIDToUserID(HashValue cid, Hub hub) {
		return CIDToUserID(cid,hub.getFavHub());
	}
	
	public static HashValue CIDToUserID(HashValue cid, FavHub hub) {
		return Tiger.tigerOfString(cid.toString()+"$"+hub.getSimpleHubaddy());
	}



	@Override
	protected void registerListenerFirst(IProtocolStatusChangedListener listener) {
		super.registerListenerFirst(listener);
	}
	

}