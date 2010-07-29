package helpers;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import javax.swing.filechooser.FileSystemView;



/**
 * Global Helpers that are useful for everyone..
 * 
 * @author Quicksilver
 *
 */
public final class GH {

	private static final Random rand = new Random();
	
	
	private static final FileSystemView chooser = FileSystemView.getFileSystemView();
	
	// http://wiki.eclipse.org/index.php/Equinox_Launcher#Startup.jar
//	private static final boolean USE_FILE_SYSTEM_VIEW;
//	static {
//		boolean win = Platform.getOS().equals(Platform.OS_WIN32);
//		USE_FILE_SYSTEM_VIEW = !win;
//	}
	/*
    *
    * @see java.util.Random#nextInt(int)
    */
	public static int nextInt(int n) {
		synchronized(rand) {
			return rand.nextInt(n);
		}
	}
	public static int nextInt() {
		synchronized(rand) {
			return rand.nextInt();
		}
	}
	
	
	
	
	private GH() {}
	
	
	/**
	 * provided for all the might use it..
	 * as creating costs memory that is never recovered..
	 * @return a FileSytemView instance..
	 */
	public static FileSystemView getFileSystemView() {
		return chooser;
	}
	
	/**
	 * emulates behaviour of FileSystemView as good as possible...
	 * though its not the same..
	 *  -> done as normal FileSystemView has a memory leak on that method..
	 */
	public static File[] getFiles(File parent,boolean useHidden) {
		File[] files;
//		if (USE_FILE_SYSTEM_VIEW) {
		files = chooser.getFiles(parent, useHidden);
		if (files == null) return new File[0];
		return files;
		
//		} else {
//			files = parent.listFiles();
//			if (files == null) return new File[0];
//				
//			if (useHidden) {
//				int k = 0;
//				for (int i = 0; i+k < files.length; i++) {
//					while (i+k < files.length && files[i+k].isHidden()) {
//						k++;
//					} 
//					if (i+k < files.length) {
//						files[i] = files[i+k];	
//					}
//				}
//				
//				if (k != 0) {
//					File[] onlyVisible = new File[files.length - k];
//					System.arraycopy(files, 0, onlyVisible, 0, onlyVisible.length );
//					return onlyVisible;
//				}
//			}
//			return files;
//		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	public static  <K> K getRandomElement(Collection<K> source) {
		if (source.isEmpty()) {
			return null;
		}
		return (K)source.toArray()[nextInt(source.size())];
	}
	
	public static <K> K getRandomElement(List<K> source) {
		if (source.isEmpty()) {
			return null;
		}
		return source.get(nextInt(source.size()));
	}
	
	/**
	 * closes all provided streams ignoring exceptions
	 * @param closeable
	 */
	public static void close(Closeable... closeable) {
		for (Closeable c: closeable) {
			try {
				if (c != null) {
					c.close();
				}
			} catch (IOException e) {
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void close(Socket s) {
		try {
			if (s != null) {
				s.close();
			}
		} catch (IOException e) {
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	public static void copy(File source,File dest) throws IOException {
		FileInputStream in = null;
		FileChannel sourcec = null; 
		FileChannel destc  = null;
		try {
			in = new FileInputStream(source);
			sourcec = in.getChannel();
			destc = new FileOutputStream(dest).getChannel();
			
			long left = source.length();
			long position= 0;
			long count;

			while(-1 != (count = sourcec.transferTo(position, Math.min(left,1024*64) , destc))){
				position += count;
				left -= count;
				if (left == 0) {
					break;
				}
			}
			destc.force(true);

		} catch (IOException ioe) {
			throw ioe;
		} finally {
			GH.close(destc,sourcec,in);
		}	
		
	}
	
	
	/**
	 * 
	 * @return A List of All network interfaces that can be used to bind ipv4 sockets to...
	 * @throws SocketException if getting addresses from machine fails...
	 */
	public static List<NetworkInterface> getFilteredNIList() throws SocketException {
		
		List<NetworkInterface> nic = new ArrayList<NetworkInterface>();
		
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		
		while(e.hasMoreElements()) {
			NetworkInterface ni = e.nextElement();
			Enumeration<InetAddress> e2 = ni.getInetAddresses();
			boolean use = false;
			while (e2.hasMoreElements()) {
				InetAddress ia = e2.nextElement();
				if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
					use = true;
				}
			}
			if (use) {
				nic.add(ni);
			}
		}
		return nic;
	}
	
	/**
	 * sleeps ignoring interruption handling..
	 * 
	 * @param millis - how long to sleep
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException ie) {}
	}
	
	/**
	 * replaces invalid characters in Filenames
	 */
	public static String replaceInvalidFilename(String filename) {
		for (char replace: new char[]{'\\','/','*','?','<','>','|'}) {
			filename = filename.replace(replace,'.');
		}
		
		filename = filename.replace('\"', '\'');
		filename = filename.replace(':', '-');
		
		for (int i = 0; i < filename.length();i++) {
			if (filename.charAt(i) < 20) {
				filename = filename.substring(0, i)+filename.substring(i+1);
			}
		}

		return filename;
	}
	
	/**
	 * replaces invalid filepath 
	 */
	public static String replaceInvalidFilpath(String filename) {
		filename = filename.replace(File.separator.equals("\\")?'/': '\\', '.');
				
		for (char replace: new char[]{'*','?','<','>','|'}) {
			filename = filename.replace(replace,'.');
		}
		filename = filename.replace('\"', '\'');
		filename = filename.replace(':', '-');    

		return filename;
	}
	
	/**
	 * 
	 * @param filename name of the file
	 * @return ending without the .  hello.pdf -> pdf
	 * empty String if none determined..
	 */
	public static String getFileEnding(String filename) {
		int i= filename.lastIndexOf('.');
		if(i != -1) {
			return filename.substring(i+1);
		} else {
			return "";
		}	
	}
	
	
	/**
	 * replaces newline characters with \n
	 * and \ with \\ 
	 * 
	 * so it does basic escaping
	 * 
	 * @param s
	 * @return
	 */
	public static String replaces(String s) {
		s = s.replace("\\", "\\\\");
		s = s.replace("\n", "\\n");
		return s;
	}
	
	/**
	 * reverses replacements of replace function
	 */
	public static String revReplace(String s) {
		int i = 0;
		while ((i = s.indexOf('\\',i)) != -1) {
			if (s.length() > i+1) {
				char c = s.charAt(i+1);
				if (c == '\\') {
					s = s.substring(0, i)+"\\"+s.substring(i+2) ;
				} else if (c == 'n') {
					s = s.substring(0, i)+"\n"+s.substring(i+2);
				}
			}
			i++;
		}
		
		return s;
	}
	
	public static boolean isLocaladdress(InetAddress ia) {
		return ia.isLoopbackAddress()|| ia.isSiteLocalAddress();
	}
	
	
/*	public static String getStacktrace(Thread t) {
		String s = t.getName();
		for (StackTraceElement ste:t.getStackTrace()) {
			s += "\n"+ste.getFileName()+" Line:"+ste.getLineNumber()+" "+ste.getMethodName();
		}
		return s;
	} */
	
	public static boolean isEmpty(String s) {
		return s.length() == 0;
	}
	
	public static boolean isNullOrEmpty(String s) {
		return s == null || s.length() == 0;
	}
	
	/**
	 * 
	 *help for debugging.. concatenates all .toString() calls in some list fashioned 
	 *way
	 */
	public static String toString(Object[] arr) {
		if (arr == null) {
			return "null";
		}
		return "{"+GH.concat(Arrays.asList(arr), ",", "empty")+"}";
	}
	
	/**
	 * 
	 * @param toFilter what Objects should be filtered..
	 * @param mayNotContain - tested against o.toString()  
	 * @return all which toString() method did not contain mayNoContain
	 */
	public static <K> List<K> filter(List<K> toFilter,String... mayNotContain) {
		ArrayList<K> s = new ArrayList<K>();
		for (K old:toFilter) {
			boolean putIn = true;
			for (String test:mayNotContain) {
				putIn = putIn && !old.toString().contains(test);
			}
			if (putIn) {
				s.add(old);
			}
		}
		return s;
	}
	
	public static <K> void sort(List<K> toSort,final String... sortUpIfContains) {
		Collections.sort(toSort, new Comparator<K>() {
			public int compare(K o1, K o2) {
				int k1 = 0,k2 = 0;
				
				for (int i = 0; i < sortUpIfContains.length ; i++) {
					if (o1.toString().contains(sortUpIfContains[i])) {
						k1+= sortUpIfContains.length-i ;
					}
					if (o2.toString().contains(sortUpIfContains[i])) {
						k2+= sortUpIfContains.length-i;
					}
				}
				
				return -compareTo(k1, k2);
			}
		});
	}
	
	
	public static byte[] concatenate(byte[]... arrays) {
		int totalsize= 0;
		for (byte[] array: arrays) {
			totalsize += array.length;
		}
		byte[] all = new byte[totalsize];
		int currentpos = 0;
		for (byte[] array: arrays) {
			System.arraycopy(array, 0, all, currentpos, array.length);
			currentpos += array.length;
		}
		return all;
		
	}
	
	public static int compareTo(int a , int b) {
		return (a < b ? -1 : (a==b ? 0 : 1));
	}
	public static int compareTo(long a , long b) {
		return (a<b ? -1 : (a==b ? 0 : 1));
	}
	
	public static int compareTo(byte a , byte b) {
		return (a < b ? -1 : (a==b ? 0 : 1));
	}
	
	public static int unsingedCompareTo(byte a,byte b) {
		return compareTo(a & 0xff , b & 0xff);
	}
	
	/**
	 * concatenates  each term in collection using .toString()
	 * and puts between each string "between" 
	 * 
	 * if the map is empty it will return the empty map string instead...
	 * 
	 */
	public static String concat(Iterable<?> terms,String between,String emptyMap) {
		StringBuilder ret = new StringBuilder();
		
		for (Object o: terms) {
			if (ret.length() != 0) {
				ret.append(between);
			} 
			ret.append(o.toString());
		}
		if (ret.length() == 0) {
			return emptyMap;
		} else {
			return ret.toString();
		}
	}
	public static String concat(Object[] terms,String between,String emptyMap) {
		return concat(Arrays.asList(terms), between, emptyMap);
	}
	
	/**
	 * 
	 * @return a string representing all stacktraces..
	 */
	public static String getAllStackTraces() {
		ThreadMXBean threadBean =
	        ManagementFactory.getThreadMXBean(); 
		
		String newLine = System.getProperty("line.separator");
		String s = "";
		for (Entry<Thread,StackTraceElement[]> e :Thread.getAllStackTraces().entrySet()) {
			s += newLine+newLine;
			Thread d = e.getKey();
			ThreadInfo ti = threadBean.getThreadInfo(d.getId());
			
			s += String.format("%s , state: %s ,id: %s",d.toString(),d.getState().toString(),d.getId())+newLine;
			String lock= ti.getLockName();
			if (lock != null) {
				s += String.format("Lock: %s, Count: %s,Time: %s, Owner: %s ", 
						lock,ti.getBlockedCount(),ti.getBlockedTime(),ti.getLockOwnerId());
				s += newLine;
			}

			s += concat(Arrays.asList(e.getValue()),newLine,"empty");
		}
		return s+newLine;
	}

	
	/**
	 * 
	 * @param s the string to search 
	 * @param what what char to be searched
	 * @return the number of occurences of what in s
	 */
	public static int getOccurences(String s , char what) {
		int count = 0,pos = 0;
		
		while (-1 != (pos = s.indexOf(what, pos))) {
			count++;
			pos++;
		}
		
		return count;
	}
	
	/**
	 * 
	 * @param input
	 * @param start
	 * @param length
	 * @return array of size length containing all bytes from start 
	 */
	public static byte[] subarray(byte[] input,int start,int length) {
		byte[] ret = new byte[length];
		System.arraycopy(input, start, ret, 0, length);
		return ret;
	}
	
	/**
	 * tries to give a measure of distance ... 
	 * the higher the distance the further the strings are away from each other..
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static int distance(String a, String b) {
		int lengthDist = Math.abs(a.length()-b.length());
		
		int dist = 0;
		int lastHit = 0;
		for (int i=0; i < a.length();i++) {
			int posB = b.indexOf(a.charAt(i));
			if (posB != -1) {
				boolean fullHit = b.length() > lastHit+1 && b.charAt(lastHit+1) == a.charAt(i);
				if (fullHit) {
					lastHit = lastHit+1;
					dist-=1;
				} else {
					lastHit = posB; //dist+= 0; -> no distance change here..
				}
			} else {
				dist++;
			}
		}
		
		return lengthDist+dist;
	}
	
	
	private static final String HEXES = "0123456789ABCDEF";
	public static String getHex( byte [] raw ) {
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
			.append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}
	
	/**
	 * creates hash with provided algorithm..
	 * throwing illegalStateException if unsuccessful
	 * 
	 * @param tohash bytes to hash
	 * @param alg what algorithm to use
	 * @return the digest output
	 */
	public static byte[] getHash(byte[] tohash,String alg) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance( alg );
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		byte[] raw = md.digest(tohash);
		return raw;
	}
	
//	/**
//	 * increments the mapped counter
//	 * @param <K>  - any value..
//	 * @param mappedCounter - 
//	 * @param k - against what this counter is mapped. if not found counter is create with value 0
//	 * @param increment - value to increment by
//	 */
//	public static <K> void incrementMappedCounter(Map<K,Integer> mappedCounters,K k, int increment) {
//		int i = getValueOfMappedCounter(mappedCounters,k);
//		i += increment;
//		mappedCounters.put(k, i);
//	}
//	
//	public static <K> int getValueOfMappedCounter(Map<K,Integer> mappedCounters,K k) {
//		Integer i = mappedCounters.get(k);
//		if (i == null) {
//			i = 0;
//		}
//		return i;
//	}
	
	/**
	 * switches the provided chars against each other in the provided string
	 * 
	 */
	public static String switchChars(String in,char a,char b) {
		StringBuilder sb = new StringBuilder(in);
		int axorb = a^b; //yeah we do it the clever way sorry KISS ...
		for (int i=0 ; i < sb.length(); i++) {
			int c = sb.charAt(i);
			if (c == a || c == b) {
				sb.setCharAt(i, (char)(c ^ axorb));
			} 
		}
		return sb.toString();
	}
	
	public static void copy(InputStream in,OutputStream out) throws IOException {
		byte[] b = new byte[4096];
		int read;
		while (-1 != (read = in.read(b, 0, b.length))) {
			out.write(b, 0, read);
		}
	}
	
	public static int[] toArray(List<Integer> ints) {
		int[] intar = new int[ints.size()];
		for (int i = 0 ; i < intar.length; i++) {
			intar[i] = ints.get(i);
		}
		return intar;
	}
	
	public static List<Integer> fromArray(int[] ints) {
		List<Integer> intList = new ArrayList<Integer>(ints.length);
		for (int i : ints) {
			intList.add(i);
		}
		return intList;
	}
	
	
	public static BitSet toSet(byte[] bytes) {
		int ammountBits = bytes.length * 8;
		BitSet bits = new BitSet(ammountBits);
		for (int i = 0; i < ammountBits; i++) {
			bits.set(i, getBit(bytes, i));
		}
		return bits;
	}
	
	
	public static boolean getBit(byte[] source,int position) {
		byte b = source[position /8];
		int bitpos = 1 << (position % 8);
		return (bitpos & b)  != 0;
	}
	
	
	public static byte[] toBytes(BitSet bits,int ammountBits) {
		byte[] bytes = new byte[ammountBits/8 ];
		
		for (int i = 0; i < bytes.length; i++) {
			int num = 0;
			for (int j=0; j < 8; j++ ) {
				if (bits.get(i*8+j)) {
					num +=  1 << j; 
				}
			}
			bytes[i] = (byte)num;
		}
		
		return bytes;
	}
	
	public static byte[] toBytes(BitSet bits) {
		int ammountBits = bits.length() ;
		return toBytes(bits, ammountBits + (ammountBits%8 == 0 ?0:1));
	}
	
	
}
