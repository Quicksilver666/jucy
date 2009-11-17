package uc.crypto;

import static org.junit.Assert.*;


import java.io.PrintStream;


import org.junit.Test;

public class BloomFilterTest {

	@Test
	public void testAddHashValue() {
		
		// m , h , k
		BloomFilter blom = new BloomFilter(1024,24,8);
		
		HashValue hash = new TigerHashValue("UDRJ6EGCH3CGWIIU2V6CH7VLFN4N2PCZKSPTBQA"),
					hash2 = new TigerHashValue("HSLRLA6TA6LWZICV326JTMKWWAJU4JP4GHWEG3Y"),
					hash3 = new TigerHashValue("QNWNDWLXIHZDZRQA7XEMHX7D5PJCZEQSTM7A45I");
		
		blom.addHashValue(hash);
		blom.addHashValue(hash2);
		
		assertTrue(blom.possiblyContains(hash));
		assertTrue(blom.possiblyContains(hash2));
		assertFalse(blom.possiblyContains(hash3));
		
		BloomFilter blom2 = new BloomFilter(1024,64,2);
		blom2.addHashValue(hash);
		blom2.addHashValue(hash2);
		
		assertTrue(blom2.possiblyContains(hash));
		assertTrue(blom2.possiblyContains(hash2));
		assertFalse(blom2.possiblyContains(hash3));
		
		byte[] b = blom2.getBytes();
		
		assertEquals(1024 / 8, b.length);
		
		BloomFilter blom2copy = new BloomFilter(b,64,2);
		
		assertTrue(blom2copy.possiblyContains(hash));
		assertTrue(blom2copy.possiblyContains(hash2));
		assertFalse(blom2copy.possiblyContains(hash3));
		
		
	}
	
	/**
	 * creates demo files..
	 */
	public static void main(String... args) throws Exception {
	//	File f = new File("demo");
	//	FileOutputStream fos = new FileOutputStream(f);
		PrintStream ps = System.out;//new PrintStream(f);
		
		
		HashValue nullHash = new TigerHashValue("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		
		write(ps,1024, 24, 8,nullHash);
		
		HashValue oneBitHash = new TigerHashValue("BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		write(ps,1024, 24, 8,oneBitHash);
		
		HashValue highestBitHash = new TigerHashValue("QAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		write(ps,1024, 24, 8,highestBitHash);
	
		write(ps,1024, 24, 8,oneBitHash,highestBitHash);
		
		
		HashValue normalHash = new TigerHashValue("UDRJ6EGCH3CGWIIU2V6CH7VLFN4N2PCZKSPTBQA");
		
		write(ps,1024, 24, 8,normalHash);
		
		
		write(ps,1024, 64, 2,nullHash);
		write(ps,1024, 64, 2,oneBitHash);
		write(ps,1024, 64, 2,highestBitHash);
		write(ps,1024, 64, 2,oneBitHash,highestBitHash);
		write(ps,1024, 64, 2,normalHash);
		
		ps.close();
		
	}
	
	private static void write(PrintStream ps,int m, int h, int k,HashValue... toadd) {
		BloomFilter blom = new BloomFilter(m,h,k);
		
		ps.println("Bloomfilter attib  m: "+m+" h:"+h+" k:"+k);
		for (HashValue hv:toadd)  {
			blom.addHashValue(hv);
			ps.println("Added hashValue: "+hv);
		}
		ps.println("Bloomfilter Base32: "+blom);
		
		ps.println();
	}
	
	
	
	

}
