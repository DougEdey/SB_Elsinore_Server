package com.sb.elsinore;

import com.sb.elsinore.devices.I2CDevice;
import com.sb.elsinore.devices.Switch;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.hardware.DeviceType;
import com.sb.elsinore.hardware.OneWireController;
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

    private static String baseUser = null;

    private final OneWireController oneWireController;
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
    private HashMap<Long, TempProbe> tempProbes = new HashMap<>();
    private SystemSettings systemSettings = null;
    private StatusRecorder recorder = null;

    @Autowired
    public LaunchControl(SystemSettingsRepository systemSettingsRepository,
                         TemperatureRepository temperatureRepository,
                         PIDRepository pidRepository,
                         OneWireController oneWireController) {
        this.systemSettingsRepository = systemSettingsRepository;
        this.temperatureRepository = temperatureRepository;
        this.pidRepository = pidRepository;
        this.oneWireController = oneWireController;

        loadAllModels();
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
            return;
        }
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


    /**
     * Add a PIDModel to the list.
     *
     * @param newTempProbe PIDModel to add.
     */
    Optional<TempRunner> addTemp(TemperatureInterface newTempProbe) {
        Optional<TempRunner> tempRunnerOpt = findTempRunner(newTempProbe);
        if (tempRunnerOpt.isEmpty()) {
            TempRunner tempRunner = new TempRunner(newTempProbe, oneWireController);
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
        if (!this.tempProbes.containsKey(tempProbe.getId())) {
            this.tempProbes.put(tempProbe.getId(), tempProbe);
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
            // TODO: Add notification to UX
        } else {
            // TODO: Add notification to UX
        }
        this.pb = null;
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
            // TODO: Add notification to UX
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
        this.tempProbes.remove(tTempProbe.getId());
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
        } else if (tempRunner.isPresent()) {
            tempRunner.get().updateTemperatureInterface(device);
        } else {
            this.logger.warn("Failed to find temperature probe with name: {}", name);
        }

        return tempRunner.orElse(null);
    }

    private Optional<TempRunner> findTempRunner(TemperatureInterface temperatureInterface) {
        return getTempRunners().stream()
                .filter(pRunner -> pRunner.getTempInterface().getId().equals(temperatureInterface.getId()))
                .findFirst();
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

    public List<TempProbe> availableProbes() {
        List<TempProbe> allDevices = this.oneWireController.getAvailableDevices(DeviceType.TEMPERATURE);
        allDevices.removeIf(d-> getTempProbes().stream().anyMatch(p -> p.getDevice().equals(p.getDevice())));
        return allDevices;
    }
}