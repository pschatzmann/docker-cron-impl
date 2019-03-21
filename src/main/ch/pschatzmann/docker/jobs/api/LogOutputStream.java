package ch.pschatzmann.docker.jobs.api;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class LogOutputStream extends BufferedOutputStream {

	public LogOutputStream(OutputStream out) {
		super(out);
	}


}
