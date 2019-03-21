package ch.pschatzmann.docker.jobs.tests;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ch.pschatzmann.docker.jobs.api.CronScheduler;
import ch.pschatzmann.docker.jobs.api.DockerAPI;
import ch.pschatzmann.docker.jobs.api.IDocker;
import ch.pschatzmann.docker.jobs.executors.ExecutorContainerShell;
import ch.pschatzmann.docker.jobs.executors.ExecutorHostShell;
import ch.pschatzmann.docker.jobs.executors.ExecutorSimulator;
import ch.pschatzmann.docker.jobs.executors.ExecutorTempContainerShell;
import ch.pschatzmann.docker.jobs.executors.IExecutor;
import ch.pschatzmann.docker.jobs.executors.ExecutorMaven;
import ch.pschatzmann.docker.jobs.model.Container;
import ch.pschatzmann.docker.jobs.model.JobDestination;
import ch.pschatzmann.docker.jobs.model.Model;

/**
 * Testing of execution of commands
 * 
 * @author pschatzmann
 *
 */

public class TestExecutor {
	@Test
	public void testExecutorHostShell() throws Exception {
		IExecutor ex = new ExecutorHostShell();
				
		IDocker docker = new DockerAPI(TestURL.URL);
		Model model = new Model(new CronScheduler(), docker, ".*");
		
		Assert.assertFalse(model.getContainers().isEmpty());

		Container c = model.getContainerByName("docker-cron");
		ex.execute(Arrays.asList("docker info", "ls"), new JobDestination(c,c),null);
	}
	
	@Test
	public void testExecutorContainerShell() throws Exception {
		IExecutor ex = new ExecutorContainerShell();
		
		IDocker docker = new DockerAPI(TestURL.URL);
		Model model = new Model(new CronScheduler(), docker, ".*");
		
		Assert.assertFalse(model.getContainers().isEmpty());

		Container c = model.getContainerByName("docker-cron");
		ex.execute(Arrays.asList("docker info", "ls"), new JobDestination(c,c),null);
	}

	@Test
	public void testExecutorSimulator() throws Exception {
		IExecutor ex = new ExecutorSimulator();
		
		IDocker docker = new DockerAPI(TestURL.URL);
		Model model = new Model(new CronScheduler(), docker, ".*");
		
		Assert.assertFalse(model.getContainers().isEmpty());
	
		Container c = model.getContainerByName("docker-cron");
		ex.execute(Arrays.asList("docker info", "ls"), new JobDestination(c,c),null);
		
	}
	
	@Test
	public void testExecutorTempContainerShell() throws Exception {
		IExecutor ex = new ExecutorTempContainerShell();
		
		IDocker docker = new DockerAPI(TestURL.URL);
		Model model = new Model(new CronScheduler(), docker, ".*");
		
		Assert.assertFalse(model.getContainers().isEmpty());

		Container c = model.getContainerByName("docker-cron");
		Container c1 = model.getContainerByName("svnserve");
		ex.execute(Arrays.asList("docker info", "ls /srv/svn", "ls /backup"), new JobDestination(c,c1),"test-docker-cron-svnserve");
	}
	
	
	@Test
	public void testMavenExecutor() throws Exception {
		IExecutor ex = new ExecutorMaven();
		
		IDocker docker = new DockerAPI(TestURL.URL);
		Model model = new Model(new CronScheduler(), docker, ".*");
		
		Assert.assertFalse(model.getContainers().isEmpty());
	
		Container c = model.getContainerByName("docker-cron");
		String command = "http://nuc.local:8081/repository/maven-public;stocks:stocks:1.2-SNAPSHOT;ch.pschatzmann.stocks.download.Test;-upschatzmann@yahoo.com -psabrina1 -fstocks.7z";
		ex.execute(Arrays.asList(command), new JobDestination(c,c),null);
		
	}

}
