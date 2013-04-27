import java.io.*;
import java.util.*;


public class BrewServer extends NanoHTTPD {
	
	private LaunchControl bLauncher;
	private File rootDir;
	public BrewServer (LaunchControl parent) throws IOException{

		
		// just serve up on port 8080 for now
		super(8080, new File(System.getProperty("user.dir")));
		rootDir = new File(System.getProperty("user.dir"));
		if(rootDir.exists() && rootDir.isDirectory()) {
			System.out.println(rootDir.toString());
		}
		bLauncher = parent;

	}

	public Response serve( String uri, String method, Properties header, Properties params, Properties files )
	{

		System.out.println("URL : " + uri + " method: " + method);
		if(method.equalsIgnoreCase("POST")) {
			// parms contains the properties here
			if(uri.toLowerCase().contains("updatepid")) {
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
				if((temp = params.getProperty("d")) != null) {
					try {
						dTemp = Double.parseDouble(temp);
						p = dTemp;
					} catch (NumberFormatException nfe) {
						System.out.print("Bad d");
					}
				}
				if((temp = params.getProperty("mode")) != null) {
					mode = temp;
				}
				String inputUnit = params.getProperty("form");
				System.out.println("Form: " + inputUnit);

				PID tPID = bLauncher.findPID(inputUnit);
				if(tPID != null) {
					System.out.println(mode +":" + duty + ":" + cycle +":"+ setpoint +":"+ p+":"+ i+":"+ k );
					tPID.updateValues(mode, duty, cycle, setpoint, p, i, k );
				}
				return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, "Updated PID" );
			}

			if(uri.toLowerCase().contains("updateday")) {
				// we're storing the data for the brew day
				String tempDateStamp;

				BrewDay brewDay = bLauncher.getBrewDay();

				// updated date
				if((tempDateStamp = params.getProperty("updated")) != null) {
					brewDay.setStart(tempDateStamp);
				} else {
					// we don't have an updated datestamp. Don't do anything for now
					return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, "No update datestamp, not updating a thang! YA HOSER!" );
				}

				// start of brew day
				if((tempDateStamp = params.getProperty("startDay")) != null) {
					brewDay.setStart(tempDateStamp);
				}

				// start mashing in
				if((tempDateStamp = params.getProperty("mashIn")) != null) {
					brewDay.setMashIn(tempDateStamp);
				}

				// start mashing out
				if((tempDateStamp = params.getProperty("mashOut")) != null) {
					brewDay.setMashOut(tempDateStamp);
				}

				// start sparging
				if((tempDateStamp = params.getProperty("spargeStart")) != null) {
					brewDay.setSpargeStart(tempDateStamp);
				}

				// stop sparging
				if((tempDateStamp = params.getProperty("spargeEnd")) != null) {
					brewDay.setSpargeEnd(tempDateStamp);
				}

				//  start boiling
				if((tempDateStamp = params.getProperty("boilStart")) != null) {
					brewDay.setBoilStart(tempDateStamp);
				}

				// start chilling
				if((tempDateStamp = params.getProperty("chillStart")) != null) {
					brewDay.setChillStart(tempDateStamp);
				}

				// stop chilling
				if((tempDateStamp = params.getProperty("chillEnd")) != null) {
					brewDay.setChillEnd(tempDateStamp);
				}

				return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, "Updated Brewday" );
			}

		}
		if(uri.toLowerCase().contains("getstatus")) {
			
			return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, bLauncher.getJSONStatus() );
		}
		if(uri.toLowerCase().contains("getimages")) {
			// try to get the String start and end
			//Setup the defaults first
			String startDate = params.getProperty("startDate");
			String endDate = params.getProperty("endDate");
			if(startDate == null || endDate == null) {
				return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, bLauncher.getCosmImages(null, null) );
			}

			return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, bLauncher.getCosmImages(startDate, endDate) );
		}

		return serveFile( uri, header, rootDir, true );
	}


}
