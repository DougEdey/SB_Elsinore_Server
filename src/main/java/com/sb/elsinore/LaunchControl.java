package com.sb.elsinore;

import com.sb.elsinore.devices.I2CDevice;
import com.sb.elsinore.devices.Switch;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.inputs.PhSensor;
import com.sb.elsinore.interfaces.TemperatureInterface;
import com.sb.elsinore.models.PIDModel;
import com.sb.elsinore.models.SystemSettings;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.elsinore.repositories.PIDRepository;
import com.sb.elsinore.repositories.SystemSettingsRepository;
import com.sb.elsinore.repositories.TemperatureRepository;
import com.sb.elsinore.wrappers.TempRunner;
import org.hibernate.cfg.NotYetImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * LaunchControl is the core class of Elsinore. It reads the config file,
 * determining whether to run setup or not It launches the threads for the PIDs,
 * TemperatureModel, and Mash It write the config file on shutdown. It sets up the
 * OWFS connection
 *
 * @author doug
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class LaunchControl {
    public static final String RepoURL = "http://dougedey.github.io/SB_Elsinore_Server/";

    /**
     * The Minimum number of volume data points.
     */
    static final int MIN_VOLUME_SIZE = 3;


    private static String baseUser = null;

    private static String message = "";
    /**
     * List of Switches.
     */
    public List<Switch> switchList = new CopyOnWriteArrayList<>();
    private SystemSettingsRepository systemSettingsRepository;
    private Logger logger = LoggerFactory.getLogger(LaunchControl.class);
    private TemperatureRepository temperatureRepository;
    private PIDRepository pidRepository;

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

    private ProcessBuilder pb = null;
    private HashMap<String, TempRunner> deviceMap = new HashMap<>();
    private HashMap<String, TempProbe> tempProbes = new HashMap<>();
    private SystemSettings systemSettings = null;
    private StatusRecorder recorder = null;

    @Autowired
    public LaunchControl(SystemSettingsRepository systemSettingsRepository,
                         TemperatureRepository temperatureRepository,
                         PIDRepository pidRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
        this.temperatureRepository = temperatureRepository;
        this.pidRepository = pidRepository;

        loadAllModels();
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

    /**
     * Update from GIT and restart.
     */
    void updateFromGit() {
        // Build command
        File jarLocation;

        jarLocation = new File(LaunchControl.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        this.logger.info("Updating from Head");
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
            this.logger.info("Couldn't check remote git SHA");
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
            this.logger.warn("Couldn't update from GIT");
            e2.printStackTrace();
            LaunchControl.setMessage(out.toString());
            return;
        }
        LaunchControl.setMessage(out.toString());
        this.logger.warn(out.toString());
        int EXIT_UPDATE = 128;
        System.exit(EXIT_UPDATE);
    }

    @Autowired
    public void setTemperatureRepository(TemperatureRepository temperatureRepository) {
        this.temperatureRepository = temperatureRepository;
    }

    @Autowired
    public void setPIDRepository(PIDRepository pidRepository) {
        this.pidRepository = pidRepository;
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
                //p = new Switch(name, gpio);
                getSwitchList().add(p);
            } catch (Exception g) {
                this.logger.warn("Could not add switch: " + g.getMessage());
                g.printStackTrace();
            }
        } else {
            p.setGpio(gpio);
        }
        return p;
    }

    /**
     * Add the system temperature
     */
    private void addSystemTemp() {
        TempProbe tTempProbe = new TempProbe("System", "System");
        this.temperatureRepository.save(tTempProbe.getModel());
        this.logger.info("Adding " + tTempProbe.getName());
        // setup the scale for each temp probe
        tTempProbe.setScale(getSystemSettings().getScale());
    }

    protected SystemSettings getSystemSettings() {
        if (this.systemSettings == null) {
            if (this.systemSettingsRepository.findAll().size() == 1) {
                this.systemSettings = this.systemSettingsRepository.findAll().get(0);
            } else {
                this.systemSettings = new SystemSettings();
                this.systemSettingsRepository.save(this.systemSettings);
            }
        }
        return this.systemSettings;
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
     * Add a PIDModel to the list.
     *
     * @param newTempProbe PIDModel to add.
     */
    public Optional<TempRunner> addTemp(TemperatureInterface newTempProbe) {
        Optional<TempRunner> tempRunnerOpt = findTempRunner(newTempProbe);
        if (tempRunnerOpt.isEmpty()) {
            TempRunner tempRunner = new TempRunner(newTempProbe);
            getTempRunners().add(tempRunner);
            Thread pThread = new Thread(tempRunner);
            pThread.setName(tempRunner.getTempInterface().getName());
            getTempThreads().add(pThread);
            pThread.start();
            tempRunnerOpt = Optional.of(tempRunner);
        }

        TempProbe tempProbe;
        if (newTempProbe instanceof TempProbe) {
            tempProbe = (TempProbe) newTempProbe;
        } else {
            tempProbe = new TempProbe(newTempProbe);
        }
        if (!this.tempProbes.containsKey(tempProbe.getName())) {
            this.tempProbes.put(tempProbe.getName(), tempProbe);
        }

        return tempRunnerOpt;
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
        return getSwitchList().stream()
                .filter(tSwitch -> tSwitch.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
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
            this.logger
                    .warn("Duplicate Mash Profile detected! Not adding: "
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
        throw new NotYetImplementedException();
        //return null;
    }

    /**
     * Check GIT for updates and update the UI.
     */
    void checkForUpdates() {
        if (this.pb != null) {
            this.logger.warn("Update is already running");
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
            this.logger.info(process.getOutputStream().toString());
        } catch (IOException | InterruptedException e3) {
            this.logger.info("Couldn't check remote git SHA");
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
        this.logger.info("Checking for last log date");

        // Run macro on target
        Process process = null;
        this.pb = new ProcessBuilder(commands);
        this.pb.directory(jarLocation);
        this.pb.redirectErrorStream(true);
        try {
            process = this.pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e3) {
            this.logger.info("Couldn't check remote git SHA");
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

                    this.logger.info(line);
                }
            }
        } catch (IOException e2) {
            this.logger.info("Couldn't read a line when checking SHA");
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
        this.logger.info("Checking for sha for " + target);

        // Run macro on target
        Process process = null;
        this.pb = new ProcessBuilder(commands);
        this.pb.directory(jarLocation);
        this.pb.redirectErrorStream(true);
        try {
            process = this.pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e3) {
            this.logger.info("Couldn't check remote git SHA");
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
                    this.logger.info(line);
                }
            }
        } catch (IOException e2) {
            this.logger.info("Couldn't read a line when checking SHA");
            e2.printStackTrace();
            process.destroy();
            this.pb = null;
            return null;
        }

        if (currentSha == null) {
            this.logger.info("Couldn't check " + target + " revision");
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
        return getSystemSettings().getBreweryName();
    }

    /**
     * Set the brewery name.
     *
     * @param newName New brewery name.
     */
    public void setName(final String newName) {
        this.logger.info("Updating brewery name from {} to {}", getSystemSettings().getBreweryName(), newName);
        getSystemSettings().setBreweryName(newName);
    }

    /**
     * Delete the TempProbe/PIDModel from the list.
     *
     * @param tTempProbe The TempProbe object to delete.
     */
    public void deleteTemp(TemperatureModel tTempProbe) {
        getTempRunners().removeIf(tempRunner -> tempRunner.getTempInterface().getName().equals(tTempProbe.getName()));
        getTempThreads().removeIf(tempThread -> tempThread.getName().equalsIgnoreCase(tTempProbe.getName()));
        tempProbes.remove(tTempProbe.getName());
        this.temperatureRepository.flush();
    }

    /**
     * Get the system temperature scale.
     *
     * @return The system temperature scale.
     */
    private String getScale() {
        return getSystemSettings().getScale();
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
        for (TemperatureModel tempProbe : this.temperatureRepository.findAll()) {
            tempProbe.setScale(scale);
        }
        setTempScales(scale);
    }

    boolean recorderEnabled() {
        return getSystemSettings().isRecorderEnabled();
    }

    StatusRecorder getRecorder() {
        return this.recorder;
    }

    void enableRecorder() {
        if (this.recorder != null) {
            return;
        }
        this.logger.info("Enabling the recorder");
        getSystemSettings().setRecorderEnabled(true);
        this.recorder = new StatusRecorder(StatusRecorder.defaultDirectory);
        this.recorder.start();
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
        if (this.recorder == null) {
            return null;
        }
        this.logger.info("Disabling the recorder");
        getSystemSettings().setRecorderEnabled(false);
        this.recorder.stop();
        StatusRecorder temp = this.recorder;
        this.recorder = null;
        return temp;
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

    void shutdown() {

        this.logger.warn("Shutting down PIDModel threads...");
        getTempRunners().stream().filter(Objects::nonNull).forEach(TempRunner::stop);

        if (getTriggerControlList().size() > 0) {
            this.logger.warn("Shutting down MashControl threads.");
            for (TriggerControl m : getTriggerControlList()) {
                m.setShutdownFlag(true);
            }
        }

        if (getSwitchList().size() > 0) {
            this.logger.warn("Shutting down switchess.");
            getSwitchList().forEach(Switch::shutdown);
        }

        if (this.recorder != null) {
            this.logger.warn("Shutting down recorder threads.");
            this.recorder.stop();
        }
        this.logger.warn("Goodbye!");
    }

    public boolean shouldRestore() {
        return getSystemSettings().isRestoreState();
    }

    private List<Switch> getSwitchList() {
        if (this.switchList == null) {
            this.switchList = new ArrayList<>();
        }
        return this.switchList;
    }

    public List<TempRunner> getTempRunners() {
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
        TemperatureModel device = this.temperatureRepository.findByNameIgnoreCase(name);
        Optional<TempRunner> tempRunner = findTempRunner(device);
        if (tempRunner.isEmpty() && device != null) {
            tempRunner = addTemp(device);
        } else {
            this.logger.warn("Failed to find temperature probe with name: {}", name);
        }

        return tempRunner.orElse(null);
    }

    private Optional<TempRunner> findTempRunner(TemperatureInterface temperatureInterface) {
        return getTempRunners().stream()
                .filter(pRunner -> pRunner.getTempInterface() == temperatureInterface).findFirst();
    }

    public PIDModel findPID(String name) {
        return this.pidRepository.findByTemperatureNameIgnoreCase(name);
    }

    @PreDestroy
    public void preDestroy() {
        this.logger.info("Shutting down!");
        shutdown();
    }

    private void loadAllModels() {
        this.temperatureRepository.findAll().forEach(this::addTemp);
    }

    public Collection<TempProbe> getTempProbes() {
        return this.tempProbes.values();
    }
}