package helpers;



import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * a little test for the substring map .. because it didn'T work properly at the beginning..
 * 
 * also later for testing of performance of different variants
 * 
 * @author Quicksilver
 *
 */
public class SubstringMapTest2  extends TestCase{

	private static final String[] strings= new String[]{
		"HelloWorld",
		"dabraka",
		"abraka",
		"pertraka",
		"abrakadabra",
		"bradabrabra",
		"drabrabra"
	}; 
	
	
	
	private SubStringMap3<String> testmap;
	
	
	@Before
	public void setUp() throws Exception {
		testmap = new SubStringMap3<String>(new ISubstringMapping2<String>(){

			@Override
			public String getMappingString(String item) {
				return item;
			}

			@Override
			public Set<String> mapItems(Set<String> nodeItems) {
				return nodeItems;
			}
			
		});
		
		
	}

	@After
	public void tearDown() throws Exception {
		testmap = null;
	}

	@Test
	public void testPut() {
	
		for (String s: strings) {
			testmap.put( s);
		}
		//search for ello in the substring map... should yield only strings[1] as result
		//HashSet<String> search = new HashSet<String>(Arrays.asList("ello"));
		String search = "ello";
		Set<String> results = testmap.search(search);
		
		assertSame(1, results.size());
		assertTrue(results.contains(strings[0])); 
		
	}

	@Test
	public void testSearch() {
		for (String s: strings) {
			testmap.put( s);
		}
		//----------------------------- abra should yield 5 hits 
		String search = "abra";
		Set<String> results = testmap.search(search);
		for (String s:results) {
			System.out.println(s);
		}
		
		assertSame(5, results.size());
		
		//---------------- abra and drab should only have one result   (strings[6])
		HashSet<String> search2 = new HashSet<String>(Arrays.asList("abra","drab"));
		results = testmap.search(search2,null);
		
		assertSame(1, results.size());
		assertTrue(results.contains(strings[6]));

	}

}
