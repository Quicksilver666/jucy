package eu.jucy.gui;


import helpers.GH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;



import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;


import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;


import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

import uc.DCClient;
import uc.PI;



public class Application implements IApplication {

	//private static final Logger logger = LoggerFactory.make(); no logger here -> Application is loaded before logger plug-in

	public static final String PLUGIN_ID = "eu.jucy.gui";
//	public static final String DEFAULT_PRESENTATION_ID =  "eu.jucy.gui.testFactory";
	public static final String DEFAULT_KEY_SCHEME = "uc.default", MAC_KEY_SCHEME = "uc.macosx";
	public static final String SIMPLE_OS = "UC.simpleos";
	
	public static volatile Event stored = null;
	
	private FileLock lock;
	
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed()) {
					workbench.close();
				}
			}
		});

	}


	public Object start(IApplicationContext context) throws Exception {

		
		if (!checkSanity()) {
			return EXIT_OK; 
		}
//		renameSettingFiles(false);
		String keyScheme = Platform.getOS().equals(Platform.OS_MACOSX)?MAC_KEY_SCHEME:DEFAULT_KEY_SCHEME;
		PlatformUI.getPreferenceStore().setDefault(
				IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID, keyScheme);

		setSimpleOS();
		List<String> args = Arrays.asList(Platform.getApplicationArgs());
		System.out.println(GH.concat(args, ";"));
		if (args.contains("-nogui")) {
			DCClient dcc = new DCClient();
			dcc.start(new NullProgressMonitor() {
				@Override
				public void subTask(String name) {
					System.out.println(name);
				}
				
			});
			GH.sleep(60000);
			System.out.println("started");
			while (!dcc.getHubs().isEmpty()) {
				GH.sleep(500);
			}
			dcc.stop(true);
			return EXIT_OK;
			
		} else {
			Display display = PlatformUI.createDisplay();
			
			try {
				
				int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
				if (returnCode == PlatformUI.RETURN_RESTART) {
					return EXIT_RESTART;
				}
				
				return EXIT_OK;
				
			} catch (Throwable t) {
				File f = new File(uc.PI.getStoragePath(),"error.log");
				PrintStream ps = new PrintStream(new FileOutputStream(f,true));
				t.printStackTrace(ps);
				ps.flush();
				ps.close();
				return EXIT_OK;
			} finally {
				display.dispose();
			}
		}
	}
	
	/**
	 * this is used to make the OS available to 
	 * core command expressions to test against with SystemTest
	 */
	private void setSimpleOS() {
		String simpleos= Platform.getOS();
		System.setProperty(SIMPLE_OS, simpleos);
	}


	private boolean checkLock() {
		File f = new File(PI.getStoragePath(),".lock");
		try {
			if (!f.isFile()) {
				f.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(f);
			FileChannel fc = fos.getChannel();
			lock = fc.tryLock();
			if (lock != null) {
				f.deleteOnExit();
			}
			fos.close();
			fc.close();
			return lock != null ;
			
		} catch (IOException ioe) {
			System.err.println("Could not accquire lock on workspace: "+ioe);
		}
		return false;
	}
	
	private boolean checkSanity() {
		try {
			Pattern.compile(Pattern.quote("\\test if quote is present in Pattern class\\"));
		} catch(Throwable t) {
			System.out.println("No proper classpath set to JVM" +
					"\nOne way to fix is to install a sun vm so jucy can work properly!" +
					"\n\"sudo apt-get install sun-java6-jre\" and \"sudo update-java-alternatives -s java-6-sun\"" +
					"\nshould do the Job!");	
			return false;
		}	
		
		
		if (!checkLock()) {
			System.out.println("An instance of this application is already running!");
			return false;
		}

		return true;
	}
	
//	/**
//	 * renames old plugin name settings to newer ones..
//	 * @param simulate - false for doing the work .. true for just printing the renames..
//	 */
//	private static void renameSettingFiles(boolean simulate) {
//		File f = PI.getStoragePath();
//		File path = new File(new File(new File(f,".metadata"),".plugins"),"org.eclipse.core.runtime");
//		path = new File(path,".settings");
//		
//		if (path.isDirectory()) {
//			for (File file: path.listFiles()) {
//				if (file.isFile() && file.getName().matches("d[eu]\\.du_hub\\.uc\\..*")) {
//					File targetFile = new File(path,file.getName().replaceFirst("d[eu]\\.du_hub\\.uc", "eu.jucy"));
//					if (simulate) {
//						System.out.println("renameing: "+file+"  to: "+targetFile);
//					} else {
//						file.renameTo(targetFile);
//					}
//				}
//			}
//		}
//	}

}
