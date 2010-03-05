package xmlhandling;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public class CreateLangFiles {

	private static final boolean SIMULATE = true;

//	
//	public static void createLangFile(File pluginbase,String packagename,String classname
//			,String propertiesname) throws IOException {
//		File propertiesFile = new File(pluginbase,propertiesname+".properties");
//		if (propertiesFile.isFile()) {
//			
//		} else {
//			throw new FileNotFoundException();
//		}
//	}
//	
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
		//4. name of properties file   5. classname == 2
		
		//pluginbase+propertiesname+.properties = propertiesfile
		
		File source = new File("Lang.template");
		String langTemplate = read(source);
		
		String concreteLang = String.format(langTemplate
				, packagename
				, classname
				, concat(list,"\n\t\t,","")
				, propertiesname
				,classname);
		 
		File target = new File(pluginbase,"src/"+packagename.replace(".", "/")+"/"+classname+".java");
		System.out.println(target);
		
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
			PrintStream ps = new PrintStream(target);
			ps.print(contents);
			ps.close();
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
