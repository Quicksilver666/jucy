package uc.protocol.hub;

import static org.junit.Assert.*;

import helpers.GH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;



import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import eu.jucy.testfragment.TestHubConnection;

import uc.Command;
import uc.DCClient;
import uc.FavHub;
import uc.HubListenerAdapter;
import uc.ICryptoManager;
import uc.IUDPHandler;
import uc.IUser;
import uc.PI;
import uc.UDPhandler;

import uc.DCClient.Initializer;
import uc.FavFolders.SharedDir;
import uc.crypto.BloomFilter;
import uc.crypto.HashValue;
import uc.crypto.UDPEncryption;
import uc.crypto.IHashEngine.IHashedListener;
import uc.files.filelist.FileListFile;
import uc.files.search.FileSearch;
import uc.protocols.AbstractADCCommand;
import uc.protocols.ConnectionProtocol;
import uc.protocols.ConnectionState;
import uc.protocols.DCProtocol;
import uc.protocols.IConnection;
import uc.protocols.hub.FeedType;
import uc.protocols.hub.Hub;
import uc.protocols.hub.PrivateMessage;
import uc.protocols.hub.SUP;
import uc.protocols.hub.Hub.ConnectionInjector;

public class AdcHubTest {

	private static DCClient dcc;
	private TestHubConnection thc;
	private static TestUDPHandler tudpH;
	private Hub hub;
	
	
	private static final List<String> userINF = new ArrayList<String>();
	private static final List<String> sharedFiles = new ArrayList<String>();
	private static final File sharedStuff = new File(new File(PI.getStoragePath(),".."),"SharedStuff");
	private static final long[]  sharedFilesizes;
	private static final String SidOwn 	 = "Y2NH";
	private static final String SidAsterix 	 = "HDTK";
	private static final String SidActiveUser = SidAsterix ;
	private static final String CIDAsterix = "JSJK4ALI7EZVQSYFGF6HD6SLDP67OITQAMLHT3Y",
								CIDObelix= "MLKJ7IKCCT3LYSONVW5FLQ6SEBYM4CIFWEJMRKA";

	
	private static final String SidPassiveUser = "IU7D";
	
	private static final String ignoreINFprefix = "BINF ";
	
	static {
		String[] users= new String[] {
				"BINF "+SidAsterix+" ID"+CIDAsterix+" NIAsterix I4213.89.70.149 HN3 HO0 HR0 SF10368 SS203141739008 SL9 SUTCP4,UDP4 U41408 VE++\\s0.699 US5242",
				"BINF CQHS ID"+CIDObelix+" NIObelix I481.200.167.218 HN8 HO0 HR0 SF0 SS0 SL1 SUADC0,TCP4,UDP4 U44638 VE++\\s0.702 US10485760",
				"BINF HLGC IDEJCANVZXUZ2SNJ4GIWTZUOQPDCASCTZ4HVLRFRY NIMajestix I482.196.97.148 DEWhisky HN0 HO2 HR1 CT4 SF0 SS0 SL1 SUADC0,TCP4,UDP4 U4412 VE++\\s0.705 US5242",
				"BINF OVD7 IDFAJ6HFSJVEUWGTRT75LTHEI7PI5QELRP7CG5VOI NIIdefix I488.83.46.99 DS49152 HN11 HO0 HR0 SF1414 SS38341855326 SL1 SUTCP4,UDP4 U41412 VE++\\s0.699 US2097152",
				"BINF FLO7 IDCL4VPGSZ6OBIO35GFIDCWJGL2ZBM76V6UKKDKXA NITroubadix HN60 HO0 HR0 SF11634 SS167565105641 SL2 SUADC0 VE++\\s0.704 US2097152",
				"BINF "+SidPassiveUser+" IDMYAWASFR4R7CZVTRMJCBL643M3J5ZGI4CZE42KQ NIMiraculix DEsdc++ HN51 HO0 HR0 SF4465 SS11308078932 SL6 SUADC0 VE++\\s0.704 US1048576"};

		userINF.addAll(Arrays.asList(users));
		
		String[] files = new String[] {
			"Lain Iwakura.avi",
			"Alice Mizuki.mp3",
			"Yasuo Iwakura.png"
		};
		sharedFiles.addAll(Arrays.asList(files));
		sharedFilesizes = new long[files.length];
		sharedFilesizes[0] = 0;
		sharedFilesizes[1] = 1025;
		sharedFilesizes[2] = 1024 * 1024 * 4;
		
	}
	
	public static class TestUDPHandler extends UDPhandler {
		public final Semaphore searchRSSent = new Semaphore(0);
		
		public TestUDPHandler(DCClient dcc) {
			super(dcc);
		}
		
		@Override
		public void sendPacket(ByteBuffer packet,
				InetSocketAddress target) {
			searchRSSent.release();
		}
		
		@Override
		public void receivedPacket(InetSocketAddress from,
				ByteBuffer packet, boolean possiblyEncrypted) {
			super.receivedPacket(from, packet, possiblyEncrypted);
		}
	}
	
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
			public IConnection getConnection(ICryptoManager manager,String addy, ConnectionProtocol connectionProt,boolean encryption,HashValue fingerPrint) {
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
		FavHub fh = new FavHub("adcs://127.0.0.1:456");
		
		hub = (Hub) fh.connect(dcc);
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
	public void testLoginADC() throws Exception {
		try {
			//on connect we Send supports
			String hsup = thc.pollNextMessage(false);
			assertEquals(SUP.SUPPORTS, hsup);
			
			/* Example messages from start...
			ISUP ADBASE ADTIGR ADUCM0 ADPING
			ISID 7KJB
			IINF CT32 VEDSHub\sTheta NIFuture][Imperial-Networks DEWelcome\sto\sImperial\sNetworks
			ISTA 000 Running\sTheta\sVersion\sof\sDSHub.
			ISTA 000 Hub\sis\sup\ssince\sThu\sMar\s20\s19:36:54\sCET\s2008 */
			
			//as an answer the hub sends: his supports, inf and gives us a SID
			hub.receivedCommand("ISUP ADBASE ADTIGR ADUCM0 ADPING");
		
			hub.receivedCommand("ISID "+SidOwn);
			String hubnick= "HubNick";			
			String hubDe = "HubDescription";
			hub.receivedCommand("IINF NI"+hubnick+" DE"+hubDe+" VEStatusMessage");
			assertEquals(hubnick, hub.getHubname());
			assertEquals(hubDe, hub.getTopic());
			
			

			String clientInf = thc.pollNextMessage(true);
			IUser u = hub.getSelf();
			

			//BINF 7KJB IDPUK62XDM5GLHOJ4NZ6KCFUGPEW5B3FKC4HMKRSA PDEZROSKSJ54SNWZFEIECDFAH5IGJRYMM5SZ2RWHQ NIQuicksilver SL4 SS695217057756 SF9712 HN1 HR0 HO0 VE++\s0.704 US5242 SUADC0;
			assertTrue(clientInf.startsWith("BINF "+SidOwn+" "));
			

			
			//add some users..
			for (String s: userINF) {
				hub.receivedCommand(s);
			}
			assertEquals(ConnectionState.CONNECTED, hub.getState());
			//now receive own inf
			String receivedINF = clientInf.replace(" PD"+u.getPD(), ""); //just delete PID from own INF
			
			String ip = "127.0.0.1"; //TODO check that IP and CT are accepted 
			receivedINF = receivedINF.replace("I40.0.0.0", "I4"+ip); //and replace IP..
			hub.receivedCommand(receivedINF); //receive own INF
			
			
			
			assertEquals("Received client inf: "+receivedINF,ConnectionState.LOGGEDIN, hub.getState());
			assertEquals("found: "+(userINF.size()+1)+" "+hub.getUsers().size(),userINF.size()+1 , hub.getUsers().size() ); //check all users noted down..
			assertTrue(!hub.getUserByNick("Miraculix").isTCPActive()); // check modechar correctly found
			assertTrue(hub.getUserByNick("Asterix").isTCPActive()); // same
			
			
		} catch (Exception e) {
			fail(e.toString());
		}
		checkErrorLogsEmpty();
	}


	@Test
	public void testSearch() throws Exception {
		goToLoggedInState();
		
		//for now on ignore further INF messages..
		thc.addIgnore(ignoreINFprefix);
		
		//first check search with empty share
		
		//FSCH XCIY +TCP4 ANbug ANmafia ANsi ANluchian-la ANfurat TOmanual
		String searchPassive = "BSCH "+SidPassiveUser+" ANIwakura TOauto";  //TRXKVJNWYEQYJQGUQ4CR2RI3YMTUHP2FKGVRKNMMQ
		String searchTTHmiss = "BSCH "+SidPassiveUser+" TRN7RLQKGSVAQOUWZKWYTCKGADQLSTP6K2CSOBBJI TOauto";
		
		hub.receivedCommand(searchPassive);
		Thread.sleep(500); // allow for time until search is executed..
		assertTrue(thc.isMessageSentEmpty());
		
		hub.receivedCommand(searchTTHmiss);
		Thread.sleep(500); // allow for time until search is executed..
		assertTrue(thc.isMessageSentEmpty());
		

		prepareSharedFiles();
		

		//check passive search that hits..
		hub.receivedCommand(searchPassive);
		//should produce 2 hits now
		Thread.sleep(500); // allow for time until search is executed..
		
		String one = thc.pollNextMessage(false);
		String two = thc.pollNextMessage(false);
		assertTrue((one.contains("Lain") && two.contains("Yasuo"))^
		 (one.contains("Yasuo") && two.contains("Lain")));
		
		assertTrue(thc.isMessageSentEmpty());

		
		// check passive search that fails..
		
		hub.receivedCommand(searchPassive+" ANSerial\nExperiments");
		Thread.sleep(500);
		assertTrue(thc.isMessageSentEmpty()); //either no message or just an INF

	
		// check active search that hits..
		hub.receivedCommand(searchPassive.replace(SidPassiveUser, SidActiveUser));
		
		assertTrue(tudpH.searchRSSent.tryAcquire(2, 500, TimeUnit.MILLISECONDS));
		
		
		FileListFile file = null;
		for (FileListFile f : dcc.getOwnFileList().getRoot()) {
			file = f;
			break;
		}
		assertNotNull(file);
		
		FileSearch fs = new FileSearch(file.getTTHRoot());
		dcc.register(fs);
		dcc.search(fs);
		String searchMessageSent = thc.pollNextMessage(true);
		assertTrue("Not a search",searchMessageSent.contains("SCH "));
		assertTrue("search not contained correct hash", searchMessageSent.contains(file.getTTHRoot().toString()));
		
		String packet = "URES "+CIDAsterix+" TO"+fs.getToken()+" TR"+file.getTTHRoot()
		+" SI"+file.getSize()+" FN"+AbstractADCCommand.doReplaces(file.getPath().replace(File.separatorChar , '/'))  +"\n";
		
		tudpH.receivedPacket(new InetSocketAddress("127.0.0.1", 5000), 
				ByteBuffer.wrap(packet.getBytes(DCProtocol.ADCCHARENCODING)), true);
		
		assertEquals("expected exactly one File: ",1, fs.getNrOfFiles());
		
		
		String packet2 = packet.replace(CIDAsterix, CIDObelix); //same result from different user..
		byte[] packet2bytes = packet2.getBytes(DCProtocol.ADCCHARENCODING);
		byte[] packet2Ency = UDPEncryption.encryptMessage(packet2bytes, fs.getEncryptionKey());
		tudpH.receivedPacket(new InetSocketAddress("127.0.0.1", 5001), 
				ByteBuffer.wrap(packet2Ency), true);
		
		
		assertEquals("encrypted File not received: "+fs.getNrOfResults(),2, fs.getNrOfResults());
		
		removeSharedFiles();
		thc.removeIgnore(ignoreINFprefix); //clean up ignore..
		checkErrorLogsEmpty();

	}
	
	@Test
	public void testChat() throws Exception {
		goToLoggedInState();
		
		final String[] m = new String[5];
		final IUser[] u = new IUser[5];
		
		hub.registerHubListener(new HubListenerAdapter() {

			public void mcReceived(IUser sender, String message,boolean me) {
				m[0] = message; 
				u[0] = sender;
			}

			public void mcReceived(String message) {
				m[1] = message; 
			}

			public void statusMessage(String message, int severity) {
				m[2] = message; 
			}


			public void pmReceived(PrivateMessage pm) {
				m[3] = pm.getMessage(); 
				u[3] = pm.getFrom();
				u[4] = pm.getSender();
			}

			public void feedReceived(FeedType ft, String message) {
				m[4] = message;
			}
			
			
		});
		//EMSG EBHH WUNF hello\sBot PMEBHH
		//BMSG EBHH Hello
		String message = "Hello";
		hub.receivedCommand("BMSG "+SidAsterix+" "+message);
		hub.receivedCommand("EMSG "+SidAsterix+" "+SidOwn+" "+message+" PM"+SidAsterix);
		
		IUser asterix = hub.getUserByNick("Asterix");
		assertEquals(message,m[0]);
		assertEquals(asterix,u[0]);
		
		assertNull(m[1]); //TODO implement test..
		assertNull(m[2]);
		
		assertEquals(message,m[3]);
		assertEquals(asterix,u[3]);
		assertEquals(asterix,u[4]);
		
		assertNull(m[4]); //TODO implement test..
		
		checkErrorLogsEmpty();

		//TODO test message sending
	}
	
	
	@Test
	public void testBloomFeature() throws Exception {
		goToLoggedInState();
		prepareSharedFiles();
		thc.addIgnore(ignoreINFprefix);
		thc.clear();
		
		int m = 1024,h = 24,k = 8;
		hub.receivedCommand("IGET blom / 0 "+(m/8)+" BH"+h+" BK"+k); //test some Blomfilter
		
		ByteBuffer bb = thc.pollNextByteBuffer(1000);
		assertNotNull(bb);
		String command = "";
		while(bb.remaining() > m/8) {
			command += (char)bb.get();
		}
		assertTrue(command.startsWith("HSND blom / 0 "+(m/8)));
		
		byte[] bloomFilter = new byte[m/8];
		bb.get(bloomFilter);
		
		BloomFilter bloomreceived = new BloomFilter(bloomFilter,h,k); 
		
		BloomFilter selfBuild = BloomFilter.create(dcc.getOwnFileList(), m, h, k);
		
		assertEquals(selfBuild, bloomreceived);
		for (FileListFile f:dcc.getOwnFileList().getRoot()) {
			assertTrue("File not Present: "+f.getName(), bloomreceived.possiblyContains(f.getTTHRoot()));
		}
		
		removeSharedFiles();
		thc.removeIgnore(ignoreINFprefix); //clean up ignore..
		checkErrorLogsEmpty();
	}
	
	
	@Test
	public void testCMDFeature() throws Exception {
		goToLoggedInState();

		assertEquals(0, hub.getUserCommands().size());
		
		int context = 1;
		String tt = "BINF %[mySID] NI%[line:Nick]\n";
		String path = "Change nick...";
		
		hub.receivedCommand("ICMD "+AbstractADCCommand.doReplaces(path)
				+" CT"+context+" TT"+AbstractADCCommand.doReplaces(tt));
		
		assertEquals(1, hub.getUserCommands().size());
		Command com = hub.getUserCommands().get(0);
		
		assertEquals(context,com.getWhere());
		assertEquals(tt, com.getCommand());
		assertEquals(path, com.getPath());
		assertEquals(path, com.getName()); //as it has no folder structure..
		
		checkErrorLogsEmpty();
	}
	
	

	
	public static void checkErrorLogsEmpty() {
		assertEquals("error.log not empty",0,LoggerFactory.getErrorLog().length());
		assertEquals("Platform .log not empty",0,
				new File(new File(PI.getStoragePath(),".metadata"),".log").length());
	}
	
	private void goToLoggedInState() throws Exception {
		testLoginADC();
	}
	
	private static void createFiles(File parent) throws IOException {
		if (!parent.isDirectory()) {
			parent.mkdirs();
		}
		for (int i = 0 ; i < sharedFiles.size(); i++ ) {
			File target = new File(parent,sharedFiles.get(i));
			FileOutputStream fos = new FileOutputStream(target);
			for (long w = 0; w < sharedFilesizes[i];w++) {
				fos.write(i);
			}
			fos.flush();
			fos.close();
		}
	}
	
	
	public static void prepareSharedFiles() throws Exception  {
		
		createFiles(sharedStuff);
		SharedDir sd = new SharedDir("shared",sharedStuff);
		
		//final Semaphore waitHashingFinished = new Semaphore(0);
		dcc.getHashEngine().registerHashedListener(new IHashedListener() {
			int count = sharedFiles.size();
			public void hashed(File f, long duration, long remainingSize) {
				if (--count == 0) {
					dcc.getFilelist().refresh(true);
				}
			}
		});
		
		dcc.getFavFolders().storeSharedDirs(Collections.singletonList(sd));
		//now wait some seconds for the hashing
		while( dcc.getFilelist().getNumberOfFiles() < sharedFiles.size() ) {
			GH.sleep(500);
			dcc.getFilelist().refresh(true);
		}
		
		assertEquals(sharedFiles.size(), dcc.getOwnFileList().getNumberOfFiles());
		
	}
	
	public static void removeSharedFiles() throws Exception  { 
		for (File f:sharedStuff.listFiles()) {
			assertTrue(f.delete());
		}
		dcc.getFavFolders().storeSharedDirs(Collections.<SharedDir>emptyList());
		dcc.getFilelist().refresh(true);
		assertEquals(0, dcc.getOwnFileList().getNumberOfFiles());
		assertTrue(sharedStuff.delete());
	}
	
}
