package com.sb.elsinore;

import Cosm.*;
import com.sb.common.CollectionsUtil;
import com.sb.elsinore.devices.I2CDevice;
import com.sb.elsinore.inputs.PhSensor;
import com.sb.elsinore.notificiations.Notifications;
import jGPIO.GPIO;
import jGPIO.InvalidGPIOException;
import org.apache.commons.cli.*;
import org.ini4j.ConfigParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.owfs.jowfsclient.Enums.OwPersistence;
import org.owfs.jowfsclient.OwfsConnection;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * LaunchControl is the core class of Elsinore. It reads the config file,
 * determining whether to run setup or not It launches the threads for the PIDs,
 * Temperature, and Mash It write the config file on shutdown. It sets up the
 * OWFS connection
 *
 * @author doug
 *
 */
@SuppressWarnings("unused")
public class LaunchControl {
    private static final String BREWERY_NAME = "brewery_name";
    private static final String PAGE_LOCK = "pagelock";
    private static final String SCALE = "scale";
    private static final String COSM_API_KEY = "cosm";
    private static final String COSM_FEED_ID = "cosm_feed";
    private static final String PACHUBE = "pachube";
    private static final String PACHUBE_FEED_ID = "pachube_feed";
    private static final String OWFS_SERVER = "owfs_server";
    private static final String OWFS_PORT = "owfs_port";
    public static final String RESTORE = "restore";

    /* MAGIC NUMBERS! */
    public static int EXIT_UPDATE = 128;
    public static boolean loadCompleted = false;
    /**
     * The default OWFS Port.
     */
    public static final int DEFAULT_OWFS_PORT = 4304;
    /**
     * The default port to serve on, can be overridden with -p <port>.
     */
    public static final int DEFAULT_PORT = 8080;
    public static final Object timerLock = new Object();
    public int server_port = 8080;
    /**
     * The Minimum number of volume data points.
     */
    public static final int MIN_VOLUME_SIZE = 3;
    public static final String RepoURL = "http://dougedey.github.io/SB_Elsinore_Server/";
    public static String baseUser = null;

    /**
     * List of PIDs.
     */
    public static final List<PID> pidList = new CopyOnWriteArrayList<>();
    /**
     * List of Temperature probes.
     */
    public static final List<Temp> tempList = new CopyOnWriteArrayList<>();
    /**
     * List of Switches.
     */
    public static final List<Switch> switchList = new CopyOnWriteArrayList<>();
    /**
     * List of Timers.
     */
    public static List<Timer> timerList = new CopyOnWriteArrayList<>();
    /**
     * List of MashControl profiles.
     */
    public static final List<TriggerControl> triggerControlList = new CopyOnWriteArrayList<>();
    /**
     * List of pH Sensors.
     */
    public static final CopyOnWriteArrayList<PhSensor> phSensorList = new CopyOnWriteArrayList<>();
    public static final HashMap<String, I2CDevice> i2cDeviceList = new HashMap<>();
    /**
     * Temperature Thread list.
     */
    public static List<Thread> tempThreads = new ArrayList<>();
    /**
     * PID Thread List.
     */
    public static List<Thread> pidThreads = new ArrayList<>();
    /**
     * Mash Threads list.
     */
    public static List<Thread> triggerThreads = new ArrayList<>();

    /**
     * ConfigParser, legacy for the older users that haven't converted.
     */
    public static ConfigParser configCfg = null;
    /**
     * Config Document, for the XML data.
     */
    public static Document configDoc = null;

    /**
     * Default config filename. Can be overridden with -c <filename>
     */
    public static String configFileName = "elsinore.cfg";

    /**
     * The BrewServer runner object that'll be used.
     */
    public static ServerRunner sRunner = null;

    public static StatusRecorder recorder = null;

    /* public fields to hold data for various functions */
    /**
     * COSM stuff, probably unused by most people.
     */
    public static Cosm cosm = null;
    /**
     * The actual feed that's in use by COSM.
     */
    public static Feed cosmFeed = null;
    /**
     * The list of available datastreams from COSM.
     */
    public static Datastream[] cosmStreams = null;

    /**
     * The Default scale to be used.
     */
    public static String scale = "F";

    /**
     * One Wire File System Connection.
     */
    public static OwfsConnection owfsConnection = null;
    /**
     * Flag whether the user has selected OWFS.
     */
    public static boolean useOWFS = false;
    /**
     * The Default OWFS server name.
     */
    public static String owfsServer = "localhost";
    /**
     * The OWFS port value.
     */
    public static Integer owfsPort = DEFAULT_OWFS_PORT;

    /**
     * The accepted startup options.
     */
    public static Options startupOptions = null;
    /**
     * The Command line used.
     */
    public static CommandLine startupCommand = null;

    /**
     * Xpath factory. This'll not change.
     */
    public static XPathFactory xPathfactory = XPathFactory.newInstance();

    /**
     * XPath static for the helper.
     */
    public static XPath xpath = xPathfactory.newXPath();
    /**
     * Xpath Expression static for the helpers.
     */
    public static XPathExpression expr = null;
    public static String message = "";
    public static double recorderDiff = .15d;
    public static boolean recorderEnabled = false;
    public static String recorderDirectory = StatusRecorder.defaultDirectory;
    public static String breweryName = null;
    public static String theme = "default";
    public static boolean pageLock = false;
    private static boolean initialized = false;
    private static ProcessBuilder pb = null;
    private static boolean m_restore = false;

    /*****
     * Main method to launch the brewery.
     *
     * @param arguments
     *            List of arguments from the command line
     */
    public static void main(final String... arguments) {
        BrewServer.LOG.info("Running Brewery Controller.");
        BrewServer.LOG.info("Currently at: " + getShaFor("HEAD"));
        int port = DEFAULT_PORT;

        // Allow for the root directory to be overridden by environment variable
        // or set on the command line with the -root option
        String rootDir = System.getenv("ELSINORE_ROOT");

        if (arguments.length > 0) {
            createOptions();
            CommandLineParser parser = new BasicParser();
            try {
                startupCommand = parser.parse(startupOptions, arguments);

                // Do we need to print the help?
                if (startupCommand.hasOption("help")) {
                    HelpFormatter hF = new HelpFormatter();
                    hF.printHelp("java -jar Elsinore.jar", startupOptions);
                    return;
                }

                // Check for a custom value
                if (startupCommand.hasOption("config")) {
                    configFileName = startupCommand.getOptionValue("config");
                }

                if (startupCommand.hasOption("gpiodefinitions")) {
                    BrewServer.LOG.info("Setting property file to: "
                            + startupCommand.getOptionValue("gpiodefinitions"));
                    System.setProperty("gpiodefinitions",
                            startupCommand.getOptionValue("gpiodefinitions"));
                }

                if (startupCommand.hasOption("port")) {
                    try {
                        port = Integer.parseInt(startupCommand
                                .getOptionValue("port"));
                    } catch (NumberFormatException e) {
                        BrewServer.LOG
                                .warning("Couldn't parse port value as an integer: "
                                        + startupCommand.getOptionValue("port"));
                        System.exit(-1);
                    }
                }

                if (startupCommand.hasOption("d")) {
                    System.setProperty("debug", "INFO");
                }

                if (startupCommand.hasOption("root")) {
                    rootDir = startupCommand.getOptionValue("root");
                }

                if (startupCommand.hasOption("rthreshold")) {
                    recorderDiff = Double.parseDouble(startupCommand
                            .getOptionValue("rthreshold"));
                }

                if (startupCommand.hasOption("baseUser")) {
                    baseUser = startupCommand.getOptionValue("baseUser");
                }

                if (startupCommand.hasOption("t")) {
                    theme = startupCommand.getOptionValue("t");
                }

                if (startupCommand.hasOption("r")) {
                    if (startupCommand.getOptionValue("r").equalsIgnoreCase(
                            "false")) {
                        recorderEnabled = false;
                    }
                }

                if (startupCommand.hasOption("rdirectory")) {
                    recorderDirectory = startupCommand
                            .getOptionValue("rdirectory");
                    if (!recorderDirectory.endsWith("/")) {
                        recorderDirectory += "/";
                    }
                }

            } catch (ParseException e) {
                BrewServer.LOG.info("Error when parsing the command line");
                e.printStackTrace();
                return;
            }
        }

        if (rootDir != null) {
            // Validate to make sure it's valid, otherwise things will go badly.
            File root = new File(rootDir);
            if (root.exists() && root.isDirectory()) {
                System.setProperty("root_override", rootDir);
            } else {
                BrewServer.LOG.warning("Invalid root directory provided: "
                        + rootDir);
                System.exit(-1);
            }
        }

        LaunchControl lc = new LaunchControl(port);
        BrewServer.LOG.warning("Started LaunchControl: " + lc.toString());
    }

    /*******
     * Used to setup the options for the command line parser.
     */
    public static void createOptions() {
        startupOptions = new Options();

        startupOptions.addOption("h", "help", false, "Show this help");

        startupOptions.addOption("d", "debug", false, "Enable debug output");

        startupOptions.addOption("o", "owfs", false,
                "Setup OWFS connection for configuration on startup");
        startupOptions.addOption("c", "config", true,
                "Specify the name of the configuration file");
        startupOptions.addOption("p", "port", true,
                "Specify the port to run the webserver on");

        startupOptions.addOption("g", "gpiodefinitions", true,
                "specify the GPIO Definitions file if"
                        + " you're on Kernel 3.8 or above");
        startupOptions.addOption("rthreshold", true,
                "specify the amount for a reading to change before "
                        + "recording the value in history");
        startupOptions.addOption("root", true,
                "specify the root directory for elsinore.  This is the location "
                        + "configuration and html files should live.");
        startupOptions.addOption("baseUser", true,
                "Specify the user who should own all the files created");
        startupOptions.addOption("t", "theme", true, "Specify the theme name");
        startupOptions.addOption("r", StatusRecorder.RECORDER_ENABLED, true,
                "Enable or disable the status recorder. Default enabled.");
        startupOptions.addOption("rdirectory",
                StatusRecorder.DIRECTORY_PROPERTY, true,
                "Set the recorder directory output, default: graph-data/");
    }

    /**
     * Constructor for launch control.
     *
     * @param port
     *            The port to start the server on.
     */
    public LaunchControl(final int port) {
        this.server_port = port;
        // Create the shutdown hooks for all the threads
        // to make sure we close off the GPIO connections
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                saveEverything();
                // Close off all the Switch GPIOs properly.
            }
        });

        /**
         * Check to make sure we have a valid folder for one wire straight away.
         */
        File w1Folder = new File("/sys/bus/w1/devices/");
        if (!w1Folder.exists()) {
            BrewServer.LOG.info("Couldn't read the one wire devices directory!");
            BrewServer.LOG.info("Did you set up One Wire?");
            System.out
                    .println("http://dougedey.github.io/2014/11/12/Setting_Up_One_Wire/");
            System.exit(-1);
        }

        // See if we have an active configuration file
        readConfig();

        // Debug info before launching the BrewServer itself
        LaunchControl.loadCompleted = true;
        BrewServer.LOG.log(Level.INFO, "CONFIG READ COMPLETED***********");
        sRunner = new ServerRunner(BrewServer.class, this.server_port);
        Thread sRunnerThread = new Thread(sRunner);
        sRunnerThread.run();
        sRunnerThread.setDaemon(false);
        try {
            sRunnerThread.join();
        } catch (InterruptedException ie) {
            BrewServer.LOG.warning("Shutdown initiated.");
            ie.printStackTrace();
        }
    }

    public static boolean isInitialized() {
        return loadCompleted;
    }

    public static void setInitialized(boolean initialized) {
        LaunchControl.initialized = initialized;
    }

    public static void setRestore(boolean restore) {
        LaunchControl.m_restore = restore;
    }

    /************
     * Start the COSM Connection.
     *
     * @param apiKey
     *            User API Key from COSM
     * @param feedID
     *            The FeedID to get
     */
    public void startCosm(final String apiKey, final int feedID) {
        BrewServer.LOG.info("API: " + apiKey + " Feed: " + feedID);
        cosm = new Cosm(apiKey);

        // get the data feed
        try {
            cosmFeed = cosm.getFeed(feedID, true);
            BrewServer.LOG.info("Got " + cosmFeed.getTitle());
        } catch (CosmException e) {
            BrewServer.LOG.warning("Couldn't get the feed: " + e.getMessage());
            return;
        }

        // get the list of feeds
        cosmStreams = cosmFeed.getDatastreams();
    }

    /*****
     * Create an image from a COSM Feed.
     *
     * @param startDate
     *            The start date to get the image from
     * @param endDate
     *            The end date to get the image from
     * @return A string indicating the image status
     */
    public String getCosmImages(final String startDate, final String endDate) {
        try {
            if (cosmFeed != null) {
                for (Temp t : tempList) {
                    Datastream tData = findDatastream(t.getName());
                    if (tData != null) {
                        if (startDate == null || endDate == null) {
                            cosm.getDatastreamImage(cosmFeed.getId(),
                                    t.getName());
                        } else {
                            cosm.getDatastreamImage(cosmFeed.getId(),
                                    t.getName(), startDate, endDate);
                        }
                    }
                }
            }

        } catch (CosmException e) {
            return "Could not get the images";
        }

        return "Grabbed images";
    }

    /*********
     * Get the XML from a COSM feed.
     *
     * @param startDate
     *            The startdate to get data from
     * @param endDate
     *            The endDate to get data to.
     * @return Returns the XML for the COSM Feed
     */
    public String getCosmXML(final String startDate, final String endDate) {
        try {
            if (cosmFeed != null) {
                for (Temp t : tempList) {
                    Datastream tData = findDatastream(t.getName());
                    if (tData != null) {
                        if (startDate == null || endDate == null) {
                            cosm.getDatastreamXML(cosmFeed.getId(),
                                    t.getName());
                        } else {
                            cosm.getDatastreamXML(cosmFeed.getId(),
                                    t.getName(), startDate, endDate);
                        }
                    }
                }
            }

        } catch (CosmException e) {
            return "Could not get the images";
        }

        return "Grabbed images";
    }

    /******
     * Get the JSON Output String. This is the current Status of the PIDs,
     * Temps, Switches, etc...
     *
     * @return The JSON String of the current status.
     */
    @SuppressWarnings("unchecked")
    public static String getJSONStatus() {

        // get each setting add it to the JSON
        JSONObject rObj = new JSONObject();
        JSONObject tJSON;
        JSONObject triggerJSON = new JSONObject();
        rObj.put("locked", LaunchControl.pageLock);
        rObj.put("breweryName", LaunchControl.getName());

        // iterate the thread lists
        // use the temp list to determine if we have a PID to go with
        JSONArray vesselJSON = new JSONArray();
        for (Temp t : tempList) {

            /* Check for a PID */
            PID tPid = findPID(t.getName());
            tJSON = new JSONObject();

            // Add the temp to the JSON Map
            JSONObject tJSONTemp = new JSONObject();
            tJSONTemp.putAll(t.getMapStatus());
            tJSON.put("name", t.getName().replaceAll(" ", "_"));
            tJSON.put("deviceaddr", t.getProbe());
            tJSON.put("tempprobe", tJSONTemp);

            if (t.hasVolume()) {
                JSONObject volumeJSON = new JSONObject();
                volumeJSON.put("volume", t.getVolume());
                volumeJSON.put("units", t.getVolumeUnit());
                if (!t.getVolumeAIN().equals("")) {
                    volumeJSON.put("ain", t.getVolumeAIN());
                } else {
                    volumeJSON.put("address", t.getVolumeAddress());
                    volumeJSON.put("offset", t.getVolumeOffset());
                }
                volumeJSON.put("gravity", t.getGravity());

                tJSON.put("volume", volumeJSON);
            }

            if (tPid != null) {
                JSONObject tJSONPID = new JSONObject();
                tJSONPID.putAll(tPid.getMapStatus());
                tJSON.put("pidstatus", tJSONPID);
            }

            // Add the JSON object with the PID Name
            vesselJSON.add(tJSON);

            // update COSM
            if (cosmFeed != null) {
                Datastream tData = findDatastream(t.getName());
                if (tData != null) {
                    tData.setCurrentValue(t.getTemp().toString());
                    Unit tUnit = new Unit();
                    tUnit.setType("temp");
                    tUnit.setSymbol(t.getScale());
                    tUnit.setLabel("temperature");
                    tData.setUnit(tUnit);
                    try {
                        cosm.updateDatastream(cosmFeed.getId(), t.getName(),
                                tData);
                    } catch (CosmException e) {
                        BrewServer.LOG.info("Failed to update datastream: "
                                + e.getMessage());
                    }
                }

            }

            if (t.getTriggerControl() != null
                    && t.getTriggerControl().triggerCount() > 0) {
                triggerJSON.put(t.getProbe(),
                        t.getTriggerControl().getJSONData());
            }
        }
        rObj.put("vessels", vesselJSON);
        rObj.put("triggers", triggerJSON);

        if (timerList.size() > 0)
        {
            JSONObject timers = new JSONObject();
            for (Timer timer: timerList)
            {
                timers.put(timer.getName(), timer.getStatus());
            }
            rObj.put("timers", timers);
        }

        // generate the list of switchess
        if (switchList != null && switchList.size() > 0) {
            tJSON = new JSONObject();

            for (Switch p : switchList) {
                tJSON.put(p.getName().replaceAll(" ", "_"), p.getStatus());
            }

            rObj.put("switches", tJSON);
        }

        if (LaunchControl.getMessage() != null) {
            rObj.put("message", LaunchControl.getMessage());
        }

        rObj.put("language", Locale.getDefault().toString());

        // Add some recipe info.
        if (BrewServer.getCurrentRecipe() != null) {
            rObj.put("recipe", BrewServer.getCurrentRecipe().getName());
        }

        if (BrewServer.getRecipeList() != null && BrewServer.getRecipeList().size() > 0) {
            rObj.put("recipeCount", BrewServer.getRecipeList().size());
        }

        // Add the notifications information
        rObj.put("notifications", Notifications.getInstance().getNotificationStatus());
        return rObj.toString();
    }

    /**
     * Get the system status.
     *
     * @return a JSON Object representing the current system status
     */
    public static String getSystemStatus() {
        JSONObject retVal = new JSONObject();
        retVal.put(StatusRecorder.RECORDER, LaunchControl.recorder != null);
        retVal.put(StatusRecorder.RECORDER_TIME, StatusRecorder.SLEEP);
        retVal.put(StatusRecorder.RECORDER_DIFF, StatusRecorder.THRESHOLD);
        retVal.put(LaunchControl.RESTORE, LaunchControl.m_restore);
        return retVal.toJSONString();
    }

    /**
     * Read the configuration file.
     */
    public void readConfig() {

        LaunchControl.loadCompleted = false;
        // read the config file from the rpibrew.cfg file
        if (configCfg == null) {
            initializeConfig();
        }

        if (configCfg == null) {
            BrewServer.LOG.info("CFG IS NULL");
        }
        if (configDoc == null) {
            BrewServer.LOG.info("DOC IS NULL");
        }

        if (configDoc == null && configCfg == null) {
            createConfig();
            // Startup the brewery with the basic temp list
            return;
        }

        if (configDoc != null) {
            parseXMLSections();
        } else {
            BrewServer.LOG.info("Couldn't get a configuration file!");
            System.exit(1);
        }

        if (tempList.size() == 0) {
            // get user input
            createConfig();
        }
        updateDeviceList();
        LaunchControl.loadCompleted = true;
    }

    /**
     * Parse the general section of the XML Configuration.
     *
     * @param config
     *            The General element to be parsed.
     */
    public void parseGeneral(final Element config) {
        if (config == null) {
            return;
        }

        try {

            breweryName = getTextForElement(config, LaunchControl.BREWERY_NAME, null);
            pageLock = Boolean.parseBoolean(getTextForElement(config, LaunchControl.PAGE_LOCK, "false"));

            scale = getTextForElement(config, LaunchControl.SCALE, "F");

            if (Boolean.parseBoolean(getTextForElement(config, StatusRecorder.RECORDER, "false"))) {
                LaunchControl.enableRecorder();
            } else {
                LaunchControl.disableRecorder();
            }

            StatusRecorder.THRESHOLD = Double.parseDouble(getTextForElement(config, StatusRecorder.RECORDER_DIFF, "0.15"));
            StatusRecorder.SLEEP = Long.parseLong(getTextForElement(config, StatusRecorder.RECORDER_TIME, "5000"));

            String cosmAPIKey = null;
            Integer cosmFeedID;

            // Check for the COSM Feed details
            cosmAPIKey = getTextForElement(config, LaunchControl.COSM_API_KEY, null);

            cosmFeedID = Integer.parseInt(getTextForElement(config, LaunchControl.COSM_FEED_ID, "0"));
            
            // Try PACHube if the Cosm fields don't exist
            if (cosmAPIKey == null)
            {
                cosmAPIKey = getTextForElement(config, LaunchControl.PACHUBE, null);
            }

            if (cosmFeedID == 0) {
                try {
                    cosmFeedID = Integer.parseInt(getTextForElement(config, LaunchControl.PACHUBE_FEED_ID, "0"));
                } catch (NumberFormatException | NullPointerException e) {
                    cosmFeedID = null;
                }

            }

            if (cosmAPIKey != null && cosmFeedID != null) {
                startCosm(cosmAPIKey, cosmFeedID);
            }

            // Check for an OWFS configuration
            try {
                owfsServer = getTextForElement(config, LaunchControl.OWFS_SERVER, null);
                owfsPort = Integer.parseInt(getTextForElement(config, LaunchControl.OWFS_PORT, null));
            } catch (NullPointerException | NumberFormatException e) {
                owfsServer = null;
                owfsPort = null;
            }

            if (owfsServer != null) {
                BrewServer.LOG.log(Level.INFO, "Setup OWFS at " + owfsServer
                        + ":" + owfsPort);

                setupOWFS();
            }

            // Check for a system temperature
            if (getFirstElement(config, "System") != null) {
                addSystemTemp();
            }

            m_restore = Boolean.parseBoolean(getTextForElement(config, LaunchControl.RESTORE, "false"));
        } catch (NumberFormatException nfe) {
            System.out.print("Number format problem!");
            nfe.printStackTrace();
        }
    }

    /**
     * Parse the switches in the specified element.
     *
     * @param config
     *            The element that contains the switches information
     */
    public void parseSwitches(final Element config) {
        if (config == null) {
            return;
        }

        NodeList switches = config.getChildNodes();

        for (int i = 0; i < switches.getLength(); i++) {
            Element curSwitch = (Element) switches.item(i);
            String switchName = curSwitch.getAttribute("name").replace("_", " ");
            String gpio;
            if (curSwitch.hasAttribute(Switch.GPIO)) {
                gpio = curSwitch.getAttribute(Switch.GPIO);
            } else {
                gpio = curSwitch.getTextContent();
            }
            int position = -1;

            String tempString = curSwitch.getAttribute(Switch.POSITION);
            if (tempString != null) {
                try {
                    position = Integer.parseInt(tempString);
                } catch (NumberFormatException nfe) {
                    BrewServer.LOG.warning("Couldn't parse switch: " + switchName
                            + " position: " + tempString);
                }
            }
            try {
                Switch tSwitch = new Switch(switchName, gpio);
                tSwitch.setPosition(position);
                switchList.add(tSwitch);
            } catch (InvalidGPIOException e) {
                BrewServer.LOG.warning("Invalid GPIO (" + gpio
                        + ") detected for switch " + switchName);
                BrewServer.LOG.warning(
                        "Please fix the config file before running");
                System.exit(-1);
            }

            Element invert = getFirstElement(curSwitch, "invert");
            if (invert != null) {
                LaunchControl.findSwitch(switchName).setInverted(
                        Boolean.parseBoolean(invert.getTextContent()));
            }
        }

    }

    /**
     * Parse the list of timers in an XML Element.
     *
     * @param config
     *            the Element containing the timers
     */
    public void parseTimers(final Element config) {
        if (config == null) {
            return;
        }
        NodeList timers = config.getChildNodes();
        timerList = new ArrayList<>();

        for (int i = 0; i < timers.getLength(); i++) {
            Element tElement = (Element) timers.item(i);
            Timer temp = new Timer(tElement.getAttribute(Timer.ID));
            if (tElement.getAttribute(Timer.POSITION) != null) {
                try {
                    temp.setPosition(Integer.parseInt(tElement
                            .getAttribute(Timer.POSITION)));
                } catch (NumberFormatException nfe) {
                    // Couldn't parse. Move on.
                }
            }
            String target = tElement.getAttribute(Timer.TARGET);
            if (target != null && !target.equals("")) {
                try {
                    temp.setTarget(target);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }
            timerList.add(temp);
        }
    }

    /**
     * Parse the list of phSensors in an XML Element.
     *
     * @param config
     *            the Element containing the ph Sensors
     */
    public void parsePhSensors(final Element config) {
        if (config == null) {
            return;
        }
        NodeList sensors = config.getChildNodes();

        for (int i = 0; i < sensors.getLength(); i++) {
            Element tElement = (Element) sensors.item(i);
            PhSensor temp = new PhSensor();
            temp.setName(tElement.getNodeName().replace("_", " "));
            temp.setDsAddress(tElement.getAttribute(PhSensor.DS_ADDRESS));
            temp.setDsOffset(tElement.getAttribute(PhSensor.DS_OFFSET));
            temp.setAinPin(tElement.getAttribute(PhSensor.AIN_PIN));
            temp.setOffset(tElement.getAttribute(PhSensor.OFFSET));
            temp.setModel(tElement.getAttribute(PhSensor.MODEL));

            BrewServer.LOG.info("Checking for an I2CDevice");
            Element i2cElement = getFirstElement(tElement, I2CDevice.I2C_NODE);
            if (i2cElement != null)
            {
                temp.i2cDevice = getI2CDevice(i2cElement);
                String channel = getTextForElement(tElement, I2CDevice.DEV_CHANNEL, null);
                temp.i2cChannel = Integer.parseInt(channel);
            }

            phSensorList.add(temp);
        }
    }

    public static I2CDevice getI2CDevice(Element i2cElement) {
        String devNumber = getTextForElement(i2cElement, I2CDevice.DEV_NUMBER, null);
        String devAddress = getTextForElement(i2cElement, I2CDevice.DEV_ADDRESS, null);
        String devType = getTextForElement(i2cElement, I2CDevice.DEV_TYPE, null);
        return getI2CDevice(devNumber, devAddress, devType);
    }

    public static I2CDevice getI2CDevice(String devNumber, String devAddress, String devType)
    {
        String devKey = String.format("%s_%s", devNumber, devAddress);
        I2CDevice i2CDevice = i2cDeviceList.get(devKey);
        if (i2CDevice == null)
        {
            i2CDevice = I2CDevice.create(devNumber, devAddress, devType);
        }

        return  i2CDevice;
    }

    /**
     * Add a new switch to the server.
     *
     * @param name
     *            The name of the switch to add.
     * @param gpio
     *            The GPIO to add
     * @return True if added OK
     */
    public static Switch addSwitch(final String name, final String gpio) {
        if (name.equals("") || gpio.equals("")) {
            return null;
        }
        Switch p = LaunchControl.findSwitch(name);
        if (p == null) {
            try {
                p = new Switch(name, gpio);
                switchList.add(p);
            } catch (Exception g) {
                BrewServer.LOG.warning("Could not add switch: " + g.getMessage());
                g.printStackTrace();
            }
        }
        else
        {
            try {
                p.setGPIO(gpio);
            }
            catch (InvalidGPIOException g)
            {
                BrewServer.LOG.warning("Could not add switch: " + g.getMessage());
                g.printStackTrace();
            }
        }
        return p;
    }

    // Add the system temperature
    public static void addSystemTemp() {
        Temp tTemp = new Temp("System", "System");
        tempList.add(tTemp);
        BrewServer.LOG.info("Adding " + tTemp.getName());
        // setup the scale for each temp probe
        tTemp.setScale(scale);
        // setup the threads
        Thread tThread = new Thread(tTemp);
        tThread.setName("Temp_system");
        tempThreads.add(tThread);
        tThread.start();
    }

    public static void delSystemTemp() {
        Temp tTemp = LaunchControl.findTemp("System");
        // Do we have anything to delete?
        if (tTemp != null) {
            tTemp.shutdown();
            tempList.remove(tTemp);
        }
    }

    /**
     * Check to see if a switch with the given name exists.
     *
     * @param name
     *            The name of the switch to check
     * @return True if the switch exists.
     */
    public static boolean switchExists(final String name) {
        for (Switch p : switchList) {
            if (p.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new timer to the list.
     *
     * @param name
     *            The name of the timer.
     * @param target
     *            The target timer of the timer.
     * @return True if it was added OK.
     */
    public static boolean addTimer(String name, String target) {
        // Mode is a placeholder for now
        if (LaunchControl.findTimer(name) != null) {
            return false;
        }
        Timer tTimer = new Timer(name);
        tTimer.setTarget(target);
        CollectionsUtil.addInOrder(timerList, tTimer);

        return true;
    }

    /**
     * Startup a PID device.
     *
     * @param input
     *            The name of the PID.
     * @param probe
     *            The One-Wire probe address
     * @param heatgpio
     *            The heat GPIO to use.
     * @param coolgpio
     *            The Cool GPIO to use.
     * @return The new Temp probe. Use it to look up the PID if applicable.
     */
    public Temp startDevice(String input, String probe, String heatgpio, String coolgpio) {

        // Startup the thread
        if (probe == null || probe.equals("0")) {
            BrewServer.LOG.info("No Probe specified for " + input);
            probe = input;
        }

        if (!probe.startsWith("28") && !probe.startsWith("10") && !input.equals("System")) {
            BrewServer.LOG.warning(probe + " is not a temperature probe");
            return null;
        }

        // input is the name we'll use from here on out
        Temp tTemp = new Temp(input, probe);
        tempList.add(tTemp);
        BrewServer.LOG.info("Adding " + tTemp.getName() + " Heat GPIO is (" + heatgpio
                + ")");
        BrewServer.LOG.info("Adding " + tTemp.getName() + " Cool GPIO is (" + coolgpio
                + ")");

        // setup the scale for each temp probe
        tTemp.setScale(scale);

        // setup the threads
        Thread tThread = new Thread(tTemp);
        tThread.setName("Temp_" + tTemp.getName());
        tempThreads.add(tThread);
        tThread.start();

        if ((heatgpio != null && !heatgpio.equals("")) || (coolgpio != null && !coolgpio.equals(""))) {

            PID tPID = new PID(tTemp, input, heatgpio);
            tPID.setCoolGPIO(coolgpio);
            pidList.add(tPID);
            Thread pThread = new Thread(tPID);
            pThread.setName("PID_" + tTemp.getName());
            pidThreads.add(pThread);
            pThread.start();
        }

        return tTemp;
    }

    /******
     * Search for a Datastream in cosm based on a tag.
     *
     * @param tag
     *            The tag to search for
     * @return The found Datastream, or null if it doesn't find anything
     */
    public static Datastream findDatastream(final String tag) {
        // iterate the list by tag
        List<Datastream> cList = Arrays.asList(cosmStreams);
        Iterator<Datastream> iterator = cList.iterator();
        Datastream tData;
        while (iterator.hasNext()) {
            // Iteratre the datastreams
            tData = iterator.next();

            if (tData.getId().equalsIgnoreCase(tag)) {
                return tData;
            }
        }

        // couldn't find the tag, lets add it
        tData = new Datastream();
        // always setup for Fahrenheit

        BrewServer.LOG.info("Creating new feed");
        List<String> lTags = new ArrayList<>();

        lTags.add("Elsinore");
        lTags.add("temperature");

        String[] aTags = new String[lTags.size()];
        lTags.toArray(aTags);

        tData.setTags(aTags);
        tData.setId(tag);
        try {
            cosm.createDatastream(cosmFeed.getId(), tData);
        } catch (CosmException e) {
            BrewServer.LOG.info("Failed to create stream:" + e.getMessage()
                    + " - " + cosmFeed.getId());

            return null;
        }
        return tData;
    }

    /******
     * Find the Temp Probe in the current list.
     *
     * @param name
     *            The Temp to find
     * @return The Temp object
     */
    public static Temp findTemp(final String name) {
        // search based on the input name
        Iterator<Temp> iterator = tempList.iterator();
        Temp tTemp;
        while (iterator.hasNext()) {
            tTemp = iterator.next();
            if (tTemp.getName().equalsIgnoreCase(name) || tTemp.getProbe().equalsIgnoreCase(name)) {
                return tTemp;
            }
        }
        return null;
    }

    /******
     * Find the PID in the current list.
     *
     * @param name
     *            The PID to find
     * @return The PID object
     */
    public static PID findPID(final String name) {
        // search based on the input name
        Iterator<PID> iterator = pidList.iterator();
        PID tPid;
        while (iterator.hasNext()) {
            tPid = iterator.next();
            if (tPid.getName().equalsIgnoreCase(name) || tPid.getTempProbe().getProbe().equalsIgnoreCase(name)) {
                return tPid;
            }
        }
        return null;
    }

    /**
     * Add a PID to the list.
     *
     * @param newPID
     *            PID to add.
     */
    public static void addPID(final PID newPID) {
        pidList.add(newPID);
        Thread pThread = new Thread(newPID);
        pThread.start();
        pidThreads.add(pThread);
    }

    /**************
     * Find the Switch in the server..
     *
     * @param name
     *            The switch to find
     * @return return the Switch object
     */
    public static Switch findSwitch(final String name) {
        // search based on the input name
        Iterator<Switch> iterator = switchList.iterator();
        Switch tSwitch;
        while (iterator.hasNext()) {
            tSwitch = iterator.next();
            if (tSwitch.getName().equalsIgnoreCase(name)
                    || tSwitch.getNodeName().equalsIgnoreCase(name)
                    || tSwitch.getName().equalsIgnoreCase(name.replace("_", " "))) {
                return tSwitch;
            }
        }
        return null;
    }

    /**
     * Delete the specified switch.
     *
     * @param name
     *            The switch to delete.
     */
    public static void deleteSwitch(final String name) {
        // search based on the input name
        Switch tSwitch = LaunchControl.findSwitch(name);
        LaunchControl.switchList.remove(tSwitch);
    }

    /**************
     * Find the Timer in the current list.
     *
     * @param name
     *            The timer to find
     * @return return the Timer object
     */
    public static Timer findTimer(final String name) {
        // search based on the input name
        Iterator<Timer> iterator = timerList.iterator();
        Timer tTimer;
        while (iterator.hasNext()) {
            tTimer = iterator.next();
            if (tTimer.getName().equalsIgnoreCase(name)
                    || tTimer.getName().equalsIgnoreCase(name.replace("_", " "))) {
                return tTimer;
            }
        }
        return null;
    }

    /**************
     * Delete the Timer in the current list.
     *
     * @param name
     *            The timer to delete
     */
    public static void deleteTimer(final String name) {
        // search based on the input name
        Timer tTimer = LaunchControl.findTimer(name);
        timerList.remove(tTimer);
    }

    /******
     * List the One Wire devices from the standard one wire file system. in
     * /sys/bus/w1/devices, basic access
     */
    public static void listOneWireSys() {
        listOneWireSys(true);
    }

    /**
     * List the one wire devices in /sys/bus/w1/devices.
     *
     * @param prompt
     *            Prompt to select OWFS if needed
     */
    public static void listOneWireSys(final boolean prompt) {
        // try to access the list of 1-wire devices
        File w1Folder = new File("/sys/bus/w1/devices/");
        if (!w1Folder.exists()) {
            BrewServer.LOG.warning("Couldn't read the one wire devices directory!");
            BrewServer.LOG.warning("Did you set up One Wire?");
            BrewServer.LOG.warning("http://dougedey.github.io/2014/11/12/Setting_Up_One_Wire/");
            System.exit(-1);
        }
        File[] listOfFiles = w1Folder.listFiles();

        assert listOfFiles != null;
        if (listOfFiles.length == 0) {
            BrewServer.LOG.warning("No 1Wire probes found! Please check your system!");
            BrewServer.LOG.warning("http://dougedey.github.io/2014/11/24/Why_Cant_I_Use_Elsinore_Without_Temperature_Probes/");
            System.exit(-1);
        }

        // Display what we found
        for (File currentFile : listOfFiles) {
            if (currentFile.isDirectory()
                    && !currentFile.getName().startsWith("w1_bus_master")) {

                // Check to see if theres a non temp probe (DS18x20)
                if (!currentFile.getName().startsWith("28") && !currentFile.getName().startsWith("10")) {
                    if (!useOWFS && prompt) {
                        System.out.println("Detected a non temp probe: "
                                + currentFile.getName() + "\n"
                                + "Do you want to setup OWFS? [y/N]");
                        String t = readInput();
                        if (t.toLowerCase().startsWith("y")) {
                            if (owfsConnection == null) {
                                createOWFS();
                            }

                            useOWFS = true;
                        }
                    }
                    // Skip this iteration
                    continue;
                }

                // Check to see if this probe exists
                if (probeExists(currentFile.getName())) {
                    continue;
                }

                BrewServer.LOG.info("Checking for " + currentFile.getName());
                Temp currentTemp = new Temp(currentFile.getName(),
                        currentFile.getName());
                tempList.add(currentTemp);
                // setup the scale for each temp probe
                currentTemp.setScale(scale);
                // setup the threads
                Thread tThread = new Thread(currentTemp);
                tThread.setName("Temp_" + currentTemp.getName());
                tempThreads.add(tThread);
                tThread.start();
            }
        }
    }

    /**
     * update the device list.
     */
    public static void updateDeviceList() {
        updateDeviceList(true);
    }

    /**
     * Update the device list.
     *
     * @param prompt
     *            Prompt for OWFS usage.
     */
    public static void updateDeviceList(boolean prompt) {
        listOneWireSys(prompt);
    }

    /**********
     * Setup the configuration. We get here if the configuration file doesn't
     * exist.
     */
    public void createConfig() {

        if (startupCommand != null && startupCommand.hasOption("owfs")) {
            createOWFS();
        }

        updateDeviceList();

        if (tempList.size() == 0) {
            BrewServer.LOG.warning("Could not find any one wire devices\n"
                    + "Please check you have the correct modules setup");
            BrewServer.LOG.warning("http://dougedey.github.io/2014/11/24/Why_Cant_I_Use_Elsinore_Without_Temperature_Probes/");
            System.exit(0);
        }

        if (configDoc == null) {
            initializeConfig();
        }

        displaySensors();
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
            String webURL = "http://" + addr.getHostName() + ":"
                    + this.server_port + "/controller";
            BrewServer.LOG.warning("Please go to the Web UI to the web UI "
                    + webURL);
        } catch (UnknownHostException e) {
            BrewServer.LOG.warning("Couldn't get localhost information.");
        }
    }

    /**
     * Save the configuration file to the default config filename as xml.
     */
    public static void saveConfigFile() {
        if (!LaunchControl.loadCompleted) {
            return;
        }
        File configOut = new File(configFileName);
        LaunchControl.setFileOwner(configOut);

        Element generalElement = getFirstElement(null, "general");
        if (generalElement == null) {
            generalElement = addNewElement(null, "general");
        }

        Element tempElement;

        tempElement = getFirstElement(generalElement, "pagelock");

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, "pagelock");
        }

        tempElement.setTextContent(Boolean.toString(pageLock));

        tempElement = getFirstElement(generalElement, LaunchControl.SCALE);

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, LaunchControl.SCALE);
        }

        tempElement.setTextContent(scale);

        tempElement = getFirstElement(generalElement, StatusRecorder.RECORDER);

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, StatusRecorder.RECORDER);
        }

        tempElement.setTextContent(Boolean.toString(LaunchControl.recorder != null));

        tempElement = getFirstElement(generalElement, StatusRecorder.RECORDER_DIFF);

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, StatusRecorder.RECORDER_DIFF);
        }

        tempElement.setTextContent(Double.toString(StatusRecorder.THRESHOLD));

        tempElement = getFirstElement(generalElement, StatusRecorder.RECORDER_TIME);

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, StatusRecorder.RECORDER_TIME);
        }

        tempElement.setTextContent(Long.toString(StatusRecorder.SLEEP));

        if (breweryName != null && !breweryName.equals("")) {
            tempElement = getFirstElement(generalElement, LaunchControl.BREWERY_NAME);

            if (tempElement == null) {
                tempElement = addNewElement(generalElement, LaunchControl.BREWERY_NAME);
            }

            tempElement.setTextContent(breweryName);
        }

        if (useOWFS) {
            if (owfsServer != null) {
                tempElement = getFirstElement(generalElement, LaunchControl.OWFS_SERVER);

                if (tempElement == null) {
                    tempElement = addNewElement(generalElement, LaunchControl.OWFS_SERVER);
                }

                tempElement.setTextContent(owfsServer);
            }

            if (owfsPort != null) {
                tempElement = getFirstElement(generalElement, LaunchControl.OWFS_PORT);

                if (tempElement == null) {
                    tempElement = addNewElement(generalElement, LaunchControl.OWFS_PORT);
                }

                tempElement.setTextContent(Integer.toString(owfsPort));
            }
        }

        tempElement = getFirstElement(generalElement, LaunchControl.RESTORE);

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, LaunchControl.RESTORE);
        }

        tempElement.setTextContent(Boolean.toString(LaunchControl.m_restore));

        try {
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(configDoc);

            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList nl = (NodeList) xp.evaluate(
                    "//text()[normalize-space(.)='']", configDoc,
                    XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); ++i) {
                Node node = nl.item(i);
                node.getParentNode().removeChild(node);
            }

            StreamResult configResult = new StreamResult(configOut);
            transformer.transform(source, configResult);

        } catch (TransformerConfigurationException e) {
            BrewServer.LOG.warning("Could not transform config file");
            e.printStackTrace();
        } catch (TransformerException e) {
            BrewServer.LOG.warning("Could not transformer file");
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the OWFSConnection configuration in a thread safe manner.
     */
    public static void setupOWFS() {
        if (owfsConnection != null) {
            try {
                owfsConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Use the thread safe mechanism
        BrewServer.LOG.info("Connecting to " + owfsServer + ":" + owfsPort);
        try {
            OwfsConnectionConfig owConfig = new OwfsConnectionConfig(owfsServer,
                    owfsPort);
            owConfig.setPersistence(OwPersistence.ON);
            owfsConnection = OwfsConnectionFactory
                    .newOwfsClientThreadSafe(owConfig);
            useOWFS = true;
        } catch (NullPointerException npe) {
            BrewServer.LOG.warning("OWFS is not able to be setup. You may need to rerun setup.");
        }
    }

    /**
     * Create the OWFS Connection to the server (owserver).
     */
    public static void createOWFS() {

        BrewServer.LOG.warning("Creating the OWFS configuration.");
        BrewServer.LOG.warning("What is the OWFS server host?"
                + " (Defaults to localhost)");

        String line = readInput();
        if (!line.trim().equals("")) {
            owfsServer = line.trim();
        }

        BrewServer.LOG.warning("What is the OWFS server port? (Defaults to 4304)");

        line = readInput();
        if (!line.trim().equals("")) {
            try {
                owfsPort = Integer.parseInt(line.trim());
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.warning("You entered: \"" + line.trim() + "\". Setting OWFS to default.");
                owfsPort = DEFAULT_OWFS_PORT;
            }
        }

        if (configDoc == null) {
            initializeConfig();
        }

        Element generalElement = getFirstElement(null, "general");
        if (generalElement == null) {
            generalElement = addNewElement(null, "general");
        }

        Element tempElement = addNewElement(generalElement, LaunchControl.OWFS_SERVER);
        tempElement.setTextContent(owfsServer);

        tempElement = addNewElement(generalElement, LaunchControl.OWFS_PORT);
        tempElement.setTextContent(Integer.toString(owfsPort));

        // Create the connection
        setupOWFS();
    }

    /**
     * Check to see if this probe already exists.
     *
     * @param address
     *            to check
     * @return true if the probe is setup.
     */
    public static boolean probeExists(final String address) {
        for (Temp t : tempList) {
            if (t.getProbe().equals(address)) {
                return true;
            }
        }
        return false;
    }

    /***********
     * Helper method to read a path value from OWFS with all checks.
     *
     * @param path
     *            The path to read from
     * @return A string representing the value (or null if there's an error
     * @throws OwfsException
     *             If OWFS throws an error
     * @throws IOException
     *             If an IO error occurs
     */
    public static String readOWFSPath(final String path) throws OwfsException,
            IOException {
        String result = "";
        if (owfsConnection == null) {
            setupOWFS();
            if (owfsConnection == null) {
                BrewServer.LOG.info("no OWFS connection");
                return "";
            }
        }
        try {
            if (owfsConnection.exists(path)) {
                result = owfsConnection.read(path);
            }
        } catch (OwfsException e) {
            // Error -1 is file not found, exists should bloody catch this
            if (!e.getMessage().equals("Error -1")) {
                throw e;
            }
        }

        return result.trim();
    }

    /*******
     * Helper function to read the user input and tidy it up.
     *
     * @return Trimmed String representing the UserInput
     */
    public static String readInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        try {
            input = br.readLine();
        } catch (IOException ioe) {
            System.out.println("IO error trying to read your input: " + input);
        }
        return input.trim();
    }

    /*******
     * Prints the list of probes that're found.
     */
    public void displaySensors() {
        // iterate the list of temperature Threads to get values
        Integer i = 1;
        for (Temp tTemp : tempList) {
            // launch all the PIDs first,
            // since they will launch the temp theads too
            BigDecimal currentTemp = tTemp.updateTemp();

            System.out.print(i.toString() + ") " + tTemp.getName());
            if (currentTemp.equals(Temp.ERROR_TEMP)) {
                BrewServer.LOG.warning(" doesn't have a valid temperature");
            } else {
                BrewServer.LOG.info(" " + currentTemp);
            }
            i++;
        }
    }

    /*****
     * Save the configuration to the Config.
     */
    public static void saveSettings() {
        if (configDoc == null) {
            setupConfigDoc();
        }

        // Delete the existing PIDs and Temps.
        NodeList devList = getElementsByXpath(null, "/elsinore/device");
        if (devList != null) {
            Set<Element> delElements = new HashSet<>();
            // Can't delete directly from the nodelist, concurrency issues.
            for (int i = 0; i < devList.getLength(); i++) {
                delElements.add((Element) devList.item(i));
            }
            // now we can delete them.
            for (Element e : delElements) {
                e.getParentNode().removeChild(e);
            }
        }

        // go through the list of Temps and save each one
        for (Temp fTemp : tempList) {

            PID n = LaunchControl.findPID(fTemp.getName());
            if (n == null || n.getName().equals("")) {
                // Delete the info
                deletePIDConfig(fTemp.getName());
            } else if (!n.getName().equals(fTemp.getProbe())) {
                BrewServer.LOG.info("Saving PID " + n.getName());
                savePID(n);
            }
            fTemp.save();

            if (fTemp.getVolumeBase() != null) {
                if (!fTemp.getVolumeAIN().equals("")) {
                    saveVolume(fTemp.getName(), fTemp.getVolumeAIN(),
                            fTemp.getVolumeUnit(), fTemp.getVolumeBase());
                } else if (fTemp.getVolumeAddress() != null
                        && fTemp.getVolumeOffset() != null) {
                    saveVolume(fTemp.getName(), fTemp.getVolumeAddress(),
                            fTemp.getVolumeOffset(), fTemp.getVolumeUnit(),
                            fTemp.getVolumeBase());
                } else {
                    BrewServer.LOG.info("No valid volume probe found");
                }
            } else {
                BrewServer.LOG.info("No Volume base set");
            }
        }

        // Save the timers
        if (timerList.size() > 0) {

            Element timersElement = getFirstElement(null, "timers");

            if (timersElement == null) {
                timersElement = addNewElement(null, "timers");
            }

            Node childTimer = timersElement.getFirstChild();
            while (childTimer != null) {
                timersElement.removeChild(childTimer);
                childTimer = timersElement.getFirstChild();
            }

            for (Timer t : timerList) {
                BrewServer.LOG.warning("Saving Timer: " + t.getName());
                Element timerElement = getFirstElementByXpath(null,
                        "/elsinore/timers/timer[@id='" + t + "']");
                if (timerElement == null) {
                    // No timer by this name
                    Element newTimer =
                            addNewElement(timersElement, "timer");
                    newTimer.setAttribute("id", t.getName());
                    newTimer.setAttribute("position", "" + t.getPosition());
                    newTimer.setAttribute("target", "" + t.getTarget());
                }
            }
        }

        // Delete all the switches first
        Element switchElement = getFirstElement(null, "switches");

        if (switchElement == null) {
            switchElement = addNewElement(null, "switches");
        }

        Node childSwitches = switchElement.getFirstChild();
        while (childSwitches != null) {
            switchElement.removeChild(childSwitches);
            childSwitches = switchElement.getFirstChild();
        }

        // Save the switches
        if (switchList.size() > 0) {

            for (Switch tSwitch : switchList) {

                Element newSwitch = getFirstElementByXpath(null,
                        "/elsinore/switches/" + tSwitch.getNodeName());

                if (newSwitch == null) {
                    // No timer by this name
                    newSwitch = addNewElement(switchElement,
                            "switch");
                    newSwitch.setAttribute("name", tSwitch.getNodeName());
                }
                newSwitch.setAttribute("gpio", tSwitch.getGPIO());
                newSwitch.setAttribute("position", "" + tSwitch.getPosition());
                Element invertElement = addNewElement(newSwitch, "invert");
                invertElement.setTextContent(
                        Boolean.toString(tSwitch.getInverted()));
                newSwitch.appendChild(invertElement);
            }
        }

        // Delete all the ph Sensors first
        Element phSensorsElement = getFirstElement(null, "phSensors");

        if (phSensorsElement == null) {
            phSensorsElement = addNewElement(null, "phSensors");
        }

        Node childSensor = phSensorsElement.getFirstChild();
        while (childSensor != null) {
            phSensorsElement.removeChild(childSensor);
            childSensor = phSensorsElement.getFirstChild();
        }

        // Save the PH Sensors
        if (phSensorList.size() > 0) {

            for (PhSensor tSensor : phSensorList) {

                Element newSensor = getFirstElementByXpath(null,
                        "/elsinore/phSensors/" + tSensor.getName());

                if (newSensor == null) {
                    // No timer by this name
                    newSensor = addNewElement(phSensorsElement,
                            tSensor.getName());
                }
                newSensor.setAttribute(PhSensor.MODEL, tSensor.getModel());
                newSensor.setAttribute(PhSensor.AIN_PIN, tSensor.getAIN());
                newSensor.setAttribute(PhSensor.DS_ADDRESS, tSensor.getDsAddress());
                newSensor.setAttribute(PhSensor.DS_OFFSET, tSensor.getDsOffset());
                newSensor.setAttribute(PhSensor.OFFSET, tSensor.getOffset().toString());
                if (tSensor.i2cDevice != null)
                {
                    Element i2cDevice = addNewElement(newSensor, I2CDevice.I2C_NODE);
                    addNewElement(i2cDevice, I2CDevice.DEV_ADDRESS).setTextContent(Integer.toString(tSensor.i2cDevice.getAddress()));
                    addNewElement(i2cDevice, I2CDevice.DEV_NUMBER).setTextContent(Integer.toString(tSensor.i2cDevice.getDevNumber()));
                    addNewElement(i2cDevice, I2CDevice.DEV_TYPE).setTextContent(tSensor.i2cDevice.getDevName());
                    addNewElement(i2cDevice, I2CDevice.DEV_CHANNEL).setTextContent(Integer.toString(tSensor.i2cChannel));
                }
            }
        }

        // Delete all the triggers first
        Element triggerControlsElement = getFirstElement(null, TriggerControl.NAME);

        if (triggerControlsElement != null) {
            childSwitches = triggerControlsElement.getFirstChild();
            while (childSwitches != null) {
                triggerControlsElement.removeChild(childSwitches);
                childSwitches = triggerControlsElement.getFirstChild();
            }
        }

        for (Temp temp: tempList)
        {
            TriggerControl triggerControl = temp.getTriggerControl();
            if (triggerControl == null || triggerControl.getTriggersSize() == 0)
            {
                continue;
            }
            Element triggerElement = getFirstElementByXpath(null,
                    "/elsinore/" + TriggerControl.NAME + "[@name='" + temp.getName() + "']");
            if (triggerElement == null) {
                triggerElement = addNewElement(null, TriggerControl.NAME);
                triggerElement.setAttribute("name", temp.getName());
            }
            triggerControl.saveTriggers(triggerElement);
        }

        saveConfigFile();
    }

    public static void deletePIDConfig(String name) {
        if (configDoc == null) {
            return;
        }

        // save any changes
        Element device = getFirstElementByXpath(null, "/elsinore/device[@id='"
                + name + "']");
        if (device == null) {
            return;
        }

        // Save the config to the configuration file
        BrewServer.LOG.info("Deleting the PID information for " + name);

        BrewServer.LOG.info("Using base node " + device.getNodeName()
                + " with ID " + device.getAttribute("id"));

        device.getParentNode().removeChild(device);
    }

    /******
     * Save the PID to the config doc.
     *
     * @param pid
     *            The PID to save
     */
    public static void savePID(final PID pid) {

        if (pid.getName() == null || pid.getName().equals("")) {
            new Throwable().printStackTrace();
            return;
        }

        if (configDoc == null) {
            setupConfigDoc();
        }

        // Save the config to the configuration file
        BrewServer.LOG.info("Saving the information for " + pid.getName());

        // save any changes
        Element device = getFirstElementByXpath(null, "/elsinore/device[@id='"
                + pid.getName() + "']");

        if (device == null) {
            BrewServer.LOG.info("Creating new Element");
            device = addNewElement(null, "device");
            device.setAttribute(PID.ID, pid.getName());
        }

        BrewServer.LOG.info("Using base node " + device.getNodeName()
                + " with ID " + device.getAttribute(PID.ID));

        setElementText(device, PID.DUTY_CYCLE, pid.getManualCycle().toString());
        setElementText(device, PID.DUTY_TIME, pid.getManualTime().toString());
        setElementText(device, PID.SET_POINT, pid.getSetPoint().toString());
        setElementText(device, PID.MODE, pid.getMode());

        if (pid.getHeatSetting() != null) {

            Element heatElement = getFirstElement(device, PID.HEAT);
            if (heatElement == null)
            {
                heatElement = addNewElement(device, PID.HEAT);
            }
            setElementText(heatElement, PID.CYCLE_TIME, pid.getHeatCycle().toString());
            setElementText(heatElement, PID.PROPORTIONAL, pid.getHeatP().toString());
            setElementText(heatElement, PID.INTEGRAL, pid.getHeatI().toString());
            setElementText(heatElement, PID.DERIVATIVE, pid.getHeatD().toString());
            setElementText(heatElement, PID.GPIO, pid.getHeatGPIO());
            setElementText(heatElement, PID.DELAY, pid.getHeatDelay().toString());
            setElementText(heatElement, PID.INVERT, Boolean.toString(pid.getHeatInverted()));

        }

        if (pid.getCoolSetting() != null) {
            Element coolElement = getFirstElement(device, PID.COOL);
            if (coolElement == null)
            {
                coolElement = addNewElement(device, PID.COOL);
            }

            setElementText(coolElement, PID.CYCLE_TIME, pid.getCoolCycle().toString());
            setElementText(coolElement, PID.DELAY, pid.getCoolDelay().toString());
            setElementText(coolElement, PID.PROPORTIONAL, pid.getCoolP().toString());
            setElementText(coolElement, PID.INTEGRAL, pid.getCoolI().toString());
            setElementText(coolElement, PID.DERIVATIVE, pid.getCoolD().toString());
            setElementText(coolElement, PID.GPIO, pid.getCoolGPIO());
            setElementText(coolElement, PID.INVERT, Boolean.toString(pid.getCoolInverted()));
        }
        
        if( pid.getTemp() != null && pid.getTemp().getProbe() != null) {
            setElementText(device, Temp.PROBE_ELEMENT, pid.getTemp().getProbe() );
        }
        
        setElementText(device, PID.MIN, pid.getMin().toString());
        setElementText(device, PID.MAX, pid.getMax().toString());
        setElementText(device, PID.TIME, pid.getTime().toString());

        if (pid.getAuxGPIO() != null) {
            setElementText(device, PID.AUX, pid.getAuxGPIO());
        }

        saveConfigFile();
    }

    /*******
     * Add a temperature device to the configuration file.
     *
     * @param temp
     *            The temp probe to save.
     * @return The newly created Document Element
     */
    public static Element addTempToConfig(Temp temp) {

        if (temp == null) {
            return null;
        }

        String probe = temp.getProbe();
        String name = temp.getName();
        String cutoff = temp.getCutoff();

        /**
         * Some people want to watch the world burn and don't name their probes!
        if (probe.equalsIgnoreCase(name)) {
            BrewServer.LOG.info("Probe: " + probe + " is not setup, not saving");
            return null;
        }
        **/
        // save any changes
        BrewServer.LOG.info("Saving " + name + " with probe " + probe);
        // save any changes
        Element device = getFirstElementByXpath(null, "/elsinore/device[@id='"
                + name + "']");
        if (device == null) {
            // no idea why there wouldn't be this section
            device = addNewElement(null, "device");
            device.setAttribute("id", name);
        }
        setElementText(device, Temp.PROBE_ELEMENT, probe);
        setElementText(device, Temp.POSITION, "" + temp.getPosition());
        setElementText(device, PID.CUTOFF, cutoff);
        setElementText(device, PID.CALIBRATION, temp.getCalibration());
        setElementText(device, PID.HIDDEN, Boolean.toString(temp.isHidden()));
        addNewElement(device, Temp.PROBE_SIZE).setTextContent(Integer.toString(temp.getSize()));

        BrewServer.LOG.info("Checking for volume");
        if (temp.hasVolume()) {
            System.out.println("Saving volume");
            setElementText(device, VolumeUnits.VOLUME_UNITS, temp.getVolumeUnit());
            if (!temp.getVolumeAIN().equals("")) {
                setElementText(device, VolumeUnits.VOLUME_PIN, temp.getVolumeAIN());
            } else {
                setElementText(device, VolumeUnits.VOLUME_ADDRESS, temp.getVolumeAddress());
                setElementText(device, VolumeUnits.VOLUME_OFFSET, temp.getVolumeOffset());
            }

            if (temp.i2cDevice != null)
            {
                Element i2cDevice = addNewElement(device, I2CDevice.I2C_NODE);
                addNewElement(i2cDevice, I2CDevice.DEV_ADDRESS).setTextContent(Integer.toString(temp.i2cDevice.getAddress()));
                addNewElement(i2cDevice, I2CDevice.DEV_NUMBER).setTextContent(Integer.toString(temp.i2cDevice.getDevNumber()));
                addNewElement(i2cDevice, I2CDevice.DEV_TYPE).setTextContent(temp.i2cDevice.getDevName());
                addNewElement(i2cDevice, I2CDevice.DEV_CHANNEL).setTextContent(Integer.toString(temp.i2cChannel));
            }

            ConcurrentHashMap<BigDecimal, BigDecimal> volumeBase = temp
                    .getVolumeBase();
            // Iterate over the entries

            if (volumeBase != null) {
                for (Entry<BigDecimal, BigDecimal> e : volumeBase.entrySet()) {
                    System.out.println("Saving volume point " + e.getKey()
                            + " value " + e.getValue());
                    Element volEntry = getFirstElementByXpath(null,
                            "/elsinore/device[@id='" + name + "']"
                            + "/volume[@vol='" + e.getKey().toString()
                            + "']");
                    if (volEntry == null) {
                        volEntry = addNewElement(device, "volume");
                        volEntry.setAttribute("vol", e.getKey().toString());
                    }

                    volEntry.setAttribute("vol", e.getKey().toString());
                    volEntry.setTextContent(e.getValue().toString());
                    device.appendChild(volEntry);
                }
            }
        }
        return device;
    }

    /*******
     * Save the volume details to the config.
     *
     * @param name
     *            name of the device to add the information to
     * @param address
     *            the the one Wire ADC converter
     * @param offset
     *            The 1wire input (A/B/C/D)
     * @param volumeUnit
     *            The units for the volume (free text string)
     * @param concurrentHashMap
     *            Hashmap of the volume ranges and readings
     */
    public static void saveVolume(final String name, final String address,
            final String offset, final String volumeUnit,
            final ConcurrentHashMap<BigDecimal, BigDecimal> concurrentHashMap) {

        BrewServer.LOG.info("Saving volume for " + name);

        Element device = saveVolumeMeasurements(name, concurrentHashMap,
                volumeUnit);

        // Units for the Volume OneWire Address
        Element tElement = getFirstElement(device, "volume-address");
        if (tElement == null) {
            tElement = addNewElement(device, "volume-address");
        }

        tElement.setTextContent(address);

        // Units for the Volume OneWire Offset
        tElement = getFirstElement(device, "volume-offset");
        if (tElement == null) {
            tElement = addNewElement(device, "volume-offset");
        }

        tElement.setTextContent(offset);
    }

    /******
     * Save the volume information for an onboard Analogue Input.
     *
     * @param name
     *            Device name
     * @param volumeAIN
     *            Analogue input pin
     * @param volumeUnit
     *            The units for the volume (free text string)
     * @param concurrentHashMap
     *            Hashmap of the volume ranges and readings
     */
    public static void saveVolume(final String name, final String volumeAIN,
            final String volumeUnit,
            final ConcurrentHashMap<BigDecimal, BigDecimal> concurrentHashMap) {

        BrewServer.LOG.info("Saving volume for " + name);

        // save any changes
        Element device = saveVolumeMeasurements(name, concurrentHashMap,
                volumeUnit);

        Element tElement = getFirstElement(device, "volume-pin");
        if (tElement == null) {
            tElement = addNewElement(device, "volume-pin");
        }

        tElement.setTextContent(volumeAIN);

    }

    /**
     * Save the volume measurements for a device.
     *
     * @param name
     *            The name of the Device to save the volume measurements to
     * @param volumeBase
     *            The Calibration array of analog values and volume value
     * @param volumeUnit
     *            The units for the volume.
     * @return The element that was created
     */
    public static Element saveVolumeMeasurements(final String name,
            final ConcurrentHashMap<BigDecimal, BigDecimal> volumeBase,
            final String volumeUnit) {
        Element device = getFirstElementByXpath(null, "/elsinore/device[@id='"
                + name + "']");

        if (device == null) {
            // no idea why there wouldn't be this section
            device = addNewElement(null, "device");
            device.setAttribute("id", name);
        }

        Element tElement = getFirstElement(device, "volume-unit");
        if (tElement == null) {
            tElement = addNewElement(device, "volume-unit");
        }
        tElement.setTextContent(volumeUnit);

        for (Entry<BigDecimal, BigDecimal> entry : volumeBase.entrySet()) {
            System.out.println("Looking for volume entry: "
                    + entry.getKey().toString());

            tElement = getFirstElementByXpath(null, "/elsinore/device[@id='"
                    + name + "']/volume[@vol='" + entry.getKey().toString()
                    + "']");
            if (tElement == null) {
                tElement = addNewElement(device, "volume");
                tElement.setAttribute("vol", entry.getKey().toString());
            }

            tElement.setTextContent(entry.getValue().toString());
        }
        return device;
    }

    /******
     * Helper method to initialize the configuration.
     */
    public static void initializeConfig() {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        }
        File existingConfig = new File(configFileName);
        if (!existingConfig.exists()) {
            return;
        }

        try {
            assert dBuilder != null;
            configDoc = dBuilder.parse(existingConfig);
            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList nl = (NodeList) xp.evaluate(
                    "//text()[normalize-space(.)='']", configDoc,
                    XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); ++i) {
                Node node = nl.item(i);
                node.getParentNode().removeChild(node);
            }
            return;
        } catch (Exception e) {
            BrewServer.LOG.info(configFileName
                    + " isn't an XML File, trying configparser");
        }

        try {
            configCfg = new ConfigParser();

            BrewServer.LOG.info("Backing up config to " + configFileName
                    + ".original");
            File originalFile = new File(configFileName + ".original");
            File sourceFile = new File(configFileName);
            copyFile(sourceFile, originalFile);

            LaunchControl.setFileOwner(originalFile);
            LaunchControl.setFileOwner(sourceFile);

            configCfg.read(configFileName);
            BrewServer.LOG.info("Created configCfg");
        } catch (IOException e) {
            BrewServer.LOG.info("Config file at: " + configFileName
                    + " doesn't exist");
            configCfg = null;
        }
    }

    /**
     * Parse the config using the XML Parser.
     */
    public void parseXMLSections() {
        NodeList configSections = configDoc.getDocumentElement()
                .getChildNodes();

        if (configSections.getLength() == 0) {
            return;
        }
        // setup general first
        parseGeneral(getFirstElement(null, "general"));
        parseSwitches(getFirstElement(null, "switches"));
        parsePhSensors(getFirstElement(null, "phSensors"));

        for (int i = 0; i < configSections.getLength(); i++) {
            Node temp = configSections.item(i);
            if (temp.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) temp;
                BrewServer.LOG.info("Checking section " + e.getNodeName());
                // Parsed general first
                if (e.getNodeName().equalsIgnoreCase("timers")) {
                    parseTimers(e);
                } else if (e.getNodeName().equalsIgnoreCase("device")) {
                    parseDevice(e);
                }
            }
        }
    }

    /**
     * Parse a configuration section, such as a specific device.
     *
     * @param config
     *            The configuration element to parse.
     */
    public void parseDevice(final Element config) {
        String probe = null;
        String heatGPIO = null;
        String coolGPIO = null;
        String volumeUnits = "Litres";
        String dsAddress = null, dsOffset = null;
        String auxPin = null, cutoffTemp = null, calibration = null;
        String mode = "off";
        int probeSize = Temp.SIZE_LARGE;
        ConcurrentHashMap<BigDecimal, BigDecimal> volumeArray =
                new ConcurrentHashMap<>();
        BigDecimal duty = new BigDecimal(0), heatCycle = new BigDecimal(0.0),
                setpoint = new BigDecimal(0.0), heatP = new BigDecimal(0.0),
                heatI = new BigDecimal(0.0), heatD = new BigDecimal(0.0),
                heatDelay = new BigDecimal(0),
                min = new BigDecimal(0.0), max = new BigDecimal(0.0),
                time = new BigDecimal(0.0), coolP = new BigDecimal(0.0),
                coolI = new BigDecimal(0.0), coolD = new BigDecimal(0.0),
                coolCycle = new BigDecimal(0.0), cycle = new BigDecimal(0.0),
                coolDelay = new BigDecimal(0.0);
        boolean coolInvert = false, heatInvert = false, hidden = false;
        int analoguePin = -1, position = -1;
        Element i2cElement = null;

        String deviceName = config.getAttribute("id");

        BrewServer.LOG.info("Parsing XML Device: " + deviceName);
        try {
            probe = getTextForElement(config, Temp.PROBE_ELEMENT, null);
            position = Integer.parseInt(getTextForElement(config, Temp.POSITION, "-1"));
            duty = new BigDecimal(getTextForElement(config, PID.DUTY_CYCLE, "0.0"));
            cycle = new BigDecimal(getTextForElement(config, PID.DUTY_TIME, "0.0"));
            setpoint = new BigDecimal(getTextForElement(config, PID.SET_POINT, "0.0"));

            Element heatElement = getFirstElement(config, "heat");

            if (heatElement == null) {
                heatElement = config;
            }

            if (m_restore) {
                mode = getTextForElement(config, PID.MODE, "off");
            }

            heatGPIO = getTextForElement(heatElement, PID.GPIO, null);


            heatCycle = new BigDecimal(getTextForElement(heatElement, PID.CYCLE_TIME, "0.0"));
            heatP = new BigDecimal(getTextForElement(heatElement, PID.PROPORTIONAL, "0.0"));
            heatI = new BigDecimal(getTextForElement(heatElement, PID.INTEGRAL, "0.0"));
            heatD = new BigDecimal(getTextForElement(heatElement, PID.DERIVATIVE, "0.0"));
            heatDelay = new BigDecimal(getTextForElement(heatElement, PID.DELAY, "0.0"));
            heatInvert = Boolean.parseBoolean(getTextForElement(heatElement, PID.INVERT, "false"));

            Element coolElement = getFirstElement(config, "cool");

            if (coolElement != null)
            {
                coolCycle = new BigDecimal(getTextForElement(coolElement, PID.CYCLE_TIME, "0.0"));
                coolP = new BigDecimal(getTextForElement(coolElement, PID.PROPORTIONAL, "0.0"));
                coolI = new BigDecimal(getTextForElement(coolElement, PID.INTEGRAL, "0.0"));
                coolD = new BigDecimal(getTextForElement(coolElement, PID.DERIVATIVE, "0.0"));
                coolDelay = new BigDecimal(getTextForElement(coolElement, PID.DELAY, "0.0"));
                coolInvert = Boolean.parseBoolean(getTextForElement(coolElement, PID.INVERT, "false"));
                coolGPIO = getTextForElement(coolElement, PID.GPIO, null);
            }


            min = new BigDecimal(getTextForElement(config, PID.MIN, "0.0"));
            max = new BigDecimal(getTextForElement(config, PID.MAX, "0.0"));
            time = new BigDecimal(getTextForElement(config, PID.TIME, "0.0"));
            cutoffTemp = getTextForElement(config, PID.CUTOFF, "0.0");
            calibration = getTextForElement(config, PID.CALIBRATION, "0.0");
            auxPin = getTextForElement(config, PID.AUX, null);

            hidden = Boolean.parseBoolean(getTextForElement(config, PID.HIDDEN, "false"));

            NodeList tList = config.getElementsByTagName("volume");

            if (tList.getLength() >= 1) {
                for (int j = 0; j < tList.getLength(); j++) {
                    Element curOption = (Element) tList.item(j);

                    // Append the volume to the array
                    try {
                        BigDecimal volValue = new BigDecimal(curOption.getAttribute("vol"));
                        BigDecimal volReading = new BigDecimal(curOption.getTextContent());

                        volumeArray.put(volValue, volReading);
                        // we can parse this as an integer
                    } catch (NumberFormatException e) {
                        BrewServer.LOG.warning("Could not parse "
                                + curOption.getNodeName() + " as an integer");
                    }
                }
            }

            volumeUnits = getTextForElement(config, VolumeUnits.VOLUME_UNITS, null);
            analoguePin = Integer.parseInt(getTextForElement(config, VolumeUnits.VOLUME_PIN, "-1"));
            dsAddress = getTextForElement(config, VolumeUnits.VOLUME_ADDRESS, null);
            dsOffset = getTextForElement(config, VolumeUnits.VOLUME_OFFSET, null);

            probeSize = Integer.parseInt(getTextForElement(config, Temp.PROBE_SIZE, Integer.toString(Temp.SIZE_LARGE)));

            i2cElement = getFirstElement(config, I2CDevice.I2C_NODE);

            if (volumeUnits == null) {
                BrewServer.LOG.warning("Couldn't find a volume unit for "
                        + deviceName);
                volumeArray = null;
            }

            if (volumeArray != null && volumeArray.size() < MIN_VOLUME_SIZE) {
                BrewServer.LOG.info("Not enough volume data points, "
                        + volumeArray.size() + " found");
                volumeArray = null;
            } else if (volumeArray == null) {
                // we don't have a basic level
                // not implemented yet, math is hard
                System.out
                    .println("No Volume Presets, check your config or rerun the setup!");
                // otherwise we are OK
            }

        } catch (NumberFormatException nfe) {
            System.out.print("NumberFormatException when reading from: "
                    + deviceName);
            nfe.printStackTrace();
        } catch (Exception e) {
            BrewServer.LOG.info(e.getMessage() + " Ocurred when reading "
                    + deviceName);
            e.printStackTrace();
        }

        Temp newTemp = startDevice(deviceName, probe, heatGPIO, coolGPIO);
        if (newTemp == null) {
            System.out.println("Problems parsing device " + deviceName);
            System.exit(-1);
        }
        newTemp.setPosition(position);
        try {
            if ((heatGPIO != null && !heatGPIO.equals("") && GPIO.getPinNumber(heatGPIO) >= 0)
            || (coolGPIO != null && !coolGPIO.equals("") && GPIO.getPinNumber(coolGPIO) >= 0)) {
                PID tPID = LaunchControl.findPID(newTemp.getName());
                try {
                    tPID.setHysteria(min, max, time);
                } catch (NumberFormatException nfe) {
                    System.out
                        .println("Invalid options when setting up Hysteria: "
                                + nfe.getMessage());
                }

                tPID.updateValues(mode, duty, heatCycle, setpoint, heatP,
                        heatI, heatD);
                tPID.setHeatDelay(heatDelay);
                tPID.setCoolDelay(coolDelay);
                tPID.setCoolCycle(coolCycle);
                tPID.setCoolP(coolP);
                tPID.setCoolI(coolI);
                tPID.setCoolD(coolD);
                tPID.setCoolGPIO(coolGPIO);
                tPID.setCoolInverted(coolInvert);
                tPID.setHeatInverted(heatInvert);
                tPID.setManualTime(cycle);
                tPID.setManualDuty(duty);
                if (auxPin != null && !auxPin.equals("")) {
                    tPID.setAux(auxPin);
                }
            }
        } catch (InvalidGPIOException e) {
            BrewServer.LOG.info("Invalid GPIO provided");
            e.printStackTrace();
        }

        if (cutoffTemp != null) {
            newTemp.setCutoffTemp(cutoffTemp);
        }

        newTemp.setSize(probeSize);

        if (analoguePin != -1) {
            try {
                newTemp.setupVolumes(analoguePin, volumeUnits);
            } catch (InvalidGPIOException e) {
                e.printStackTrace();
            }
        } else if (dsAddress != null && dsOffset != null) {
            newTemp.setupVolumes(dsAddress, dsOffset, volumeUnits);
        }

        if (i2cElement != null)
        {
            String channel = getTextForElement(i2cElement, I2CDevice.DEV_CHANNEL, null);
            newTemp.setupVolumeI2C(getI2CDevice(i2cElement), channel, volumeUnits);
        }

        if (volumeArray != null && volumeArray.size() >= MIN_VOLUME_SIZE) {

            for (Entry<BigDecimal, BigDecimal> entry : volumeArray
                    .entrySet()) {
                newTemp.addVolumeMeasurement(entry.getKey(), entry.getValue());
            }
        }

        if (newTemp != null) {
            newTemp.setCalibration(calibration);
            if (hidden) {
                newTemp.hide();
            }
            Element triggerElement = getFirstElementByXpath(null,
                    "/elsinore/" + TriggerControl.NAME + "[@name='" + newTemp.getName() + "']");
            if (triggerElement != null) {
                TriggerControl triggerControl = newTemp.getTriggerControl();
                if (triggerControl == null)
                {
                    triggerControl = new TriggerControl();
                }
                triggerControl.readTriggers(triggerElement);
            }
        }
    }


    public static String getTextForElement(Element config, String name, String defaultValue) {
        Element tElement = getFirstElement(config, name);
        if (tElement != null) {
            return tElement.getTextContent();
        }
        return defaultValue;
    }

    /**
     * Copy a file helper, used for backing data the config file.
     *
     * @param sourceFile
     *            The file to copy from
     * @param destFile
     *            The file name to copy to
     * @throws IOException
     *             If there's an issue with copying the file
     */
    public static void copyFile(final File sourceFile, final File destFile)
            throws IOException {

        if (!destFile.exists() && !destFile.createNewFile()) {
            BrewServer.LOG.warning("Couldn't create " + destFile.getName());
            return;
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            BrewServer.LOG.warning("Failed to copy file: "
                    + e.getLocalizedMessage());
            throw e;
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Sets up the base configuration Document.
     */
    public static void setupConfigDoc() {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            configDoc = dBuilder.newDocument();
            configDoc.appendChild(configDoc.createElement("elsinore"));
        } catch (ParserConfigurationException e1) {
            BrewServer.LOG.info("Error creating the new document: "
                    + e1.getMessage());
            e1.printStackTrace();
        }
    }

    /**
     * Get a list of all the nodes that match the nodeName. If no baseNode is
     * provided (null) then it checks from the document root.
     *
     * @param baseNode
     *            The root element to check from, null for the base element
     * @param nodeName
     *            The node name to match
     * @return A NodeList of matching node names
     */
    public static NodeList getAllNodes(final Element baseNode,
            final String nodeName) {

        Element trueBase = baseNode;
        if (baseNode == null) {
            if (configDoc == null) {
                setupConfigDoc();
            }

            trueBase = configDoc.getDocumentElement();
        }

        return trueBase.getElementsByTagName(nodeName);
    }

    /**
     * Get the first element matching nodeName from the baseNode. If baseNode is
     * null, use the document root
     *
     * @param baseNode
     *            The base element to search, null matches from the root
     * @param nodeName
     *            The nodeName to find.
     * @return The First matching element.
     */
    public static Element getFirstElement(final Element baseNode,
            final String nodeName) {

        NodeList nodeList = getAllNodes(baseNode, nodeName);

        Element eFound = null;
        if (nodeList != null && nodeList.getLength() > 0) {
            eFound = (Element) nodeList.item(0);
        }

        return eFound;
    }

    /**
     * Add a new element to the specified baseNode.
     *
     * @param baseNode
     *            The baseNode to append to, if null, use the document root
     * @param nodeName
     *            The new node name to add.
     * @return The newly created element.
     */
    public static Element addNewElement(Element baseNode,
            String nodeName) {

        if (configDoc == null) {
            setupConfigDoc();
        }

//        // See if this element exists.
//        if (baseNode != null) {
//            NodeList nl = baseNode.getChildNodes();
//
//            if (nl.getLength() > 0) {
//                for (int i = 0; i < nl.getLength(); i++) {
//                    Node item = nl.item(i);
//                    if (item != null && item.getNodeName().equals(nodeName)) {
//                        return (Element) item;
//                    }
//                }
//            }
//        }

        Element newElement = configDoc.createElement(nodeName);
        Element trueBase = baseNode;
        BrewServer.LOG.info("Creating element of " + nodeName);

        if (trueBase == null) {
            BrewServer.LOG.info("Creating on configDoc base");
            trueBase = configDoc.getDocumentElement();
        } else {
            BrewServer.LOG.info("on " + trueBase.getNodeName());
        }

        newElement = (Element) trueBase.appendChild(newElement);

        return newElement;
    }

    /**
     * Returns all the elements using an Xpath Search.
     *
     * @param baseNode
     *            The base node to search on. if null, use the document root.
     * @param xpathIn
     *            The Xpath to search.
     * @return The first matching element
     */
    @SuppressWarnings("unused")
    public static NodeList getElementsByXpath(final Element baseNode,
            final String xpathIn) {

        NodeList tList = null;

        if (configDoc == null) {
            return null;
        }

        try {
            expr = xpath.compile(xpathIn);
            tList = (NodeList) expr.evaluate(configDoc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            BrewServer.LOG.warning("Bad XPATH: " + xpathIn);
            e.printStackTrace();
            System.exit(-1);

        }

        return tList;
    }

    /**
     * Returns the first element using an Xpath Search.
     *
     * @param baseNode
     *            The base node to search on. if null, use the document root.
     * @param xpathIn
     *            The Xpath to search.
     * @return The first matching element
     */
    public static Element getFirstElementByXpath(final Element baseNode,
            final String xpathIn) {

        Element tElement = null;

        NodeList tList = getElementsByXpath(baseNode, xpathIn);

        if (tList != null && tList.getLength() > 0) {
            tElement = (Element) tList.item(0);
        }

        return tElement;

    }

    /**
     * Sets the first found named element text.
     *
     * @param baseNode
     *            The baseNode to use, if null, use the root.
     * @param elementName
     *            The element name to look for.
     * @param textContent
     *            The new text content
     */
    public static void setElementText(final Element baseNode,
            final String elementName, final String textContent) {

        Element trueBase = baseNode;
        if (baseNode == null) {
            trueBase = configDoc.getDocumentElement();
        }

        Element tElement = getFirstElement(trueBase, elementName);

        if (tElement == null) {
            tElement = addNewElement(trueBase, elementName);
        }

        tElement.setTextContent(textContent);

        trueBase.appendChild(tElement);
    }

    /**
     * Delete the named element from the baseNode.
     *
     * @param baseNode
     *            The Node to delete a child from
     * @param elementName
     *            The child element name to delete
     */
    public static void deleteElement(Element baseNode, String elementName) {
        Element trueBase = baseNode;
        if (baseNode == null) {
            trueBase = configDoc.getDocumentElement();
        }

        Element tElement = getFirstElement(trueBase, elementName);
        if (tElement == null) {
            return;
        }

        trueBase.removeChild(tElement);

    }

    /**
     * Add a MashControl object to the master mash control list.
     *
     * @param mControl
     *            The new mashControl to add
     */
    public static void addMashControl(final TriggerControl mControl) {
        if (findTriggerControl(mControl.getOutputControl()) != null) {
            BrewServer.LOG
                    .warning("Duplicate Mash Profile detected! Not adding: "
                            + mControl.getOutputControl());
            return;
        }
        triggerControlList.add(mControl);
    }

    /**
     * Start the mashControl thread associated with the PID.
     *
     * @param pid
     *            The PID to find the mash control thread for.
     */
    public static void startMashControl(final String pid) {
        TriggerControl mControl = findTriggerControl(pid);
        Thread mThread = new Thread(mControl);
        mThread.setName("Mash-Thread[" + pid + "]");
        triggerThreads.add(mThread);
        mThread.start();
    }

    /**
     * Look for the TriggerControl for the specified PID.
     *
     * @param pid
     *            The PID string to search for.
     * @return The MashControl for the PID.
     */
    public static TriggerControl findTriggerControl(final String pid) {
        Temp tTemp = LaunchControl.findTemp(pid);
        if (tTemp != null)
        {
            return tTemp.getTriggerControl();
        }
        return  null;
    }

    /**
     * Get the current OWFS connection.
     *
     * @return The current OWFS Connection object
     */
    public static OwfsConnection getOWFS() {
        return owfsConnection;
    }

    /**
     * Helper to get the current list of timers.
     *
     * @return The current list of timers.
     */
    public static List<Timer> getTimerList() {
        return timerList;
    }

    /**
     * Check GIT for updates and update the UI.
     */
    public static void checkForUpdates() {
        if (pb != null) {
            BrewServer.LOG.warning("Update is already running");
            return;
        }
        // Build command
        File jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        List<String> commands = new ArrayList<>();
        commands.add("git");
        commands.add("fetch");
        pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
            process.waitFor();
            BrewServer.LOG.info(process.getOutputStream().toString());
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            pb = null;
            return;
        }

        String currentSha = getShaFor("HEAD");
        String headSha = getShaFor("origin");

        if (headSha != null && !headSha.equals(currentSha)) {
            LaunchControl.setMessage("Update Available. "
                    + "<span class='btn' id=\"UpdatesFromGit\""
                    + " type=\"submit\"" + " onClick='updateElsinore();'>"
                    + "Click here to update</span>");
        } else {
            LaunchControl.setMessage("No updates available!");
        }
        pb = null;
    }

    public static String getShaFor(String target) {
        File jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        ArrayList<String> commands = new ArrayList<>();
        commands.add("git");
        // Add arguments
        commands.add("rev-parse");
        commands.add("HEAD");
        BrewServer.LOG.info("Checking for sha for " + target);

        // Run macro on target
        Process process = null;
        pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        try {
            process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            if (process != null) {
                process.destroy();
            }
            pb = null;
            return null;
        }

        // Read output
        StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        String line, previous = null;
        String currentSha = null;

        try {
            while ((line = br.readLine()) != null) {
                if (!line.equals(previous)) {
                    previous = line;
                    if (Pattern.matches("[0-9a-f]{5,40}", line)) {
                        currentSha = line;
                    }
                    out.append(line).append('\n');
                    BrewServer.LOG.info(line);
                }
            }
        } catch (IOException e2) {
            BrewServer.LOG.info("Couldn't read a line when checking SHA");
            e2.printStackTrace();
            if (process != null) {
                process.destroy();
            }
            pb = null;
            return null;
        }

        if (currentSha == null) {
            BrewServer.LOG.info("Couldn't check " + target + " revision");
            LaunchControl.setMessage("Couldn't check " + target + " revision");
            if (process != null) {
                process.destroy();
            }
            pb = null;
            return null;
        }
        return currentSha;
    }
    /**
     * Update from GIT and restart.
     */
    public static void updateFromGit() {
        // Build command
        File jarLocation;

        jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        BrewServer.LOG.info("Updating from Head");
        List<String> commands = new ArrayList<>();
        commands.add("git");
        commands.add("pull");
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            LaunchControl.setMessage("Failed to update from Git");
            return;
        }

        StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        String line, previous = null;

        try {
            while ((line = br.readLine()) != null) {
                if (!line.equals(previous)) {
                    previous = line;
                    out.append(line).append('\n');
                }
            }
        } catch (IOException e2) {
            BrewServer.LOG.warning("Couldn't update from GIT");
            e2.printStackTrace();
            LaunchControl.setMessage(out.toString());
            return;
        }
        LaunchControl.setMessage(out.toString());
        BrewServer.LOG.warning(out.toString());

        System.exit(EXIT_UPDATE);
    }

    /**
     * Set the system message for the UI.
     *
     * @param newMessage
     *            The message to set.
     */
    public static void setMessage(final String newMessage) {
        LaunchControl.message = newMessage;
    }

    /**
     * Add a message to the current one.
     * @param newMessage The message to append.
     */
    public static void addMessage(final String newMessage) {
        LaunchControl.message += "\n" + newMessage;
    }

    /**
     * Get the current message.
     *
     * @return The current message.
     */
    public static String getMessage() {
        return LaunchControl.message;
    }

    /**
     * @return The brewery name.
     */
    public static String getName() {
        return LaunchControl.breweryName;
    }

    /**
     * Set the brewery name.
     *
     * @param newName
     *            New brewery name.
     */
    public static void setName(final String newName) {
        BrewServer.LOG.info("Updating brewery name from "
                + LaunchControl.breweryName + " to " + newName);
        LaunchControl.breweryName = newName;
    }

    /**
     * Delete the PID from the list.
     *
     * @param tPID
     *            The PID object to delete.
     */
    public static void deletePID(PID tPID) {
        tPID.stop();
        pidList.remove(tPID);
    }

    public static void deleteTemp(Temp tTemp) {
        tTemp.shutdown();
        tempList.remove(tTemp);
    }
    /**
     * Get the system temperature scale.
     *
     * @return The system temperature scale.
     */
    public static String getScale() {
        return scale;
    }

    /**
     * Change the file owner.
     *
     * @param targetFile
     *            File to change the owner for.
     */
    public static void setFileOwner(File targetFile) {
        if (baseUser == null || baseUser.equals("")) {
            return;
        }

        if (targetFile != null && !targetFile.exists()) {
            try {
                new FileOutputStream(targetFile).close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        assert targetFile != null;
        Path path = Paths.get(targetFile.getAbsolutePath());
        UserPrincipalLookupService lookupService = FileSystems.getDefault()
                .getUserPrincipalLookupService();
        UserPrincipal userPrincipal;
        try {
            userPrincipal = lookupService.lookupPrincipalByName(baseUser);
            Files.setOwner(path, userPrincipal);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean setTempScales(String scale) {
        if (scale == null) {
            if (LaunchControl.scale.equals("C")) {
                scale = "F";
            } else {
                scale = "C";
            }
        }
        if (!scale.equals("C") && !scale.equals("F")) {
            return false;
        }
        // Change the temperature probes
        for (Temp t : tempList) {
            PID p = LaunchControl.findPID(t.getName());
            if (p != null) {
                if (!t.getScale().equals(scale)) {
                    // convert the target temp.
                    if (scale.equals("F")) {
                        p.setTemp(Temp.cToF(p.getSetPoint()));
                        p.setHysteria(Temp.cToF(p.getMin()), Temp.cToF(p.getMax()), p.getTime());
                    }
                }
            }
            t.setScale(scale);
        }
        LaunchControl.scale = scale;
        return true;
    }

    public static boolean isLocked() {
        return LaunchControl.pageLock;
    }

    public static boolean recorderEnabled() {
        return LaunchControl.recorderEnabled;
    }

    public static StatusRecorder getRecorder() {
        return LaunchControl.recorder;
    }

    public static void enableRecorder() {
        if (LaunchControl.recorder != null) {
            return;
        }
        BrewServer.LOG.info("Enabling the recorder");
        LaunchControl.recorderEnabled = true;
        LaunchControl.recorder = new StatusRecorder(recorderDirectory);
        LaunchControl.recorder.setThreshold(recorderDiff);
        LaunchControl.recorder.start();
    }

    public static StatusRecorder disableRecorder() {
        if (LaunchControl.recorder == null) {
            return null;
        }
        BrewServer.LOG.info("Disabling the recorder");
        LaunchControl.recorderEnabled = false;
        LaunchControl.recorder.stop();
        StatusRecorder temp = LaunchControl.recorder;
        LaunchControl.recorder = null;
        return temp;
    }

    public static List<String> getOneWireDevices(String prefix) {
        List<String> devices;
        devices = new ArrayList<>();
        if (owfsConnection == null) {
            LaunchControl.setMessage("OWFS is not setup,"
                    + " please delete your configuration file and start again");
            return devices;
        }
        try {
            List<String> owfsDirs = owfsConnection.listDirectory("/");
            if (owfsDirs.size() > 0) {
                BrewServer.LOG.info("Listing OWFS devices on " + owfsServer
                        + ":" + owfsPort);
            }
            Iterator<String> dirIt = owfsDirs.iterator();
            String dir;

            while (dirIt.hasNext()) {
                dir = dirIt.next();
                if (dir.startsWith(prefix)) {
                    devices.add(dir);
                }
            }
        } catch (OwfsException | IOException e) {
            e.printStackTrace();
        }

        return devices;
    }

    public static PhSensor findPhSensor(String string) {
        string = string.replace(" ", "_");
        Iterator<PhSensor> iterator = phSensorList.iterator();
        PhSensor tPh;
        while (iterator.hasNext()) {
            tPh = iterator.next();
            if (tPh.getName().equalsIgnoreCase(string)) {
                return tPh;
            }
        }
        return null;
    }

    /**
     * Delete the specified pH Sensor.
     *
     * @param name
     *            The sensor to delete.
     * @return true if the sensor is deleted.
     */
    public static boolean deletePhSensor(final String name) {
        // search based on the input name
        String realName = name.replace(" ", "_");
        PhSensor tSensor = findPhSensor(realName);
        return tSensor != null && phSensorList.remove(tSensor);

    }

    public static void sortTimers() {
        Collections.sort(LaunchControl.timerList);
    }

    public static void sortDevices() {
        Collections.sort(LaunchControl.tempList);
    }

    public static void saveEverything() {
        BrewServer.LOG.warning("Shutting down. Saving configuration");
        saveSettings();
        BrewServer.LOG.warning("Configuration saved.");

        BrewServer.LOG.warning("Shutting down temperature probe threads.");
        for (Temp t : tempList) {
            if (t != null) {
                t.save();
            }
        }

        BrewServer.LOG.warning("Shutting down PID threads.");
        for (PID n : pidList) {
            if (n != null) {
                n.shutdown();
            }
        }

        if (triggerControlList.size() > 0) {
            BrewServer.LOG.warning("Shutting down MashControl threads.");
            for (TriggerControl m : triggerControlList) {
                m.setShutdownFlag(true);
            }
        }
        if (switchList.size() > 0) {
            BrewServer.LOG.warning("Shutting down switchess.");
            for (Switch p : switchList) {
                p.shutdown();
            }
        }

        if (recorder != null) {
            BrewServer.LOG.warning("Shutting down recorder threads.");
            recorder.stop();
        }
        ServerRunner.running = false;
        BrewServer.LOG.warning("Goodbye!");
    }

    public static boolean shouldRestore() {
        return m_restore;
    }
}