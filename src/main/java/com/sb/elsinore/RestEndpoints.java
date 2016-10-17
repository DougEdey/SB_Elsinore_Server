package com.sb.elsinore;

import com.sb.elsinore.annotations.RestEndpoint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.sb.elsinore.BrewServer.MIME_TYPES;
import static com.sb.elsinore.UrlEndpoints.INPUTUNIT;

/**
 * This class hosts the REST based endpoints for Angular et al
 * Created by Douglas on 2016-03-31.
 */
@SuppressWarnings("Duplicates")
public class RestEndpoints {
    public static final String FORM = "form";
    public File rootDir;
    public Map<String, String> parameters = null;
    public Map<String, String> files = null;
    public Map<String, String> header = null;

    public static final String MIME_HTML = "text/html";

    public Map<String, String> ParseParams() {
        return ParseParams(this.parameters);
    }

    /**
     * Convert a JSON parameter set into a hashmap.
     *
     * @param params The incoming parameter listing
     * @return The converted parameters
     */
    public static Map<String, String> ParseParams(Map<String, String> params) {
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
            inputUnit = params.get(FORM);
            parms = params;
        }

        if (inputUnit != null) {
            parms.put(INPUTUNIT, inputUnit);
        }

        if (parms.size() == 0) {
            return params;
        }

        return parms;
    }

    @RestEndpoint(path = "probes", method = NanoHTTPD.Method.GET, help = "Lists all the Temperature probes in the system")
    public NanoHTTPD.Response GetTemperatureProbes() {
        NanoHTTPD.Response.Status status = NanoHTTPD.Response.Status.INTERNAL_ERROR;
        JSONObject responseJSON = new JSONObject();
        if (LaunchControl.getInstance().tempList.size() == 0) {
            status = NanoHTTPD.Response.Status.NO_CONTENT;
            responseJSON.put("Error", "No temperature probes");
        } else {
            for (Temp t : LaunchControl.getInstance().tempList) {
                responseJSON.put(t.getName(), t.getProbe());
            }
        }
        return new NanoHTTPD.Response(status, MIME_TYPES.get("json"), responseJSON.toJSONString());
    }

}
