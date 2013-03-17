package Pachube;

public class PachubeException extends Exception {
	
	/**
	 * Error message: This is a HTTP status code retrieved from a failed http request
	 */
	public String errorMessage;

	public PachubeException(String errorMessage) {
		super();
		this.errorMessage = errorMessage;
	}
	

}
