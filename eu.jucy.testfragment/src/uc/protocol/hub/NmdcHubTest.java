package uc.protocol.hub;

import static org.junit.Assert.*;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import uc.DCClient;
import uc.FavHub;
import uc.IUDPHandler;
import uc.IUser;
import uc.DCClient.Initializer;
import uc.crypto.HashValue;
import uc.protocol.hub.AdcHubTest.TestUDPHandler;
import uc.protocols.ConnectionProtocol;
import uc.protocols.IConnection;
import uc.protocols.hub.Hub;
import uc.protocols.hub.Hub.ConnectionInjector;
import eu.jucy.testfragment.TestHubConnection;


public class NmdcHubTest {

	private static DCClient dcc;
	private static TestUDPHandler tudpH;
	private TestHubConnection thc;
	
	private Hub hub;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DCClient.setInitializer(new Initializer() {
			@Override
			protected IUDPHandler createUDPHandler(DCClient dcc) {
				return tudpH = new TestUDPHandler(dcc);
			}
		});
		
		dcc = new DCClient();
		dcc.start(new NullProgressMonitor());
		Hub.setConnectionInjector(new ConnectionInjector() {
			@Override
			public IConnection getConnection(String addy, ConnectionProtocol connectionProt,boolean encryption,HashValue fingerPrint) {
				return new TestHubConnection(addy, connectionProt, encryption,fingerPrint);
			}
		});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dcc.stop(false);
	}
	
	@Before
	public void setUp() throws Exception {
		FavHub fh = new FavHub("dchub://127.0.0.1:456");
		hub = (Hub)fh.connect(dcc);
		thc =  (TestHubConnection)hub.getConnection();
		Thread.sleep(200);
	}

	@After
	public void tearDown() throws Exception {
		tudpH.searchRSSent.drainPermits();
		hub.close();
		hub = null;
		thc = null;
	}
	
	
	@Test
	public void testLoginNMDC() throws Exception {
		//on connect the hub sends us some kind of Lock
		hub.receivedCommand("$Lock EXTENDEDPROTOCOL::This_hub_was_written_by_Yoshi::CTRL[.4.'.] Pk=YnHub");

		String supportKeyValidateNick = thc.pollNextMessage(false);
		assertTrue("Bad key: ",supportKeyValidateNick.startsWith("$Supports ") && 
				supportKeyValidateNick.contains("|$Key "));
		IUser u = hub.getSelf();
		String expected= "$ValidateNick "+u.getNick()+"|";
		assertTrue("received: "+supportKeyValidateNick+"\n expected end is: "+expected,
				supportKeyValidateNick.endsWith(expected));
		
		
		String hubname = "TestHubname";
		// now hub sends 
		hub.receivedCommand("$Supports NoHello NoGetINFO UserIP2 BotINFO Feed MCTo ");
		hub.receivedCommand("$HubName "+hubname);
		hub.receivedCommand("$Hello "+u.getNick());
		
		// now dc client sends: $Version 1,0091|$GetNickList|$MyINFO $ALL Test <++ V:0.699,M:A,H:1/0/0,S:1>$ $0.005.$$5937142231$|
		String version = thc.pollNextMessage(true);
		assertEquals("$Version 1,0091", version);
		
		String nicklist = thc.pollNextMessage(true);
		assertEquals( "$GetNickList", nicklist);
		
		String myinfo = thc.pollNextMessage(true);
		assertTrue("Received: "+myinfo,myinfo.startsWith("$MyINFO $ALL "+u.getNick()));
		
		
		
		AdcHubTest.checkErrorLogsEmpty();
	}
	
	
	@Test
	public void testSearch() throws Exception {
		goToLoggedInState();
		
		
		
		
		//TODO implement
		
		AdcHubTest.checkErrorLogsEmpty();
	}
	
	
	private void goToLoggedInState() throws Exception {
		testLoginNMDC();
	}
	
	
	
}
