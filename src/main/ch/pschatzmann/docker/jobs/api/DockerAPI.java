package ch.pschatzmann.docker.jobs.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.EventsResultCallback;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import ch.pschatzmann.docker.jobs.model.Container;
import ch.pschatzmann.docker.jobs.model.Job;
import ch.pschatzmann.docker.jobs.model.JobDestination;
import ch.pschatzmann.docker.jobs.model.Model;
import ch.pschatzmann.docker.jobs.model.Utils;
import ch.pschatzmann.docker.jobs.model.Volume;

/**
 * Access to Docker functionality. We have layered all calls to the external
 * interface so that we can exchange the API implementation easily.
 * 
 * @author pschatzmann
 *
 */

public class DockerAPI implements IDocker {
	private static final Logger LOG = Logger.getLogger(DockerAPI.class);
	private DockerClient dockerClient; // 2017-11-23T11:10:45.228007177Z"

	public DockerAPI() {
		this(null);
	}

	/**
	 * Setup the connection to Docker using the indicated host
	 */
	public DockerAPI(String host) {
		try {
			dockerClient = DockerClientBuilder.getInstance(getDockerHost(host)).build();
			dockerClient.pingCmd().exec();
		} catch (Exception ex) {
			LOG.error("Could not use " + host + ": " + ex, ex);
			Map<String, String> env = System.getenv();
			String dockerhost = env.get("DOCKER_HOST");
			boolean tls = "1".equals(env.get("DOCKER_TLS_VERIFY"));
			String cert = env.get("DOCKER_CERT_PATH");
			LOG.info("Login wiht alternative information from system environment " + dockerhost);
			DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
					.withDockerHost(dockerhost).withDockerTlsVerify(tls).withDockerCertPath(cert).build();
			dockerClient = DockerClientBuilder.getInstance(config).build();
		}
	}

	/**
	 * Determines the host from the system properties. If not defined we connect to
	 * localhost
	 * 
	 * @return
	 */
	private String getDockerHost(String host) {
		String result = host;
		if (Utils.isEmpty(result)) {
			result = System.getProperty("host");
			if (Utils.isEmpty(result)) {
				result = "unix:///var/run/docker.sock";
			}
		}
		LOG.info("Using host " + result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.pschatzmann.docker.jobs.api.IDocker#loadContainers(ch.pschatzmann.
	 * docker.jobs.model.Model, java.lang.String)
	 */
	public void loadContainers(Model model, String regexName) throws IOException {

		// load all containers in order to determine the model container first
		Collection<Container> containers = loadContainersEx(model, regexName);

		// all all containers to the model
		containers.forEach(c -> model.addContainer(c));

	}

	/**
	 * Read all containers and load the detailed information to translate them into
	 * our data model
	 * 
	 * @param model
	 * @param regexName
	 * @param hostName
	 * @return
	 * @throws UnknownHostException
	 */
	private Collection<Container> loadContainersEx(Model model, String regexName) throws UnknownHostException {
		String hostName = InetAddress.getLocalHost().getHostName();

		Collection<Container> containers = new ArrayList<Container>();
		for (com.github.dockerjava.api.model.Container c : dockerClient.listContainersCmd().withShowAll(true).exec()) {
			String name = Utils.toString(c.getNames(), ":");
			if (name.matches(regexName)) {
				// determine details
				try {
					processContainer(model, hostName, containers, c);
				} catch (Exception ex) {
					LOG.error("Could not process container" + name, ex);
				}
			}
		}
		return containers;
	}

	private void processContainer(Model model, String hostName, Collection<Container> containers,
			com.github.dockerjava.api.model.Container c) {
		InspectContainerCmd cmd = dockerClient.inspectContainerCmd(c.getId());
		InspectContainerResponse details = cmd.exec();

		// collect attributes
		Map<String, String> attributes = new HashMap<String, String>();
		// remove leading /
		String containerName = details.getName();
		containerName = containerName.startsWith("/") ? containerName.substring(1) : containerName;
		attributes.put("name", containerName);
		attributes.put("host", details.getConfig().getHostName());
		attributes.put("docker-cron-host", hostName);
		attributes.put("image", c.getImage());
		attributes.put("created", details.getCreated());

		attributes.put("id", c.getId());
		attributes.putAll(details.getConfig().getLabels());
		Container modelContainer = new Container(model, attributes);

		// record current container ID
		if (hostName.equals(details.getConfig().getHostName()) || (containerName.equals("docker-cron"))) {
			LOG.info("The batch container has been identifed for id " + c.getId());
			LOG.info("host is " + hostName + " vs " + details.getConfig().getHostName());
			model.setBatchContainer(modelContainer);
		}

		// add mount information
		for (Mount mount : details.getMounts()) {
			if (mount.getRW()) {
				modelContainer.addMount(new Volume(mount.getName(), mount.getSource(),
						mount.getDestination().toString(), mount.getRW()));
			}
		}
		containers.add(modelContainer);
	}

	/**
	 * Subscribe to docker events
	 */
	@Override
	public void subscribeEvents(Model model) {

		try {
			dockerClient.eventsCmd().withSince(String.valueOf(String.valueOf(System.currentTimeMillis() / 1000)))
					.exec(new EventsResultCallback() {
						@Override
						public void onNext(Event evt) {
							processEvent(model, evt);
						}
					});
		} catch (Exception e) {
			LOG.error(e, e);
		}
	}

	/**
	 * Process events
	 * 
	 * @param model
	 * @param evt
	 */
	private void processEvent(Model model, Event evt) {
		if (evt.getType() == EventType.CONTAINER)
			LOG.info("event " + evt);
		String status = evt.getStatus();
		if (status != null) {
			String containerID = evt.getId();
			if (status.matches("start|restart")) {
				// this will just process the new containers
				try {
					loadContainers(model, ".*");
				} catch (IOException e) {
					LOG.error("Could not subscribe to events for new containers", e);
				}
			} else if (status.matches("die")) {
				Container c = model.deleteContainer(containerID);
				if (c != null) {
					LOG.info("Stopping josbs for " + c);
					for (Job job : c.getJobs()) {
						LOG.info("The jobs for the container have been descheduled " + job);
						model.deschedule(job.getId());
					}
				}
			}
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.pschatzmann.docker.jobs.api.IDocker#execute(java.lang.String[],
	 * java.lang.String)
	 */

	public void execute(String[] cmd, String id) throws InterruptedException {
		ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(id).withAttachStdout(true)
				.withAttachStderr(true).withCmd(cmd).exec();
		dockerClient.execStartCmd(execCreateCmdResponse.getId())
				.exec(new ExecStartResultCallback(System.out, System.err)).awaitStarted().awaitCompletion();

	}

	public Map<String, String> createContainer(String image, JobDestination jd, String jobName, String command) {
		CreateContainerResponse container = null;
		container = dockerClient.createContainerCmd(image)
				.withVolumesFrom(new VolumesFrom(jd.getExecutionContainer().getId()),
						new VolumesFrom(jd.getDataSourceContainer().getId()))
				.withAttachStdin(true).withStdinOpen(true).withName(jobName).withCmd(command).exec();

		dockerClient.startContainerCmd(container.getId()).exec();

		InspectContainerCmd cmd = dockerClient.inspectContainerCmd(container.getId());
		InspectContainerResponse details = cmd.exec();
		String containerName = details.getName();
		containerName = containerName.startsWith("/") ? containerName.substring(1) : containerName;
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("name", containerName);
		attributes.put("host", details.getConfig().getHostName());
		attributes.put("id", details.getId());
		attributes.putAll(details.getConfig().getLabels());
		return attributes;

	}

	public void deleteContainer(String id) {
		for (int j = 0; j < 5; j++) {
			try {
				dockerClient.stopContainerCmd(id).exec();
			} catch (NotFoundException ex) {
				return;
			} catch (Exception ex) {
			}
			try {
				dockerClient.removeContainerCmd(id).exec();
				LOG.info("Container deleted " + id);
				return;
			} catch (NotFoundException ex) {
				return;
			} catch (Exception ex) {
				LOG.warn("Could not remove container" + ex);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public Collection<LogEntry> getLogsForContainer(String containerId, Date from, int timeOutInSec){
		List<LogEntry> result = new ArrayList();
		long max = Long.parseLong(Utils.getProperty("maxLogSize","100000"));
		LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withTimestamps(true).withSince((int) (from.getTime()/1000));
	    try {
	        logContainerCmd.exec(new LogContainerResultCallback() {	 
	        		long count=0;

	        		@Override
	            public void onNext(Frame item) {
            			count++;
	            		LogEntry log = new LogEntry(count, new String(item.getPayload()));
	            		result.add(log);
	            		if (count>max) {
	            			result.remove(0);
	            		}
	            }
	        }).awaitCompletion(timeOutInSec, TimeUnit.SECONDS);
	    } catch (InterruptedException e) {
	    }
	    
	    return result;

	}
	
			
}
