package logger;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.runtime.Platform;

public final class LoggerFactory {

	private static final Logger logger = Logger.getRootLogger();

	private static File errorLog;
	
	public static File getErrorLog() {
		return errorLog;
	}

	static {
		// initialise the logger
		try {
			Layout layout = new PatternLayout(
					/*Platform.inDevelopmentMode() ? */ "%d{dd MMM yyyy HH:mm:ss,SSS} %-5p %F Line:%L \t\t %m%n"
							/* : "%-5p: %m%n" */);
			
			
			if (Platform.inDevelopmentMode()) {
				ConsoleAppender consoleAppender = new ConsoleAppender(layout);
				logger.addAppender(consoleAppender);
			}
			
			errorLog = new File(Platform.getInstanceLocation().getURL().getFile(),"error.log");
			
			//for severe errors..
			FileAppender fileAppender = new FileAppender(layout, errorLog.getPath(), true);
				fileAppender.setThreshold(Level.WARN);
			logger.addAppender(fileAppender);
	
			// ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
			logger.setLevel(Level.ALL);
		} catch (IOException ex) {
			System.out.println(ex);
		}

	}

	private LoggerFactory() {
	}

	public static Logger make(Level level) {
		return makeIndirect(level);
	}
	/**
	 * 
	 * @return a logger for the calling class 
	 * (used so loggers can be created in static context using always the same line..)
	 */
	public static Logger make() {
		return makeIndirect(Level.INFO);
	}
	
	private static Logger makeIndirect(Level level) {
		Throwable t = new Throwable();
		StackTraceElement directCaller = t.getStackTrace()[2];
		Logger l = null;
		try {
			l = Logger.getLogger(Class.forName(directCaller.getClassName()));
		} catch (ClassNotFoundException cnfe) {
			l = Logger.getLogger(directCaller.getClassName());
		}
		if (!Platform.inDevelopmentMode() && Level.INFO.isGreaterOrEqual(level)) {
			level = Level.INFO;
		}
		l.setLevel(level);
		return l;
	}

	/**
	 * allows adding an appender to the Root logger
	 * @param appender - the appender to be added
	 */
	public static void addAppender(Appender appender) {
		logger.addAppender(appender);
	}
	
	public static void clearErrorLog() {
		
		errorLog.deleteOnExit();
	}

}
