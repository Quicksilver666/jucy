// $Id: Speed.java 54 2007-01-24 16:22:09Z tp $

package fr.cryptohash.test;

import fr.cryptohash.Digest;
import fr.cryptohash.MD2;
import fr.cryptohash.MD4;
import fr.cryptohash.MD5;
import fr.cryptohash.SHA0;
import fr.cryptohash.SHA1;
import fr.cryptohash.SHA224;
import fr.cryptohash.SHA256;
import fr.cryptohash.SHA384;
import fr.cryptohash.SHA512;
import fr.cryptohash.RIPEMD;
import fr.cryptohash.RIPEMD128;
import fr.cryptohash.RIPEMD160;
import fr.cryptohash.Tiger;
import fr.cryptohash.Tiger2;
import fr.cryptohash.PANAMA;
import fr.cryptohash.HAVAL256_3;
import fr.cryptohash.HAVAL256_4;
import fr.cryptohash.HAVAL256_5;
import fr.cryptohash.WHIRLPOOL;

/**
 * <p>This class implements some speed tests for hash functions.</p>
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

public class Speed {

	public static void main(String[] args)
	{
		int todo = 0;
		for (int i = 0; i < args.length; i ++) {
			String s = normalize(args[i]);
			if (s.equals("md2"))
				todo |= DO_MD2;
			else if (s.equals("md4"))
				todo |= DO_MD4;
			else if (s.equals("md5"))
				todo |= DO_MD5;
			else if (s.equals("sha0"))
				todo |= DO_SHA0;
			else if (s.equals("sha1"))
				todo |= DO_SHA1;
			else if (s.equals("sha2"))
				todo |= DO_SHA224 | DO_SHA256;
			else if (s.equals("sha224"))
				todo |= DO_SHA224;
			else if (s.equals("sha256"))
				todo |= DO_SHA256;
			else if (s.equals("sha3"))
				todo |= DO_SHA384 | DO_SHA512;
			else if (s.equals("sha384"))
				todo |= DO_SHA384;
			else if (s.equals("sha512"))
				todo |= DO_SHA512;
			else if (s.equals("rmd") || s.equals("ripemd"))
				todo |= DO_RIPEMD;
			else if (s.equals("rmd128") || s.equals("ripemd128"))
				todo |= DO_RIPEMD128;
			else if (s.equals("rmd160") || s.equals("ripemd160"))
				todo |= DO_RIPEMD160;
			else if (s.equals("tiger"))
				todo |= DO_TIGER;
			else if (s.equals("tiger2"))
				todo |= DO_TIGER2;
			else if (s.equals("panama"))
				todo |= DO_PANAMA;
			else if (s.equals("haval3"))
				todo |= DO_HAVAL3;
			else if (s.equals("haval4"))
				todo |= DO_HAVAL4;
			else if (s.equals("haval5"))
				todo |= DO_HAVAL5;
			else if (s.equals("whirlpool"))
				todo |= DO_WHIRLPOOL;
			else
				usage(args[i]);
		}
		if (todo == 0)
			todo = -1;
		if ((todo & DO_MD2) != 0)
			speed("MD2", new MD2());
		if ((todo & DO_MD4) != 0)
			speed("MD4", new MD4());
		if ((todo & DO_MD5) != 0)
			speed("MD5", new MD5());
		if ((todo & DO_SHA0) != 0)
			speed("SHA-0", new SHA0());
		if ((todo & DO_SHA1) != 0)
			speed("SHA-1", new SHA1());
		if ((todo & DO_SHA224) != 0)
			speed("SHA-224", new SHA224());
		if ((todo & DO_SHA256) != 0)
			speed("SHA-256", new SHA256());
		if ((todo & DO_SHA384) != 0)
			speed("SHA-384", new SHA384());
		if ((todo & DO_SHA512) != 0)
			speed("SHA-512", new SHA512());
		if ((todo & DO_RIPEMD) != 0)
			speed("RIPEMD", new RIPEMD());
		if ((todo & DO_RIPEMD128) != 0)
			speed("RIPEMD-128", new RIPEMD128());
		if ((todo & DO_RIPEMD160) != 0)
			speed("RIPEMD-160", new RIPEMD160());
		if ((todo & DO_TIGER) != 0)
			speed("Tiger", new Tiger());
		if ((todo & DO_TIGER2) != 0)
			speed("Tiger2", new Tiger2());
		if ((todo & DO_PANAMA) != 0)
			speed("PANAMA", new PANAMA());
		if ((todo & DO_HAVAL3) != 0)
			speed("HAVAL[3 passes]", new HAVAL256_3());
		if ((todo & DO_HAVAL4) != 0)
			speed("HAVAL[4 passes]", new HAVAL256_4());
		if ((todo & DO_HAVAL5) != 0)
			speed("HAVAL[5 passes]", new HAVAL256_5());
		if ((todo & DO_WHIRLPOOL) != 0)
			speed("WHIRLPOOL", new WHIRLPOOL());
	}

	private static final int DO_MD2        = 0x00000001;
	private static final int DO_MD4        = 0x00000002;
	private static final int DO_MD5        = 0x00000004;
	private static final int DO_SHA0       = 0x00000008;
	private static final int DO_SHA1       = 0x00000010;
	private static final int DO_SHA224     = 0x00000020;
	private static final int DO_SHA256     = 0x00000040;
	private static final int DO_SHA384     = 0x00000080;
	private static final int DO_SHA512     = 0x00000100;
	private static final int DO_RIPEMD     = 0x00000200;
	private static final int DO_RIPEMD128  = 0x00000400;
	private static final int DO_RIPEMD160  = 0x00000800;
	private static final int DO_TIGER      = 0x00001000;
	private static final int DO_TIGER2     = 0x00002000;
	private static final int DO_PANAMA     = 0x00004000;
	private static final int DO_HAVAL3     = 0x00008000;
	private static final int DO_HAVAL4     = 0x00010000;
	private static final int DO_HAVAL5     = 0x00020000;
	private static final int DO_WHIRLPOOL  = 0x00040000;

	private static String normalize(String name)
	{
		name = name.toLowerCase();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < name.length(); i ++) {
			char c = name.charAt(i);
			if (c != '-' && c != '/')
				sb.append(c);
		}
		return sb.toString();
	}

	private static void usage(String name)
	{
		System.err.println("unknown hash function name: '"
			+ name + "'");
		System.exit(1);
	}

	private static void speed(String name, Digest dig)
	{
		System.out.println("Speed test: " + name);
		byte[] buf = new byte[8192];
		for (int i = 0; i < buf.length; i ++)
			buf[i] = 'a';
		long num = 2L;
		for (int clen = 16;; clen <<= 2) {
			if (clen == 4096) {
				clen = 8192;
				if (num > 1L)
					num >>= 1;
			}
			long tt;
			for (;;) {
				tt = speedUnit(dig, buf, clen, num);
				if (tt > 6000L) {
					if (num <= 1L)
						break;
					num >>= 1L;
				} else if (tt < 2000L) {
					num += num;
				} else {
					break;
				}
			}
			long tlen = (long)clen * num;
			long div = 10L * tt;
			long rate = (tlen + (div - 1) / 2) / div;
			System.out.println("message length = "
				+ formatLong((long)clen, 5)
				+ " -> "
				+ prependSpaces(Long.toString(rate / 100L), 4)
				+ "."
				+ prependZeroes(Long.toString(rate % 100L), 2)
				+ " MBytes/s");
			if (clen == 8192) {
				tt = speedLong(dig, buf, clen, num);
				tlen = (long)clen * num;
				div = 10L * tt;
				rate = (tlen + (div - 1) / 2) / div;
				System.out.println("long messages          -> "
					+ prependSpaces(
						Long.toString(rate / 100L), 4)
					+ "."
					+ prependZeroes(
						Long.toString(rate % 100L), 2)
					+ " MBytes/s");
				break;
			}
			if (num > 4L)
				num >>= 2;
		}
	}

	private static long speedUnit(Digest dig, byte[] buf, int len, long num)
	{
		byte[] out = new byte[dig.getDigestLength()];
		long orig = System.currentTimeMillis();
		while (num -- > 0) {
			dig.update(buf, 0, len);
			dig.digest(out, 0, out.length);
		}
		long end = System.currentTimeMillis();
		return end - orig;
	}

	private static long speedLong(Digest dig, byte[] buf, int len, long num)
	{
		byte[] out = new byte[dig.getDigestLength()];
		long orig = System.currentTimeMillis();
		while (num -- > 0) {
			dig.update(buf, 0, len);
		}
		long end = System.currentTimeMillis();
		dig.digest(out, 0, out.length);
		return end - orig;
	}

	private static String formatLong(long num, int len)
	{
		return prependSpaces(Long.toString(num), len);
	}

	private static String prependSpaces(String s, int len)
	{
		return prependChar(s, ' ', len);
	}

	private static String prependZeroes(String s, int len)
	{
		return prependChar(s, '0', len);
	}

	private static String prependChar(String s, char c, int len)
	{
		int slen = s.length();
		if (slen >= len)
			return s;
		StringBuffer sb = new StringBuffer();
		while (len -- > slen)
			sb.append(c);
		sb.append(s);
		return sb.toString();
	}
}
