package uc.protocol.client;


import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import uc.DCClient;

public class ADCClientTest {

	private static DCClient dcc;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		dcc = new DCClient();
		dcc.start(new NullProgressMonitor());
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dcc.stop(false);
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	
	
	
	
}
