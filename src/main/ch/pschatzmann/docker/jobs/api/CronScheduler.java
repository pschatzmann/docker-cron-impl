package ch.pschatzmann.docker.jobs.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import ch.pschatzmann.docker.jobs.model.Job;
import it.sauronsoftware.cron4j.Predictor;
import it.sauronsoftware.cron4j.Scheduler;

/**
 * Access to external Scheduler api. implemented as separate class so that it can be eaily exchanged
 * 
 * @author pschatzmann
 *
 */
public class CronScheduler implements IScheduler {
	private int countOfScheduledJobs = 0;
	private Scheduler scheduler;
	
	public CronScheduler() {
		scheduler = new Scheduler();
	}
	
	
	/* (non-Javadoc)
	 * @see ch.pschatzmann.docker.jobs.api.IScheduler#getCountOfScheduledJobs()
	 */
	public int getCountOfScheduledJobs() {
		return countOfScheduledJobs;
	}
	

	/* (non-Javadoc)
	 * @see ch.pschatzmann.docker.jobs.api.IScheduler#schedule(java.lang.String, ch.pschatzmann.docker.jobs.model.Job)
	 */
	public String schedule(String schedule, Job job) {
		++countOfScheduledJobs;
		return this.scheduler.schedule(schedule, job);
	}


	/* (non-Javadoc)
	 * @see ch.pschatzmann.docker.jobs.api.IScheduler#deschedule(java.lang.String)
	 */
	public void deschedule(String id) {
		--countOfScheduledJobs;
		this.scheduler.deschedule(id);
	}


	/* (non-Javadoc)
	 * @see ch.pschatzmann.docker.jobs.api.IScheduler#stopScheduler()
	 */
	@Override
	public void stop() {
		countOfScheduledJobs = 0;
		this.scheduler.stop();;
	}


	@Override
	public void start() {
		this.scheduler.start();;
	}
	
	
	public Collection<Date> getDates(String schedule, Date from , Date until) {
		Collection<Date> result = new ArrayList();
		Predictor p = new Predictor(schedule);
		Date next = p.nextMatchingDate();
		int count=0;
		while (next.before(until) && ++count<100000) {
			if (next.after(from)) {
				result.add(next);
			}
			next = p.nextMatchingDate();
		}
		return result;
	}
	


}
