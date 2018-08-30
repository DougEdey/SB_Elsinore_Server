package com.sb.elsinore;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sb.common.CollectionsUtil;
import com.sb.elsinore.devices.I2CDevice;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.inputs.PhSensor;
import com.sb.elsinore.models.*;
import com.sb.elsinore.models.Timer;
import com.sb.elsinore.repositories.PIDRepository;
import com.sb.elsinore.repositories.TemperatureRepository;
import com.sb.elsinore.wrappers.TempRunner;
import jGPIO.InvalidGPIOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.owfs.jowfsclient.Enums.OwPersistence;
import org.owfs.jowfsclient.OwfsConnection;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import java.io.*;
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

/**
 * LaunchControl is the core class of Elsinore. It reads the config file,
 * determining whether to run setup or not It launches the threads for the PIDs,
 * Temperature, and Mash It write the config file on shutdown. It sets up the
 * OWFS connection
 *
 * @author doug
 */
public class LaunchControl {
    public static final Object timerLock = new Object();
    public static final String RepoURL = "http://dougedey.github.io/SB_Elsinore_Server/";
    /**
     * The default port to serve on, can be overridden with -p <port>.
     */
    static final int DEFAULT_PORT = 8080;
    /**
     * The Minimum number of volume data points.
     */
    static final int MIN_VOLUME_SIZE = 3;
    /**
     * The default OWFS Port.
     */
    private static final int DEFAULT_OWFS_PORT = 4304;
    /**
     * The Default scale to be used.
     */
    public static String scale = "F";
    private static boolean loadCompleted = false;
    private static String baseUser = null;
    /**
     * The accepted startup options.
     */
    private static Options startupOptions = null;
    /**
     * The Command line used.
     */
    private static CommandLine startupCommand = null;
    private static String message = "";
    private static LaunchControl instance;
    @Expose
    @SerializedName("general")
    public SystemSettings systemSettings = new SystemSettings();
    /**
     * List of Switches.
     */
    public List<Switch> switchList = new CopyOnWriteArrayList<>();
    boolean recorderEnabled = false;
    String breweryName = null;
    String theme = "default";
    private TemperatureRepository temperatureRepository;
    private PIDRepository pidRepository;
    /**
     * List of Timers.
     */
    @Expose
    @SerializedName("timers")
    private List<com.sb.elsinore.models.Timer> timerList = new CopyOnWriteArrayList<>();
    /**
     * List of MashControl profiles.
     */
    private List<TriggerControl> triggerControlList;

    /* public fields to hold data for various functions */
    /**
     * List of pH Sensors.
     */
    private CopyOnWriteArrayList<PhSensor> phSensorList;
    private HashMap<String, I2CDevice> i2cDeviceList;
    /**
     * PIDModel Thread List.
     */
    private List<TempRunner> tempRunners = null;
    private List<Thread> pidThreads;
    /**
     * One Wire File System Connection.
     */
    private OwfsConnection owfsConnection = null;
    private ProcessBuilder pb = null;
    private HashMap<String, TempRunner> deviceMap = new HashMap<>();
    private HashMap<String, TempProbe> tempProbes = new HashMap<>();

    public LaunchControl() {
        startup(DEFAULT_PORT);
    }

    public static LaunchControl getInstance() {
        if (instance == null) {
            instance = new LaunchControl();
        }
        return instance;
    }

    /**
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

    private static String convertAddress(String oldAddress) {
        String[] newAddress = oldAddress.split("[.\\-]");
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
     * Copy a file helper, used for backing data the config file.
     *
     * @param sourceFile The file to copy from
     * @param destFile   The file name to copy to
     * @throws IOException If there's an issue with copying the file
     */
    private static void copyFile(final File sourceFile, final File destFile)
            throws IOException {

        if (!destFile.exists() && !destFile.createNewFile()) {
            BrewServer.LOG.warning("Couldn't create " + destFile.getName());
            return;
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            BrewServer.LOG.warning("Failed to copy file: "
                    + e.getLocalizedMessage());
            throw e;
        }
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
     * Set the system message for the UI.
     *
     * @param newMessage The message to set.
     */
    public static void setMessage(final String newMessage) {
        LaunchControl.message = newMessage;
    }


    @Autowired
    public void setTemperatureRepository(TemperatureRepository temperatureRepository) {
        this.temperatureRepository = temperatureRepository;
    }

    @Autowired
    public void setPIDRepository(PIDRepository pidRepository) {
        this.pidRepository = pidRepository;
    }

    private void startup(int port) {
        this.systemSettings.server_port = port;

        // Create the shutdown hooks for all the threads
        // to make sure we close off the GPIO connections
        // Close off all the Switch GPIOs properly.
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

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
        if (this.temperatureRepository != null) {
            Temperature probe = this.temperatureRepository.findByName("System");
            if (probe == null) {
                addSystemTemp();
            }
            this.temperatureRepository.findAll().forEach(this::addTemp);
        }
        // Debug info before launching the BrewServer itself
        LaunchControl.loadCompleted = true;
        BrewServer.LOG.log(Level.INFO, "CONFIG READ COMPLETED***********");
    }

    boolean isInitialized() {
        return loadCompleted;
    }

    void setRestore(boolean restore) {
        this.systemSettings.restoreState = restore;
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

    /**
     * Add the system temperature
     */
    private void addSystemTemp() {
        TempProbe tTempProbe = new TempProbe("System", "System");
        this.temperatureRepository.save(tTempProbe.getModel());
        BrewServer.LOG.info("Adding " + tTempProbe.getName());
        // setup the scale for each temp probe
        tTempProbe.setScale(this.systemSettings.scale);
    }

    void delSystemTemp() {
        TempRunner tempRunner = this.deviceMap.get("System");
        // Do we have anything to delete?
        if (tempRunner == null) {
            return;
        }

        tempRunner.shutdown();
        TemperatureInterface temperatureInterface = tempRunner.getTempInterface();
        this.temperatureRepository.delete(temperatureInterface.getModel());
        getTempRunners().removeIf(tr -> tr.getTempInterface() == temperatureInterface);

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
        com.sb.elsinore.models.Timer tTimer = new com.sb.elsinore.models.Timer(name);
        tTimer.setTarget(target);
        CollectionsUtil.addInOrder(getTimerList(), tTimer);

        return true;
    }

    /**
     * Add a PIDModel to the list.
     *
     * @param newTempProbe PIDModel to add.
     */
    public void addTemp(TemperatureInterface newTempProbe) {
        if (getTempRunners().stream().noneMatch(pRunner -> pRunner.getTempInterface() == newTempProbe)) {
            TempRunner tempRunner = new TempRunner(newTempProbe);
            getTempRunners().add(tempRunner);
            Thread pThread = new Thread(tempRunner);
            pThread.setName(tempRunner.getTempInterface().getName());
            getTempThreads().add(pThread);
            pThread.start();
        }

        TempProbe tempProbe = null;
        if (newTempProbe instanceof TempProbe) {
            tempProbe = (TempProbe) newTempProbe;
        } else {
            tempProbe = new TempProbe(newTempProbe);
        }

        if (!this.tempProbes.containsKey(newTempProbe.getName())) {
            this.tempProbes.put(newTempProbe.getName(), tempProbe);
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
    private com.sb.elsinore.models.Timer findTimer(final String name) {
        // search based on the input name
        Iterator<com.sb.elsinore.models.Timer> iterator = getTimerList().iterator();
        com.sb.elsinore.models.Timer tTimer;
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
        com.sb.elsinore.models.Timer tTimer = findTimer(name);
        getTimerList().remove(tTimer);
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
        return this.temperatureRepository.findByDevice(address) != null
                || this.temperatureRepository.findByDevice(owfsAddress) != null;
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
     * Start the mashControl thread associated with the PIDModel.
     *
     * @param pid The PIDModel to find the mash control thread for.
     */
    public void startMashControl(final String pid) {
        TriggerControl mControl = findTriggerControl(pid);
        Thread mThread = new Thread(mControl);
        mThread.setName("Mash-Thread[" + pid + "]");
        mThread.start();
    }

    /**
     * Look for the TriggerControl for the specified PIDModel.
     *
     * @param pid The PIDModel string to search for.
     * @return The MashControl for the PIDModel.
     */
    public TriggerControl findTriggerControl(final String pid) {
        if (this.tempProbes.containsKey(pid)) {
            return this.tempProbes.get(pid).getTriggerControl();
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
     * Delete the TempProbe/PIDModel from the list.
     *
     * @param tTempProbe The TempProbe object to delete.
     */
    public void deleteTemp(Temperature tTempProbe) {
        getTempRunners().removeIf(tempRunner -> tempRunner.getTempInterface().getName().equals(tTempProbe.getName()));
        getTempThreads().removeIf(tempThread -> tempThread.getName().equalsIgnoreCase(tTempProbe.getName()));
    }

    /**
     * Get the system temperature scale.
     *
     * @return The system temperature scale.
     */
    private String getScale() {
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

    private void setTempScales(String scale) {
        if (scale == null) {
            if (getScale().equals("C")) {
                scale = "F";
            } else {
                scale = "C";
            }
        }
        if (!scale.equals("C") && !scale.equals("F")) {
            return;
        }

        // Change the temperature probes
        for (Temperature tempProbe : this.temperatureRepository.findAll()) {
            tempProbe.setScale(scale);
        }
        setTempScales(scale);
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

    public I2CDevice getI2CDevice(String devNumber, String devAddress, String devType) {
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

    private PhSensor findPhSensor(String string) {
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

    void shutdown() {

        BrewServer.LOG.warning("Shutting down PIDModel threads...");
        getTempRunners().stream().filter(Objects::nonNull).forEach(TempRunner::stop);

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

    private List<Switch> getSwitchList() {
        if (this.switchList == null) {
            this.switchList = new ArrayList<>();
        }
        return this.switchList;
    }

    private List<TempRunner> getTempRunners() {
        if (this.tempRunners == null) {
            this.tempRunners = new ArrayList<>();
        }
        return this.tempRunners;
    }

    private List<Thread> getTempThreads() {
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

    public TempRunner findTemp(String name) {
        TempRunner tempRunner = this.deviceMap.get(name);
        Temperature device = this.temperatureRepository.findByName(name);
        if (device != null) {
            tempRunner = new TempRunner(device);
        } else {
            BrewServer.LOG.warning("Failed to find temperature probe with name: " + name);
        }

        return tempRunner;
    }

    public PIDModel findPID(String name) {
        List<PIDModel> deviceList = this.pidRepository.findByTemperatureName(name);
        if (deviceList.size() == 1) {
            return deviceList.get(0);
        }
        return null;
    }

    @PreDestroy
    public void preDestroy() {
        BrewServer.LOG.info("Shutting down! ");
        shutdown();
    }

    public List<TempProbe> getTempProbes() {
        return (List<TempProbe>) this.tempProbes.values();
    }

}