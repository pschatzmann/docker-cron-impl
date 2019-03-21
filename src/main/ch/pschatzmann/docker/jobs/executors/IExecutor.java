package ch.pschatzmann.docker.jobs.executors;

import java.util.List;

import ch.pschatzmann.docker.jobs.model.JobDestination;

public interface IExecutor {
	public void execute(List<String> commands, JobDestination jobDestination, String name);
}