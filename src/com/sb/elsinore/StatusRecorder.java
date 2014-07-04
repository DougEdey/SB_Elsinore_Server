package com.sb.elsinore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 *
 */
public class StatusRecorder implements Runnable
{

    private JSONObject lastStatus = null;
    private long lastStatusTime = 0;
    private String logFile = null;
    private Thread thread;
    private static final long SLEEP = 1000 * 5; // 5 seconds - is this too fast?
    private long startTime = 0;
    

    public void start()
    {
        if (thread == null || !thread.isAlive())
        {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

    }

    public void stop()
    {
        if (thread != null)
        {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void run()
    {
        //This will store multiple logs - one for raw data, one for each series (duty & temperature per vessel)
        //For now - we'll store Duty, temperature vs time
        
        //Assume new logs on each run
        startTime = System.currentTimeMillis();
        
        String directory = "graph-data/"+startTime+"/";
        
        File directoryFile = new File(directory);
        directoryFile.mkdirs();
        
        //Generate a new log file under the current directory
        logFile = directory+"raw.log";
        
        File file = new File(this.logFile);
        boolean fileExists = file.exists();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        
        
        
        try
        {
            while (true)
            {
                //Just going to record when something changes
                String status = LaunchControl.getJSONStatus();
                JSONObject newStatus = (JSONObject) JSONValue.parse(status);

                if (lastStatus == null || isDifferent(lastStatus, newStatus))
                {
                    
                    
                    //For now just log the whole status and we'll figure it out later. Eventually we may want multiple logs, etc. 
                    writeToLog(newStatus, fileExists);
                    
                    Date now = new Date();
                    
                   
                    if( lastStatus != null && now.getTime() - lastStatusTime > SLEEP )
                    {
                        //Print out a point before now to make sure the plot lines are correct
                        printJsonToCsv(new Date(now.getTime()-SLEEP), lastStatus, directory);
                        
                    }
                    
                    printJsonToCsv(now, newStatus, directory);
                    lastStatus = newStatus;
                    lastStatusTime = now.getTime();
                    
                    fileExists = true;
                }

                Thread.sleep(SLEEP);

            }
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
            //Don't do anything, this is how we close this out.
        }

    }
    
    protected void printJsonToCsv(Date now, JSONObject newStatus, String directory)
    {
        //Now look for differences in the temperature and duty
                    JSONArray vessels = (JSONArray)newStatus.get("vessels");
                    for (int x=0;x<vessels.size();x++)
                    {
                        JSONObject vessel = (JSONObject) vessels.get(x);
                        if( vessel.containsKey("name") )
                        {
                            String name = vessel.get("name").toString();
                            
                            if( vessel.containsKey("tempprobe"))
                            {
                                String temp = ((JSONObject)vessel.get("tempprobe")).get("temp").toString();
                                File tempFile = new File(directory+name+"-temp.csv");
                                //appendToLog(tempFile, '"'+sdf.format(now)+"\","+temp+"\r\n");
                                appendToLog(tempFile, now.getTime()+","+temp+"\r\n");
                            }
                            
                            if( vessel.containsKey("pidstatus"))
                            {
                                JSONObject pid = (JSONObject)vessel.get("pidstatus");
                                String duty = "0";
                                if( pid.containsKey("actualduty"))
                                {
                                    duty = pid.get("actualduty").toString();
                                }
                                else if ( !pid.get("mode").equals("off"))
                                {
                                    duty = pid.get("duty").toString();
                                }
                                File dutyFile = new File(directory+name+"-duty.csv");
                                //appendToLog(dutyFile, '"'+sdf.format(now)+"\","+duty+"\r\n");
                                appendToLog(dutyFile, now.getTime()+","+duty+"\r\n");
                            }
                            
                        }
                    }
    }
    
    protected void appendToLog(File file, String toAppend)
    {
        FileWriter fileWriter = null;
        try
        {
            fileWriter = new FileWriter(file, true);
            fileWriter.write(toAppend);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            //TODO Log something
        }
        finally
        {
            try
            {
                if (fileWriter != null)
                {
                    fileWriter.close();
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }
    
    protected void writeToLog(JSONObject status, boolean fileExists)
    {
        String append = fileExists?",":"[" + status.toJSONString();
        appendToLog(new File(this.logFile), append);
    }

    protected boolean isDifferent(JSONObject previous, JSONObject current)
    {
        
        if( previous.size() != current.size())
        {
            return true;
        }
        
        
        for (Iterator it =  previous.keySet().iterator(); it.hasNext();)
        {
            Object key = it.next();
            if( !"elapsed".equals(key) )
            {
                Object previousValue = previous.get(key);
                Object currentValue = current.get(key);

                if( compare(previousValue, currentValue) )
                {
                    return true;
                }
            }
               
        }
        
        return false;
    }
    
    protected boolean isDifferent(JSONArray previous, JSONArray current)
    {
        
        
        if( previous.size() != current.size())
        {
            return true;
        }
        
        
        for (int x=0;x<previous.size();x++)
        {
            Object previousValue = previous.get(x);
            Object currentValue = current.get(x);
            
            if( compare(previousValue, currentValue) )
            {
                return true;
            }

        }
        
        return false;
    }
    
    protected boolean compare(Object previousValue, Object currentValue)
    {
        if( previousValue instanceof JSONObject && currentValue instanceof JSONObject)
        {
            if( isDifferent((JSONObject) previousValue, (JSONObject) currentValue))
            {
                return true;
            }
        }
        else if( previousValue instanceof JSONArray && currentValue instanceof JSONArray)
        {
            if( isDifferent((JSONArray) previousValue, (JSONArray) currentValue))
            {
                return true;
            }
        }
        else
        {
            if( !previousValue.equals(currentValue))
            {
                return true;
            }
        }
        
        return false;
    }
    
    
    
}
