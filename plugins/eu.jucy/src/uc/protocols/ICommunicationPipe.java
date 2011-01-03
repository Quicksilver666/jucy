package uc.protocols;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface ICommunicationPipe extends Closeable {


	
	
	
	public static interface ICommunicationPipeDown extends Closeable {
		
		void sendData(ByteBuffer data);
	
	}
	
	public static interface ICommunicationPipeUp extends Closeable {
		
		void beforeConnect() throws IOException;
		
		/**
		 * the communication pipe is told that connection  has closed
		 */
		void connectionEnded() throws IOException;
		
		void receivedData(ByteBuffer data);
		
		/**
		 * the communication pipe is told that the connection has started..
		 */
		void connectionStarted() throws IOException;
	}
	

	
}
