package ch.pschatzmann.docker.jobs.api;

import java.util.Collection;
import java.util.Date;

import ch.pschatzmann.docker.jobs.model.Job;

public interface IScheduler {

	/**
	 * Return the number of scheduled jobs, so that we can see if the application does anything at all
	 * @return
	 */
	int getCountOfScheduledJobs();

	String schedule(String schedule, Job job);

	void deschedule(String id);

	void start();

	void stop();
	
	Collection<Date> getDates(String schedule, Date from , Date until);

}