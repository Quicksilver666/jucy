package eu.jucy.op;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import uc.PI;

import eu.jucy.op.fakeshare.FileListStorage;

public class Activator extends AbstractUIPlugin {

	//  Shared instance of bundle context
	public static BundleContext bundleContext;
	
	private static final Object synch = new Object();
	private static FileListStorage storage;
	private static OperatorPlugin opPlugin;
	
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		synchronized (synch) {
			storage = new FileListStorage();
			storage.init(PI.getStoragePath());
			opPlugin = new OperatorPlugin();
		}
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		bundleContext = null;
		synchronized (synch) {
			storage.shutdown();
		}
	}
	
	public static FileListStorage getStorage() {
		synchronized (synch) {
			return storage;
		}
	}
	
	public static OperatorPlugin getOPPlugin() {
		synchronized (synch) {
			return opPlugin;
		}
	}
	
}
