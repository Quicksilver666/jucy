package eu.jucy.hashengine;




import helpers.GH;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;



import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;




import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import uc.crypto.HashValue;
import uc.crypto.IBlock;
import uc.crypto.IHashEngine;
import uc.crypto.InterleaveHashes;




/**
 * the standard implementation of the IHashEngine Interface...
 * 
 * 
 * @author Quicksilver 
 */
public class HashEngine implements IHashEngine {

	private static final Logger logger = LoggerFactory.make();


	private final BlockingQueue<HashJob> blocksToVerify = new LinkedBlockingQueue<HashJob>();
	private final BlockingQueue<HashJob> filesForHashing = new LinkedBlockingQueue<HashJob>();
	
	/**
	 * set to check if the HashJob is not already present in the 
	 * queue
	 */
	private final Set<HashFileJob> filesToBeHashed = 
		Collections.synchronizedSet(new HashSet<HashFileJob>());
	
	private volatile HashJob currentlyHashed = null;
	
	private volatile long sizeLeftForHashing = 0;
	
	private Set<IHashedListener> listeners = 
		Collections.synchronizedSet(new HashSet<IHashedListener>());
	


	public HashEngine() {
	}
	

	public void init() {
		Runnable r = new Runnable() {
			public void run() {
				try {
					while(true) {
						HashJob h = blocksToVerify.poll(); //by first trying blocks and then files.. but after all waiting on blocks.. its waitfree for blocks and fair for files
						if (h == null) {
							h = filesForHashing.poll();
						}
						if (h == null) {
							h = blocksToVerify.poll(10, TimeUnit.SECONDS);
						}
						currentlyHashed = h;
						if (h != null) {
							h.run();
						}
						synchronized(filesToBeHashed) {
							if (h instanceof HashFileJob) {
								HashFileJob hfj = (HashFileJob)h;
								
								filesToBeHashed.remove(h); //remove if its a file
								sizeLeftForHashing -= hfj.file.length();
							}
							currentlyHashed = null;
						}
						
					}
				} catch(Exception e) {
					logger.error("Error in hashthread: "+e,e);
				}

				execThread(this); //if some error occurs -> reschedule..
			}
		};
		execThread(r);	
	}

	



	private void execThread(Runnable r) {
		Thread t = new Thread(r,"Main-HashThread");
		t.start();
	}



	/**
	 * enqueues a file to be hashed.
	 */

	public void hashFile(File f, IHashedFileListener listener) {

		HashFileJob hfj = new HashFileJob(f,listener);
		synchronized(filesToBeHashed) {
			if (!filesToBeHashed.contains(hfj) && !hfj.equals(currentlyHashed)) {
				filesForHashing.offer(hfj);
				filesToBeHashed.add(hfj);
				sizeLeftForHashing += f.length();
			}
		}
	}
	

	
	

	/**
	 */
	public void clearFileJobs() {
		synchronized(filesToBeHashed) {
//			while (filesTohash.peekLast() instanceof HashFileJob) {
//				filesTohash.pollLast();
//			}
			filesForHashing.clear();
			filesToBeHashed.clear();
			sizeLeftForHashing = 0;
			if (currentlyHashed instanceof HashFileJob) {
				HashFileJob hfj = (HashFileJob)currentlyHashed ;
				filesToBeHashed.add(hfj);
				sizeLeftForHashing+= hfj.file.length();
			}
		}
	}



	
	public boolean verifyInterleaves(InterleaveHashes hashes, HashValue root) {
		if (root == null || hashes == null) {
			throw new IllegalArgumentException("no argument may be null");
		}
		if (hashes.getInterleaves().isEmpty()) {
			return false; //empty interleaves won't match..
		}
		
		TigerTreeHasher2 tiger = new TigerTreeHasher2();
		return root.equals(tiger.hash(hashes));
	}


	public void checkBlock(IBlock block, VerifyListener checkListener) {
		blocksToVerify.offer( new VerifyBlock(block,checkListener) ); //hash with high priority..
	}

	
	public void registerHashedListener(IHashedListener listener) {
		listeners.add(listener);		
	}


	public void unregisterHashedListener(IHashedListener listener) {
		listeners.remove(listener);
	}
	
	



	class VerifyBlock extends HashJob {
	
		
		/**
		 * the block to be verified..
		 */
		private IBlock block;
		
		/**
		 * the listener for returning success..
		 */
		private VerifyListener listener;
		
		VerifyBlock(IBlock block,VerifyListener listener) {
			this.block = block;
			this.listener = listener;
		}
		
	
		public void run() {
			ReadableByteChannel rbc = null;

			try {
				rbc = block.getReadChannel();

				InterleaveHashes inter = getHasher().hash( rbc , block.getLength(), new FakeProgressmonitor() );

				HashValue root = getHasher().hash(inter);
				listener.checked(root.equals(block.getHashOfBlock()));

			} catch(IOException ioe) {
				logger.warn(ioe,ioe);
			} finally {
				GH.close(rbc);
			}
			
		}
		
	}

	/**
	 * same as HashJob1 just for hashing whole files
	 * 
	 * @author Quicksilver
	 */
	class HashFileJob extends HashJob {
	
	
		private final File file;
		private final IHashedFileListener listener;
		
		HashFileJob(File f, IHashedFileListener listener){
			file	=	f;
			this.listener = listener;
		}
		
		public void run() {
			Job hashJob = new Job("Hashing "+file.getName()) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						monitor.beginTask("Hashing "+file.getName(), ((int)(file.length() /65536))+1 );
						HashFileJob.this.run(monitor);
						
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}

				@Override
				protected void canceling() {
					clearFileJobs();
				}
				
				
			};
			hashJob.schedule();
			try {
				hashJob.join();
			} catch(InterruptedException ie) {}
		}
		
		
		
		public void run(IProgressMonitor monitor) {
			Date datechanged = new Date(file.lastModified());
			FileChannel fc = null;
			try {
				
				fc = new FileInputStream(file).getChannel();
				Date before = new Date();
				
				
				InterleaveHashes inter = getHasher().hash( fc , file.length(), monitor);
				
				HashValue root = getHasher().hash(inter);
				
				
				Date after = new Date();
				
				synchronized(listeners) {
					for (IHashedListener listener: listeners) {
						listener.hashed(file, after.getTime()- before.getTime(),sizeLeftForHashing-file.length());
					}
				}	
				listener.hashedFile(file, root, inter,datechanged);
				  
	
			} catch(FileNotFoundException fnfe) {
				logger.debug(fnfe, fnfe);
			} catch(IOException ioe){
				logger.warn(ioe+" File: "+file,ioe);
			} finally {
				GH.close(fc);
			}
			logger.debug("datechanged: "+datechanged);
			if (datechanged.getTime() != file.lastModified()) {
				logger.info("File changed during hashing "+file.getPath());
				//run(monitor);
			} 
		}



		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final HashFileJob other = (HashFileJob) obj;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			return true;
		}
				
	}
	abstract class HashJob implements Runnable {

		private IHasher tiger;
		
		protected IHasher getHasher() {
			if (tiger == null) {
				tiger =  new TigerTreeHasher2();
			}
			return tiger;
		}

	}
}
