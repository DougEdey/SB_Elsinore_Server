import java.util.List;
import java.util.ArrayList;
import java.io.*;

import org.json.simple.JSONObject;
import org.ini4j.*;
import org.ini4j.ConfigParser.*;

public final class LaunchControl {
	public PID hlt = null; public PID mlt = null; public PID kettle = null;
	private Temp hltTemp = null; private Temp mltTemp = null; private Temp kettleTemp = null;
	private String scale = "F";

	public static void main(String... arguments) {
		System.out.println( "Running Brewery Controller." );
		LaunchControl lc = new LaunchControl();
	}

	public LaunchControl() {
		final List<Thread> procList = new ArrayList<Thread>();
		final List<PID> pidList = new ArrayList<PID>();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				for(PID n : pidList) {
					if(n != null) {
						n.shutdown();
					}
				}

			}
		});
		Thread hlt_thread = null;
		Thread mlt_thread = null;
		Thread kettle_thread = null;

		readConfig();

		try {
			BrewServer bServer = new BrewServer(this);
		} catch (IOException e) {
			System.out.print("IOException starting the brew server");
			e.printStackTrace();
		}
		

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

		//build an MLT and start it running
		if(!mlt_probe.equals("") && !mlt_probe.equals("0")) {
			mltTemp = new Temp(mlt_probe);
			mltTemp.setScale(scale);
			Thread mltTemp_thread = new Thread(mltTemp);
			mltTemp_thread.start();
			if(mlt_gpio != -1) {
				mlt = new PID(mltTemp, "HLT", mlt_duty, mlt_cycle, mlt_p, mlt_i, mlt_k, mlt_gpio);
				mlt_thread = new Thread( mlt );
				mlt_thread.start();
			}
			pidList.add(mlt);
			procList.add(mlt_thread);
		}

		//build a Kettle and start it running
		if(!kettle_probe.equals("") && !kettle_probe.equals("0")) {
			kettleTemp = new Temp(kettle_probe);
			kettleTemp.setScale(scale);
			Thread kettleTemp_thread = new Thread(kettleTemp);
			kettleTemp_thread.start();
			if(kettle_gpio != -1) {
				kettle = new PID(kettleTemp, "HLT", kettle_duty, kettle_cycle, kettle_p, kettle_i, kettle_k, kettle_gpio);
				kettle_thread = new Thread( kettle );
				kettle_thread.start();
			}
			pidList.add(kettle);
			procList.add(kettle_thread);
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
			else if(inputBroken[0].equalsIgnoreCase("mlt")) {
				if(mlt_thread != null) {
					// not here, dump it
					System.out.println(mlt.getStatus());
				}else if(mltTemp != null) {
					// not here, dump it
					System.out.println(mltTemp.getTemp() + "@" + mltTemp.getTime());
				} else {
					System.out.println("MLT Thread isn't started");
				}
			}
			else if(inputBroken[0].equalsIgnoreCase("hlt")) {
				if(hlt_thread != null) {
					// not here, dump it
					System.out.println(hlt.getStatus());
				}else if(hltTemp != null) {
					// not here, dump it
					System.out.println(hltTemp.getTemp() + "@" + hltTemp.getTime());
				} else {
					System.out.println("HLT Thread isn't started");
				}
				
			}
			else if(inputBroken[0].equalsIgnoreCase("kettle")) {
				if(kettle_thread != null) {
					// not here, dump it
					System.out.println(kettle.getStatus());
				}else if(kettleTemp != null) {
					// not here, dump it
					System.out.println(kettleTemp.getTemp() + "@" + kettleTemp.getTime());
				} else {
					System.out.println("Kettle Thread isn't started");
				}

			}
			else if(inputBroken[0].equalsIgnoreCase("list")) {
				for(PID p : pidList) {
					if(p != null)
					System.out.println(p.getStatus());
				}
			}
		}
	}

	public String getJSONStatus() {
		// get each setting add it to the JSON
		JSONObject rObj = new JSONObject();

		String active_devices = "";

		// init the params in case there's no value
		mlt_temp = mlt_duty = mlt_cycle = mlt_setpoint = mlt_k = mlt_i = mlt_p = 0.0;

		hlt_temp = hlt_duty = hlt_cycle = hlt_setpoint = hlt_k = hlt_i = hlt_p = 0.0;

		kettle_temp = kettle_duty = kettle_cycle = kettle_setpoint = kettle_k = kettle_i = kettle_p = 0.0;

		JSONObject mltJSON = null;
		JSONObject hltJSON = null;
		JSONObject kettleJSON = null;

		if(mlt != null) {
			mltJSON = new JSONObject();
			mltJSON.put("temp", mltTemp.getTemp());
			mltJSON.put("mode", mlt.getMode());
			mltJSON.put("gpio", mlt.getGPIO());
			mltJSON.put("duty", mlt.getDuty());
			mltJSON.put("cycle", mlt.getCycle());
			mltJSON.put("setpoint", mlt.getSetPoint());
			mltJSON.put("p", mlt.getP());
			mltJSON.put("i", mlt.getI());
			mltJSON.put("k", mlt.getK());
			mltJSON.put("elapsed", mltTemp.getTime());
			mltJSON.put("scale", mltTemp.getScale());
			rObj.put("mlt_pid", mltJSON);
		} else if(mltTemp != null ) {
			mltJSON = new JSONObject();
			mltJSON.put("temp", mltTemp.getTemp());
			mltJSON.put("elapsed", mltTemp.getTime());
			mltJSON.put("scale", mltTemp.getScale());
			rObj.put("mlt_temp", mltJSON);
		}
		
		if(hlt != null) {
			hltJSON = new JSONObject();
			hltJSON.put("temp", hltTemp.getTemp());
			hltJSON.put("mode", hlt.getMode());
			hltJSON.put("gpio", hlt.getGPIO());
			hltJSON.put("duty", hlt.getDuty());
			hltJSON.put("cycle", hlt.getCycle());
			hltJSON.put("setpoint", hlt.getSetPoint());
			hltJSON.put("p", hlt.getP());
			hltJSON.put("i", hlt.getI());
			hltJSON.put("k", hlt.getK());
			hltJSON.put("elapsed", hltTemp.getTime());
			hltJSON.put("scale", hltTemp.getScale());
			rObj.put("hlt_pid", hltJSON);
		} else if(hltTemp != null ) {
			hltJSON = new JSONObject();
			hltJSON.put("temp", hltTemp.getTemp());
			hltJSON.put("elapsed", hltTemp.getTime());
			hltJSON.put("scale", hltTemp.getScale());
			rObj.put("hlt_temp", hltJSON);
		} 

		if(kettle != null) {
			kettleJSON = new JSONObject();
			kettleJSON.put("temp", kettleTemp.getTemp());
			kettleJSON.put("mode", kettle.getMode());
			kettleJSON.put("gpio", kettle.getGPIO());
			kettleJSON.put("duty", kettle.getDuty());
			kettleJSON.put("cycle", kettle.getCycle());
			kettleJSON.put("setpoint", kettle.getSetPoint());
			kettleJSON.put("p", kettle.getP());
			kettleJSON.put("i", kettle.getI());
			kettleJSON.put("k", kettle.getK());
			kettleJSON.put("elapsed", kettleTemp.getTime());
			kettleJSON.put("scale", kettleTemp.getScale());
			rObj.put("kettle_pid", kettleJSON);
		} else if(kettleTemp != null ) {
			kettleJSON = new JSONObject();
			kettleJSON.put("temp", kettleTemp.getTemp());
			kettleJSON.put("elapsed", kettleTemp.getTime());
			kettleJSON.put("scale", kettleTemp.getScale());
			rObj.put("kettle_temp", kettleJSON);
		} 

		StringWriter out = new StringWriter();
		try {
			rObj.writeJSONString(out);
		} catch (IOException e) {
			System.out.print("IOException generating JSON String");
		}
		return out.toString();

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

		try {
			if(config.hasSection("general")) {
				if(config.hasOption("general", "scale")) {
					scale = config.get("general", "scale");
				}
			}
			if(config.hasSection("mlt")) {
				if(config.hasOption("mlt", "probe")) {
					mlt_probe = config.get("mlt", "probe");
				}
				if(config.hasOption("mlt", "gpio")) {
					mlt_gpio = config.getInt("mlt", "gpio");
				}
				if(config.hasOption("mlt", "duty_cycle")) {
					mlt_duty = config.getDouble("mlt", "duty_cycle");
				}
				if(config.hasOption("mlt", "cycle_time")) {
					mlt_cycle = config.getDouble("mlt", "cycle_time");
				}
				if(config.hasOption("mlt", "set_point")) {
					mlt_setpoint = config.getDouble("mlt", "set_point");
				}
				if(config.hasOption("mlt", "k_param")) {
					mlt_k = config.getDouble("mlt", "k_param");
				}
				if(config.hasOption("mlt", "p_param")) {
					mlt_p = config.getDouble("mlt", "p_param");
				}
				if(config.hasOption("mlt", "i_param")) {
					mlt_i = config.getDouble("mlt", "i_param");
				}
			}
			if(config.hasSection("hlt")) {
				if(config.hasOption("hlt", "probe")) {
					hlt_probe = config.get("hlt", "probe");
				}
				if(config.hasOption("hlt", "gpio")) {
					hlt_gpio = config.getInt("hlt", "gpio");
				}
				if(config.hasOption("hlt", "duty_cycle")) {
					hlt_duty = config.getDouble("hlt", "duty_cycle");
				}
				if(config.hasOption("hlt", "cycle_time")) {
					hlt_cycle = config.getDouble("hlt", "cycle_time");
				}
				if(config.hasOption("hlt", "set_point")) {
					hlt_setpoint = config.getDouble("hlt", "set_point");
				}
				if(config.hasOption("hlt", "k_param")) {
					hlt_k = config.getDouble("hlt", "k_param");
				}
				if(config.hasOption("hlt", "p_param")) {
					hlt_p = config.getDouble("hlt", "p_param");
				}
				if(config.hasOption("hlt", "i_param")) {
					hlt_i = config.getDouble("hlt", "i_param");
				}
			}
			if(config.hasSection("kettle")) {
				if(config.hasOption("kettle", "probe")) {
					kettle_probe = config.get("kettle", "probe");
				}
				if(config.hasOption("kettle", "gpio")) {
					kettle_gpio = config.getInt("kettle", "gpio");
				}
				if(config.hasOption("kettle", "duty_cycle")) {
					kettle_duty = config.getDouble("kettle", "duty_cycle");
				}
				if(config.hasOption("kettle", "cycle_time")) {
					kettle_cycle = config.getDouble("kettle", "cycle_time");
				}
				if(config.hasOption("kettle", "set_point")) {
					kettle_setpoint = config.getDouble("kettle", "set_point");
				}
				if(config.hasOption("kettle", "k_param")) {
					kettle_k = config.getDouble("kettle", "k_param");
				}
				if(config.hasOption("kettle", "p_param")) {
					kettle_p = config.getDouble("kettle", "p_param");
				}
				if(config.hasOption("kettle", "i_param")) {
					kettle_i = config.getDouble("kettle", "i_param");
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

	private String mlt_probe = ""; private String hlt_probe = ""; private String kettle_probe = "";
	private String mlt_mode = ""; private String hlt_mode = ""; private String kettle_mode = "";
	private int mlt_gpio = -1; private int hlt_gpio = -1; private int kettle_gpio = -1;
	private long mlt_elapsed = 0;
	private long hlt_elapsed = 0;
	private long kettle_elapsed = 0;
	private double mlt_temp, mlt_duty = 4, mlt_cycle = 0, mlt_setpoint = 175, mlt_k = 44, mlt_i = 165, mlt_p = 4;
	private double hlt_temp, hlt_duty = 4, hlt_cycle = 0, hlt_setpoint = 175, hlt_k = 44, hlt_i = 165, hlt_p = 4;
	private double kettle_temp, kettle_duty = 4, kettle_cycle = 0, kettle_setpoint = 175, kettle_k = 44, kettle_i = 165, kettle_p = 4;
}
