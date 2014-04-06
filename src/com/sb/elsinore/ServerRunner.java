package com.sb.elsinore;

import java.io.IOException;

public class ServerRunner implements Runnable {
    
    Class serverClass = null; 
    int port = 8080;

    public ServerRunner(Class serverClass, int port) {
        this.serverClass = serverClass;
        this.port = port;
    }

    public static void executeInstance(NanoHTTPD server) {
        try {
            
            server.start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

        System.out.println("Server started, kill to stop.\n");

        try {
            while (true)
                System.in.read();
        } catch (Throwable ignored) {
        }

        server.stop();
        System.out.println("Server stopped.\n");
    }

    @Override
    public void run() {
         try {
            executeInstance((NanoHTTPD) serverClass.getDeclaredConstructor(int.class).newInstance(port));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}