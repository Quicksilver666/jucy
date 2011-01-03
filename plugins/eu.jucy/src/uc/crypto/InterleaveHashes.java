package uc.crypto;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import uc.DCClient;

/**
 * 
 * A class standing for the interleave hashes of one level(in the merkletree) in one file
 * 
 * 
 * @author Quicksilver
 *
 */
public class InterleaveHashes {

	private final List<HashValue> interleaves;

	/**
	 * used for small files < 64 KiB
	 * the interleaves only consist of the root..
	 * 
	 * @param root - the roothash
	 */
	public InterleaveHashes(HashValue root) {
		interleaves = Arrays.asList(root);
	}
	
	public InterleaveHashes(ByteBuffer bbuf) {
		interleaves = new ArrayList<HashValue>();
		while (bbuf.hasRemaining()) {
			byte[] array = new byte[TigerHashValue.digestlength];
			bbuf.get(array);
			interleaves.add(HashValue.createHash(array));
		}
	}
	
	public InterleaveHashes getParentInterleaves() {
		List<HashValue> nextValues = new ArrayList<HashValue>(interleaves.size()/2 +1);
		
		Iterator<HashValue> it = interleaves.iterator();
		while (it.hasNext()) {
			HashValue first = it.next();
			if (it.hasNext()) {
				nextValues.add(first.internalHash(it.next())); 
			} else {
				nextValues.add(first);
			}
		}
		return new InterleaveHashes(nextValues);
	}
	
	/**
	 * 
	 * @param interleaves - the interleaves of one level
	 */
	public InterleaveHashes(HashValue[] interleaves){
		this.interleaves = Arrays.asList(interleaves);
	}
	
	public InterleaveHashes(List<HashValue> interleaves) {
		this.interleaves = interleaves;
	}
	
	/**
	 * creates interleaves from a ' ' separated base32 encoded string ( toString())
	 * @param serilaizedInterleaves 
	 */
	public InterleaveHashes(String serilaizedInterleaves){
		String[] values = serilaizedInterleaves.split(Pattern.quote(" "));
		interleaves = new ArrayList<HashValue>();
		for (int i=0; i < values.length; i++) {
			interleaves.add(HashValue.createHash(values[i]));
		}
	}
	
	/**
	 * 
	 * @return a copy 
	 */
	public List<HashValue> copy(){
		List<HashValue> ret = new ArrayList<HashValue>();
		for (HashValue value: interleaves) {
			ret.add(value.copy());
		}
		return ret;
	}
	
	/**
	 * 
	 * @param roothash - root hash against which verification is done..
	 * @return true if the interleaves match the root hash..
	 */
	public boolean verify(HashValue roothash) {
		return DCClient.get().getHashEngine().verifyInterleaves(this, roothash);
	}
	
	/**
	 * computes the granularity of the file
	 * meaning for how many bytes one single HashValue in this file stands for
	 * @param filesize - the size of the file
	 * @return the minimum size of verification for this file
	 */
	public long getGranularity(long filesize){
		long gran	= filesize / interleaves.size();
		
		long roundup = 1024;
		while ( roundup < gran ) {
			roundup *=2;
		}
		
		return roundup;
		
	}
	
	public int hashValuesSize() {
		return interleaves.size();
	}

	/**
	 * 
	 * @return an array of Hashvalues.. the interleaves
	 */
	public List<HashValue> getInterleaves() {
		return interleaves;
	}
	
	public HashValue getHashValue(int position) {
		return interleaves.get(position);
	}
	
	/**
	 * computes the size in bytes ... needed for uploading to some client
	 * @return number of bytes
	 */
	public long byteSize() {
		return interleaves.size() * TigerHashValue.digestlength; 
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (HashValue hv: interleaves) {
			sb.append(hv.toString());
			sb.append(" ");
		}
		return sb.substring(0, sb.length()-1);
	}
	
	
	
}
