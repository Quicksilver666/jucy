package network;

import java.io.IOException;
import java.nio.ByteBuffer;

import uc.crypto.HashValue;
import uc.crypto.Tiger;



/**
 * This class represents the P2PMessage needed
 * Header Definition
 * StartByte - length
 * 0 - 1 Byte		0x1a  MagicValue to help identify the P2P protocol
 * 1 - 24 Bytes 	TargetID the ID of a target message
 * 25 - 24 Bytes 	TargetID the ID of a target message
 * 49 - 1 Byte		Checksum of header and data (first byte of the TigerHash of all with Checksum field set to zero)
 * 50 - 1 Byte		Flags   Bit - 0     datacentric (the target is not the given ID but the peer responsible for the ID)
 * 51 - 1 Byte      NextHeader
 * 52 - x Bytes		Data
 * 
 * @author Quicksilver 
 *
 */
public class P2PMessage {
	
	private static final int HeaderLength = 52;
	private static final int ChecksumBytePos = 49;
	public static final byte MAGIC_HEADER = (byte)0x1a;

	public static final int FLAG_DATACENTRIC = 1 ; //if this flag is enabled the target is not the given ID but a peer responsible for the given ID
	public static final int FLAG_PUSHPULL = 4; //for push pull operations..
	
	
	private final ID target;
	private final ID source;
	private final byte[] bytes;
	private final byte nextHeader;
	private final byte flags;
	
	
	
	/**
	 * constructor from received data packet
	 * @param buf - the whole UDP packet received
	 *
	*/
	public static P2PMessage receiveP2PMessage(ByteBuffer buf ) throws IOException {
		byte checksum = buf.get(ChecksumBytePos);
		buf.put(ChecksumBytePos, (byte)0);
		HashValue hash = Tiger.tigerOfBytes(buf.array());
		if (hash.getRaw()[0] != checksum) {
			throw new IOException("Bad Checksum");
		}
				
		if (buf.get() != MAGIC_HEADER) {
			throw new IOException("Bad Header Magic");
		}
		
		byte[] target = new byte[HashValue.digestlength];
		buf.get(target);
		
		byte[] source = new byte[HashValue.digestlength];
		buf.get(source);
		
		buf.get(); //remove checksum..
		byte flag = buf.get();
		
		byte nextHeader = buf.get();
		
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		
		return new P2PMessage(data,new ID(target),new ID(source),flag,nextHeader);
	}
	
	/**
	 * constructor from data and target id
	 * @param data
	 * @param target
	 */
	public P2PMessage(byte[] data,ID target,ID source,int flags,byte nextHeader) {
		this.target = target;
		this.source = source;
		this.bytes = data;
		this.flags = (byte)flags;
		this.nextHeader = nextHeader;
	}
	
	/**
	 * creates a  Packet from this P2P message
	 * 
	 * @return BteBuffer..
	 */
	public ByteBuffer getPacket() {
		ByteBuffer b = ByteBuffer.allocate(HeaderLength+ bytes.length);
		b.put(MAGIC_HEADER);
		b.put(target.getRaw());
		b.put(source.getRaw());
		b.put((byte)0); //puts a zero where the checksum will be
		b.put(flags);
		b.put(nextHeader);
		b.put(bytes);
		
		HashValue hash = Tiger.tigerOfBytes(b.array());
		b.put(ChecksumBytePos, hash.getRaw()[0]);
		
		b.flip();
		
		return b;
	}
}
