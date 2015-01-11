package com.sb.elsinore;

import jGPIO.GPIO;
import jGPIO.InvalidGPIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.ini4j.ConfigParser;
import org.ini4j.ConfigParser.InterpolationException;
import org.ini4j.ConfigParser.NoOptionException;
import org.ini4j.ConfigParser.NoSectionException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.owfs.jowfsclient.Enums.OwPersistence;
import org.owfs.jowfsclient.OwfsConnection;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import Cosm.Cosm;
import Cosm.CosmException;
import Cosm.Datastream;
import Cosm.Feed;
import Cosm.Unit;

import com.sb.common.CollectionsUtil;
import com.sb.common.ServeHTML;
import com.sb.elsinore.NanoHTTPD.Response;
import com.sb.elsinore.NanoHTTPD.Response.Status;
import com.sb.elsinore.inputs.PhSensor;

/**
 * LaunchControl is the core class of Elsinore. It reads the config file,
 * determining whether to run setup or not It launches the threads for the PIDs,
 * Temperature, and Mash It write the config file on shutdown. It sets up the
 * OWFS connection
 * 
 * @author doug
 * 
 */
public final class LaunchControl {
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
    public int server_port = 8080;
    /**
     * The pump parameters length for creating the config.
     */
    public static final int PUMP_PARAM_LENGTH = 3;
    /**
     * The Minimum number of volume data points.
     */
    public static final int MIN_VOLUME_SIZE = 3;
    public static final String RepoURL = "http://dougedey.github.io/SB_Elsinore_Server/";
    public static String baseUser = null;

    /**
     * List of PIDs.
     */
    public static ArrayList<PID> pidList = new ArrayList<PID>();
    /**
     * List of Temperature probes.
     */
    public static ArrayList<Temp> tempList = new ArrayList<Temp>();
    /**
     * List of Pumps.
     */
    public static ArrayList<Pump> pumpList = new ArrayList<Pump>();
    /**
     * List of Timers.
     */
    public static CopyOnWriteArrayList<Timer> timerList = new CopyOnWriteArrayList<Timer>();
    /**
     * List of MashControl profiles.
     */
    public static List<TriggerControl> triggerControlList = new ArrayList<TriggerControl>();
    /**
     * List of pH Sensors.
     */
    public static ArrayList<PhSensor> phSensorList = new ArrayList<PhSensor>();
    /**
     * Temperature Thread list.
     */
    public static List<Thread> tempThreads = new ArrayList<Thread>();
    /**
     * PID Thread List.
     */
    public static List<Thread> pidThreads = new ArrayList<Thread>();
    /**
     * Mash Threads list.
     */
    public static List<Thread> triggerThreads = new ArrayList<Thread>();

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
     * The BrewDay object to manage timers.
     */
    public static BrewDay brewDay = null;
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
    public static boolean recorderEnabled = true;
    public static String recorderDirectory = StatusRecorder.defaultDirectory;
    public static String breweryName = null;
    public static String theme = "default";
    public static boolean pageLock = false;
    public static boolean allDevicesListed = false;

    /*****
     * Main method to launch the brewery.
     * 
     * @param arguments
     *            List of arguments from the command line
     */
    public static void main(final String... arguments) {
        BrewServer.LOG.info("Running Brewery Controller.");
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
                        int t = Integer.parseInt(startupCommand
                                .getOptionValue("port"));
                        port = t;
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
            if (root != null && root.exists() && root.isDirectory()) {
                System.setProperty("root_override", rootDir);
            } else {
                BrewServer.LOG.warning("Invalid root directory proviced: "
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
                BrewServer.LOG.warning("Shutting down. Saving configuration");
                saveSettings();
                BrewServer.LOG.warning("Configuration saved.");
                
                BrewServer.LOG.warning("Shutting down temperature probe threads.");
                synchronized (tempList) {
                    for (Temp t : tempList) {
                        if (t != null) {
                            t.save();
                        }
                    }
                }

                BrewServer.LOG.warning("Shutting down PID threads.");
                synchronized (pidList) {
                    for (PID n : pidList) {
                        if (n != null) {
                            n.shutdown();
                        }
                    }
                }

                synchronized (triggerControlList) {
                    if (triggerControlList.size() > 0) {
                        BrewServer.LOG.warning("Shutting down MashControl threads.");
                        for (TriggerControl m : triggerControlList) {
                            m.setShutdownFlag(true);
                        }
                    }
                }
                // Close off all the Pump GPIOs properly.
                synchronized (pumpList) {
                    if (pumpList.size() > 0) {
                        BrewServer.LOG.warning("Shutting down pumps.");
                        for (Pump p : pumpList) {
                            p.shutdown();
                        }
                    }
                }

                saveConfigFile();

                if (recorder != null) {
                    BrewServer.LOG.warning("Shutting down recorder threads.");
                    recorder.stop();
                }
                BrewServer.LOG.warning("Goodbye!");
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

        if (LaunchControl.recorderEnabled) {
            BrewServer.LOG.log(Level.INFO, "Starting Status Recorder");

            recorder = new StatusRecorder(recorderDirectory);
            recorder.setThreshold(recorderDiff);
            recorder.start();
        }

        // Debug info before launching the BrewServer itself
        LaunchControl.loadCompleted = true;
        BrewServer.LOG.log(Level.INFO, "CONFIG READ COMPLETED***********");
        sRunner = new ServerRunner(BrewServer.class, this.server_port);
        sRunner.run();

        // iterate the list of Threads to kick off any PIDs
        Iterator<Temp> iterator = tempList.iterator();
        while (iterator.hasNext()) {
            // launch all the PIDs first
            // since they will launch the temperature threads too
            Temp tTemp = iterator.next();
            findPID(tTemp.getName());
        }

        // Old way to close off the System
        BrewServer.LOG.info("Waiting for input... Type 'quit' to exit");
        String input = "";
        String[] inputBroken;

        while (true) {
            System.out.print(">");
            input = "";
            // open up standard input
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    System.in));
            try {
                input = br.readLine();
            } catch (IOException ioe) {
                BrewServer.LOG.info("IO error trying to read your input: "
                        + input);
            }

            // parse the input and determine where to throw the data
            inputBroken = input.split(" ");
            // is the first value something we recognize?
            if (inputBroken[0].equalsIgnoreCase("quit")) {
                BrewServer.LOG.info("Quitting");
                System.exit(0);
            }
        }
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
        if (cosm == null) {
            BrewServer.LOG.warning("Couldn't connect to PACHUBE/COSM");
            return;
        }

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
        return;
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
                synchronized (tempList) {
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
                synchronized (tempList) {
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
            }

        } catch (CosmException e) {
            return "Could not get the images";
        }

        return "Grabbed images";
    }

    /**
     * Call to generate the HTML to be served back to the server.
     * 
     * @return The HTML of the controller page.
     */
    public static String getControlPage() {
        HashMap<String, String> devList = new HashMap<String, String>();
        synchronized (tempList) {
            for (Temp t : tempList) {
                PID tPid = findPID(t.getName());
                String type = "Temp";

                if (tPid != null) {
                    type = "PID";
                }
                devList.put(t.getName(), type);
            }
        }

        ServeHTML pidServe = new ServeHTML(devList, pumpList);
        return pidServe.getPage();
    }

    /******
     * Get the JSON Output String. This is the current Status of the PIDs,
     * Temps, Pumps, etc...
     * 
     * @return The JSON String of the current status.
     */
    @SuppressWarnings("unchecked")
    public static String getJSONStatus() {

        // get each setting add it to the JSON
        JSONObject rObj = new JSONObject();
        JSONObject tJSON = null;
        JSONObject triggerJSON = new JSONObject();
        rObj.put("locked", LaunchControl.pageLock);
        rObj.put("breweryName", LaunchControl.getName());

        // iterate the thread lists
        // use the temp list to determine if we have a PID to go with
        JSONArray vesselJSON = new JSONArray();
        synchronized (tempList) {
            for (Temp t : tempList) {
                if (LaunchControl.pageLock && t.getName().equals(t.getProbe())) {
                    continue;
                }

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

                if (t.getTriggerControl() != null
                        && t.getTriggerControl().triggerCount() > 0) {
                    triggerJSON.put(t.getName(),
                            t.getTriggerControl().getJSONData());
                }
            }
        }
        rObj.put("vessels", vesselJSON);
        rObj.put("triggers", triggerJSON);

        if (brewDay != null) {
            rObj.put("brewday", brewDay.brewDayStatus());
        }

        // generate the list of pumps
        if (pumpList != null && pumpList.size() > 0) {
            tJSON = new JSONObject();

            for (Pump p : pumpList) {
                tJSON.put(p.getName().replaceAll(" ", "_"), p.getStatus());
            }

            rObj.put("pumps", tJSON);
        }

        if (LaunchControl.getMessage() != null) {
            rObj.put("message", LaunchControl.getMessage());
        }

        rObj.put("language", Locale.getDefault().toString());
        return rObj.toString();
    }

    /**
     * Get the system status.
     *
     * @return a JSON Object representing the current system status
     */
    public static String getSystemStatus() {
        JSONObject retVal = new JSONObject();
        retVal.put("recorder", LaunchControl.recorder != null);
        retVal.put("recorderTime", StatusRecorder.SLEEP);
        retVal.put("recorderDiff", StatusRecorder.THRESHOLD);
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

            Element tElement = getFirstElement(config, "brewery_name");

            if (tElement != null) {
                breweryName = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "pagelock");
            if (tElement != null) {
                pageLock = Boolean.parseBoolean(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "scale");
            if (tElement != null) {
                scale = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "recorder");
            if (tElement != null) {
                if (Boolean.parseBoolean(tElement.getTextContent())) {
                    LaunchControl.recorderEnabled = true;
                    LaunchControl.enableRecorder();
                } else {
                    LaunchControl.recorderEnabled = false;
                    LaunchControl.disableRecorder();
                }
            }

            tElement = getFirstElement(config, "recorderDiff");
            if (tElement != null) {
                try {
                    StatusRecorder.THRESHOLD = Double.parseDouble(tElement
                            .getTextContent());
                } catch (Exception e) {
                    LaunchControl.setMessage(LaunchControl.getMessage()
                            + "\n Failed to parse recorder diff as a double.\n"
                            + e.getMessage());
                }
            }

            tElement = getFirstElement(config, "recorderTime");
            if (tElement != null) {
                try {
                    StatusRecorder.SLEEP = Long.parseLong(tElement
                            .getTextContent());
                } catch (Exception e) {
                    LaunchControl.setMessage(LaunchControl.getMessage()
                            + "\n Failed to parse recorder sleep as a long.\n"
                            + e.getMessage());
                }
            }
            String cosmAPIKey = null;
            Integer cosmFeedID = null;

            // Check for the COSM Feed details
            tElement = getFirstElement(config, "cosm");
            if (tElement != null) {
                cosmAPIKey = tElement.getTextContent();
            }
            try {
                cosmFeedID = Integer.parseInt(getFirstElement(config,
                        "cosm_feed").getTextContent());
            } catch (NumberFormatException e) {
                cosmFeedID = null;
            } catch (NullPointerException ne) {
                cosmFeedID = null;
            }

            // Try PACHube if the Cosm fields don't exist
            if (cosmAPIKey == null) {
                try {
                    cosmAPIKey = getFirstElement(config, "pachube")
                            .getTextContent();
                } catch (NullPointerException e) {
                    cosmAPIKey = null;
                }
            }

            if (cosmFeedID == null) {
                try {
                    cosmFeedID = Integer.parseInt(getFirstElement(config,
                            "pachube_feed").getTextContent());
                } catch (NumberFormatException e) {
                    cosmFeedID = null;
                } catch (NullPointerException e) {
                    cosmFeedID = null;
                }

            }

            if (cosmAPIKey != null && cosmFeedID != null) {
                startCosm(cosmAPIKey, cosmFeedID);
            }

            // Check for an OWFS configuration
            try {
                owfsServer = getFirstElement(config, "owfs_server")
                        .getTextContent();
                owfsPort = Integer
                        .parseInt(getFirstElement(config, "owfs_port")
                                .getTextContent());
            } catch (NullPointerException e) {
                owfsServer = null;
                owfsPort = null;
            }

            if (owfsServer != null && owfsPort != null) {
                BrewServer.LOG.log(Level.INFO, "Setup OWFS at " + owfsServer
                        + ":" + owfsPort);

                setupOWFS();
            }

            // Check for a system temperature
            if (getFirstElement(config, "System") != null) {
                Temp tTemp = new Temp("System", "");
                tempList.add(tTemp);
                BrewServer.LOG.info("Adding " + tTemp.getName());
                // setup the scale for each temp probe
                tTemp.setScale(scale);
                // setup the threads
                Thread tThread = new Thread(tTemp);
                tThread.setName("Temp_" + tTemp.getName());
                tempThreads.add(tThread);
                tThread.start();
            }
        } catch (NumberFormatException nfe) {
            System.out.print("Number format problem!");
            nfe.printStackTrace();
        }
    }

    /**
     * Parse the pumps in the specified element.
     * 
     * @param config
     *            The element that contains the pumps information
     */
    public void parsePumps(final Element config) {
        if (config == null) {
            return;
        }

        NodeList pumps = config.getChildNodes();

        for (int i = 0; i < pumps.getLength(); i++) {
            Element curPump = (Element) pumps.item(i);
            String pumpName = curPump.getNodeName().replace("_", " ");
            String gpio = null;
            if (curPump.hasAttribute("gpio")) {
                gpio = curPump.getAttribute("gpio");
            } else {
                gpio = curPump.getTextContent();
            }
            int position = -1;

            String tempString = curPump.getAttribute("position");
            if (tempString == null) {
                try {
                    position = Integer.parseInt(tempString);
                } catch (NumberFormatException nfe) {
                    BrewServer.LOG.warning("Couldn't parse pump " + pumpName
                            + " position: " + tempString);
                }
            }
            try {
                synchronized (pumpList) {
                    pumpList.add(new Pump(pumpName, gpio));
                }
            } catch (InvalidGPIOException e) {
                BrewServer.LOG.warning("Invalid GPIO (" + gpio
                        + ") detected for pump " + pumpName);
                BrewServer.LOG.warning(
                        "Please fix the config file before running");
                System.exit(-1);
            }

            Element invert = getFirstElement(curPump, "invert");
            if (invert != null) {
                LaunchControl.findPump(pumpName).setInverted(
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
        timerList = new CopyOnWriteArrayList<Timer>();

        for (int i = 0; i < timers.getLength(); i++) {
            Element tElement = (Element) timers.item(i);
            Timer temp = new Timer(tElement.getAttribute("id"));
            if (tElement.getAttribute("position") != null) {
                try {
                    temp.setPosition(Integer.parseInt(tElement
                            .getAttribute("position")));
                } catch (NumberFormatException nfe) {
                    // Couldn't parse. Move on.
                }
            }
            synchronized (timerList) {
                timerList.add(temp);
            }
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
            temp.setDsAddress(tElement.getAttribute("dsAddress"));
            temp.setDsOffset(tElement.getAttribute("dsOffset"));
            temp.setAinPin(tElement.getAttribute("ainPin"));
            temp.setOffset(tElement.getAttribute("offset"));
            temp.setModel(tElement.getAttribute("model"));
            synchronized (phSensorList) {
                phSensorList.add(temp);
            }
        }
    }

    /**
     * Add a new pump to the server.
     *
     * @param name
     *            The name of the pump to add.
     * @param gpio
     *            The GPIO to add
     * @return True if added OK
     */
    public static boolean addPump(final String name, final String gpio) {
        if (name.equals("") || gpio.equals("") || pumpExists(name)) {
            return false;
        }
        if (LaunchControl.findPump(name) != null) {
            return false;
        }

        try {
            Pump p = new Pump(name, gpio);
            synchronized (pumpList) {

                pumpList.add(p);
            }
        } catch (Exception g) {
            BrewServer.LOG.warning("Could not add pump: " + g.getMessage());
            g.printStackTrace();
            return false;
        }

        return true;

    }

    // Add the system temperature
    public static void addSystemTemp() {
        Temp tTemp = new Temp("System", "");
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
     * Check to see if a pump with the given name exists.
     * 
     * @param name
     *            The name of the pump to check
     * @return True if the pump exists.
     */
    public static boolean pumpExists(final String name) {
        synchronized (pumpList) {
            for (Pump p : pumpList) {
                if (p.getName().equals("name")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a new timer to the list.
     * 
     * @param name
     *            The name of the timer.
     * @param mode
     *            The mode of the timer.
     * @return True if it was added OK.
     */
    public static boolean addTimer(final String name, final String mode) {
        // Mode is a placeholder for now
        synchronized (timerList) {
            if (timerList.contains(name) || name.equals("")) {
                return false;
            }
            CollectionsUtil.addInOrder(timerList, new Timer(name));
        }

        return true;
    }

    /**
     * Startup a PID device.
     * 
     * @param input
     *            The name of the PID.
     * @param probe
     *            The One-Wire probe address
     * @param gpio
     *            The GPIO to use, null doesn't start the device.
     * @return The new Temp probe. Use it to look up the PID if applicable.
     */
    public Temp startDevice(final String input, final String probe,
            final String gpio) {

        // Startup the thread
        if (probe == null || probe.equals("0")) {
            BrewServer.LOG.info("No Probe specified for " + input);
            return null;
        }

        if (!probe.startsWith("28") && !input.equals("System")) {
            BrewServer.LOG.warning(probe + " is not a temperature probe");
            return null;
        }

        // input is the name we'll use from here on out
        Temp tTemp = new Temp(input, probe);
        tempList.add(tTemp);
        BrewServer.LOG.info("Adding " + tTemp.getName() + " GPIO is (" + gpio
                + ")");

        // setup the scale for each temp probe
        tTemp.setScale(scale);

        // setup the threads
        Thread tThread = new Thread(tTemp);
        tThread.setName("Temp_" + tTemp.getName());
        tempThreads.add(tThread);
        tThread.start();

        if (gpio != null && !gpio.equals("")) {
            BrewServer.LOG.info("Adding PID with GPIO: " + gpio);
            PID tPID = new PID(tTemp, input, gpio);

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
        Datastream tData = null;
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
        List<String> lTags = new ArrayList<String>();

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
        synchronized (tempList) {
            Iterator<Temp> iterator = tempList.iterator();
            Temp tTemp = null;
            while (iterator.hasNext()) {
                tTemp = iterator.next();
                if (tTemp.getName().equalsIgnoreCase(name)) {
                    return tTemp;
                }
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
        synchronized (pidList) {
            Iterator<PID> iterator = pidList.iterator();
            PID tPid = null;
            while (iterator.hasNext()) {
                tPid = iterator.next();
                if (tPid.getName().equalsIgnoreCase(name)) {
                    return tPid;
                }
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
        synchronized (pidList) {
            pidList.add(newPID);
        }
        Thread pThread = new Thread(newPID);
        pThread.start();
        pidThreads.add(pThread);
    }

    /**************
     * Find the Pump in the current list.
     * 
     * @param name
     *            The pump to find
     * @return return the PUMP object
     */
    public static Pump findPump(final String name) {
        // search based on the input name
        synchronized (pumpList) {
            Iterator<Pump> iterator = pumpList.iterator();
            Pump tPump = null;
            while (iterator.hasNext()) {
                tPump = iterator.next();
                if (tPump.getName().equalsIgnoreCase(name)) {
                    return tPump;
                }
            }
        }
        return null;
    }

    /**
     * Delete the specified pump.
     *
     * @param name
     *            The pump to delete.
     */
    public static void deletePump(final String name) {
        // search based on the input name
        synchronized (pumpList) {
            Iterator<Pump> iterator = pumpList.iterator();
            Pump tPump = null;

            while (iterator.hasNext()) {
                tPump = iterator.next();
                if (tPump.getName().equalsIgnoreCase(name)) {
                    iterator.remove();
                    return;
                }
            }

        }
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
        synchronized (timerList) {
            Iterator<Timer> iterator = timerList.iterator();
            Timer tTimer = null;
            while (iterator.hasNext()) {
                tTimer = iterator.next();
                if (tTimer.getName().equalsIgnoreCase(name)) {
                    return tTimer;
                }
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
        synchronized (timerList) {
            Iterator<Timer> iterator = timerList.iterator();
            Timer tTimer = null;
            while (iterator.hasNext()) {
                tTimer = iterator.next();
                if (tTimer.getName().equalsIgnoreCase(name)) {
                    timerList.remove(tTimer);
                }
            }
        }
    }

    /********
     * Get the BrewDay object. Create one if there is no brewday object.
     * 
     * @return The current brewday object.
     */
    public static BrewDay getBrewDay() {
        if (brewDay == null) {
            brewDay = new BrewDay();
        }

        return brewDay;
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
                if (!currentFile.getName().startsWith("28")) {
                    if (!useOWFS && prompt) {
                        System.out.println("Detected a non temp probe."
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
                synchronized (tempList) {
                    tempList.add(currentTemp);
                }
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

        Element tempElement = null;

        tempElement = getFirstElement(generalElement, "pagelock");

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, "pagelock");
        }

        tempElement.setTextContent(Boolean.toString(pageLock));

        tempElement = getFirstElement(generalElement, "scale");

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, "scale");
        }

        tempElement.setTextContent(scale);

        tempElement = getFirstElement(generalElement, "recorder");

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, "recorder");
        }

        tempElement.setTextContent(Boolean
                .toString(LaunchControl.recorder != null));

        tempElement = getFirstElement(generalElement, "recorderDiff");

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, "recorderDiff");
        }

        tempElement.setTextContent(Double.toString(StatusRecorder.THRESHOLD));

        tempElement = getFirstElement(generalElement, "recorderTime");

        if (tempElement == null) {
            tempElement = addNewElement(generalElement, "recorderTime");
        }

        tempElement.setTextContent(Long.toString(StatusRecorder.SLEEP));

        if (breweryName != null && !breweryName.equals("")) {
            tempElement = getFirstElement(generalElement, "brewery_name");

            if (tempElement == null) {
                tempElement = addNewElement(generalElement, "brewery_name");
            }

            tempElement.setTextContent(breweryName);
        }

        if (useOWFS) {
            if (owfsServer != null) {
                tempElement = getFirstElement(generalElement, "owfs_server");

                if (tempElement == null) {
                    tempElement = addNewElement(generalElement, "owfs_server");
                }

                tempElement.setTextContent(owfsServer);
            }

            if (owfsPort != null) {
                tempElement = getFirstElement(generalElement, "owfs_port");

                if (tempElement == null) {
                    tempElement = addNewElement(generalElement, "owfs_port");
                }

                tempElement.setTextContent(Integer.toString(owfsPort));
            }
        }

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
            owfsPort = Integer.parseInt(line.trim());
        }

        if (configDoc == null) {
            initializeConfig();
        }

        Element generalElement = getFirstElement(null, "general");
        if (generalElement == null) {
            generalElement = addNewElement(null, "general");
        }

        Element tempElement = addNewElement(generalElement, "owfs_server");
        tempElement.setTextContent(owfsServer);

        tempElement = addNewElement(generalElement, "owfs_port");
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
        synchronized (tempList) {
            Iterator<Temp> iterator = tempList.iterator();
            while (iterator.hasNext()) {
                // launch all the PIDs first,
                // since they will launch the temp theads too
                Temp tTemp = iterator.next();
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
    }

    /*****
     * Save the configuration to the Config.
     */
    public void saveSettings() {
        if (configDoc == null) {
            setupConfigDoc();
        }

        // Delete the existing PIDs and Temps.
        NodeList devList = getElementsByXpath(null, "/elsinore/device");

        Set<Element> delElements = new HashSet<Element>();
        // Can't delete directly from the nodelist, concurrency issues.
        for (int i = 0; i < devList.getLength(); i++) {
            delElements.add((Element) devList.item(i));
        }
        // now we can delete them.
        for (Element e : delElements) {
            e.getParentNode().removeChild(e);
        }

        // go through the list of Temps and save each one
        for (Temp fTemp : tempList) {
            fTemp.save();

            PID n = LaunchControl.findPID(fTemp.getName());
            if (n == null || n.getName().equals("")) {
                // Delete the info
                deletePIDConfig(fTemp.getName());
            } else if (!n.getName().equals(fTemp.getProbe())) {
                BrewServer.LOG.info("Saving PID " + n.getName());
                savePID(n);
            }

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

            synchronized (timerList) {
                for (Timer t : timerList) {
                    Element timerElement = getFirstElementByXpath(null,
                            "/elsinore/timers/timer[@id='" + t + "']");
                    if (timerElement == null) {
                        // No timer by this name
                        Element newTimer =
                                addNewElement(timersElement, "timer");
                        newTimer.setAttribute("id", t.getName());
                        newTimer.setAttribute("position", "" + t.getPosition());
                    }
                }
            }
        }

        // Delete all the pumps first
        Element pumpsElement = getFirstElement(null, "pumps");

        if (pumpsElement == null) {
            pumpsElement = addNewElement(null, "pumps");
        }

        Node childPumps = pumpsElement.getFirstChild();
        while (childPumps != null) {
            pumpsElement.removeChild(childPumps);
            childPumps = pumpsElement.getFirstChild();
        }

        // Save the Pumps
        if (pumpList.size() > 0) {

            Iterator<Pump> pumpIt = pumpList.iterator();

            while (pumpIt.hasNext()) {

                Pump tPump = pumpIt.next();

                Element newPump = getFirstElementByXpath(null,
                        "/elsinore/pumps/" + tPump.getNodeName());

                if (newPump == null) {
                    // No timer by this name
                    newPump = addNewElement(pumpsElement,
                            tPump.getNodeName());
                }
                newPump.setAttribute("gpio", tPump.getGPIO());
                newPump.setAttribute("position", "" + tPump.getPosition());
                Element invertElement = addNewElement(newPump, "invert");
                invertElement.setTextContent(
                        Boolean.toString(tPump.getInverted()));
                newPump.appendChild(invertElement);
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

        // Save the Pumps
        if (phSensorList.size() > 0) {

            Iterator<PhSensor> phSensorIt = phSensorList.iterator();

            while (phSensorIt.hasNext()) {

                PhSensor tSensor = phSensorIt.next();

                Element newSensor = getFirstElementByXpath(null,
                        "/elsinore/phSensors/" + tSensor.getName());

                if (newSensor == null) {
                    // No timer by this name
                    newSensor = addNewElement(phSensorsElement,
                            tSensor.getName());
                }
                newSensor.setAttribute("model", tSensor.getModel());
                newSensor.setAttribute("ainPin", tSensor.getAIN());
                newSensor.setAttribute("dsAddress", tSensor.getDsAddress());
                newSensor.setAttribute("dsOffset", tSensor.getDsOffset());
                newSensor.setAttribute("offset", "" + tSensor.getOffset());
            }
        }
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
            device.setAttribute("id", pid.getName());
        }

        BrewServer.LOG.info("Using base node " + device.getNodeName()
                + " with ID " + device.getAttribute("id"));

        setElementText(device, "duty_cycle", pid.getManualCycle().toString());
        setElementText(device, "duty_time", pid.getManualTime().toString());
        setElementText(device, "set_point", pid.getSetPoint().toString());

        if (pid.getHeatSetting() != null) {
            Element heatElement = addNewElement(device, "heat");
            setElementText(heatElement, "cycle_time", pid.getHeatCycle()
                    .toString());
            setElementText(heatElement, "proportional", pid.getHeatP()
                    .toString());
            setElementText(heatElement, "integral", pid.getHeatI().toString());
            setElementText(heatElement, "derivative",
                    pid.getHeatD().toString());
            setElementText(heatElement, "gpio", pid.getHeatGPIO());
            setElementText(heatElement, "invert",
                    Boolean.toString(pid.getHeatInverted()));
        }

        if (pid.getCoolSetting() != null) {
            Element coolElement = addNewElement(device, "cool");
            setElementText(coolElement, "cycle_time", pid.getCoolCycle()
                    .toString());
            setElementText(coolElement, "delay", pid.getCoolDelay().toString());
            setElementText(coolElement, "proportional", pid.getCoolP()
                    .toString());
            setElementText(coolElement, "integral", pid.getCoolI().toString());
            setElementText(coolElement, "derivative",
                    pid.getCoolD().toString());
            setElementText(coolElement, "gpio", pid.getCoolGPIO());
            setElementText(coolElement, "invert",
                    Boolean.toString(pid.getCoolInverted()));
        }

        setElementText(device, "min", pid.getMin().toString());
        setElementText(device, "max", pid.getMax().toString());
        setElementText(device, "time", pid.getTime().toString());

        if (pid.getAuxGPIO() != null) {
            setElementText(device, "aux", pid.getAuxGPIO());
        }

        saveConfigFile();
    }

    /*******
     * Add a temperature device to the configuration file.
     *
     * @param probe
     *            The probe address.
     * @param name
     *            The temperature probe name
     * @return The newly created Document Element
     */
    public static Element addTempToConfig(Temp temp) {

        if (temp == null) {
            return null;
        }

        String probe = temp.getProbe();
        String name = temp.getName();
        String cutoff = temp.getCutoff();

        if (probe.equalsIgnoreCase(name)) {
            BrewServer.LOG.info("Probe: " + probe + " is not setup, not saving");
            return null;
        }
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
        setElementText(device, "probe", probe);
        setElementText(device, "cutoff", cutoff);
        setElementText(device, "calibration", temp.getCalibration());

        BrewServer.LOG.info("Checking for volume");
        if (temp.hasVolume()) {
            System.out.println("Saving volume");
            setElementText(device, "volume-units", temp.getVolumeUnit());
            if (!temp.getVolumeAIN().equals("")) {
                setElementText(device, "volume-ain",
                        temp.getVolumeAIN());
            } else {
                setElementText(device, "volume-address",
                        temp.getVolumeAddress());
                setElementText(device, "volume-offset", temp.getVolumeOffset());
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
    public void saveVolume(final String name, final String address,
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
    public void saveVolume(final String name, final String volumeAIN,
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
    public Element saveVolumeMeasurements(final String name,
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
        tElement.setTextContent(volumeUnit.toString());

        Iterator<Entry<BigDecimal, BigDecimal>> volIter = volumeBase.entrySet()
                .iterator();
        while (volIter.hasNext()) {
            Entry<BigDecimal, BigDecimal> entry = volIter.next();
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
        parsePumps(getFirstElement(null, "pumps"));
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
        String cutoffTemp = null, auxPin = null, calibration = "";
        ConcurrentHashMap<BigDecimal, BigDecimal> volumeArray =
                new ConcurrentHashMap<BigDecimal, BigDecimal>();
        BigDecimal duty = new BigDecimal(0), heatCycle = new BigDecimal(0.0),
                setpoint = new BigDecimal(0.0), heatP = new BigDecimal(0.0),
                heatI = new BigDecimal(0.0), heatD = new BigDecimal(0.0),
                min = new BigDecimal(0.0), max = new BigDecimal(0.0),
                time = new BigDecimal(0.0), coolP = new BigDecimal(0.0),
                coolI = new BigDecimal(0.0), coolD = new BigDecimal(0.0),
                coolCycle = new BigDecimal(0.0), cycle = new BigDecimal(0.0),
                coolDelay = new BigDecimal(0.0);
        boolean coolInvert = false, heatInvert = false;
        int analoguePin = -1;

        String deviceName = config.getAttribute("id");

        BrewServer.LOG.info("Parsing XML Device: " + deviceName);
        try {
            Element tElement = getFirstElement(config, "probe");
            if (tElement != null) {
                probe = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "duty_cycle");
            if (tElement != null) {
                duty = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "duty_time");
            if (tElement != null) {
                cycle = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "set_point");
            if (tElement != null) {
                setpoint = new BigDecimal(tElement.getTextContent());
            }

            Element heatElement = getFirstElement(config, "heat");

            if (heatElement == null) {
                heatElement = config;
            }

            tElement = getFirstElement(heatElement, "gpio");
            if (tElement != null) {
                heatGPIO = tElement.getTextContent();
            }

            tElement = getFirstElement(heatElement, "cycle_time");
            if (tElement != null) {
                heatCycle = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(heatElement, "proportional");
            if (tElement != null) {
                heatP = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(heatElement, "integral");
            if (tElement != null) {
                heatI = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(heatElement, "derivative");
            if (tElement != null) {
                heatD = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "invert");
            if (tElement != null) {
                heatInvert = Boolean.parseBoolean(tElement.getTextContent());
            }

            Element coolElement = getFirstElement(config, "cool");

            if (coolElement != null) {

                tElement = getFirstElement(coolElement, "cycle_time");
                if (tElement != null) {
                    coolCycle = new BigDecimal(tElement.getTextContent());
                }

                tElement = getFirstElement(coolElement, "proportional");
                if (tElement != null) {
                    coolP = new BigDecimal(tElement.getTextContent());
                }

                tElement = getFirstElement(coolElement, "integral");
                if (tElement != null) {
                    coolI = new BigDecimal(tElement.getTextContent());
                }

                tElement = getFirstElement(coolElement, "derivative");
                if (tElement != null) {
                    coolD = new BigDecimal(tElement.getTextContent());
                }

                tElement = getFirstElement(coolElement, "gpio");
                if (tElement != null) {
                    coolGPIO = tElement.getTextContent();
                }

                tElement = getFirstElement(coolElement, "delay");
                if (tElement != null) {
                    coolDelay = new BigDecimal(tElement.getTextContent());
                }

                tElement = getFirstElement(coolElement, "inverted");
                if (tElement != null) {
                    coolInvert = Boolean.parseBoolean(tElement.getTextContent());
                }
            }

            tElement = getFirstElement(config, "min");
            if (tElement != null) {
                min = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "max");
            if (tElement != null) {
                max = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "time");
            if (tElement != null) {
                time = new BigDecimal(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "cutoff");
            if (tElement != null) {
                cutoffTemp = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "calibration");
            if (tElement != null) {
                calibration = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "aux");
            if (tElement != null) {
                auxPin = tElement.getTextContent();
            }

            NodeList tList = config.getElementsByTagName("volume");

            if (tList.getLength() >= 1) {
                for (int j = 0; j < tList.getLength(); j++) {
                    Element curOption = (Element) tList.item(j);

                    // Append the volume to the array
                    try {
                        BigDecimal volValue = new BigDecimal(
                                curOption.getAttribute("vol"));
                        BigDecimal volReading = new BigDecimal(
                                curOption.getTextContent());

                        volumeArray.put(volValue, volReading);
                        // we can parse this as an integer
                    } catch (NumberFormatException e) {
                        BrewServer.LOG.warning("Could not parse "
                                + curOption.getNodeName() + " as an integer");
                    }
                }
            }

            tElement = getFirstElement(config, "volume-unit");
            if (tElement != null) {
                volumeUnits = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "volume-pin");
            if (tElement != null) {
                analoguePin = Integer.parseInt(tElement.getTextContent());
            }

            tElement = getFirstElement(config, "volume-address");
            if (tElement != null) {
                dsAddress = tElement.getTextContent();
            }

            tElement = getFirstElement(config, "volume-offset");
            if (tElement != null) {
                dsOffset = tElement.getTextContent();
            }

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

        Temp newTemp = startDevice(deviceName, probe, heatGPIO);
        if (newTemp == null) {
            System.out.println("Problems parsing device " + deviceName);
            System.exit(-1);
        }
        try {
            if (heatGPIO != null && GPIO.getPinNumber(heatGPIO) >= 0) {
                PID tPID = LaunchControl.findPID(newTemp.getName());
                try {
                    tPID.setHysteria(min, max, time);
                } catch (NumberFormatException nfe) {
                    System.out
                        .println("Invalid options when setting up Hysteria: "
                                + nfe.getMessage());
                }

                tPID.updateValues("off", duty, heatCycle, setpoint, heatP,
                        heatI, heatD);
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
        if (analoguePin != -1) {
            try {
                newTemp.setupVolumes(analoguePin, volumeUnits);
            } catch (InvalidGPIOException e) {
                e.printStackTrace();
            }
        } else if (dsAddress != null && dsOffset != null) {
            newTemp.setupVolumes(dsAddress, dsOffset, volumeUnits);
        }

        if (volumeArray != null && volumeArray.size() >= MIN_VOLUME_SIZE) {
            Iterator<Entry<BigDecimal, BigDecimal>> volIter = volumeArray
                    .entrySet().iterator();

            while (volIter.hasNext()) {
                Entry<BigDecimal, BigDecimal> entry = volIter.next();
                newTemp.addVolumeMeasurement(entry.getKey(), entry.getValue());
            }
        }

        if (newTemp != null) {
            newTemp.setCalibration(calibration);
        }
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

        if (!destFile.exists()) {
            destFile.createNewFile();
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
        DocumentBuilder dBuilder = null;
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

        NodeList foundList = trueBase.getElementsByTagName(nodeName);

        return foundList;
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
    public static Element addNewElement(final Element baseNode,
            final String nodeName) {

        if (configDoc == null) {
            setupConfigDoc();
        }

        // See if this element exists.
        /*if (baseNode != null) {
            NodeList nl = baseNode.getChildNodes();

            if (nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Node item = nl.item(i);
                    if (item != null && item.getNodeName().equals(nodeName)) {
                        return (Element) item;
                    }
                }
            }
        }*/

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

        if (tList.getLength() > 0) {
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
        return LaunchControl.findTemp(pid).getTriggerControl();
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
    public static CopyOnWriteArrayList<Timer> getTimerList() {
        return timerList;
    }

    /**
     * Check GIT for updates and update the UI.
     */
    public static void checkForUpdates() {
        // Build command
        File jarLocation = null;

        jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        List<String> commands = new ArrayList<String>();
        commands.add("git");
        commands.add("fetch");
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        Process process = null;
        try {
            process = pb.start();
            process.waitFor();
            BrewServer.LOG.info(process.getOutputStream().toString());
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            return;
        }

        commands = new ArrayList<String>();
        commands.add("git");
        // Add arguments
        commands.add("rev-parse");
        commands.add("HEAD");
        LaunchControl.setMessage("Checking for updates from git...");
        BrewServer.LOG.info("Checking for updates from the head repo");

        // Run macro on target
        pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        process = null;
        try {
            process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            return;
        }

        // Read output
        StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        String line = null, previous = null;
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
            BrewServer.LOG.info("Couldn't read a line when checking local SHA");
            e2.printStackTrace();
            return;
        }

        if (currentSha == null) {
            BrewServer.LOG.info("Couldn't check Head revision");
            LaunchControl.setMessage("Couldn't check head revision");
            return;
        }

        // Build command for head check

        commands = new ArrayList<String>();
        commands.add("git");
        // Add arguments
        commands.add("rev-parse");
        commands.add("origin");
        // Run macro on target
        pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        try {
            process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e1) {
            BrewServer.LOG.warning("Couldn't check remote SHA");
            e1.printStackTrace();
            return;
        }

        // Read output
        out = new StringBuilder();
        br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        line = null;
        previous = null;
        String headSha = null;

        try {
            while ((line = br.readLine()) != null) {
                if (!line.equals(previous)) {
                    previous = line;
                    if (Pattern.matches("[0-9a-f]{5,40}", line)) {
                        headSha = line;
                    }
                    out.append(line).append('\n');
                    BrewServer.LOG.info(line);
                }
            }
        } catch (IOException e) {
            BrewServer.LOG.warning("Couldn't read remote head revision output");
            e.printStackTrace();
            return;
        }

        if (headSha == null) {
            BrewServer.LOG.info("Couldn't check ORIGIN revision");
            LaunchControl.setMessage("Couldn't check ORIGIN revision");
            return;
        }

        if (!headSha.equals(currentSha)) {
            LaunchControl.setMessage("Update Available. "
                    + "<span class='btn' id=\"UpdatesFromGit\""
                    + " type=\"submit\"" + " onClick='updateElsinore();'>"
                    + "Click here to update</span>");
        } else {
            LaunchControl.setMessage("No updates available!");
        }

    }

    /**
     * Update from GIT and restart.
     * 
     * @return
     */
    public static void updateFromGit() {
        // Build command
        File jarLocation = null;

        jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        BrewServer.LOG.info("Updating from Head");
        List<String> commands = new ArrayList<String>();
        commands.add("git");
        commands.add("pull");
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(jarLocation);
        pb.redirectErrorStream(true);
        Process process = null;
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
        String line = null, previous = null;

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

        Path path = Paths.get(targetFile.getAbsolutePath());
        FileOwnerAttributeView view = Files.getFileAttributeView(path,
                FileOwnerAttributeView.class);
        UserPrincipalLookupService lookupService = FileSystems.getDefault()
                .getUserPrincipalLookupService();
        UserPrincipal userPrincipal;
        try {
            userPrincipal = lookupService.lookupPrincipalByName(baseUser);
            Files.setOwner(path, userPrincipal);
        } catch (IOException e) {
            // TODO Auto-generated catch block
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

    public static void disableRecorder() {
        if (LaunchControl.recorder == null) {
            return;
        }
        BrewServer.LOG.info("Disabling the recorder");
        LaunchControl.recorderEnabled = false;
        LaunchControl.recorder.stop();
        LaunchControl.recorder = null;
    }

    public static List<String> getOneWireDevices(String prefix) {
        List<String> devices = new ArrayList<String>();
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
            String dir = null;

            while (dirIt.hasNext()) {
                dir = dirIt.next();
                if (dir.startsWith(prefix)) {
                    devices.add(dir);
                }
            }
        } catch (OwfsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return devices;
    }

    public static PhSensor findPhSensor(String string) {
        string = string.replace(" ", "_");
        synchronized (phSensorList) {
            Iterator<PhSensor> iterator = phSensorList.iterator();
            PhSensor tPh = null;
            while (iterator.hasNext()) {
                tPh = iterator.next();
                if (tPh.getName().equalsIgnoreCase(string)) {
                    return tPh;
                }
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
        synchronized (phSensorList) {
            Iterator<PhSensor> iterator = phSensorList.iterator();
            PhSensor tSensor = null;

            while (iterator.hasNext()) {
                tSensor = iterator.next();
                if (tSensor.getName().equalsIgnoreCase(realName)) {
                    iterator.remove();
                    return true;
                }
            }

        }
        return false;
    }
}