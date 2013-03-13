import java.io.*;
import java.util.*;


public class BrewServer extends NanoHTTPD {
	
	private LaunchControl bLauncher;
	private File rootDir;
	public BrewServer (LaunchControl parent) throws IOException{

		
		// just serve up on port 8080 for now
		super(8080, new File("/var/www/templates/"));
		rootDir = new File("/var/www/templates/");
		if(rootDir.exists() && rootDir.isDirectory()) {
			System.out.println(rootDir.toString());
		}
		bLauncher = parent;

	}

	public Response serve( String uri, String method, Properties header, Properties params, Properties files )
	{

//		System.out.print("URL : " + uri + " mehtod: " + method);
		if(method.equalsIgnoreCase("POST")) {
			// parms contains the properties here
				// parse the values if possible

				String temp, mode = "off";
				double dTemp, duty = 0, cycle = 4, setpoint = 175, k = 44, i = 165, p = 4;
				if((temp = params.getProperty("dutycycle")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						duty = dTemp;
						System.out.println("Duty cycle: " + duty);
					} catch (NumberFormatException nfe) {
						System.out.print("Bad duty");
					}
				}
				if((temp = params.getProperty("cycletime")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						cycle = dTemp;
					} catch (NumberFormatException nfe) {
						System.out.print("Bad cycle");
					}
				}
				if((temp = params.getProperty("setpoint")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						setpoint = dTemp;
					} catch (NumberFormatException nfe) {
						System.out.print("Bad setpoint");
					}
				}
				if((temp = params.getProperty("k")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						k = dTemp;
					} catch (NumberFormatException nfe) {
						System.out.print("Bad k");
					}
				}
				if((temp = params.getProperty("i")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						i = dTemp;
					} catch (NumberFormatException nfe) {
						System.out.print("Bad i");
					}
				}
				if((temp = params.getProperty("p")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						p = dTemp;
					} catch (NumberFormatException nfe) {
						System.out.print("Bad p");
					}
				}
				if((temp = params.getProperty("mode")) != null) {
					mode = temp;
				}
			String inputUnit = params.getProperty("form");
			System.out.println("Form: " + inputUnit);

			PID tPID = bLauncher.findPID(inputUnit);
			if(tPID != null) {
				tPID.updateValues(mode, duty, cycle, setpoint, p, i, k );
			}
		}
		if(uri.toLowerCase().contains("getstatus")) {
			
			return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, bLauncher.getJSONStatus() );
		}

		return serveFile( uri, header, rootDir, true );
	}


}
