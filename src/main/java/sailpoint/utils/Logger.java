package sailpoint.utils;

/**
 * Changed Logger class to be singleton to make it accessible to other classes with a single instance.
 * 
 * TODO: This needs to be replaced with Log4J.  Will then allow aggregation of logs to Splunk etc too.
 */

public class Logger {

	boolean verbose = true;
	
	private static Logger instance = null;

	private Logger() {
		this( false );
	}

	public Logger( boolean verbose ) {
		super();
		this.verbose = verbose;
	}

	public void error( String message ) {
		System.out.println(String.format("ERROR: %s", message));
	}
	
	public void info( String message ) {
		System.out.println(String.format("INFO: %s", message));
	}
	
	public void debug( String message ) {
		if ( verbose )
			System.out.println(String.format("DEBUG: %s", message));
	}
	
	// Updated to singleton.
	public static Logger getInstance(boolean verbose) {
		if (Logger.instance == null) {
			Logger.instance = new Logger(verbose);
		}
		return Logger.instance;
	}
	
	public static Logger getInstance() {
		if (Logger.instance == null) {
			Logger.instance = Logger.getInstance(true);
		}
		return Logger.instance;
	}
}