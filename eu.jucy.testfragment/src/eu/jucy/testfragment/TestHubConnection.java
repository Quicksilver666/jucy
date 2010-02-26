package eu.jucy.testfragment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import logger.LoggerFactory;


import uc.crypto.HashValue;
import uc.protocols.Compression;
import uc.protocols.ConnectionProtocol;
import uc.protocols.DCProtocol;
import uc.protocols.IConnection;
import uc.protocols.ICryptoInfo;

public class TestHubConnection implements IConnection {

	private static final Logger logger = LoggerFactory.make();
	
	private final boolean encryption;
	private HashValue fingerp;
	
	private boolean open = false;
	
	private final List<String> ignoredPrefixes = new CopyOnWriteArrayList<String>();
	
	
	private final BlockingQueue<Object> messagesSent = new LinkedBlockingQueue<Object>();
	
		
	private final ConnectionProtocol cp;
	private final String addy;
	
	public TestHubConnection(String addy, ConnectionProtocol connectionProt,boolean encryption,HashValue fingerp) {
		this.encryption = encryption;
		this.cp = connectionProt;
		this.addy = addy;
		this.fingerp = fingerp;
	}
	
	public void send(String toSend) throws IOException {
		boolean ignore = false;
		for (String s:ignoredPrefixes) {
			ignore = ignore || toSend.startsWith(s);
		}
		if (!ignore) {
			messagesSent.offer(toSend);
		}
	}
	
	
	public void close() {
		if (open) {
			open = false;
			try {
				cp.onDisconnect();
			} catch(IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		} else {
			throw new IllegalStateException("closed Connection that was already closed");
		}
		
	}

	public boolean flush(int miliseconds) {
		return false;
	}

	public InetSocketAddress getInetSocketAddress() {
		int i = addy.indexOf(':');
		return new InetSocketAddress(addy.substring(0, i), Integer.parseInt(addy.substring(i+1)));
	}

	public boolean isLocal() {
		return false;
	}

	public void refreshCharsetCoders() {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}

	public void reset(SocketChannel soChan) {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}

	public void reset(InetSocketAddress addy) {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}

	public void reset(String addy) {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}

	public ByteChannel retrieveChannel() throws IOException {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}

	public boolean returnChannel(ByteChannel sochan) {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}



	public void send(ByteBuffer toSend) throws IOException {
		messagesSent.add(toSend);
	}

	public void setIncomingDecompression(Compression comp) throws IOException {
		logger.error("unimplemented Method in TestHubConnection called");
		throw new IllegalStateException("unimplemented Method in TestHubConnection called");
	}

	public void start() {
		if (open) {
			new IllegalStateException("Connection already open");
		} else {
			open = true;
			cp.beforeConnect();
			try {
				cp.onConnect();
			} catch (IOException ioe) {
				logger.error(ioe, ioe);
			}
		}
		
	}

	public boolean usesEncryption() {
		return encryption;
	}

	
	
	
	// --- end interface methods
	
	public void clearMessagesSent() {
		messagesSent.clear();
	}
	

	
	
/*	public String getLastMessageSentAndCheckSingle() {
		if (messagesSent.size() == 1) {
			return messagesSent.get(0);
		} else {
			throw new IllegalStateException("not one message found "+messagesSent.size());
		}
	} */
	
	/**
	 * gets the last message the connection sent out..
	 * if removeNL is set.. trailing character  will be removed 
	 * @throws UnsupportedEncodingException 
	 */
	public String pollNextMessage(boolean removeLastChar) throws InterruptedException, UnsupportedEncodingException {
		Object o = messagesSent.take();
		if (o instanceof ByteBuffer) {
			o = new String(((ByteBuffer)o).array(),DCProtocol.NMDCCHARENCODING);
		}
		
		String s = (String)o;
		if (removeLastChar) {
			s = s.substring(0, s.length()-1);
		}
		return s;
	}
	//
	
	public String pollNextMessage(long millisecondstimeout) throws InterruptedException {
		String s = (String)messagesSent.poll(millisecondstimeout, TimeUnit.MILLISECONDS);
		
		return s;
	}
	
	/**
	 * polls next object which has to be a byteBuffer..
	 * @return
	 */
	public ByteBuffer pollNextByteBuffer(long millisecondstimeout) throws InterruptedException {
		Object o = messagesSent.poll(millisecondstimeout, TimeUnit.MILLISECONDS);
		if ( o instanceof String) {
			logger.error("Found bad thing in queue: "+o);
		}
		return (ByteBuffer)o;
	}
	
	
	public boolean isMessageSentEmpty() {
		return messagesSent.isEmpty();
	}
	
	public void clear() {
		messagesSent.clear();
	}

	public void addIgnore(String o) {
		ignoredPrefixes.add(o);
	}

	public boolean removeIgnore(Object o) {
		return ignoredPrefixes.remove(o);
	}

	public boolean setFingerPrint(HashValue hash) {
		fingerp=hash;
		return true;
	}

	public boolean isFingerPrintUsed() {
		return fingerp !=null;
	}

	public void getCryptoInfo(ICryptoInfo cryptoInfo) {
		throw new IllegalStateException();
	}
	
	
	
	
}
