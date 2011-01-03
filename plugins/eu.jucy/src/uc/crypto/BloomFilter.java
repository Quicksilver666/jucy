package uc.crypto;


import helpers.GH;

import java.util.BitSet;



import uc.files.filelist.FileList;
import uc.files.filelist.FileListFile;

/**
 * 
Implementation: DC++, ADCH++

Description: Bloom filters allow the hub to filter certain searches using 
bitmap that represents the hashes of the files in the users share. For an 
introduction to bloom filters, see Wikipedia. When the user updates the share, 
it must send an INF containing the flag SF. The hub may at any time request 
that the client sends an updated version of its bloom filter by sending a 
GET command to the client. The client will then respond using SND and send 
the bloom filter binary data.

Let b be the number of bits used for file hashes, n the number of files in 
the users share, m the size of the bloom filter in bits, k the number of subhashes 
constructed from the file hash, h the number of bits to use for each subhash 
and p the probability of a false positive. The hub chooses k, h and m. k must 
be chosen so that k*h <= b. h must be chosen <= 64 and such that 2^h > m. m 
must be chosen such that m % 64 == 0. The hub then sends GET specifying "blom" 
as type, "/" as path, "0" as start and "m/8" as number of bytes, k in the flag 
"BK" and h in the flag "BH".

The client constructs the bloom filter by creating a bit array of m bits set to 0. 
For each file it then sets to "1" k positions constructed from the file hash. Seeing 
the file hash as a stream of bits (starting from the lowest bit of the first byte, 
ending at the highest bit of the last byte), the client should use h bits starting 
at the first bit of the first byte to create an integer and apply modulo m to get 
the position in the bit array, then redo the process k times advancing the starting 
position by h each time.

Once the hub has received the bloom filter bit array, for each search command it 
processes, if it contains a hash search term, it can discard skip broadcasting 
the search to a particular client if at least one of the k bits in that clients 
bit array is "0", calculating positions as the client does when setting bits to 
"1". The hub has to evaluate the filter for each client that it has a bloom filter 
for, for each search.

p = (1 - (1 - 1 / m)^(k * n))^k, thus p becomes smaller as m grows and larger as n 
grows. Larger m means more bits to transfer but also fewer false positives. The 
optimum value for k given m and n is (m / n) * ln 2. The largest k supported by a 
hash of a certain size is b / h, so if the hub wants the smallest p possible, it 
should choose the smallest possible h which gives the largest k, and then calculate 
m = k * n/ln 2, checking that the resulting m < 2^h. 2^h should much be larger than m 
(at least 3-4 times), because of how the modulo operator works. Also, with m grows the 
required bandwidth to transfer the bloom filter, so the hub may wish to cap m. In that 
case, it should still choose k according to m / n * ln 2, but send an h as big as possible 
to alleviate biasing problems.

For TTH roots, b is 192 and a reasonable value for h is 24, giving a maximum k = 8 which 
means that m = 8 * n / ln 2 ≈ 11.5 * n. The required bandwidth then becomes 11.5 * n / 8 bytes, 
so approximately 1.44 bytes per file in the users share. For 20000 files, m should then be 
230016 (taking into account the modulo 64 requirement), giving a p = 0.004, in other words 
~99.6% of all searches that don't match a users share would not be sent, saving valuable upload 
bandwidth for the hub. The client calculates i valid positions, if x is an array of bytes 
containing the hash of the file, on a little-endian machine, by doing 
pos = x[0+i*h/8] | (x[1+i*h/8] << 8) | (x[2+i*h/8] << 16) for i = [0;k). 
This is of course a special case where h % 8 = 0, the actual algorithm has to take into 
consideration values for h where the integer crosses byte boundaries. 
 *
 */
public class BloomFilter {

	//private final byte[] filter;
//	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	private final BitSet bits;
	
	private static final int b = TigerHashValue.digestlength * 8;
	
	private final int h,k,m;
	
	
	public static BloomFilter create(FileList fileList, int m, int h, int k) {
		BloomFilter blom = new BloomFilter(m,h,k);
		
		for (FileListFile f:fileList.getRoot()) {
			blom.addHashValue(f.getTTHRoot());
		}
	//	logger.debug("Created Bloomfilter: k:"+k+" h:"+h+" m:"+m);
		
		return blom;
	}
	
	/**
	 * 
	 * @param m  the size of the Filter in Bits
	 * @param h - 
	 * @param k
	 */
	public BloomFilter(int m, int h, int k) {
		if (k*h > b ) {
			throw new IllegalArgumentException("k*h > b!  k:"+k+" h:"+h);
		}
		if (h > 64) {
			throw new IllegalArgumentException("h:"+h);
		}
		if (m % 64 != 0) {
			throw new IllegalArgumentException("m:"+m);
		}
		
		this.m = m;
		bits = new BitSet(m);
		this.h = h;
		this.k = k;
	}
	
	/**
	 * constructs bloomfilter from bytearray..
	 * 
	 * @param bytes - bytes being bloomfilter
	 * @param h 
	 * @param k
	 */
	public BloomFilter(byte[] bytes,int h,int k) {
		this(bytes.length * 8,h,k);
		
		BitSet bs = GH.toSet(bytes);
		bits.or(bs);
	}
	
	
	public void addHashValue(HashValue hash)  {
		/*
		 * The client constructs the bloom filter by creating a bit array of m bits 
		 * set to 0.  
		 * For each file it then sets to "1" k positions constructed from 
		 * the file hash. 
		 * Seeing the file hash as a stream of bits (starting from the 
		 * lowest bit of the first byte, ending at the highest bit of the last byte), 
		 * the client should use h bits starting at the first bit of the first byte to 
		 * create an integer and apply modulo m to get the position in the bit array, 
		 * then redo the process k times advancing the starting position by h each time
		 */
		for (int i = 0; i < k; i++ ) {
			int pos = calcPos(hash.getRaw(),i*h);
			bits.set(pos);
		}
	}
	
	public boolean possiblyContains(HashValue hash) {
		boolean possiblyContains = true;
		for (int i = 0; i < k && possiblyContains; i++ ) {
			int pos = calcPos(hash.getRaw(),i*h);
			possiblyContains &= bits.get(pos);
		}
		return possiblyContains;
	}
	
	private int calcPos(byte[] hash, int startpos) {
		long pos = getLong(hash,startpos,h);
		if (pos < 0 ) { //circumvent sign for negative numbers..
			long modpos = pos >>> 1;
			long res = modpos % m +  pos % 2 ;
			return (int)res % m;
		} else {
			return (int) (pos % m);
		}
	}
	
	private static long getLong(byte[] source,int startpos,int length) {
		long ret = 0;
		for (int i = 0; i < length; i++) {
			boolean b = GH.getBit(source,startpos+i);
			if (b) {
				ret +=  1 << i;
			}
		}
		return ret;
	}
	


	/**
	 * fill a byte array with the BloomFilter ... starting with the first bit
	 * of the BloomFilter as lowest bit of the first byte of the byte array.
	 * 
	 * @return
	 */
	public byte[] getBytes() {
		return GH.toBytes(bits,m);
	}
	
	public String toString() {
		return BASE32Encoder.encode(getBytes());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bits == null) ? 0 : bits.hashCode());
		result = prime * result + h;
		result = prime * result + k;
		result = prime * result + m;
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
		BloomFilter other = (BloomFilter) obj;
		if (bits == null) {
			if (other.bits != null)
				return false;
		} else if (!bits.equals(other.bits))
			return false;
		if (h != other.h)
			return false;
		if (k != other.k)
			return false;
		if (m != other.m)
			return false;
		return true;
	}
	
	
	
	
}
