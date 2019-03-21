package ch.pschatzmann.docker.jobs.executors;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.aether.resolution.ArtifactResult;

import ch.pschatzmann.docker.jobs.model.JobDestination;

/**
 * Executes a java main class with the help of a maven repository. The command
 * has the syntax: repository;maven string;java class;parameters
 * 
 * @author pschatzmann
 *
 */

public class ExecutorMaven implements IExecutor {
	private static final Logger LOG = Logger.getLogger(ExecutorMaven.class);

	@Override
	public void execute(List<String> commands, JobDestination jobDestination, String name) {		
		LOG.info("------------------ N E W   J O B ------------------");

		for (String command : commands) {
			LOG.info(name+" -> "+command);
			String sa[] = parse(command);
			try {
				execute(sa[0], sa[1], sa[2], sa[3]);
			} catch (Exception e) {
				LOG.error(e, e);
			}
		}
		LOG.info("---------------------- E N D ----------------------");

	}

	private void execute(String repository, String maven, String mainClass, String args) throws Exception {
		DependencyResolver resolver = new DependencyResolver(
				new File(System.getProperty("user.home") + "/.m2/repository"), repository.split(" "));
		DependencyResolver.ResolveResult result = resolver.resolve(maven);

		List<URL> artifactUrls = new ArrayList<URL>();
		for (ArtifactResult artRes : result.artifactResults) {
			File file = artRes.getArtifact().getFile();
			URL url = file.toURI().toURL();
			LOG.info(url);
			artifactUrls.add(url);
		}
		final URLClassLoader urlClassLoader = new URLClassLoader(artifactUrls.toArray(new URL[artifactUrls.size()]));
		Class<?> clazz = urlClassLoader.loadClass(mainClass);
		Method meth = clazz.getMethod("main", String[].class);
		String[] params = args.split(" "); // init params accordingly
		meth.invoke(null, (Object) params); // static method doesn't have an
											// instance
	}

	private String[] parse(String command) {
		return command.split(";");
	}
}
