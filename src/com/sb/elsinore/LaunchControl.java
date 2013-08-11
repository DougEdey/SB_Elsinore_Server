package com.sb.elsinore;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.ini4j.*;
import org.ini4j.ConfigParser.*;

import Cosm.*;

import com.sb.common.ServePID;

public final class LaunchControl {
	public static List<PID> pidList = new ArrayList<PID>(); 
	public static List<Temp> tempList = new ArrayList<Temp>(); 
	private String scale = "F";
	private static BrewDay brewDay = null;

	public static void main(String... arguments) {
		BrewServer.log.info( "Running Brewery Controller." );
		LaunchControl lc = new LaunchControl();
	}

	public LaunchControl() {
		final List<Thread> procList = new ArrayList<Thread>();
	//	final List<PID> pidList = new ArrayList<PID>();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				for(PID n : pidList) {
					if(n != null) {
						n.shutdown();
					}
				}

			}
		});


		readConfig();


		ServerRunner.run(BrewServer.class);
		
		
		// iterate the list of Threads to kick off any PIDs
		Iterator<Temp> iterator = tempList.iterator();
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			Temp tTemp = iterator.next();
			PID tPid = findPID(tTemp.getName());
		}


		System.out.println("Waiting for input...");
		String input = "";
		String[] inputBroken;
		while(true) {
			System.out.print(">");
			input = "";
			 //  open up standard input
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				input = br.readLine();
			} catch (IOException ioe) {
				
				System.out.println("IO error trying to read your input: " + input);
			}
	
			// parse the input and determine where to throw the data
			inputBroken = input.split(" ");
			// is the first value something we recognize?
			if(inputBroken[0].equalsIgnoreCase("quit")) {
				System.out.println("Quitting");
				System.exit(0);
			}
		}
	}
	
	public String getCosmImages(String startDate, String endDate) {
		try {
			if(cosmFeed != null ){
				for(Temp t : tempList) {
					Datastream tData = findDatastream(t.getName());
					if(tData != null) {
						if(startDate == null || endDate == null) {
							cosm.getDatastreamImage(cosmFeed.getId(), t.getName());
						} else {
							cosm.getDatastreamImage(cosmFeed.getId(), t.getName(), startDate, endDate);
						}
					}
				}
			}

		} catch (CosmException e) {
			return "Could not get the images";
		}

		return "Grabbed images";
	}

	public String getCosmXML(String startDate, String endDate) {
		try {
			if(cosmFeed != null ){
				for(Temp t : tempList) {
					Datastream tData = findDatastream(t.getName());
					if(tData != null) {
						if(startDate == null || endDate == null) {
							cosm.getDatastreamXML(cosmFeed.getId(), t.getName());
						} else {
							cosm.getDatastreamXML(cosmFeed.getId(), t.getName(), startDate, endDate);
						}
					}
				}
			}

		} catch (CosmException e) {
			return "Could not get the images";
		}

		return "Grabbed images";
	}

	public static String getControlPage() {
		HashMap<String, String> devList = new HashMap<String, String>();
		 for(Temp t : tempList) {
			PID tPid = findPID(t.getName());
			String type = "Temp";

			if (tPid != null) {
				type = "PID";
			}
		 	devList.put(t.getName(), type);
		 }

		ServePID pidServe = new ServePID(devList);
		return pidServe.getPage();
	}

	public static String getJSONStatus() {
		// get each setting add it to the JSON
		JSONObject rObj = new JSONObject();

		String active_devices = "";

		JSONObject tJSON = null;
		try {
		
		//get the datestamps data 
		
		// iterate the thread lists
		// use the temp list to determine if we have a PID to go with
		for(Temp t : tempList) {
			PID tPid = findPID(t.getName());
			tJSON = new JSONObject();
			if(tPid != null) {
				tJSON.put("mode", tPid.getMode());
				tJSON.put("gpio", tPid.getGPIO());
				tJSON.put("duty", tPid.getDuty());
				tJSON.put("cycle", tPid.getCycle());
				tJSON.put("setpoint", tPid.getSetPoint());
				tJSON.put("p", tPid.getP());
				tJSON.put("i", tPid.getI());
				tJSON.put("d", tPid.getD());
				tJSON.put("status", tPid.getStatus());
			} 
			tJSON.put("temp", t.getTemp());
			tJSON.put("elapsed", t.getTime());
			tJSON.put("scale", t.getScale());
			rObj.put(t.getName(), tJSON);
			
			// update COSM
			if (cosmFeed != null) {
				Datastream tData = findDatastream(t.getName());
				tData.setCurrentValue(Double.toString(t.getTemp()));
				Unit tUnit = new Unit();
				tUnit.setType("temp");
				tUnit.setSymbol(t.getScale());
				tUnit.setLabel("temperature");
				// generate the date time parameters
				DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
				DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");

				Date updateDate = new Date(t.getTime());
				long totSeconds = TimeZone.getDefault().getRawOffset() / 1000; 
				long currSecond = (int)(totSeconds % 60);
				long totMinutes = totSeconds / 60;
				long currMinute = (int)(totMinutes % 60);
				long totHours = totMinutes / 60;

			//	tData.setAt(dFormat.format(updateDate) + "T" + sFormat.format(updateDate) + "Z" + totHours+ ":" + currMinute);
//				tUnit.setAt(dFormat.format(updateDate) + "T" + sFormat.format(updateDate) + "Z" + Calendar.get(Calendar.ZONE_OFFSET));

				tData.setUnit(tUnit);

				
				try {
					cosm.updateDatastream(cosmFeed.getId(), t.getName(), tData);
				} catch (CosmException e) {
					System.out.println("Failed to update datastream: " + e.getMessage());
				}

			}

			if (brewDay != null) {
				rObj.put("brewday", brewDay.brewDayStatus());
			}
		}	
		} catch (JSONException e) {
			return "Failed";
		}
		return rObj.toString();

	}
		

	public void readConfig() {

		// read the config file from the rpibrew.cfg file
		ConfigParser config = new ConfigParser();
		try {
			config.read("rpibrew.cfg");
		} catch (IOException e) {	
			System.out.println("Config file doesn't exist");
			createConfig();
			return;
		}

		List<String> configSections = config.sections();
		
		Iterator<String> iterator = configSections.iterator();
		while (iterator.hasNext()) {
			String temp = iterator.next().toString();
			if(temp.equalsIgnoreCase("general")){
				// parse the general config
				parseGeneral(config, temp);
			} else {
				parseDevice(config, temp);
			}
			
		}

		if (tempList.size() == 0) {
			// get user input
			createConfig();
		}
		
		// Add a new temperature probe for the system
		// input is the name we'll use from here on out
		Temp tTemp = new Temp("system", "");
		tempList.add(tTemp);
		BrewServer.log.info("Adding " + tTemp.getName());
		// setup the scale for each temp probe
		tTemp.setScale(scale);
		// setup the threads
		Thread tThread = new Thread(tTemp);
		tempThreads.add(tThread);
		tThread.start();
	}


	// parse the general section
	private void parseGeneral(ConfigParser config, String input) {
		try {
			if(config.hasSection(input)) {
				if(config.hasOption(input, "scale")) {
					scale = config.get(input, "scale");
				}
				if(config.hasOption(input, "cosm") && config.hasOption(input, "cosm_feed")) {
					startCosm(config.get(input, "cosm"), config.getInt(input, "cosm_feed"));
				} else if (config.hasOption(input, "pachube") && config.hasOption(input, "pachube_feed")) {
					startCosm(config.get(input, "pachube"), config.getInt(input, "pachube_feed"));
				}
			}
		} catch (NoSectionException nse) {
			System.out.print("No Such Section");
			nse.printStackTrace();
		} catch (InterpolationException ie) {
			System.out.print("Int");
			ie.printStackTrace();
		} catch (NoOptionException noe) {
			System.out.print("No Such Option");
			noe.printStackTrace();
		}
	}

	// parse each section
	private void parseDevice(ConfigParser config, String input) {
		String probe = null;
		String gpio = "";
		double duty = 0.0, cycle = 0.0, setpoint = 0.0, p = 0.0, i = 0.0, d = 0.0;
		BrewServer.log.info("Parsing : " + input);
		try {
			if(config.hasSection(input)) {
				if(config.hasOption(input, "probe")) {
					probe = config.get(input, "probe");
				}
				if(config.hasOption(input, "gpio")) {
					gpio = config.get(input, "gpio");
				}
				if(config.hasOption(input, "duty_cycle")) {
					duty = config.getDouble(input, "duty_cycle");
				}
				if(config.hasOption(input, "cycle_time")) {
					cycle = config.getDouble(input, "cycle_time");
				}
				if(config.hasOption(input, "set_point")) {
					setpoint = config.getDouble(input, "set_point");
				}
				if(config.hasOption(input, "k_param")) {
					d = config.getDouble(input, "k_param");
				}
				if(config.hasOption(input, "p_param")) {
					p = config.getDouble(input, "p_param");
				}
				if(config.hasOption(input, "i_param")) {
					i = config.getDouble(input, "i_param");
				}
			}
		} catch (NoSectionException nse) {
			System.out.print("No Such Section");
			nse.printStackTrace();
		} catch (InterpolationException ie) {
			System.out.print("Int");
			ie.printStackTrace();
		} catch (NoOptionException noe) {
			System.out.print("No Such Option");
			noe.printStackTrace();
		}
		
		if(probe != null && !probe.equals("0")) {
			// input is the name we'll use from here on out
			Temp tTemp = new Temp(input, probe);
			tempList.add(tTemp);
			BrewServer.log.info("Adding " + tTemp.getName() + " GPIO is (" + gpio + ")");
			// setup the scale for each temp probe
			tTemp.setScale(scale);
			// setup the threads
			Thread tThread = new Thread(tTemp);
			tempThreads.add(tThread);
			tThread.start();

			if(!gpio.equals("")) {
				BrewServer.log.info("Adding PID with GPIO: " + gpio);
				PID tPID = new PID(tTemp, input, duty, cycle, p, i, d, gpio);
				pidList.add(tPID);
				Thread pThread = new Thread(tPID);
				pidThreads.add(pThread);
				pThread.start();
			}

			// try to createthe data stream
			if(cosm == null || cosmFeed == null) {
				// we have a pacfeed that is valid

			}
		}


	}

	private static Datastream findDatastream(String tag) {
		// iterate the list by tag
		List <Datastream> cList = Arrays.asList(cosmStreams);
		Iterator<Datastream> iterator = cList.iterator();
		Datastream tData = null;
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			tData = iterator.next();

			if(tData.getId().equalsIgnoreCase(tag)) {
				return tData;
			}
		}

		// couldn't find the tag, lets add it
		tData = new Datastream();//cosmFeed.getId(), tag, 0d, 0d, 240d); // always setup for Fahrenheit

		BrewServer.log.info("Creating new feed");
		List<String> lTags = new ArrayList<String> ();

		lTags.add("Elsinore");
		lTags.add("temperature");
		
		String[] aTags = new String[lTags.size()];
		lTags.toArray(aTags);

		tData.setTags(aTags);
		tData.setId(tag);
		try {
			cosm.createDatastream(cosmFeed.getId(), tData);
		} catch (CosmException e) {
			BrewServer.log.info("Failed to create stream:" + e.getMessage() + " - " + cosmFeed.getId());

			return null;
		}
		return tData;
	}


	public static PID findPID(String name) {
		// search based on the input name
		Iterator<PID> iterator = pidList.iterator();
		PID tPid = null;
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			tPid = iterator.next();
			if(tPid.getName().equalsIgnoreCase(name)) {
				return tPid;
			}
		}
		return null;
	}

	private void startCosm(String APIKey, int feedID) {
		BrewServer.log.info("API: " + APIKey + " Feed: " + feedID);
		cosm = new Cosm(APIKey);
		if(cosm == null) {
			// failed
			return;
		}

		// get the data feed
		try {
			cosmFeed = cosm.getFeed(feedID, true);
			BrewServer.log.info("Got " + cosmFeed.getTitle());
		} catch (CosmException e) {
			return;
		}

		// get the list of feeds
		
		cosmStreams = cosmFeed.getDatastreams();
		return;
	}
	
	public static BrewDay getBrewDay() {
		if (brewDay == null) {
			brewDay = new BrewDay();
		}

		return brewDay;
	}

	private void createConfig() {
		// try to access the list of 1-wire devices
		File w1Folder = new File("/sys/bus/w1/devices/");
		File[] listOfFiles = w1Folder.listFiles();
		for ( File currentFile : listOfFiles) {
			if (currentFile.isDirectory() && !currentFile.getName().startsWith("w1_bus_master")) {
				// got a directory, check for a temp
				System.out.println("Checking for " + currentFile.getName());
				Temp currentTemp = new Temp(currentFile.getName(), currentFile.getName());
				tempList.add(currentTemp);
				// setup the scale for each temp probe
				currentTemp.setScale(scale);
				// setup the threads
				Thread tThread = new Thread(currentTemp);
				tempThreads.add(tThread);
				tThread.start();
			}
		}

		if (tempList.size() == 0) {
			System.out.println("Could not find any one wire devices, please check you have the correct modules setup");
			System.exit(0);
		}
		
		displaySensors();

		ConfigParser config = new ConfigParser();

		System.out.println("Select the input");
		String input = "";
		String[] inputBroken;
		while(true) {
			System.out.print(">");
			input = "";
			 //  open up standard input
			input = readInput();
			// parse the input and determine where to throw the data
			inputBroken = input.split(" ");
			// is the first value something we recognize?
			if(inputBroken[0].equalsIgnoreCase("quit")) {
				System.out.println("Quitting");
				System.out.println("Updating config file, please check it in rpibrew.cfg.new");

				File configOut = new File("rpibrew.cfg.new");
				try {
					config.write(configOut);
				} catch (IOException ioe) {
					System.out.println("Could not update file");
				}
				System.exit(0);
			}
			if(inputBroken[0].startsWith("r") || inputBroken[0].startsWith("R")) {
				System.out.println("Refreshing...");
				displaySensors();
			}

			int selector = 0;
			try {
				selector = Integer.parseInt(inputBroken[0]);
				// we parsed ok
				if (selector > 0 && selector <= tempList.size()) {
					// we have a valid input
					String name = "";
					String GPIO = "";

					if (inputBroken.length > 1) {
						name = inputBroken[1];
					} else {
						System.out.println("Please name the input: ");
						input = readInput();
						inputBroken = input.split(" ");
						if(inputBroken.length > 0) {
							name = inputBroken[0];
						}
					}

					displayGPIO();

					System.out.println( "Adding " + tempList.get(selector-1).getName() + " as " + name);
					System.out.println("To make this a PID add the GPIO number now, or anything else for a temp probe only: ");
					input = readInput();
					try {
						GPIO = input;
						// try to parse the GPIO to determine the type
						Pattern pinPattern = Pattern.compile("(GPIO)([0-9])_([0-9]*)");
						Pattern pinPatternAlt = Pattern.compile("(GPIO)?([0-9]*)");
						
						Matcher pinMatcher = pinPattern.matcher(GPIO);
						
						if(pinMatcher.groupCount() > 0) {
							// Beagleboard style input
							System.out.println("Matched GPIO pinout for Beagleboard: " + input + ". OS: " + System.getProperty("os.name"));
						} else {
							pinMatcher = pinPatternAlt.matcher(GPIO);
							if(pinMatcher.groupCount() > 0) {
								System.out.println("Direct GPIO Pinout detected. OS: " + System.getProperty("os.name"));
							} else {
								System.out.println("Could not match the GPIO!");							
							}
						}
						
					} catch (NumberFormatException n) {
						// not a GPIO

					}

					// Check for Valid GPIO values
					tempList.get(selector-1).setName(name);
					if (GPIO != "GPIO1" && GPIO != "GPIO7") {
						addPIDToConfig(config, tempList.get(selector-1).getProbe(), name, GPIO);
					} else {
						System.out.println("No valid GPIO Value found, adding as a temperature only probe");
						addTempToConfig(config, tempList.get(selector-1).getProbe(), name);
					}

				} else {
					System.out.println( "Input number (" + input + ") is not valid\n");
				}
			} catch (NumberFormatException e) {
				// not a number
			}
		}

	}

	private static Cosm cosm = null;
	private static Feed cosmFeed = null;
	private static Datastream[] cosmStreams = null;

	private List<Thread> tempThreads = new ArrayList<Thread>();
	private List<Thread> pidThreads = new ArrayList<Thread>();

	private String readInput() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input = "";
		try {
			input = br.readLine();
		} catch (IOException ioe) {
			
			System.out.println("IO error trying to read your input: " + input);
		}
		return input;
	}
	
	private void displaySensors() {
		// iterate the list of temperature Threads to get values
		Integer i = 1;
		Iterator<Temp> iterator = tempList.iterator();
		System.out.println("\n\nNo config data found as usable. Select a input and name it (i.e. \"1 kettle\" no quotes) or type \"r\" to refresh:");
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			Temp tTemp = iterator.next();
			System.out.println( i.toString() + ") " + tTemp.getName() + " = " + tTemp.updateTemp());
			i++;
		}
	}
	
	private void displayGPIO() {
		System.out.println("GPIO Values, use the outer values");
		System.out.println(" USE     |PHYSICAL |USE");
		System.out.println("  NA     |  1 * 2  | NA");
		System.out.println(" GPIO0/2 |  3 * 4  | NA");
		System.out.println(" GPIO1/3 |  5 * 6  | NA");
		System.out.println("  NA     |  7 * 8  | GPIO14");
		System.out.println("  NA     |  9 * 10 | GPIO15");
		System.out.println("  GPIO17 | 11 * 12 | GPIO18 ");
		System.out.println("GPIO21/27| 13 * 14 | NA ");
		System.out.println("  GPIO22 | 15 * 16 | GPIO23");
		System.out.println(" NA      | 17 * 18 | GPIO24");
		System.out.println(" GPIO10  | 19 * 20 | NA ");
		System.out.println(" GPIO9   | 21 * 22 | GPIO25");
		System.out.println(" GPIO11  | 23 * 24 | GPIO8 ");
		System.out.println(" NA      | 25 * 26 | GPIO7 ");
	}

	private void addTempToConfig(ConfigParser config, String device, String name) {
		try {
		  if (!config.hasSection(name) ) {
		  		// update this one
				config.addSection(name);
		  }

		  config.set(name, "probe", device);
		} catch (Exception e) {
		}
	}

	private void addPIDToConfig(ConfigParser config, String device, String name, String GPIO) {
		try {
		  if (!config.hasSection(name) ) {
		  		// update this one
				config.addSection(name);
		  }

		  config.set(name, "probe", device);
		  config.set(name, "gpio", GPIO);
		  config.set(name, "k_param", 0);
		  config.set(name, "i_param", 0);
		  config.set(name, "d_param", 0);
		} catch (Exception e) {
			return;
		}
	}
}
