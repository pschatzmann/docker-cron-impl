package ch.pschatzmann.docker.jobs.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.api.LogEntry;

/**
 * Model of Docker Container which provides the access to the information about
 * the batch jobs
 * 
 * @author pschatzmann
 *
 */

public class Container {
	private static final Logger LOG = Logger.getLogger(Container.class);
	private String id;
	private String name;
	private String image;
	private String created;
	private boolean deleted = false;
	private Collection<Volume> mounts = new ArrayList<Volume>();
	private Map<String, String> attributes = new HashMap<String, String>();
	private Model model;
	private Collection<Job> jobs = new ArrayList<Job>();

	/**
	 * Constructor
	 * 
	 * @param model
	 * @param attributes
	 */

	public Container(Model model, Map<String, String> attributes) {
		this.model = model;
		this.attributes = attributes;
		this.setId(attributes.get("id"));
		this.setName(attributes.get("name"));
		this.setImage(attributes.get("image"));
		this.setCreated(attributes.get("created"));
		// LOG.info("Container " + this);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<Volume> getMounts() {
		return mounts;
	}

	public void addMount(Volume volume) {
		this.mounts.add(volume);
	}

	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Model getModel() {
		return this.model;
	}

	public boolean isBatchContainer() {
		return this.model == null ? false : this == this.model.getDefaultBatchContainer();
	}

	public boolean isLocalContainer() {
		return !this.isBatchContainer();
	}

	public void createJobs() {
		// the creation of job has been delegated to the job planner
		JobPlanner jp = new JobPlanner(this);
		jp.createJobs();

		LOG.info("Evaluating jobs for '" + this + "': " + this.getJobs());
		if (this.getJobCount() > 0) {
			LOG.info("- Attributes for '" + this + "': ");
			for (Entry<String, String> e : this.getAttributes().entrySet()) {
				LOG.info(" -- " + e.getKey() + ":" + e.getValue());
			}
		}
	}

	public int getJobCount() {
		int count = 0;
		for (Job job : this.getJobs()) {
			if (job.isValid()) {
				count++;
			}
		}
		return count;

	}

	/**
	 * Schedule all jobs
	 * 
	 */
	public void scheduleJobs() {
		// Schedule a task for each container
		for (Job job : this.getJobs()) {
			if (job.isValid()) {
				LOG.info("- Job '" + job + "' for container  '" + this + "' will be scheduled with '"
						+ job.getSchedule() + "'");
				String scheduleID = model.schedule(job.getSchedule(), job);
				job.setId(scheduleID);
			}
		}
	}

	public Collection<Job> getJobs() {
		return jobs;
	}

	public void addJob(Job job) {
		this.jobs.add(job);
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getCreated() {
		return this.created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (name != null) {
			sb.append(name);
			sb.append(" ");
			String img = this.getAttributes().get("image");
			if (img != null) {
				sb.append(" (");
				sb.append(img);
				sb.append(")");
			}
		}
		return sb.toString();
	}

	public Job findJob(String jobName, String schedule) {
		for (Job job : this.getJobs()) {
			if (job.getName().equals(jobName) && job.getSchedule().equals(schedule)) {
				return job;
			}
		}
		Job newJob = new Job(jobName, schedule, this);
		return newJob;
	}

	/**
	 * Stops and deletes the container
	 */
	public void delete() {
		this.getModel().getDockerClient().deleteContainer(this.getId());
	}

	/**
	 * Temporary worker container
	 * 
	 * @return
	 */
	public boolean isTempContainer() {
		return getName().startsWith("Temp-Container-");
	}

	/**
	 * Generate the scheduling events. One for the history and the others for the
	 * future planned events
	 * 
	 * @param from
	 * @param to
	 * @return
	 * @throws ParseException
	 */

	public Collection<ScheduleEvent> getScheduleEvents(Date from, Date to) throws ParseException {
		Collection<ScheduleEvent> result = new TreeSet();
		if (this.getJobCount() > 0) {

			Date now = new Date();
			// past before current day
			Date startOfDay = Utils.getStartOfDay();
			Date created = Utils.toDate(this.getCreated(), null);

			if (created!=null) {
				if (created.before(startOfDay)) {
					result.add(new ScheduleEvent(this.getName(), created, startOfDay, false, true));
					// current day up to now
					result.add(new ScheduleEvent(this.getName(), Utils.getStartOfDay(), now, false, false));
				} else {
					result.add(new ScheduleEvent(this.getName(), created, now, false, false));				
				}
			}

			// future
			for (Job job : this.getJobs()) {
				if (job.isValid()) {
					String schedule = job.getSchedule();
					for (Date date : this.model.getScheduler().getDates(schedule, from, to)) {
						ScheduleEvent event = new ScheduleEvent(this.getName(), date, null, true, false);
						List<String> commands = new ArrayList();
						for (JobCommandGroup grp : job.getCommandGroups()) {
							commands.addAll(grp.getCommands());
						}
						event.setCommands(commands);
						event.setSchedule(job.getSchedule());
						result.add(event);

					}
				}
			}
		}

		return result;
	}

	/**
	 * Determines the log entries for this container
	 * 
	 * @param from
	 * @param timeOutInSec
	 * @return
	 */
	public Collection<LogEntry> getLogs(Date from, int timeOutInSec) {
		return this.model.getDockerClient().getLogsForContainer(this.getId(), from, timeOutInSec);
	}

}
