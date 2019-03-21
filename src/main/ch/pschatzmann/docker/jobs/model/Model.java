package ch.pschatzmann.docker.jobs.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.pschatzmann.docker.jobs.api.IDocker;
import ch.pschatzmann.docker.jobs.api.IScheduler;

/**
 * Access to the basic Model of our application which consists of the
 * containers, the API to docker commands and the scheduler.
 * 
 * @author pschatzmann
 *
 */

public class Model {
	private static final Logger LOG = Logger.getLogger(Model.class);
	private Map<String, Container> containers = new HashMap<String, Container>();
	private IDocker docker;
	private IScheduler scheduler;
	private Container batchContainer;

	/**
	 * Model w/o API integration. 
	 */
	public Model(IScheduler scheduler) {
		this.scheduler = scheduler;
	}


	/**
	 * Set up initial default settings to activate the basic functionality
	 * 
	 * @param scheduler
	 * @param docker
	 * @param regex
	 * @throws Exception
	 */
	public Model(IScheduler scheduler, IDocker docker, String regex) throws Exception {
		this(scheduler);
		this.docker = docker;
		readModel(regex);
	}

	/**
	 * Subscribes to changes and schedules tasks
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		subscribeEvents();
		this.getScheduler().start();
	}

	/**
	 * Reads the Images from docker to determine what needs to be backed up
	 * 
	 * @throws DockerException
	 * @throws InterruptedException
	 */
	private void readModel(String regex) throws Exception {
		LOG.info("reading containers from docker");
		docker.loadContainers(this, regex);

	}

	/**
	 * Provides the collection of all containers
	 * @return
	 */
	public Collection<Container> getContainers() {
		Collection<Container> result = new ArrayList<Container>();
		for (Entry<String, Container> e : containers.entrySet()) {
			if (!e.getValue().isDeleted()) {
				result.add(e.getValue());
			}
		}
		return result;
	}
	
	/**
	 * Provides the requested container by the id
	 * @return
	 */
	public Container getContainer(String name) {
		return containers.get(name);
	}
	
	/**
	 * Provides the requested container by the id
	 * @return
	 */
	public Container getContainerByName(String name) {
		for (Entry<String, Container> e : containers.entrySet()) {
			if (!e.getValue().isDeleted() && name.equals(e.getValue().getName())) {
				return e.getValue();
			}
		}
		return null;
	}

	/**
	 * Delete the container from our model
	 * @param id
	 * @return
	 */
	public Container deleteContainer(String id) {
		Container c = containers.get(id);
		if (c != null) {
			c.setDeleted(true);
			containers.remove(id);
			LOG.info("The container was removed from the model "+c);
		}
		
		if (c==this.batchContainer) {
			batchContainer = null;
		}
		
		return c;
	}

	/**
	 * Add a new container to our model
	 * @param c
	 * @return
	 */
	public Container addContainer(Container c) {
		if (containers.get(c.getId()) == null) {
			this.containers.put(c.getId(), c);
			// additional attributes
			if (!c.getMounts().isEmpty()) {
			    final StringBuilder stringDestination = new StringBuilder();
			    c.getMounts().forEach(l -> stringDestination.append(l.getDestination()+" "));
			    final StringBuilder stringSource = new StringBuilder();
			    c.getMounts().forEach(l -> stringSource.append(l.getSource()+" "));
			    final StringBuilder localPath = new StringBuilder();
			    c.getMounts().forEach(l -> stringSource.append(l.getLocalPath()+" "));
				
				c.getAttributes().put("volumes.localpath", localPath.toString().trim());
				c.getAttributes().put("volumes.destination", stringDestination.toString().trim());
				c.getAttributes().put("volumes.source", stringSource.toString().trim());
				c.getAttributes().put("volumes", c.getAttributes().get("volumes.destination"));
			}
			if (!c.isTempContainer()) {			
				c.createJobs();			
				c.scheduleJobs();
			}
		}		
		return c;
	}

	/**
	 * Access to the Docker API
	 * @return
	 */
	public IDocker getDockerClient() {
		return this.docker;
	}

	/**
	 * We also execute jobs for the containers that were launched after the
	 * start of this program
	 * 
	 * @throws DockerException
	 * @throws InterruptedException
	 */
	private void subscribeEvents() throws Exception {
		docker.subscribeEvents(this);
	}



	/**
	 * Access to the Scheduler
	 * @return
	 */
	public IScheduler getScheduler() {
		return this.scheduler;
	}

	/**
	 * Returns this container which is used to execute batch jobs
	 * 
	 * @return
	 */
	public Container getDefaultBatchContainer() {
		return batchContainer;
	}

	/**
	 * Defines the this container which is used to execute batch job
	 * @param batchContainer
	 */
	public void setBatchContainer(Container batchContainer) {
		this.batchContainer = batchContainer;
	}

	/**
	 * Return the number of scheduled jobs, so that we can see if the application does anything at all
	 * @return
	 */
	public int getCountOfScheduledJobs() {
		return this.scheduler.getCountOfScheduledJobs();
	}
	

	public String schedule(String schedule, Job job) {
		String result = null;
		if (job.isValid()) {
			result = this.scheduler.schedule(schedule, job);
		}
		return result;
	}


	public void deschedule(String id) {
		this.scheduler.deschedule(id);
	}


	public void stopScheduler() {
		this.scheduler.stop();;
	}


	public Container createContainer(String image, JobDestination jobDestination, String name, String command) {
		Map<String,String> attributes = this.getDockerClient().createContainer(image, jobDestination, name, command);
		return  new Container(this, attributes);
	}


	public void deleteWorkerContainers() {
		LOG.info("deleteWorkerContainers");
		for (Container c : this.getContainers()) {
			if (c.isTempContainer()) {
				c.delete();
			}
		}
	}
	
	public  Collection<ScheduleEvent> getEvents(Date from, Date to) throws JsonProcessingException, ParseException {
		LOG.info("getEvents "+from+" to "+ to);
		long start = System.currentTimeMillis();
		Collection<ScheduleEvent> events = new TreeSet();
		for (Container c : this.getContainers()) {
			LOG.info("- "+c.getName()+" "+events.size());
			events.addAll(c.getScheduleEvents(from, to));
		}
		LOG.info("getEvents runtime in sec: "+(System.currentTimeMillis()-start)/1000);
		return events;
	}


}
