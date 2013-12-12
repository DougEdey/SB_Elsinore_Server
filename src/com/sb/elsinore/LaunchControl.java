package com.sb.elsinore;
import jGPIO.GPIO;
import jGPIO.InvalidGPIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ini4j.ConfigParser;
import org.ini4j.ConfigParser.DuplicateSectionException;
import org.ini4j.ConfigParser.InterpolationException;
import org.ini4j.ConfigParser.NoOptionException;
import org.ini4j.ConfigParser.NoSectionException;
import org.json.simple.JSONObject;

import Cosm.Cosm;
import Cosm.CosmException;
import Cosm.Datastream;
import Cosm.Feed;
import Cosm.Unit;

import com.sb.common.ServePID;

public final class LaunchControl {
	public static List<PID> pidList = new ArrayList<PID>(); 
	public static List<Temp> tempList = new ArrayList<Temp>();
	public static List<Pump> pumpList = new ArrayList<Pump>();
	public static enum Volumes {US_GALLON("US Gallon"), UK_GALLON("UK Gallon"), LITRES("Litres");
	
		private Volumes(final String text) {
			this.text = text;
		}
	
		private final String text;
		
		@Override
		public String toString() {
			return text;
		}
	};
	
	private String scale = "F";
	private static BrewDay brewDay = null;

	public static void main(String... arguments) {
		BrewServer.log.info( "Running Brewery Controller." );
		LaunchControl lc = new LaunchControl();
	}

	public LaunchControl() {
	//	final List<Thread> procList = new ArrayList<Thread>();
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
			findPID(tTemp.getName());
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

		ServePID pidServe = new ServePID(devList, pumpList);
		return pidServe.getPage();
	}

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
		String volumeUnits = null;
		HashMap<Double, Integer> volumeArray = new HashMap<Double, Integer>();
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
					
					try {
						double volValue = Double.parseDouble(curOption);
						int volReading = config.getInt(volumeSection, curOption);
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
				} else {
					if (!volumeArray.containsKey(0)) {
						// we don't have a basic level, not implemented yet, math is hard
						System.out.println("No key for '0' level! Sorry this is unimplemented!");
						volumeArray = null;
					} 
					
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

			if (analoguePin != -1 && volumeArray != null && volumeArray.size() >= 3) {
				try {
					tTemp.setupVolumes(analoguePin, Volumes.valueOf(volumeUnits));
					
					Iterator<Entry<Double, Integer>> volIter = volumeArray.entrySet().iterator();
					
					while (volIter.hasNext()) {
						Entry<Double, Integer> entry = volIter.next();
						tTemp.addVolumeMeasurement(entry.getKey(), entry.getValue());
					}
				} catch (InvalidGPIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
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
		if (!w1Folder.exists()) {
			System.out.println("Couldn't read the one wire devices directory!");
			System.exit(-1);
		}
		File[] listOfFiles = w1Folder.listFiles();
		
		if (listOfFiles.length == 0) {
			System.out.println("No 1Wire probes found! Please check your system!");
			System.exit(-1);
		}
		
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
		
		ConfigParser config = new ConfigParser();

		displaySensors();
		System.out.println("Select the input, enter \"r\" to refresh, or use \"pump <name> <gpio>\" to add a pump");
		String input = "";
		String[] inputBroken;
		while(true) {
			System.out.print(">");
			input = "";
			 //  open up standard input
			input = readInput();
			// parse the input and determine where to throw the data
			inputBroken = input.split(" ");
			// is the first value something we recognise?
			if(inputBroken[0].equalsIgnoreCase("quit")) {
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
					calibrateVessel(vesselNumber);
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
						addPIDToConfig(config, tTemp.getProbe(), name, GPIO);
					} else {
						System.out.println("No valid GPIO Value found, adding as a temperature only probe");
						addTempToConfig(config, tTemp.getProbe(), name);
					}
					
					if (tTemp.volumeBase != null) {
						saveVolume(config, tTemp.getName(), tTemp.volumeAIN, tTemp.getVolumeUnit(), tTemp.volumeBase);
					}

				} else {
					System.out.println( "Input number (" + input + ") is not valid\n");
				}
			} catch (NumberFormatException e) {
				// not a number
			}
		}

	}

	private void saveVolume(ConfigParser config, String name, int volumeAIN, Volumes volumeUnit,
			HashMap<Double, Integer> volumeBase) {
		
		String name_volume = name + "-volume";
		try {
			if (!config.hasSection(name_volume)) {
				config.addSection(name_volume);
			}
			
			config.set(name_volume, "unit", volumeUnit.toString());
			config.set(name_volume, "pin", volumeAIN);
			
			Iterator<Entry<Double, Integer>> volIter = volumeBase.entrySet().iterator();
			
			while (volIter.hasNext()) {
				Entry<Double, Integer> entry = volIter.next();
				config.set(name_volume, entry.getKey().toString(), entry.getValue().toString());
			}
 			
		} catch (NoSectionException nse) {
			// Never going to happen
		} catch (DuplicateSectionException e) {
			// Never going to happen
			
		}
		
	}

	private void calibrateVessel(int vesselNumber) {
		int volInt = -1, ain = -1;
		System.out.println("Calibrating " + tempList.get(vesselNumber-1).getName());
		
		System.out.println("Please select the volume unit");
		for( Volumes vol : Volumes.values()) {
			System.out.println(vol.ordinal() + ": " + vol);
		}
		
		System.out.println(">");
		String line = readInput();
		
		try {
			volInt = Integer.parseInt(line);
		} catch (NumberFormatException nfe) {
			System.out.println("Couldn't read '" + line + "' as an integer");
			return;
		}
		
		System.out.println("Please select the Analogue Input number>");
		line = readInput();
		
		try {
			ain = Integer.parseInt(line);
		} catch (NumberFormatException nfe) {
			System.out.println("Couldn't read '" + line + "' as an integer");
			return;
		}
		
		try {
			tempList.get(vesselNumber-1).setupVolumes(ain, (Volumes.values())[volInt]);
		} catch (InvalidGPIOException e) {
			System.out.println("Something is wrong with the analog input (" + ain + ") selected");
			return;
		}
		
		System.out.println("This will loop until you type 'q' (no quotes) to continue");
		System.out.println("Add liquid, wait for the level to settle, then type the amount you currently have in.");
		System.out.println("For instance, add 1G of water, then type '1'<Enter>, add another 1G of water and type '2'<enter>");
		System.out.println("Until you are done calibrating");
		
		boolean unbroken = true;
		
		while (unbroken) {
			System.out.println(">");
			line = readInput();
			
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
	
	private void displayPumps() {
		System.out.println("The following pumps are configured:");
		Iterator<Pump> iterator = pumpList.iterator();
		
		while (iterator.hasNext()) {
			Pump tPump = iterator.next();
			System.out.println(tPump.getName() + " on GPIO" + tPump.getGPIO());
		}
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
		} catch (Exception e) {
			return;
		}
	}
	
	public void saveSettings() {
		// go through the list of PIDs and save each
		for(PID n : pidList) {
			if(n != null) {
				savePID(n.getName(), n.heatSetting);
			}
		}

	}
	
	public static void savePID(String name, PID.Settings settings) {
		// read the config file from the rpibrew.cfg file
		BrewServer.log.info("Saving the information for " + name);
		ConfigParser config = new ConfigParser();
		try {
			config.read("rpibrew.cfg");
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
			
			
			File configOut = new File("rpibrew.cfg");
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

}
