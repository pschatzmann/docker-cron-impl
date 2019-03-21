package ch.pschatzmann.docker.jobs.model;

/**
 *  Docker Mount information which is used to determine the backup directory
 * 
 * @author pschatzmann
 *
 */

public class Volume {
	private String name;
	private String source;
	private String destination;	
	private Boolean backup=true;

	public Volume(String name, String source, String destination, boolean rw) {
		this.name = name;
		this.source = source;
		this.destination = destination;
		this.backup = rw;
	}
	
	public Volume(String name, String source, String destination) {
		this(name,source,destination,true);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public Boolean getBackup() {
		return backup;
	}

	public void setBackup(Boolean backup) {
		this.backup = backup;
	}
	
	public String getLocalPath() {
		return source;
	}
	
	@Override
	public String toString() {
		return getLocalPath();
	}
	
}
