package com.sb.elsinore;
import jGPIO.GPIO;
import jGPIO.InvalidGPIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ini4j.ConfigParser;
import org.ini4j.ConfigParser.DuplicateSectionException;
import org.ini4j.ConfigParser.InterpolationException;
import org.ini4j.ConfigParser.NoOptionException;
import org.ini4j.ConfigParser.NoSectionException;
import org.json.simple.JSONObject;
import org.owfs.jowfsclient.Enums.OwPersistence;
import org.owfs.jowfsclient.OwfsConnection;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;

import Cosm.Cosm;
import Cosm.CosmException;
import Cosm.Datastream;
import Cosm.Feed;
import Cosm.Unit;

import com.sb.common.ServePID;

public final class LaunchControl {
	/* List of PIDs, Temperatures, and Pump objects */
	public static List<PID> pidList = new ArrayList<PID>(); 
	public static List<Temp> tempList = new ArrayList<Temp>();
	public static List<Pump> pumpList = new ArrayList<Pump>();
	
	/* Temperature and PID threads */
	private List<Thread> tempThreads = new ArrayList<Thread>();
	private List<Thread> pidThreads = new ArrayList<Thread>();
	
	/* ConfigParser details, need to investigate moving to a better ini parser */
	private static ConfigParser config = null;
	private static String configFileName = "rpibrew.cfg";
	
	/* Volume Units here are to be used in the future for conversion */
	public final class volumeUnits {
		public final String US_GALLONS = "US Gallons";
		public final String UK_GALLONS = "UK Gallons";
		public final String LITRES = "Litres";
	}

	/* Private fields to hold data for various functions */
	/* COSM stuff, probably unused by most people */
	private static Cosm cosm = null;
	private static Feed cosmFeed = null;
	private static Datastream[] cosmStreams = null;
	
	private String scale = "F";
	private static BrewDay brewDay = null;
	/* One Wire File System Information */
	public static OwfsConnection owfsConnection = null;
	public static boolean useOWFS = false;
	private static String owfsServer = "localhost";
	private static int owfsPort = 2121;

	/*****
	 * Main method to launch the brewery
	 * @param arguments
	 */
	public static void main(String... arguments) {
		BrewServer.log.info( "Running Brewery Controller." );
		LaunchControl lc = new LaunchControl();
	}

	/* Constructor */
	public LaunchControl() {
		
		// Create the shutdown hooks for all the threads to make sure we close off the GPIO connections
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				for(PID n : pidList) {
					if(n != null) {
						n.shutdown();
					}
				}

			}
		});

		// See if we have an active confiration file
		readConfig();

		// Debug info before launching the BrewServer itself
		BrewServer.log.log(Level.INFO, "CONFIG READ COMPLETED***********");
		ServerRunner.run(BrewServer.class);
		
		// iterate the list of Threads to kick off any PIDs
		Iterator<Temp> iterator = tempList.iterator();
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			Temp tTemp = iterator.next();
			findPID(tTemp.getName());
		}

		// Old way to close off the System
		System.out.println("Waiting for input... Type 'quit' to exit");
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
	
	/************
	 * Start the COSM Connection
	 * @param APIKey User APIKey from COSM
	 * @param feedID The FeedID to get
	 */
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

	/*****
	 * Create an image from a COSM Feed
	 * @param startDate The start date to get the image from
	 * @param endDate The end date to get the image from
	 * @return A string indicating the image status
	 */
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

	/*********
	 * Get the XML from a COSM feed
	 * @param startDate
	 * @param endDate
	 * @return
	 */
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

	/* Call to generate the HTML to be served back to the server */
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

		ServePID pidServe = new ServePID(devList, pumpList);
		return pidServe.getPage();
	}

	/******
	 * Get the JSON Output String for the current Status of the PIDs, Temps, Pumps, etc...
	 * @return
	 */
	public static String getJSONStatus() {
		// get each setting add it to the JSON
		JSONObject rObj = new JSONObject();
		JSONObject tJSON = null;
	
		// iterate the thread lists
		// use the temp list to determine if we have a PID to go with
		for(Temp t : tempList) {
			PID tPid = findPID(t.getName());
			tJSON = new JSONObject();
			if(tPid != null) {
				tJSON.put("mode", tPid.getMode());
				tJSON.put("gpio", tPid.getGPIO());
				// hack to get the real duty out
				if(tPid.getMode().contains("auto")) {
					tJSON.put("actualduty", tPid.heatSetting.calculatedDuty);
				}
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
			
			double tVolume = t.getVolume();
			if (t.volumeMeasurement && tVolume != -1.0) {
				tJSON.put("volume", tVolume);
				tJSON.put("volumeUnits", t.getVolumeUnit());
			}
			
			rObj.put(t.getName(), tJSON);
			
			// update COSM
			if (cosmFeed != null) {
				Datastream tData = findDatastream(t.getName());
				tData.setCurrentValue(Double.toString(t.getTemp()));
				Unit tUnit = new Unit();
				tUnit.setType("temp");
				tUnit.setSymbol(t.getScale());
				tUnit.setLabel("temperature");
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
		
		// generate the list of pumps
		if (pumpList != null && pumpList.size() > 0) {
			tJSON = new JSONObject();
			
			for(Pump p : pumpList) {
				tJSON.put(p.getName(), p.getStatus());
			}
			
			rObj.put("pumps", tJSON);
		}
		return rObj.toString();
	}
		

	/*********
	 * Read the configuration file
	 */
	public void readConfig() {

		// read the config file from the rpibrew.cfg file
		if (config == null) {
			initializeConfig();
		}
		
		try {
			config.read(configFileName);
		} catch (IOException e) {	
			System.out.println("Config file doesn't exist");
			createConfig();
			return;
		}

		List<String> configSections = config.sections();
		
		Iterator<String> iterator = configSections.iterator();
		while (iterator.hasNext()) {
			String temp = iterator.next().toString();;
			if(temp.equalsIgnoreCase("general")){
				// parse the general config
				parseGeneral(config, temp);
			} else if(temp.equalsIgnoreCase("pumps")){
				// parse the pump config
				parsePumps(config, temp);
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
		try {
			if(config.hasOption("general", "system_temp") && config.getBoolean("general", "system_temp")) {
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
		} catch (NoSectionException e) {
			// impossible
		} catch (NoOptionException e) {
			// Impossible
		} catch (InterpolationException e) {
			// Impossible
		}
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
				
				if (config.hasOption(input, "owfs_server") && config.hasOption(input, "owfs_port")) {
	
					owfsServer = config.get(input, "owfs_server");
					owfsPort = config.getInt(input, "owfs_port");
					BrewServer.log.log(Level.INFO, "Setup OWFS at "+  owfsServer + ":" + owfsPort);
					
					setupOWFS();
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

	/*****
	 * Parse the list of pumps
	 * @param config The config Parser object to use
	 * @param input the name of the section to parse
	 */
	private void parsePumps(ConfigParser config, String input) {
		try {
			List<String> pumps = config.options(input);
			for(String pumpName : pumps) {
				String gpio = config.get(input, pumpName);
				try {
					pumpList.add(new Pump(pumpName, gpio));
				} catch (InvalidGPIOException e) {
					System.out.println("Invalid GPIO (" + gpio + ") detected for pump " + pumpName);
					System.out.println("Please fix the config file before running");
					System.exit(-1);
				}
			}
		} catch (NoSectionException nse) {
			// we shouldn't get here!
			nse.printStackTrace();
		} catch (NoOptionException e) {
			// We shouldn't get here!
			e.printStackTrace();
		} catch (InterpolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// parse each section
	private void parseDevice(ConfigParser config, String input) {
		String probe = null;
		String gpio = "";
		String volumeUnits = "Litres";
		String dsAddress = null, dsOffset = null;
		HashMap<Double, Double> volumeArray = new HashMap<Double, Double>();
		double duty = 0.0, cycle = 0.0, setpoint = 0.0, p = 0.0, i = 0.0, d = 0.0;
		int analoguePin = -1;
		
		BrewServer.log.info("Parsing : " + input);
		try {
			if (config.hasSection(input)) {
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
				if(config.hasOption(input, "derivative")) {
					d = config.getDouble(input, "derivative");
				}
				if(config.hasOption(input, "proportional")) {
					p = config.getDouble(input, "proportional");
				}
				if(config.hasOption(input, "integral")) {
					i = config.getDouble(input, "integral");
				}
			}
			
			// Check to see if there is a volume section
			String volumeSection = input + "-volume";
			if (config.hasSection(volumeSection)) {
				List<String> volumeOptions = config.options(volumeSection);
				
				// Whilst we really want a 0th element, lets just dump this data into a list
				for (String curOption : volumeOptions) {
					if ( curOption.equalsIgnoreCase("unit")) {
						volumeUnits = config.get(volumeSection, curOption);
						continue;
					} 
					
					if ( curOption.equalsIgnoreCase("pin")) {
						analoguePin = config.getInt(volumeSection, curOption);
						continue;
					} 
					
					if ( curOption.equalsIgnoreCase("address")) {
						dsAddress = config.get(volumeSection, curOption);
						continue;
					} 
					if ( curOption.equalsIgnoreCase("offset")) {
						dsOffset = config.get(volumeSection, curOption);
						continue;
					} 
					
					try {
						double volValue = Double.parseDouble(curOption);
						double volReading = config.getDouble(volumeSection, curOption);
						volumeArray.put(volValue, volReading);
						
						// we can parse this as an integer
					} catch (NumberFormatException e) {
						System.out.println("Could not parse " + curOption + " as an integer");
					}
					
				}
				
				if (volumeUnits == null) {
					System.out.println("Couldn't find a volume unit for " + input);
					volumeArray = null;
				}
				
				if (volumeArray != null && volumeArray.size() < 3) {
					System.out.println("Not enough volume data points, " + volumeArray.size() + " found");
					volumeArray = null;
				} else if (volumeArray == null) {
					// we don't have a basic level, not implemented yet, math is hard
					System.out.println("No Volume Presets, check your config or rerun the setup!");
					// otherwise we are OK
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
		
		// Startup the thread
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

			if (analoguePin != -1 ) {
				try {
					tTemp.setupVolumes(analoguePin, volumeUnits);
				} catch (InvalidGPIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (dsAddress != null && dsOffset != null) {
				tTemp.setupVolumes(dsAddress, dsOffset, volumeUnits);
			} else { 
				return;
			}
			
			if (volumeArray != null && volumeArray.size() >= 3) {
				Iterator<Entry<Double, Double>> volIter = volumeArray.entrySet().iterator();
				
				while (volIter.hasNext()) {
					Entry<Double, Double> entry = volIter.next();
					tTemp.addVolumeMeasurement(entry.getKey(), entry.getValue());
				}
			}
			
			// try to create the data stream
			if(cosm == null || cosmFeed == null) {
				// we have a pacfeed that is valid

			}
		}


	}

	/******
	 * 
	 * Search for a Datastream in cosm based on a tag
	 */
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


	/******
	 * Find the PID in the current list
	 * @param name The PID to find
	 * @return The PID object
	 */
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

	/**************
	 * Find the Pump in the current list
	 * @param name The pump to find
	 * @return return the PUMP object
	 */
	public static Pump findPump(String name) {
		// search based on the input name
		Iterator<Pump> iterator = pumpList.iterator();
		Pump tPump = null;
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			tPump = iterator.next();
			if(tPump.getName().equalsIgnoreCase(name)) {
				return tPump;
			}
		}
		return null;
	}
	
	/********
	 * Get the BrewDay object
	 * @return
	 */
	public static BrewDay getBrewDay() {
		if (brewDay == null) {
			brewDay = new BrewDay();
		}

		return brewDay;
	}
	
	/******
	 * List the One Wire devices from the standard one wire file system
	 * in /sys/bus/w1/devices, basic access
	 */
	private void listOneWireSys() {
		// try to access the list of 1-wire devices
		File w1Folder = new File("/sys/bus/w1/devices/");
		if (!w1Folder.exists()) {
			System.out.println("Couldn't read the one wire devices directory!");
			System.exit(-1);
		}
		File[] listOfFiles = w1Folder.listFiles();
		
		if (listOfFiles.length == 0) {
			System.out.println("No 1Wire probes found! Please check your system!");
			System.exit(-1);
		}
		
		// Display what we found
		for ( File currentFile : listOfFiles) {
			if (currentFile.isDirectory() && !currentFile.getName().startsWith("w1_bus_master")) {
				// Check to see if theres a non temp probe (DS18x20)
				if (!currentFile.getName().contains("/28")) {
					System.out.println("Detected a non temp probe, do you want to switch to OWFS? [y/N]");
					String t = readInput();
					if (t.toLowerCase().startsWith("y")) {
						if (owfsConnection == null) {
							createOWFS();
						}

						useOWFS = true;
						tempList.clear();
						listOWFSDevices();
						return;
					}
				}
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
	}
	
	/**********
	 * Setup the configuration, we get here if the configuration file doesn't exist
	 */
	private void createConfig() {
		
		if (useOWFS) {
			listOWFSDevices();
		} else {
			listOneWireSys();
		}
		if (tempList.size() == 0) {
			System.out.println("Could not find any one wire devices, please check you have the correct modules setup");
			System.exit(0);
		}
		
		if (config == null) {
			initializeConfig();
		}
		
		displaySensors();
		System.out.println("Select the input, enter \"r\" to refresh, or use \"pump <name> <gpio>\" to add a pump");
		System.out.println("Type \"volume\" to start volume calibration");
		String input = "";
		String[] inputBroken;
		
		
		/******
		 * REPL - not well written, but it works
		 * TODO: REFACTOR THIS SHIT
		 */
		while(true) {
			System.out.print(">");
			input = "";
			 //  open up standard input
			input = readInput();
			// parse the input and determine where to throw the data
			inputBroken = input.split(" ");
			// is the first value something we recognise?
			if(inputBroken[0].equalsIgnoreCase("quit")) {
				saveSettings();
				System.out.println("Quitting");
				System.out.println("Updating config file, please check it in rpibrew.cfg.new");
				
				File configOut = new File("rpibrew.cfg.new");
				try {
					config.write(configOut);
				} catch (IOException ioe) {
					System.out.println("Could not update file");
				}
				
				System.out.println("Config file updated. Please copy it from rpibrew.cfg.new to rpibrew.cfg to use the data");
				System.out.println("You may need to do this as root");
				
				System.exit(0);
			}
			if(inputBroken[0].startsWith("r") || inputBroken[0].startsWith("R")) {
				System.out.println("Refreshing...");
				displaySensors();
			}

			// Read in the pumps
			if (inputBroken[0].equalsIgnoreCase("pump")) {
				if (inputBroken.length != 3 || inputBroken[1].length() == 0 || inputBroken[2].length() == 0 ) {
					System.out.println("Not enough parameters to add a pump");
					continue;
				}
				
				try {
					GPIO.getPinNumber(inputBroken[2]);
				} catch (InvalidGPIOException e) {
					System.out.println(e.getMessage());
					continue;
				}
				
				// We should be good to go now
				if (!config.hasSection("pumps")) {
					try {
						config.addSection("pumps");
					} catch (DuplicateSectionException e) {
						// won't happen
					}
				}
				
				try {
					config.set("pumps", inputBroken[1], inputBroken[2]);
				} catch (NoSectionException e) {
					// Never going to happen
				}
				
				System.out.println("Added " + inputBroken[1] + " pump on GPIO pin " + inputBroken[2]);
				displayPumps();
				
				continue;
				
			}
			
			if (inputBroken[0].equalsIgnoreCase("volume")) {
				System.out.println("Adding volume details, please select the vessel to use:");
				
				displaySensors();
				input = readInput();
				inputBroken = input.split(" ");
				try {
					int vesselNumber = Integer.parseInt(inputBroken[0]);
					calibrateVessel(tempList.get(vesselNumber - 1));
				} catch (NumberFormatException e) {
					System.out.println("Couldn't read " + inputBroken[0] + " as an integer");
				}
				continue;
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
					Temp tTemp = tempList.get(selector-1);
					tTemp.setName(name);
					if (GPIO != "GPIO1" && GPIO != "GPIO7") {
						addPIDToConfig(tTemp.getProbe(), name, GPIO);
					} else {
						System.out.println("No valid GPIO Value found, adding as a temperature only probe");
						addTempToConfig(tTemp.getProbe(), name);
					}
					
					if (tTemp.volumeBase != null) {
						if (tTemp.volumeAIN != -1) {
							saveVolume(tTemp.getName(), tTemp.volumeAIN, tTemp.getVolumeUnit(), tTemp.volumeBase);
						} else if (tTemp.volumeAddress != null && tTemp.volumeOffset != null) {
							saveVolume(tTemp.getName(), tTemp.volumeAddress, tTemp.volumeOffset, 
									tTemp.getVolumeUnit(), tTemp.volumeBase);
						} else {
							BrewServer.log.info("No valid volume probe found");
						}
					} else {
						BrewServer.log.info("No Volume base set");
					}

				} else {
					System.out.println( "Input number (" + input + ") is not valid\n");
				}
			} catch (NumberFormatException e) {
				// not a number
				e.printStackTrace();
			}
		}

	}

	/******
	 * Create the OWFSConnection configuration in a thread safe manner
	 * @param server Server Name where owserver is hosted
	 * @param port The port the owserver is running on
	 */
	public static void setupOWFS() {
		if (owfsConnection != null) {
			try {
				owfsConnection.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Use the thread safe mechanism
		OwfsConnectionConfig owConfig = new OwfsConnectionConfig(owfsServer, owfsPort);
		owConfig.setPersistence(OwPersistence.ON);
		owfsConnection = OwfsConnectionFactory.newOwfsClientThreadSafe(owConfig);
		useOWFS = true;
	}
	
	/********
	 * Create the OWFS Connection to the server (owserver)
	 */
	private void createOWFS() {
		
		System.out.println("Creating the OWFS configuration.");
		System.out.println("What is the OWFS server host? (Defaults to localhost)");
		
		String line = readInput();
		if (!line.trim().equals("")) {
			owfsServer = line.trim();
		}
		
		System.out.println("What is the OWFS server port? (Defaults to 4304)");
		
		line = readInput();
		if (!line.trim().equals("")) {
			owfsPort = Integer.parseInt(line.trim());
		}
		
		if (config == null ) {
			initializeConfig();
		}
		
		try {
			if(!config.hasSection("general")) {
				config.addSection("general");
			}
			config.set("general", "owfs_server", owfsServer);
			config.set("general", "owfs_port", owfsPort);
		} catch (NoSectionException e) {
			// Impossibru
		} catch (DuplicateSectionException e) {
			// Impossible
		}
			
		// Create the connection
		setupOWFS();
	}
	
	/************
	 * Reconnects the OWFS connection
	 */

	/***********
	 * List the One-Wire devices in OWFS
	 * Much more fully featured access
	 */
	private void listOWFSDevices() {
		try {
			List<String> owfsDirs = owfsConnection.listDirectory("/");
			Iterator<String> dirIt = owfsDirs.iterator();
			String dir = null;
			
			while (dirIt.hasNext()) {
				dir = dirIt.next();
				if (dir.startsWith("/2")) {
					// we have a "good' directory, I'm only aware that directories starting with a number of 2 are good
					// got a directory, check for a temp
					System.out.println("Checking for " + dir);
					Temp currentTemp = new Temp(dir, dir);
					tempList.add(currentTemp);
					// setup the scale for each temp probe
					currentTemp.setScale(scale);
					// setup the threads
					Thread tThread = new Thread(currentTemp);
					tempThreads.add(tThread);
					tThread.start();
				}
			}
		} catch (OwfsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/***********
	 * Helper method to read a path value from OWFS with all checks
	 * @param path The path to read from
	 * @return	A string representing the value (or null if there's an error
	 * @throws OwfsException If OWFS throws an error
	 * @throws IOException If an IO error occurs
	 */
	public static String readOWFSPath(String path) throws OwfsException, IOException {
		String result = null;
		if (owfsConnection == null) {
			setupOWFS();
			if (owfsConnection == null) {
				BrewServer.log.info("no OWFS connection");
			}
		}
		
		if (owfsConnection.exists(path)) {
			result = owfsConnection.read(path);
		}
		
		return result;
	}

	/*********
	 * Calibrate the vessel specified using a REPL loop
	 * @param vesselNumber
	 */
	private void calibrateVessel(Temp tempObject) {
		int ain = -1;
		String volumeUnit = null;
		
		System.out.println("Calibrating " +  tempObject.getName());
		System.out.print("Please enter the volume uni>t");
		
		String line = readInput();
		volumeUnit = line;
		
		System.out.print("\nPlease select the Analogue Input number or 1wire Address>");
		line = readInput();
		
		// setup these here just incase
		String address = null;
		String offset = null;
		
		try {
			ain = Integer.parseInt(line);
		} catch (NumberFormatException nfe) {
			System.out.println("Couldn't read '" + line + "' as an integer So I'll treat it as an address for 1wire");
			address = line;
			// Check to see if the address exists
			if (owfsConnection == null) {
				createOWFS();
			}
			
			System.out.print("\nWhat's the Offset for the 1Wire input? (Q cancels):");
			offset = readInput().trim();
			if (offset.startsWith("q")) {
				System.out.println("Exitting");
				System.exit(0);
			}
			
		}
		
		try {
			if (ain != -1) {
				tempObject.setupVolumes(ain, volumeUnit);
			} 
			
			if (address != null && offset != null) {
				tempObject.setupVolumes(address, offset, volumeUnit);
			}
		} catch (InvalidGPIOException e) {
			System.out.println("Something is wrong with the analog input (" + ain + ") selected");
			return;
		}
		
		System.out.println("This will loop until you type 'q' (no quotes) to continue");
		System.out.println("Add liquid, wait for the level to settle, then type the amount you currently have in.");
		System.out.println("For instance, add 1G of water, then type '1'<Enter>, add another 1G of water and type '2'<enter>");
		System.out.println("Until you are done calibrating");
		
		
		// REPL 
		while (true) {
			System.out.print(">");
			line = readInput();
			
			if (line.startsWith("q")) {
				break;
			}
			
			Double currentVolume = Double.parseDouble(line.trim());
			Double currentValue = tempObject.updateVolume();
			
			tempObject.addVolumeMeasurement(currentVolume, currentValue);
		}
		
		// Check to see if we actually added anything first
		if (tempObject.volumeBase != null) {
			if (tempObject.volumeAIN != -1) {
				saveVolume(tempObject.getName(), tempObject.volumeAIN, tempObject.getVolumeUnit(), tempObject.volumeBase);
			} else if (tempObject.volumeAddress != null && tempObject.volumeOffset != null) {
				saveVolume(tempObject.getName(), tempObject.volumeAddress, tempObject.volumeOffset, 
						tempObject.getVolumeUnit(), tempObject.volumeBase);
			} else {
				BrewServer.log.info("No valid volume probe found");
			}
		} else {
			BrewServer.log.info("No Volume base set");
		}
		
		saveSettings();
	}


	/*******
	 * Helper function to read the user input and tidy it up
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
	
	/*******
	 * Prints the list of pumps to STDOUT
	 */
	
	private void displayPumps() {
		System.out.println("The following pumps are configured:");
		Iterator<Pump> iterator = pumpList.iterator();
		
		while (iterator.hasNext()) {
			Pump tPump = iterator.next();
			System.out.println(tPump.getName() + " on GPIO" + tPump.getGPIO());
		}
	}
	
	/*******
	 * Prints the list of probes that're found
	 */
	private void displaySensors() {
		// iterate the list of temperature Threads to get values
		Integer i = 1;
		Iterator<Temp> iterator = tempList.iterator();
		System.out.println("\n\nNo config data found as usable. Select a input and name it (i.e. \"1 kettle\" no quotes) or type \"r\" to refresh:");
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			Temp tTemp = iterator.next();
			Double currentTemp = tTemp.updateTemp();
			System.out.print(i.toString() + ") " + tTemp.getName());
			if(currentTemp == -999) {
				  System.out.print(" doesn't have a valid temperature");
			} else {
				System.out.print( " " + currentTemp);
			}
			i++;
		}
	}
	
	/*****
	 * Prints the GPIO Pinout for the RPi
	 */
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

	/*****
	 * Save the configration to the Config
	 */
	public void saveSettings() {
		if (config == null) {
			initializeConfig();
		}
		// go through the list of PIDs and save each
		for(PID n : pidList) {
			if(n != null) {
				BrewServer.log.info("Saving " + n.getName());
				savePID(n.getName(), n.heatSetting);
				
				// Do we need to save the volume information
				if (n.fTemp.volumeBase != null) {
					if (n.fTemp.volumeAIN != -1) {
						saveVolume(n.fTemp.getName(), n.fTemp.volumeAIN, n.fTemp.getVolumeUnit(), n.fTemp.volumeBase);
					} else if (n.fTemp.volumeAddress != null && n.fTemp.volumeOffset != null) {
						saveVolume(n.fTemp.getName(), n.fTemp.volumeAddress, n.fTemp.volumeOffset, 
								n.fTemp.getVolumeUnit(), n.fTemp.volumeBase);
					} else {
						BrewServer.log.info("No valid volume probe found");
					}
				} else {
					BrewServer.log.info("No Volume base set");
				}
			}
			
		}
		
	}
	
	/******
	 * Save the PID to the config parser
	 * @param name Name of the PID
	 * @param settings The PID settings, normally these are taken from the live device
	 */
	public static void savePID(String name, PID.Settings settings) {
		// Save the config  to the configuration file
		BrewServer.log.info("Saving the information for " + name);
		
		try {
			config.read(configFileName);
		} catch (IOException e) {	
			System.out.println("Config file doesn't exist");
			return;
		}
		
		// save any changes
		try {
			if(!config.hasSection(name)) {
				// no idea why there wouldn't be this section
				config.addSection(name);
			}
			
			config.set(name, "duty_cycle", settings.duty_cycle);
			config.set(name, "cycle_time", settings.cycle_time);
			config.set(name, "set_point", settings.set_point);
			config.set(name, "proportional", settings.proportional);
			config.set(name, "integral", settings.integral);
			config.set(name, "derivative", settings.derivative);
			
			
			File configOut = new File(configFileName);
			try {
				config.write(configOut);
			} catch (IOException ioe) {
				System.out.println("Could not update file");
			}
			
		} catch (NoSectionException nse) {
			System.out.print("No Such Section " + name);
			nse.printStackTrace();
		} catch (DuplicateSectionException e) {
			// TODO Auto-generated catch block
			System.out.print("Duplicate section");
			e.printStackTrace();
		}
	}

	
	/****
	 * Add the specified basic PID info to the Config
	 * @param device Probe address
	 * @param name Device Name
	 * @param GPIO GPIO to be used to control the Output
	 */
	private void addPIDToConfig(String device, String name, String GPIO) {
		try {
		  if (!config.hasSection(name) ) {
		  		// update this one
				config.addSection(name);
		  }
	
		  config.set(name, "probe", device);
		  config.set(name, "gpio", GPIO);
		} catch (Exception e) {
			return;
		}
		
		
	}

	/*******
	 * Add a temperature device to the confiration file
	 * @param device
	 * @param name
	 */
	private void addTempToConfig(String device, String name) {
		try {
			// Check for the section
		  if (!config.hasSection(name) ) {
		  		// New device, create a section
				config.addSection(name);
		  }
	
		  config.set(name, "probe", device);
		} catch (Exception e) {
			// Not going to happen, we check for a section and create it
		}
	}
	
	/*******
	 * Save the volume details to the config
	 * @param name name of the device to add the informaiton to
	 * @param address the the one Wire ADC convertor
	 * @param offset The 1wire input (A/B/C/D)
	 * @param volumeUnit The units for the volume (free text string)
	 * @param volumeBase Hashmap of the volume ranges and readings
	 */
	private void saveVolume(String name, String address, String offset, String volumeUnit,
			HashMap<Double, Double> volumeBase) {
		
		BrewServer.log.info("Saving volume for " + name);
		String name_volume = name + "-volume";
		
		try {
			if (!config.hasSection(name_volume)) {
				config.addSection(name_volume);
			}
			
			config.set(name_volume, "unit", volumeUnit.toString());
			config.set(name_volume, "address", address);
			config.set(name_volume, "offset", offset);
			
			Iterator<Entry<Double, Double>> volIter = volumeBase.entrySet().iterator();
			
			while (volIter.hasNext()) {
				Entry<Double, Double> entry = volIter.next();
				config.set(name_volume, entry.getKey().toString(), entry.getValue().toString());
			}
			
		} catch (Exception nse) {
			// Not Going to happen
		}
		
	}

	/******
	 * Save the volume information for an onboard Analogue Input
	 * @param name Device name
	 * @param volumeAIN Analogue input pin
	 * @param volumeUnit The units for the volume (free text string)
	 * @param volumeBase Hashmap of the volume ranges and readings
	 */
	private void saveVolume(String name, int volumeAIN, String volumeUnit,
			HashMap<Double, Double> volumeBase) {
		
		String name_volume = name + "-volume";
		try {
			if (!config.hasSection(name_volume)) {
				config.addSection(name_volume);
			}
			
			config.set(name_volume, "unit", volumeUnit.toString());
			config.set(name_volume, "pin", volumeAIN);
			
			Iterator<Entry<Double, Double>> volIter = volumeBase.entrySet().iterator();
			
			while (volIter.hasNext()) {
				Entry<Double, Double> entry = volIter.next();
				config.set(name_volume, entry.getKey().toString(), entry.getValue().toString());
			}
			
		} catch (NoSectionException nse) {
			// Never going to happen
		} catch (DuplicateSectionException e) {
			// Never going to happen
			
		}
		
	}

	/******
	 * Helper method to initialize the configuration
	 */
	private void initializeConfig() {
		config = new ConfigParser();
	}
}
