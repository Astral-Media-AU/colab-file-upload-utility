package sailpoint.utils;

/**
 * Changed Logger class to be singleton to make it accessible to other classes with a single instance.
 */

public class Logger {

	boolean verbose = false;
	
	private static Logger instance = null;

	private Logger() {
		this( false );
	}

	public Logger( boolean verbose ) {
		super();
		this.verbose = verbose;
	}

	public void error( String message ) {
		System.out.println( message );
	}
	
	public void info( String message ) {
		System.out.println( message );
	}
	
	public void debug( String message ) {
		if ( verbose )
			System.out.println( message );
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
			Logger.instance = Logger.getInstance(false);
		}
		return Logger.instance;
	}
}