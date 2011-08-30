
public class TestCores extends Thread {

	public TestCores() {
		setPriority(Thread.MIN_PRIORITY);
	}
	
	public static void main(String[] args) {
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("Number of cores detected: "+ cores);
		for (int i= 0; i < cores ; i++) {
			new TestCores().start();
		}
	}
	
	public void run() {
//		int x= 0;
//		while (true) {
//			x++;
//		}
	}
}
