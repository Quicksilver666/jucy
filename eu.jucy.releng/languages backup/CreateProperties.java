package xmlhandling;

import java.io.File;
import java.io.FileInputStream;


/**
 * create all properties file from XML file
 * 
 * @author Quicksilver
 *
 */
public class CreateProperties {

	private static boolean test = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File basepath = new File(test?".":".."); 
		
		Translation trans = new Translation();
		
		File source = new File("translation/Translations.xml");
		FileInputStream in = new FileInputStream(source);
		trans.readXML(in);
		in.close();
		
		trans.writeProperties(basepath);
		
		
		System.out.println("done");

	}

}
