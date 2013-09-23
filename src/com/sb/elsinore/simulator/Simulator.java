package com.sb.elsinore.simulator;

import asg.cliche.Command;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import com.sb.elsinore.BrewServer;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.ini4j.ConfigParser;

/**
 *
 */
public class Simulator
{

    private double kettleGallons = 12;
    private double kettleWattage = 5500;
    private double kettleTemp = 65;
    private String kettleGPIO = "gpio6";
    private String kettleOneWire = "28-000003608f6f";
    private String rootPath = "/mock";
    private Burner burner;

    public Simulator() 
    {
        BrewServer.log.setLevel(Level.OFF);
        // read the config file from the simulator.cfg file
        ConfigParser config = new ConfigParser();
        try
        {
            config.read("simulator.cfg");
            kettleGallons = config.getDouble("kettle", "gallons");
            kettleWattage = config.getDouble("kettle", "watts");
            kettleTemp = config.getDouble("kettle", "temp");
            kettleGPIO = config.get("kettle", "gpio");
            kettleOneWire = config.get("kettle", "onewire");
            rootPath = config.get("system", "path");
        }
        catch (Exception e)
        {
            System.out.println("Couldn't find configuration or configuration was out of date.  Making a new configuration");
            
            writeConfig();
        }
    }
    
    private void writeConfig()
    {
        try
            {
                ConfigParser config = new ConfigParser();
                config.addSection("system");
                config.set("system", "path", rootPath);
                config.addSection("kettle");
                config.set("kettle", "gallons", kettleGallons);
                config.set("kettle", "watts", kettleWattage);
                config.set("kettle", "temp", kettleTemp);
                config.set("kettle", "gpio", kettleGPIO);
                config.set("kettle", "onewire", kettleOneWire);
                config.write(new File("simulator.cfg"));
            }
            catch(Exception e2)
            {
                System.out.println("Unable to initialize a new configuration under: "+new File("simulator.cfg").getAbsolutePath());
                System.out.println("Exiting...");
                System.exit(-1);
            }
    }

    @Command
    public void start()
    {
        System.out.println("Starting Kettle.  Type 'stop' to end the simulation.");
        burner = new Burner(rootPath, kettleGallons, kettleWattage, kettleTemp, kettleGPIO, kettleOneWire);
        burner.setLogStateChange(true);
        burner.start();
    }

    @Command
    public void stop()
    {
        burner.setRunning(false);
    }
    
    public void printConfig()
    {
        System.out.println("Configuration");
        System.out.println("System - Path: "+rootPath);
        System.out.println("Kettle with "+kettleGallons+" gallons of water currently at "+kettleTemp+"F, powered by a "+kettleWattage+"W element on GPIO: "+kettleGPIO+" and temperature measured 1-wire device: "+kettleOneWire );
        
    }
    
    public void runSimulator() throws IOException, Exception
    {
        Shell shell = ShellFactory.createConsoleShell("simulator", "", this);

        System.out.println("Kettle Simulator");
        System.out.println("================");
        System.out.println("type 'exit' at any time to quit");
        System.out.println("type '?h' for help");
        System.out.println("");
        printConfig();
        
        System.out.println("");
        System.out.println("Commands:");
        shell.processLine("?l");
        shell.commandLoop();
    }

    public static void main(String[] args) throws IOException, Exception 
    {
        new Simulator().runSimulator();
    }

    /**
     * @return the kettleGallons
     */
    public double getKettleGallons()
    {
        return kettleGallons;
    }

    /**
     * @param kettleGallons the kettleGallons to set
     */
    @Command(abbrev = "g", name = "gallons")
    
    public void setKettleGallons(double kettleGallons)
    {
        this.kettleGallons = kettleGallons;
        
        writeConfig();
        printConfig();
    }

    /**
     * @return the kettleWattage
     */
    public double getKettleWattage()
    {
        return kettleWattage;
    }

    /**
     * @param kettleWattage the kettleWattage to set
     */
    @Command(abbrev = "w", name = "watts")
    public void setKettleWattage(double kettleWattage)
    {
        this.kettleWattage = kettleWattage;
        writeConfig();
        printConfig();
    }

    /**
     * @return the kettleTemp
     */
    public double getKettleTemp()
    {
        return kettleTemp;
    }

    /**
     * @param kettleTemp the kettleTemp to set
     */
    @Command(abbrev = "t", name = "temp")
    public void setKettleTemp(double kettleTemp)
    {
        this.kettleTemp = kettleTemp;
        writeConfig();
        printConfig();
    }

    /**
     * @return the kettleGPIO
     */
    public String getKettleGPIO()
    {
        return kettleGPIO;
    }

    /**
     * @param kettleGPIO the kettleGPIO to set
     */
    @Command(abbrev = "gp", name = "gpio")
    public void setKettleGPIO(String kettleGPIO)
    {
        this.kettleGPIO = kettleGPIO;
        writeConfig();
        printConfig();
    }

    /**
     * @return the kettleOneWire
     */
    public String getKettleOneWire()
    {
        return kettleOneWire;
    }

    /**
     * @param kettleOneWire the kettleOneWire to set
     */
    @Command(abbrev = "ow", name = "1-wire")
    public void setKettleOneWire(String kettleOneWire)
    {
        this.kettleOneWire = kettleOneWire;
        writeConfig();
        printConfig();
    }

    /**
     * @return the rootPath
     */
    public String getRootPath()
    {
        return rootPath;
    }

    /**
     * @param rootPath the rootPath to set
     */
    @Command(abbrev = "p", name = "path")
    public void setRootPath(String rootPath)
    {
        this.rootPath = rootPath;
        writeConfig();
        printConfig();
    }
}
