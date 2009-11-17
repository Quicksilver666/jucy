package uc.protocols;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.DCClient;

public class ProtocolTimer implements Runnable {
	
	private static Logger logger = LoggerFactory.make(); 

	private final CopyOnWriteArrayList<ConnectionProtocol> all = 
		new CopyOnWriteArrayList<ConnectionProtocol>();
	
	private ScheduledFuture<?> future;
	
	public ProtocolTimer() {}
	
	
	
	public synchronized boolean registerCP(ConnectionProtocol cp) {
		if (all.isEmpty()) {
			future = DCClient.getScheduler().scheduleAtFixedRate(this, 1, 1 , TimeUnit.SECONDS);
		}
		return all.addIfAbsent(cp);
	}
	
	public synchronized void deregisterCP(ConnectionProtocol cp){
		all.remove(cp);
		if (all.isEmpty()) {
			future.cancel(false);
		}
	}
	
	public void run() {
		for (ConnectionProtocol cp: all) {
			try {
				cp.timer();
			} catch(Exception re) {
				logger.warn(re, re);
			}
		}
	}
	

	
}
