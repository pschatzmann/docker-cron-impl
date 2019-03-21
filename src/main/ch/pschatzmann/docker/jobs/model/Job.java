package ch.pschatzmann.docker.jobs.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.JobPlanner.Scenario;

/**
 * Instance which is executed as scheduled job. For each job we need to execute
 * at least 1 command step.
 * 
 * We record the job id so that we can cancel the job again.
 * 
 * @author pschatzmann
 *
 */
public class Job implements Runnable {
	private static final Logger LOG = Logger.getLogger(Job.class);
	private String id;
	private String name;
	private String schedule;
	private List<JobCommandGroup> commandGroups = new ArrayList<JobCommandGroup>();
	private boolean isValid = false;
	private int count = 0;
	
	public Job(String name, String schedule, Container owner) {
		this.name = name;
		this.schedule = schedule;
		owner.addJob(this);
	}

	public void run() {
		for (JobCommandGroup cg : this.commandGroups) {
			LOG.info("Job is running on container " + cg.getDestination().getExecutionContainer() + " for data of "
					+ cg.getDestination().getDataSourceContainer());
			for (String command : cg.getCommands()) {
				cg.execute(command.split(" "), cg);
			}
		}
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


	public String getSchedule() {
		return schedule;
	}

	public List<JobCommandGroup> getCommandGroups() {
		return this.commandGroups;
	}

	public JobCommandGroup findCommandGroup(Container executionContainer, Container dataSourceContainer, String scriptEngine, Boolean scriptingAsTemplates, Job job, Scenario sceanrio) {
		for (JobCommandGroup cg : this.commandGroups) {
			if (cg.getDestination().getExecutionContainer().equals(executionContainer)
				&& cg.getScenario() == sceanrio	
				&& cg.getDestination().getDataSourceContainer().equals(dataSourceContainer)) {
				return cg;
			}
		}
		count++;
		return new JobCommandGroup(new JobDestination(executionContainer, dataSourceContainer), job, job.getName()+"-"+count+"-"+dataSourceContainer.getName(), sceanrio);
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name);
		if (!this.isValid) {
			sb.append("(invalid)");
		}
		return sb.toString();
	}


}
