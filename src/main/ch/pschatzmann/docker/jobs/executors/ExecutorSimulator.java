package ch.pschatzmann.docker.jobs.executors;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.JobDestination;

/**
 * just writes the command to the console. To be used for testing the jobs
 * 
 * @author pschatzmann
 *
 */
public class ExecutorSimulator implements IExecutor {
	private static final Logger LOG = Logger.getLogger(ExecutorSimulator.class);
	private static Long count = 0l;
	private static CountDownLatch latch;

	public void execute(List<String> commands, JobDestination jobDestination, String name) {
		for (String command : commands) {
			// Create container
			LOG.info("Simmulate --> '" + command+"' ");
			count++;
			// To be used by the testing framework
			if (latch != null) {
				latch.countDown();
			}
		}
	}

	public static Long getCount() {
		return count;
	}

	public static void clear() {
		count = 0L;
		;
	}

	public static void clear(CountDownLatch clatch) {
		count = 0L;
		;
		latch = clatch;
	}

}
