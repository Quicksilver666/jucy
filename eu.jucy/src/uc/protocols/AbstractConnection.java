package uc.protocols;

import helpers.GH;

import java.io.Closeable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;



import java.util.Enumeration;
import java.util.regex.Pattern;



import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;




import uc.ICryptoManager;
import uc.PI;



/**
 * base class for all connections..
 * 
 * @author Quicksilver
 *
 */
public abstract class AbstractConnection implements Closeable, IConnection {

	private static Logger logger = LoggerFactory.make(Level.DEBUG); 
	
	
	protected final ConnectionProtocol cp; //our Connection protocol..
	
	protected volatile CharsetDecoder charsetDecoder;
	protected volatile CharsetEncoder charsetEncoder;
	
	protected final ICryptoManager cryptoManager;
	
	
	public AbstractConnection(ICryptoManager cryptoManager,ConnectionProtocol cp) {
		this.cryptoManager = cryptoManager;
		this.cp = cp;
	}
	
	protected InetSocketAddress addytransformation(String addy){    //transforms an url string to ip/dns and port
		String[] a = addy.split(Pattern.quote(":"));
		if (a.length == 1) {
			a = new String[]{a[0], ""+cp.defaultPort};
		}
		return new InetSocketAddress(a[0], new Integer(a[1])  ); 
		
	}
	
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#start()
	 */
	public abstract void start();
	

	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#send(java.lang.String)
	 */
	public abstract void send(String toSend) throws IOException ;
	
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#send(java.nio.ByteBuffer)
	 */
	public abstract void send(ByteBuffer toSend) throws IOException ;
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#reset(java.nio.channels.SocketChannel)
	 */
	public abstract void  reset(SocketChannel soChan);
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#reset(java.net.InetSocketAddress)
	 */
	public abstract void reset(InetSocketAddress addy);
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#reset(java.lang.String)
	 */
	public void reset(String addy) {
		reset(addytransformation(addy));
	}
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#close()
	 */
	public abstract void close();
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#getInetSocketAddress()
	 */
	public abstract InetSocketAddress getInetSocketAddress();
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#isLocal()
	 */
	public boolean isLocal(){
		InetAddress ia= getInetSocketAddress().getAddress();
		return GH.isLocaladdress(ia);
	}
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#retrieveChannel()
	 */
	public abstract ByteChannel retrieveChannel() throws IOException;
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#returnChannel(java.nio.channels.ByteChannel)
	 */
	public abstract boolean returnChannel(ByteChannel sochan);
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#flush(int)
	 */
	public abstract boolean flush(int miliseconds);
	
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#setIncomingDecompression(uc.protocols.Compression)
	 */
	public abstract void setIncomingDecompression(Compression comp) throws IOException;
	

	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#refreshCharsetCoders()
	 */
	public void refreshCharsetCoders() {
		charsetDecoder = cp.getCharset().newDecoder();
		charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE);  
		charsetEncoder = cp.getCharset().newEncoder();
		charsetEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		charsetEncoder.onMalformedInput(CodingErrorAction.REPLACE); 
	}
	
	/* (non-Javadoc)
	 * @see uc.protocols.IConnection#usesEncryption()
	 */
	public abstract boolean usesEncryption();
	
	
	/**
	 * binds an InetSocketChannel to the given interface name... 
	 * or doesn't bind it if name could not be found ..
	 * 
	 * @param interfacename - which interface to bind to ..
	 * @param sochan - which socket channel to bind
	 * @param bindPort - 0 for system decision  otherwise 1-65k 
	 */
	public static void bindSocket(String inetaddress,SocketChannel sochan) throws IOException {
		bindSocket(inetaddress, sochan,0);
	}
	
	public static void bindSocket(SocketChannel sochan) throws IOException {
		bindSocket(PI.get(PI.bindAddress),sochan);
	}
	
	private static volatile String unresolveableBindAddrees;
	
	/**
	 * 
	 * @return currently set bind address... null if none...
	 */
	public static InetAddress getBindAddress(String bindAddressString) throws SocketException {
		if (GH.isNullOrEmpty(bindAddressString)) {
			return null;
		}
		logger.trace("Trying to resolve: "+bindAddressString);
		NetworkInterface ni = NetworkInterface.getByName(bindAddressString);
		
		if (ni != null) {
			Enumeration<InetAddress> ias = ni.getInetAddresses();
			while (ias.hasMoreElements()) {
				InetAddress ia = ias.nextElement();
				if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
					logger.trace("NI: "+ia);
					unresolveableBindAddrees = null;
					return ia;
				}
			}
		} else {
			try {
				InetAddress ina = InetAddress.getByName(bindAddressString);
				logger.trace("inet address: "+ina);
				if (NetworkInterface.getByInetAddress(ina) != null) {
					unresolveableBindAddrees = null;
					return ina;
				}
			} catch(UnknownHostException uhe) {
				if (!bindAddressString.equals(unresolveableBindAddrees)) {
					logger.warn("Bind address could not be resolved: "+bindAddressString,uhe);
					unresolveableBindAddrees = bindAddressString;
				}
			}
		}

		return null;
	}
	
	public static void bindSocket(String inetaddress,ServerSocketChannel sochan,int bindPort) throws IOException {
		bindSocket(inetaddress, (Object)sochan,bindPort);
		if (!sochan.socket().isBound()) {
			sochan.socket().bind(new InetSocketAddress(bindPort));
		}
	}
	
	public static void bindSocket(ServerSocketChannel sochan, int bindPort) throws IOException {
		bindSocket(PI.get(PI.bindAddress), sochan,bindPort);
	}
	
	private static void bindSocket(String bindAddressString,Object sochan,int bindPort) throws IOException {
		InetAddress ia = getBindAddress(bindAddressString);
		if (ia != null) {
			InetSocketAddress isa = new InetSocketAddress(ia, bindPort);
			if (sochan instanceof SocketChannel) {
				((SocketChannel)sochan).socket().bind(isa);
			} else if (sochan instanceof ServerSocketChannel) {
				((ServerSocketChannel)sochan).socket().bind(isa);
			}
		}
	}
	
}
