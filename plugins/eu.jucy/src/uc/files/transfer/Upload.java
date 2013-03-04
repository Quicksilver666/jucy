package uc.files.transfer;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


import uc.DCClient;
import uc.PI;
import uc.protocols.Compression.FinishWrapper;
import uc.protocols.client.ClientProtocol;

public class Upload extends AbstractFileTransfer {

	
	private static final int UPDATES_PER_SECOND = 10;
	private static final int DRAINTIME = UPDATES_PER_SECOND*5;
	private static Semaphore globalUploads = new Semaphore(1);
//	private static final CopyOnWriteArrayList<Semaphore> RUNNING_UPLOADS = new CopyOnWriteArrayList<Semaphore>();
	
//	private final Semaphore localUploads = new Semaphore(1);
	private volatile static ScheduledFuture<?> updateTask;
	
	private static void update() {
		if (updateTask != null) {
			updateTask.cancel(false);
		}
		updateTask = DCClient.getScheduler().scheduleAtFixedRate(new Runnable() {
			private final int maxSpeed = PI.getInt(PI.uploadLimit) <= 0? 
					Integer.MAX_VALUE / DRAINTIME
					: PI.getInt(PI.uploadLimit);
	
			private int i = 0;
			private int overDue = 0;
		
			public void run() {
	//			int divFactor= RUNNING_UPLOADS.size() * UPDATES_PER_SECOND;
				
	//			if (++i % DRAINTIME == 0 ) {
	//				int drained = 0;
	//				for (Semaphore sem:RUNNING_UPLOADS) {
	//					drained += sem.drainPermits();
	//				}
	//				overDue += drained % divFactor;
	//			}
				if (++i % DRAINTIME == 0 ) {
					globalUploads.drainPermits();
				}
				int releaseGlobal = maxSpeed+overDue;
			
				globalUploads.release(releaseGlobal / UPDATES_PER_SECOND);

				overDue = releaseGlobal % UPDATES_PER_SECOND; 
				
//				int releaseLocal  = (maxSpeed+overDue);
//				if (releaseLocal > 0) {
//					for (Semaphore sem:RUNNING_UPLOADS) {
//						sem.release(releaseLocal);
//					}
//				}
//				overDue = releaseLocal % divFactor;
			
			}
				
		},1000/UPDATES_PER_SECOND,1000/UPDATES_PER_SECOND,TimeUnit.MILLISECONDS);
	}
	
	static {
		new PreferenceChangedAdapter(PI.get(),PI.uploadLimit) {
			@Override
			public void preferenceChanged(String preference,String oldValue, String newValue) {
				update();
			}
		};
		update();
		
		
		
	}
	
	private static final Logger logger = LoggerFactory.make();

	
	private volatile long bytesTransferred;
	
	private AbstractReadableFileInterval fileInterval;
	
	private volatile WritableByteChannel target;
	private volatile ByteChannel sink; //channel representing the internet connection 
	
	public Upload(FileTransferInformation fti,ClientProtocol cp, AbstractReadableFileInterval rfi) throws IOException {
		super(fti,cp);
		fileInterval = rfi;
	}


	public void cancel() {
		GH.close(sink,target); //closes sink first -> should prevent closing target from blocking
	}
	
	

	@Override
	public boolean isUpload() {
		return true;
	}


	public AbstractFileInterval getFileInterval() {
		return fileInterval;
	}
	
	@Override
	public void transferData(ByteChannel sink) throws IOException {
		this.sink = sink;

		FinishWrapper fw = getCompression().wrapOutgoing(sink); //add compression
		this.compressor = fw;
		this.target = fw.getChan();  
	
		ReadableByteChannel source = fileInterval.getReadableChannel();
		
		ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
		bb.clear();
	//	RUNNING_UPLOADS.add(localUploads);
		int written;
		int toAcquire = 0;
		try {
			notifyObservers( TransferChange.STARTED);
		//	logger.info("Upload started: "+fileInterval.toString());
			while ( source.read(bb) >= 0 || bb.position() != 0) { 
				bb.flip();
				if ((written = target.write(bb)) < 0) { 
					break;
				}
				
				bytesTransferred += written;
				toAcquire += written;
				globalUploads.acquireUninterruptibly(toAcquire/1024);
				//localUploads.acquireUninterruptibly(toAcquire/1024);
				toAcquire %= 1024 ;
				
				bb.compact();
			}

		} finally {
	//		RUNNING_UPLOADS.remove(localUploads);
			
			GH.close(source); 
			notifyObservers(TransferChange.FINISHED);
			if (bytesTransferred == fileInterval.length) { //only on success wrap up
				fw.finnish();
			}
		//	logger.info("Upload "+(bytesTransferred==fileInterval.length?"succ":"fail") 
		//			+": "   +fileInterval.toString()+"   bytes transferred: "+bytesTransferred);
			
		}
		logger.debug("finished upload");
	}

	@Override
	public long getBytesTransferred() {
		return bytesTransferred;
	}
	
	
	
}
