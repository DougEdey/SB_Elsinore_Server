package Pachube;

public class Location {
	
	private String name;
	
	private double lat;
	
	private double lon;
	
	private double elevation;
	
	private Exposure exposure;
	
	private Domain domain;
	
	private Disposition disposition;
	
	

	public Location() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void setLat(String lat){
		try{
			this.lat = Double.parseDouble(lat);
		}catch  (Exception c){
			this.lat = 0.0;
		}
	}

	
	public void setLon(String lon){
		try{
			this.lon= Double.parseDouble(lon);
		}catch  (Exception c){
			this.lon = 0.0;
		}
	}

	
	public void setElevation(String elevation) {
		try{
			this.elevation = Double.parseDouble(elevation);
		}catch  (Exception c){
			this.elevation = 0.0;
		}
	}
	
	public String toXML(){
		String ret = "";
		ret = "<location ";
		
		if(this.domain != null){
			ret = ret + "domain=\""+this.domain+"\" ";
		}
		
		if(this.exposure != null){
			ret = ret + "exposure=\""+ this.exposure + "\" ";
		}
		
		if(this.disposition != null){
			ret = ret + "disposition=\""+ this.disposition +"\" ";
		}
		ret = ret + ">\n\t\t";
		ret = ret + "<name>"+ this.name + "</name>\n\t\t";
		ret = ret + "<lat>"+ this.lat + "</lat>\n\t\t";
		ret = ret + "<lon>"+ this.lon + "</lon>\n\t\t";
		ret = ret + "<ele>"+ this.elevation + "</ele>\n\t";
		ret = ret + "</location>";
		return ret;
	}

	@Override
	public String toString() {
		return "Location [disposition=" + disposition + ", domain=" + domain
				+ ", elevation=" + elevation + ", exposure=" + exposure
				+ ", lat=" + lat + ", lon=" + lon + ", name=" + name + "]";
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the lat
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}

	/**
	 * @return the lon
	 */
	public double getLon() {
		return lon;
	}

	/**
	 * @param lon the lon to set
	 */
	public void setLon(double lon) {
		this.lon = lon;
	}

	/**
	 * @return the elevation
	 */
	public double getElevation() {
		return elevation;
	}

	/**
	 * @param elevation the elevation to set
	 */
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	/**
	 * @return the exposure
	 */
	public Exposure getExposure() {
		return exposure;
	}

	/**
	 * @param exposure the exposure to set
	 */
	public void setExposure(Exposure exposure) {
		this.exposure = exposure;
	}

	/**
	 * @return the domain
	 */
	public Domain getDomain() {
		return domain;
	}

	/**
	 * @param domain the domain to set
	 */
	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	/**
	 * @return the disposition
	 */
	public Disposition getDisposition() {
		return disposition;
	}

	/**
	 * @param disposition the disposition to set
	 */
	public void setDisposition(Disposition disposition) {
		this.disposition = disposition;
	}

}
