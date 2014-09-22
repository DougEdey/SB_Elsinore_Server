package com.sb.elsinore;

import java.io.IOException;
import java.util.Locale;

/**
 * A helper class to run the NanoHTTPD instance.
 * @author Doug Edey
 *
 */
public class ServerRunner implements Runnable {

    /**
     * The current class.
     */
    @SuppressWarnings("rawtypes")
    private Class<?> serverClass = null;
    /**
     * The port to run on.
     */
    private int port;

    /**
     * The main constructor.
     * @param inClass The class to run
     * @param inPort The port to run on
     */
    public ServerRunner(final Class<?> inClass, final int inPort) {
        this.serverClass = inClass;
        this.port = inPort;
    }

    /**
     * Run the server.
     * @param server The NanoHTTPD instance to run
     */
    public static void executeInstance(final NanoHTTPD server) {
        try {
            server.start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

        System.out.println("Language: " + Locale.getDefault().toString());
        System.out.println("Server started, kill to stop.\n");

        try {
            while (true) {
                System.in.read();
            }
        } catch (Throwable ignored) {
            System.out.println(ignored.getLocalizedMessage());
        }

        server.stop();
        System.out.println("Server stopped.\n");
    }

    /**
     * Run the Server.
     */
    @Override
    public final void run() {
         try {
            executeInstance((NanoHTTPD)
                serverClass.getDeclaredConstructor(int.class)
                .newInstance(port));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}