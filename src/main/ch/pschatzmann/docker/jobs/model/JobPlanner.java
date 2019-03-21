package ch.pschatzmann.docker.jobs.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.scripting.EvaluationEngines;

/**
 * The job execution information is stored in Docker labels. This class provides
 * the API to interpret this information.
 * 
 * job.backup.condition= job.backup.schedule= job.backup.command.1=
 * job.backup.command.2= job.backup.level=system | container | volume
 * 
 * The we return only JobInfo records which have at a minimum 1 step and where
 * all steps can be scheduled at the same time.
 * 
 * @author pschatzmann
 *
 */ 
 
public class JobPlanner {
	private static final Logger LOG = Logger.getLogger(JobPlanner.class);
	private Collection<CommandInfo> steps = new ArrayList<CommandInfo>();
	private Container currentContainer;

	public enum Action {
		condition, schedule, command, executor, level, scriptengine, scriptingastemplates
	};

	public enum Scenario {
		 Maven, Local, Central
	};

	public JobPlanner(Container currentContainer) {
		this.currentContainer = currentContainer;
		Map<String, String> jobAttributes = getJobAttributes(currentContainer.getAttributes());
		setupCommandSteps(jobAttributes, currentContainer.isBatchContainer());
	}

	public void createJobs() {
		createLocalJobs();
		createCentralJobs();
		activate();
	}

	/**
	 * Set to active if there are any valid steps
	 */
	private void activate() {
		for (Job job : this.currentContainer.getJobs()) {
			for (JobCommandGroup cg : job.getCommandGroups()) {
				for (String command : cg.getCommands()) {
					if (!command.isEmpty()) {
						cg.setValid(true);
						job.setValid(true);
						break;
					} 
				}
			}
		}
	}

	private void createLocalJobs() {
		if (this.currentContainer.isLocalContainer()) {
			for (CommandInfo step : this.steps) {
				Job job = this.currentContainer.findJob(step.getJobName(), step.getSchedule());
				Scenario scenario = step.getScenario();
				Container executionContainer = step.isCentralExecutor() ? this.currentContainer.getModel().getDefaultBatchContainer() : this.currentContainer;
				Container dataSourceContainer = this.currentContainer;
				JobCommandGroup jcg = job.findCommandGroup(executionContainer, dataSourceContainer, step.getScriptEngine(), step.scriptingAsTemplates, job, scenario);
				addCommands(step, dataSourceContainer, jcg);
			}
		} else {

		}
	}

	private void createCentralJobs() {
		Container central = this.currentContainer.getModel().getDefaultBatchContainer();
		if (central != null) {
			if (this.currentContainer.isLocalContainer()) {
				createCentralStepsForLevel(central, Arrays.asList(Scenario.Local));				
			} else {
				createCentralStepsForLevel(central, Arrays.asList(Scenario.Central, Scenario.Maven));				
			}
		}
	}

	private void createCentralStepsForLevel(Container central, List<Scenario> levels) {
		Collection<CommandInfo> centralSteps = new JobPlanner(central).getSteps();
		for (CommandInfo step : centralSteps) {
			if (levels.contains(step.getScenario())) {
				Job job = this.currentContainer.findJob(step.getJobName(), step.getSchedule());
				Container executionContainer = step.isCentralExecutor() ? central : this.currentContainer;
				Container dataSourceContainer = this.currentContainer;
				JobCommandGroup jcg = job.findCommandGroup(executionContainer, dataSourceContainer,
						step.getScriptEngine(), step.scriptingAsTemplates, job, step.getScenario());
				addCommands(step, dataSourceContainer, jcg);
			}
		}

	}

	private void addCommands(CommandInfo step, Container dataSourceContainer, JobCommandGroup jcg) {
		Object vl[] = {null};
//		if (Arrays.asList(Scenario.volume, Scenario.containerVolumesWithTempSystem).contains(step.getScenario())) {
//			vl = dataSourceContainer.getMounts().toArray();
//		}
		for (Object v : vl) {
			Map<String,String> values = new HashMap<String, String>(dataSourceContainer.getAttributes());
			if (v != null) {
				Volume volume = (Volume)v;
				values.put("volume.local", volume.getLocalPath());
				values.put("volume", volume.getDestination());
				values.put("volume.name", volume.getName());
				values.put("volume.destination", volume.getDestination());
			}
			EvaluationEngines e = new EvaluationEngines(jcg, values);
			if (e.isValid(step.getCondition())) {
				try {
					String command = step.getCommand();
					String evaluatedCommand = e.templateValue(command);
					if (!Utils.isEmpty(evaluatedCommand)) {
						jcg.addCommands(evaluatedCommand);
					}
				} catch (Exception e1) {
					LOG.error("Could not evaluate the expression " + step.getCommand(), e1);
				}
			}
		}
	}

	private Collection<CommandInfo> getSteps() {
		return this.steps;
	}

	private Map<String, String> getJobAttributes(Map<String, String> attributes) {
		Map<String, String> result = new HashMap<String, String>();
		for (Entry<String, String> e : attributes.entrySet()) {
			String key = e.getKey().toLowerCase();
			if (key.startsWith("job.")) {
				result.put(key.toLowerCase().trim(), e.getValue());
			}
		}
		return result;
	}

	private void setupCommandSteps(Map<String, String> attributes, boolean isBatch) {
		for (String key : attributes.keySet()) {
			String keyArray[] = key.split("\\.");
			if (keyArray.length == 3 && "command".equalsIgnoreCase(keyArray[2])) {
				addCommand(keyArray[1], "", attributes, isBatch);
			} else if (keyArray.length == 4 && "command".equalsIgnoreCase(keyArray[2])) {
				addCommand(keyArray[1], keyArray[3], attributes, isBatch);
			}
		}
	}

	private void addCommand(String jobName, String stepName, Map<String, String> attr, boolean isBatch) {
		CommandInfo step = new CommandInfo();
		step.jobName = jobName;
		step.stepName = stepName;
		step.scriptEngine = getValue(attr, Arrays.asList("job.scriptengine", "job.{jobname}.scriptengine"), jobName,
				null, "javascript");
		step.scriptingAsTemplates = "true".equals(getValue(attr,
				Arrays.asList("job.scriptingAsTemplates", "job.{jobname}.scriptingAsTemplates"), jobName, null));
		step.condition = getValue(attr,
				Arrays.asList("job.condition", "job.{jobname}.condition", "job.{jobname}.condition.{stepname}"),
				jobName, stepName);
		step.scenario = getScenario(
				getValue(attr, Arrays.asList("job.level","job.scenario", "job.{jobname}.level", "job.{jobname}.scenario", "job.{jobname}.level.{stepname}", "job.{jobname}.scenario.{stepname}"),
						jobName, stepName, currentContainer.isBatchContainer() ? "Central" : "Local"));
		step.schedule = getValue(attr,
				Arrays.asList("job.schedule", "job.{jobname}.schedule", "job.{jobname}.schedule.{stepName}"), jobName,
				stepName);
		step.command = getValue(attr,
				Arrays.asList("job.command", "job.{jobname}.command", "job.{jobname}.command.{stepName}"), jobName,
				stepName);
		
		steps.add(step);

	}

	private Scenario getScenario(String value) {
		return value == null ? null : Scenario.valueOf(value);
	}

	private String getValue(Map<String, String> map, List<String> l, String jobName, String stepName) {
		return getValue(map, l, jobName, stepName, null);
	}

	private String getValue(Map<String, String> map, List<String> l, String jobName, String stepName,
			String defaultValue) {
		String result = null;
		for (String key : l) {
			key = key.toLowerCase().trim();
			key = key.replace("{jobname}", jobName);
			if (stepName != null) {
				key = key.replace("{stepname}", stepName);
			}
			String tmp = map.get(key);
			if (tmp != null) {
				result = tmp;
			}
		}
		if (result == null && !Utils.isEmpty(defaultValue)) {
			result = defaultValue;
		}

		return result;
	}

	public static class CommandInfo {
		private String jobName = "";
		private String stepName = "";
		private String schedule = "";
		private String command = "";
		private String condition = "";
		private Scenario scenario = null;
		private String scriptEngine = "";
		private Boolean scriptingAsTemplates;

		public CommandInfo() {
		}

		public String getSchedule() {
			return schedule;
		}

		public String getCommand() {
			return command;
		}

		public String getCondition() {
			return condition;

		}

		public Scenario getScenario() {
			return scenario;
		}

		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		public String getStepName() {
			return stepName;
		}

		public void setStepName(String stepName) {
			this.stepName = stepName;
		}

		public String getScriptEngine() {
			return scriptEngine;
		}

		public void setScriptEngine(String scriptEngine) {
			this.scriptEngine = scriptEngine;
		}

		public Boolean getScriptingAsTemplates() {
			return scriptingAsTemplates;
		}

		public void setScriptingAsTemplates(Boolean scriptingAsTemplates) {
			this.scriptingAsTemplates = scriptingAsTemplates;
		}

		public void setSchedule(String schedule) {
			this.schedule = schedule;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public void setLevel(Scenario level) {
			this.scenario = level;
		}

		public boolean isCentralExecutor() {
			return Arrays.asList(Scenario.Central).contains(this.scenario);
		}

		public String toString() {
			return this.jobName + "(" + stepName + ") " + "->" + command;
		}
	}

}
