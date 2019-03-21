package ch.pschatzmann.docker.jobs.model.scripting;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.Utils;

/**
 * Template engine with replaces all strings indicated as {parameter} with their
 * corresponding values.
 * 
 * @author pschatzmann
 *
 */
public class CurlyBracesSubstitutionProcessor implements IProcessor {
	private static final Logger LOG = Logger.getLogger(CurlyBracesSubstitutionProcessor.class);
	private Map<String, String> map = new HashMap<String, String>();
	private String start = "{";
	private String end = "}";

	public CurlyBracesSubstitutionProcessor(Map<String, String> map) {
		this.map = map;
		setup();
	}

	/**
	 * Use start and end delimiters defined in the system properties
	 */
	private void setup() {
		String temp = System.clearProperty("StartChar");
		if (!Utils.isEmpty(temp)) {
			start = temp;
		}
		temp = System.clearProperty("EndChar");
		if (!Utils.isEmpty(temp)) {
			end = temp;
		}
	}

	/**
	 * Main function
	 */
	public String getValue(String str) {
		String result = str;
		int startPos = result.indexOf(start);
		int lenStart = start.length();
		// process until we do not find any start characters any more
		while (startPos > -1) {
			int endPos = result.indexOf(end, startPos);
			String key = result.substring(startPos + lenStart, endPos);
			String newValue = map.get(key);
			if (!Utils.isEmpty(newValue)) {
				result = result.replace(expand(key), newValue);
				// the start does not need to be increased because then length
				// is decreased by 2 characters
			} else {
				// the current variable is not substituted. we log an error and
				// process the next one, therefore
				// we increase the start
				startPos++;
				LOG.error("Could not find the variable '" + key + "' available values are: "
						+ Utils.toString(map.keySet(), ", "));
			}
			startPos = result.indexOf(start, startPos);
		}
		return result;
	}

	/**
	 * Determines the string that needs to be replaced
	 * 
	 * @param value
	 * @return
	 */
	private String expand(String value) {
		StringBuffer sb = new StringBuffer();
		sb.append(start);
		sb.append(value);
		sb.append(end);
		return sb.toString();
	}

}
