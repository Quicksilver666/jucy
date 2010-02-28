
import java.io.File;
import javax.swing.filechooser.FileSystemView;


public class LeakTest {

	private static long counter = 0;
	
	private static FileSystemView view = FileSystemView.getFileSystemView();

	public static void main(String[] args) throws InterruptedException  {
		
		while (true) {
			for (File f : File.listRoots()) {
				if (f.isDirectory()) {
					countFiles(f);
					System.out.println("found: "+counter);
				}
			}
			System.out.println("found Total: "+counter);
			counter = 0;
			Thread.sleep(1000);
		}
		
		

	}
	
	private static void countFiles(File root) {
		
		for (File f :view.getFiles(root, false)) {
			if (f.isDirectory()) {
				countFiles(f);
			} else {
				counter++;
			}
		}
	}

}
