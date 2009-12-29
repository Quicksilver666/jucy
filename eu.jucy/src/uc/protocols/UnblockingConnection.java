package uc.protocols;

import helpers.GH;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;


import logger.LoggerFactory;
import uc.DCClient;
import uc.protocols.MultiStandardConnection.IUnblocking;

/**
 * more or less the replacement for NewStandardConnection
 * 
 * @author Quicksilver
 *
 */
public class UnblockingConnection extends AbstractConnection implements IUnblocking {
	
	private static final Logger logger = LoggerFactory.make();

	/**
	 * here we store Ip addresses of other clients that failed DHE key exchange with us..
	 */
	private static final Set<InetAddress> problematic = Collections.synchronizedSet(new HashSet<InetAddress>());
	
	private final boolean encryption;


	private final boolean serverSide;
	private volatile SSLEngine engine;
	private volatile boolean connectSent = false;
	
	private volatile boolean disconnectSent = false;
	
	private volatile SelectionKey key; 
	private volatile boolean blocking = false;
	

	
	public void setKey(SelectionKey key) {
		this.key = key;
	}
	
	private static final Object inetAddySynch = new Object();
	private volatile InetSocketAddress inetAddress = null ; 

	private final Object bufferLock = new Object();
	
	private volatile ByteBuffer byteBuffer = ByteBuffer.allocate(1024*2); //in Buffer.. 
	private final CharBuffer outcharBuffer = CharBuffer.allocate(1024);
	private final ByteBuffer outBuffer = ByteBuffer.allocate(outcharBuffer.capacity()*4);
	
	private ByteBuffer encrypting, decrypting; //bytebuffers for encrypting and decrypting data..
	
	private final VarByteBuffer varOutBuffer = new VarByteBuffer(1024*4); //replaces the outgoing stringbuffer..
	private final VarByteBuffer varInBuffer = new VarByteBuffer(1024*4);

	private final Semaphore semaphore = new Semaphore(0,false);
	
	private Object target; // socketchannel... or hubaddy in string format
	
	
	/**
	 * store for unfinished commands..
	 */
	private final StringBuffer stringbuffer = new StringBuffer();
	


	
	/**
	 * used for client as well as server mode.
	 */
	public UnblockingConnection(SocketChannel soChan, ConnectionProtocol connectionProt,boolean encryption, boolean serverSide) {
		super(connectionProt);
		this.encryption = encryption;
		this.serverSide = serverSide;
		this.target = soChan;
	
	}
	
	
	
	/**
	 * only used for client mode
	 * @param addy
	 * @param connectionProt
	 * @param encryption
	 * @param allowDH - can forbid DH keys due to problems with DH and other clients... especialyl apex..
	 */
	public UnblockingConnection(String addy, ConnectionProtocol connectionProt,boolean encryption) {
		super(connectionProt);
		this.encryption = encryption;
		this.target = addy;
		serverSide = false;

	}
	
	public void start() { //todo ... if Proxy in use start connect should be in seperate thread..
		if (target instanceof String) {
			reset((String)target);
		} else if (target instanceof SocketChannel) {
			reset((SocketChannel)target);
		} else if (target instanceof InetSocketAddress) {
			reset((InetSocketAddress)target);
		}
	}
	public UnblockingConnection(InetSocketAddress isa, ConnectionProtocol connectionProt,boolean encryption){
		super(connectionProt);
		this.target = isa;
		this.encryption = encryption;
		this.serverSide = false;
	} 
	
	/**
	 * @param op -  marks the key if it is valid for the provided op
	 */
	private void addInterestOp(final int op) {
		MultiStandardConnection.get().asynchExec(new Runnable() {
			public void run() {
				if (key != null && key.isValid()) {
					key.interestOps(key.interestOps()| op);
				}
			}
		});
	}
	
	public void send(String toSend) {
		logger.debug("send("+toSend+")");

		if (charsetEncoder == null) {
			refreshCharsetCoders();
		}
		synchronized (charsetEncoder) {
			outcharBuffer.clear();
			if (toSend.length() < outcharBuffer.remaining()) {
				outcharBuffer.put(toSend).flip();
				outBuffer.clear();
				charsetEncoder.encode(outcharBuffer, outBuffer, true);
				outBuffer.flip();
				send(outBuffer);

			} else {
				send(toSend.substring(0, toSend.length()/2));
				send(toSend.substring(toSend.length()/2));
			}
		}
		
		
	}
	
	public void send(ByteBuffer toSend) {
		synchronized (bufferLock) {
			if (encryption) {
				try {
					encrypting.clear();
					SSLEngineResult ssler = engine.wrap(toSend, encrypting);
					encrypting.flip();
					varOutBuffer.putBytes(encrypting);
					evaluateHandshakeStatus(ssler.getHandshakeStatus());
				
				} catch(RuntimeException re) {
					addProblematic(re);
				} catch(SSLException ssle) {
					logger.debug(ssle, ssle);
				}
				
			} else {
				varOutBuffer.putBytes(toSend);
			}
		}
		if (!blocking) {
			addInterestOp(SelectionKey.OP_WRITE);
		}
	}
	
	
	public void write() throws IOException {
		synchronized (bufferLock) {
			if (varOutBuffer.hasRemaining()) {
				SocketChannel sochan=(SocketChannel)key.channel();
				//outBuffer.flip();
				int numBytesWritten = varOutBuffer.writeToChannel(sochan);
				logger.debug("written "+numBytesWritten);
				
				if (numBytesWritten == -1) {
					GH.close(sochan);
					onDisconnect();
				}
			} else {
				key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE ));
				logger.debug("no more write interest");
			}
		}
		
	
	}

	
	public void onDisconnect() throws IOException{
	/*	if (printLifetime) {
			synchronized(bufferLock) {
			logger.info("Lifetime in Seconds: "+ ((System.currentTimeMillis()-connectionCreated)/1000)+ "  "+varInBuffer.remaining());
			}
		} */
		logger.debug("onDisconnect()");
		if (!disconnectSent) {
			disconnectSent = true;
			key.cancel();//unregister with selector
			SocketChannel sochan = (SocketChannel)key.channel();
			sochan.close();
			DCClient.execute(new Runnable() {
				public void run() {
					synchronized(cp) {
						try {
							cp.onDisconnect();
						} catch(IOException ioe) {
							logger.error(ioe,ioe); 
						}
					}
				}
			});
		}
		
	}
	
	/**
	 * the io thread reads here bytes from the socket channel
	 * converts to chars  and then passes the gained string
	 * to read(String read) for further work
	 * 
	 * @throws IOException
	 */
	public void read() throws IOException {
	
		SocketChannel sochan =(SocketChannel)key.channel();
		

		if (varInBuffer.remaining() > 250 *1024) { 
			//if there is more than 250kiB of data lying around in the VarInBuffer.. 
			//-> stop reading -> security (nobody can flood us while we write) + performance gain (Buffer grows too large)
			return;
		}
		
		int numBytesRead = sochan.read(byteBuffer);
		logger.debug("read "+numBytesRead);
	//	if (encryption) {
	//		logger.debug("read "+numBytesRead);
	//	}
		
		if (numBytesRead >= 0) {
			synchronized(bufferLock) {
				
				if (encryption) {
					unwrap();
				} else {
					byteBuffer.flip();
					varInBuffer.putBytes(byteBuffer);
					byteBuffer.clear();
				}
					
				checkRead();
				
			}

		} else {
			GH.close(sochan);
			onDisconnect();
		}
	}
	
	/**
	 * checks if reading is possible and reads if possible..
	 * must be done while holding bufferlock
	 */
	private void checkRead() {
		if (varInBuffer.hasRemaining() && semaphore.tryAcquire()) {
			DCClient.execute(new Runnable(){
				public void run() {
					try {
						if (!connectSent && Platform.inDevelopmentMode()) {  //DEBUG  remove
							logger.warn("connect not sent");
						}
						processread();
					} finally {
						semaphore.release();
					}
				}
			});
		}
	}
	
	private void signalOnConnect() {
		if (!connectSent) {
			connectSent = true;
			DCClient.execute(new Runnable() {
				public void run() {
					synchronized (cp) {
						try {
							if (cp.getState() == ConnectionState.CONNECTING) {
								cp.onConnect();
							} else {
								if (Platform.inDevelopmentMode() && cp.getState() != ConnectionState.DESTROYED) {//TODO remove here .. only debug
									logger.warn("bad connection state: "+cp.getState()+"  "+cp.getClass().getSimpleName());
								}
							}
			
						} catch (IOException ioe) {
							logger.debug(ioe, ioe);
						}
					}
					semaphore.release();
					synchronized (bufferLock) { //check if VarInBuffer already contains sth..
						checkRead();
					}
				}
			});
		}
	}
	
	private void evaluateHandshakeStatus(HandshakeStatus status) {
		logger.debug(status);
		switch (status) {
		case FINISHED:
		case NOT_HANDSHAKING:
			signalOnConnect();
			break;
		case NEED_WRAP:
			send(ByteBuffer.allocate(0));
			break;
		case NEED_TASK:
			DCClient.execute(new Runnable() {
				public void run() {
					Runnable r;
					while ((r = engine.getDelegatedTask()) != null) {
						r.run();
						logger.debug("executing task");
					}
					logger.debug("Status after task: "+engine.getHandshakeStatus());
					evaluateHandshakeStatus(engine.getHandshakeStatus());

				}
			});
			break;
		case NEED_UNWRAP:
			synchronized(bufferLock) {
				unwrap();
			}
			break;
		}
	}
	
	/*
	 * 
	 */
	private void unwrap() {
		int positionLast = 0;
		while (byteBuffer.position() != positionLast) {
			positionLast = byteBuffer.position();
			byteBuffer.flip();
			SSLEngineResult ssler = null;
			try {
				decrypting.clear();
				ssler = engine.unwrap(byteBuffer, decrypting);
				decrypting.flip();
				//logger.debug("bytes after encryption: "+decrypting.remaining()+" "+ssler);
				varInBuffer.putBytes(decrypting);
			} catch (Exception e) {  
				addProblematic(e);
				DCClient.execute(new Runnable() {
					public void run() {
						close();
					}
				});
				
			} 
			
			byteBuffer.compact();
			if (ssler != null && !ssler.getHandshakeStatus().equals(HandshakeStatus.NEED_UNWRAP)) {// check against need unwrap prevents recursiveness.. -> we do further unwrapping here anyway..
				evaluateHandshakeStatus(ssler.getHandshakeStatus());
			}
		}
	}
	/*
	 * adds a user to a problematic set so no DH is used with that user
	 * workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6521495
	 */
	private void addProblematic(Exception e) {
		synchronized(inetAddySynch) {
			if (inetAddress != null) {
				boolean contained = problematic.contains(inetAddress.getAddress());
				problematic.add(inetAddress.getAddress());
				if (Platform.inDevelopmentMode()) {
					if (e.toString().contains("unknown_ca")) {
						logger.info(e+" "+inetAddress.getAddress()+" probcontains: "+contained);
					} else {
						logger.warn(e+" "+inetAddress.getAddress()+" probcontains: "+contained,e);
					}
				}
			}
		}
	}
	
	/**
	 * when a socket has finished connecting
	 * this method is called by MultiStandardConnection
	 * no one else may call it..
	 * 
	 *
	 */
	public void connected() {
		disconnectSent = false;
		//logger.info("disconnect cleared 1");
		final SocketChannel sochan =(SocketChannel)key.channel();
		synchronized (inetAddySynch) {
			inetAddress = null;
			inetAddress = (InetSocketAddress)sochan.socket().getRemoteSocketAddress();
		}
		
		DCClient.execute(new Runnable(){
			public void run() {
				long startconnect;
				synchronized(inetAddySynch) {
					startconnect = System.currentTimeMillis();
				}
				try {
					while (((inetAddress = (InetSocketAddress)sochan.socket().getRemoteSocketAddress()) == null ||   
							inetAddress.getAddress() == null )    
							&& sochan.isOpen()) {

						if (sochan.isConnectionPending()) {
							sochan.finishConnect();
						}
						GH.sleep(25);
						synchronized(cp) {
							if (startconnect + cp.getSocketTimeout() < System.currentTimeMillis()) {
								sochan.close();
							}
						}
					}
					
					
					synchronized(cp) {
						if (sochan.isOpen() && cp.getState() == ConnectionState.CONNECTING) {
							synchronized (inetAddySynch) {
								logger.debug("connected to: "+inetAddress.getAddress().getHostAddress());
							}
							if (encryption) {
								synchronized (inetAddySynch) {
									if (problematic.contains(inetAddress.getAddress()) || !(target instanceof String)) {
										List<String> s = Arrays.asList(engine.getEnabledCipherSuites());
										s = GH.filter(s, "_DHE_");
										s = GH.filter(s, "_ECDH_");
										s = GH.filter(s, "_ECDHE_");
										s = GH.filter(s, "_DH_");
										engine.setEnabledCipherSuites(s.toArray(new String[]{}));
										logger.debug("enabled ciphers: "+GH.toString(engine.getEnabledCipherSuites()));
									}
								}
							//	logger.debug("enabled ciphers: "+GH.toString(engine.getEnabledCipherSuites()));
							/*	if (!semaphore.tryAcquire()) {//prevent reading of commands..
									throw new IOException("Acquire failed");
								} */
								//semaphore.acquireUninterruptibly();
						
								send(ByteBuffer.allocate(0)); //send an empty message to start handshake..
							} else {
								signalOnConnect();
							}
						}
					}

				} catch(IOException ioe) {
					logger.error(ioe,ioe);
					close();
				}

			}
		});
		
	}

	
	/**
	 * when IO-thread has transfered read data to a string
	 * this method will be called from a separate thread and
	 * process/divide the string to suitable pieces for  the 
	 * ConnectionProtocol
	 */
	private void processread() {
		
		boolean stopp = false;
		do {
			synchronized (bufferLock) {
				String s = varInBuffer.readUntil((byte)cp.getCommandStopByte(), charsetDecoder);
				if (GH.isEmpty(s)) {
					return;
				}
				stringbuffer.append(s);
				
			}
			
			synchronized(cp) {
			logger.debug("processread("+stringbuffer.toString()+")");
				Matcher m = null;
				while ((m = cp.getCommandRegexPattern().matcher(stringbuffer)).find()) {
					String found = m.group(1);
					logger.debug("command: "+found);
					if (!GH.isNullOrEmpty(found)) {
						stringbuffer.delete(0, m.end());
						try {
							if (encryption) {
								synchronized(bufferLock) {
									logger.debug("[read]: "+found+" varInBuffer remaining:"+varInBuffer.remaining()+" byteBufferPosition: "+byteBuffer.position()); 
								}
							}
							cp.receivedCommand(found);
						} catch (IOException ioe) {
							close();
							logger.debug(ioe,ioe);
						} catch (RuntimeException re) {
							logger.warn(re,re); 
						}
						
					}  else {
						stringbuffer.deleteCharAt(0);
						break;
					}
				}
			}
			
			
			synchronized(bufferLock) {
				stopp = !varInBuffer.hasRemaining();
			}
		
		} while (!stopp);
	}
	
	

	/**
	 *  resets the connection with the given channel ... can be called on reconnection to the same hub for example..
	 * @param soChan
	 * @param connectionProt
	 */
	public void reset(SocketChannel soChan) {
		disconnectSent = false;
		//logger.info("disconnect cleared 2");
		//executingCommands = false;
		connectSent = false;
		logger.debug("in reset(sochan)");

		refreshCharsetCoders();
		outBuffer.clear();
		outBuffer.flip(); //nothing available in the outBuffer..
		outcharBuffer.clear();
		byteBuffer.clear();
		//charBuffer.clear();
		
		//outgoingBuffer.clear();
		synchronized (varOutBuffer) {
			varOutBuffer.clear();
		}
		stringbuffer.delete(0, stringbuffer.length()); //clear
		
		if (encryption) {
			try {
				synchronized(bufferLock) {
		
						engine = TLS.createSSLEngine();
						engine.setUseClientMode(!serverSide);
						
						if (serverSide) {
							engine.setNeedClientAuth(false);
							engine.setWantClientAuth(false);
							engine.setEnableSessionCreation(true);
						}
	
						List<String> enabledCS = Arrays.asList(engine.getSupportedCipherSuites());
						/*
						 * disabled:
						 * MD5: Old hashfunction and broken.
						 * RC4: old streamcipher and weak
						 * Kerberos: needs server.. probably special settings.. nobody will use it
						 * SSL: Enforces use of TLS
						 */
						enabledCS = GH.filter(enabledCS,"MD5","RC4","KRB5","SSL");
					
					//	enabledCS = GH.filter(enabledCS, "DHE");
					//	enabledCS = GH.filter(enabledCS, "RSA");
		
					
						//move these ciphers to the end so DHE is preferred..
						String noDHEStrong= "TLS_RSA_WITH_AES_256_CBC_SHA";
						if (enabledCS.remove(noDHEStrong)) {
							enabledCS.add(noDHEStrong);
						}
						String noDHE = "TLS_RSA_WITH_AES_128_CBC_SHA";
						if (enabledCS.remove(noDHE)) {
							enabledCS.add(noDHE);
						}
						
					//	enabledCS.remove("TLS_RSA_WITH_AES_256_CBC_SHA");
					//	enabledCS.add("TLS_RSA_WITH_AES_128_CBC_SHA");
						if (!enabledCS.isEmpty()) {
							engine.setEnabledCipherSuites( enabledCS.toArray(new String[]{}));
						}
						
						//logger.debug("Supported CS: "+GH.toString(engine.getSupportedCipherSuites()));
						List<String> enabledProt = Arrays.asList(engine.getSupportedProtocols());
						enabledProt = GH.filter(enabledProt, "SSL");
						
						//String[] enabledProt = new String[] {"TLSv1"}; //,"SSLv3"
						if (!enabledProt.isEmpty()) {
							engine.setEnabledProtocols(enabledProt.toArray(new String[]{}));
						}
					//	logger.info("Enabled Prot: "+GH.toString(engine.getEnabledProtocols()));
					//	logger.info("Enabled CS: "+enabledCS.size());
					//	logger.info("Enabled CS: "+GH.toString(engine.getEnabledCipherSuites()));
						

						byteBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
						decrypting = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
						encrypting = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
						
						logger.debug("encrypted connection created");
				//	}
				}
			} catch (Exception e) {
				logger.error(e, e);
			}
		}
		
		logger.debug("after registering reset(sochan)");
		
		MultiStandardConnection.get().register(soChan, this,false);

		logger.debug("after registering reset(sochan)");
		
		synchronized(cp) {
			cp.beforeConnect();
		}
				
		logger.debug("registered key with channel");
	}
	
	@Override
	public void reset(final InetSocketAddress addy ) {
		
		logger.debug("in reset("+addy.getHostName()+")");
		try {
			final SocketChannel sochan = SocketChannel.open();
			AbstractConnection.bindSocket( sochan);
			logger.debug("in reset openedChanel");
			sochan.configureBlocking(false); //important ... so we can immediately go on here
			logger.debug("in reset configured not Blocking");
			
			//set preferences in performance..
			int[] pp = cp.getPerformancePrefs();
			if (pp != null) { 
				sochan.socket().setPerformancePreferences(pp[0], pp[1], pp[2]);
			}
			
			if (!Socks.isEnabled()) {
				sochan.connect( addy  );
				reset(sochan);
			} else {
				DCClient.execute(new Runnable() {
					public void run() {
						try {
							Socks.getDefaultSocks().connect(sochan, addy);
							sochan.configureBlocking(false);
							reset(sochan);
						} catch(IOException e){ 
							logger.warn(e, e);
						}
					}
				});
			}
			
		} catch (UnresolvedAddressException uae) {
			logger.debug(uae);
		} catch(IOException e){ //here send message
			logger.warn(e, e);
		}
			
	}
	

	@Override
	public void close() {
		//logger.info("disconnect cleared 3");
		//disconnectSent = false;
		
		Runnable r = new Runnable() {
			public void run() {
				if (key != null) {
					if (encryption && key.channel().isOpen()) {
						engine.closeOutbound();
						send(ByteBuffer.allocate(0)); //used for wrapping remaining data..
						flush(250);
						
					}
				
					GH.close(key.channel());
					key.cancel();
				}

				try {
					if (key != null && !key.isValid()) {
						onDisconnect();
					}
				} catch(IOException ioe){
					logger.warn(ioe, ioe);
				}
			}
		};
		if (blocking) {
			if (key != null) {
				GH.close(key.channel());
				key.cancel();
				try {
					if (!key.isValid()) {
						onDisconnect();
					}
				} catch(IOException ioe){
					logger.warn(ioe, ioe);
				}
			}
		} else {
			MultiStandardConnection.get().synchExec(r);
		}
	}
	
	
	@Override
	public boolean flush(int miliseconds) {
		int sleptTotal = 0;
		boolean hasRemaining;
		synchronized(bufferLock) {
			hasRemaining = varOutBuffer.hasRemaining();
		}
		while (hasRemaining && sleptTotal < miliseconds) {
			int sleep = miliseconds - sleptTotal;
			if (sleep > 20 ) {
				sleep = Math.min(100, sleep);
			} else {
				sleep = 100;
			}
			sleptTotal += sleep;
			GH.sleep(sleep );
			
			synchronized(bufferLock) {
				hasRemaining = varOutBuffer.hasRemaining();
			}
		}
		return hasRemaining;
	}

	@Override
	public InetSocketAddress getInetSocketAddress() {
		synchronized (inetAddySynch) {
			return inetAddress;
		}
	}
	

	@Override
	public ByteChannel retrieveChannel() throws IOException {
		SocketChannel sc = (SocketChannel)key.channel();
		
		MultiStandardConnection.get().synchExec(new Runnable() {
			public void run() {
				key.cancel();
			}
		});
		
		sc.configureBlocking(true);
		sc.socket().setSoTimeout(10000);
		blocking = true;
		
		
		return new BlockingChannel();
	}

	@Override
	public boolean returnChannel(ByteChannel bc) {
		if (!blocking) {
			throw new IllegalStateException();
		}

		SocketChannel soChan = (SocketChannel)key.channel();

		if (soChan.isOpen()) {
			try {
				soChan.configureBlocking(false);
			} catch(IOException ioe) {
				return false;
			}
			blocking = false;
			MultiStandardConnection.get().register(soChan, this, true);
			return soChan.isOpen();
		} else {
			close();
			return false;
		}

	}
	
	
	@Override
	public void setIncomingDecompression(Compression comp) throws IOException {
		varInBuffer.setDecompression(comp,bufferLock);
	}
	
	
	public boolean usesEncryption() {
		return encryption;
	}

	
	/**
	 * @author Quicksilver
	 *
	 */
	private class BlockingChannel implements ByteChannel {
		
		//private VarByteBuffer in,out;
		//private int outBuffersize = 1000*1024;
		
		private BlockingChannel( /*VarByteBuffer in,VarByteBuffer out */) {
		//	this.in = in;
		//	this.out = out;
			//synchronized (varOutBuffer) {} // after this constructor is finished.. only we will call this buffers..
		}

		public int write(ByteBuffer src) throws IOException {

			//int toWrite = src.remaining();

			UnblockingConnection.this.send(src);
			

			int toWrite = varOutBuffer.writeToChannel((SocketChannel)key.channel());

			//logger.debug("remaining on write: "+varOutBuffer.remaining()+"  written: "+toWrite);
			
			return toWrite;
		}

		public int read(ByteBuffer dst) throws IOException {
			if (!isOpen() && Platform.inDevelopmentMode()) {
				logger.warn("reading from closed channel");
			}
			synchronized (bufferLock) {
				
				while (!varInBuffer.hasRemaining()) {
					UnblockingConnection.this.read();
					if (!encryption || engine.isInboundDone()) { //without encryption read is always successful... also we need a break if encryption is done..
						break;
					}
				} 
				return varInBuffer.getBytes(dst);
			}
		}

		public void close() throws IOException {
			UnblockingConnection.this.close();
		}

		public boolean isOpen() {
			return key.channel().isOpen();
		}
		
	}
	
	
}