package com.sb.elsinore;

import com.sb.elsinore.annotations.Parameter;
import com.sb.elsinore.annotations.RestEndpoint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class hosts the REST based endpoints for Angular et al
 * Created by Douglas on 2016-03-31.
 */
@SuppressWarnings("Duplicates")
public class RestEndpoints {
    public File rootDir;
    public Map<String, String> parameters = null;
    public Map<String, String> files = null;
    public Map<String, String> header = null;

    public static final Map<String, String> MIME_TYPES
            = new HashMap<String, String>() {
        /**
         * The Serial UID.
         */
        public static final long serialVersionUID = 1L;
        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("axml", "application/xml");
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
            put("json", "application/json");
        }
    };
    public static final String MIME_HTML = "text/html";

    public Map<String, String> ParseParams() {
        return this.ParseParams(this.parameters);
    }
    /**
     * Convert a JSON parameter set into a hashmap.
     * @param params The incoming parameter listing
     * @return The converted parameters
     */
    public Map<String, String> ParseParams(Map<String, String> params) {
        Set<Map.Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Map.Entry<String, String>> it = incomingParams.iterator();
        Map.Entry<String, String> param;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();
        String inputUnit = null;

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            BrewServer.LOG.info("Key: " + param.getKey());
            BrewServer.LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getValue());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                    inputUnit = param.getKey();
                }
            } catch (Exception e) {
                BrewServer.LOG.info("couldn't read " + param.getValue()
                        + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null) {
            // Use the JSON Data
            BrewServer.LOG.info("Found valid data for " + inputUnit);
            parms = (Map<String, String>) incomingData;
        } else {
            inputUnit = params.get("form");
            parms = params;
        }

        if (inputUnit != null) {
            parms.put("inputunit", inputUnit);
        }

        return parms;
    }
    @RestEndpoint(path="probes", method = NanoHTTPD.Method.GET, help = "Lists all the Temperature probes in the system")
    public NanoHTTPD.Response GetTemperatureProbes()
    {
        NanoHTTPD.Response.Status status = NanoHTTPD.Response.Status.INTERNAL_ERROR;
        JSONObject responseJSON = new JSONObject();
        if (LaunchControl.tempList.size() == 0)
        {
            status = NanoHTTPD.Response.Status.NO_CONTENT;
            responseJSON.put("Error", "No temperature probes");
        }
        else
        {
            for(Temp t: LaunchControl.tempList)
            {
                responseJSON.put(t.getName(), t.getProbe());
            }
        }
        return new NanoHTTPD.Response(status, MIME_TYPES.get("json"), responseJSON.toJSONString());
    }

}
