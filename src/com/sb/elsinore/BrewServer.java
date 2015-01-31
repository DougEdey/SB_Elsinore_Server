package com.sb.elsinore;

import jGPIO.InvalidGPIOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rendersnake.HtmlCanvas;

import com.sb.elsinore.NanoHTTPD.Response.Status;
import com.sb.elsinore.NanoHTTPD.Response;
import com.sb.elsinore.annotations.UrlEndpoint;
import com.sb.elsinore.html.PhSensorForm;
import com.sb.elsinore.html.RenderHTML;
import com.sb.elsinore.html.VolumeEditForm;
import com.sb.elsinore.inputs.PhSensor;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * A custom HTTP server for Elsinore.
 * Designed to be very simple and lightweight.
 *
 * @author Doug Edey
 *
 */
public class BrewServer extends NanoHTTPD {

    /**
     * The Root Directory of the files to be served.
     */
    public File rootDir;

    /**
     * The Logger object.
     */
    public static final Logger LOG = Logger.getLogger("com.sb.manager.Server");

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE.
     */
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

    /**
     * Constructor to create the HTTP Server.
     *
     * @param port
     *            The port to run on
     * @throws IOException
     *             If there's an issue starting up
     */
    public BrewServer(final int port) throws IOException {

        super(port);
        // default level, this can be changed
        initializeLogger(BrewServer.LOG);

        Level logLevel = Level.WARNING;
        String newLevel = System.getProperty("debug") != null ?
                                System.getProperty("debug"):
                                System.getenv("ELSINORE_DEBUG");
        if ("INFO".equalsIgnoreCase(newLevel)) {
            logLevel = Level.INFO;
        }

        BrewServer.LOG.info("Launching on port " + port);
        BrewServer.LOG.info("Enabled logging at level:" + logLevel.toString());
        BrewServer.LOG.setLevel(logLevel);

        this.rootDir = new File(BrewServer.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParentFile();

        if (System.getProperty("root_override") != null) {
            this.rootDir = new File(System.getProperty("root_override"));
            LOG.info("Overriding Root Directory from System Property: "
                    + rootDir.getAbsolutePath());
        }

        LOG.info("Root Directory is: " + rootDir.toString());

        if (rootDir.exists() && rootDir.isDirectory()) {
            LOG.info("Root directory: " + rootDir.toString());
        }
    }

    /**
     * Initialize the logger.  Look at the current logger and its parents to see
     * if it already has a handler setup.  If not, it adds one.
     *
     * @param logger
     *            The logger to initialize
     */
    private void initializeLogger(final Logger logger) {
        if (logger.getHandlers().length == 0) {
            if (logger.getParent() != null
                    && logger.getUseParentHandlers()) {
                initializeLogger(LOG.getParent());
            } else {
                Handler newHandler = new ConsoleHandler();
                logger.addHandler(newHandler);
            }
        }
    }

    /**
     * The main method that checks the data coming into the server.
     *
     * @param uri
     *            The URI requested
     * @param method
     *            The type of the request (GET/POST/DELETE)
     * @param header
     *            The header map from the request
     * @param parms
     *            The incoming Parameter map.
     * @param files
     *            A map of incoming files.
     * @return A NanoHTTPD Response Object
     */
    public final Response serve(final String uri, final Method method,
            final Map<String, String> header, final Map<String, String> parms,
            final Map<String, String> files) {

        BrewServer.LOG.info("URL : " + uri + " method: " + method);

        if (uri.equalsIgnoreCase("/clearStatus")) {
            LaunchControl.setMessage("");
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "Status Cleared");
        }

        if (uri.equalsIgnoreCase("/addsystem")) {
            LaunchControl.addSystemTemp();
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "Added system temperature");
        }

        if (uri.equalsIgnoreCase("/delsystem")) {
            LaunchControl.delSystemTemp();
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "Deleted system temperature");
        }

        if (uri.equalsIgnoreCase("/getstatus")) {
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    LaunchControl.getJSONStatus());
        }

        if (uri.equalsIgnoreCase("/getsystemsettings")) {
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    LaunchControl.getSystemStatus());
        }

        if (uri.equalsIgnoreCase("/graph")) {
            return serveFile("/templates/static/graph/graph.html", header,
                    rootDir);
        }

        if (uri.equalsIgnoreCase("/checkgit")) {
            LaunchControl.checkForUpdates();
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    "{Status:'OK'}");
        }

        if (uri.equalsIgnoreCase("/restartupdate")) {
            LaunchControl.updateFromGit();
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    "{Status:'OK'}");
        }

        if (uri.equalsIgnoreCase("/settheme")) {
            String newTheme = parms.get("name");

            if (newTheme == null) {
                return new NanoHTTPD.Response(Status.BAD_REQUEST,
                        MIME_TYPES.get("json"),
                        "{Status:'No name provided'}");
            }

            String fileName = "/logos/" + newTheme + ".ico";
            if (!(new File(rootDir, fileName).exists())) {
                // It doesn't exist
                LaunchControl.setMessage("Favicon for the new theme: "
                        + newTheme + ", doesn't exist."
                        + " Please add: " + fileName + " and try again");
                return new NanoHTTPD.Response(Status.BAD_REQUEST,
                        MIME_TYPES.get("json"),
                        "{Status:'Favicon doesn\'t exist'}");
            }

            fileName = "/logos/" + newTheme + ".gif";
            if (!(new File(rootDir, fileName).exists())) {
                // It doesn't exist
                LaunchControl.setMessage("Brewry image for the new theme: "
                        + newTheme + ", doesn't exist."
                        + " Please add: " + fileName + " and try again");
                return new NanoHTTPD.Response(Status.BAD_REQUEST,
                        MIME_TYPES.get("json"),
                        "{Status:'Brewery Image doesn\'t exist'}");
            }

            LaunchControl.theme = newTheme;
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    "{Status:'OK'}");

        }

        if (uri.equalsIgnoreCase("/favicon.ico")) {
            // Has the favicon been overridden?
            // Check to see if there's a theme set.
            if (LaunchControl.theme != null
                    && !LaunchControl.theme.equals("")) {
                if (new File(rootDir,
                        "/logos/" + LaunchControl.theme + ".ico").exists()) {
                    return serveFile("/logos/" + LaunchControl.theme + ".ico",
                            header, rootDir);
                }
            }

            if (new File(rootDir, uri).exists()) {
                return serveFile(uri, header, rootDir);
            }

        }

        // NLS Support
        if (uri.startsWith("/nls/")) {
            return serveFile(uri.replace("/nls/", "/src/com/sb/elsinore/nls/"),
                header, rootDir);
        }

        if (uri.equalsIgnoreCase("/stop")) {
            System.exit(128);
        }

        for (java.lang.reflect.Method m
                : UrlEndpoints.class.getDeclaredMethods()) {
           UrlEndpoint urlMethod =
                   (UrlEndpoint) m.getAnnotation(UrlEndpoint.class);
           if (urlMethod != null) {
               if (urlMethod.url().equalsIgnoreCase(uri)) {
                   try {
                       UrlEndpoints urlEndpoints = new UrlEndpoints();
                       urlEndpoints.parameters = parms;
                       urlEndpoints.files = files;
                       urlEndpoints.header = header;
                       urlEndpoints.rootDir = rootDir;
                       return (Response) m.invoke(urlEndpoints);
                   } catch (IllegalAccessException e) {
                       LOG.warning("Couldn't access URL: " + uri);
                       e.printStackTrace();
                   } catch (InvocationTargetException o) {
                       LOG.warning("Couldn't access URL: " + uri);
                       o.printStackTrace();
                   } catch (Exception e) {
                       LOG.warning("Couldn't access URL: " + uri);
                       e.printStackTrace();
                   }
               }
           }
        }

        if (!uri.equals("") && new File(rootDir, uri).exists()) {
            return serveFile(uri, header, rootDir);
        }

        BrewServer.LOG.warning("Failed to find URI: " + uri);

        BrewServer.LOG.info("Unidentified URL: " + uri);
        JSONObject usage = new JSONObject();
        usage.put("controller", "Get the main controller page");
        usage.put("getstatus", "Get the current status as a JSON object");
        usage.put("timers", "Get the current timer status");

        usage.put("addpump", "Add a new pump");
        usage.put("addtimer", "Add a new timer");
        usage.put("addvolpoint", "Add a new volume point");

        usage.put("toggleaux", "toggle an aux output");
        usage.put("mashprofile", "Set a mash profile for the output");
        usage.put("editdevice", "Edit the settings on a device");

        usage.put("updatepid", "Update the PID Settings");
        usage.put("updateday", "Update the brewday information");
        usage.put("updatepump", "Change the pump status off/on");

        BrewServer.LOG.info("Invalid URI: " + uri);
        return new NanoHTTPD.Response(Status.NOT_FOUND, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     *
     * @param incomingUri
     *            The URI requested
     * @param header
     *            The headers coming in
     * @param homeDir
     *            The root directory.
     * @return A NanoHTTPD Response for the file
     */
    public static Response serveFile(final String incomingUri,
            final Map<String, String> header, final File homeDir) {
        Response res = null;
        String uri = incomingUri;

        // Make sure we won't die of an exception later
        if (!homeDir.isDirectory()) {
            res = new Response(Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERRROR: serveFile(): "
                            + "given homeDir is not a directory.");
        }

        if (res == null) {
            // Remove URL arguments
            uri = uri.trim().replace(File.separatorChar, '/');
            if (uri.indexOf('?') >= 0) {
                uri = uri.substring(0, uri.indexOf('?'));
            }

            // Prohibit getting out of current directory
            if (uri.startsWith("src/main") || uri.endsWith("src/main")
                    || uri.contains("../")) {
                res = new Response(Response.Status.FORBIDDEN,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "FORBIDDEN: Won't serve ../ for security reasons.");
            }
        }

        File f = new File(homeDir, uri);
        if (res == null && !f.exists()) {
            res = new Response(Response.Status.NOT_FOUND,
                    NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
        }

        // List the directory, if necessary
        if (res == null && f.isDirectory()) {
            // Browsers get confused without '/' after the
            // directory, send a redirect.
            if (!uri.endsWith("/")) {
                uri += "/";
                res = new Response(Response.Status.REDIRECT,
                        NanoHTTPD.MIME_HTML,
                        "<html><body>Redirected: <a href=\"" + uri + "\">"
                                + uri + "</a></body></html>");
                res.addHeader("Location", uri);
            }

            if (res == null) {
                // First try index.html and index.htm
                if (new File(f, "index.html").exists()) {
                    f = new File(homeDir, uri + "/index.html");
                } else if (new File(f, "index.htm").exists()) {
                    f = new File(homeDir, uri + "/index.htm");
                    // No index file, list the directory if it is readable
                } else if (f.canRead()) {
                    String[] files = f.list();
                    String msg = "<html><body><h1>Directory " + uri
                            + "</h1><br/>";

                    if (uri.length() > 1) {
                        String u = uri.substring(0, uri.length() - 1);
                        int slash = u.lastIndexOf('/');
                        if (slash >= 0 && slash < u.length()) {
                            msg += "<b><a href=\""
                                    + uri.substring(0, slash + 1)
                                    + "\">..</a></b><br/>";
                        }
                    }

                    if (files != null) {
                        for (int i = 0; i < files.length; ++i) {
                            File curFile = new File(f, files[i]);
                            boolean dir = curFile.isDirectory();
                            if (dir) {
                                msg += "<b>";
                                files[i] += "/";
                            }

                            msg += "<a href=\"" + encodeUri(uri + files[i])
                                    + "\">" + files[i] + "</a>";

                            // Show file size
                            if (curFile.isFile()) {
                                long len = curFile.length();
                                msg += " &nbsp;<font size=2>(";
                                if (len < 1024) {
                                    msg += len + " bytes";
                                } else if (len < 1024 * 1024) {
                                    msg += len / 1024 + "."
                                            + (len % 1024 / 10 % 100) + " KB";
                                } else {
                                    msg += len / (1024 * 1024) + "." + len
                                            % (1024 * 1024) / 10 % 100 + " MB";
                                }
                                msg += ")</font>";
                            }
                            msg += "<br/>";
                            if (dir) {
                                msg += "</b>";
                            }
                        }
                    }
                    msg += "</body></html>";
                    res = new Response(msg);
                } else {
                    res = new Response(Response.Status.FORBIDDEN,
                            NanoHTTPD.MIME_PLAINTEXT,
                            "FORBIDDEN: No directory listing.");
                }
            }
        }
        try {
            if (res == null) {
                // Get MIME type from file name extension, if possible
                String mime = null;
                int dot = f.getCanonicalPath().lastIndexOf('.');
                if (dot >= 0) {
                    mime = MIME_TYPES.get(f.getCanonicalPath()
                            .substring(dot + 1).toLowerCase());
                }
                if (mime == null) {
                    mime = NanoHTTPD.MIME_HTML;
                }
                // Calculate etag
                String etag = Integer.toHexString((f.getAbsolutePath()
                        + f.lastModified() + "" + f.length()).hashCode());

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
                                startFrom = Long.parseLong(range.substring(0,
                                        minus));
                                endAt = Long.parseLong(range
                                        .substring(minus + 1));
                            }
                        } catch (NumberFormatException ignored) {
                            BrewServer.LOG.info(ignored.getMessage());
                        }
                    }
                }

                // Change return code and add Content-Range header
                // when skipping is requested
                long fileLen = f.length();
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = new Response(
                                Response.Status.RANGE_NOT_SATISFIABLE,
                                NanoHTTPD.MIME_PLAINTEXT, "");
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                        res.addHeader("ETag", etag);
                    } else {
                        if (endAt < 0) {
                            endAt = fileLen - 1;
                        }
                        long newLen = endAt - startFrom + 1;
                        if (newLen < 0) {
                            newLen = 0;
                        }

                        final long dataLen = newLen;
                        FileInputStream fis = new FileInputStream(f) {
                            @Override
                            public int available() throws IOException {
                                return (int) dataLen;
                            }
                        };
                        fis.skip(startFrom);

                        res = new Response(Response.Status.PARTIAL_CONTENT,
                                mime, fis);
                        res.addHeader("Content-Length", "" + dataLen);
                        res.addHeader("Content-Range", "bytes " + startFrom
                                + "-" + endAt + "/" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                } else {
                    if (etag.equals(header.get("if-none-match"))) {
                        res = new Response(Response.Status.NOT_MODIFIED, mime,
                                "");
                    } else {
                        res = new Response(Response.Status.OK, mime,
                                new FileInputStream(f));
                        res.addHeader("Content-Length", "" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                }
            }
        } catch (IOException ioe) {
            res = new Response(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "FORBIDDEN: Reading file failed.");
        }

        res.addHeader("Accept-Ranges", "bytes");
        // Announce that the file server accepts partial content requestes
        return res;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     * @param uri
     *            The URI to be encoded
     * @return The Encoded URI
     */
    public static String encodeUri(final String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/")) {
                newUri += "/";
            } else if (tok.equals(" ")) {
                newUri += "%20";
            } else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                    BrewServer.LOG.info(ignored.getMessage());
                }
            }
        }
        return newUri;
    }
}