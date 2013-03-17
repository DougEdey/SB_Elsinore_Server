package Pachube.httpClient;

import java.util.Hashtable;

public class HttpResponse {

	/**
	 * Body of the Response
	 */
	private String Body;

	/**
	 * Header information, there is a Item called "Status" which is the HTTP
	 * status code of the Response
	 */
	private Hashtable<String, String> header;

	/**
	 * Constructor
	 */
	public HttpResponse() {
		super();
		this.header = new Hashtable<String, String>();
	}

	/**
	 * Add an item to the Response header, this method is intended only to be
	 * used by the HttpClient.
	 * 
	 * @param Key
	 * @param Value
	 */
	public void addHeaderItem(String Key, String Value) {
		this.header.put(Key, Value);
	}

	/**
	 * Get an item from the Response header, there is an Item called "Status"
	 * which is the HTTP stutus code of the Response
	 * 
	 * @param Key
	 * @return
	 */
	public String getHeaderItem(String Key) {
		return this.header.get(Key);
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return Body;
	}

	/**
	 * @param body
	 *            the body to set
	 */
	public void setBody(String body) {
		Body = body;
	}

}
