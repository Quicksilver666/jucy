package uc.protocols;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

public interface IConnection extends Closeable {

	/**
	 * used to start the connection after initialization
	 */
	void start();

	/**
	 * for sending a string that will be send using CharsetEncoders 
	 * 
	 * @param toSend - the ususla method for textbased protocols..
	 * 
	 * @throws IOException
	 */
	void send(String toSend) throws IOException;

	void send(ByteBuffer toSend) throws IOException;

	/**
	 * reset methods.. 
	 * usage: disconnect the connection if it is connected..
	 * use the provided socketchannel for new connection
	 * clear all Buffers..and alike stuff
	 * charset decoder/encoder will be resetted..
	 * pattern will be recompiled..
	 * 
	 * - then reconnect /connect
	 * 
	 * @param soChan
	 * @param connectionProt
	 */
	void reset(SocketChannel soChan);

	void reset(InetSocketAddress addy);

	void reset(String addy);

	/**
	 * close the connection..
	 *
	 */
	void close();

	/**
	 * 
	 * @return the address to witch this Connection is bound..
	 */
	InetSocketAddress getInetSocketAddress();

	/**
	 * tests whether the connection is local..
	 * 
	 * @return if the connection is local..
	 */
	boolean isLocal();

	/**
	 * retrieves a ByteChannel that can be used to directly writing to the connection..
	 * this ByteChannel is guaranteed to be in blocking mode..
	 *  
	 *  important..
	 *  when retrieve is Called.. the Connection will no longer 
	 *  read on its own from the channel and do protocol calls..
	 *  
	 * @return a ByteChannel for writing/reading to the Connection
	 */
	ByteChannel retrieveChannel() throws IOException;

	/**
	 * when a ByteChannel was retrieved this method can be used to bring it back 
	 * to normal state again..
	 * 
	 * @return if returning was successful
	 * @throws IOException
	 */
	boolean returnChannel(ByteChannel sochan) throws IOException;

	/**
	 * should try to flush the stream for up to provided time
	 * @param miliseconds - how long it should try to flush
	 * @returns true if after flushing there is no data more to be sent
	 *  false if still some data is waiting to be sent..
	 */
	boolean flush(int miliseconds);

	/**
	 * turn on and off decompression of incoming data
	 * @param comp - set a compression 
	 * Compression.NONE to stop decompressing
	 */
	void setIncomingDecompression(Compression comp) throws IOException;

	/**
	 * when the protocol changes this is called by the protocol to refresh protocol specific 
	 * stuff like charsets 
	 * 
	 */
	void refreshCharsetCoders();

	boolean usesEncryption();

}