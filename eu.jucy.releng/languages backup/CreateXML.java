package xmlhandling;

import java.io.File;
import java.io.FileOutputStream;

public class CreateXML {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File basepath = new File("..");
		
		Translation trans = new Translation();
		
		trans.readProperties(basepath);
		
		File target = new File("TranslationTest.xml");
		FileOutputStream fos = new FileOutputStream(target);
		trans.writeXML(fos);
		fos.close();
		
		
		System.out.println("done");

	}

}
