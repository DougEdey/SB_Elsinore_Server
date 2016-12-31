package com.sb.elsinore;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sb.common.CollectionsUtil;
import com.sb.elsinore.devices.I2CDevice;
import com.sb.elsinore.inputs.PhSensor;
import io.gsonfire.GsonFireBuilder;
import jGPIO.InvalidGPIOException;
import org.apache.commons.cli.*;
import org.owfs.jowfsclient.Enums.OwPersistence;
import org.owfs.jowfsclient.OwfsConnection;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;

import java.io.*;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.elsinore.UrlEndpoints.COOL;

/**
 * LaunchControl is the core class of Elsinore. It reads the config file,
 * determining whether to run setup or not It launches the threads for the PIDs,
 * Temperature, and Mash It write the config file on shutdown. It sets up the
 * OWFS connection
 *
 * @author doug
 */
@SuppressWarnings("unused")
public class LaunchControl {

    private static boolean loadCompleted = false;
    private Gson gson;

    @Expose
    @SerializedName("general")
    public SystemSettings systemSettings = new SystemSettings();

    /**
     * The default OWFS Port.
     */
    private static final int DEFAULT_OWFS_PORT = 4304;
    /**
     * The default port to serve on, can be overridden with -p <port>.
     */
    static final int DEFAULT_PORT = 8080;
    public static final Object timerLock = new Object();

    /**
     * The Minimum number of volume data points.
     */
    static final int MIN_VOLUME_SIZE = 3;
    public static final String RepoURL = "http://dougedey.github.io/SB_Elsinore_Server/";
    private static String baseUser = null;

    /**
     * List of Temperature probes.
     */
    @Expose
    @SerializedName("devices")
    public List<Temp> tempList = new CopyOnWriteArrayList<>();

    /**
     * List of Switches.
     */
    @Expose
    @SerializedName("switches")
    public List<Switch> switchList = new CopyOnWriteArrayList<>();
    /**
     * List of Timers.
     */
    @Expose
    @SerializedName("timers")
    private List<Timer> timerList = new CopyOnWriteArrayList<>();
    /**
     * List of MashControl profiles.
     */
    private List<TriggerControl> triggerControlList;
    /**
     * List of pH Sensors.
     */
    private CopyOnWriteArrayList<PhSensor> phSensorList;
    private HashMap<String, I2CDevice> i2cDeviceList;

    /**
     * PID Thread List.
     */
    private List<PIDRunner> pidRunners = null;
    private List<Thread> pidThreads;

    /**
     * Default config filename. Can be overridden with -c <filename>
     */
    private static String configFileName = "elsinore.cfg";

    /* public fields to hold data for various functions */

    /**
     * The Default scale to be used.
     */
    public static String scale = "F";

    /**
     * One Wire File System Connection.
     */
    private OwfsConnection owfsConnection = null;


    /**
     * The accepted startup options.
     */
    private static Options startupOptions = null;
    /**
     * The Command line used.
     */
    private static CommandLine startupCommand = null;

    public static String message = "";
    boolean recorderEnabled = false;
    String breweryName = null;
    String theme = "default";

    private ProcessBuilder pb = null;
    private static LaunchControl instance;

    public static LaunchControl getInstance() {
        return instance;
    }

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

                if (startupCommand.hasOption("baseUser")) {
                    baseUser = startupCommand.getOptionValue("baseUser");
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

        Gson gson = getGsonParser();


        try {
            instance = gson.fromJson(new FileReader(configFileName), LaunchControl.class);
            instance.startup(port);
        } catch (Exception fnfe) {
            BrewServer.LOG.warning("Failed to load JSON");
            fnfe.printStackTrace();
        }
        if (instance == null) {
            instance = new LaunchControl(port);
        }

        BrewServer.LOG.warning("Started LaunchControl: " + instance.toString());
    }

    /*******
     * Used to setup the options for the command line parser.
     */
    private static void createOptions() {
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
     * @param port The port to start the server on.
     */
    public LaunchControl(final int port) {
        instance = this;
        createConfig();
        startup(port);
    }

    private static Gson getGsonParser() {
        GsonFireBuilder builder = new GsonFireBuilder()
                .registerTypeSelector(Device.class, jsonElement -> {
                    String kind = jsonElement.getAsJsonObject().get("type").getAsString();
                    switch (kind) {
                        case Temp.TYPE:
                            return Temp.class; //This will cause Gson to deserialize the json mapping to A
                        case PID.TYPE:
                            return PID.class; //This will cause Gson to deserialize the json mapping to B
                        default:
                            return null; //returning null will trigger Gson's default behavior
                    }
                });
        return builder.createGsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting().create();
    }

    private void startup(int port) {
        this.systemSettings.server_port = port;
        updateDeviceList();
        // Create the shutdown hooks for all the threads
        // to make sure we close off the GPIO connections
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                saveEverything();
                // Close off all the Switch GPIOs properly.
            }
        });

        this.gson = getGsonParser();

        //Check to make sure we have a valid folder for one wire straight away.
        File w1Folder = new File("/sys/bus/w1/devices/");
        if (!w1Folder.exists()) {
            BrewServer.LOG.info("Couldn't read the one wire devices directory!");
            BrewServer.LOG.info("Did you set up One Wire?");
            System.out
                    .println("http://dougedey.github.io/2014/11/12/Setting_Up_One_Wire/");
            //System.exit(-1);
        }

        // Load the System probe if it's not configured
        Temp systemProbe = findTemp("System");
        if (systemProbe == null) {
            addSystemTemp();
        }
        // Debug info before launching the BrewServer itself
        LaunchControl.loadCompleted = true;
        BrewServer.LOG.log(Level.INFO, "CONFIG READ COMPLETED***********");
        /*
      The BrewServer runner object that'll be used.
     */
        ServerRunner sRunner = new ServerRunner(BrewServer.class, this.systemSettings.server_port);
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

    boolean isInitialized() {
        return loadCompleted;
    }


    void setRestore(boolean restore) {
        this.systemSettings.restoreState = restore;
    }

    /******
     * Get the JSON Output String. This is the current Status of the PIDs,
     * Temps, Switches, etc...
     *
     * @return The JSON String of the current status.
     */
    @SuppressWarnings("unchecked")
    String getJSONStatus() {
        return this.gson.toJson(this);
    }

    /**
     * Get the system status.
     *
     * @return a JSON Object representing the current system status
     */
    @SuppressWarnings("unchecked")
    String getSystemStatus() {
        return this.gson.toJson(this.systemSettings, SystemSettings.class);
    }

    /**
     * Parse the general section of the XML Configuration.
     *
     * @param config The General element to be parsed.
     */
    private void parseGeneral(String config) {
        if (config == null) {
            return;
        }
        this.systemSettings = this.gson.fromJson(config, SystemSettings.class);

    }

    /**
     * Add a new switch to the server.
     *
     * @param name The name of the switch to add.
     * @param gpio The GPIO to add
     * @return True if added OK
     */
    Switch addSwitch(final String name, final String gpio) {
        if (name.equals("") || gpio.equals("")) {
            return null;
        }
        Switch p = findSwitch(name);
        if (p == null) {
            try {
                p = new Switch(name, gpio);
                getSwitchList().add(p);
            } catch (Exception g) {
                BrewServer.LOG.warning("Could not add switch: " + g.getMessage());
                g.printStackTrace();
            }
        } else {
            try {
                p.setGPIO(gpio);
            } catch (InvalidGPIOException g) {
                BrewServer.LOG.warning("Could not add switch: " + g.getMessage());
                g.printStackTrace();
            }
        }
        return p;
    }

    // Add the system temperature
    void addSystemTemp() {
        Temp tTemp = new Temp("System", "System");
        addTemp(tTemp);
        BrewServer.LOG.info("Adding " + tTemp.getName());
        // setup the scale for each temp probe
        tTemp.setScale(this.systemSettings.scale);
    }

    void delSystemTemp() {
        Temp tTemp = findTemp("System");
        // Do we have anything to delete?
        if (tTemp != null) {
            tTemp.shutdown();
            getTempList().remove(tTemp);
            getPidRunners().removeIf(pidRunner -> pidRunner.getTemp() == tTemp);
        }
    }

    /**
     * Check to see if a switch with the given name exists.
     *
     * @param name The name of the switch to check
     * @return True if the switch exists.
     */
    public boolean switchExists(final String name) {
        for (Switch p : getSwitchList()) {
            if (p.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new timer to the list.
     *
     * @param name   The name of the timer.
     * @param target The target timer of the timer.
     * @return True if it was added OK.
     */
    boolean addTimer(String name, String target) {
        // Mode is a placeholder for now
        if (findTimer(name) != null) {
            return false;
        }
        Timer tTimer = new Timer(name);
        tTimer.setTarget(target);
        CollectionsUtil.addInOrder(getTimerList(), tTimer);

        return true;
    }

    /**
     * Startup a PID device.
     *
     * @param input    The name of the PID.
     * @param probe    The One-Wire probe address
     * @param heatgpio The heat GPIO to use.
     * @param coolgpio The Cool GPIO to use.
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
        Temp tTemp = findTemp(probe);
        if (tTemp == null) {
            tTemp = new Temp(input, probe);
        }

        if (!isNullOrEmpty(heatgpio) || !isNullOrEmpty(coolgpio)) {
            PID tPID = new PID(input, probe, heatgpio);

            tPID.getSettings(COOL).setGPIO(coolgpio);
            // make sure we remove the old temp probe
            getTempList().remove(tTemp);
            tTemp.shutdown();
            tTemp = tPID;
        }

        BrewServer.LOG.info("Adding " + tTemp.getName() + " Heat GPIO is (" + heatgpio
                + ")");
        BrewServer.LOG.info("Adding " + tTemp.getName() + " Cool GPIO is (" + coolgpio
                + ")");

        // setup the scale for each temp probe
        tTemp.setScale(this.systemSettings.scale);
        addTemp(tTemp);
        return tTemp;
    }

    /******
     * Find the Temp Probe in the current list.
     *
     * @param name
     *            The Temp to find
     * @return The Temp object
     */
    public Temp findTemp(String name) {
        // search based on the input name
        Iterator<Temp> iterator = getTempList().iterator();
        Temp tTemp;
        while (iterator.hasNext()) {
            tTemp = iterator.next();
            if (tTemp.getName().equalsIgnoreCase(name)
                    || tTemp.getProbe().equalsIgnoreCase(name)) {
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
    public PID findPID(String name) {
        // search based on the input name
        Iterator<Temp> iterator = getTempList().iterator();
        Temp tPid;
        while (iterator.hasNext()) {
            tPid = iterator.next();
            if (tPid instanceof PID && tPid.getName().equalsIgnoreCase(name)) {
                return (PID) tPid;
            }
        }
        return null;
    }

    /**
     * Add a PID to the list.
     *
     * @param newTemp PID to add.
     */
    void addTemp(Temp newTemp) {
        Temp existingTemp = findTemp(newTemp.getName());
        if (existingTemp != null) {
            // same type, do nothing, otherwise remove the existing one
            if (existingTemp.getClass() == newTemp.getClass()) {
                return;
            }
            deleteTemp(existingTemp);
        }
        getTempList().add(newTemp);


        if (!getPidRunners().stream().anyMatch(pRunner -> pRunner.getTemp() == newTemp)) {
            PIDRunner pidRunner = new PIDRunner(newTemp);
            getPidRunners().add(pidRunner);
            Thread pThread = new Thread(pidRunner);
            pThread.setName(pidRunner.getTemp().getName());
            getPidThreads().add(pThread);
            pThread.start();
        }
    }

    /**************
     * Find the Switch in the server..
     *
     * @param name
     *            The switch to find
     * @return return the Switch object
     */
    public Switch findSwitch(String name) {
        // search based on the input name
        Iterator<Switch> iterator = getSwitchList().iterator();
        Switch tSwitch;
        while (iterator.hasNext()) {
            tSwitch = iterator.next();
            if (tSwitch.getName().equalsIgnoreCase(name)
                    || tSwitch.getNodeName().equalsIgnoreCase(name)) {
                return tSwitch;
            }
        }
        return null;
    }

    /**
     * Delete the specified switch.
     *
     * @param name The switch to delete.
     */
    void deleteSwitch(final String name) {
        // search based on the input name
        Switch tSwitch = findSwitch(name);
        getSwitchList().remove(tSwitch);
    }

    /**************
     * Find the Timer in the current list.
     *
     * @param name
     *            The timer to find
     * @return return the Timer object
     */
    Timer findTimer(final String name) {
        // search based on the input name
        Iterator<Timer> iterator = getTimerList().iterator();
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
    void deleteTimer(final String name) {
        // search based on the input name
        Timer tTimer = findTimer(name);
        getTimerList().remove(tTimer);
    }

    /**
     * List the one wire devices in /sys/bus/w1/devices.
     **/
    private void listOneWireSys() {
        // try to access the list of 1-wire devices
        File w1Folder = new File("/sys/bus/w1/devices/");
        if (!w1Folder.exists()) {
            BrewServer.LOG.warning("Couldn't read the one wire devices directory!");
            BrewServer.LOG.warning("Did you set up One Wire?");
            BrewServer.LOG.warning("http://dougedey.github.io/2014/11/12/Setting_Up_One_Wire/");
            return;
        }

        File[] listOfFiles = w1Folder.listFiles();

        assert listOfFiles != null;
        if (listOfFiles.length == 0) {
            BrewServer.LOG.warning("No 1Wire probes found! Please check your system!");
            BrewServer.LOG.warning("http://dougedey.github.io/2014/11/24/Why_Cant_I_Use_Elsinore_Without_Temperature_Probes/");
        }

        // Display what we found
        for (File currentFile : listOfFiles) {
            if (currentFile.isDirectory()
                    && !currentFile.getName().startsWith("w1_bus_master")) {

                // Check to see if this probe exists
                if (probeExists(currentFile.getName()) || !currentFile.getName().startsWith("28")) {
                    continue;
                }
                BrewServer.LOG.info("Checking for " + currentFile.getName());
                Temp currentTemp = new Temp(currentFile.getName(),
                        currentFile.getName());
                addTemp(currentTemp);
                // setup the scale for each temp probe
                currentTemp.setScale(this.systemSettings.scale);
            }
        }
    }

    /**
     * If the OWFS connection is setup, use this to
     * read the OWFS server for the temperature probes
     */
    void listOWFSTemps() {
        if (this.owfsConnection == null) {
            return;
        }

        List<String> owfsTemps = getOneWireDevices("/28");
        for (String temp : owfsTemps) {
            if (temp.startsWith("/")) {
                temp = temp.replace("/", "");
            }

            if (probeExists(temp)) {
                // We already have this one read in from the FS.
                continue;
            }
            BrewServer.LOG.info("Checking for " + temp);
            Temp currentTemp = new Temp(temp,
                    temp);
            addTemp(currentTemp);
            // setup the scale for each temp probe
            currentTemp.setScale(this.systemSettings.scale);
        }
    }

    private static String convertAddress(String oldAddress) {
        String[] newAddress = oldAddress.split("\\.|-");
        boolean oldIsOwfs = (oldAddress.contains("."));
        String sep = oldIsOwfs ? "-" : ".";

        if (newAddress.length == 2) {
            String devFamily = newAddress[0];
            String devAddress = "";
            // Byte swap!
            devAddress += newAddress[1].subSequence(10, 12);
            devAddress += newAddress[1].subSequence(8, 10);
            devAddress += newAddress[1].subSequence(6, 8);
            devAddress += newAddress[1].subSequence(4, 6);
            devAddress += newAddress[1].subSequence(2, 4);
            devAddress += newAddress[1].subSequence(0, 2);

            String fixedAddress = devFamily + sep
                    + devAddress.toLowerCase();

            BrewServer.LOG.info("Converted address: " + fixedAddress);

            return fixedAddress;
        }
        return oldAddress;
    }

    /**
     * update the device list.
     */
    private void updateDeviceList() {
        listOneWireSys();
        listOWFSTemps();
    }

    /**********
     * Setup the configuration. We get here if the configuration file doesn't
     * exist.
     */
    private void createConfig() {

        if (startupCommand != null && startupCommand.hasOption("owfs")) {
            createOWFS();
        }

        updateDeviceList();

        if (getTempList().size() == 0) {
            BrewServer.LOG.warning("Could not find any one wire devices\n"
                    + "Please check you have the correct modules setup");
            BrewServer.LOG.warning("http://dougedey.github.io/2014/11/24/Why_Cant_I_Use_Elsinore_Without_Temperature_Probes/");
        }

        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
            String webURL = "http://" + addr.getHostName() + ":"
                    + this.systemSettings.server_port + "/controller";
            BrewServer.LOG.warning("Please go to the Web UI to the web UI "
                    + webURL);
        } catch (UnknownHostException e) {
            BrewServer.LOG.warning("Couldn't get localhost information.");
        }
    }

    /**
     * Save the configuration file to the default config filename as xml.
     */
    void saveConfigFile() {
        if (!LaunchControl.loadCompleted) {
            return;
        }
        File configOut = new File(configFileName);
        setFileOwner(configOut);

        try (Writer writer = new FileWriter(configFileName)) {
            this.gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the OWFSConnection configuration in a thread safe manner.
     */
    public void setupOWFS() {
        if (this.owfsConnection != null) {
            try {
                this.owfsConnection.disconnect();
                this.owfsConnection = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!this.systemSettings.useOWFS) {
            return;
        }
        // Use the thread safe mechanism
        BrewServer.LOG.info("Connecting to " + this.systemSettings.owfsServer + ":" + this.systemSettings.owfsPort);
        try {
            OwfsConnectionConfig owConfig = new OwfsConnectionConfig(this.systemSettings.owfsServer,
                    this.systemSettings.owfsPort);
            owConfig.setPersistence(OwPersistence.ON);
            this.owfsConnection = OwfsConnectionFactory
                    .newOwfsClientThreadSafe(owConfig);
            this.systemSettings.useOWFS = true;
        } catch (NullPointerException npe) {
            BrewServer.LOG.warning("OWFS is not able to be setup. You may need to rerun setup.");
        }
    }

    /**
     * Create the OWFS Connection to the server (owserver).
     */
    private void createOWFS() {

        BrewServer.LOG.warning("Creating the OWFS configuration.");
        BrewServer.LOG.warning("What is the OWFS server host?"
                + " (Defaults to localhost)");

        String line = readInput();
        if (!line.trim().equals("")) {
            this.systemSettings.owfsServer = line.trim();
        }

        BrewServer.LOG.warning("What is the OWFS server port? (Defaults to 4304)");

        line = readInput();
        if (!line.trim().equals("")) {
            try {
                this.systemSettings.owfsPort = Integer.parseInt(line.trim());
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.warning("You entered: \"" + line.trim() + "\". Setting OWFS to default.");
                this.systemSettings.owfsPort = DEFAULT_OWFS_PORT;
            }
        }
        saveConfigFile();
        // Create the connection
        setupOWFS();
    }

    /**
     * Check to see if this probe already exists.
     *
     * @param address to check
     * @return true if the probe is setup.
     */
    private boolean probeExists(String address) {
        String owfsAddress = convertAddress(address);
        for (Temp t : getTempList()) {
            if (t.getProbe().equals(address) || t.getProbe().equals(owfsAddress)) {
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
    public String readOWFSPath(String path) throws OwfsException,
            IOException {
        String result = "";
        if (this.owfsConnection == null) {
            setupOWFS();
            if (this.owfsConnection == null) {
                BrewServer.LOG.info("no OWFS connection");
                return "";
            }
        }
        try {
            if (this.owfsConnection.exists(path)) {
                result = this.owfsConnection.read(path);
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
    private String readInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        try {
            input = br.readLine();
        } catch (IOException ioe) {
            System.out.println("IO error trying to read your input: " + input);
        }
        return input.trim();
    }

    /******
     * Helper method to initialize the configuration.
     */
    private void initializeConfig() {
        File existingConfig = new File(configFileName);
        if (!existingConfig.exists()) {
            return;
        }

        try {
            BrewServer.LOG.info("Backing up config to " + configFileName
                    + ".original");
            File originalFile = new File(configFileName + ".original");
            File sourceFile = new File(configFileName);
            copyFile(sourceFile, originalFile);

            setFileOwner(originalFile);
            setFileOwner(sourceFile);

            BrewServer.LOG.info("Created configCfg");
        } catch (IOException e) {
            BrewServer.LOG.info("Config file at: " + configFileName
                    + " doesn't exist");
        }
    }

    /**
     * Copy a file helper, used for backing data the config file.
     *
     * @param sourceFile The file to copy from
     * @param destFile   The file name to copy to
     * @throws IOException If there's an issue with copying the file
     */
    static void copyFile(final File sourceFile, final File destFile)
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
     * Add a MashControl object to the master mash control list.
     *
     * @param mControl The new mashControl to add
     */
    public void addMashControl(final TriggerControl mControl) {
        if (findTriggerControl(mControl.getOutputControl()) != null) {
            BrewServer.LOG
                    .warning("Duplicate Mash Profile detected! Not adding: "
                            + mControl.getOutputControl());
            return;
        }
        getTriggerControlList().add(mControl);
    }

    /**
     * Start the mashControl thread associated with the PID.
     *
     * @param pid The PID to find the mash control thread for.
     */
    public void startMashControl(final String pid) {
        TriggerControl mControl = findTriggerControl(pid);
        Thread mThread = new Thread(mControl);
        mThread.setName("Mash-Thread[" + pid + "]");
        mThread.start();
    }

    /**
     * Look for the TriggerControl for the specified PID.
     *
     * @param pid The PID string to search for.
     * @return The MashControl for the PID.
     */
    public TriggerControl findTriggerControl(final String pid) {
        Temp tTemp = findTemp(pid);
        if (tTemp != null) {
            return tTemp.getTriggerControl();
        }
        return null;
    }

    /**
     * Get the current OWFS connection.
     *
     * @return The current OWFS Connection object
     */
    public OwfsConnection getOWFS() {
        return this.owfsConnection;
    }

    /**
     * Helper to get the current list of timers.
     *
     * @return The current list of timers.
     */
    private List<Timer> getTimerList() {
        return this.timerList;
    }

    /**
     * Check GIT for updates and update the UI.
     */
    void checkForUpdates() {
        if (this.pb != null) {
            BrewServer.LOG.warning("Update is already running");
            return;
        }
        // Build command
        File jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        List<String> commands = new ArrayList<>();
        commands.add("git");
        commands.add("fetch");
        this.pb = new ProcessBuilder(commands);
        this.pb.directory(jarLocation);
        this.pb.redirectErrorStream(true);
        Process process;
        try {
            process = this.pb.start();
            process.waitFor();
            BrewServer.LOG.info(process.getOutputStream().toString());
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            this.pb = null;
            return;
        }
        this.pb = null;
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
        this.pb = null;
    }

    private String getLastLogDate() {
        File jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        ArrayList<String> commands = new ArrayList<>();
        commands.add("git");
        // Add arguments
        commands.add("log");
        commands.add("-1");
        BrewServer.LOG.info("Checking for last log date");

        // Run macro on target
        Process process = null;
        this.pb = new ProcessBuilder(commands);
        this.pb.directory(jarLocation);
        this.pb.redirectErrorStream(true);
        try {
            process = this.pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            if (process != null) {
                process.destroy();
            }
            this.pb = null;
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
                    if (line.startsWith("Date:")) {
                        line = line.substring(5).trim();
                        out.append(line).append('\n');
                    }

                    BrewServer.LOG.info(line);
                }
            }
        } catch (IOException e2) {
            BrewServer.LOG.info("Couldn't read a line when checking SHA");
            e2.printStackTrace();
            process.destroy();
            this.pb = null;
            return null;
        }
        this.pb = null;
        return out.toString();
    }

    private String getShaFor(String target) {
        File jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        ArrayList<String> commands = new ArrayList<>();
        commands.add("git");
        // Add arguments
        commands.add("rev-parse");
        commands.add(target);
        BrewServer.LOG.info("Checking for sha for " + target);

        // Run macro on target
        Process process = null;
        this.pb = new ProcessBuilder(commands);
        this.pb.directory(jarLocation);
        this.pb.redirectErrorStream(true);
        try {
            process = this.pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e3) {
            BrewServer.LOG.info("Couldn't check remote git SHA");
            e3.printStackTrace();
            if (process != null) {
                process.destroy();
            }
            this.pb = null;
            return null;
        }

        // Read output
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
                    BrewServer.LOG.info(line);
                }
            }
        } catch (IOException e2) {
            BrewServer.LOG.info("Couldn't read a line when checking SHA");
            e2.printStackTrace();
            process.destroy();
            this.pb = null;
            return null;
        }

        if (currentSha == null) {
            BrewServer.LOG.info("Couldn't check " + target + " revision");
            LaunchControl.setMessage("Couldn't check " + target + " revision");
            process.destroy();
            this.pb = null;
            return null;
        }
        this.pb = null;
        return currentSha;
    }

    /**
     * Update from GIT and restart.
     */
    static void updateFromGit() {
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
        int EXIT_UPDATE = 128;
        System.exit(EXIT_UPDATE);
    }

    /**
     * Set the system message for the UI.
     *
     * @param newMessage The message to set.
     */
    public static void setMessage(final String newMessage) {
        LaunchControl.message = newMessage;
    }

    /**
     * Add a message to the current one.
     *
     * @param newMessage The message to append.
     */
    static void addMessage(final String newMessage) {
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
    public String getName() {
        return this.systemSettings.breweryName;
    }

    /**
     * Set the brewery name.
     *
     * @param newName New brewery name.
     */
    public void setName(final String newName) {
        BrewServer.LOG.info("Updating brewery name from "
                + this.systemSettings.breweryName + " to " + newName);
        this.systemSettings.breweryName = newName;
    }

    /**
     * Delete the Temp/PID from the list.
     *
     * @param tTemp The Temp object to delete.
     */
    void deleteTemp(Temp tTemp) {
        getTempList().forEach(temp -> {
            if (temp.getName().equals(tTemp.getName())) {
                tTemp.shutdown();
            }
        });
        getTempList().removeIf(temp -> temp.getName().equals(tTemp.getName()));

        getPidRunners().removeIf(pidRunner -> pidRunner.getTemp().getName().equals(tTemp.getName()));
        getPidThreads().removeIf(pidThread -> pidThread.getName().equalsIgnoreCase(tTemp.getName()));
    }

    /**
     * Get the system temperature scale.
     *
     * @return The system temperature scale.
     */
    public String getScale() {
        return this.systemSettings.scale;
    }

    /**
     * Change the file owner.
     *
     * @param targetFile File to change the owner for.
     */
    void setFileOwner(File targetFile) {
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

    boolean setTempScales(String scale) {
        if (scale == null) {
            if (getScale().equals("C")) {
                scale = "F";
            } else {
                scale = "C";
            }
        }
        if (!scale.equals("C") && !scale.equals("F")) {
            return false;
        }
        // Change the temperature probes
        for (Temp t : getTempList()) {
            PID p = findPID(t.getName());
            if (p != null) {
                if (!t.getScale().equals(scale)) {
                    // convert the target temp.
                    if (scale.equals("F")) {
                        p.setTemp(Temp.cToF(p.getSetPoint()));
                        p.setHysteria(Temp.cToF(p.getMin()), Temp.cToF(p.getMax()), p.getMinTime());
                    }
                }
            }
            t.setScale(scale);
        }
        setTempScales(scale);
        return true;
    }

    boolean recorderEnabled() {
        return this.recorderEnabled;
    }

    StatusRecorder getRecorder() {
        return this.systemSettings.recorder;
    }

    void enableRecorder() {
        if (this.systemSettings.recorder != null) {
            return;
        }
        BrewServer.LOG.info("Enabling the recorder");
        LaunchControl.getInstance().recorderEnabled = true;
        this.systemSettings.recorder = new StatusRecorder(StatusRecorder.defaultDirectory);
        this.systemSettings.recorder.start();
    }

    I2CDevice getI2CDevice(String devNumber, String devAddress, String devType) {
        String devKey = String.format("%s_%s", devNumber, devAddress);
        I2CDevice i2CDevice = getI2cDeviceList().get(devKey);
        if (i2CDevice == null) {
            i2CDevice = I2CDevice.create(devNumber, devAddress, devType);
        }

        return i2CDevice;
    }

    StatusRecorder disableRecorder() {
        if (this.systemSettings.recorder == null) {
            return null;
        }
        BrewServer.LOG.info("Disabling the recorder");
        this.recorderEnabled = false;
        this.systemSettings.recorder.stop();
        StatusRecorder temp = this.systemSettings.recorder;
        this.systemSettings.recorder = null;
        return temp;
    }

    public List<String> getOneWireDevices(String prefix) {
        List<String> devices;
        devices = new ArrayList<>();
        if (this.owfsConnection == null) {
            LaunchControl.setMessage("OWFS is not setup,"
                    + " please delete your configuration file and start again");
            return devices;
        }
        try {
            List<String> owfsDirs = this.owfsConnection.listDirectory("/");
            if (owfsDirs.size() > 0) {
                BrewServer.LOG.info("Listing OWFS devices on " + this.systemSettings.owfsServer
                        + ":" + this.systemSettings.owfsPort);
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

    PhSensor findPhSensor(String string) {
        string = string.replace(" ", "_");
        Iterator<PhSensor> iterator = getPhSensorList().iterator();
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
     * @param name The sensor to delete.
     * @return true if the sensor is deleted.
     */
    boolean deletePhSensor(final String name) {
        // search based on the input name
        String realName = name.replace(" ", "_");
        PhSensor tSensor = findPhSensor(realName);
        return tSensor != null && getPhSensorList().remove(tSensor);
    }

    void addPhSensor(PhSensor sensor) {
        getPhSensorList().add(sensor);
    }

    void sortTimers() {
        Collections.sort(getTimerList());
    }

    void sortDevices() {
        Collections.sort(getTempList());
    }

    void saveEverything() {
        BrewServer.LOG.warning("Shutting down. Saving configuration");
        saveConfigFile();
        BrewServer.LOG.warning("Configuration saved.");

        BrewServer.LOG.warning("Shutting down temperature probe threads.");

        BrewServer.LOG.warning("Shutting down PID threads.");
        getPidRunners().stream().filter(n -> n != null).forEach(PIDRunner::stop);

        if (getTriggerControlList().size() > 0) {
            BrewServer.LOG.warning("Shutting down MashControl threads.");
            for (TriggerControl m : getTriggerControlList()) {
                m.setShutdownFlag(true);
            }
        }

        if (getSwitchList().size() > 0) {
            BrewServer.LOG.warning("Shutting down switchess.");
            getSwitchList().forEach(Switch::shutdown);
        }

        if (this.systemSettings.recorder != null) {
            BrewServer.LOG.warning("Shutting down recorder threads.");
            this.systemSettings.recorder.stop();
        }
        ServerRunner.running = false;
        BrewServer.LOG.warning("Goodbye!");
    }

    public boolean shouldRestore() {
        return this.systemSettings.restoreState;
    }

    public String toJsonString(Object o) {
        return this.gson.toJson(o, o.getClass());
    }

    private List<Switch> getSwitchList() {
        if (this.switchList == null) {
            this.switchList = new ArrayList<>();
        }
        return this.switchList;
    }

    private List<Temp> getTempList() {
        if (this.tempList == null) {
            this.tempList = new ArrayList<>();
        }
        return this.tempList;
    }

    private List<PIDRunner> getPidRunners() {
        if (this.pidRunners == null) {
            this.pidRunners = new ArrayList<>();
        }
        return this.pidRunners;
    }

    private List<Thread> getPidThreads() {
        if (this.pidThreads == null) {
            this.pidThreads = new ArrayList<>();
        }
        return this.pidThreads;
    }

    private List<TriggerControl> getTriggerControlList() {
        if (this.triggerControlList == null) {
            this.triggerControlList = new ArrayList<>();
        }
        return this.triggerControlList;
    }

    private HashMap<String, I2CDevice> getI2cDeviceList() {
        if (this.i2cDeviceList == null) {
            this.i2cDeviceList = new HashMap<>();
        }
        return this.i2cDeviceList;
    }

    private CopyOnWriteArrayList<PhSensor> getPhSensorList() {
        if (this.phSensorList == null) {
            this.phSensorList = new CopyOnWriteArrayList<>();
        }
        return this.phSensorList;
    }
}