package uc.protocols;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CopyOnWriteArraySet;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;



import uc.PI;
import uc.protocols.MultiStandardConnection.IUnblocking;


/**
 * encapsulation for Socks5 protocol..
 * 
 * 
 * @author Quicksilver
 *
 */
public class Socks {
	
	
	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	private static Socks defaultSocks;
	
	private static final CopyOnWriteArraySet<UDPRelay> RELAYS = new CopyOnWriteArraySet<UDPRelay>();
	static {
		updateSocks();
		new PreferenceChangedAdapter(PI.get(),PI.socksProxyEnabled,PI.socksProxyHost,PI.socksProxyPassword,PI.socksProxyPort,PI.socksProxyUsername) {
			@Override
			public void preferenceChanged(String preference, String oldValue,
					String newValue) {
				updateSocks();
			}
		};
	}
	
	private synchronized static void updateSocks() {
		for (UDPRelay u:RELAYS) {
			u.close();
		}
		boolean e = PI.getBoolean(PI.socksProxyEnabled) &&  !GH.isEmpty(PI.get(PI.socksProxyHost));
		System.setProperty("socksProxyHost", e ? PI.get(PI.socksProxyHost): "");
		System.setProperty("socksProxyPort", e ? PI.get(PI.socksProxyPort): "");
		System.setProperty("java.net.socks.username", e? PI.get(PI.socksProxyUsername):"");
		System.setProperty("java.net.socks.password",  e? PI.get(PI.socksProxyPassword):"");
		
		if (e) {
			InetSocketAddress proxy = new InetSocketAddress(
					PI.get(PI.socksProxyHost),PI.getInt(PI.socksProxyPort));
			defaultSocks = new Socks(PI.get(PI.socksProxyUsername),PI.get(PI.socksProxyPassword),proxy);
		} else {
			defaultSocks = null;
		}
	}
	
	/**
	 * 
	 * @return an instance of Socks as set in the jucy settings,
	 * or null if using Socks is currently disabled..
	 */
	public synchronized static Socks getDefaultSocks() {
		return defaultSocks;
	}
	public synchronized static boolean isEnabled() {
		return defaultSocks != null;
	}
	
	
	private static final byte NO_AUTH = 0x00, USERNAME_PW = 0x02;
	
	private final String username;
	private final String password;
	private final InetSocketAddress socksServerAddy;
	
	public Socks(String username,String password, InetSocketAddress socksServer) {
		this.username = username;
		this.password = password;
		this.socksServerAddy = socksServer;
	}
	
	/**
	 * creates connection to a specified target address
	 * 
	 * @param target - where we want that our data go to
	 * @param socksServer - a connection to an existing Server
	 * @return
	 */
	public void connect(SocketChannel unconnectedSocket,InetSocketAddress target) throws IOException {
		if (!unconnectedSocket.isBlocking()) {
			unconnectedSocket.configureBlocking(true);
		}
		
		connectAndAuth( unconnectedSocket );
		InetAddress ia = target.getAddress();
		request(unconnectedSocket,CMD.CONNECT,ia,target.getPort());
		readResponse(unconnectedSocket,ia);
		
		//if we reach here... then all went well
	}
	
	public UDPRelay openUDPRelay() throws IOException {
		UDPRelay relay = new UDPRelay();
		relay.connect();
		return relay;
	}
	
	private void connectAndAuth(SocketChannel unconnectedSocket) throws IOException {

		unconnectedSocket.socket().setSoTimeout(5000);
		
		unconnectedSocket.connect(socksServerAddy);
	//	byte[] b;
		int read;
		boolean pAuth = !GH.isNullOrEmpty(username) && !GH.isNullOrEmpty(password);
		ByteBuffer auth = ByteBuffer.allocate(4);
		auth.put((byte)0x05).put((byte)(pAuth?0x02:0x01)).put(NO_AUTH);
		
		if (pAuth) {  // start hello
			auth.put(USERNAME_PW);
		}
		auth.flip();
		unconnectedSocket.write(auth);
		
		ByteBuffer methodSelected = ByteBuffer.allocate(2); // new byte[2];
		
		read = unconnectedSocket.read(methodSelected);
		if (read != 2) {
			throw new IOException();
		}
		
		if (methodSelected.get(0) == 5) {
			switch(methodSelected.get(1)) {
			case NO_AUTH:
				break;
			case USERNAME_PW:
				/*
				 * The client's authentication request is:
			     * field 1: version number, 1 byte (must be 0x01)
			     * field 2: username length, 1 byte
			     * field 3: username
			     * field 4: password length, 1 byte
			     * field 5: password
				 */
				if (pAuth) {
					
					byte[] usr = username.getBytes();
					byte[] pass= password.getBytes();
					ByteBuffer buf = ByteBuffer.allocate(usr.length+pass.length+3 );
					
					buf.put((byte)0x01).put((byte)usr.length).put(usr).put((byte)pass.length).put(pass).flip();
					unconnectedSocket.write(buf);
					
					ByteBuffer authSuccess = ByteBuffer.allocate(2);
					read = unconnectedSocket.read(authSuccess);
					if (read != 2 || authSuccess.get(1) != 0) {
						throw new IOException("Auth Failed! "+read+ " "+authSuccess.get(1));
					}
				} else {
					throw new IOException("No Password Auth possible");
				}
				break;
			default:
				throw new IOException("Authentication Failed: "+methodSelected.get(1));
			}
		}
	}
	
	private void request(SocketChannel sc,CMD c,InetAddress ia, int port) throws IOException {
		boolean ipv4 = ia instanceof Inet4Address;
		//now open up the socket...
		byte[] head =  new byte[]{5,c.getVal(),0, (byte)(ipv4? 1:6)};
		
		ByteBuffer send = ByteBuffer.allocate(head.length+ ia.getAddress().length+2);
		send.put(head).put(ia.getAddress()).putShort((short)port).flip();
		sc.write(send);
	}
	
	private InetSocketAddress readResponse(SocketChannel sc,InetAddress originalSentIA) throws IOException {
		ByteBuffer response = ByteBuffer.allocate(6+originalSentIA.getAddress().length);
		
		int read = sc.read(response);
		response.flip();
		if (read != response.capacity()) {
			throw new IOException("unexpected length for response! "+read);
		}
		
		if (response.get(1) != 0x00) { //if not successful throw Error
			String error= "";
			switch(response.get(1)) {
			case 1: error = "General SOCKS server failure"; break;
			case 2: error = "connection not allowed by ruleset"; break;
			case 3: error = "Network unreachable"; break;
			case 4: error = "Host unreachable"; break;
			case 5: error = "Connection refused"; break;
			case 6: error = "TTL expired"; break;
			case 7: error = "Command not supported"; break;
			case 8: error = "Address type not supported"; break;
			default: error = "Unknown: "+response.get(1); break;
			}
			throw new IOException("Error: "+error);
		}
		byte[] address = new byte[originalSentIA.getAddress().length];
		response.position(4);
		response.get(address);
		

		InetAddress ia = InetAddress.getByAddress(address);
		response.position(response.limit()-2);
		int port = response.getShort() & 0xffff;
		
		return new InetSocketAddress(ia,port);
	}
	
	private static enum CMD {
		CONNECT(1),BIND(2),UDP_ASC(3);
		
		CMD(int val) {
			this.val = (byte)val;
		}
		private final byte val;
		
		public byte getVal() {
			return val;
		}
	}
	
	public class UDPRelay implements IUnblocking  {
		private InetSocketAddress udpServer;
		private SocketChannel connectionToSocks;
		private SelectionKey key;
		
		private DatagramChannel datagramChannel;
		
		private UDPRelay() {}
		
		/**
		 * closes the tcp socket associated with the Socks server..
		 * so the UDP connection is finally closed..
		 */
		public synchronized void close() {
			GH.close(connectionToSocks);
			if (key != null) {
				key.cancel();
			}
			RELAYS.remove(this);
		}
		
		public synchronized void connect() throws IOException {
			connectionToSocks = SocketChannel.open();
			connectAndAuth( connectionToSocks );
			InetAddress ia = InetAddress.getByAddress(new byte[]{0,0,0,0});
			request(connectionToSocks,CMD.UDP_ASC,ia ,0);
			udpServer = readResponse(connectionToSocks, ia);
			
			logger.debug("Socketaddy fo UDP Server of Socks: "+udpServer);
			
			connectionToSocks.configureBlocking(false);
			MultiStandardConnection.get().register(connectionToSocks, this, true);
			
			RELAYS.add(this);
			
			datagramChannel = DatagramChannel.open();
			datagramChannel.socket().bind(null); //bind to random port...
			datagramChannel.configureBlocking(true);
			datagramChannel.connect(udpServer);
		}
		
		/**
		 * sends the provided packet over the UDP relay server..
		 * 
		 * @param toSend - packet to send
		 * @param target - where the packet should be targeted to..
		 */
		public synchronized void send(byte[] toSend,InetSocketAddress target) {
			ByteBuffer buf = ByteBuffer.allocate(toSend.length+6 + target.getAddress().getAddress().length);
			buf.putShort((short)0); //reserved
			buf.put((byte)0); //fragment number always zero
			byte[] address = target.getAddress().getAddress();
			buf.put((byte) (address.length == 4 ? 0x01: 0x04) ); //address type
			buf.put( address );  //address
			buf.putShort((short)target.getPort()); // port
			buf.put(toSend); // data
			buf.flip();
			
			sendPacket( buf , udpServer );
		}

		private void sendPacket(ByteBuffer packet,InetSocketAddress target ){
	    	if (0 <  target.getPort() && target.getPort() <= 65535) {
	    	    logger.debug("sending udp packet: "+target+ " "+ packet);
	    	    try {
	    	    	int i = datagramChannel.send(packet, target);
	    	    	logger.debug("sent bytes: "+i);
	    	    } catch(BindException be) {
	    	    	logger.debug("Packet could not be send: "+be,be);
	    	    } catch (IOException ioe) {
	    	    	logger.warn(ioe+" packet: "+target+"  "+new String(packet.array()),ioe);
	    	    }
	    	}
	    }

		/**
		 * disconnects should never happen -> may be 
		 * TODO reconnect on next UDP packet..
		 */
		public void onDisconnect() throws IOException {
			throw new IllegalStateException();
		}

		/**
		 * read method -> not used...
		 */
		public void read() throws IOException {
			ByteBuffer readBytes = ByteBuffer.allocate(1024);
			int read = connectionToSocks.read(readBytes);
			if (read > 0) {
				readBytes.flip();
				logger.debug("Read bytes from Socks: "+new String(readBytes.array()));
			} else {
				close();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see uc.protocols.MultiStandardConnection.IUnblocking#setKey(java.nio.channels.SelectionKey)
		 */
		public synchronized void setKey(SelectionKey key) {
			this.key = key;
		}

		/*
		 * (non-Javadoc)
		 * @see uc.protocols.MultiStandardConnection.IUnblocking#write()
		 */
		public void write() throws IOException {
			throw new IllegalStateException();
		}
		
		/*
		 * (non-Javadoc)
		 * @see uc.protocols.MultiStandardConnection.IUnblocking#connected()
		 */
		public void connected() {
			throw new IllegalStateException();
		}
	}
}
