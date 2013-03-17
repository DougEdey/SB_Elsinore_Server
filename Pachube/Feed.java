package Pachube;

import java.awt.Color;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class Feed {

	/**
	 * Gateway to the pachube service
	 */
	private Pachube p;

	/**
	 * id of the feed
	 */
	private Integer id;

	/**
	 * Title of the feed
	 */
	private String title;

	/**
	 * Last time the feed was updated
	 */
	private String updated;

	/**
	 * Url of the feed
	 */
	private URL feed;

	/**
	 * Wheather the feed is live or frozen
	 */
	private Status status;

	/**
	 * A description of this feed
	 */
	private String description;

	/**
	 * Url of a Website which is related to this feed
	 */
	private URL website;

	/**
	 * Publicly available email address
	 */
	private String email;

	/**
	 * Details of the Location of the feed source
	 */
	private Location location;

	/**
	 * Collection of Data object which represent the datastreams of this feed
	 */
	private ArrayList<Data> data;

	/**
	 * Wheather the feed has been created by the PachubeFactory.
	 * This variable should not be changed by users
	 */
	private boolean Created = false;

	/**
	 * Constructor
	 * @param p Pachube object which is used to authenticate user when updating this feed
	 */
	public Feed(Pachube p) {
		super();
		this.p = p;
		this.data = new ArrayList<Data>();

	}

	/**
	 * Default constructor
	 */
	public Feed() {
		super();
		this.p = null;
		this.data = new ArrayList<Data>();
	}

	/**
	 * Creates a datastream on this feed
	 * @param d Datastream
	 * @throws PachubeException
	 */
	public void createDatastream(Data d) throws PachubeException {
		if (p != null) {
			if(this.p.createDatastream(this.id, d.toXMLWithWrapper())){
				this.data.add(d);
			}
			
		}
	}

	/**
	 * Deletes a datastream
	 * @param id id of the datastream to delete
	 */
	public void deleteDatastream(int id) {
		if (p != null) {
			this.p.deleteDatastream(this.id, id);
		}
	}

	/**
	 * Updates a Datastream
	 * @param id id of the datastream to update
	 * @param value new datastream to replace the previous datastream
	 */
	public void updateDatastream(int id, Double value) {
		if (p != null) {
			Data d = this.lookup(id);
			if (d != null) {
				d.setValue(value);
				this.p.updateDatastream(this.id, id, d.toXMLWithWrapper());
			}
		}
	}

	/**
	 * Updates a Datastream
	 * @param value new datastream to replace the previous datastream
	 */
	public void updateDatastream(Data da) {
		if (p != null) {
			if (da != null) {

				this.p.updateDatastream(this.id, lookup(da.getId()).getId(), da
						.toXMLWithWrapper());
			}
		}
	}

	/**
	 * Gets a datastream from the feed
	 * @param id id of the datastream to get
	 * @return
	 */
	public Double getDatastream(int id) {
		return lookup(id).getValue();
	}

	/**
	 * Gets the last 24 hours values from a datastream
	 * @param id id of datastream
	 * @return
	 */
	public Double[] getDatastreamHistory(int id) {
		if (p != null) {
			return this.p.getDatastreamHistory(this.id, id);
		}
		return null;
	}

	/**
	 * Gets all values submitted to datastream since its creation
	 * @param id
	 * @return
	 */
	public String[] getDatastreamArchive(int id) {
		if (p != null) {
			return this.p.getDatastreamArchive(this.id, id);
		}
		return null;
	}
	
	/**
	 * Get the url of a graph
	 * @param id id of the datastream
	 * @param width Width of the Image
	 * @param height Height of the Image
	 * @param c Color of the Line on the graph
	 * @return
	 */
	public String getGraph(int id, int width, int height, Color c){
		if(p != null){
			return  this.p.showGraph(this.id, id, width, height, c);
		}
		return null;
	}

	/**
	 * Gets a Data object from the internal collection
	 * @param id id of the Data object to get
	 * @return
	 */
	private Data lookup(int id) {
		for (int i = 0; i < this.data.size(); i++) {
			if (this.data.get(i).getId() == id) {
				return this.data.get(i);
			}
		}
		return null;
	}

	/**
	 * Submits the data to pachube
	 * @throws PachubeException
	 */
	private void update() throws PachubeException {
		if (p != null) {
			if (this.Created) {
				this.p.updateFeed(this.id, this.toXMLWithoutData());
			}
		}
	}

	/**
	 * Add a datastream to the internal data collection
	 * This method does not automatically submit data to pachube, this method is intended 
	 * for the creation of feeds.
	 * @param d
	 */
	public void addData(Data d) {
		this.data.add(d);
	}

	/**
	 * Gets the id of the Feed
	 * @return
	 */
	public int getId() {
		return id;
	}

	/**
	 * sets the id of the feed, this is not intended for users to use.
	 * When creating a new datastream to submit to pachube, Pachube will provide a Unique id.
	 * THIS METHOD SHOULD ONLY BE USED BY THE PachubeFactory.
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * sets the id of the feed, this is not intended for users to use.
	 * When creating a new datastream to submit to pachube, Pachube will provide a Unique id.
	 * THIS METHOD SHOULD ONLY BE USED BY THE PachubeFactory.
	 * @param id
	 */
	public void setId(String id) {
		try {
			this.id = Integer.parseInt(id);
		} catch (Exception e) {

		}

	}

	/**
	 * Gets the title of the feed
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title of the feed, this method will submit any changes to Pachube 
	 * @param title
	 * @throws PachubeException
	 */
	public void setTitle(String title) throws PachubeException {
		this.title = title;
		update();
	}

	/**
	 * Gets when the feed was last updated, this will be the last time the feed was update after
	 * getting the feed from pachube.
	 * @return
	 */
	public String getUpdated() {
		return updated;
	}

	/**
	 * Sets when the feed was last updated, this method is not intended for users to use.
	 * THIS METHOD IS INTENDED TO BE USED BY THE PachubeFactory.
	 * @param updated
	 */
	public void setUpdated(String updated) {
		this.updated = updated;
	}

	/**
	 * Gets the Url of the Feed.
	 * @return
	 */
	public URL getFeed() {
		return feed;
	}

	/**
	 * Sets the Url of the feed, this method is not intended for users to use.
	 * THIS METHOD IS INTENDED TO BE USED BY THE PachubeFactory.
	 * @param feed
	 */
	public void setFeed(URL feed) {
		this.feed = feed;
	}

	/**
	 * Gets the status of the feed when the feed was featched from pachube
	 * @return
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Sets the url of the feed
	 * @param status
	 * @throws PachubeException
	 */
	public void setStatus(Status status) throws PachubeException {
		this.status = status;
		update();
	}

	/**
	 * Gets the Description of the feed
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the Description of the feed
	 * @param description
	 * @throws PachubeException
	 */
	public void setDescription(String description) throws PachubeException {
		this.description = description;
		update();
	}

	/**
	 * Gets the URL of the website associated with the feed
	 * @return
	 */
	public URL getWebsite() {
		return website;
	}

	/**
	 * Sets the URL of the website associated with the feed
	 * @param website
	 * @throws PachubeException
	 */
	public void setWebsite(URL website) throws PachubeException {
		this.website = website;
		update();
	}

	/**
	 * Gets the publicly available email address of this feed
	 * @return
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the publicly available email address of thie feed.
	 * THE EMAIL ADDRESS WILL BE PUBLICLY AVAILABLE, DO NOT USE AN EMAIL ADDRESS YOU WISH TO KEEP PRIVATE
	 * @param email
	 * @throws PachubeException
	 */
	public void setEmail(String email) throws PachubeException {
		this.email = email;
		update();
	}

	/**
	 * Gets the Location details of the feed
	 * @return
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Sets the location detals of the feed
	 * @param location
	 * @throws PachubeException
	 */
	public void setLocation(Location location) throws PachubeException {
		this.location = location;
		update();
	}

	/**
	 * Get a list of all datastreams in the feed
	 * @return
	 */
	public ArrayList<Data> getData() {
		return data;
	}

	/**
	 * Sets a list of all datastream in the feed
	 * @param data
	 */
	public void setData(ArrayList<Data> data) {
		this.data = data;
	}

	/**
	 * Produces eeml of this feed without any datastream information
	 * @return String of well formed eeml
	 */
	private String toXMLWithoutData() {
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<eeml xmlns=\"http://www.eeml.org/xsd/005\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"5\" xsi:schemaLocation=\"http://www.eeml.org/xsd/005 http://www.eeml.org/xsd/005/005.xsd\">";

		ret = ret + "\n\t<environment ";
		if (this.updated != null) {
			ret = ret + "updated=\"" + this.updated + "\" ";
		}

		if (this.id != null) {
			ret = ret + "id=\"" + this.id + "\"";
		}

		ret = ret + ">\n\t<title>" + this.title + "</title>\n\t";
		ret = ret + "<feed>" + this.feed + "</feed>\n\t";
		ret = ret + "<status>" + this.status + "</status>\n\t";
		ret = ret + "<description>" + this.description + "</description>\n\t";
		ret = ret + "<website>" + this.website + "</website>\n\t";
		ret = ret + this.location.toXML() + "\n\t";

		ret = ret + "</environment>\n</eeml>";

		return ret;

	}

	/**
	 * Produces eeml of this feed with any datastream information
	 * @return String of well formed eeml
	 */
	public String toXML() {
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<eeml xmlns=\"http://www.eeml.org/xsd/005\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"5\" xsi:schemaLocation=\"http://www.eeml.org/xsd/005 http://www.eeml.org/xsd/005/005.xsd\">";

		ret = ret + "\n\t<environment ";
		if (this.updated != null) {
			ret = ret + "updated=\"" + this.updated + "\" ";
		}

		if (this.id != null) {
			ret = ret + "id=\"" + this.id + "\"";
		}

		ret = ret + ">\n\t<title>" + this.title + "</title>\n\t";
		ret = ret + "<feed>" + this.feed + "</feed>\n\t";
		ret = ret + "<status>" + this.status + "</status>\n\t";
		ret = ret + "<description>" + this.description + "</description>\n\t";
		ret = ret + "<website>" + this.website + "</website>\n\t";
		if (this.location != null) {
			ret = ret + this.location.toXML() + "\n\t";
		}

		for (int i = 0; i < this.data.size(); i++) {
			if (i == this.data.size() - 1) {
				ret = ret + this.data.get(i).toXML() + "\n";
			} else {
				ret = ret + this.data.get(i).toXML() + "\n\t";
			}
		}

		ret = ret + "</environment>\n</eeml>";

		return ret;

	}

	@Override
	public String toString() {
		return "Feed [data=" + data + ", description=" + description
				+ ", email=" + email + ", feed=" + feed + ", id=" + id
				+ ", location=" + location + ", status=" + status + ", title="
				+ title + ", updated=" + updated + ", website=" + website + "]";
	}

	public void setCreated(boolean Created) {
		if (Created) {
			this.Created = Created;
		}
	}

}
