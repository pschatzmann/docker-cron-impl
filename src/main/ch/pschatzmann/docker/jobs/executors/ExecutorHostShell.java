package ch.pschatzmann.docker.jobs.executors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.JobDestination;
import ch.pschatzmann.docker.jobs.model.Utils;

/**
 * Executes a shell command via the Runime.exec
 * 
 * @author pschatzmann
 *
 */
public class ExecutorHostShell implements IExecutor {
	private static final Logger LOG = Logger.getLogger(ExecutorHostShell.class);

	
	public void execute(List<String> commands, JobDestination jobDestination, String name) {
		for (String command : commands) {
			StringBuffer sb = new StringBuffer();
			try {
				Process p = Runtime.getRuntime().exec(command);
				p.waitFor();
				sb.append("\n------------------ N E W   J O B ------------------");
				sb.append("\n");
				sb.append(Utils.toString(command.split(" "), " "));
				sb.append(" -> ");

				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = "";
				while ((line = reader.readLine()) != null) {
					sb.append("\n");
					sb.append(line);
				}
				reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				while ((line = reader.readLine()) != null) {
					sb.append("\n");
					sb.append(line);
				}
				sb.append("\n------------------ E N D ------------------");

				LOG.info(sb);
			} catch (Exception ex) {
				LOG.error("Could not execute command '" + command+"'", ex);
			}
		}
	}

}
