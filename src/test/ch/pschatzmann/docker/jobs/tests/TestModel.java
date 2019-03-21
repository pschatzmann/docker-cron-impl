package ch.pschatzmann.docker.jobs.tests;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.pschatzmann.docker.jobs.api.CronScheduler;
import ch.pschatzmann.docker.jobs.api.DockerAPI;
import ch.pschatzmann.docker.jobs.api.IDocker;
import ch.pschatzmann.docker.jobs.executors.ExecutorSimulator;
import ch.pschatzmann.docker.jobs.model.Container;
import ch.pschatzmann.docker.jobs.model.Model;
import ch.pschatzmann.docker.jobs.model.ScheduleEvent;
import ch.pschatzmann.docker.jobs.model.Utils;
import ch.pschatzmann.docker.jobs.model.Volume;

/**
 * Test to read containers via the Docker API and populate the model Test cases
 * for the scheduling
 * 
 * @author pschatzmann
 *
 */
public class TestModel {
	private static final Logger LOG = Logger.getLogger(TestModel.class);
	private IDocker docker;

	@Before
	public void setup() {
		docker = new DockerAPI(TestURL.URL); //"tcp://nuc.local:2375");
	}

	@Test
	public void testDockerAPI() throws Exception {
		Model model = new Model(new CronScheduler(), docker, ".*");
		model.start();
		Assert.assertEquals(docker, model.getDockerClient());
		Assert.assertFalse(model.getContainers().isEmpty());

		Collection<Volume> volumes = new ArrayList<Volume>();
		for (Container c : model.getContainers()) {
			Assert.assertFalse(c.getAttributes().isEmpty());
			Assert.assertFalse(Utils.isEmpty(c.getId()));
			Assert.assertFalse(Utils.isEmpty(c.getName()));
			volumes.addAll(c.getMounts());
		}
		Assert.assertFalse(volumes.isEmpty());
		model.stopScheduler();

	}

	/**
	 * Scheduling in one local container
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLocalJobEvaluatorMinimum() throws Exception {
		Model model = new Model(new CronScheduler());
		Map<String, String> attributes3 = new HashMap<String, String>();
		attributes3.put("id", "test");
		attributes3.put("name", "container with schedule");
		attributes3.put("job.test.executor", "Simulator");
		attributes3.put("job.test.condition", "true");
		attributes3.put("job.test.schedule", "* * * * *");
		attributes3.put("job.test.command.1", "'echo '+volumes");
		attributes3.put("job.test.condition.1", "true");
		Container container = new Container(model, attributes3);
		container.addMount(new Volume("v1-name", "v1-source", "v1-destination"));
		container.addMount(new Volume("v2-name", "v2-source", "v2-destination"));
		model.addContainer(container);
		Assert.assertEquals(1, model.getCountOfScheduledJobs());


	}



	/**
	 * Schedulung of local job on system level
	 * 
	 * @throws Exception
	 */

	@Test
	public void testLocalJobEvaluatorMinimumLevelContainer() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		Model model = new Model(new CronScheduler());
		Map<String, String> attributes3 = new HashMap<String, String>();
		attributes3.put("id", "test");
		attributes3.put("name", "container with schedule");
		attributes3.put("job.test.executor", "Simulator");
		attributes3.put("job.test.condition", "true");
		attributes3.put("job.test.schedule", "* * * * *");
		attributes3.put("job.test.command.1", "'echo test'");
		attributes3.put("job.test.condition.1", "true");
		Container container = new Container(model, attributes3);
		container.addMount(new Volume("v1-name", "v1-source", "v1-destination"));
		container.addMount(new Volume("v2-name", "v2-source", "v2-destination"));
		model.addContainer(container);

		Assert.assertEquals(1, model.getCountOfScheduledJobs());


	}

	/**
	 * Scheduling of local job on system level
	 * 
	 * @throws Exception
	 */

	@Test
	public void testLocalJobEvaluatorWithIrrelevantContainers() throws Exception {
		Model model = new Model(new CronScheduler());
		// setup batch container
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("id", "1");
		attributes.put("name", "container1");
		Container batchContainer = new Container(model, attributes);
		model.addContainer(batchContainer);
		model.setBatchContainer(batchContainer);

		// setup additional containsrs
		for (int j = 1; j < 10; j++) {
			Map<String, String> attributes2 = new HashMap<String, String>();
			attributes2.put("id", "" + j);
			attributes2.put("name", "container" + j);
			model.addContainer(new Container(model, attributes2));
		}

		Map<String, String> attributes3 = new HashMap<String, String>();
		attributes3.put("id", "test");
		attributes3.put("name", "container with schedule");
		attributes3.put("job.test.executor", "Simulator");
		attributes3.put("job.test.condition", "true");
		attributes3.put("job.test.schedule", "* * * * *");
		attributes3.put("job.test.command.1", "'echo test'");
		attributes3.put("job.test.condition.1", "true");
		model.addContainer(new Container(model, attributes3));

		Assert.assertEquals(1, model.getCountOfScheduledJobs());



	}

	@Test
	public void testBatchJobEvaluatorWithSystemContainer() throws Exception {
		Model model = new Model(new CronScheduler());

		// central batch
		Map<String, String> attributes3 = new HashMap<String, String>();
		attributes3.put("id", "test");
		attributes3.put("name", "container with schedule");
		attributes3.put("job.test.scenario", "Central");
		attributes3.put("job.test.condition", "true");
		attributes3.put("job.test.schedule", "* * * * *");
		attributes3.put("job.test.command.1", "'echo test'");
		attributes3.put("job.test.condition.1", "true");
		Container c = new Container(model, attributes3);
		model.setBatchContainer(c);
		c.addMount(new Volume("v1-name", "v1-source", "v1-destination"));
		c.addMount(new Volume("v2-name", "v2-source", "v2-destination"));

		model.addContainer(c);

		// setup 10 basic containsrs
		for (int j = 1; j <= 10; j++) {
			Map<String, String> attributes2 = new HashMap<String, String>();
			attributes2.put("id", "" + j);
			attributes2.put("name", "container" + j);
			model.addContainer(new Container(model, attributes2));
		}

		Assert.assertEquals(1, model.getCountOfScheduledJobs());


	}

	@Test
	public void testBatchJobEvaluatorWithContainer() throws Exception {
		Model model = new Model(new CronScheduler());

		// central batch
		Map<String, String> attributes3 = new HashMap<String, String>();
		attributes3.put("id", "test");
		attributes3.put("name", "container with schedule");
		attributes3.put("job.test.scenario", "Local");
		attributes3.put("job.test.executor", "Simulator");
		attributes3.put("job.test.condition", "true");
		attributes3.put("job.test.schedule", "* * * * *");
		attributes3.put("job.test.command.1", "'echo test'");
		attributes3.put("job.test.condition.1", "true");
		Container c = new Container(model, attributes3);
		model.setBatchContainer(c);
		c.addMount(new Volume("v1-name", "v1-source", "v1-destination"));
		c.addMount(new Volume("v2-name", "v2-source", "v2-destination"));

		model.addContainer(c);

		// setup 10 basic containsrs
		for (int j = 1; j <= 10; j++) {
			Map<String, String> attributes2 = new HashMap<String, String>();
			attributes2.put("id", "" + j);
			attributes2.put("name", "container" + j);
			model.addContainer(new Container(model, attributes2));
		}

		Assert.assertEquals(10, model.getCountOfScheduledJobs());


	}

	
	
	@Test
	public void testSchedule() throws Exception {
		Model model = new Model(new CronScheduler());
		// setup batch container
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("id", "1");
		attributes.put("name", "container1");
		attributes.put("job.test.executor", "Simulator");
		attributes.put("job.test.condition", "true");
		attributes.put("job.test.schedule", "0 0 * * *");
		attributes.put("job.test.command.1", "'echo test'");
		attributes.put("job.test.condition.1", "true");
		Container batchContainer = new Container(model, attributes);
		model.addContainer(batchContainer);
		model.setBatchContainer(batchContainer);

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Collection<ScheduleEvent> events = batchContainer.getScheduleEvents(new Date(), df.parse("2018-12-30"));
		LOG.info(events.size());;
		Assert.assertTrue(!events.isEmpty());


	}


}
