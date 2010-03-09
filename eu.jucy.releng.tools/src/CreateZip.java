

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CreateZip {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		File buildfolder = new File(".").getAbsoluteFile();
		createZipOfBuildDir(buildfolder);
	}
	public static void createZipOfBuildDir(File buildfolder) throws IOException {
		for (File f:buildfolder.listFiles()) {
			if (f.isDirectory() && f.listFiles().length == 1) {
				System.out.println("Creating new Zipfile of: "+f.listFiles()[0].getPath());
				createZipOfDir(f.listFiles()[0]);
			}
		}
	}
	
	private static void createZipOfDir(File dir) throws IOException {
		File zipFile = new File(dir.getParentFile().getParentFile(),dir.getName()+"."+dir.getParentFile().getName()+".zip");
		System.out.println("creating zipfile: "+zipFile);
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		out.setLevel(9);
		rekzipDir(out,dir,dir);
		out.close();
		System.out.println("finished");
	}
	
	private static void rekzipDir(ZipOutputStream zip,File parentFolder,File folder) throws IOException {
		for (File f:folder.listFiles()) {
			if (f.isFile()) {
				addFile(zip,parentFolder,f);
			} 
			if (f.isDirectory()) {
				rekzipDir(zip,parentFolder,f);
			}
		}
	}
	
	private static void addFile(ZipOutputStream zip,File parentFolder,File file) throws IOException{
		String relativename = file.getPath().substring(parentFolder.getParent().length()+1);
		System.out.println("adding: "+relativename);
		byte[] buf = new byte[1024]; 

		FileInputStream in = new FileInputStream(file); // Add ZIP entry to output stream. 
		ZipEntry ze = new ZipEntry(relativename);
		ze.setSize(file.length());
		zip.putNextEntry(ze); // Transfer bytes from the file to the ZIP file 
		int len; 
		while ((len = in.read(buf)) > 0) { 
			zip.write(buf, 0, len); 
		} // Complete the entry 
		
		zip.closeEntry(); 
		in.close(); 
	}
	

}
