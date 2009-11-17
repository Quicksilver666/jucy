

import java.util.Random;

public class NativeHasher {


	/**
	 * Native method declaration
	 * 
	 * gives an array length multiple of 64KiB
	 * to be hashed to the same amount of digests.
	 */
	native byte[] hashBytes(byte[] toHash);
	//Load the library
	static {
		System.loadLibrary("nHasher");
	}

	public static void main(String args[]) {
		byte buf[];
		//Create class instance
		NativeHasher mappedFile=new NativeHasher();
		byte[] test = new byte[64*1024 *5];
		new Random().nextBytes(test);
		
		//Call native method to load ReadFile.java
		buf = mappedFile.hashBytes(test);
		//Print contents of ReadFile.java
		for(int i=0;i<buf.length;i++) {
			System.out.print((char)buf[i]);
		}
	}

	  
}
