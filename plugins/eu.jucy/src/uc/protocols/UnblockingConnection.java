package uc.protocols;

import helpers.GH;
import helpers.LockedRunnable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;


import logger.LoggerFactory;
import uc.DCClient;
import uc.ICryptoManager;
import uc.crypto.HashValue;
import uc.protocols.MultiStandardConnection.IUnblocking;

/**
 * Good example for bad looking ugly code that somehow more or less works...
 * 
 * 
 * @author Quicksilver
 *
 */
public class UnblockingConnection extends AbstractConnection implements IUnblocking {
	
	private static final Logger logger = LoggerFactory.make();

	/**
	 * here we store IP addresses of other clients that failed DHE key exchange with us..
	 */
	private static final Set<InetAddress> problematic = 
		Collections.synchronizedSet(new HashSet<InetAddress>());
	
	private static final Set<InetAddress> invalidEncodedCerts =  //TODO this would be nice as workaround for those invalid certs
			Collections.synchronizedSet(new HashSet<InetAddress>());
	
	private final boolean encryption;
	private final boolean serverSide;
	
	
	private volatile SSLEngine engine;
	private volatile HashValue fingerPrint;
	private final AtomicBoolean connectSent = new AtomicBoolean(false);
	
	private final AtomicBoolean disconnectSent = new AtomicBoolean(false);
//	private final Semaphore disconnectSentSem = new Semaphore(1);
	
	private volatile SelectionKey key; 
	private volatile boolean blocking = false;
	

	

	private static final Object inetAddySynch = new Object();
	private volatile InetSocketAddress inetAddress = null ; 

//	private final Object bufferLock = new Object();
	private final Lock bufferLock = new ReentrantLock();
	private final Condition bytesChanged = bufferLock.newCondition();
	
	private volatile ByteBuffer byteBuffer = ByteBuffer.allocate(1024*2); //in Buffer.. 
//	private final CharBuffer outcharBuffer = CharBuffer.allocate(1024);
//	private final ByteBuffer outBuffer = ByteBuffer.allocate(outcharBuffer.capacity()*4);
	
	private ByteBuffer encrypting, decrypting; //bytebuffers for encrypting and decrypting data..
	
	private final VarByteBuffer varOutBuffer = new VarByteBuffer(1024*8); //replaces the outgoing stringbuffer..
	private final VarByteBuffer varInBuffer = new VarByteBuffer(1024*8);

	private final Semaphore semaphore = new Semaphore(0,false);


	
	private Object target; // socketchannel... or hubaddy in string format
	
	

	


	
	/**
	 * used for client as well as server mode.
	 */
	public UnblockingConnection(ICryptoManager cryptoManager,SocketChannel soChan, ConnectionProtocol connectionProt,boolean encryption, boolean serverSide,HashValue fingerPrint) {
		super(cryptoManager,connectionProt);
		this.encryption = encryption;
		this.serverSide = serverSide;
		this.target = soChan;
		this.fingerPrint = fingerPrint;
	}
	
	
	
	/**
	 * only used for client mode
	 * @param addy
	 * @param connectionProt
	 * @param encryption
	 * @param allowDH - can forbid DH keys due to problems with DH and other clients... especially apex..
	 */
	public UnblockingConnection(ICryptoManager cryptoManager,String addy, ConnectionProtocol connectionProt,boolean encryption,HashValue fingerPrint) {
		super(cryptoManager,connectionProt);
		this.encryption = encryption;
		this.target = addy;
		serverSide = false;
		this.fingerPrint = fingerPrint;
	}
	
	public void setKey(SelectionKey key) {
		this.key = key;
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
	public UnblockingConnection(ICryptoManager cryptoManager,InetSocketAddress isa, ConnectionProtocol connectionProt,boolean encryption,HashValue fingerPrint){
		super(cryptoManager,connectionProt);
		this.target = isa;
		this.encryption = encryption;
		this.serverSide = false;
		this.fingerPrint = fingerPrint;
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
	
	
	public void send(ByteBuffer toSend) {
		bufferLock.lock();
		
		try {
			if (encryption) {
				try {
					
					int lastRemaining;
					do {
						lastRemaining = toSend.remaining();
						encrypting.clear();
						SSLEngineResult ssler = engine.wrap(toSend, encrypting);
						encrypting.flip();
						varOutBuffer.putBytes(encrypting);
						evaluateHandshakeStatus(ssler.getHandshakeStatus());
					} while (toSend.hasRemaining() && lastRemaining != toSend.remaining()); //even empty buffers need to execute this at least once..
					
				} catch(RuntimeException re) {
					addProblematic(re);
				} catch(SSLException ssle) {
					logger.debug(ssle, ssle);
				}
				
			} else {
				varOutBuffer.putBytes(toSend);
			}
		} finally {
			bufferLock.unlock();
		}
		
		if (!blocking) {
			addInterestOp(SelectionKey.OP_WRITE);
		}
		
	}
	
	
	public void write() throws IOException {

			
		bufferLock.lock();
		try {
			int numBytesWritten = 0;
			if (varOutBuffer.hasRemaining()) {
				SocketChannel sochan = (SocketChannel) key.channel();
				//outBuffer.flip();
				numBytesWritten = varOutBuffer.writeToChannel(sochan);
				logger.debug("written " + numBytesWritten);

				if (numBytesWritten == -1) {
					GH.close(sochan);
					onDisconnect();
				}
			}
			if (numBytesWritten == 0) {
				key.interestOps(key.interestOps()
						& (~SelectionKey.OP_WRITE));
				//	logger.debug("no more write interest");
			}
		} finally {
			bufferLock.unlock();
		}

		
		
	
	}

	
	public void onDisconnect() throws IOException{
	/*	if (printLifetime) {
			synchronized(bufferLock) {
			logger.debug("Lifetime in Seconds: "+ ((System.currentTimeMillis()-connectionCreated)/1000)+ "  "+varInBuffer.remaining());
			}
		} */
		logger.debug("onDisconnect()");
		if (disconnectSent.compareAndSet(false, true)) {
			
		//	disconnectSent = true;
			key.cancel();//unregister with selector
			SocketChannel sochan = (SocketChannel)key.channel();
			sochan.close();
			
			DCClient.execute(new LockedRunnable(cp.writeLock()) {
				@Override
				protected void lockedRun() {
					try {	
						cp.onDisconnect();
					} catch(IOException ioe) {
						logger.error(ioe,ioe); 
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
		bufferLock.lock();
		try {
			if (varInBuffer.remaining() > 250 * 1024) {
				//if there is more than 250kiB of data lying around in the VarInBuffer.. 
				//-> stop reading -> security (nobody can flood us while we write) + performance gain (Buffer grows too large)
				return;
			}
		} finally {
			bufferLock.unlock();
		}
		
		
		int numBytesRead = sochan.read(byteBuffer);
		
		
		if (numBytesRead >= 0) {
			

			bufferLock.lock();
			try {
				if (encryption) {
					unwrap();
				} else {
					byteBuffer.flip();
					varInBuffer.putBytes(byteBuffer);
					byteBuffer.clear();
				}
				checkRead();
			} finally {
				bufferLock.unlock();
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
						if (!connectSent.get() && Platform.inDevelopmentMode()) {  //DEBUG  remove
							logger.warn("connect not sent: "+varInBuffer.toString());
						}
						processread();
					} finally {
						semaphore.release();
					}
				}
			});
		}
	}
	
	/**
	 * signals on connect if not already done so..
	 */
	private void signalOnConnect() {
		if (connectSent.compareAndSet(false, true)) {
			//connectSent = true;
			DCClient.execute(new Runnable() {
				public void run() {
					Lock l = cp.writeLock();
					l.lock();
					try {
						if (cp.getState() == ConnectionState.CONNECTING) {
							if (getInetSocketAddress() != null) { // check one last time for socket addy being set..
								cp.onConnect();
								semaphore.release();
							} else {
								asynchClose();
							}
						} 
					} catch (IOException ioe) {
						logger.debug(ioe, ioe);
					} finally {
						l.unlock();
					}
	
					bufferLock.lock();
					try {
						checkRead();
					} finally {
						bufferLock.unlock();
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
			boolean tryValidation = fingerPrint != null && !connectSent.get();
			
			if (tryValidation && !checkFingerprint()) {
				return;
			}
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
			
			bufferLock.lock();
			try {
				unwrap();
			} finally {
				bufferLock.unlock();
			}
			break;
		}
	}

	
	/*
	 * 
	 */
	private void unwrap() {
		int positionLast = 0;
		
		while (byteBuffer.position() != positionLast) { // just taking remaining doesn't work sometimes (bytes are in line but can't be decoded)
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
				if (e.toString().contains("Cipher buffering error") && Platform.inDevelopmentMode()) {
					logger.warn(cp.toString());
				}
				addProblematic(e);
				asynchClose();
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
				if (e.toString().contains("Invalid encoding: zero length Int value")) {
					invalidEncodedCerts.add(inetAddress.getAddress());
				}
				
				if (Platform.inDevelopmentMode()) {
					if (e.toString().contains("unknown_ca") || e.toString().contains("Invalid encoding: zero length Int value")) {
						logger.info(e+" "+inetAddress.getAddress()+" probcontains: "+contained);
					} else {
						logger.warn(e+" "+inetAddress.getAddress()+" probcontains: "+contained,e);
						
					}
				}
			}
		}
	}
	
	private boolean trySetInetAddy(SocketChannel sochan) {
		synchronized (inetAddySynch) {
			if (getInetSocketAddress() != null) {
				return true;
			}
			return !((inetAddress = (InetSocketAddress)sochan.socket().getRemoteSocketAddress()) == null ||   
				inetAddress.getAddress() == null);
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
		disconnectSent.set(false);
		
		final SocketChannel sochan =(SocketChannel)key.channel();
		trySetInetAddy(sochan);
		
		DCClient.execute(new Runnable(){
			public void run() {
				long startconnect = System.currentTimeMillis();
				try {
					while (!trySetInetAddy(sochan) && sochan.isOpen()) {
					//	if (sochan.isConnectionPending()) {
						sochan.finishConnect();
					//	}
						GH.sleep(50);	
						if (startconnect + cp.getSocketTimeout() < System.currentTimeMillis()) {
							sochan.close();
						}
					}
					if (sochan.isOpen() && cp.getState() == ConnectionState.CONNECTING) {
						synchronized (inetAddySynch) {
							logger.debug("connected to: "+inetAddress.getAddress().getHostAddress());
						}
						if (encryption) {
							synchronized (inetAddySynch) {
								if (problematic.contains(inetAddress.getAddress()) || !(target instanceof String) ) { 
									List<String> s = Arrays.asList(engine.getEnabledCipherSuites());
									s = GH.filter(s, "_DHE_");
									s = GH.filter(s, "_ECDH_");
									s = GH.filter(s, "_ECDHE_");
									s = GH.filter(s, "_DH_");
									engine.setEnabledCipherSuites(s.toArray(new String[]{}));
									logger.debug("enabled ciphers: "+GH.toString(engine.getEnabledCipherSuites()));
								}
							}
						
							send(ByteBuffer.allocate(0)); //send an empty message to start handshake..
						} else {
							signalOnConnect();
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
			byte[] sBa;
			
			bufferLock.lock();
			try {
				sBa = varInBuffer.readUntil((byte) cp.getCommandStopByte());
				if (sBa.length == 0) {
					return;
				}
			} finally {
				bufferLock.unlock();
			}
			
			Lock l = cp.writeLock();
			l.lock();
			try {
				cp.receivedCommand(sBa);
			} catch (IOException ioe) {
				close();
				logger.debug(ioe,ioe);
			} catch (RuntimeException re) {
				close();
				logger.warn(re,re); 
			} finally {
				l.unlock();
			}

			
			bufferLock.lock();
			try {
				stopp = !varInBuffer.hasRemaining();
			} finally {
				bufferLock.unlock();
			}
			
		
		} while (!stopp);
	}
	
	

	/**
	 *  resets the connection with the given channel ... can be called on reconnection to the same hub for example..
	 * @param soChan
	 * @param connectionProt
	 */
	public void reset(SocketChannel soChan) {
		semaphore.drainPermits();
		
	//	disconnectSent = false;
		disconnectSent.set(false);
	//	disconnectSentSem.drainPermits();
	//	disconnectSentSem.release();
		
		connectSent.set(false); 
		logger.debug("in reset(sochan)");

		
//		outBuffer.clear();
//		outBuffer.flip(); //nothing available in the outBuffer..
		byteBuffer.clear();
		
		
		//charBuffer.clear();
		
		//outgoingBuffer.clear();
		synchronized (varOutBuffer) {
			varOutBuffer.clear();
		}
		
		
		if (encryption) {
			bufferLock.lock();
			try {
				engine = cryptoManager.createSSLEngine();
				engine.setUseClientMode(!serverSide);
				if (serverSide) {
					engine.setNeedClientAuth(true);
					engine.setWantClientAuth(true);
					engine.setEnableSessionCreation(true);
				}
				List<String> enabledCS = Arrays.asList(engine
						.getSupportedCipherSuites());
				/*
				 * disabled: MD5: Old hashfunction and broken. RC4: old
				 * streamcipher and weak Kerberos: needs server.. probably
				 * special settings.. nobody will use it SSL: Enforces use
				 * of TLS
				 */
				enabledCS = GH.filter(enabledCS, "MD5", "RC4", "KRB5",
				"SSL");
				if (!enabledCS.isEmpty()) {
					engine.setEnabledCipherSuites(enabledCS
							.toArray(new String[] {}));
				}
				List<String> enabledProt = Arrays.asList(engine
						.getSupportedProtocols());
				enabledProt = GH.filter(enabledProt, "SSL");
				if (!enabledProt.isEmpty()) {
					engine.setEnabledProtocols(enabledProt
							.toArray(new String[] {}));
				}
				SSLSession ssle = engine.getSession();
				byteBuffer = ByteBuffer.allocate(ssle
						.getPacketBufferSize());
				decrypting = ByteBuffer.allocate(ssle
						.getApplicationBufferSize());
				encrypting = ByteBuffer.allocate(ssle
						.getPacketBufferSize());
				logger.debug("encrypted connection created");


			} catch (RuntimeException e) {
				logger.error(e, e);
			} finally {
				bufferLock.unlock();
			}
		}
		
		Lock l = cp.writeLock();
		l.lock();
		try {
			cp.beforeConnect();
		} finally {
			l.unlock();
		}
		
		logger.debug("after registering reset(sochan)");
		
		MultiStandardConnection.get().register(soChan, this,false);

		logger.debug("after registering reset(sochan)");
	}
	
	@Override
	public void reset(final InetSocketAddress addy ) {
		
		logger.debug("in reset("+addy+")");
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
		} catch(IOException e){ 
			logger.warn(e + " addy: "+addy, e);
		}
			
	}
	
	/**
	 * will close if keyprint is not correct ..
	 * @return true if correct.. false otherwise..
	 */
	private boolean checkFingerprint() {
		if (fingerPrint == null) {
			throw new IllegalStateException();
		}
		Certificate cert = null;
		SSLSession ssle = engine.getSession();
		boolean correct = false;
		try {
			cert = ssle.getPeerCertificates()[0]; // ssle.getPeerCertificateChain()[0];
			if (fingerPrint != null) {
				HashValue hash = HashValue.createHash( cert.getEncoded(), fingerPrint.magnetString());
				correct = hash.equals(fingerPrint);
//				logger.debug("fingerprint correct: "+ correct +"  "+hash);
//				logger.debug("sha-1 fingerprint: "+ GH.getHex( GH.getHash(cert.getEncoded(),"SHA-1")));
				if (!correct) {
					logger.info("Bad Fingerprint found from "+getInetSocketAddress() 
							+"\nFound: "+hash
							+"\nExpected: "+fingerPrint);
				}
			}
			
		} catch (SSLPeerUnverifiedException e) {
			logger.error(getInetSocketAddress()+ " did not present a valid certificate.");
		} catch (CertificateEncodingException e) {
			logger.warn(e,e);
		}
		if (!correct) {
			asynchClose();
		}
		return correct;
		
	}
	
	
	

	public boolean setFingerPrint(HashValue hash) {
		if (encryption && connectSent.get() && this.fingerPrint == null) {
			this.fingerPrint = hash;
			return checkFingerprint();
		} else {
			this.fingerPrint = hash;
			return true;
		}
	}


	public boolean isFingerPrintUsed() {
		return encryption && fingerPrint != null;
	}


	public void getCryptoInfo(ICryptoInfo info) {
		if (!encryption) {
			throw new IllegalStateException();
		}
		info.setInfo(engine);
	}

	private void asynchClose() {
		DCClient.execute(new Runnable() {
			public void run() {
				close();
			}
		});
	}

	@Override
	public void close() {
		final Semaphore sem = new Semaphore(0);
		Runnable r = new Runnable() {
			public void run() {
				if (key != null) {
					if (key.channel().isOpen()) {
						if (encryption) {
							engine.closeOutbound();
							send(ByteBuffer.allocate(0)); //used for wrapping remaining data..
						}
						
						flush(encryption?800:400);
					}
				
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
				sem.release();
			}
		};
		DCClient.execute(r);
		sem.acquireUninterruptibly();
	}
	
//	private final Lock blockingChannelLock = new ReentrantLock();
	
	public boolean flush(int milliseconds) {
		if (blocking) {
			boolean locked = false;
			try {
				locked = bufferLock.tryLock(milliseconds,TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
			if (locked) {
				try {
					((SocketChannel)key.channel()).socket().setSoTimeout(milliseconds);
					varOutBuffer.writeToChannel((SocketChannel)key.channel());
					((SocketChannel)key.channel()).socket().setSoTimeout(cp.getSocketTimeout());
				} catch(IOException ioe) {
					logger.debug(ioe + toString(),ioe);
				} finally {
					bufferLock.unlock();
				}
				
			}
			
			return varOutBuffer.hasRemaining();
			
			
		} else {
			
			long sleepEnd = System.currentTimeMillis() + milliseconds;
			
				
			bufferLock.lock();
			try {
				while (varOutBuffer.hasRemaining() && System.currentTimeMillis() < sleepEnd) {
					try {
						bytesChanged.await(20,TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						Thread.interrupted();
						break;
					}
				}
				return varOutBuffer.hasRemaining();
			} finally {
				bufferLock.unlock();
			}
			
		}
	}

	@Override
	public InetSocketAddress getInetSocketAddress() {
		synchronized (inetAddySynch) {
			if (inetAddress == null || inetAddress.getAddress() == null) {
				return null;
			}
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
		varInBuffer.setDecompression(comp,bufferLock,bytesChanged);
	
	}
	
	
	public boolean usesEncryption() {
		return encryption;
	}

	
	/**
	 * @author Quicksilver
	 *
	 */
	private class BlockingChannel implements ByteChannel {
		private BlockingChannel() {}

		public int write(ByteBuffer src) throws IOException {
			UnblockingConnection.this.send(src);
			
			bufferLock.lock();
			try {
				int toWrite = varOutBuffer.writeToChannel((SocketChannel) key.channel());
				return toWrite;
			} finally {
				bufferLock.unlock();
			}
			
		}

		public int read(ByteBuffer dst) throws IOException {
			boolean varInBufferHasRemaining;
			
			bufferLock.lock();
			try {
				varInBufferHasRemaining = varInBuffer.hasRemaining();
			} finally {
				bufferLock.unlock();
			}
			
			while (!varInBufferHasRemaining) {
				
				UnblockingConnection.this.read();
				
				bufferLock.lock();
				try {
					if (!encryption || engine.isInboundDone()) { //without encryption read is always successful... also we need a break if encryption is done..
						break;
					}
					varInBufferHasRemaining = varInBuffer.hasRemaining();
				} finally {
					bufferLock.unlock();
				}
				
					
			} 
			
			bufferLock.lock();
			try {
				return varInBuffer.getBytes(dst);
			} finally {
				bufferLock.unlock();
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