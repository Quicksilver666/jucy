package uc.files.transfer;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
	private static volatile int maxSpeed;
	
	
	
	private static void update() {
		maxSpeed = PI.getInt(PI.uploadLimit);
		if (maxSpeed <= 0) {
			maxSpeed = Integer.MAX_VALUE / DRAINTIME; 
		}
	}
	
	static {
		new PreferenceChangedAdapter(PI.get(),PI.uploadLimit) {
			@Override
			public void preferenceChanged(String preference,String oldValue, String newValue) {
				update();
			}
		};
		update();
		
		
		DCClient.getScheduler().scheduleAtFixedRate(new Runnable() {		
			private int i = 0;
			private int overDue = 0;
			public void run() {
				if (++i % DRAINTIME == 0 ) {
					globalUploads.drainPermits();
				}
				int releaseGlobal = maxSpeed+overDue;
				int globalDiv = UPDATES_PER_SECOND;
				globalUploads.release(releaseGlobal / globalDiv);

				overDue = releaseGlobal % globalDiv; 
				
			}
				
		},1000/UPDATES_PER_SECOND,1000/UPDATES_PER_SECOND,TimeUnit.MILLISECONDS);
		
	}
	
	private static final Logger logger = LoggerFactory.make();

	
	private volatile long bytesTransferred;
	
	private ReadableFileInterval fileInterval;
	
	private volatile WritableByteChannel target;
	private volatile ByteChannel sink; //channel representing the internet connection 
	
	public Upload(FileTransferInformation fti,ClientProtocol cp, ReadableFileInterval rfi) throws IOException {
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
		
	
		ByteBuffer bb = ByteBuffer.allocate(1024);
		bb.clear();
		
		
	//	fireListeners(TransferChange.STARTED);
		int written;
		int toAcquire = 0;
//		runningUploads.add(this);
		try {
			notifyObservers( TransferChange.STARTED);
			while ( source.read(bb) >= 0 || bb.position() != 0) { 
				bb.flip();
				if ((written = target.write(bb)) < 0) { 
					break;
				}
				
				bytesTransferred += written;
					
				
				toAcquire += written;
				
			//	if (!localUpload.tryAcquire(toAcquire/1024)) {
				globalUploads.acquireUninterruptibly(toAcquire/1024);
			//	}
				
				toAcquire %= 1024 ;
				
				bb.compact();
			}

		} finally {
		//	runningUploads.remove(this);
			GH.close(source); //target is no longer closed.. but must be flushed somehow..
			notifyObservers(TransferChange.FINISHED);
			
			fw.finnish();			//flush ..
			
			
		}
		logger.debug("finished upload");
	}

	@Override
	public long getBytesTransferred() {
		return bytesTransferred;
	}
	
	
	
}
