package eu.jucy.hashengine;




import helpers.GH;
import helpers.SizeEnum;

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

import java.util.concurrent.PriorityBlockingQueue;






import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import uc.crypto.HashValue;
import uc.crypto.IBlock;
import uc.crypto.IHashEngine;
import uc.crypto.InterleaveHashes;
import uc.database.HashedFile;




/**
 * the standard implementation of the IHashEngine Interface...
 * 
 * 
 * @author Quicksilver 
 */
public class HashEngine implements IHashEngine {

	private static final Logger logger = LoggerFactory.make();

	private final PriorityBlockingQueue<HashJob> jobQueue = new PriorityBlockingQueue<HashJob>();

	
	/**
	 * set to check if the HashJob is not already present in the 
	 * queue
	 */
	private final Set<HashFileJob> filesToBeHashed = 
		Collections.synchronizedSet(new HashSet<HashFileJob>());
	
	private volatile HashJob currentlyHashed = null;
	
	private volatile long sizeLeftForHashing = 0;
	private volatile long hashedConsecutively = 0;
	private static final long WCF = 1000000; // correction factor...
	
	private Set<IHashedListener> listeners = 
		Collections.synchronizedSet(new HashSet<IHashedListener>());
	

	//private final Object hashersynch = new Object();
	private volatile Job hasher;
	private volatile boolean shutdown = false;

	public HashEngine() {
	}
	

	
	
	public void init() {
		shutdown = false;
	}

	public void stop() {
		shutdown = true;
		synchronized(filesToBeHashed) {
			while (hasher != null) {
				hasher.cancel();
				try {
					filesToBeHashed.wait(50);
				} catch (InterruptedException e) {
					logger.warn(e,e);
				}
			}
		}
		clearFileJobs();
		jobQueue.clear();
	}
	



	private void scheduleIfNeeded() {
		synchronized(filesToBeHashed) {
			if (hasher == null & !shutdown) {
				hasher = new Job("Hashing "+SizeEnum.getReadableSize(hashedConsecutively+sizeLeftForHashing)) {
					private long total;
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						synchronized(filesToBeHashed) {
							total = hashedConsecutively+sizeLeftForHashing;
						}
						monitor.beginTask("Hashing", (int)(total/WCF));
						monitor.worked((int)(hashedConsecutively/WCF));		
						try {
							while(!monitor.isCanceled()) {
								HashJob h = jobQueue.poll();
								
								if (h != null) {
									currentlyHashed = h;
									if (h instanceof HashFileJob) {
										monitor.subTask(((HashFileJob) h).file.getName());
									} else {
										monitor.subTask("Chunk");
									}
									h.run();
								} else {
									hashedConsecutively = 0;
									break;
								}
								synchronized(filesToBeHashed) {
									filesToBeHashed.remove(h); 
									sizeLeftForHashing -= h.getSize();
									hashedConsecutively+= h.getSize();
									monitor.worked((int)(h.getSize() / WCF));
									currentlyHashed = null;
									if (total != hashedConsecutively+sizeLeftForHashing) {
										break;
									}
								}	
							}
						} catch(Exception e) {
							logger.error("Error in hashthread: "+e,e);
						} finally {
							monitor.done();
							synchronized(filesToBeHashed) {
								hasher = null;
							}
							if (!jobQueue.isEmpty() && !monitor.isCanceled()) {
								scheduleIfNeeded();
							} 
							
						}
						return Status.OK_STATUS;
					}
				};
				hasher.setUser(false);
				
				hasher.setSystem(sizeLeftForHashing < 1L*1000L*1000L*1000L | filesToBeHashed.size() <= 2); // not visible for small amount of hashing..
				
				hasher.schedule();
			}
		}
	}



	/**
	 * enqueues a file to be hashed.
	 */

	public void hashFile(File f,boolean highPriority, IHashedFileListener listener) {
		HashFileJob hfj = new HashFileJob(f,listener,highPriority);
		synchronized(filesToBeHashed) {
			if (!filesToBeHashed.contains(hfj) && !hfj.equals(currentlyHashed)) {
				jobQueue.offer(hfj);
				//(highPriority? highPriorityQueue: lowPriorityQueue).offer(hfj);
				filesToBeHashed.add(hfj);
				sizeLeftForHashing += f.length();
				scheduleIfNeeded();
			}
		}
	}
	
	public void checkBlock(IBlock block, VerifyListener checkListener) {
		VerifyBlock vb = new VerifyBlock(block,checkListener);
		synchronized(filesToBeHashed) {
			jobQueue.offer(vb);
			sizeLeftForHashing += vb.getSize();
			scheduleIfNeeded();
		}
	}
	
	

	/**
	 */
	public void clearFileJobs() {
		synchronized(filesToBeHashed) {
			jobQueue.removeAll(filesToBeHashed);
			filesToBeHashed.clear();
			sizeLeftForHashing = 0;
			if (currentlyHashed instanceof HashFileJob) {
				HashFileJob hfj = (HashFileJob)currentlyHashed ;
				filesToBeHashed.add(hfj);
				sizeLeftForHashing+= hfj.file.length();
			}
			for (HashJob job:jobQueue) {
				sizeLeftForHashing+= job.getSize();
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
			super(2);
			this.block = block;
			this.listener = listener;
		}
		
	
		public void run() {
			ReadableByteChannel rbc = null;

			try {
				rbc = block.getReadChannel();

				InterleaveHashes inter = getHasher().hash( rbc , block.getLength(), new NullProgressMonitor() );

				HashValue root = getHasher().hash(inter);
				GH.close(rbc);
				listener.checked(root.equals(block.getHashOfBlock()));

			} catch(IllegalArgumentException iae) {
				if (Platform.inDevelopmentMode()) {
					logger.error("Interleaves are null for block: "+block+"  len: "+block.getLength());
				}
				throw iae;
			} catch(IOException ioe) {
				logger.warn(ioe,ioe);
			} finally {
				GH.close(rbc);
			}
		}


		@Override
		public int compareTo(HashJob o) {
			int x = super.compareTo(o);
			if (x == 0 & o instanceof VerifyBlock) {
				VerifyBlock vb = (VerifyBlock)o;
				return block.compareTo(vb.block);
			}
			return x;
		}
		
		@Override
		public long getSize() {
			return block.getLength();
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
		
		HashFileJob(File f, IHashedFileListener listener, boolean highPriority) {
			super(highPriority? 1:10);			
			file	=	f;
			this.listener = listener;
		}
		
		public void run() {
			Job hashJob = new Job("Hashing "+file.getName()) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					IStatus s = null;
					try {
						monitor.beginTask("Hashing "+file.getName(), (int)(file.length()/65536)+1 );
						s = HashFileJob.this.run(monitor);
						
					} finally {
						monitor.done();
					}
					return s;
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
		
		
		
		public IStatus run(IProgressMonitor monitor) {
			Date datechanged = new Date(file.lastModified());
			FileChannel fc = null;
			try {
				
				fc = new FileInputStream(file).getChannel();
				Date before = new Date();
				
				
				InterleaveHashes inter = getHasher().hash( fc , file.length(), monitor);
				if (inter == null || monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				while (inter.byteSize() > 1024*256) { //128 KiB max size -> DB does not allow more...
					inter = inter.getParentInterleaves();
				}
				
				HashValue root = getHasher().hash(inter);
				
				
				Date after = new Date();
				
				synchronized(listeners) {
					logger.info("size left: "+sizeLeftForHashing + "  "+file.length());
					for (IHashedListener listener: listeners) {
						listener.hashed(file, after.getTime()- before.getTime(),sizeLeftForHashing-file.length());
					}
				}	
				HashedFile hf = new HashedFile(datechanged,root,file);
				listener.hashedFile( hf, inter);
				  
	
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
			return Status.OK_STATUS;
		}

		

		@Override
		public long getSize() {
			return file.length();
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
		
		@Override
		public int compareTo(HashJob o) {
			int x = super.compareTo(o);
			if (x == 0 & o instanceof HashFileJob) {
				HashFileJob hfj = (HashFileJob)o;
				return file.compareTo(hfj.file);
			}
			return x;
		}
				
	}
	abstract class HashJob implements Runnable ,Comparable<HashJob> {

		private final int priority;
		private IHasher tiger;
		
		
		
		public HashJob(int priority) {
			super();
			this.priority = priority;
		}



		protected IHasher getHasher() {
			if (tiger == null) {
				tiger =  new TigerTreeHasher2();
			}
			return tiger;
		}


		@Override
		public int compareTo(HashJob o) {
			return GH.compareTo(priority, o.priority);
		}
		
		public abstract long getSize();

	}
}
