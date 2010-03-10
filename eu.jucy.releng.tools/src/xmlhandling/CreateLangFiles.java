package xmlhandling;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public class CreateLangFiles {

	private static final boolean SIMULATE = false;


	public static void createLangFile(File pluginbase,String filename
			,List<String> list,String propertiesname) throws IOException {
		
		Collections.sort(list);

		int i = filename.lastIndexOf('.');
		if (i == -1) {
			throw new IOException("no file provided");
		}
		String packagename = filename.substring(0, i);
		String classname = filename.substring(i+1);
		
		//1. packagename  2. classname , 3. all strings as commaseparated List
		//4. name of properties file  
		//pluginbase+propertiesname+.properties = propertiesfile
		
		File source = new File("translation/Lang.template");
		String langTemplate = read(source);
		
		String concreteLang = String.format(langTemplate
				, packagename
				, classname
				, concat(list,"\n\t\t,","")
				, "nl."+propertiesname);
		 
		File target = new File(pluginbase,"src/"+packagename.replace(".", "/")+"/"+classname+".java");
		
		
		write(target,concreteLang);
		
		
	}
	
	private static String read(File source)  throws IOException {
		FileReader reader = new FileReader(source); 
		StringBuilder builder = new StringBuilder();
		int c;
		while (-1!= (c=reader.read())) {
			builder.append((char)c);
		}
		reader.close();
		return builder.toString();
	}
	
	private static void write(File target,String contents) throws IOException {
		if (SIMULATE) {
			System.out.println("writing file "+target);
			System.out.println("contents: "+contents);
		} else {
			if (!target.isFile() || !contents.equals(read(target))) { // check if the file not already exists and contains the same stuff
				System.out.print(target.getCanonicalPath());
				PrintStream ps = new PrintStream(target);
				ps.print(contents);
				ps.close();
				System.out.println(" ...written");
			} else {
			//	System.out.println(" ...unchanged");
			}
		}
	}
	
	
	
	/**
	 * concatenates  each term in collection using .toString()
	 * and puts between each string "between" 
	 * 
	 * if the map is empty it will return the empty map string instead...
	 * 
	 */
	public static String concat(Iterable<?> terms,String between,String emptyMap) {
		StringBuilder ret = new StringBuilder();
		
		for (Object o: terms) {
			if (ret.length() != 0) {
				ret.append(between);
			} 
			ret.append(o.toString());
		}
		if (ret.length() == 0) {
			return emptyMap;
		} else {
			return ret.toString();
		}
	}

}
