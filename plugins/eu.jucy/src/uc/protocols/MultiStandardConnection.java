package uc.protocols;



import java.io.IOException;


import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;



import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


import logger.LoggerFactory;



import org.apache.log4j.Logger;

import uc.DCClient;



/**
 * This class will handle in non blocking io fashion every io
 * from all running sockets..
 * 
 * will also give possibility for the protocols to retrieve a socket (and unregister it..)
 * that will be for example needed if Compression or sth alike must be done..
 * 
 *
 * 
 * @author Quicksilver
 *
 */
public class MultiStandardConnection implements Runnable {

	private static final Logger logger = LoggerFactory.make();

	
	private static MultiStandardConnection msc = null;
	
	private final Queue<Runnable> executeingQueue =  new LinkedList<Runnable>();
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();
//	private final Object synch = new Object();

	//private HashMap<ConnectionProtocol,SelectionKey> protocolToKey = new HashMap<ConnectionProtocol,SelectionKey>();
	private Selector selector = null;
	
	/**
	 *  
	 * @return the singleton MultiStandardConnection, creates one if none exists..
	 */
	public synchronized static MultiStandardConnection get() {
		logger.debug("in get()");
		if (msc == null) {
			msc = new MultiStandardConnection();
			msc.start();
			try {
				//wait for selector getting set
				synchronized(msc) {
					while (msc.selector == null) {
						msc.wait(100);
					}
				}
				
			} catch(InterruptedException ie) {}
		}

		logger.debug("end of get");
		return msc;
	}
	

	
	private MultiStandardConnection(){} // only private constructor..
	
	private void start() {
		Thread t = new Thread(this,"IO-Thread");
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * 
	 * @param sc - socketChannel needing registering
	 * @param ucp - the object managing the connection
	 * @param reregister - if reregister is true then socket won't be checked for connecting
	 */
	public void register(final SocketChannel sc, final IUnblocking ucp,final boolean reregister) {
		logger.debug("registering");

		synchExec(new Runnable() {
			public void run() {
				try {
					final boolean pending = !reregister && sc.isConnectionPending();
					SelectionKey key = sc.register(selector,
							pending ? SelectionKey.OP_CONNECT
									: SelectionKey.OP_READ, ucp);
					ucp.setKey(key);
					//ucp.key = key;
					if (!reregister && !pending) { //workaround.. if the channel has already completed connection when we arrive here..
						logger.debug("shortcut taken");	
						ucp.connected();
					}

				} catch(ClosedChannelException cce) {
					logger.debug(cce, cce);
				} 
			}
		});
	}
	public void register(final ServerSocketChannel ssc, final ISocketReceiver ucp) {
		logger.debug("registering serversocket");
		
		asynchExec(new Runnable() {
			public void run() {
				try {
					ssc.configureBlocking(false);
					SelectionKey sel = ssc.register(selector, SelectionKey.OP_ACCEPT, ucp);
					ucp.setKey(sel);
				} catch(IOException ioe) {
					logger.error(ioe,ioe);
				}
			}
		});
	}
	
	
	
	public void synchExec(Runnable run) {
		lock.lock();
		try {
			executeingQueue.offer(run);
			do {
				cond.awaitUninterruptibly();
			} while(executeingQueue.contains(run));
		
		} finally {
			lock.unlock();
		}
	}
	
	public void asynchExec(Runnable run) {
		lock.lock();
		try {
			executeingQueue.offer(run);
		} finally {
			lock.unlock();
		}
	}
	
	private void runSubmitted() {
		lock.lock();
		try {
			Runnable run;
			while (null != (run = executeingQueue.poll())) {
				run.run();
			}
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}


	public void run() {
		try {
	        // Create the selector
			synchronized(this) {
				if (selector == null) {
					selector = Selector.open();
				}
				notifyAll();
			}
			logger.debug("selector created");
	        // Wait for events
	        while (true) {
	            try {
	                // Wait for an event
	                selector.select(100);
	            } catch (IOException e) {
	            	logger.error("selector error",e);
	            }
	            runSubmitted();
	            
	            // Get list of selection keys with pending events
	            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
	        
	            // Process each key at a time
	            while (it.hasNext()) {
	                // Get the selection key
	                SelectionKey selKey = it.next();
	                
	                // Remove it from the list to indicate that it is being processed
	                it.remove();

	                try {
	                    processSelectionKey(selKey);
	                } catch (IOException e) {
	                    // Handle error with channel and unregister
	                	if (selKey.attachment() instanceof IUnblocking) {
	                		((IUnblocking )selKey.attachment()).onDisconnect();
	                	} else {
	                		logger.error("Exception received: "+e,e);
	                	}
	                } catch(CancelledKeyException ce) {
	                	//between isValid check and using the key could be invalidated.. 
	                	if (selKey.attachment() instanceof IUnblocking) {
	                		((IUnblocking )selKey.attachment()).onDisconnect();
	                	} else {
	                		logger.error("Exception received: "+ce,ce);
	                	}
	                } catch(NotYetConnectedException nyce) {
	                	if (selKey.attachment() instanceof IUnblocking) {
	                		((IUnblocking )selKey.attachment()).onDisconnect();
	                	} else {
	                		logger.error("Exception received: "+nyce,nyce);
	                	}
	                	//this should not be caught... shows error on beginning connection..
	                }
	            }

	        } 
	        

	    } catch(Exception e) {
	    	logger.warn("Selection error, IO-Thread died ... restarting IO-Thread",e);
	    	start();
	    }
	    

	}
	
	public void processSelectionKey(SelectionKey selKey) throws IOException {
        // Since the ready operations are cumulative,
        // need to check readiness for each operation
        if (selKey.isValid() && selKey.isConnectable()) {
        	logger.debug("key is connectable");
            // Get channel with connection request
            SocketChannel sChannel = (SocketChannel)selKey.channel();
    
            boolean success = sChannel.finishConnect();
            
            if (!success) {
                // An error occurred; handle it
                // Unregister the channel with this selector
                // notify disconnect..
            	logger.debug("no success in connection"+sChannel.isConnectionPending());
                ((IUnblocking)selKey.attachment()).onDisconnect();
            } else {
            	selKey.interestOps(SelectionKey.OP_READ);
            	((IUnblocking)selKey.attachment()).connected();
            }
        }
        if (selKey.isValid() && selKey.isReadable()) {
        	logger.debug("key is readable");
            // Get channel with bytes to read
        	((IUnblocking)selKey.attachment()).read();
    

        }
        if (selKey.isValid() && selKey.isWritable()) {
        	logger.debug("key is writeable");
            // Get channel that's ready for more bytes
        	((IUnblocking)selKey.attachment()).write();
        }
        if (selKey.isValid() && selKey.isAcceptable()) {
        	final ServerSocketChannel ssc = (ServerSocketChannel)selKey.channel();
        	final SocketChannel sc = ssc.accept();
        	final ISocketReceiver isr = (ISocketReceiver)selKey.attachment();
        	
        	DCClient.execute(new Runnable() {
				public void run() {
					isr.socketReceived(ssc, sc);
				}
			});
        }

    }
	
	public static interface IUnblocking extends IHasKey {
		
		void connected();
		void onDisconnect() throws IOException;
		void read() throws IOException;
		void write() throws IOException;
	}
	
	public static interface ISocketReceiver extends IHasKey  {
		
		/**
		 * called directly by IO thread.. must therefore return immediately..
		 * 
		 * @param port - the port the SererSocketchannel had
		 * @param created - the socket that was created..
		 */
		void socketReceived(ServerSocketChannel port, SocketChannel created);
		
		
	}
	
	public static interface IHasKey {
		void setKey(SelectionKey key);
	}

}
