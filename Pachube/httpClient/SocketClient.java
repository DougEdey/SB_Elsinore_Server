package Pachube.httpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Http Client
 * @author Sam Wilson
 * @version 1.0
 */
public class SocketClient implements HttpClient{

	/**
	 * Address of the Server
	 */
	private String Host;

	/**
	 * Port of client should connect to
	 * Default value is 80
	 */
	private int port = 80;

	/**
	 * Constructor
	 * @param host Server Address
	 * @param port Server port
	 */
	public SocketClient(String host, int port) {
		super();
		Host = host;
		this.port = port;
	}

	/**
	 * Constructor
	 * 
	 * @param host Server Address
	 */
	public SocketClient(String host) {
		super();
		Host = host;
	}

	/**
	 * Sends a HttpRequest to the Server specified by the constructor of this object
	 * @param request HttpRequst to be forwarded to the Server
	 * @return
	 */
	public HttpResponse send(HttpRequest request) {
		try {
			Socket soc = new Socket(Host, port);

			PrintWriter out = new PrintWriter(soc.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(soc
					.getInputStream()));

			out.println(request.getHttpCommand());
			out.flush();
			HttpResponse hr = new HttpResponse();

			String str = in.readLine();
			int length = 0;
			hr.addHeaderItem("Status", str);
			String body = "";
			while ((str = in.readLine()) != null) {

				String g[] = str.split(" ");
				hr.addHeaderItem(g[0].replace(":", ""), str.substring(str.indexOf(" ")+1));
				if (str.contains("Content-Length:")) {
					hr.addHeaderItem(g[0].replace(":", ""), g[1]);
					length = Integer.parseInt(g[1]);
				}
				if (str.equals("")) {
					for (int i = 0; i < length - 1; i++) {
						body = body + (char) in.read();
					}
					hr.setBody(body);
					return hr;

				}

			}
		} catch (Exception ioe) {
			ioe.printStackTrace();
			HttpResponse errorhr = new HttpResponse();
			errorhr.addHeaderItem("Status", "HTTP/1.1 418 ");
			return errorhr;

		} 
		
		return null;
	}

	/**
	 * Getter for Host
	 * @return Server address
	 */
	public String getHost() {
		return Host;
	}

	/**
	 * Setter for Host
	 * @param host Server Address
	 */
	public void setHost(String host) {
		Host = host;
	}

	/**
	 * Getter for Port
	 * @return Server port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Setter for Port
	 * @param port Server port 
	 */
	public void setPort(int port) {
		this.port = port;
	}

}
