package ch.pschatzmann.docker.jobs.main;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.pschatzmann.docker.jobs.api.LogEntry;
import ch.pschatzmann.docker.jobs.model.Model;
import ch.pschatzmann.docker.jobs.model.ScheduleEvent;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * Serve html and /events and /logs json
 * 
 * @author pschatzmann
 *
 */
public class ServiceHttpHandler implements HttpHandler {
	private static final Logger LOG = Logger.getLogger(ServiceHttpHandler.class);
	private Model model;
	private ObjectMapper mapper = new ObjectMapper();

	public ServiceHttpHandler(Model model) {
		this.model = model;
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		if (exchange.isInIoThread()) {
			exchange.dispatch(this);
			return;
		}

		String fileName = exchange.getRelativePath();
		if (fileName.equals("/") || fileName.equals("index.html")) {
			exchange.setStatusCode(303);
			exchange.getResponseHeaders().put(Headers.LOCATION, "/calendar.html");
		} else if (fileName.equals("/events")) {
			processEvents(exchange);
		} else if (fileName.equals("/logs")) {
			processLogs(exchange);
		} else {
			String type = getContentType(fileName);
			InputStream is = ServiceHttpHandler.class.getResourceAsStream(("/gui" + fileName));
			LOG.info("/gui" + fileName + " ->" + (is != null) + " " + type);
			if (is != null) {
				byte[] bytes = IOUtils.toByteArray(is);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, type);
				exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
			} else {
				if (fileName.endsWith("html") || fileName.isEmpty()) {
					exchange.setStatusCode(303);
					exchange.getResponseHeaders().put(Headers.LOCATION, "http://index.html");
				} else {
					exchange.setStatusCode(404);
					exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
					exchange.getResponseSender().send("Page Not Found!!");
				}
			}
		}
	};

	private void processEvents(HttpServerExchange exchange) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		try {
			Map<String, Deque<String>> par = exchange.getQueryParameters();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			String start = getString(par,"start","2017-01-01");
			String end = getString(par,"end",df.format(new Date()));
			Collection<ScheduleEvent> events = model.getEvents(df.parse(start), df.parse(end));
			exchange.startBlocking();
			mapper.writer().writeValue(exchange.getOutputStream(), events);
			exchange.endExchange();
		} catch (Exception ex) {
			LOG.error(ex, ex);
			exchange.setStatusCode(400);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
			exchange.getResponseSender().send(ex.getMessage());
		}
	}
	

	private void processLogs(HttpServerExchange exchange) {
		try {
			Map<String, Deque<String>> par = exchange.getQueryParameters();
			Date startDate = getStartDate(par);
			String container = par.get("container").getLast();
			if (container.endsWith("_hist")) {
				container = container.replace("_hist", "");
			}
			Collection<LogEntry> log = model.getContainerByName(container).getLogs(startDate, 60);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
			exchange.startBlocking();
			mapper.writer().writeValue(exchange.getOutputStream(), log);
			exchange.endExchange();
		} catch (Exception ex) {
			LOG.error(ex, ex);
			exchange.setStatusCode(400);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
			exchange.getResponseSender().send(ex.getMessage());
		}
	}

	private String getString(Map<String, Deque<String>> par, String name, String defaultValue) {
		Deque<String> d = par.get(name);
		String result = defaultValue;
		if (d!=null) {
			result = d.getLast();
		}
		return result;
	}

	private Date getStartDate(Map<String, Deque<String>> par) throws ParseException {
		Date startDate = new Date(0);
		Deque<String> startDeq = par.get("start");
		if (startDeq != null) {
			String start = startDeq.getLast();
			if (start.contains("-")) {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				startDate = df.parse(start);
			} else {
				startDate.setTime(Long.parseLong(start));
			}
		}
		return startDate;
	}

	private static String getContentType(String fileName) {
		if (fileName.endsWith(".html")) {
			return "text/html";
		}
		if (fileName.endsWith(".css")) {
			return "text/css";
		}
		if (fileName.endsWith(".js")) {
			return "text/javascript";
		}
		if (fileName.endsWith(".png")) {
			return "image/png";
		}
		if (fileName.endsWith(".yaml")) {
			return "application/yaml";
		}
		return "text/html";
	}

}