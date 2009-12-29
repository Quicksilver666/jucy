package eu.jucy.gui;

import helpers.SizeEnum;

import java.io.File;
import java.util.LinkedList;

import logger.LoggerFactory;

import org.apache.log4j.Logger;


/**
 * A token that can be logged when the hashing is done..
 * 
 * @author Quicksilver
 */
public class TimeToken {
	
	private static final Logger logger = LoggerFactory.make();
	
	private static LinkedList<TimeToken> lastToken = new LinkedList<TimeToken>();
	
	private static void  addTimeToken(TimeToken to) {
		synchronized (lastToken) {
			while (!lastToken.isEmpty() && lastToken.getFirst().creationTime < to.creationTime - 20000 ) {
				lastToken.remove();
			}
			lastToken.add(to);
		}
	}
	
	private static long getMinimumEstimateForRest() {
		long minimum = Long.MAX_VALUE;
		synchronized (lastToken) {
			for (TimeToken tt: lastToken) {
				long timeest = tt.getEstimateForRest(); 
				minimum = Math.min(timeest, minimum);
			}
			if (minimum == 0) {
				return lastToken.getLast().getEstimateForRest();
			} else {
				return minimum;
			}
		}
	}
	

	
	private final long creationTime;
	private long duration; 
	private File hashed;
	
	
	private long getEstimateForRest() {
		long size = hashed.length() == 0? 1000 : hashed.length(); 
		long estimatedDurationForRest =  (duration * sizeLeftForHashing)/size ;
		estimatedDurationForRest /= 1000;
		return estimatedDurationForRest;
	}

	
	private final long sizeLeftForHashing;
	/**
	 * 
	 * @param before - before the the hashing started
	 * @param after - the time when the hashing was finished
	 * @param hashed - the file that was hashed
	 */
	public TimeToken(long duration, File hashed, long sizeLeftForHashing) {
		creationTime = System.currentTimeMillis();
		this.duration = duration;
		this.hashed = hashed;
		this.sizeLeftForHashing = sizeLeftForHashing;
		addTimeToken(this);
	}
	
	
	
	public String toString() {
		
		String durationstring = SizeEnum.toSpeedString(duration, hashed.length());
		
		
		long estimatedDurationForRest = getMinimumEstimateForRest();
		
		logger.debug("left size: "+sizeLeftForHashing+" ("+SizeEnum.getReadableSize(sizeLeftForHashing)+") seconds estimation: "+estimatedDurationForRest);
		
		String ret = String.format(Lang.FinishedHashingXinY,hashed.getName(), durationstring,SizeEnum.timeEstimation(estimatedDurationForRest));

		return ret;
	}
	
	
	
}