package sailpoint.app;

import okhttp3.ResponseBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import sailpoint.object.Source;
import sailpoint.object.config.Config;
import sailpoint.object.config.ConfigAggregation;
import sailpoint.service.FileProcessorService;
import sailpoint.service.SailPointService;
import sailpoint.utils.ConfigUtils;
import sailpoint.utils.EncryptionUtils;
import sailpoint.utils.FileManagementUtils;
import sailpoint.utils.Logger;
import sailpoint.utils.Reporter;
import sailpoint.utils.SailPointUrl;
import sailpoint.utils.Timer;

import static sailpoint.app.FileUploadUtility.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(usageHelpAutoWidth = true, name = "java -jar sailpoint-file-upload-utility.jar", sortOptions = false, headerHeading = "%nUsage:%n%n", synopsisHeading = "%n", descriptionHeading = "%nDescription:%n%n", parameterListHeading = "%nParameters:%n", optionListHeading = "%nOptions:%n", header = "Perform bulk file aggregations to Identity Security Cloud.", description = "Scans specified files and directories for files in bulk, to send to Identity Security Cloud for account or entitlement aggregation.  For more details see: "
		+ ABOUT_LINK, version = { "SailPoint File Upload Utility " + ABOUT_VERSION, "Build: " + ABOUT_DATE,
				"Documentation: " + ABOUT_LINK,
				"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
				"OS: ${os.name} ${os.version} ${os.arch}" })
public class FileUploadUtility implements Callable<Integer> {

	/**
	 * Metadata about the File Upload Utility
	 */
	public static final String ABOUT_DATE = "2025-06-28 03:15 AWST";
	public static final String ABOUT_VERSION = "4.1.1-RC";
	public static final String ABOUT_LINK = "https://developer.sailpoint.com/discuss/t/file-upload-utility/18181";

	/**
	 * Command Line Parameters
	 */

	@Option(names = { "-c", "--config-file" }, description = "Path to config JSON file.")
	private String configFile = "";

	@Option(names = { "-u", "--url" }, description = "SailPoint API Gateway (e.g. https://tenant.api.identitynow.com)")
	private String url = "";

	@Option(names = { "-i", "--clientId" }, description = "SailPoint Client ID (PAT)")
	private String clientId = "";

	@Option(names = { "-s",
			"--clientSecret" }, description = "SailPoint Client Secret (PAT)", arity = "0..1", interactive = true)
	private String clientSecret = "";

	@Option(names = { "-f", "--file" }, description = "File or directories for bulk aggregation.")
	private List<File> files = null;

	@Option(names = { "-d", "--disableOptimization" }, description = "Disable Optimization on Account Aggregation")
	private boolean disableOptimization = false;

	@Option(names = { "-o",
			"--objectType" }, description = "File Type; Account or Entitlement Schema. Default: Account")
	private String objectType = DEFAULT_ACCOUNT_AGGREGATION;

	@Option(names = { "-R", "--recursive" }, description = "Recursively search directories")
	private boolean recursive = false;

	@Option(names = { "-S", "--simulate" }, description = "Simulation Mode.  Scans for files but does not aggregate.")
	private boolean simulate = false;

	@Option(names = { "-t", "--timeout" }, description = "Timeout (in milliseconds). Default: 10000 (10s)")
	private Integer timeout = 10000; // Default is 10s

	@Option(names = { "-x",
			"--extension" }, description = "File extensions to search (for directories only).  Default: csv")
	private List<String> fileExtensions = Arrays.asList("csv");

	@Option(names = { "-v", "--verbose" }, description = "Verbose logging. Default: false")
	private boolean verbose = false;

	@Option(names = { "-H", "--proxyHost" }, description = "Proxy Host")
	private String proxyHost;

	@Option(names = { "-P", "--proxyPort" }, description = "Proxy Post")
	private int proxyPort = -1;

	@Option(names = { "-U", "--proxyUser" }, description = "Proxy User; Used for authenticated proxies")
	private String proxyUser = null;

	@Option(names = { "-W",
			"--proxyPassword" }, description = "Proxy Password; Used for authenticated proxies", arity = "0..1", interactive = true)
	private String proxyPassword = null;

	@Option(names = { "-V", "--version" }, versionHelp = true, description = "Displays the current version.")
	boolean versionRequested;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Display help.")
	boolean helpRequested;

	/**
	 * Variables for Execution
	 */
	private Logger logger;

	private Reporter reporter;

	private SailPointService sailPointService;

	private FileProcessorService fileProcessorService;

	public static final String DEFAULT_ACCOUNT_AGGREGATION = "account";

	public FileUploadUtility() {
		super();

//		this.logger = new Logger();
		this.reporter = new Reporter();
	}

	public static void main(String[] args) throws Exception {

		CommandLine commandLine = new CommandLine(new FileUploadUtility());

		try {

			commandLine.parseArgs(args);

			FileUploadUtility fileUploadUtility = commandLine.populateCommand(new FileUploadUtility(), args);

			if (fileUploadUtility.helpRequested || args == null || args.length == 0) {
				commandLine.usage(System.out);
				return;
			}

			if (fileUploadUtility.versionRequested) {
				commandLine.printVersionHelp(System.out);
				return;
			}

			System.exit(fileUploadUtility.call());

		} catch (UnsupportedClassVersionError ue) {

			System.out.println("Unsupported version of Java: Please upgrade to JDK 11 or higher.");
			System.exit(1);

		} catch (RuntimeException rte) {

			System.out.println(rte.getMessage());
			System.exit(1);

		} catch (Exception e) {

			if (args == null || args.length == 0)
				System.out.println("No command arguments given.  Please provide arguments as indicated by the usage.");
			else
				System.out.println("Invalid parameters given. Args [" + String.join(" ", args) + "].");

			commandLine.usage(System.out);
		}
	}

	@Override
	public Integer call() throws Exception {

		this.logger = Logger.getInstance(this.verbose);

		Config config = null;

		logger.info(
				"------------------------------------------------------------------------------------------------------------");
		logger.info(" SailPoint File Upload Utility");
		logger.info(
				"------------------------------------------------------------------------------------------------------------");
		logger.info(String.format("%1$-20s %2$-30s ", " Version:", ABOUT_VERSION));
		logger.info(String.format("%1$-20s %2$-30s ", " Date:", ABOUT_DATE));
		logger.info(String.format("%1$-20s %2$-30s ", " Docs:", ABOUT_LINK));
		logger.info(
				"------------------------------------------------------------------------------------------------------------");

		/**
		 * Load config file if it exists, if there are env's for the client Id and
		 * secret, these override the config file.
		 */
		if (!configFile.isEmpty()) {
			logger.debug(" --config-file specified, attempting to load.");
			config = ConfigUtils.ReadConfigFile(configFile);
			ConfigUtils.EncodeSecrets(config);
//			System.out.println(config.getTenant().getClientSecret());
			ConfigUtils.WriteConfigFile(configFile, config);

			// Set the variables from the config file
			this.clientId = config.getTenant().getClientId();
//			this.clientSecret = config.getTenant().getClientSecret();
			if (config.getTenant().getClientSecret().length() > 1
					&& !config.getTenant().getClientSecret().equalsIgnoreCase("env")) {
				this.clientSecret = ConfigUtils.DecodeConfigItem(config,
						config.getTenant().getClientSecret().substring(2));
			} else if (config.getTenant().getClientSecret().equalsIgnoreCase("env")) {
				this.clientSecret = config.getTenant().getClientSecret();
			}

//			System.out.println("DEC: " + this.clientSecret);
			this.url = config.getTenant().getUrl();

			if (config.getProxy().isEnabled()) {
				this.proxyHost = config.getProxy().getHost();
				this.proxyPort = config.getProxy().getPort();
				this.proxyUser = config.getProxy().getUser();
				if (config.getProxy().getPassword().length() > 1
						&& !config.getProxy().getPassword().equalsIgnoreCase("env")) {
					this.proxyPassword = ConfigUtils.DecodeConfigItem(config,
							config.getProxy().getPassword().substring(2));
				} else if (config.getProxy().getPassword().equalsIgnoreCase("env")) {
					this.clientSecret = config.getProxy().getPassword();
				}
			}
		}

		/*
		 * Perform some basic validations of the parameters provided. Picocli already
		 * does validation of required parameters.
		 */
		if (StringUtils.endsWithIgnoreCase(clientId, "env")) {
			logger.debug(" --clientId derived from $SAIL_CLIENT_ID");
			clientId = System.getenv("SAIL_CLIENT_ID");
		}

		if (StringUtils.endsWithIgnoreCase(clientSecret, "env")) {
			logger.debug(" --clientSecret derived from $SAIL_CLIENT_SECRET");
			clientSecret = System.getenv("SAIL_CLIENT_SECRET");

		}

		// Allow Proxy user and password to be set as environment variables for
		// consistency
		if (StringUtils.endsWithIgnoreCase(proxyUser, "env")) {
			logger.debug(" --proxyUser derived from $SAIL_PROXY_USER");
			clientId = System.getenv("SAIL_PROXY_USER");
		}

		if (StringUtils.endsWithIgnoreCase(proxyPassword, "env")) {
			logger.debug(" --proxyPassword derived from $SAIL_PROXY_PASS");
			clientSecret = System.getenv("SAIL_PROXY_PASS");

		}

		if (!StringUtils.startsWithIgnoreCase(url, "https://"))
			throw new RuntimeException("Usage: The provided --url parameter must begin with 'https://'");

		if (!SailPointUrl.isValid(url))
			throw new RuntimeException("Usage: The provided --url parameter must be a valid API URL: \n"
					+ SailPointUrl.getDisplayableUrls());

		/*
		 * Display configurations so that people can see how this is will run. Also,
		 * useful for troubleshooting. We do not want to display the client secret here
		 * for security reasons.
		 */
		logger.info(String.format("%1$-20s %2$-30s ", " URL:", url));
		logger.info(String.format("%1$-20s %2$-30s ", " Client ID:", clientId));
//		logger.info( String.format("%1$-20s %2$-30s ", " Files:", StringUtils.join( files, ", \n" ) ) );
		logger.info(String.format("%1$-20s %2$-30s ", " Verbose:", verbose));
		logger.info(
				"------------------------------------------------------------------------------------------------------------");

		Timer.start();

		/*
		 * Create a SailPoint Service which will do all the API calls.
		 */
		this.sailPointService = new SailPointService.Builder().url(url).clientId(clientId).clientSecret(clientSecret)
				.timeout(timeout).proxy(proxyHost, proxyPort).proxyAuthentication(proxyUser, proxyPassword).build();

		if (proxyHost != null && proxyPort != -1)
			logger.debug("Proxy enabled!  Initializing proxy with settings: proxyHost[" + proxyHost + "], proxyPort["
					+ proxyPort + "].");

		fileProcessorService = new FileProcessorService(config, sailPointService);

		/*
		 * Check to make sure credentials are valid before we process files.
		 */

		logger.info("Checking credentials...");

		try {

			sailPointService.createSession();

		} catch (Exception e) {
			logger.error("Error Logging into Identity Security Cloud.  Please check your credentials and try again. ["
					+ e.getMessage() + "]");
			System.exit(1);
		}

		if (config != null) {
			if (config.getAggregations().length > 0) {
				fileProcessorService.processAggregations(config, reporter);
			} else {
				logger.error("No aggregations to process, exiting...");
				return -1;
			}
		} else {
			ConfigAggregation configAggregation = new ConfigAggregation(disableOptimization, objectType, recursive,
					simulate, proxyPort, fileExtensions.toArray(new String[0]));

			fileProcessorService.processFiles(files, configAggregation, reporter);
		}

		logger.info("Complete.");

		logger.info(
				"------------------------------------------------------------------------------------------------------------");
		logger.info(String.format("%1$-20s %2$-30s ", " Elapsed time:", (Timer.secondsElapsed()) + " seconds"));
		logger.info(String.format("%1$-20s %2$-30s ", " Files processed:", reporter.countTotal()));
		logger.info(
				"------------------------------------------------------------------------------------------------------------");

		logger.info(String.format("%1$-20s %2$-30s ", " Success:", reporter.countSuccess()));
		for (String successFile : reporter.getSuccess())
			logger.debug("\t" + successFile);

		logger.info(
				"------------------------------------------------------------------------------------------------------------");

		logger.info(String.format("%1$-20s %2$-30s ", " Error:", reporter.countErrors()));
		for (String errorFile : reporter.getErrors())
			logger.debug("\t" + errorFile);

		logger.info(
				"------------------------------------------------------------------------------------------------------------");

		logger.info(String.format("%1$-20s %2$-30s ", " Skipped:", reporter.countSkips()));
		for (String skippedFile : reporter.getSkips())
			logger.debug("\t" + skippedFile);

		logger.info(
				"------------------------------------------------------------------------------------------------------------");

		return 0;
	}
}
