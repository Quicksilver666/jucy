
public class Testternary {
	
	
	public static void main(String[] args) {
	    int a = Integer.MAX_VALUE;
	    long b = 3L + a; 
	    System.out.println(a);
	    System.out.println(b);
	    System.out.println(chooseLucky(a, b, false));
	}
	
	
	public static long chooseLucky(int a,long b,boolean lucky) {
	    return lucky ? a : b; //say good bye to large numbers...
	}

}
