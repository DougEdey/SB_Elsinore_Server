package com.sb.elsinore.simulator;

import com.sb.elsinore.Temp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Burner extends Thread
{
    //Read this gpio to fire the heater

    private String heaterPath = "/sys/class/gpio/gpio0/value";
    //Read and then increase this based on time
    private String thermometerPath = "/sys/bus/w1/devices/foo/w1_slave";
    private double gallons;
    private double watts;
    private boolean on = false;
    private String probe;

    public Burner()
    {
        this.setDaemon(true);
    }

    public Burner(double gallons, double watts, double initialTemp, String gpio, String probe)
    {
        this.gallons = gallons;
        this.watts = watts;
        thermometerPath = "/sys/bus/w1/devices/" + probe + "/w1_slave";
        heaterPath = "/sys/class/gpio/gpio" + gpio + "/value";
        this.probe = probe;
        //TODO Initialize Paths
        File therm = new File(thermometerPath);
        therm.getParentFile().mkdirs();
        
        File heater = new File(heaterPath);
        heater.getParentFile().mkdirs();
        
        writeTemp(initialTemp);
        this.setDaemon(true);
    }

    public void run()
    {
        long timeCheck = System.currentTimeMillis();
        while (true)
        {

            int currentValue = 0;
            try
            {
                FileInputStream fis = new FileInputStream(heaterPath);
                currentValue = fis.read();
                currentValue = currentValue - 48;
                //System.out.println(currentValue);
                fis.close();
            }
            catch (FileNotFoundException ex)
            {
                //ex.printStackTrace();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }

            setOn(currentValue == 1);

            long newTimeCheck = System.currentTimeMillis();

            if (currentValue == 1)
            {
                double increase = increaseTempC(newTimeCheck - timeCheck);
                Temp temp = new Temp(probe, probe);
                double curTemp = temp.updateTemp();
                curTemp += increase;
                writeTemp(curTemp);


            }

            timeCheck = newTimeCheck;



            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Burner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public double increaseTempC(long ms)
    {

        //1 kw / hr = 3414 BTU
        // 1 BTU = 1 lb water up one degree F
        // water is 8.3 lb/gallon

        double btuPerMS = watts * 3.414d / (60 * 60 * 1000);

        double btuGained = btuPerMS * ms;

        double lbsOfWater = gallons * 8.3;

        double tempIncrease = btuGained / lbsOfWater;

        //Convert to C
        tempIncrease = tempIncrease * (9.0 / 5.0);


        return tempIncrease;



    }

    /**
     * @return the on
     */
    public boolean isOn()
    {
        return on;
    }

    /**
     * @param on the on to set
     */
    public void setOn(boolean on)
    {
        this.on = on;
    }

    private void writeTemp(double curTemp)
    {
        String output = "YES\r\nt=" + curTemp * 1000;


        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(thermometerPath);
            fos.write(output.getBytes());
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(Burner.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex)
        {
            Logger.getLogger(Burner.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch (IOException ex)
            {
                Logger.getLogger(Burner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
