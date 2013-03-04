package xmlhandling;

import java.io.File;
import java.io.FileInputStream;


/**
 * create all properties file from XML file
 * 
 * @author Quicksilver
 *
 */
public class CreatePropertiesAndLangFiles {

	private static boolean test = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File basepath = new File(test?".":"../plugins"); 
		
		Translation trans = new Translation(basepath);
		
		File source = new File("translation/lang.xml");
		FileInputStream in = new FileInputStream(source);
		trans.readXML(in);
		in.close();
		
		trans.writeProperties();
		trans.writeLangFiles();
		
		System.out.println("done");

	}

}
