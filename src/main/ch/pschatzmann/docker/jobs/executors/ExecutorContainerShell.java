package ch.pschatzmann.docker.jobs.executors;

import java.util.List;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.JobDestination;

/**
 * Execute the command against the current container using the Docker Container
 * API
 * 
 * @author pschatzmann
 *
 */
public class ExecutorContainerShell implements IExecutor {
	private static final Logger LOG = Logger.getLogger(ExecutorContainerShell.class);

	@Override
	public void execute(List<String> commands, JobDestination jobDestination, String name) {
		
		for (String command : commands) {
			StringBuffer sb = new StringBuffer();
			sb.append("\n------------------ N E W   J O B ------------------");
			sb.append("\n");
			LOG.info(sb.toString());

			LOG.info(command + " -> executed against the container " + jobDestination.getDataSourceContainer());
			try {
				jobDestination.getDockerClient().execute(command.split(" "),
						jobDestination.getDataSourceContainer().getId());

			} catch (Exception ex) {
				LOG.error("Could not execute the command '" + command, ex);
			}
			sb = new StringBuffer();
			sb.append("\n------------------ E N D ------------------");
			LOG.info(sb);
		}
		
		
		
	}

}
