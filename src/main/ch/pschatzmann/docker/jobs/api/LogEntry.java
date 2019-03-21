package ch.pschatzmann.docker.jobs.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.pschatzmann.docker.jobs.model.Utils;

public class LogEntry {
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Date date;
	private String message;
	private long line;

	LogEntry(long line, String payload) {
		try {
			this.line = line;
			this.date = Utils.toDate(payload, null);
			this.message = payload.substring(payload.indexOf(" ")+1, payload.length());
		} catch(Exception ex) {
			this.message = payload;
		}
	}
	
	public String getDate() {
		return date==null? "" : df.format(date);
	}
	
	public String getMessage() {
		return message;
	}
	public long getLine() {
		return line;
	}
}
