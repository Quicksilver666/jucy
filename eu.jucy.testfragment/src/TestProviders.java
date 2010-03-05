import java.security.Provider;
import java.security.Security;


public class TestProviders {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int i = 0;
		for (Provider p :Security.getProviders()) {
			i++;
			System.out.println(i+": "+p.getName());
		}

	}

}
