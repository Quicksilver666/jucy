package uc.protocol;

import static org.junit.Assert.*;

import helpers.GH;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import uc.protocols.Socks;
import uc.protocols.Socks.UDPRelay;

public class SocksTest {

	private static final InetSocketAddress socksserver = new InetSocketAddress("192.168.0.18",1080); //$NON-NLS-1$
	
	private static Thread t;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		t = new Thread() {
			public void run() {
				SOCKS.main(new String[] {});
			}
		};
		t.start();
		GH.sleep(1000);
	}

	@SuppressWarnings("deprecation")
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (t != null) {
			t.interrupt();
			t.stop();
		}
	}
	

	
	@Test
	public void testConnect() {
		Socks s = new Socks("","",socksserver); //$NON-NLS-1$ //$NON-NLS-2$
		
		try {
			SocketChannel sc = SocketChannel.open();
			s.connect(sc, new InetSocketAddress("du-hub1.dnsalias.com",6999)); //$NON-NLS-1$
			ByteBuffer bu = ByteBuffer.allocate(20);
			int count = sc.read(bu);
			assertTrue(Messages.SocksTest_4,count > 0);
			bu.flip();
			
			String lock = new String(bu.array());
			assertTrue("Message did not start with $Lock: "+lock, lock.startsWith("$Lock ")); //$NON-NLS-1$ //$NON-NLS-2$
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
			fail("IOE: "+ioe); //$NON-NLS-1$
		}
	}

	
	@Test
	public void testOpenUDPRelay() {
		Socks s = new Socks("","",socksserver); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			int localPort = 23232;
			DatagramSocket ds = new DatagramSocket(localPort);
			ds.setSoTimeout(5000);
			
			UDPRelay udpr = s.openUDPRelay();
			String written = "0123456789"; //$NON-NLS-1$
			udpr.send(written.getBytes(), new InetSocketAddress("192.168.0.18",localPort)); //$NON-NLS-1$
			DatagramPacket dp = new DatagramPacket(new byte[10],10);
			ds.receive(dp);
			
			String read = new String(dp.getData());
			assertEquals(written, read);
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
			fail(Messages.SocksTest_12+ioe);
		}
	
	} 

}
