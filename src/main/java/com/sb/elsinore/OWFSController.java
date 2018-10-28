package com.sb.elsinore;

import com.sb.elsinore.models.SystemSettings;
import org.owfs.jowfsclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@Singleton
public class OWFSController {
    /**
     * The default OWFS Port.
     */
    private static final int DEFAULT_OWFS_PORT = 4304;
    private SystemSettings systemSettings;
    private Logger logger = LoggerFactory.getLogger(OWFSController.class);
    /**
     * One Wire File System Connection.
     */
    private OwfsConnection owfsConnection = null;

    private String convertAddress(String oldAddress) {
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

            this.logger.info("Converted address: " + fixedAddress);

            return fixedAddress;
        }
        return oldAddress;
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

        if (!this.systemSettings.isOwfsEnabled()) {
            return;
        }
        // Use the thread safe mechanism
        this.logger.info("Connecting to " + this.systemSettings.getOwfsServer() + ":" + this.systemSettings.getOwfsPort());
        try {
            OwfsConnectionConfig owConfig = new OwfsConnectionConfig(this.systemSettings.getOwfsServer(),
                    this.systemSettings.getOwfsPort());
            owConfig.setPersistence(Enums.OwPersistence.ON);
            this.owfsConnection = OwfsConnectionFactory
                    .newOwfsClientThreadSafe(owConfig);
            this.systemSettings.setOwfsEnabled(true);
        } catch (NullPointerException npe) {
            this.logger.warn("OWFS is not able to be setup. You may need to rerun setup.");
        }
    }


    /**
     * Get the current OWFS connection.
     *
     * @return The current OWFS Connection object
     */
    public OwfsConnection getOWFS() {
        return this.owfsConnection;
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
                this.logger.info("no OWFS connection");
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
                this.logger.info("Listing OWFS devices on " + this.systemSettings.getOwfsServer()
                        + ":" + this.systemSettings.getOwfsPort());
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

}
