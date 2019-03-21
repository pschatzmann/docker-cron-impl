package ch.pschatzmann.docker.jobs.executors;

import java.util.List;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.Container;
import ch.pschatzmann.docker.jobs.model.JobDestination;

/**
 * Executes the command against a temporary docker container which is using the
 * volumes of the current (source) container
 * 
 * @author pschatzmann
 *
 */
public class ExecutorTempContainerShell implements IExecutor {
	private static final Logger LOG = Logger.getLogger(ExecutorTempContainerShell.class);

	public void execute(List<String> commands, JobDestination jobDestination, String name) {
		Container execContainer = jobDestination.getExecutionContainer();
		if (execContainer != null) {
			String image = execContainer.getImage();
			Container newContainer = execContainer.getModel().createContainer(image,
					jobDestination, "Temp-Container-" + name, "sh");
			try {
				for (String command : commands) {
					// Create container
					StringBuffer sb = new StringBuffer();
					sb.append("\n------------------ N E W   J O B ------------------");
					sb.append("\n");
					LOG.info(sb.toString());

					LOG.info(command + " -> executed against the container " + jobDestination.getExecutionContainer()
							+ " attaching from " + jobDestination.getDataSourceContainer());

					try {
						jobDestination.getDockerClient().execute(command.split(" "), newContainer.getId());
					} catch (Exception ex) {
						LOG.error("Could not execute command " + command + "-" + ex, ex);
					}
					sb = new StringBuffer();
					sb.append("\n------------------ E N D ------------------");
					LOG.info(sb);
				}
			} finally {
				if (newContainer != null) {
					newContainer.delete();
				}
			}
		}
	}
}
