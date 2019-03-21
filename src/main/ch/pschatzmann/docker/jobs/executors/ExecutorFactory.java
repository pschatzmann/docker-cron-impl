package ch.pschatzmann.docker.jobs.executors;

import ch.pschatzmann.docker.jobs.model.JobPlanner.Scenario;

/**
 * Determine the IExecutor based on the scenario
 * @author pschatzmann
 *
 */

public class ExecutorFactory {
	public static IExecutor getExecutor(boolean isOneSource,  Scenario scenario) {
		IExecutor result = null;
		if (scenario == Scenario.Maven) {
			result = new ExecutorMaven();
		} else if (scenario == Scenario.Central) {
			result = new ExecutorHostShell();
		} else if (scenario == Scenario.Local) {
			result = new ExecutorContainerShell();
		} else {
			result= new ExecutorTempContainerShell();
		}
		return result;
	}
}
