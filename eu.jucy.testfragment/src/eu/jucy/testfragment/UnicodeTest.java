package eu.jucy.testfragment;

public class UnicodeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i =0 ; i < 256;i++) {
			char x = (char)i;
			if (Character.toLowerCase(x) == Character.toUpperCase(x)) {
				System.out.print(x);
			}
		}

	}

}
