package eu.jucy.testfragment;

import helpers.GH;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;

public class SpeedTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		File test = new File("test.bin");
		System.out.println(test.getCanonicalPath());
		Random rand = new Random();
		FileOutputStream fos = new FileOutputStream(test);
		byte[] b = new byte[1024];
		for (int i = 0; i < 512*1024;i++) { //one gib size
			rand.nextBytes(b);
			fos.write(b);
		}
		fos.close();
		
		File target = new File("testWrite.bin");
		long beginTime = System.currentTimeMillis();
		moveA(test,target);
		System.out.println("timeA: " +(System.currentTimeMillis()-beginTime));
		beginTime = System.currentTimeMillis();
		
		moveB(test,target);
		System.out.println("timeB: " +(System.currentTimeMillis()-beginTime));
		beginTime = System.currentTimeMillis();
		moveC(test,target,64*1024);
		System.out.println("timec: " +(System.currentTimeMillis()-beginTime));
		beginTime = System.currentTimeMillis();
		moveC(test,target,1024*1024);
		System.out.println("timecMiB: " +(System.currentTimeMillis()-beginTime));
		beginTime = System.currentTimeMillis();
		
		moveC(test,target,2*1024*1024);
		System.out.println("timec2MiB: " +(System.currentTimeMillis()-beginTime));
		beginTime = System.currentTimeMillis();
		moveC(test,target,4*1024*1024);
		System.out.println("timec4MiB: " +(System.currentTimeMillis()-beginTime));
		
		
	}
	
	
	
	private static void moveA(File source,File dest) throws IOException{
		FileInputStream in = null;
		FileChannel sourcec = null; 
		FileChannel destc  = null;
		try {
			in = new FileInputStream(source);
			sourcec = in.getChannel();
		

			destc = new RandomAccessFile(dest,"rw").getChannel();
			
			long left = source.length();
			long position= 0;
			long count;

			while(-1 != (count = sourcec.transferTo(position, Math.min(left,1024*64) , destc))){
				position += count;
				left -= count;
				if (left == 0) {
					break;
				}
			}
			//close the files
			destc.force(true);

		} catch (IOException ioe) {
			throw ioe;
		} finally {
			GH.close(destc,sourcec,in);
		}	
	}
	
	private static void moveB(File source,File dest) throws IOException{
		FileInputStream in = null;
		FileChannel sourcec = null; 
		FileChannel destc  = null;
		try {
			in = new FileInputStream(source);
			sourcec = in.getChannel();
		

			destc = new RandomAccessFile(dest,"rw").getChannel();
			
			long left = source.length();
			long position= 0;
			long count;

			ByteBuffer bb = ByteBuffer.allocate(1024*64);
			
			while(-1 != sourcec.read(bb, position)) {
				bb.flip();
				count = destc.write(bb, position);
				bb.compact();
				
				position += count;
				left -= count;
				if (left == 0) {
					break;
				}
			}
			//close the files
			destc.force(true);

		} catch (IOException ioe) {
			throw ioe;
		} finally {
			GH.close(destc,sourcec,in);
		}	
	}
	
	private static void moveC(File source,File dest,int size) throws IOException{
		FileInputStream in = null;
		FileChannel sourcec = null; 
		FileChannel destc  = null;
		try {
			in = new FileInputStream(source);
			sourcec = in.getChannel();
		

			destc = new RandomAccessFile(dest,"rw").getChannel();
			
			long left = source.length();
			long position= 0;
			long count;

			MappedByteBuffer mbb = null;
			ByteBuffer bb = ByteBuffer.allocate(64*1024);
			
			
			while(null != (mbb=sourcec.map(MapMode.READ_ONLY, position, Math.min(size,left)))) {
			//	bb.flip();
				while (mbb.hasRemaining()) {
					mbb.get(bb.array());
					count = destc.write(bb, position);
					position += count;
					left -= count;
					bb.clear();
				}
			
			//	bb.compact();
				
				
				if (left == 0) {
					break;
				}
			}
			//close the files
			destc.force(true);

		} catch (IOException ioe) {
			throw ioe;
		} finally {
			GH.close(destc,sourcec,in);
		}	
	}
	
	

}
