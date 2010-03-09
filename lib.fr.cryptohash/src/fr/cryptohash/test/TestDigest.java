// $Id: TestDigest.java 81 2007-02-25 12:45:17Z tp $

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
import fr.cryptohash.HAVAL128_3;
import fr.cryptohash.HAVAL128_4;
import fr.cryptohash.HAVAL128_5;
import fr.cryptohash.HAVAL160_3;
import fr.cryptohash.HAVAL160_4;
import fr.cryptohash.HAVAL160_5;
import fr.cryptohash.HAVAL192_3;
import fr.cryptohash.HAVAL192_4;
import fr.cryptohash.HAVAL192_5;
import fr.cryptohash.HAVAL224_3;
import fr.cryptohash.HAVAL224_4;
import fr.cryptohash.HAVAL224_5;
import fr.cryptohash.HAVAL256_3;
import fr.cryptohash.HAVAL256_4;
import fr.cryptohash.HAVAL256_5;
import fr.cryptohash.WHIRLPOOL;
import fr.cryptohash.WHIRLPOOL0;
import fr.cryptohash.WHIRLPOOL1;
import fr.cryptohash.HMAC;

/**
 * This class is a program entry point; it includes tests for the
 * implementation of the hash functions.
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
 * @version   $Revision: 81 $
 * @author    Thomas Pornin &lt;thomas.pornin@cryptolog.com&gt;
 */

public class TestDigest {

	/**
	 * Program entry. Parameters are ignored.
	 *
	 * @param args   the parameter input (ignored)
	 */
	public static void main(String[] args)
	{
		testMD2();
		testMD4();
		testMD5();
		testSHA0();
		testSHA1();
		testSHA224();
		testSHA256();
		testSHA384();
		testSHA512();
		testRIPEMD();
		testRIPEMD128();
		testRIPEMD160();
		testTiger();
		testTiger2();
		testPANAMA();
		testHAVAL();
		testWHIRLPOOL();
		testHMAC();
	}

	private static final void fail(String message)
	{
		throw new RuntimeException("test failed: " + message);
	}

	private static final byte[] strtobin(String str)
	{
		int blen = str.length() / 2;
		byte[] buf = new byte[blen];
		for (int i = 0; i < blen; i ++) {
			String bs = str.substring(i * 2, i * 2 + 2);
			buf[i] = (byte)Integer.parseInt(bs, 16);
		}
		return buf;
	}

	private static final byte[] encodeLatin1(String str)
	{
		int blen = str.length();
		byte[] buf = new byte[blen];
		for (int i = 0; i < blen; i ++)
			buf[i] = (byte)str.charAt(i);
		return buf;
	}

	private static final boolean equals(byte[] b1, byte[] b2)
	{
		if (b1 == b2)
			return true;
		if (b1 == null || b2 == null)
			return false;
		if (b1.length != b2.length)
			return false;
		for (int i = 0; i < b1.length; i ++)
			if (b1[i] != b2[i])
				return false;
		return true;
	}

	private static final void assertTrue(boolean expr)
	{
		if (!expr)
			fail("assertion failed");
	}

	private static final void assertEquals(byte[] b1, byte[] b2)
	{
		if (!equals(b1, b2))
			fail("byte streams are not equal");
	}

	private static final void assertNotEquals(byte[] b1, byte[] b2)
	{
		if (equals(b1, b2))
			fail("byte streams are equal");
	}

	private static final void reportSuccess(String name)
	{
		System.out.println("===== test " + name + " passed");
	}

	private static void testKat(Digest dig, String data, String ref)
	{
		/*
		 * First test the hashing itself.
		 */
		byte[] buf = encodeLatin1(data);
		byte[] out = dig.digest(buf);
		byte[] exp = strtobin(ref);
		assertEquals(out, exp);

		/*
		 * Now the update() API; this also exercise auto-reset.
		 */
		for (int i = 0; i < buf.length; i ++)
			dig.update(buf[i]);
		assertEquals(dig.digest(), exp);

		/*
		 * The cloning API.
		 */
		int blen = buf.length;
		dig.update(buf, 0, blen / 2);
		Digest dig2 = dig.copy();
		dig.update(buf, blen / 2, blen - (blen / 2));
		assertEquals(dig.digest(), exp);
		dig2.update(buf, blen / 2, blen - (blen / 2));
		assertEquals(dig2.digest(), exp);
	}

	private static void testKatMillionA(Digest dig, String ref)
	{
		byte[] buf = new byte[1000];
		for (int i = 0; i < 1000; i ++)
			buf[i] = 'a';
		for (int i = 0; i < 1000; i ++)
			dig.update(buf);
		assertEquals(dig.digest(), strtobin(ref));
	}

	private static void testCollision(Digest dig, String s1, String s2)
	{
		byte[] msg1 = strtobin(s1);
		byte[] msg2 = strtobin(s2);
		assertNotEquals(msg1, msg2);
		assertEquals(dig.digest(msg1), dig.digest(msg2));
	}

	/**
	 * Test MD2 implementation.
	 */
	private static void testMD2()
	{
		Digest dig = new MD2();
		testKat(dig, "", "8350e5a3e24c153df2275c9f80692773");
		testKat(dig, "a", "32ec01ec4a6dac72c0ab96fb34c0b5d1");
		testKat(dig, "abc", "da853b0d3f88d99b30283a69e6ded6bb");
		testKat(dig, "message digest",
			"ab4f496bfb2a530b219ff33031fe06b0");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"4e8ddff3650292ab5a4108c3aa47940b");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstu"
			+ "vwxyz0123456789",
			"da33def2a42df13975352846c30338cd");
		testKat(dig, "1234567890123456789012345678901234567890123456789"
			+ "0123456789012345678901234567890",
			"d5976f79d83d3a0dc9806c3c66f3efd8");

		testKatMillionA(dig, "8c0a09ff1216ecaf95c8130953c62efd");

		reportSuccess("MD2");
	}

	/**
	 * Test MD4 implementation.
	 */
	private static void testMD4()
	{
		Digest dig = new MD4();
		testKat(dig, "", "31d6cfe0d16ae931b73c59d7e0c089c0");
		testKat(dig, "a", "bde52cb31de33e46245e05fbdbd6fb24");
		testKat(dig, "abc", "a448017aaf21d8525fc10ae87aa6729d");
		testKat(dig, "message digest",
			"d9130a8164549fe818874806e1c7014b");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"d79e1c308aa5bbcdeea8ed63df412da9");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstu"
			+ "vwxyz0123456789",
			"043f8582f241db351ce627e153e7f0e4");
		testKat(dig, "1234567890123456789012345678901234567890123456789"
			+ "0123456789012345678901234567890",
			"e33b4ddc9c38f2199c3e7b164fcc0536");

		testKatMillionA(dig, "bbce80cc6bb65e5c6745e30d4eeca9a4");

		testCollision(dig,
			"839c7a4d7a92cb5678a5d5b9eea5a7573c8a74deb366c3dc20"
			+ "a083b69f5d2a3bb3719dc69891e9f95e809fd7e8b23ba631"
			+ "8edd45e51fe39708bf9427e9c3e8b9",
			"839c7a4d7a92cbd678a5d529eea5a7573c8a74deb366c3dc20"
			+ "a083b69f5d2a3bb3719dc69891e9f95e809fd7e8b23ba631"
			+ "8edc45e51fe39708bf9427e9c3e8b9");

		testCollision(dig,
			"839c7a4d7a92cb5678a5d5b9eea5a7573c8a74deb366c3dc20"
			+ "a083b69f5d2a3bb3719dc69891e9f95e809fd7e8b23ba631"
			+ "8edd45e51fe39740c213f769cfb8a7",
			"839c7a4d7a92cbd678a5d529eea5a7573c8a74deb366c3dc20"
			+ "a083b69f5d2a3bb3719dc69891e9f95e809fd7e8b23ba631"
			+ "8edc45e51fe39740c213f769cfb8a7");

		reportSuccess("MD4");
	}

	/**
	 * Test MD5 implementation.
	 */
	private static void testMD5()
	{
		Digest dig = new MD5();
		testKat(dig, "", "d41d8cd98f00b204e9800998ecf8427e");
		testKat(dig, "a", "0cc175b9c0f1b6a831c399e269772661");
		testKat(dig, "abc", "900150983cd24fb0d6963f7d28e17f72");
		testKat(dig, "message digest",
			"f96b697d7cb7938d525a2f31aaf161d0");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"c3fcd3d76192e4007dfb496cca67e13b");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstu"
			+ "vwxyz0123456789",
			"d174ab98d277d9f5a5611c2c9f419d9f");
		testKat(dig, "1234567890123456789012345678901234567890123456789"
			+ "0123456789012345678901234567890",
			"57edf4a22be3c955ac49da2e2107b67a");

		testKatMillionA(dig, "7707d6ae4e027c70eea2a935c2296f21");

		testCollision(dig,
			"d131dd02c5e6eec4693d9a0698aff95c2fcab58712467eab40"
			+ "04583eb8fb7f8955ad340609f4b30283e488832571415a08"
			+ "5125e8f7cdc99fd91dbdf280373c5b960b1dd1dc417b9ce4"
			+ "d897f45a6555d535739ac7f0ebfd0c3029f166d109b18f75"
			+ "277f7930d55ceb22e8adba79cc155ced74cbdd5fc5d36db1"
			+ "9b0ad835cca7e3",
			"d131dd02c5e6eec4693d9a0698aff95c2fcab50712467eab40"
			+ "04583eb8fb7f8955ad340609f4b30283e4888325f1415a08"
			+ "5125e8f7cdc99fd91dbd7280373c5b960b1dd1dc417b9ce4"
			+ "d897f45a6555d535739a47f0ebfd0c3029f166d109b18f75"
			+ "277f7930d55ceb22e8adba794c155ced74cbdd5fc5d36db1"
			+ "9b0a5835cca7e3");

		testCollision(dig,
			"d131dd02c5e6eec4693d9a0698aff95c2fcab58712467eab40"
			+ "04583eb8fb7f8955ad340609f4b30283e488832571415a08"
			+ "5125e8f7cdc99fd91dbdf280373c5bd8823e3156348f5bae"
			+ "6dacd436c919c6dd53e2b487da03fd02396306d248cda0e9"
			+ "9f33420f577ee8ce54b67080a80d1ec69821bcb6a8839396"
			+ "f9652b6ff72a70",
			"d131dd02c5e6eec4693d9a0698aff95c2fcab50712467eab40"
			+ "04583eb8fb7f8955ad340609f4b30283e4888325f1415a08"
			+ "5125e8f7cdc99fd91dbd7280373c5bd8823e3156348f5bae"
			+ "6dacd436c919c6dd53e23487da03fd02396306d248cda0e9"
			+ "9f33420f577ee8ce54b67080280d1ec69821bcb6a8839396"
			+ "f965ab6ff72a70");

		reportSuccess("MD5");
	}

	/**
	 * Test SHA-0 implementation.
	 */
	private static void testSHA0()
	{
		Digest dig = new SHA0();
		testKat(dig, "abc", "0164b8a914cd2a5e74c4f7ff082c4d97f1edf880");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlm"
			+ "nomnopnopq",
			"d2516ee1acfa5baf33dfc1c471e438449ef134c8");

		testKatMillionA(dig,
			"3232affa48628a26653b5aaa44541fd90d690603");

		testCollision(dig,
			"a766a602b65cffe773bcf25826b322b3d01b1a972684ef533e"
			+ "3b4b7f53fe376224c08e47e959b2bc3b519880b928656824"
			+ "7d110f70f5c5e2b4590ca3f55f52feeffd4c8fe68de83532"
			+ "9e603cc51e7f02545410d1671d108df5a4000dcf20a43949"
			+ "49d72cd14fbb0345cf3a295dcda89f998f87552c9a58b1bd"
			+ "c384835e477185f96e68bebb0025d2d2b69edf21724198f6"
			+ "88b41deb9b4913fbe696b5457ab39921e1d7591f89de8457"
			+ "e8613c6c9e3b242879d4d8783b2d9ca9935ea526a729c06e"
			+ "dfc50137e69330be976012cc5dfe1c14c4c68bd1db3ecb24"
			+ "438a59a09b5db435563e0d8bdf572f77b53065cef31f32dc"
			+ "9dbaa04146261e9994bd5cd0758e3d",
			"a766a602b65cffe773bcf25826b322b1d01b1ad72684ef51be"
			+ "3b4b7fd3fe3762a4c08e45e959b2fc3b51988039286528a4"
			+ "7d110d70f5c5e034590ce3755f52fc6ffd4c8d668de87532"
			+ "9e603e451e7f02d45410d1e71d108df5a4000dcf20a43949"
			+ "49d72cd14fbb0145cf3a695dcda89d198f8755ac9a58b13d"
			+ "c384815e4771c5796e68febb0025d052b69edda17241d876"
			+ "88b41f6b9b49117be696f5c57ab399a1e1d7199f89de8657"
			+ "e8613cec9e3b26a879d498783b2d9e29935ea7a6a729806e"
			+ "dfc50337e693303e9760104c5dfe5c14c4c68951db3ecba4"
			+ "438a59209b5db435563e0d8bdf572f77b53065cef31f30dc"
			+ "9dbae04146261c1994bd5c50758e3d");

		reportSuccess("SHA-0");
	}

	/**
	 * Test SHA-1 implementation.
	 */
	private static void testSHA1()
	{
		Digest dig = new SHA1();
		testKat(dig, "abc", "a9993e364706816aba3e25717850c26c9cd0d89d");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlm"
			+ "nomnopnopq",
			"84983e441c3bd26ebaae4aa1f95129e5e54670f1");

		testKatMillionA(dig,
			"34aa973cd4c4daa4f61eeb2bdbad27316534016f");

		reportSuccess("SHA-1");
	}

	/**
	 * Test SHA-224 implementation.
	 */
	private static void testSHA224()
	{
		Digest dig = new SHA224();
		testKat(dig, "abc",
    "23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlm"
			+ "nomnopnopq",
    "75388b16512776cc5dba5da1fd890150b0c6455cb4f58b1952522525");

		testKatMillionA(dig,
    "20794655980c91d8bbb4c1ea97618a4bf03f42581948b2ee4ee7ad67");

		reportSuccess("SHA-224");
	}

	/**
	 * Test SHA-256 implementation.
	 */
	private static void testSHA256()
	{
		Digest dig = new SHA256();
		testKat(dig, "abc",
    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlm"
			+ "nomnopnopq",
    "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1");

		testKatMillionA(dig,
    "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0");

		reportSuccess("SHA-256");
	}

	/**
	 * Test SHA-384 implementation.
	 */
	private static void testSHA384()
	{
		Digest dig = new SHA384();
		testKat(dig, "abc",
			"cb00753f45a35e8bb5a03d699ac65007272c32ab0eded163"
			+ "1a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7");
		testKat(dig, "abcdefghbcdefghicdefghijdefghijkefghijklfghij"
			+ "klmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnop"
			+ "qrsmnopqrstnopqrstu",
			"09330c33f71147e83d192fc782cd1b4753111b173b3b05d2"
			+ "2fa08086e3b0f712fcc7c71a557e2db966c3e9fa91746039");

		testKatMillionA(dig,
			"9d0e1809716474cb086e834e310a4a1ced149e9c00f24852"
			+ "7972cec5704c2a5b07b8b3dc38ecc4ebae97ddd87f3d8985");

		reportSuccess("SHA-384");
	}

	/**
	 * Test SHA-512 implementation.
	 */
	private static void testSHA512()
	{
		Digest dig = new SHA512();
		testKat(dig, "abc",
    "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
    + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f");
		testKat(dig, "abcdefghbcdefghicdefghijdefghijkefghijklfghij"
			+ "klmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnop"
			+ "qrsmnopqrstnopqrstu",
    "8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018"
    + "501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909");

		testKatMillionA(dig,
    "e718483d0ce769644e2e42c7bc15b4638e1f98b13b2044285632a803afa973eb"
    + "de0ff244877ea60a4cb0432ce577c31beb009c5c2c49aa2e4eadb217ad8cc09b");

		reportSuccess("SHA-512");
	}

	/**
	 * Test RIPEMD implementation.
	 */
	private static void testRIPEMD()
	{
		Digest dig = new RIPEMD();
		testKat(dig, "",
			"9f73aa9b372a9dacfb86a6108852e2d9");
		testKat(dig, "a",
			"486f74f790bc95ef7963cd2382b4bbc9");
		testKat(dig, "abc",
			"3f14bad4c2f9b0ea805e5485d3d6882d");
		testKat(dig, "message digest",
			"5f5c7ebe1abbb3c7036482942d5f9d49");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"ff6e1547494251a1cca6f005a6eaa2b4");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqr"
			+ "stuvwxyz0123456789",
			"ff418a5aed3763d8f2ddf88a29e62486");
		testKat(dig, "12345678901234567890123456789012345678901234"
			+ "567890123456789012345678901234567890",
			"dfd6b45f60fe79bbbde87c6bfc6580a5");

		testCollision(dig,
			"8eaf9f5779f5ec09ba6a4a5711354178a410b4a29f6c2fad2c"
			+ "20560b1179754de7aade0bf291bc787d6dbc47b1d1bd9a15"
			+ "205da4ff047181a8584726a54e0661",
			"8eaf9f5779f5ec09ba6a4a5711355178a410b4a29f6c2fad2c"
			+ "20560b1179754de7aade0bf291bc787d6dc0c7b1d1bd9a15"
			+ "205da4ff047181a8584726a54e06e1");

		testCollision(dig,
			"8eaf9f5779f5ec09ba6a4a5711354178a410b4a29f6c2fad2c"
			+ "20560b1179754de7aade0bf291bc787d6dbc47b1d1bd9a15"
			+ "205da4ff04a5a0a8588db1b6660ce7",
			"8eaf9f5779f5ec09ba6a4a5711355178a410b4a29f6c2fad2c"
			+ "20560b1179754de7aade0bf291bc787d6dc0c7b1d1bd9a15"
			+ "205da4ff04a5a0a8588db1b6660c67");

		reportSuccess("RIPEMD");
	}

	/**
	 * Test RIPEMD-128 implementation.
	 */
	private static void testRIPEMD128()
	{
		Digest dig = new RIPEMD128();
		testKat(dig, "",
			"cdf26213a150dc3ecb610f18f6b38b46");
		testKat(dig, "a",
			"86be7afa339d0fc7cfc785e72f578d33");
		testKat(dig, "abc",
			"c14a12199c66e4ba84636b0f69144c77");
		testKat(dig, "message digest",
			"9e327b3d6e523062afc1132d7df9d1b8");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"fd2aa607f71dc8f510714922b371834e");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmn"
			+ "lmnomnopnopq",
			"a1aa0689d0fafa2ddc22e88b49133a06");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqr"
			+ "stuvwxyz0123456789",
			"d1e959eb179c911faea4624c60c5c702");
		testKat(dig, "12345678901234567890123456789012345678901234"
			+ "567890123456789012345678901234567890",
			"3f45ef194732c2dbb2c4a2c769795fa3");

		testKatMillionA(dig,
			"4a7f5723f954eba1216c9d8f6320431f");

		reportSuccess("RIPEMD-128");
	}

	/**
	 * Test RIPEMD-160 implementation.
	 */
	private static void testRIPEMD160()
	{
		Digest dig = new RIPEMD160();
		testKat(dig, "",
			"9c1185a5c5e9fc54612808977ee8f548b2258d31");
		testKat(dig, "a",
			"0bdc9d2d256b3ee9daae347be6f4dc835a467ffe");
		testKat(dig, "abc",
			"8eb208f7e05d987a9b044a8e98c6b087f15a0bfc");
		testKat(dig, "message digest",
			"5d0689ef49d2fae572b881b123a85ffa21595f36");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"f71c27109c692c1b56bbdceb5b9d2865b3708dbc");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmn"
			+ "lmnomnopnopq",
			"12a053384a9c0c88e405a06c27dcf49ada62eb2b");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqr"
			+ "stuvwxyz0123456789",
			"b0e20b6e3116640286ed3a87a5713079b21f5189");
		testKat(dig, "12345678901234567890123456789012345678901234"
			+ "567890123456789012345678901234567890",
			"9b752e45573d4b39f4dbd3323cab82bf63326bfb");

		testKatMillionA(dig,
			"52783243c1697bdbe16d37f97f68f08325dc1528");

		reportSuccess("RIPEMD-160");
	}

	/**
	 * Test Tiger implementation.
	 */
	private static void testTiger()
	{
		Digest dig = new Tiger();
		testKat(dig, "",
			"3293AC630C13F0245F92BBB1766E16167A4E58492DDE73F3");
		testKat(dig, "a",
			"77BEFBEF2E7EF8AB2EC8F93BF587A7FC613E247F5F247809");
		testKat(dig, "abc",
			"2AAB1484E8C158F2BFB8C5FF41B57A525129131C957B5F93");
		testKat(dig, "message digest",
			"D981F8CB78201A950DCF3048751E441C517FCA1AA55A29F6");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"1714A472EEE57D30040412BFCC55032A0B11602FF37BEEE9");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmn"
			+ "lmnomnopnopq",
			"0F7BF9A19B9C58F2B7610DF7E84F0AC3A71C631E7B53F78E");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"8DCEA680A17583EE502BA38A3C368651890FFBCCDC49A8CC");
		testKat(dig, "1234567890123456789012345678901234567890"
			+ "1234567890123456789012345678901234567890",
			"1C14795529FD9F207A958F84C52F11E887FA0CABDFD91BFD");

		testKatMillionA(dig,
			"6DB0E2729CBEAD93D715C6A7D36302E9B3CEE0D2BC314B41");

		reportSuccess("Tiger");
	}

	/**
	 * Test Tiger2 implementation.
	 */
	private static void testTiger2()
	{
		Digest dig = new Tiger2();
		testKat(dig, "",
			"4441BE75F6018773C206C22745374B924AA8313FEF919F41");
		testKat(dig, "a",
			"67E6AE8E9E968999F70A23E72AEAA9251CBC7C78A7916636");
		testKat(dig, "abc",
			"F68D7BC5AF4B43A06E048D7829560D4A9415658BB0B1F3BF");
		testKat(dig, "message digest",
			"E29419A1B5FA259DE8005E7DE75078EA81A542EF2552462D");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
			"F5B6B6A78C405C8547E91CD8624CB8BE83FC804A474488FD");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijkijkljklmklmn"
			+ "lmnomnopnopq",
			"A6737F3997E8FBB63D20D2DF88F86376B5FE2D5CE36646A9");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"EA9AB6228CEE7B51B77544FCA6066C8CBB5BBAE6319505CD");
		testKat(dig, "1234567890123456789012345678901234567890"
			+ "1234567890123456789012345678901234567890",
			"D85278115329EBAA0EEC85ECDC5396FDA8AA3A5820942FFF");

		testKatMillionA(dig,
			"E068281F060F551628CC5715B9D0226796914D45F7717CF4");

		reportSuccess("Tiger2");
	}

	/**
	 * Test PANAMA implementation.
	 */
	private static void testPANAMA()
	{
		Digest dig = new PANAMA();
		testKat(dig, "",
    "aa0cc954d757d7ac7779ca3342334ca471abd47d5952ac91ed837ecd5b16922b");
		testKat(dig, "T",
    "049d698307d8541f22870dfa0a551099d3d02bc6d57c610a06a4585ed8d35ff8");
		testKat(dig, "The quick brown fox jumps over the lazy dog",
    "5f5ca355b90ac622b0aa7e654ef5f27e9e75111415b48b8afe3add1c6b89cba1");

		testKatMillionA(dig,
    "af9c66fb6058e2232a5dfba063ee14b0f86f0e334e165812559435464dd9bb60");

		reportSuccess("PANAMA");
	}

	/**
	 * Test HAVAL implementation.
	 */
	private static void testHAVAL()
	{
		Digest dig128_3 = new HAVAL128_3();
		Digest dig128_4 = new HAVAL128_4();
		Digest dig128_5 = new HAVAL128_5();
		Digest dig160_3 = new HAVAL160_3();
		Digest dig160_4 = new HAVAL160_4();
		Digest dig160_5 = new HAVAL160_5();
		Digest dig192_3 = new HAVAL192_3();
		Digest dig192_4 = new HAVAL192_4();
		Digest dig192_5 = new HAVAL192_5();
		Digest dig224_3 = new HAVAL224_3();
		Digest dig224_4 = new HAVAL224_4();
		Digest dig224_5 = new HAVAL224_5();
		Digest dig256_3 = new HAVAL256_3();
		Digest dig256_4 = new HAVAL256_4();
		Digest dig256_5 = new HAVAL256_5();

		testKat(dig128_3, "",
			"C68F39913F901F3DDF44C707357A7D70");
		testKat(dig128_3, "a",
			"0CD40739683E15F01CA5DBCEEF4059F1");
		testKat(dig128_3, "HAVAL",
			"DC1F3C893D17CC4EDD9AE94AF76A0AF0");
		testKat(dig128_3, "0123456789",
			"D4BE2164EF387D9F4D46EA8EFB180CF5");
		testKat(dig128_3, "abcdefghijklmnopqrstuvwxyz",
			"DC502247FB3EB8376109EDA32D361D82");
		testKat(dig128_3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"DE5EB3F7D9EB08FAE7A07D68E3047EC6");

		testKat(dig160_3, "",
			"D353C3AE22A25401D257643836D7231A9A95F953");
		testKat(dig160_3, "a",
			"4DA08F514A7275DBC4CECE4A347385983983A830");
		testKat(dig160_3, "HAVAL",
			"8822BC6F3E694E73798920C77CE3245120DD8214");
		testKat(dig160_3, "0123456789",
			"BE68981EB3EBD3F6748B081EE5D4E1818F9BA86C");
		testKat(dig160_3, "abcdefghijklmnopqrstuvwxyz",
			"EBA9FA6050F24C07C29D1834A60900EA4E32E61B");
		testKat(dig160_3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"97DC988D97CAAE757BE7523C4E8D4EA63007A4B9");

		testKat(dig192_3, "",
			"E9C48D7903EAF2A91C5B350151EFCB175C0FC82DE2289A4E");
		testKat(dig192_3, "a",
			"B359C8835647F5697472431C142731FF6E2CDDCACC4F6E08");
		testKat(dig192_3, "HAVAL",
			"8DA26DDAB4317B392B22B638998FE65B0FBE4610D345CF89");
		testKat(dig192_3, "0123456789",
			"DE561F6D818A760D65BDD2823ABE79CDD97E6CFA4021B0C8");
		testKat(dig192_3, "abcdefghijklmnopqrstuvwxyz",
			"A25E1456E6863E7D7C74017BB3E098E086AD4BE0580D7056");
		testKat(dig192_3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"DEF6653091E3005B43A61681014A066CD189009D00856EE7");

		testKat(dig224_3, "",
    "C5AAE9D47BFFCAAF84A8C6E7CCACD60A0DD1932BE7B1A192B9214B6D");
		testKat(dig224_3, "a",
    "731814BA5605C59B673E4CAAE4AD28EEB515B3ABC2B198336794E17B");
		testKat(dig224_3, "HAVAL",
    "AD33E0596C575D7175E9F72361CA767C89E46E2609D88E719EE69AAA");
		testKat(dig224_3, "0123456789",
    "EE345C97A58190BF0F38BF7CE890231AA5FCF9862BF8E7BEBBF76789");
		testKat(dig224_3, "abcdefghijklmnopqrstuvwxyz",
    "06AE38EBC43DB58BD6B1D477C7B4E01B85A1E7B19B0BD088E33B58D1");
		testKat(dig224_3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "939F7ED7801C1CE4B32BC74A4056EEE6081C999ED246907ADBA880A7");

		testKat(dig256_3, "",
    "4F6938531F0BC8991F62DA7BBD6F7DE3FAD44562B8C6F4EBF146D5B4E46F7C17");
		testKat(dig256_3, "a",
    "47C838FBB4081D9525A0FF9B1E2C05A98F625714E72DB289010374E27DB021D8");
		testKat(dig256_3, "HAVAL",
    "91850C6487C9829E791FC5B58E98E372F3063256BB7D313A93F1F83B426AEDCC");
		testKat(dig256_3, "0123456789",
    "63238D99C02BE18C3C5DB7CCE8432F51329012C228CCC17EF048A5D0FD22D4AE");
		testKat(dig256_3, "abcdefghijklmnopqrstuvwxyz",
    "72FAD4BDE1DA8C8332FB60561A780E7F504F21547B98686824FC33FC796AFA76");
		testKat(dig256_3, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "899397D96489281E9E76D5E65ABAB751F312E06C06C07C9C1D42ABD31BB6A404");

		testKat(dig128_4, "",
			"EE6BBF4D6A46A679B3A856C88538BB98");
		testKat(dig128_4, "a",
			"5CD07F03330C3B5020B29BA75911E17D");
		testKat(dig128_4, "HAVAL",
			"958195D3DAC591030EAA0292A37A0CF2");
		testKat(dig128_4, "0123456789",
			"2215D3702A80025C858062C53D76CBE5");
		testKat(dig128_4, "abcdefghijklmnopqrstuvwxyz",
			"B2A73B99775FFB17CD8781B85EC66221");
		testKat(dig128_4, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"CAD57C0563BDA208D66BB89EB922E2A2");

		testKat(dig160_4, "",
			"1D33AAE1BE4146DBAACA0B6E70D7A11F10801525");
		testKat(dig160_4, "a",
			"E0A5BE29627332034D4DD8A910A1A0E6FE04084D");
		testKat(dig160_4, "HAVAL",
			"221BA4DD206172F12C2EBA3295FDE08D25B2F982");
		testKat(dig160_4, "0123456789",
			"E387C743D14DF304CE5C7A552F4C19CA9B8E741C");
		testKat(dig160_4, "abcdefghijklmnopqrstuvwxyz",
			"1C7884AF86D11AC120FE5DF75CEE792D2DFA48EF");
		testKat(dig160_4, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"148334AAD24B658BDC946C521CDD2B1256608C7B");

		testKat(dig192_4, "",
			"4A8372945AFA55C7DEAD800311272523CA19D42EA47B72DA");
		testKat(dig192_4, "a",
			"856C19F86214EA9A8A2F0C4B758B973CCE72A2D8FF55505C");
		testKat(dig192_4, "HAVAL",
			"0C1396D7772689C46773F3DAACA4EFA982ADBFB2F1467EEA");
		testKat(dig192_4, "0123456789",
			"C3A5420BB9D7D82A168F6624E954AAA9CDC69FB0F67D785E");
		testKat(dig192_4, "abcdefghijklmnopqrstuvwxyz",
			"2E2E581D725E799FDA1948C75E85A28CFE1CF0C6324A1ADA");
		testKat(dig192_4, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"E5C9F81AE0B31FC8780FC37CB63BB4EC96496F79A9B58344");

		testKat(dig224_4, "",
    "3E56243275B3B81561750550E36FCD676AD2F5DD9E15F2E89E6ED78E");
		testKat(dig224_4, "a",
    "742F1DBEEAF17F74960558B44F08AA98BDC7D967E6C0AB8F799B3AC1");
		testKat(dig224_4, "HAVAL",
    "85538FFC06F3B1C693C792C49175639666F1DDE227DA8BD000C1E6B4");
		testKat(dig224_4, "0123456789",
    "BEBD7816F09BAEECF8903B1B9BC672D9FA428E462BA699F814841529");
		testKat(dig224_4, "abcdefghijklmnopqrstuvwxyz",
    "A0AC696CDB2030FA67F6CC1D14613B1962A7B69B4378A9A1B9738796");
		testKat(dig224_4, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "3E63C95727E0CD85D42034191314401E42AB9063A94772647E3E8E0F");

		testKat(dig256_4, "",
    "C92B2E23091E80E375DADCE26982482D197B1A2521BE82DA819F8CA2C579B99B");
		testKat(dig256_4, "a",
    "E686D2394A49B44D306ECE295CF9021553221DB132B36CC0FF5B593D39295899");
		testKat(dig256_4, "HAVAL",
    "E20643CFA66F5BE2145D13ED09C2FF622B3F0DA426A693FA3B3E529CA89E0D3C");
		testKat(dig256_4, "0123456789",
    "ACE5D6E5B155F7C9159F6280327B07CBD4FF54143DC333F0582E9BCEB895C05D");
		testKat(dig256_4, "abcdefghijklmnopqrstuvwxyz",
    "124F6EB645DC407637F8F719CC31250089C89903BF1DB8FAC21EA4614DF4E99A");
		testKat(dig256_4, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "46A3A1DFE867EDE652425CCD7FE8006537EAD26372251686BEA286DA152DC35A");

		testKat(dig128_5, "",
			"184B8482A0C050DCA54B59C7F05BF5DD");
		testKat(dig128_5, "a",
			"F23FBE704BE8494BFA7A7FB4F8AB09E5");
		testKat(dig128_5, "HAVAL",
			"C97990F4FCC8FBA76AF935C405995355");
		testKat(dig128_5, "0123456789",
			"466FDCD81C3477CAC6A31FFA1C999CA8");
		testKat(dig128_5, "abcdefghijklmnopqrstuvwxyz",
			"0EFFF71D7D14344CBA1F4B25F924A693");
		testKat(dig128_5, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"4B27D04DDB516BDCDFEB96EB8C7C8E90");

		testKat(dig160_5, "",
			"255158CFC1EED1A7BE7C55DDD64D9790415B933B");
		testKat(dig160_5, "a",
			"F5147DF7ABC5E3C81B031268927C2B5761B5A2B5");
		testKat(dig160_5, "HAVAL",
			"7730CA184CEA2272E88571A7D533E035F33B1096");
		testKat(dig160_5, "0123456789",
			"41CC7C1267E88CEF0BB93697D0B6C8AFE59061E6");
		testKat(dig160_5, "abcdefghijklmnopqrstuvwxyz",
			"917836A9D27EED42D406F6002E7D11A0F87C404C");
		testKat(dig160_5, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"6DDBDE98EA1C4F8C7F360FB9163C7C952680AA70");

		testKat(dig192_5, "",
			"4839D0626F95935E17EE2FC4509387BBE2CC46CB382FFE85");
		testKat(dig192_5, "a",
			"5FFA3B3548A6E2CFC06B7908CEB5263595DF67CF9C4B9341");
		testKat(dig192_5, "HAVAL",
			"794A896D1780B76E2767CC4011BAD8885D5CE6BD835A71B8");
		testKat(dig192_5, "0123456789",
			"A0B635746E6CFFFFD4B4A503620FEF1040C6C0C5C326476E");
		testKat(dig192_5, "abcdefghijklmnopqrstuvwxyz",
			"85F1F1C0ECA04330CF2DE5C8C83CF85A611B696F793284DE");
		testKat(dig192_5, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
			"D651C8AC45C9050810D9FD64FC919909900C4664BE0336D0");

		testKat(dig224_5, "",
    "4A0513C032754F5582A758D35917AC9ADF3854219B39E3AC77D1837E");
		testKat(dig224_5, "a",
    "67B3CB8D4068E3641FA4F156E03B52978B421947328BFB9168C7655D");
		testKat(dig224_5, "HAVAL",
    "9D7AE77B8C5C8C1C0BA854EBE3B2673C4163CFD304AD7CD527CE0C82");
		testKat(dig224_5, "0123456789",
    "59836D19269135BC815F37B2AEB15F894B5435F2C698D57716760F2B");
		testKat(dig224_5, "abcdefghijklmnopqrstuvwxyz",
    "1B360ACFF7806502B5D40C71D237CC0C40343D2000AE2F65CF487C94");
		testKat(dig224_5, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "180AED7F988266016719F60148BA2C9B4F5EC3B9758960FC735DF274");

		testKat(dig256_5, "",
    "BE417BB4DD5CFB76C7126F4F8EEB1553A449039307B1A3CD451DBFDC0FBBE330");
		testKat(dig256_5, "a",
    "DE8FD5EE72A5E4265AF0A756F4E1A1F65C9B2B2F47CF17ECF0D1B88679A3E22F");
		testKat(dig256_5, "HAVAL",
    "153D2C81CD3C24249AB7CD476934287AF845AF37F53F51F5C7E2BE99BA28443F");
		testKat(dig256_5, "0123456789",
    "357E2032774ABBF5F04D5F1DEC665112EA03B23E6E00425D0DF75EA155813126");
		testKat(dig256_5, "abcdefghijklmnopqrstuvwxyz",
    "C9C7D8AFA159FD9E965CB83FF5EE6F58AEDA352C0EFF005548153A61551C38EE");
		testKat(dig256_5, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "B45CB6E62F2B1320E4F8F1B0B273D45ADD47C321FD23999DCF403AC37636D963");

		testCollision(dig128_3,
			"8b447763189fe5d9bb3caaf2ba92cbd6444a54ee76a59f8733"
			+ "46a31c4f5dca76428a7aa68bdc3a8d14d8e3b68d993056cd"
			+ "5dea867bae39a7328efd54362bbbac9a3c183889927ab6b2"
			+ "9972c4e59e0327145e55ddd8189083c9d9bbaa32c68fd7a7"
			+ "b3f4ff96000040ac6a467fc0fbffffd216405fd016405fb0"
			+ "e21200877f30f4",
			"8b487763189fe5d9bb3caaf2ba92cbd6444a54ee76a59f8733"
			+ "46a31c4f5dca76428a7aa68bdc3a8d14d8e3b68d9930d6cd"
			+ "5dea867bae39a7328efd54362bbbac9a3c183889927ab6ba"
			+ "9972c4e59e0327145e55ddd8189083c9d9bbaa32c68fd7a7"
			+ "b3f4ff96000040ac6a467fc0fbffffd216405fd016405fb0"
			+ "e21200877f30f4");

		testCollision(dig128_3,
			"8b447763189fe5d9bb3caaf2ba92cbd6444a54ee76a59f8733"
			+ "46a31c4f5dca76428a7aa68bdc3a8d14d8e3b68d993056cd"
			+ "5dea867bae39a7328efd54362bbbac9a3c183889927ab6b2"
			+ "9972c4e59e0327145e55ddd8189083c9d9bbaa32c68fd7a7"
			+ "b3f4ff96000040ac6a467fc0fbffffd216405fd016405fb0"
			+ "e212006369b1f5",
			"8b487763189fe5d9bb3caaf2ba92cbd6444a54ee76a59f8733"
			+ "46a31c4f5dca76428a7aa68bdc3a8d14d8e3b68d9930d6cd"
			+ "5dea867bae39a7328efd54362bbbac9a3c183889927ab6ba"
			+ "9972c4e59e0327145e55ddd8189083c9d9bbaa32c68fd7a7"
			+ "b3f4ff96000040ac6a467fc0fbffffd216405fd016405fb0"
			+ "e212006369b1f5");

		reportSuccess("HAVAL");
	}

	/**
	 * Test WHIRLPOOL implementation.
	 */
	private static void testWHIRLPOOL()
	{
		Digest dig = new WHIRLPOOL();
		Digest dig0 = new WHIRLPOOL0();
		Digest dig1 = new WHIRLPOOL1();

		testKat(dig, "",
    "19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A7"
    + "3E83BE698B288FEBCF88E3E03C4F0757EA8964E59B63D93708B138CC42A66EB3");
		testKat(dig, "a",
    "8ACA2602792AEC6F11A67206531FB7D7F0DFF59413145E6973C45001D0087B42"
    + "D11BC645413AEFF63A42391A39145A591A92200D560195E53B478584FDAE231A");
		testKat(dig, "abc",
    "4E2448A4C6F486BB16B6562C73B4020BF3043E3A731BCE721AE1B303D97E6D4C"
    + "7181EEBDB6C57E277D0E34957114CBD6C797FC9D95D8B582D225292076D4EEF5");
		testKat(dig, "message digest",
    "378C84A4126E2DC6E56DCC7458377AAC838D00032230F53CE1F5700C0FFB4D3B"
    + "8421557659EF55C106B4B52AC5A4AAA692ED920052838F3362E86DBD37A8903E");
		testKat(dig, "abcdefghijklmnopqrstuvwxyz",
    "F1D754662636FFE92C82EBB9212A484A8D38631EAD4238F5442EE13B8054E41B"
    + "08BF2A9251C30B6A0B8AAE86177AB4A6F68F673E7207865D5D9819A3DBA4EB3B");
		testKat(dig, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz0123456789",
    "DC37E008CF9EE69BF11F00ED9ABA26901DD7C28CDEC066CC6AF42E40F82F3A1E"
    + "08EBA26629129D8FB7CB57211B9281A65517CC879D7B962142C65F5A7AF01467");
		testKat(dig, "123456789012345678901234567890"
			+ "12345678901234567890123456789012345678901234567890",
    "466EF18BABB0154D25B9D38A6414F5C08784372BCCB204D6549C4AFADB601429"
    + "4D5BD8DF2A6C44E538CD047B2681A51A2C60481E88C5A20B2C2A80CF3A9A083B");
		testKat(dig, "abcdbcdecdefdefgefghfghighijhijk",
    "2A987EA40F917061F5D6F0A0E4644F488A7A5A52DEEE656207C562F988E95C69"
    + "16BDC8031BC5BE1B7B947639FE050B56939BAAA0ADFF9AE6745B7B181C3BE3FD");

		testKatMillionA(dig,
    "0C99005BEB57EFF50A7CF005560DDF5D29057FD86B20BFD62DECA0F1CCEA4AF5"
    + "1FC15490EDDC47AF32BB2B66C34FF9AD8C6008AD677F77126953B226E4ED8B01");

		testKat(dig0, "",
    "B3E1AB6EAF640A34F784593F2074416ACCD3B8E62C620175FCA0997B1BA23473"
    + "39AA0D79E754C308209EA36811DFA40C1C32F1A2B9004725D987D3635165D3C8");
		testKat(dig0, "The quick brown fox jumps over the lazy dog",
    "4F8F5CB531E3D49A61CF417CD133792CCFA501FD8DA53EE368FED20E5FE0248C"
    + "3A0B64F98A6533CEE1DA614C3A8DDEC791FF05FEE6D971D57C1348320F4EB42D");
		testKat(dig0, "The quick brown fox jumps over the lazy eog",
    "228FBF76B2A93469D4B25929836A12B7D7F2A0803E43DABA0C7FC38BC11C8F2A"
    + "9416BBCF8AB8392EB2AB7BCB565A64AC50C26179164B26084A253CAF2E012676");

		testKat(dig1, "",
    "470F0409ABAA446E49667D4EBE12A14387CEDBD10DD17B8243CAD550A089DC0F"
    + "EEA7AA40F6C2AAAB71C6EBD076E43C7CFCA0AD32567897DCB5969861049A0F5A");
		testKat(dig1, "The quick brown fox jumps over the lazy dog",
    "3CCF8252D8BBB258460D9AA999C06EE38E67CB546CFFCF48E91F700F6FC7C183"
    + "AC8CC3D3096DD30A35B01F4620A1E3A20D79CD5168544D9E1B7CDF49970E87F1");
		testKat(dig1, "The quick brown fox jumps over the lazy eog",
    "C8C15D2A0E0DE6E6885E8A7D9B8A9139746DA299AD50158F5FA9EECDDEF744F9"
    + "1B8B83C617080D77CB4247B1E964C2959C507AB2DB0F1F3BF3E3B299CA00CAE3");

		reportSuccess("WHIRLPOOL");
	}

	/**
	 * Test HMAC implementation.
	 */
	private static void testHMAC()
	{
		Digest hmac;

		/*
		 * From RFC 2104.
		 */
		hmac = new HMAC(new MD5(),
			strtobin("0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B0B"));
		testKat(hmac, "Hi There",
			"9294727A3638BB1C13F48EF8158BFC9D");
		hmac = new HMAC(new MD5(), encodeLatin1("Jefe"));
		testKat(hmac, "what do ya want for nothing?",
			"750C783E6AB0B503EAA86E310A5DB738");
		hmac = new HMAC(new MD5(),
			strtobin("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
		testKat(hmac, "\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD"
			+ "\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD"
			+ "\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD"
			+ "\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD"
			+ "\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD"
			+ "\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD\u00DD"
			+ "\u00DD\u00DD\u00DD",
			"56BE34521D144C88DBB8C733F0E8B3F6");

		/*
		 * From FIPS 198a.
		 */
		hmac = new HMAC(new SHA1(),
			strtobin("000102030405060708090A0B0C0D0E0F101112131"
				+ "415161718191A1B1C1D1E1F20212223242526272"
				+ "8292A2B2C2D2E2F303132333435363738393A3B3"
				+ "C3D3E3F"));
		testKat(hmac, "Sample #1",
			"4F4CA3D5D68BA7CC0A1208C9C61E9C5DA0403C0A");

		hmac = new HMAC(new SHA1(),
			strtobin("303132333435363738393A3B3C3D3E3F40414243"));
		testKat(hmac, "Sample #2",
			"0922D3405FAA3D194F82A45830737D5CC6C75D24");

		hmac = new HMAC(new SHA1(),
			strtobin("505152535455565758595A5B5C5D5E5F606162636"
				+ "465666768696A6B6C6D6E6F70717273747576777"
				+ "8797A7B7C7D7E7F808182838485868788898A8B8"
				+ "C8D8E8F909192939495969798999A9B9C9D9E9FA"
				+ "0A1A2A3A4A5A6A7A8A9AAABACADAEAFB0B1B2B3"));
		testKat(hmac, "Sample #3",
			"BCF41EAB8BB2D802F3D05CAF7CB092ECF8D1A3AA");

		hmac = new HMAC(new SHA1(),
			strtobin("707172737475767778797A7B7C7D7E7F808182838"
				+ "485868788898A8B8C8D8E8F90919293949596979"
				+ "8999A9B9C9D9E9FA0"), 12);
		testKat(hmac, "Sample #4",
			"9EA886EFE268DBECCE420C75");

		reportSuccess("HMAC");
	}
}
