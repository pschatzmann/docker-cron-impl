package ch.pschatzmann.docker.jobs.main;

import java.util.TimeZone;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.api.CronScheduler;
import ch.pschatzmann.docker.jobs.api.DockerAPI;
import ch.pschatzmann.docker.jobs.model.Model;
import ch.pschatzmann.docker.jobs.model.Utils;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

/**
 * Starts the Backup scheduler
 * 
 * @author pschatzmann
 *
 */
public class Main {
	private static final Logger LOG = Logger.getLogger(Main.class);

	/**
	 * The Program main starts here !
	 * @param args
	 * @throws Exception
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws Exception, InterruptedException {
		try {
			LOG.info("Timezone="+TimeZone.getDefault());
					
			Utils.printScriptingInfo();
			Model model = new Model(new CronScheduler(), new DockerAPI(), ".*");
			model.deleteWorkerContainers();
			model.start();
			setupShutdownHook(model);	
			LOG.info("Initialization has been completed. Nuber of scheduled jobs: "+model.getCountOfScheduledJobs());
			startService(model, 8080);
			
			sleep();
		} catch(Exception ex){
			LOG.error(ex,ex);
		}
	}

	/**
	 * We just try to stop the scheduler when java is shutting down
	 * @param model
	 */
	private static void setupShutdownHook(Model model) {
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					LOG.info("Shutting down...");
					 model.stopScheduler();
					 // delete any remining worker containers
					 model.deleteWorkerContainers();
				     mainThread.join();

				} catch (Exception e) {
					LOG.error("Could not stop scheduler via shutdown hook",e);;
				}
			}
		});
	}
	
	private static void startService(Model model, int port) {
		Undertow.builder().addHttpListener(port, "0.0.0.0")
		.setWorkerThreads(5)
		.setHandler(new ServiceHttpHandler(model))
		.build().start();
	}
	
	/**
	 * We do not return because we want to keep the Docker container running...
	 * @throws InterruptedException
	 */
	private static final void sleep() throws InterruptedException {
		while(true) {
			Thread.sleep(1000);
		}
	}

}
