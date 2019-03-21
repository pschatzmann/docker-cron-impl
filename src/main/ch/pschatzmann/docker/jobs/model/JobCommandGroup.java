package ch.pschatzmann.docker.jobs.model;

import java.util.ArrayList;
import java.util.List;

import ch.pschatzmann.docker.jobs.executors.ExecutorFactory;
import ch.pschatzmann.docker.jobs.executors.IExecutor;
import ch.pschatzmann.docker.jobs.model.JobPlanner.Scenario;

/**
 * A group of commands which can be processed together by the same execution logic
 * 
 * @author pschatzmann
 *
 */
public class JobCommandGroup {
	private Job job;
	private JobDestination destination;
	private Scenario scenario;
	private Boolean isScriptingAsTemplates;
	private String scriptEngine;
	private List<String> commands = new ArrayList<String>();
	private String name;
	private boolean isValid = false;
	
	public JobCommandGroup(JobDestination destination,Job job, String name, Scenario scenario) {
		this.destination = destination;
		this.job = job;
		this.name = name;
		this.scenario = scenario;
		job.getCommandGroups().add(this);
	}
	
	public List<String> getCommands() {
		return commands;
	}
	
	public void addCommands(String command) {
		this.commands.add(command);
	}

	public JobDestination getDestination() {
		return destination;
	}

	public void setDestination(JobDestination destination) {
		this.destination = destination;
	}
	public String getScriptEngine() {
		return this.scriptEngine;
	}

	public Boolean isScriptingAsTemplates() {
		return this.isScriptingAsTemplates;
	}
	
	public void setScriptingAsTemplates(Boolean isScriptingAsTemplates) {
		this.isScriptingAsTemplates = isScriptingAsTemplates;
	}

	public void setScriptEngine(String scriptEngine) {
		this.scriptEngine = scriptEngine;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public void execute(String[] split, JobCommandGroup cg) {			
		IExecutor exec = ExecutorFactory.getExecutor(cg.getDestination().isOneSource(), cg.getScenario());
		exec.execute(commands, cg.getDestination(), cg.getName());
	}

	private String getName() {
		return this.name;
	}

	public Job getJob() {
		return this.job;
	}

	public Scenario getScenario() {
		return scenario;
	}
}
