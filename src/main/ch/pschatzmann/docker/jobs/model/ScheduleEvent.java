package ch.pschatzmann.docker.jobs.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

public class ScheduleEvent implements Comparable<ScheduleEvent> {
	private static final Logger LOG = Logger.getLogger(ScheduleEvent.class);
	public static DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // ("yyyy-MM-dd HH:mm:ss");
	public static DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // ("yyyy-MM-dd HH:mm:ss");
	private String title;
	private Date start;
	private Date end;
	private boolean future;
	private boolean allDay = false;
	private String id;
	private String schedule;
	private List<String> commands;

	public ScheduleEvent(String name, Date start, Date end, boolean future, boolean allDay) {
		this.title = name;
		this.start = start;
		this.end = end;
		this.future = future;
		this.allDay = allDay;
		this.id = allDay ? name + "_hist" : name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@JsonIgnore
	public Date getStartDate() {
		return start;
	}

	public String getStart() {
		return start == null ? null : iso8601.format(start);
	}

	public void setStartDate(Date start) {
		this.start = start;
	}

	@JsonIgnore
	public Date getEndDate() {
		return end;
	}

	public String getEnd() {
		return end == null ? null : iso8601.format(end);
	}

	public void setEndDate(Date end) {
		this.end = end;
	}

	public String getId() {
		return this.id;
	}

	public String getBackgroundColor() {
		return this.future ? "Yellow" : "lime";
	}

	public String getBorderColor() {
		return "gray";
	}

	public boolean isAllDay() {
		return allDay;
	}

	public void setAllDay(boolean allDay) {
		this.allDay = allDay;
	}

	public String getTooltip() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table>");
		sb.append("<tr>");
		sb.append("<td>");
		sb.append("Name:");
		sb.append("</td>");
		sb.append("<td>");
		sb.append(this.getTitle());
		sb.append("</td>");
		sb.append("</tr>");

		sb.append("<tr>");
		sb.append("<td>");
		sb.append("Start:");
		sb.append("</td>");
		sb.append("<td>");
		sb.append(this.df.format(this.getStartDate()));
		sb.append("</td>");
		sb.append("</tr>");

		if (this.getEndDate() != null) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("End:");
			sb.append("</td>");
			sb.append("<td>");
			sb.append(this.df.format(this.getEndDate()));
			sb.append("</td>");
			sb.append("</tr>");
		}

		if (this.getSchedule() != null) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("Cron Schedule:");
			sb.append("</td>");
			sb.append("<td>");
			sb.append(this.getSchedule());
			sb.append("</td>");
			sb.append("</tr>");
		}

		if (this.getCommands() != null) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append("Commands:");
			sb.append("</td>");
			sb.append("<td>");
			for (String command : this.getCommands()) {
				command = command.replaceAll("&", "&amp;");
				command = command.replaceAll("<", "&lt;");
				command = command.replaceAll(">", "&gt;");
				command = command.replaceAll("'", "&#39;");
				command = command.replaceAll("\"", "&quot;");
				sb.append(command);
				sb.append("<br/>");
			}
			sb.append("</td>");
			sb.append("</tr>");
		}

		sb.append("</table>");
		String table = sb.toString();
		return table;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}

	@Override
	public String toString() {
		return getStart();
	}

	@Override
	public int compareTo(ScheduleEvent o) {
		int result = this.getTitle().compareTo(o.getTitle());
		if (result == 0) {
			result = this.getStartDate().compareTo(o.getStartDate());
		}
		return result;
	}

}
