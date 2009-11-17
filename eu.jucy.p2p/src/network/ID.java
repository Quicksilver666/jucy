package network;





import uc.crypto.HashValue;

public class ID extends HashValue  {

	public static final ID FirstID = new ID (); 
	
/*	public static final ID EndID;
	
	static {
		byte[] val = new byte[HashValue.digestlength];
		Arrays.fill(val, (byte)0xff);
		EndID = new ID(val);
	} */
	
	public ID(HashValue hash) {
		this(hash.getRaw());
	}
	
	public ID(byte[] value) {
		super(value);
	}
	
	public ID() {
		super(new byte[HashValue.digestlength]);
	}
	
	
	public ID getMiddleID(int level) {
		if (level < 0) {
			throw new IllegalArgumentException();
		}
		
		byte[] id = copy().getRaw();
		
		
		int bytePos =  level / 8;
		int subBytePos = level % 8 ;
		int byteValue = (int)Math.pow(2, 7-subBytePos); 
			
		if ((id[bytePos] & byteValue) == byteValue ) {
			throw new IllegalArgumentException("no startvalue for given level");
		}
		id[bytePos] |= byteValue;
			
		return new ID(id);
	}
	
	public ID getEndID(int level) {
		byte[] id = copy().getRaw();
		int bytePos = level / 8 ;
		
		for (int subBytePos = level % 8; subBytePos < 8 ; subBytePos++) {
			int byteValue = (int)Math.pow(2, 7-subBytePos);
			id[bytePos] |= byteValue; 
		}
		
		bytePos++;
		
		for (; bytePos < id.length ; bytePos++ ) {
			id[bytePos] = (byte)0xff;
		}
		
		return new ID(id);
	}
	
	public ID getDistance(ID other) {
		ID id = new ID();
		byte[] distance = id.getRaw();
		
		for (int i = 0 ; i < distance.length ; i++ ) {
			distance[i] =  distance(other.getRaw()[i],getRaw()[i]);   
		}
		
		return id;
	}
	
	
	private static byte distance(byte a, byte b) {
		return (byte)Math.abs( (a & 0xff) - (b & 0xff) );
	}



}
