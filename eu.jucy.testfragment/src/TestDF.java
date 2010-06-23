import java.io.File;


public class TestDF {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (File root: File.listRoots()) {
			System.out.println(root.toString());
			System.out.println(root.getFreeSpace()+" : "+root.getUsableSpace()+" : "+root.getTotalSpace());
		}
		File f = new File("c:\\Windows");
		System.out.println(f.getFreeSpace());
	}

}
