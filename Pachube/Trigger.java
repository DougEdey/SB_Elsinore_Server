package Pachube;

import java.net.URL;

public class Trigger {

	/**
	 * id of the Trigger
	 */
	private Integer ID;

	/**
	 * Url the Trigger posts to
	 */
	private URL url;

	/**
	 * id of the environment this Trigger reports on
	 */
	private Integer env_id;

	/**
	 * if of the stream this Trigger reposts on
	 */
	private Integer stream_id;

	/**
	 * Threshold of the trigger
	 */
	private Double threshold;

	
	/**
	 * The event that the trigger reports on
	 */
	private TriggerType type;

	/**
	 * Default constructor
	 */
	public Trigger() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Constructor
	 * 
	 */
	public Trigger(URL url, int envId, int streamId, double threshold,
			TriggerType type) {
		super();
		this.url = url;
		env_id = envId;
		stream_id = streamId;
		this.threshold = threshold;
		this.type = type;
	}

	public String toString() {
		String str = "";
		str = str + "trigger[url]=" + this.url.toString() + "&";
		str = str + "trigger[trigger_type]=" + this.type + "&";
		str = str + "trigger[threshold_value]=" + this.threshold + "&";
		str = str + "trigger[environment_id]=" + this.env_id + "&";
		str = str + "trigger[stream_id]=" + this.stream_id + "&";

		return str;
	}

	/**
	 * @return the iD
	 */
	public Integer getID() {
		return ID;
	}

	/**
	 * @param iD the iD to set
	 */
	public void setID(Integer iD) {
		ID = iD;
	}

	/**
	 * @return the url
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(URL url) {
		this.url = url;
	}

	/**
	 * @return the env_id
	 */
	public Integer getEnv_id() {
		return env_id;
	}

	/**
	 * @param envId the env_id to set
	 */
	public void setEnv_id(Integer envId) {
		env_id = envId;
	}

	/**
	 * @return the stream_id
	 */
	public Integer getStream_id() {
		return stream_id;
	}

	/**
	 * @param streamId the stream_id to set
	 */
	public void setStream_id(Integer streamId) {
		stream_id = streamId;
	}

	/**
	 * @return the threshold
	 */
	public Double getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold the threshold to set
	 */
	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	/**
	 * @return the type
	 */
	public TriggerType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(TriggerType type) {
		this.type = type;
	}

	
}
