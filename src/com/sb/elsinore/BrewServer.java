package com.sb.elsinore;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.sb.elsinore.NanoHTTPD.Response.Status;

public class BrewServer extends NanoHTTPD {
	
	
	private File rootDir;
    public final static Logger log = Logger.getLogger("com.sb.manager.Server");


	/**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("xml", "text/xml");
        put("txt", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("ogg", "application/x-ogg");
        put("zip", "application/octet-stream");
        put("exe", "application/octet-stream");
        put("class", "application/octet-stream");
    }};

    
	public BrewServer (int port) throws IOException{

		super(port);
		// just serve up on port 8080 for now
		BrewServer.log.info("Launching on port " + port);
		
		
		//setup the logging handlers
        Handler[] lH = log.getHandlers();
        for (Handler h : lH) {
                h.setLevel(Level.INFO);
        }

        if(lH.length == 0) {
                log.addHandler(new ConsoleHandler());
                
                // default level, this can be changed
                try {
                	log.info("Debug System property: "+System.getProperty("debug"));
                	if (System.getProperty("debug").equalsIgnoreCase("INFO")) {
                	
                		log.setLevel(Level.INFO);
                		log.info("Enabled logging at an info level");
                	} 
                } catch (NullPointerException e) {
                	log.setLevel(Level.WARNING);
                }
                
        }

		rootDir = new File(BrewServer.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath()).getParentFile();
		BrewServer.log.info("Root Directory is: " + rootDir.toString());
				
		if(rootDir.exists() && rootDir.isDirectory()) {
			BrewServer.log.info("Root directory: " + rootDir.toString());
		}


	}

	private boolean updateMashProfile(Map<String, String> parms) {
		String inputUnit = null;
		Set<Entry<String, String>> incomingParams = parms.entrySet();
		Iterator<Entry<String, String>> it = incomingParams.iterator();
		Entry<String, String> param = null;
		JSONObject incomingData = new JSONObject();
		
		JSONParser parser = new JSONParser();
		
		// Try to Parse JSON Data
		while (it.hasNext()) {
			param = it.next();
			BrewServer.log.info("Key: " + param.getKey());
			BrewServer.log.info("Entry: " + param.getValue());
			try {
				Object parsedData = parser.parse(param.getKey());
				if (parsedData instanceof JSONArray) {
					incomingData = (JSONObject) ((JSONArray)parsedData).get(0);
				} else {
					incomingData = (JSONObject) parsedData;
				}
			} catch (Exception e) {
				BrewServer.log.info("couldn't read " + param.getValue() + " as a JSON Value " + e.getMessage());
			}
		
		}
	
		if (incomingData != null) {
			// Use the JSON Data
			BrewServer.log.info("Found valid data for " + inputUnit);
			System.out.println(incomingData.toJSONString());
			
			if (LaunchControl.mashObject == null) {
				LaunchControl.mashObject = new MashControl();
			}
			
			// Default to Fahrenheit
			String temp_unit = "F";
			if (incomingData.containsKey("temp_unit")) {
				temp_unit = incomingData.get("temp_unit").toString();
				incomingData.remove("temp_unit");
			}
			
			if (incomingData.containsKey("pid")) {
				LaunchControl.mashObject.outputControl = incomingData.get("pid").toString();
				incomingData.remove("pid");
			}
			
			// Iterate through the JSON
			for (Object key: incomingData.keySet()) {
				String cKey = key.toString();
			
				try {
					int stepCount = Integer.parseInt(cKey);
					// We have a good step count, parse the value
					JSONObject valueObj = (JSONObject) incomingData.get(key);
					
					int duration = Integer.parseInt(valueObj.get("duration").toString());
					double temp = Double.parseDouble(valueObj.get("temp").toString());
					
					String method = valueObj.get("method").toString();
					String type = valueObj.get("type").toString();
					
					// Add the step
					MashStep newStep = LaunchControl.mashObject.addMashStep(stepCount);
					newStep.setDuration(duration);
					newStep.setTemp(temp);
					newStep.setMethod(method);
					newStep.setType(type);
					
					BrewServer.log.info(newStep.toString());
					
				} catch (NumberFormatException e) {
					// Couldn't work out what this is
					BrewServer.log.warning("Couldn't parse " + cKey);
					BrewServer.log.warning(incomingData.get(key).toString());
				}
			}
			
		}
		return true;
	}
	
	public Response serve( String uri, Method method,  Map<String, String> header, Map<String, String> parms, Map<String, String> files)
	{

		BrewServer.log.info("URL : " + uri + " method: " + method);
		if(method == Method.POST) {
			// parms contains the properties here
			if(uri.toLowerCase().equals("/mashprofile")) {
				if (updateMashProfile(parms)) {
					return new NanoHTTPD.Response( Status.OK, MIME_HTML, "Updated MashProfile" );
				}
				return new NanoHTTPD.Response( Status.BAD_REQUEST, MIME_HTML, "Failed to update Mashprofile" );
			}
			
			if(uri.toLowerCase().equals("/updatepid")) {
				// parse the values if possible
				
				String temp, mode = "off", inputUnit = null;
				double dTemp, duty = 0, cycle = 4, setpoint = 175, p = 0, i = 0, d = 0;
				Set<Entry<String, String>> incomingParams = parms.entrySet();
				Iterator<Entry<String, String>> it = incomingParams.iterator();
				Entry<String, String> param = null;
				JSONObject incomingData = null;
				
				JSONParser parser = new JSONParser();
				
				// Try to Parse JSON Data
				while (it.hasNext()) {
					param = it.next();
					BrewServer.log.info("Key: " + param.getKey());
					BrewServer.log.info("Entry: " + param.getValue());
					try {
						Object parsedData = parser.parse(param.getValue());
						if (parsedData instanceof JSONArray) {
							incomingData = (JSONObject) ((JSONArray)parsedData).get(0);
						} else {
							incomingData = (JSONObject) parsedData;
							inputUnit = param.getKey();
						}
					} catch (Exception e) {
						BrewServer.log.info("couldn't read " + param.getValue() + " as a JSON Value " + e.getMessage());
					}
				
				}
			
				if (incomingData != null) {
					// Use the JSON Data
					BrewServer.log.info("Found valid data for " + inputUnit);
					parms = incomingData;
				} else {
					inputUnit = parms.get("form");
				}
				
				// Fall back to the old style
				if(parms.containsKey("dutycycle")) {
					temp = parms.get("dutycycle");
					try {
						dTemp = Double.parseDouble(temp);
						duty = dTemp;
						BrewServer.log.info("Duty cycle: " + duty);
					} catch (NumberFormatException nfe) {
						System.out.print("Bad duty");
					}
				}
				
				if(parms.containsKey("cycletime")) {
					temp = parms.get("cycletime");
					try {
						dTemp = Double.parseDouble(temp);
						cycle = dTemp;
						BrewServer.log.info("Cycle time: " + cycle);
					} catch (NumberFormatException nfe) {
						BrewServer.log.info("Bad cycle");
					}
				}
				
				if(parms.containsKey("setpoint")) {
					temp = parms.get("setpoint");
					try {
						dTemp = Double.parseDouble(temp);
						setpoint = dTemp;
						BrewServer.log.info("Set Point: " + setpoint);
					} catch (NumberFormatException nfe) {
						BrewServer.log.info("Bad setpoint");
					}
				}
				
				if(parms.containsKey("p")) {
					temp = parms.get("p");
					try {
						dTemp = Double.parseDouble(temp);
						p = dTemp;
						BrewServer.log.info("P: " + p);
					} catch (NumberFormatException nfe) {
						BrewServer.log.info("Bad p");
					}
				}
				
				if(parms.containsKey("i")) {
					temp = parms.get("i");
					try {
						dTemp = Double.parseDouble(temp);
						i = dTemp;
						BrewServer.log.info("I: " + i);
					} catch (NumberFormatException nfe) {
						BrewServer.log.info("Bad i");
					}
				}
				
				if(parms.containsKey("d")) {
					temp = parms.get("d");
					try {
						dTemp = Double.parseDouble(temp);
						d =  dTemp;
						BrewServer.log.info("D: " + d);
					} catch (NumberFormatException nfe) {
						BrewServer.log.info("Bad d");
					}
				}
				
				if(parms.containsKey("mode")) {
					mode = parms.get("mode");
					BrewServer.log.info("Mode: " + mode);
				}

				
				BrewServer.log.info("Form: " + inputUnit);

				PID tPID = LaunchControl.findPID(inputUnit);
				if(tPID != null) {
					BrewServer.log.info(mode +":" + duty + ":" + cycle +":"+ setpoint +":"+ p+":"+ i+":"+ d );
					tPID.updateValues(mode, duty, cycle, setpoint, p, i, d );
				} else {
					BrewServer.log.warning("Attempted to update a non existent PID, " + inputUnit + ". Please check your client");
				}
				return new NanoHTTPD.Response( Status.OK, MIME_HTML, "Updated PID" );
			}

			if(uri.toLowerCase().equals("/updateday")) {
				// we're storing the data for the brew day
				String tempDateStamp;

				BrewDay brewDay = LaunchControl.getBrewDay();

				// updated date
				if(parms.containsKey("updated")) {
					tempDateStamp = parms.get("updated");
					brewDay.setUpdated(tempDateStamp);
				} else {
					// we don't have an updated datestamp. Don't do anything for now
					return new NanoHTTPD.Response( Status.OK, MIME_HTML, "No update datestamp, not updating a thang! YA HOSER!" );
				}

				Iterator<Entry<String, String>> it = parms.entrySet().iterator();
				
				Entry<String, String> e = null;
				
				while (it.hasNext()) {
					e = it.next();
					
					if (e.getKey().endsWith("Start")) {
						int trimEnd = e.getKey().length() - 5;
						String name = e.getKey().substring(0, trimEnd);
						brewDay.startTimer(name, e.getValue());
					} else if (e.getKey().endsWith("End"))  {
						int trimEnd = e.getKey().length() - 3;
						String name = e.getKey().substring(0, trimEnd);
						brewDay.stopTimer(name, e.getValue());
					}
				}

				return new NanoHTTPD.Response( Status.OK, MIME_HTML, "Updated Brewday" );
			}
			
			if(uri.toLowerCase().equals("/updatepump")) {
				if(parms.containsKey("toggle")) {
					String pumpname = parms.get("toggle");
					Pump tempPump = LaunchControl.findPump(pumpname);
					if (tempPump != null) {
						if(tempPump.getStatus()) {
							tempPump.turnOff();
						} else {
							tempPump.turnOn();
						}
						return new NanoHTTPD.Response( Status.OK, MIME_HTML, "Updated Pump" );
					} else {
						log.warning("Invalid pump: " + pumpname + " provided.");
					}
				}
			}
			
			if(uri.toLowerCase().equals("/toggleaux")) {
				if(parms.containsKey("toggle")) {
					String pidname = parms.get("toggle");
					PID tempPID = LaunchControl.findPID(pidname);
					if (tempPID != null) {
						tempPID.toggleAux();
						return new NanoHTTPD.Response( Status.OK, MIME_HTML, "Updated Aux for " + pidname );
					} else {
						log.warning("Invalid PID: " + pidname + " provided.");
					}
				}
			}
		}
		
		if(uri.toLowerCase().equals("/getstatus")) {
			
			return new NanoHTTPD.Response( Status.OK, MIME_HTML, LaunchControl.getJSONStatus() );
		}
				
		if (uri.toLowerCase().equals("/controller")) {
			return new NanoHTTPD.Response( Status.OK, MIME_HTML, LaunchControl.getControlPage());
		}
		
		if (uri.toLowerCase().equals("/timers")) {
			return new NanoHTTPD.Response( Status.OK, MIME_HTML, LaunchControl.getBrewDay().brewDayStatus().toString());
		}
		
		if(new File(rootDir, uri).exists()) {
            return serveFile(uri, header, rootDir);
		}

		BrewServer.log.info("Invalid URI: " + uri);
		return new NanoHTTPD.Response( Status.OK, MIME_HTML, "Unrecognized URL");
	}

	/**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI, ignores all headers and HTTP parameters.
     */
    public Response serveFile(String uri, Map<String, String> header, File homeDir) {
        Response res = null;

        // Make sure we won't die of an exception later
        if (!homeDir.isDirectory())
            res = new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");

        if (res == null) {
            // Remove URL arguments
            uri = uri.trim().replace(File.separatorChar, '/');
            if (uri.indexOf('?') >= 0)
                uri = uri.substring(0, uri.indexOf('?'));

            // Prohibit getting out of current directory
            if (uri.startsWith("src/main") || uri.endsWith("src/main") || uri.contains("../"))
                res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
        }

        File f = new File(homeDir, uri);
        if (res == null && !f.exists())
            res = new Response(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");

        // List the directory, if necessary
        if (res == null && f.isDirectory()) {
            // Browsers get confused without '/' after the
            // directory, send a redirect.
            if (!uri.endsWith("/")) {
                uri += "/";
                res = new Response(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri
                        + "</a></body></html>");
                res.addHeader("Location", uri);
            }

            if (res == null) {
                // First try index.html and index.htm
                if (new File(f, "index.html").exists())
                    f = new File(homeDir, uri + "/index.html");
                else if (new File(f, "index.htm").exists())
                    f = new File(homeDir, uri + "/index.htm");
                    // No index file, list the directory if it is readable
                else if (f.canRead()) {
                    String[] files = f.list();
                    String msg = "<html><body><h1>Directory " + uri + "</h1><br/>";

                    if (uri.length() > 1) {
                        String u = uri.substring(0, uri.length() - 1);
                        int slash = u.lastIndexOf('/');
                        if (slash >= 0 && slash < u.length())
                            msg += "<b><a href=\"" + uri.substring(0, slash + 1) + "\">..</a></b><br/>";
                    }

                    if (files != null) {
                        for (int i = 0; i < files.length; ++i) {
                            File curFile = new File(f, files[i]);
                            boolean dir = curFile.isDirectory();
                            if (dir) {
                                msg += "<b>";
                                files[i] += "/";
                            }

                            msg += "<a href=\"" + encodeUri(uri + files[i]) + "\">" + files[i] + "</a>";

                            // Show file size
                            if (curFile.isFile()) {
                                long len = curFile.length();
                                msg += " &nbsp;<font size=2>(";
                                if (len < 1024)
                                    msg += len + " bytes";
                                else if (len < 1024 * 1024)
                                    msg += len / 1024 + "." + (len % 1024 / 10 % 100) + " KB";
                                else
                                    msg += len / (1024 * 1024) + "." + len % (1024 * 1024) / 10 % 100 + " MB";

                                msg += ")</font>";
                            }
                            msg += "<br/>";
                            if (dir)
                                msg += "</b>";
                        }
                    }
                    msg += "</body></html>";
                    res = new Response(msg);
                } else {
                    res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
                }
            }
        }
        try {
            if (res == null) {
                // Get MIME type from file name extension, if possible
                String mime = null;
                int dot = f.getCanonicalPath().lastIndexOf('.');
                if (dot >= 0)
                    mime = MIME_TYPES.get(f.getCanonicalPath().substring(dot + 1).toLowerCase());
                if (mime == null)
                    mime = NanoHTTPD.MIME_HTML;

                // Calculate etag
                String etag = Integer.toHexString((f.getAbsolutePath() + f.lastModified() + "" + f.length()).hashCode());

                // Support (simple) skipping:
                long startFrom = 0;
                long endAt = -1;
                String range = header.get("range");
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length());
                        int minus = range.indexOf('-');
                        try {
                            if (minus > 0) {
                                startFrom = Long.parseLong(range.substring(0, minus));
                                endAt = Long.parseLong(range.substring(minus + 1));
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // Change return code and add Content-Range header when skipping is requested
                long fileLen = f.length();
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = new Response(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                        res.addHeader("ETag", etag);
                    } else {
                        if (endAt < 0)
                            endAt = fileLen - 1;
                        long newLen = endAt - startFrom + 1;
                        if (newLen < 0)
                            newLen = 0;

                        final long dataLen = newLen;
                        FileInputStream fis = new FileInputStream(f) {
                            @Override
                            public int available() throws IOException {
                                return (int) dataLen;
                            }
                        };
                        fis.skip(startFrom);

                        res = new Response(Response.Status.PARTIAL_CONTENT, mime, fis);
                        res.addHeader("Content-Length", "" + dataLen);
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                } else {
                    if (etag.equals(header.get("if-none-match")))
                        res = new Response(Response.Status.NOT_MODIFIED, mime, "");
                    else {
                        res = new Response(Response.Status.OK, mime, new FileInputStream(f));
                        res.addHeader("Content-Length", "" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                }
            }
        } catch (IOException ioe) {
            res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        res.addHeader("Accept-Ranges", "bytes"); // Announce that the file server accepts partial content requestes
        return res;
    }
    
    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20' instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/"))
                newUri += "/";
            else if (tok.equals(" "))
                newUri += "%20";
            else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

}
