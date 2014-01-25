package com.sb.elsinore;
import jGPIO.GPIO;
import jGPIO.InvalidGPIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import Cosm.Cosm;
import Cosm.CosmException;
import Cosm.Datastream;
import Cosm.Feed;
import Cosm.Unit;

import com.sb.common.ServeHTML;
import javax.xml.validation.Schema;

public final class LaunchControl {
	/* List of PIDs, Temperatures, and Pump objects */
	public static List<PID> pidList = new ArrayList<PID>(); 
	public static List<Temp> tempList = new ArrayList<Temp>();
	public static List<Pump> pumpList = new ArrayList<Pump>();
	public static List<String> timerList = new ArrayList<String>(); 
	
	/* Temperature and PID threads */
	private List<Thread> tempThreads = new ArrayList<Thread>();
	private List<Thread> pidThreads = new ArrayList<Thread>();
	
	/* ConfigParser details, need to investigate moving to a better ini parser */
	private static ConfigParser configCfg = null;
	private static Document configDoc = null;
	
	private static String configFileName = "elsinore.cfg";
	
	private static ServerRunner sRunner = null;
	public static int port = 8080;
	
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
	private static Integer owfsPort = 4304;
	
	private static Options startupOptions = null;
	private static CommandLine startupCommand = null;

	private static XPathFactory xPathfactory = XPathFactory.newInstance();
	private static XPath xpath = xPathfactory.newXPath();
	private static XPathExpression expr = null;
	/*****
	 * Main method to launch the brewery
	 * @param arguments
	 */
	public static void main(String... arguments) {
		BrewServer.log.info( "Running Brewery Controller." );
		if (arguments.length > 0) {
			createOptions();
			CommandLineParser parser = new BasicParser();
			try {
				startupCommand = parser.parse( startupOptions, arguments);
				
				// Do we need to print the help?
				if (startupCommand.hasOption("help")) {
					HelpFormatter hF = new HelpFormatter();
					hF.printHelp("java -jar Elsinore.jar", startupOptions);
					return;
				}
				
				// Check for a custom value
				if (startupCommand.hasOption("config")) {
					configFileName = startupCommand.getOptionValue("config");
				}
				
				if (startupCommand.hasOption("gpio_definitions")) {
					System.setProperty("gpio_definitions", startupCommand.getOptionValue("gpio_definitions"));
				}
				
				if (startupCommand.hasOption("port")) {
					try {
						int t = Integer.parseInt(startupCommand.getOptionValue("port"));
						port = t;
					} catch (NumberFormatException e) {
						System.out.println("Couldn't parse port value as an integer: " + startupCommand.getOptionValue("port"));
						System.exit(-1);
					}
				}
				
				if (startupCommand.hasOption("d")) {
					System.setProperty("debug", "INFO");
				}
				
			} catch (ParseException e) {
				System.out.println("Error when parsing the command line");
				e.printStackTrace();
				return;
			}
		}
		LaunchControl lc = new LaunchControl();
	}

	/*******
	 *  Used to setup the options for the command line parser
	 */
	private static void createOptions() {
		startupOptions = new Options();
		
		startupOptions.addOption("h", "help", false, "Show this help");
		
		startupOptions.addOption("d", "debug", false, "Enable debug output");
		
		startupOptions.addOption("o", "owfs", false, "Setup OWFS connection for configuration on startup");
		startupOptions.addOption("c", "config", true, "Specify the name of the configuration file");
		startupOptions.addOption("p", "port", true, "Specify the port to run the webserver on");
		
		startupOptions.addOption("g", "gpio_definitions", false, "specify the GPIO Definitions file if you're on Kernel 3.8 or above");
	}
	
	/* Constructor */
	public LaunchControl() {
		
		// Create the shutdown hooks for all the threads to make sure we close off the GPIO connections
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				
				saveSettings();
				for (Temp t : tempList) {
					if (t != null) {
						t.save();
					}
				}
				
				for(PID n : pidList) {
					if(n != null) {
						n.shutdown();
					}
				}
				saveConfigFile();
			}
		});

		// See if we have an active confiration file
		readConfig();

		// Debug info before launching the BrewServer itself
		BrewServer.log.log(Level.INFO, "CONFIG READ COMPLETED***********");
		sRunner = new ServerRunner(BrewServer.class, port);
		sRunner.run();
		
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

		ServeHTML pidServe = new ServeHTML(devList, pumpList);
		return pidServe.getPage();
	}

	/******
	 * Get the JSON Output String for the current Status of the PIDs, Temps, Pumps, etc...
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getJSONStatus() {
		// get each setting add it to the JSON
		JSONObject rObj = new JSONObject();
		JSONObject tJSON = null;
	
		// iterate the thread lists
		// use the temp list to determine if we have a PID to go with
		for(Temp t : tempList) {
			/* Check for a PID */
			PID tPid = findPID(t.getName());
			tJSON = new JSONObject();
			if(tPid != null) {
				tJSON.putAll(tPid.getMapStatus());
			} 
			
			// Add the temp to the JSON Map
			tJSON.putAll(t.getMapStatus());
			
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
		if (configCfg == null) {
			initializeConfig();
		}
		
		if (configCfg == null) {
			System.out.println("CFG IS NULL");
		}
		if (configDoc == null) {
			System.out.println("DOC IS NULL");
		}
		
		if (configDoc == null && configCfg == null) {
			createConfig();
			return;
		}

		if (configDoc != null) {
			parseXMLSections();
			
		} else if (configCfg != null) {
			parseCfgSections();
			// Add a new temperature probe for the system
			// input is the name we'll use from here on out
			try {
				if(configCfg.hasOption("general", "system_temp") && configCfg.getBoolean("general", "system_temp")) {
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
			
		} else {
			System.out.println("Couldn't get a configuration file!");
			System.exit(1);
		}

		if (tempList.size() == 0) {
			// get user input
			createConfig();
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

	private void parseGeneral(Element config) {
		if (config == null) {
			return;
		}
		
		try {
			
			Element tElement = getFirstElement(config, "scale"); 
			if (tElement != null) {
				scale = tElement.getTextContent();
			}
			
			String cosmAPIKey = null;
			Integer cosmFeed = null;
			
			tElement = getFirstElement(config, "cosm");
			if (tElement != null) {
				cosmAPIKey = tElement.getTextContent();
			}	
			try {
				cosmFeed = Integer.parseInt(getFirstElement(config, "cosm_feed").getTextContent());
			} catch (NumberFormatException e) {
				cosmFeed = null;
			} catch (NullPointerException ne) {
				cosmFeed = null;
			}
			
			if (cosmAPIKey == null) {
				try {
					cosmAPIKey = getFirstElement(config, "pachube").getTextContent();
				} catch (NullPointerException e)  {
					cosmAPIKey = null;
				}
			}
			
			if (cosmFeed == null) {
				try {
					cosmFeed = Integer.parseInt(getFirstElement(config, "cosm_feed").getTextContent());
				} catch (NumberFormatException e) {
					cosmFeed = null;
				} catch (NullPointerException e)  {
					cosmFeed = null;
				}
				
			}
			
			if (cosmAPIKey != null && cosmFeed != null) {
				startCosm(cosmAPIKey, cosmFeed);
			}
			try {
				owfsServer = getFirstElement(config, "owfs_server").getTextContent();
				owfsPort = Integer.parseInt(getFirstElement(config, "owfs_port").getTextContent());
			} catch (NullPointerException e)  {
				owfsServer = null;
				owfsPort = null;
			}
			
			
			
			if (owfsServer != null && owfsPort != null) {
				BrewServer.log.log(Level.INFO, "Setup OWFS at "+  owfsServer + ":" + owfsPort);
				
				setupOWFS();
			}
			
		} catch (NumberFormatException nfe) {
			System.out.print("Number format problem!");
			nfe.printStackTrace();
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
	
	private void parsePumps(Element config) {
		if (config == null) {
			return;
		}
		
		NodeList pumps = config.getChildNodes();
		
		for(int i = 0; i < pumps.getLength(); i++) {
			Element curPump = (Element) pumps.item(i);
			String pumpName = curPump.getNodeName();
			String gpio = curPump.getTextContent();
			try {
				pumpList.add(new Pump(pumpName, gpio));
			} catch (InvalidGPIOException e) {
				System.out.println("Invalid GPIO (" + gpio + ") detected for pump " + pumpName);
				System.out.println("Please fix the config file before running");
				System.exit(-1);
			}
		}
	
	}
	
	/*****
	 * Parse the list of timers
	 * @param config The config Parser object to use
	 * @param input the name of the section to parse
	 */
	private void parseTimers(ConfigParser config, String input) {
		if (config == null) {
			return;
		}
		
		try {
			timerList = config.options(input);
			// TODO: Add in countup/countdown detection
			
		} catch (NoSectionException nse) {
			// we shouldn't get here!
			nse.printStackTrace();
		} 
	}
	
	private void parseTimers(Element config) {
		if (config == null) {
			return;
		}
		NodeList timers = config.getChildNodes();
		timerList = new ArrayList<String>();
		
		for (int i = 0; i < timers.getLength(); i++) {
			Element tElement = (Element) timers.item(i);
			timerList.add(tElement.getAttribute("id"));
		}
	} 
	
	// Todo: This is a bloody stupid method incovation...
	private void startDevice(String input, String probe, String gpio, double duty, double cycle,
			double setpoint, double p, double i, double d, String cutoffTemp, String auxPin, 
			String volumeUnits, int analoguePin, String dsAddress, String dsOffset, 
			ConcurrentHashMap<Double, Double> volumeArray) {
		// Startup the thread
		if(probe != null && !probe.equals("0")) {
			// input is the name we'll use from here on out
			Temp tTemp = new Temp(input, probe);
			tempList.add(tTemp);
			BrewServer.log.info("Adding " + tTemp.getName() + " GPIO is (" + gpio + ")");
			// setup the scale for each temp probe
			tTemp.setScale(scale);
			
			if (cutoffTemp != null) {
				tTemp.setCutoffTemp(cutoffTemp);
			}
			
			// setup the threads
			Thread tThread = new Thread(tTemp);
			tempThreads.add(tThread);
			tThread.start();

			if(gpio != null && !gpio.equals("")) {
				BrewServer.log.info("Adding PID with GPIO: " + gpio);
				PID tPID = new PID(tTemp, input, duty, cycle, p, i, d, gpio);
				
				if (auxPin != null && !auxPin.equals("")) {
					tPID.setAux(auxPin);
				}
				
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
		
		if (startupCommand != null && startupCommand.hasOption("owfs")) {
			createOWFS();
		}
		
		if (useOWFS) {
			listOWFSDevices();
		} else {
			listOneWireSys();
		}
		if (tempList.size() == 0) {
			System.out.println("Could not find any one wire devices, please check you have the correct modules setup");
			System.exit(0);
		}
		
		if (configDoc == null) {
			initializeConfig();
		}
		
		displaySensors();
		System.out.println("Select the input, enter \"r\" to refresh, or use \"pump <name> <gpio>\" to add a pump");
		System.out.println("Type \"volume\" to start volume calibration");
		System.out.println("Type \"timer <name>\" to add a timer");
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
			// is the first value something we recognize?
			if(inputBroken[0].equalsIgnoreCase("quit")) {
				saveSettings();
				System.out.println("Quitting");
				System.out.println("Updating config file, please check it in " + configFileName + ".new");
				
				saveConfigFile(configFileName + ".new");
				
				System.out.println("Config file updated. Please copy it from rpibrew.cfg.new to rpibrew.cfg to use the data");
				System.out.println("You may need to do this as root");
				
				System.exit(0);
			}
			if(inputBroken[0].startsWith("r") || inputBroken[0].startsWith("R")) {
				System.out.println("Refreshing...");
				displaySensors();
				continue;
			}

			// Read in the pumps
			if (inputBroken[0].equalsIgnoreCase("pump")) {
				if (inputBroken.length != 3 || inputBroken[1].length() == 0 || inputBroken[2].length() == 0 ) {
					System.out.println("Not enough parameters to add a pump");
					System.out.println("pump <name> <gpio>");
					continue;
				}
				
				try {
					GPIO.getPinNumber(inputBroken[2]);
				} catch (InvalidGPIOException e) {
					System.out.println(e.getMessage());
					continue;
				}
				
				// We should be good to go now
				Element pumpElement = getFirstElement(null, "pumps");
				
				if (pumpElement == null) {
					pumpElement = addNewElement(null, "pumps");
				}
				
				Element newPump = addNewElement(pumpElement, inputBroken[1]);
				
				newPump.appendChild(configDoc.createTextNode(inputBroken[2]));
				pumpElement.appendChild(newPump);
				
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
			
			// Timers code
			if (inputBroken[0].equalsIgnoreCase("timer")) {
				String name = "";
			
				if (inputBroken.length > 1 && inputBroken[1].length() > 0 ) {
					name = inputBroken[1];
				} else {
					System.out.println("");
					System.out.print("What do you want to call the timer? ");
					name = readInput();
				}
				
				if (name.equals("")) {
					System.out.println("No name provided");
				} else {
					System.out.println("Adding Timer " + name);
					timerList.add(name);
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
					} else if (tTemp.getTemp() != -999) {
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

	private void saveConfigFile(String outFileName) {
		File configOut = new File(outFileName);
		
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			
			XPath xp = XPathFactory.newInstance().newXPath();
			NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", configDoc, XPathConstants.NODESET);

			for (int i=0; i < nl.getLength(); ++i) {
			    Node node = nl.item(i);
			    node.getParentNode().removeChild(node);
			}
			
			DOMSource source = new DOMSource(configDoc);
			StreamResult configResult = new StreamResult(configOut);
			transformer.transform(source, configResult);
		} catch (TransformerConfigurationException e) {
			System.out.println("Could not transform config file");
			e.printStackTrace();
		} catch (TransformerException e) {
			System.out.println("Could not transformer file");
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static private void saveConfigFile() {
		File configOut = new File(configFileName);
		
		Element generalElement = getFirstElement(null, "general");
		if (generalElement == null) {
			generalElement = addNewElement(null, "general");
		}
		
		NodeList tList = null;
		Element tempElement = null;
		
		if (owfsServer != null) {
			tempElement = getFirstElement(generalElement, "owfs_server");
			
			if (tempElement == null) {
				tempElement = addNewElement(generalElement, "owfs_server");
			} 
			
			tempElement.setTextContent(owfsServer);
		}
		
		if (owfsPort != null) {
			tempElement = getFirstElement(generalElement, "owfs_port");
			
			if (tempElement == null) {
				tempElement = addNewElement(generalElement, "owfs_port");
			} 
			
			tempElement.setTextContent(Integer.toString(owfsPort));
		}
		
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(configDoc);
			
			XPath xp = XPathFactory.newInstance().newXPath();
			NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", configDoc, XPathConstants.NODESET);

			for (int i=0; i < nl.getLength(); ++i) {
			    Node node = nl.item(i);
			    node.getParentNode().removeChild(node);
			}
			
			
			StreamResult configResult = new StreamResult(configOut);
			StreamResult result = new StreamResult(System.out);
			
			transformer.transform(source, configResult);
			
		} catch (TransformerConfigurationException e) {
			System.out.println("Could not transform config file");
			e.printStackTrace();
		} catch (TransformerException e) {
			System.out.println("Could not transformer file");
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		System.out.println("Connecting to " + owfsServer + ":" + owfsPort);
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
		
		if (configDoc == null ) {
			initializeConfig();
		}
		
		Element generalElement = getFirstElement(null, "general");
		if (generalElement == null) {
			generalElement = addNewElement(null, "general");
		}
	
		Element tempElement = addNewElement(null, "owfs_server");
		tempElement.setTextContent(owfsServer);
		
		tempElement = addNewElement(null, "owfs_port");
		tempElement.setTextContent(Integer.toString(owfsPort));
		
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
			if (owfsDirs.size() > 0) {
				System.out.println("Listing OWFS devices on " + owfsServer + ":" + owfsPort);
			}
			Iterator<String> dirIt = owfsDirs.iterator();
			String dir = null;
			
			while (dirIt.hasNext()) {
				dir = dirIt.next();
				if (dir.startsWith("/28")) {
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
		String result = "";
		if (owfsConnection == null) {
			setupOWFS();
			if (owfsConnection == null) {
				BrewServer.log.info("no OWFS connection");
			}
		}
		try {
			if(owfsConnection.exists(path)) {
				result = owfsConnection.read(path);
			}
		} catch (OwfsException e) {
			// Error -1 is file not found, exists should bloody catch this
			if (!e.getMessage().equals("Error -1")) {
				throw e;
			}
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
				  System.out.println(" doesn't have a valid temperature");
			} else {
				System.out.println( " " + currentTemp);
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
	 * Save the configuration to the Config
	 */
	public void saveSettings() {
		if (configDoc == null) {
			initializeConfig();
		}
		// go through the list of PIDs and save each
		for(PID n : pidList) {
			if(n != null) {
				
				if (n.getName().equals("") ) {
					continue;
				} else {
					System.out.println("Saving PID " + n.getName());
				
					savePID(n.getName(), n.heatSetting, n.getGPIO(), n.getAuxGPIO());
					
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
		
		// Save the timers
		Element timersElement = getFirstElement(null, "timers");
		
		if (timersElement == null) {
			timersElement = addNewElement(null, "timers");
		}
		
		for (String t : timerList) {
			
			if (getFirstElementByXpath(null, "/elsinore/timers/timer[@id='" + t + "']") == null) {
				// No timer by this name
				Element newTimer = addNewElement(timersElement, "timer");
				newTimer.setAttribute("id", t);
			}
		}
		
	}
	
	/******
	 * Save the PID to the config doc
	 * @param name Name of the PID
	 * @param settings The PID settings, normally these are taken from the live device
	 */
	public static void savePID(String name, PID.Settings settings, String gpio, String auxGPIO) {

		if (name == null || name.equals("")) {
			new Throwable().printStackTrace();
		}
			
		if (configDoc == null) {
			setupConfigDoc();
		}
		
		// Save the config  to the configuration file
		System.out.println("Saving the information for " + name);
		
		// save any changes
		Element device = getFirstElementByXpath(null, "/elsinore/device[@id='" + name + "']");
		if (device == null) {
			System.out.println("Creating new Element");
			// no idea why there wouldn't be this section
			device = addNewElement(null, "device");
			device.setAttribute("id", name);
		}
		
		System.out.println("Using base node " + device.getNodeName() + " with ID " + device.getAttribute("id"));
		
		setElementText(device, "duty_cycle", Double.toString(settings.duty_cycle));
		setElementText(device, "cycle_time", Double.toString(settings.cycle_time));
		setElementText(device, "set_point", Double.toString(settings.set_point));
		setElementText(device, "proportional", Double.toString(settings.proportional));
		setElementText(device, "integral", Double.toString(settings.integral));
		setElementText(device, "derivative", Double.toString(settings.derivative));
		setElementText(device, "gpio", gpio);
		
		if (auxGPIO != null) {
			setElementText(device, "aux", auxGPIO);
		}
		
		saveConfigFile();
	}

	
	/****
	 * Add the specified basic PID info to the Config
	 * @param device Probe address
	 * @param name Device Name
	 * @param GPIO GPIO to be used to control the Output
	 */
	private Element addPIDToConfig(String probe, String name, String GPIO) {

		System.out.println("Saving " + name + " with GPIO " + GPIO);
				
		Element device = addTempToConfig(probe, name);
		
		if (device != null) {
			setElementText(device, "gpio", GPIO);
		}
		
		return device;
			
	}

	/*******
	 * Add a temperature device to the confiration file
	 * @param device
	 * @param name
	 */
	public static Element addTempToConfig(String probe, String name) {
		
		if (probe.equalsIgnoreCase(name)) {
			System.out.println("Probe: " + probe + " is not setup, not saving");
			return null;
		}
		// save any changes
		System.out.println("Saving " + name + " with probe " + probe);
		// save any changes
		Element device = getFirstElementByXpath(null, "/elsinore/device[@id='" + name + "']");
		if (device == null) {
			// no idea why there wouldn't be this section
			device = addNewElement(null, "device");
			device.setAttribute("id", name);
		}
		
		setElementText(device, "probe", probe);
		
		return device;
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
			ConcurrentHashMap<Double, Double> volumeBase) {
		
		System.out.println("Saving volume for " + name);
		
		Element device = saveVolumeMeasurements(name, volumeBase, volumeUnit);
	
		// Units for the Volume OneWire Address
		Element tElement = getFirstElement(device, "volume-address");
		if (tElement == null) {
			tElement = addNewElement(device, "volume-address");
		}
		
		tElement.setTextContent(address);
		
		// Units for the Volume OneWire Offset
		tElement = getFirstElement(device, "volume-offset");
		if (tElement == null) {
			tElement = addNewElement(device, "volume-offset");
		}
		
		tElement.setTextContent(offset);
		
		
	}

	/******
	 * Save the volume information for an onboard Analogue Input
	 * @param name Device name
	 * @param volumeAIN Analogue input pin
	 * @param volumeUnit The units for the volume (free text string)
	 * @param volumeBase Hashmap of the volume ranges and readings
	 */
	private void saveVolume(String name, int volumeAIN, String volumeUnit,
			ConcurrentHashMap<Double, Double> volumeBase) {
		
		BrewServer.log.info("Saving volume for " + name);
		
		// save any changes
		Element device = saveVolumeMeasurements(name, volumeBase, volumeUnit);
		
		Element tElement = getFirstElement(device, "volume-pin");
		if (tElement == null) {
			tElement = addNewElement(device, "volume-pin");
		}
		
		tElement.setTextContent(Integer.toString(volumeAIN));
		
	}
	
	public Element saveVolumeMeasurements(String name, ConcurrentHashMap<Double, Double> volumeBase, String volumeUnit) {
		Element device = getFirstElementByXpath(null, "/elsinore/device[@id='" + name + "']");
	
		if (device == null) {
			// no idea why there wouldn't be this section
			device = addNewElement(null, "device");
			device.setAttribute("id", name);
		}
		
		Element tElement = getFirstElement(device, "volume-unit");
		
		if (tElement == null) {
			tElement = addNewElement(device, "volume-unit");
		}
		
		tElement.setTextContent(volumeUnit.toString());
	
		
		Iterator<Entry<Double, Double>> volIter = volumeBase.entrySet().iterator();
		
		while (volIter.hasNext()) {
			Entry<Double, Double> entry = volIter.next();
			BrewServer.log.info("Looking for volume entry: " + entry.getKey().toString());
			
			tElement = getFirstElementByXpath(null, "/elsinore/device[@id='" + name + "']/volume[@vol='"+entry.getKey().toString()+"']");
			
			if (tElement == null) {
				tElement = addNewElement(device, "volume");
				tElement.setAttribute("vol", entry.getKey().toString());
			}
			
			tElement.setTextContent(entry.getValue().toString());
			
		}
		
		return device;
	}

	/******
	 * Helper method to initialize the configuration
	 */
	private void initializeConfig() {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		//dbFactory.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaSource", "elsinore.xsd");
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		File existingConfig = new File(configFileName);
		if (!existingConfig.exists()) {
			return;
		}
		
		try {
			configDoc = dBuilder.parse(existingConfig);
			XPath xp = XPathFactory.newInstance().newXPath();
			NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", configDoc, XPathConstants.NODESET);

			for (int i=0; i < nl.getLength(); ++i) {
			    Node node = nl.item(i);
			    node.getParentNode().removeChild(node);
			}
			
			return;
		} catch (Exception e) {
			System.out.println(configFileName + " isn't an XML File, trying configparser");
			
		}
		
		try {
			configCfg = new ConfigParser();
			
			System.out.println("Backing up config to " + configFileName + ".original");
			
			copyFile(new File(configFileName), new File(configFileName+".original"));
			
			configCfg.read(configFileName);
			System.out.println("Created configCfg");
		
		} catch (IOException e) {	
			System.out.println("Config file at: " + configFileName + " doesn't exist");
			configCfg = null;
		}
	}
	
	private void parseCfgSections() {

		List<String> configSections = configCfg.sections();
		
		Iterator<String> iterator = configSections.iterator();
		while (iterator.hasNext()) {
			String temp = iterator.next().toString();
			if(temp.equalsIgnoreCase("general")){
				// parse the general config
				parseGeneral(configCfg, temp);
			} else if(temp.equalsIgnoreCase("pumps")){
				// parse the pump config
				parsePumps(configCfg, temp);
			} else if(temp.equalsIgnoreCase("timers")){
				// parse the pump config
				parseTimers(configCfg, temp);
			} else {
				parseDevice(configCfg, temp);
			}
			
		}
	}
	
	private void parseXMLSections() {
		NodeList configSections = configDoc.getDocumentElement().getChildNodes();
		
		if (configSections.getLength() == 0) {
			return;
		}
		
		
		// setup generl first
		parseGeneral(getFirstElement(null, "general"));
		parsePumps(getFirstElement(null, "pumps"));
		
		
		for (int i = 0; i < configSections.getLength(); i++) {
			Node temp = configSections.item(i);
			if (temp.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) temp;
				System.out.println("Checking section " + e.getNodeName());
				// Parsed general first
				if (e.getNodeName().equalsIgnoreCase("general")) {
					continue;
				} else if (e.getNodeName().equalsIgnoreCase("pumps")) {
					parsePumps(e); 
				} else if (e.getNodeName().equalsIgnoreCase("timers")) {
					parseTimers(e); 
				} else if (e.getNodeName().equalsIgnoreCase("device")) {
					parseDevice(e); 
				}  else {
					System.out.println("Unrecognized section " + e.getNodeName());
				}
			}
		}
	}
	
	// parse each section
	private void parseDevice(ConfigParser config, String input) {
		String probe = null;
		String gpio = null;
		String volumeUnits = "Litres";
		String dsAddress = null, dsOffset = null;
		String cutoffTemp = null, auxPin = null;
		ConcurrentHashMap<Double, Double> volumeArray = new ConcurrentHashMap<Double, Double>();
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
				if(config.hasOption(input, "proportional")) {
					p = config.getDouble(input, "proportional");
				}
				if(config.hasOption(input, "integral")) {
					i = config.getDouble(input, "integral");
				}
				if(config.hasOption(input, "derivative")) {
					d = config.getDouble(input, "derivative");
				}
				if (config.hasOption(input, "cutoff")) {
					cutoffTemp = config.get(input, "cutoff");
				}
				if (config.hasOption(input, "aux")) {
					auxPin = config.get(input, "aux");
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
		startDevice(input, probe, gpio, duty, cycle, setpoint, p, i, d,
				cutoffTemp, auxPin, volumeUnits, analoguePin, dsAddress, dsOffset, volumeArray);
	}
	
	// parse each section
	private void parseDevice(Element config) {
		String probe = null;
		String gpio = null;
		String volumeUnits = "Litres";
		String dsAddress = null, dsOffset = null;
		String cutoffTemp = null, auxPin = null;
		ConcurrentHashMap<Double, Double> volumeArray = new ConcurrentHashMap<Double, Double>();
		double duty = 0.0, cycle = 0.0, setpoint = 0.0, p = 0.0, i = 0.0, d = 0.0;
		int analoguePin = -1;
		
		String deviceName = config.getAttribute("id");
		
		BrewServer.log.info("Parsing XML Device: " + deviceName);
		try {
			Element tElement = getFirstElement(config, "probe");
			if (tElement != null) {
				probe = tElement.getTextContent();
			}
			
			tElement = getFirstElement(config, "gpio");
			if (tElement != null) {
				gpio = tElement.getTextContent();
			}
			
			tElement = getFirstElement(config, "duty_cycle");
			if (tElement != null) {
				duty = Double.parseDouble(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "cycle_time");
			if (tElement != null) {
				cycle = Double.parseDouble(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "set_point");
			if (tElement != null) {
				setpoint = Double.parseDouble(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "proportional");
			if (tElement != null) {
				p = Double.parseDouble(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "integral");
			if (tElement != null) {
				i = Double.parseDouble(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "derivative");
			if (tElement != null) {
				d = Double.parseDouble(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "cutoff");
			if (tElement != null) {
				cutoffTemp = tElement.getTextContent();
			}

			tElement = getFirstElement(config, "aux");
			if (tElement != null) {
				auxPin = tElement.getTextContent();
			}
			
			NodeList tList = config.getElementsByTagName("volume");
			
			if (tList.getLength() == 1) {
				// we have volume elements
				NodeList volumeOptions = tList.item(0).getChildNodes();
				
				for (int j = 0; j < volumeOptions.getLength(); j++) {
					Element curOption = (Element) volumeOptions.item(j);
					
					
					
					// Append the volume to the array
					try {
						double volValue = Double.parseDouble(curOption.getAttribute("vol"));
						double volReading = Double.parseDouble(curOption.getTextContent());
						
						volumeArray.put(volValue, volReading);
						
						// we can parse this as an integer
					} catch (NumberFormatException e) {
						System.out.println("Could not parse " + curOption.getNodeName() + " as an integer");
					}
					
				}
			}
			
			tElement = getFirstElement(config, "volume-unit");
			if (tElement != null) {
				volumeUnits = tElement.getTextContent();
			}

			tElement = getFirstElement(config, "volume-pin");
			if (tElement != null) {
				analoguePin = Integer.parseInt(tElement.getTextContent());
			}
			
			tElement = getFirstElement(config, "volume-address");
			if (tElement != null) {
				dsAddress = tElement.getTextContent();
			}
			
			tElement = getFirstElement(config, "volume-offset");
			if (tElement != null) {
				dsOffset = tElement.getTextContent();
			}
			
			if (volumeUnits == null) {
				System.out.println("Couldn't find a volume unit for " + deviceName);
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
			
		} catch (NumberFormatException nfe) {
			System.out.print("NumberFormatException when reading from: " + deviceName);
			nfe.printStackTrace();
		} catch (Exception e) {
			System.out.println(e.getMessage() + " Ocurred when reading " + deviceName);
			e.printStackTrace();
		}
		
		startDevice(deviceName, probe, gpio, duty, cycle, setpoint, p, i, d,
				cutoffTemp, auxPin, volumeUnits, analoguePin, dsAddress, dsOffset, volumeArray);
	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	private static void setupConfigDoc() {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		//dbFactory.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaSource", "elsinore.xsd");
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			configDoc = dBuilder.newDocument();
			
			configDoc.appendChild(configDoc.createElement("elsinore"));
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private static NodeList getAllNodes(Element baseNode, String nodeName) {
		
		if (baseNode == null) {
			if( configDoc == null) {
				setupConfigDoc();
			}
			
			baseNode = configDoc.getDocumentElement();
		}
		
		NodeList tempList = baseNode.getElementsByTagName(nodeName);
		
		return tempList;
	}

	private static Element getFirstElement(Element baseNode, String nodeName) {

		NodeList nodeList = getAllNodes(baseNode, nodeName);
		
		Element eFound = null;
		if (nodeList != null && nodeList.getLength() > 0){
			eFound = (Element) nodeList.item(0);
		}
		
		return eFound;	
	}
	
	private static Element addNewElement(Element baseNode, String nodeName) {
		
		Element newElement = configDoc.createElement(nodeName);
		System.out.println("Creating element of " + nodeName);
		
		if (baseNode == null) {
			System.out.println("Creating on configDoc base");
			baseNode = configDoc.getDocumentElement();
		} else {
			System.out.println("on " + baseNode.getNodeName());
		}
		
		newElement = (Element) baseNode.appendChild(newElement);
		
		return newElement;
	}
	
	private static Element getFirstElementByXpath(Element baseNode, String xpathIn) {
		
		Element tElement = null;
		
		try {
			expr = xpath.compile(xpathIn);
			NodeList tList = (NodeList) expr.evaluate(configDoc, XPathConstants.NODESET);
			if (tList.getLength() > 0) {
				tElement = (Element) tList.item(0);
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			System.exit(-1);
			e.printStackTrace();
		}
		
		return tElement;
		
	}
	
	private static void setElementText(Element baseNode, String elementName, String textContent) {
		
		if(baseNode == null) {
			baseNode = configDoc.getDocumentElement();
		}
		
		Element tElement = getFirstElement(baseNode, elementName);
		
		if (tElement == null) {
			tElement = addNewElement(baseNode, elementName);
		}
		
		tElement.setTextContent(textContent);
		
		baseNode.appendChild(tElement);
	}
}