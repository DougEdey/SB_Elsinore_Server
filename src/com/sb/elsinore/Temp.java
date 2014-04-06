package com.sb.elsinore;
import jGPIO.GPIO.Direction;
import jGPIO.InPin;
import jGPIO.InvalidGPIOException;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owfs.jowfsclient.OwfsException;

public final class Temp implements Runnable {
    
    final String BBB_SYSTEM_TEMP = "/sys/class/hwmon/hwmon0/device/temp1_input";
    final String RPI_SYSTEM_TEMP = "/sys/class/thermal/thermal_zone0/temp";
    final Pattern TEMP_REGEXP = Pattern.compile("(-?)(\\d{1,3})(C|F)");
    
    public void save() {
        if (name != null && !name.equals("")) {
            LaunchControl.addTempToConfig(probeName, name);
        }
    }
    
    public Temp (String input, String aName ) {
    
        
        BrewServer.log.info("Adding" + aName);
        if (input.equalsIgnoreCase("system")) {
            File tempFile = new File(RPI_SYSTEM_TEMP);
            if (tempFile.exists()) {
                fProbe = RPI_SYSTEM_TEMP;
            } else {
                tempFile = new File(BBB_SYSTEM_TEMP);
                if (tempFile.exists()) {
                    fProbe = BBB_SYSTEM_TEMP;
                } else {
                    BrewServer.log.info("Couldn't find a valid system temperature probe");
                    return;
                }
            }
        } else if (LaunchControl.owfsConnection != null) {
            try {
                aName = aName.replace("-", ".");
                BrewServer.log.info("Using OWFS for " + aName + "/temperature");
                if ("" == LaunchControl.readOWFSPath(aName + "/temperature")) {
                    String newAddress[] = aName.split("\\.|-");
                    
                    // OWFS contains "-", W1 contained "."
                    if (newAddress.length == 2 && aName.indexOf("-") != 2) {
                        String devFamily = newAddress[0];
                        StringBuilder devAddress = new StringBuilder();
                        
                        devAddress.append(newAddress[1].subSequence(10, 12));
                        devAddress.append(newAddress[1].subSequence(8, 10));
                        devAddress.append(newAddress[1].subSequence(6, 8));
                        devAddress.append(newAddress[1].subSequence(4, 6));
                        devAddress.append(newAddress[1].subSequence(2, 4));
                        devAddress.append(newAddress[1].subSequence(0, 2));
                        
                        String fixedAddress = devFamily.toString() + "." + devAddress.toString().toUpperCase();
                        
                        System.out.println("Converted address: " + fixedAddress);
                        
                        aName = fixedAddress;
                        if ("" == LaunchControl.readOWFSPath(aName + "/temperature")) {
                            BrewServer.log.severe("This is not a temperature probe " + aName);
                        }
                    } 
                    
                }
                
            } catch ( OwfsException e) {
                BrewServer.log.log(Level.SEVERE, "This is not a temperature probe!", e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            fProbe = null;
        } else {
        
            File probePath = new File("/sys/bus/w1/devices/" + aName + "/w1_slave");
            
            // Lets assume that OWFS has "." separated names
            if (!probePath.exists() && aName.indexOf(".") != 2) {
                String newAddress[] = aName.split("\\.|-");
                
                if (newAddress.length == 2) {
                    String devFamily = newAddress[0];
                    StringBuilder devAddress = new StringBuilder();
                    
                    devAddress.append(newAddress[1].subSequence(10, 12));
                    devAddress.append(newAddress[1].subSequence(8, 10));
                    devAddress.append(newAddress[1].subSequence(6, 8));
                    devAddress.append(newAddress[1].subSequence(4, 6));
                    devAddress.append(newAddress[1].subSequence(2, 4));
                    devAddress.append(newAddress[1].subSequence(0, 2));
                    
                    String fixedAddress = devFamily.toString() + "." + devAddress.toString().toLowerCase();
                    
                    System.out.println("Converted address: " + fixedAddress);
                    
                    aName = fixedAddress;
                    
                    probePath = null;    
                }
                
            } 
            fProbe = "/sys/bus/w1/devices/" + aName + "/w1_slave";
        }
        
        probeName = aName;
        name = input;
        BrewServer.log.info(probeName + " added.");
    }

    public void run() {
        
        
        while(true) {
            if (updateTemp() == -999) {
                if (fProbe == null || fProbe.equals("/sys/class/thermal/thermal_zone0/temp")) {
                    return;
                }
                // Uh(oh no file found, disable output to prevent logging floods
                loggingOn = false;
            } else {
                loggingOn = true;
            }
            
            if (volumeMeasurement) {
                updateVolume();
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public String getProbe() {
        return probeName;
    }
    
    /******
     * Method to take a cutoff value, parse it to the correct scale and then update the cutoffTemp
     * @param cutoffInput String describing the temperature
     */
    public void setCutoffTemp(String cutoffInput) {
        Matcher tempMatcher = TEMP_REGEXP.matcher(cutoffInput);
        
        if (tempMatcher.find()) {
            // We have matched against the TEMP_REGEXP
            String negative = tempMatcher.group(1);
            if (negative == null ) {
                negative = "+";
            }
            
            Double temperature = Double.parseDouble(negative + tempMatcher.group(2));
            String unit = tempMatcher.group(3);
            
            if (unit.equals(this.scale)) {
                cutoffTemp = temperature;
            } else if (unit.equals("F")) {
                cutoffTemp = FtoC(temperature);
            } else if (unit.equals("C")) {
                cutoffTemp = CtoF(temperature);
            }
            
        } else {
            BrewServer.log.severe(cutoffTemp + " doesn't match " + TEMP_REGEXP.pattern());
        }
    }

    // PRIVATE ////
    public String fProbe;
    
    private String name;
    private String probeName;
    private boolean loggingOn = true;
    private String currentError = null;

    private double currentTemp = 0;
    private long currentTime = 0;
    private String scale = "C";
    private double cutoffTemp = -999;
    
    public boolean volumeMeasurement = false;
    public String volumeAddress = null;
    public String volumeOffset = null;
    public int volumeAIN = -1;
    public ConcurrentHashMap<Double, Double> volumeBase = null;
    
    private double currentVolume = 0;
    private String volumeUnit = null; 
    private double volumeConstant = 0;
    private double volumeMultiplier = 0.0;
    private InPin volumePin = null;
    
    public double getTemp() {
        // set up the reader
        if(scale.equals("F")) {
            return getTempF();
        }
        return getTempC();
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String s) {
        if(s.equalsIgnoreCase("F"))
        {
            // Do we need to convert the cutoff temp
            if (cutoffTemp != -999 && !scale.equalsIgnoreCase(s)) {
                cutoffTemp = CtoF(cutoffTemp);
            }
            
            scale = s;
        }
        if(s.equalsIgnoreCase("C"))
        {
            // Do we need to convert the cutoff temp
            if (cutoffTemp != -999 && !scale.equalsIgnoreCase(s)) {
                cutoffTemp = FtoC(cutoffTemp);
            }
            
            scale = s;
        }
        
    }

    public double getTempF() {
        if(scale.equals("F")) {
            return currentTemp;
        }
        return (currentTemp-32) /(9.0*5.0);
    }

    public double getTempC() {
        if(scale.equals("C")) {
            return currentTemp;
        }
        return (9.0/5.0)*currentTemp + 32;
    }
    
    public double FtoC(double currentTemp) {
        return (currentTemp-32) /(9.0*5.0);
    }
    
    public double CtoF(double currentTemp) {
        return (9.0/5.0)*currentTemp + 32;
    }

    public long getTime() {
        return currentTime;
    }

    public double updateTemp() {
        double result = -1L;
        
        if (fProbe == null) {
            result = updateTempFromOWFS();
        } else {
            result = updateTempFromFile();
        }
        
        if (result == -999) {
            return result;
        }
        if(scale.equals("F")) {
            result = (9.0/5.0)*result + 32;
        }
        
        currentTemp = result;
        currentTime = System.currentTimeMillis();
        currentError = null;
    
        if ( cutoffTemp != -999 && currentTemp >= cutoffTemp) {
            BrewServer.log.log(Level.SEVERE, currentTemp + ": ****** CUT OFF TEMPERATURE (" + cutoffTemp + ") EXCEEDED *****");
            System.exit(-1);
        }
        return result;
    }
    
    public double updateTempFromOWFS() {
        // Use the OWFS connection
        double temp = -999;
        String rawTemp = "";
        try {
            rawTemp =  LaunchControl.readOWFSPath(probeName + "/temperature");
            if (rawTemp == null || rawTemp.equals("")) {
                BrewServer.log.severe("Couldn't find the probe " + probeName + " for " + name);
            } else {
                temp = Double.parseDouble(rawTemp);
            }
        } catch (IOException e) {
            currentError = "Couldn't read " + probeName;
            BrewServer.log.log(Level.SEVERE, currentError, e);
        } catch (OwfsException e) {
            currentError = "Couldn't read " + probeName;
            BrewServer.log.log(Level.SEVERE, currentError, e);
            LaunchControl.setupOWFS();
        } catch (NumberFormatException e) {
            currentError = "Couldn't parse" + rawTemp;
            BrewServer.log.log(Level.SEVERE, currentError, e);
        }
        
        return temp;
    }
    
    public double updateTempFromFile() {
        BufferedReader br = null;
        String temp = null;
        
        try {
            br = new BufferedReader(new FileReader(fProbe));
            String line = br.readLine();
            if(line.contains("NO")) {
                // bad CRC, do nothing
                currentError = "Bad CRC from " + fProbe;
            } else if(line.contains("YES")) {
                // good CRC
                line = br.readLine();
                // last value should be t=
                int t = line.indexOf("t=");
                temp = line.substring(t+2);
                double tTemp = Double.parseDouble(temp);
                currentTemp = tTemp/1000;
                currentError = null;
            } else {
                // System Temperature
                currentTemp = Double.parseDouble(line)/1000; 
            }
                
        } catch (IOException ie) {
            if(loggingOn) {
                currentError = "Couldn't find the device under: " + fProbe;
                System.out.println(currentError);
                if (fProbe == RPI_SYSTEM_TEMP) {
                    fProbe = BBB_SYSTEM_TEMP;
                }
            }
            return -999;
        } catch (NumberFormatException nfe) {
            currentError = "Couldn't parse " + temp + " as a double";
            nfe.printStackTrace();
        } finally {
            if(br != null){
                try {
                    br.close();
                } catch (IOException ie) {
                }
            }
        }
        return currentTemp;
    }

    
    public void setupVolumes(String address, String offset, String unit) {
        volumeMeasurement = true;
        volumeUnit = unit;
        volumeAddress = address.replace("-", ".");
        volumeOffset = offset;
        offset = offset.toUpperCase();
        try { 
            BrewServer.log.log(Level.INFO, "Volume ADC at: " + volumeAddress + " - " + offset);
            String temp = LaunchControl.readOWFSPath(volumeAddress + "/volt." + offset);
            if (temp.equals("")) {
                BrewServer.log.severe("Couldn't read the Volume from " + volumeAddress + "/volt. " + offset);
            } else {
                BrewServer.log.log(Level.INFO, "Volume reads " + temp);
            }
        } catch (IOException e) {
            BrewServer.log.log(Level.SEVERE, "IOException when access the ADC over 1wire", e);
        } catch (OwfsException e) {
            BrewServer.log.log(Level.SEVERE, "OWFSException when access the ADC over 1wire", e);
        }
                
        if (volumeConstant != 0.0 && volumeMultiplier != 0.0) {
            BrewServer.log.info("Volume constants and multiplier are good");
        } else {
            BrewServer.log.info("Volume constants and multiplier show there's no change in the pressure sensor");
        }

    }
    
    public void setupVolumes(int analogPin, String unit) throws InvalidGPIOException {
        // start a volume measurement at the same time
        volumeMeasurement = true;
        volumeUnit = unit;
        
        try {
            volumePin = new InPin(analogPin, Direction.ANALOGUE);
        } catch (InvalidGPIOException e) {
            System.out.println("Invalid Analog GPIO specified " + analogPin);
            throw(e);
        }
        
        setupVolume();
        
        if (volumeConstant == 0.0 || volumeMultiplier == 0.0) {
            volumeMeasurement = false;
        }
        
    }
    
    public void setupVolume() {
        if (volumeBase == null) {
            return;
        }
        
        // Calculate the values of b*value + c = volume
        // get the value of c
        volumeConstant = volumeBase.get(0);
        volumeMultiplier = 0.0;
        
        // for the rest of the values
        Iterator it = volumeBase.entrySet().iterator();
        Map.Entry prevPair = null;
        
        if (volumeConstant != 0.0) {
            volumeConstant = 0.0;
        }
        
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (prevPair != null) {
                // diff the pair value and dive by the diff of the key
                Integer keyDiff = (Integer) pairs.getKey() - (Integer) prevPair.getKey();
                Integer valueDiff = ((Integer) pairs.getValue() - (Integer) prevPair.getValue());
                double newMultiplier = ((double)valueDiff)/keyDiff;
                double newConstant = (Integer)pairs.getValue() - ((double)valueDiff*keyDiff);
                
                if (volumeMultiplier != 0.0) {
                    if (newMultiplier != volumeMultiplier) {
                        System.out.println("The newMultiplier isn't the same as the old one, if this is a big difference, be careful! You may need a quadratic!");
                        System.out.println("New: " + newMultiplier + ". Old: " + volumeMultiplier);
                    }            
                } else {
                    volumeMultiplier = newMultiplier;
                }
                
                if (volumeConstant != 0.0) {
                    if (newConstant != volumeConstant) {
                        System.out.println("The new constant isn't the same as the old one, if this is a big difference, be careful! You may need a quadratic!");
                        System.out.println("New: " + newConstant + ". Old: " + volumeConstant);
                    }
                } else {
                    volumeConstant = newConstant;
                }
            }
        }
        
        // we should be done now
        
    }
    
    
    public double updateVolume() {
        try {
            double pinValue = 0;
            if (volumeAIN != -1) {
                pinValue = Integer.parseInt(volumePin.readValue());
            } else if (volumeAddress != null && volumeOffset != null) {
                try {
                    pinValue = Double.parseDouble(LaunchControl.readOWFSPath(volumeAddress + "/volt." + volumeOffset));
                } catch (Exception e) {
                    BrewServer.log.log(Level.SEVERE, "Could not update the volume reading from OWFS", e);
                    
                    LaunchControl.setupOWFS();
                    return 0.0;
                }
                
            } else {
                return 0.0;
            }
            
            // Are we outside of the known range?
            
            Double curKey = null, prevKey = null;
            Double curValue = null, prevValue = null;
            SortedSet<Double> keys = null;
            try {
                keys = Collections.synchronizedSortedSet(new TreeSet<Double>(volumeBase.keySet()));
            } catch (NullPointerException npe) {
                // No VolumeBase setup, so we're probably calibrating
                return pinValue;
            } catch (Exception e) {
                BrewServer.log.log(Level.SEVERE, "Uncaught exception when creating the volume set", e);
                System.exit(-1);
            }
            
            double tVolume = -1;
            
            try  {
                for (Double key: keys) {
                    if(prevKey == null) {
                        prevKey = key;
                        prevValue = volumeBase.get(key);
                        continue;
                    } else if (curKey != null) {
                        prevKey = curKey;
                        prevValue = curValue;
                    }
                    
                    
                    curKey = key;
                    curValue = volumeBase.get(key);
                    
                    if(pinValue >= prevValue && pinValue <= curValue) {
                        // We have a set encompassing the values! assume it's linear
                        double volRange = curKey - prevKey;
                        double readingRange = curValue - prevValue;
                        
                        double ratio = ((double) pinValue - prevValue) /
                                readingRange;
                        
                        double volDiff = ratio * volRange;
                        
                        tVolume = volDiff + prevKey;
                    }
                    
                    
                }
                
                if (tVolume == -1 && curKey != null && prevKey != null) {
                    // Try to extrapolate
                    double volRange = curKey - prevKey;
                    double readingRange = curValue - prevValue;
                    
                    double ratio = ((double) pinValue - prevValue) /
                            readingRange;
                    
                    double volDiff = ratio * volRange;
                    
                    tVolume = volDiff + prevKey;
                }
                
            } catch (NoSuchElementException e) {
                // no more elements
            }
            
            if (tVolume == -1) {
                // try to assume the value
                currentVolume = (pinValue - volumeConstant) * volumeMultiplier;
            } else {
                currentVolume = tVolume;
            }
            
            return pinValue;
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0.0;
    }
    
    public void setVolumeUnit(String unit) {
        volumeUnit = unit;
    }
    
    public void addVolumeMeasurement(double volume) {
        // record 10 readings and average it
        int maxReads = 10;
        int total = 0;
        for (int i = 0; i < maxReads; i++) {
            try {
                try {
                    total += Integer.parseInt(volumePin.readValue());
                    
                } catch (RuntimeException re) {
                    re.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                
            } catch (NumberFormatException  e) {
                // TODO Auto-generated catch block
                System.out.println("Bad Analog input value!");
                return;
            }
            
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
        
        // read in ten values
        double avgValue = (double) total / maxReads;
        
        volumeBase.put(volume, avgValue);
        
        System.out.println("Read " + avgValue + " for " + volume + " " + volumeUnit.toString() );
        return;
    }
    
    public void addVolumeMeasurement(Double key, Double value) {
        BrewServer.log.info("Adding " + key + " with value " + value);
        if (volumeBase == null ) {
            volumeBase = new ConcurrentHashMap<Double, Double>();
        }
        volumeBase.put(key, value);
    }
    
    public double getVolume() {
        if (volumeMeasurement) {
            return currentVolume;
        }
        
        return -1.0;
    }
    
    public String getVolumeUnit() {
        return volumeUnit;
    }

    /*****
     * Helper function to return a map of the current status
     * @return
     */
    public Map<String, Object> getMapStatus() {
        Map<String, Object> statusMap = new HashMap<String, Object>();
        
        statusMap.put("temp", getTemp());
        statusMap.put("elapsed", getTime());
        statusMap.put("scale", getScale());
        
        double tVolume = getVolume();
        if (volumeMeasurement && tVolume != -1.0) {
            statusMap.put("volume", tVolume);
            statusMap.put("volumeUnits", getVolumeUnit());
        }
        
        if (currentError != null) {
            statusMap.put("error", currentError);
        }
        
        return statusMap;
    }
    

    
}

