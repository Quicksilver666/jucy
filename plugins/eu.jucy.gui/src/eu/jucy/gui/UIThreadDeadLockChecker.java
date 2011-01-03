package eu.jucy.gui;

import helpers.GH;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import uihelpers.SUIJob;

public class UIThreadDeadLockChecker {
	
	private static final Logger logger = LoggerFactory.make();
	
	private static final Object synch = new Object();
	private static int counter = 0;
	
	
	private static volatile ScheduledFuture<?> checker;
	
	public static void start() {
		logger.debug("started deadlock watcher");
		checker = ApplicationWorkbenchWindowAdvisor.get()
		.getSchedulerDir().scheduleWithFixedDelay(new Runnable() {
			public void run() {  //if watchdog is not run counter will increment till it reaches 90..
				boolean newDeadlockfound = false;
				synchronized(synch) {
					if (++counter > 90) { //&& !deadLockFound
						//deadLockFound = true; //only log once
						newDeadlockfound = true;
						checker.cancel(false);
					}
				}
				if (newDeadlockfound) {
					logger.warn("Detected deadlock in UI thread!\n"+GH.getAllStackTraces());
				}
			}
			
		}, 100, 1, TimeUnit.SECONDS);
		
		new SUIJob() {  //watchdog thread... if UIthread blocks  this won't be run
			@Override
			public void run() {
				synchronized(synch) {
					counter = 0;
				}
				schedule(1000);
			}
			
		}.schedule(1000);
		
	}

}
