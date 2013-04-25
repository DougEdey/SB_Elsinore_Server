import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.ini4j.*;
import org.ini4j.ConfigParser.*;

import Cosm.*;

public final class LaunchControl {
	static List<PID> pidList = new ArrayList<PID>(); // hlt = null; public PID mlt = null; public PID kettle = null;
	public List<Temp> tempList = new ArrayList<Temp>(); // hltTemp = null; private Temp mltTemp = null; private Temp kettleTemp = null;
	private String scale = "F";
	private BrewDay brewDay = null;

	public static void main(String... arguments) {
		System.out.println( "Running Brewery Controller." );
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

		try {
			BrewServer bServer = new BrewServer(this);
		} catch (IOException e) {
			System.out.print("IOException starting the brew server");
			e.printStackTrace();
		}
		
		// iterate the list of Threads to kick off any PIDs
		Iterator<Temp> iterator = tempList.iterator();
		while (iterator.hasNext()) {
			// launch all the PIDs first, since they will launch the temp theads too
			
			Temp tTemp = iterator.next();
			PID tPid = findPID(tTemp.getName());

			
		}


		/*
		//build an HLT and start it running
		System.out.println(hlt_probe);
		System.out.println(mlt_probe);
		System.out.println(kettle_probe);
		if(!hlt_probe.equals("") && !hlt_probe.equals("0")) {
			hltTemp = new Temp(hlt_probe);
			hltTemp.setScale(scale);
			Thread hltTemp_thread = new Thread(hltTemp);
			hltTemp_thread.start();
			if(hlt_gpio != -1) {
				hlt = new PID(hltTemp, "HLT", hlt_duty, hlt_cycle, hlt_p, hlt_i, hlt_k, hlt_gpio);
				hlt_thread = new Thread( hlt );
				hlt_thread.start();
			}
			pidList.add(hlt);
			procList.add(hlt_thread);
		}

		*/


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

	public String getJSONStatus() {
		// get each setting add it to the JSON
		JSONObject rObj = new JSONObject();

		String active_devices = "";

		// init the params in case there's no value
		mlt_temp = mlt_duty = mlt_cycle = mlt_setpoint = mlt_k = mlt_i = mlt_p = 0.0;

		hlt_temp = hlt_duty = hlt_cycle = hlt_setpoint = hlt_k = hlt_i = hlt_p = 0.0;

		kettle_temp = kettle_duty = kettle_cycle = kettle_setpoint = kettle_k = kettle_i = kettle_p = 0.0;

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
				tJSON.put("k", tPid.getK());
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

	}

	private String mlt_probe = ""; private String hlt_probe = ""; private String kettle_probe = "";
	private String mlt_mode = ""; private String hlt_mode = ""; private String kettle_mode = "";
	private int mlt_gpio = -1; private int hlt_gpio = -1; private int kettle_gpio = -1;
	private long mlt_elapsed = 0;
	private long hlt_elapsed = 0;
	private long kettle_elapsed = 0;
	private double mlt_temp, mlt_duty = 4, mlt_cycle = 0, mlt_setpoint = 175, mlt_k = 44, mlt_i = 165, mlt_p = 4;
	private double hlt_temp, hlt_duty = 4, hlt_cycle = 0, hlt_setpoint = 175, hlt_k = 44, hlt_i = 165, hlt_p = 4;


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
		int gpio = -1;
		double duty = 0.0, cycle = 0.0, setpoint = 0.0, k = 0.0, i = 0.0, p = 0.0;
		System.out.println("Parsing : " + input);
		try {
			if(config.hasSection(input)) {
				if(config.hasOption(input, "probe")) {
					probe = config.get(input, "probe");
				}
				if(config.hasOption(input, "gpio")) {
					gpio = config.getInt(input, "gpio");
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
					k = config.getDouble(input, "k_param");
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
			System.out.println("Adding " + tTemp.getName());
			// setup the scale for each temp probe
			tTemp.setScale(scale);
			// setup the threads
			Thread tThread = new Thread(tTemp);
			tempThreads.add(tThread);
			tThread.start();

			if((duty != 0.0 || cycle != 0.0 || p != 0.0 || i != 0.0 || k != 0.0) && gpio != -1) {
				PID tPID = new PID(tTemp, input, duty, cycle, p, i, k, gpio);
				pidList.add(tPID);
				Thread pThread = new Thread(tPID);
				pidThreads.add(pThread);
				pThread.start();
				tPID.setCool(10, 2, 12, p, i, k);
			}

			// try to createthe data stream
			if(cosm == null || cosmFeed == null) {
				// we have a pacfeed that is valid

			}
		}


	}

	private Datastream findDatastream(String tag) {
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

		System.out.println("Creating new feed");
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
			System.out.println("Fauiled to create stream:" + e.getMessage() + " - " + cosmFeed.getId());

			return null;
		}
		return tData;
	}


	public PID findPID(String name) {
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
		System.out.println("API: " + APIKey + " Feed: " + feedID);
		cosm = new Cosm(APIKey);
		if(cosm == null) {
			// failed
			return;
		}

		// get the data feed
		try {
			cosmFeed = cosm.getFeed(feedID, true);
			System.out.println("Got " + cosmFeed.getTitle());
		} catch (CosmException e) {
			return;
		}

		// get the list of feeds
		
		cosmStreams = cosmFeed.getDatastreams();
		return;
	}
	
	public BrewDay getBrewDay() {
		if (brewDay == null) {
			brewDay = new BrewDay();
		}

		return brewDay;
	}

	private Cosm cosm = null;
	private Feed cosmFeed = null;
	private Datastream[] cosmStreams = null;

	private double kettle_temp, kettle_duty = 4, kettle_cycle = 0, kettle_setpoint = 175, kettle_k = 44, kettle_i = 165, kettle_p = 4;
	private List<Thread> tempThreads = new ArrayList<Thread>();
	private List<Thread> pidThreads = new ArrayList<Thread>();
}
