package eu.jucy.hashengine;


import helpers.GH;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;


import fr.cryptohash.Digest;
import fr.cryptohash.Tiger;

import uc.DCClient;
import uc.PI;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.crypto.TigerHashValue;

public class TigerTreeHasher2  implements IHasher {

	private static final int BufferSize = 1024 * 64;
	
	private final BlockingQueue<HashPart> partsForHashing = new LinkedBlockingQueue<HashPart>(100);
	
	private volatile boolean hashRunning;
	
	private final List<Map<Integer,HashValue>> levels = new CopyOnWriteArrayList<Map<Integer,HashValue>>();
	
	private final Semaphore sem = new Semaphore(0);
	
	private final Digest md;
	
	private int hashThreads = 1;
	
	private final int maxHashSpeedChunks;
	
	public TigerTreeHasher2() {
		md = new Tiger();
		maxHashSpeedChunks = (int)((PI.getInt(PI.maxHashSpeed)*1024L * 1024L) /BufferSize);
	}
	
	/**
	 * 
	 * @param chan - the channel of the source
	 * @param size - the size from start to end of the file
	 * @return The interleave hashes of the file
	 */
	public InterleaveHashes hash(ReadableByteChannel chan,long size, IProgressMonitor monitor) throws IOException {
		if (size == 0) { //handle the special case of size 0
			return new InterleaveHashes(new HashValue[]{TigerHashValue.ZEROBYTEHASH});
		}
		hashThreads = size <= BufferSize*10? 1 :Runtime.getRuntime().availableProcessors();  
		
		initialiseLevels(getLevel(size));
	
		hashRunning = true;
		
		for (int i = 0; i < hashThreads; i++) { //may be use min maxthreads na segments..
			DCClient.execute(new HashThread());
			//new HashThread(i).start();
		}
		
		long last = System.currentTimeMillis()-1000; //first time more lies more than one sec in the past -> no stopping first time  
		
		ByteBuffer buf = ByteBuffer.allocate(BufferSize);
		int counter = -1;
		while (-1 != chan.read(buf)) {
			buf.flip();
			HashPart part = new HashPart(buf,++counter);
			synchronized(part) {
				try {
					partsForHashing.put(part);
				} catch(InterruptedException ie) {}
			}
			
			if (maxHashSpeedChunks > 0  &&  counter % maxHashSpeedChunks == 0) {
				long timedif = 1000 - (System.currentTimeMillis() - last);
				if (timedif >= 20) {
					GH.sleep(timedif);
				}
				last = System.currentTimeMillis();
			}
			
			buf = ByteBuffer.allocate(BufferSize);
			if (counter % 100 == 0) {
				monitor.worked(100);
			}
		}
		hashRunning = false;
		
		sem.acquireUninterruptibly(hashThreads);
		
		monitor.worked(counter % 100);
	
		
		finishLevel(0);
		HashValue[] inter = getInterleaves();

		return new InterleaveHashes(inter);
	}
	
	private void initialiseLevels(int levels) {
		this.levels.clear();
		for (int i= 0; i < levels; i++) {
			this.levels.add(Collections.synchronizedMap(new HashMap<Integer,HashValue>()));
		}
	}
	
	
	private void finishLevel(int level) {
		if (level < levels.size() -1) {
			Map<Integer,HashValue> hashes = levels.get(level);
			synchronized(hashes) {
				if (!hashes.isEmpty()) {
					if (hashes.size() == 1) {
						Entry<Integer,HashValue> e = hashes.entrySet().iterator().next();
						hashes.clear();
						finishedHashSegment(e.getValue(), e.getKey()/2, level+1,md);
					} else {
						throw new IllegalStateException(""+hashes.size());
					}
				}
			}
		
			finishLevel(level+1);
		}
	}
	
	private HashValue[] getInterleaves() {
		Map<Integer,HashValue> hashes = levels.get(levels.size()-1);
		synchronized(hashes) {
			HashValue[] inter = new HashValue[hashes.size()];
			for (Entry<Integer,HashValue> e:hashes.entrySet()) {
				inter[e.getKey()] = e.getValue();
			}
			return inter;
		}
	}


	
	/**
	 * 
	 * @param interleaves - the interleave hashes of a Merkletree
	 * @return the TTH root hash 
	 */
	public HashValue hash(InterleaveHashes interleaves) {
		if (interleaves.getInterleaves().isEmpty()) {
			throw new IllegalArgumentException("empty interleaves provided");
		}
		
		List<HashValue> values = interleaves.getInterleaves();
		while(values.size() != 1) {
			List<HashValue> nextValues = new ArrayList<HashValue>(values.size()/2 +1);
			
			Iterator<HashValue> it = values.iterator();
			while (it.hasNext()) {
				HashValue first = it.next();
				if (it.hasNext()) {
					nextValues.add(internalHash(first,it.next(),md));
				} else {
					nextValues.add(first);
				}
			}
			values = nextValues;
		}
	
		return values.get(0);
	}
	
	
	
	private static int getLevel(long size) {
		
		long cur = 4*1024 * 1024; // minimum size -> will get 64KiB sizes  (2^6 * 64 KiB) 7 levels
		int level = 1;
		while (cur <= size) {  //this will result in having at least 7 levels of interleaves..
			cur *= 2;
			level++;
		}
		
		while (getPartSize(level) > 1024*1024 ) { //cap size.. at least have a hash for each MiB of the file
			level--;
		}
		
		return level;
	}
	

	
	/**
	 * 
	 * @param level - level above 64KiB
	 * @return the partsize for specified filesize and level used
	 */
	private static long getPartSize(int level) {
		long sizeLevelZero = 64*1024;
		 
		while (--level > 0) {
			sizeLevelZero*=2;
		}
		return sizeLevelZero;
	}
	
	
	private static class HashPart {
		private final ByteBuffer hashPart;
		private final int order;
		
		public HashPart(ByteBuffer hashPart, int order) {
			this.hashPart = hashPart;
			this.order = order;
		}
	}
	
//	private static final CryptixCrypto cr = new CryptixCrypto();
	
	class HashThread implements Runnable {
		
		
	    private final Digest md;
	    private byte[] small = new byte[1025];
	    
	    private HashValue[] hashes = new HashValue[7]; //64 KiB -> 1 2 4 8 16 32 64 -> 7 levels 
	    
		public HashThread() {
			//super("Hasher-"+number);
		//	try {
		        md =  new Tiger(); // MessageDigest.getInstance("Tiger", cr);
		 //  } catch (NoSuchAlgorithmException nsae) {
		  //     nsae.printStackTrace();
		  // }
		    //setPriority(Thread.MIN_PRIORITY);
		}
		
		public void run() {
			int oldPriority = Thread.currentThread().getPriority();
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			try {
				while(true) {
					HashPart part = partsForHashing.poll(30, TimeUnit.MILLISECONDS);
					if (part != null) {
						synchronized(part) {
							while(part.hashPart.hasRemaining()) {
								small[0] = (byte)0;
								int current = Math.min(1024, part.hashPart.remaining());
								part.hashPart.get(small, 1, current);
								md.reset();
								md.update(small, 0, current+1);
								addHash(new TigerHashValue(md.digest()),0);
							}
						
							finishTree();
					
							finishedHashSegment(hashes[hashes.length-1], part.order,0,md);
							hashes[hashes.length-1] = null;
						}
					} else {
						if (!hashRunning) {
							break;
						}
					}
				}
				
			} catch(InterruptedException ie) {
				
			}
			sem.release();
			Thread.currentThread().setPriority(oldPriority);
		}
		
		private void addHash(HashValue hash,int pos) {
			if (hashes[pos] == null) {
				hashes[pos] = hash;
			} else {
				addHash(internalHash(hashes[pos],hash,md),pos+1);
				hashes[pos] = null;
			}
		}
		

	
	    private void finishTree() {
	    	if (hashes[hashes.length-1] != null) { //shortcut ..
	    		return;
	    	}
	    	for (int i = 0 ; i < hashes.length - 1 ; i++) {
	    		if (hashes[i] != null) {
	    			addHash(hashes[i],i+1);
	    			hashes[i]= null;
	    		}
	    	}
	    }
	    
	    

		
	}
	
    /**
     * Computes  the  internal hash value of to childs in the tree..
     * @param firstchild
     * @param secondchild
     * @return
     */
    private static HashValue internalHash(HashValue leftchild, HashValue rightchild, Digest md){
    	md.reset();
    	md.update((byte)1);
    	md.update(leftchild.getRaw());
    	return new TigerHashValue( md.digest(rightchild.getRaw()) );
    }
    
	/**
	 * a finished segment of larger than 64KiB
	 * 
	 * @param hash - the computed hash
	 * @param pos - the position on the 64KiB level
	 */
	private void finishedHashSegment(HashValue hash,int pos,int level,Digest md) {
		Map<Integer,HashValue> map = levels.get(level);
		if (level == levels.size()-1) {
			synchronized(map) {
				map.put(pos, hash);
			}
		} else {
			boolean even = pos % 2 == 0;
			int lookAt =  even ? pos+1: pos-1;
			HashValue found;
			synchronized(map) {
				found = map.remove(lookAt);
				if (found == null) {
					map.put(pos, hash);
				}
			}
			if (found != null) {
				HashValue next = even? internalHash(hash, found,md):internalHash(found, hash,md);
				finishedHashSegment(next,pos/2,level+1,md);
			}
		}
	}
}
