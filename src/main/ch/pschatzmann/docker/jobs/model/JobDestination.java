package ch.pschatzmann.docker.jobs.model;

import ch.pschatzmann.docker.jobs.api.IDocker;

/**
 * Defines where and how the job needs to be executed
 * @author pschatzmann
 *
 */
public class JobDestination {
	private Container executionContainer;
	private Container dataSourceContainer;
	
	public JobDestination(Container executionContainer, Container dataSourceContainer) {
		this.executionContainer = executionContainer;
		this.dataSourceContainer = dataSourceContainer;
	}
	
	public Container getExecutionContainer() {
		return executionContainer;
	}
	public Container getDataSourceContainer() {
		return dataSourceContainer;
	}

	public boolean isOneSource() {
		return executionContainer.equals(dataSourceContainer);
	}
	
	public boolean isCentralExecution() {
		return executionContainer.isBatchContainer();
	}

	public IDocker getDockerClient() {
		return executionContainer.getModel().getDockerClient();
	}
	
}
