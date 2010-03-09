package xmlhandling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


public class CreateXML2FromXML {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Translation trans = new Translation(new File(".."));
		
		File source = new File("translation/Translations.xml");
		FileInputStream in = new FileInputStream(source);
		trans.readXML(in);
		in.close();

		File target = new File("TranslationTest.xml");
		FileOutputStream fos = new FileOutputStream(target);
		trans.writeXML2(fos);
		fos.close();
		
	}

}
