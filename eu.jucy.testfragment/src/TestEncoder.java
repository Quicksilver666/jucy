import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class TestEncoder {

	
	
	/**
	 * @param args
	 * @throws CharacterCodingException 
	 */
	public static void main(String[] args) throws CharacterCodingException {
		List<byte[]> list= generateRandomBytes(1000000,150);
		Charset cs = Charset.forName("utf-8");
		CharsetDecoder cd = cs.newDecoder();
		CharBuffer cb = CharBuffer.allocate(150);
	
		long withDecoderStart = System.currentTimeMillis(); 
		for (byte[] b:list) {
			cb.clear();
			cd.decode(ByteBuffer.wrap(b),cb,true);
			cb.toString();
		}
		long withDecodertime = System.currentTimeMillis()-withDecoderStart; 
		
		long noDecoderStart = System.currentTimeMillis(); 
		for (byte[] b:list) {
			cs.decode(ByteBuffer.wrap(b)).toString();
		}
		long noDecoderTime = System.currentTimeMillis()-noDecoderStart; 
		
		System.out.println("with: "+withDecodertime+"  no:"+noDecoderTime);
		
	}

	
	
	
	private static List<byte[]> generateRandomBytes(int ammount,int length) {
		List<byte[]> list = new ArrayList<byte[]>();
		Random rand = new Random();
		for (int i= 0; i < ammount; i++) {
			byte[] b = new byte[length];
			for (int x=0 ; x < b.length; x++) {
				b[x] = (byte)(rand.nextInt(107)+21);
			}
			list.add(b);
		}
		return list;
	}
	
}
