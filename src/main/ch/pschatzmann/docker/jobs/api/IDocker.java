package ch.pschatzmann.docker.jobs.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import ch.pschatzmann.docker.jobs.model.JobDestination;
import ch.pschatzmann.docker.jobs.model.Model;

public interface IDocker {

	/**
	 * We read all Docker Containers and translate them into Containers and add them to our
	 * scheduling data model
	 * 
	 * @param model
	 * @throws IOException
	 */
	void loadContainers(Model model, String regexName) throws IOException;

	/**
	 * Subscribes to Docker Events. For new containers we schedule the new jobs
	 * automatically
	 * 
	 * @param model
	 */

	void subscribeEvents(Model model);

	/**
	 * Executes a command against the indicated container id
	 * 
	 * @param cmd
	 * @param id
	 * @throws InterruptedException
	 */

	void execute(String[] cmd, String id) throws InterruptedException;


	/**
	 * Stops and deletes the container
	 * @param id
	 */
	void deleteContainer(String id);

	/**
	 * Creates a new container
	 * @param jobDestination
	 * @param name
	 * @param command
	 * @return 
	 * @return
	 */
	public Map<String, String> createContainer(String image, JobDestination jobDestination, String name, String command);

	/**
	 * Determines the log entries from the container
	 * 
	 * @param containerId
	 * @param from
	 * @param timeOutInSec
	 * @return
	 */
	public Collection<LogEntry> getLogsForContainer(String containerId, Date from, int timeOutInSec);

}
