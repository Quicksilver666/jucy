// $Id: Digest.java 54 2007-01-24 16:22:09Z tp $

package fr.cryptohash;

/**
 * <p>This interface documents the API for a hash function. This interface
 * somewhat mimics the standard <code>java.security.MessageDigest</code>
 * class. We do not extend that class in order to provide compatibility
 * with reduced Java implementations such as J2ME. Implementing a
 * <code>Provider</code> compatible with Sun's JCA ought to be easy.</p>
 *
 * <p>A <code>Digest</code> object maintains a running state for a hash
 * function computation. Data is inserted with <code>update()</code>
 * calls; the result is obtained from a <code>digest()</code> method
 * (where some final data can be inserted as well). When a digest output
 * has been produced, the objet is automatically resetted, and can be
 * used immediately for another digest operation. The state of a
 * computation can be cloned with the <code>copy()</code> method; this
 * can be used to get a partial hash result without interrupting the
 * complete computation.</p>
 *
 * <p><code>Digest</code> objects are statefull and hence not thread-safe;
 * however, distinct <code>Digest</code> objects can be accessed
 * concurrently without any problem.</p>
 *
 * <pre>
 * ==========================(LICENSE BEGIN)============================
 *
 * Copyright (c) 2007  Projet RNRT SAPHIR
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * ===========================(LICENSE END)=============================
 * </pre>
 *
 * @version   $Revision: 54 $
 * @author    Thomas Pornin &lt;thomas.pornin@cryptolog.com&gt;
 */

public interface Digest {

	/**
	 * Insert one more input data byte.
	 *
	 * @param in   the input byte
	 */
	public void update(byte in);

	/**
	 * Insert some more bytes.
	 *
	 * @param inbuf   the data bytes
	 */
	public void update(byte[] inbuf);

	/**
	 * Insert some more bytes.
	 *
	 * @param inbuf   the data buffer
	 * @param off     the data offset in <code>inbuf</code>
	 * @param len     the data length (in bytes)
	 */
	public void update(byte[] inbuf, int off, int len);

	/**
	 * Finalize the current hash computation and return the hash value
	 * in a newly-allocated array. The object is resetted.
	 *
	 * @return  the hash output
	 */
	public byte[] digest();

	/**
	 * Input some bytes, then finalize the current hash computation
	 * and return the hash value in a newly-allocated array. The object
	 * is resetted.
	 *
	 * @param inbuf   the input data
	 * @return  the hash output
	 */
	public byte[] digest(byte[] inbuf);

	/**
	 * Finalize the current hash computation and store the hash value
	 * in the provided output buffer. The <code>len</code> parameter
	 * contains the maximum number of bytes that should be written;
	 * no more bytes than the natural hash function output length will
	 * be produced. If <code>len</code> is smaller than the natural
	 * hash output length, the hash output is truncated to its first
	 * <code>len</code> bytes. The object is resetted.
	 *
	 * @param outbuf   the output buffer
	 * @param off      the output offset within <code>outbuf</code>
	 * @param len      the requested hash output length (in bytes)
	 * @return  the number of bytes actually written in <code>outbuf</code>
	 */
	public int digest(byte[] outbuf, int off, int len);

	/**
	 * Get the natural hash function output length (in bytes).
	 *
	 * @return  the digest output length (in bytes)
	 */
	public int getDigestLength();

	/**
	 * Reset the object: this makes it suitable for a new hash
	 * computation. The current computation, if any, is discarded.
	 */
	public void reset();

	/**
	 * Clone the current state. The returned object evolves independantly
	 * of this object.
	 *
	 * @return  the clone
	 */
	public Digest copy();

	/**
	 * Return the "block length" for the hash function. This value is
	 * defined for iterated hash function (Merkle-Damgard). It is used
	 * in HMAC.
	 *
	 * @return  the internal block length (in bytes)
	 */
	public int getBlockLength();
}
