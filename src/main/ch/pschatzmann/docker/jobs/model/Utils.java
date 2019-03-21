package ch.pschatzmann.docker.jobs.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.main.Main;

/**
 * Generic utils
 * 
 * @author pschatzmann
 *
 */
public class Utils {
	private static final Logger LOG = Logger.getLogger(Utils.class);
	private static DateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	/**
	 * Convert a Collection of objects to a String which is separated with the
	 * indicated delimiter.
	 * 
	 * @param objects
	 * @param delimiter
	 * @return
	 */
	public static String toString(Collection<?> objects, String delimiter) {
		StringBuffer sb = new StringBuffer();
		for (Object name : objects) {
			if (sb.length() > 0) {
				sb.append(delimiter);
			}
			sb.append(String.valueOf(name));
		}
		return sb.toString();
	}

	/**
	 * Converts a map to a string
	 * 
	 * @param map
	 * @param delimiter
	 * @param equals
	 * @return
	 */
	public static String toString(Map<String, String> map, String delimiter, String equals) {
		StringBuffer sb = new StringBuffer();
		for (Entry<?, ?> e : map.entrySet()) {
			if (sb.length() > 0) {
				sb.append(delimiter);
			}
			sb.append(String.valueOf(e.getKey()));
			sb.append(equals);
			sb.append(String.valueOf(e.getValue()));
		}
		return sb.toString();
	}

	/**
	 * Returns true if the string is null or an empty string or a string wiht only
	 * spaces
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	/**
	 * Converts an array of objects to the string representation, separated with the
	 * indicated delimiter string.
	 * 
	 * @param objects
	 * @param delimiter
	 * @return
	 */
	public static String toString(Object[] objects, String delimiter) {
		return Utils.toString(Arrays.asList(objects), delimiter);
	}

	public static void printScriptingInfo() {
		ScriptEngineManager mgr = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = mgr.getEngineFactories();
		LOG.info("The following scripting engines are supported:");
		for (ScriptEngineFactory factory : factories) {
			String engName = factory.getEngineName();
			String engVersion = factory.getEngineVersion();
			String langName = factory.getLanguageName();
			String langVersion = factory.getLanguageVersion();

			LOG.info(String.format("Script Engine: %s (%s)", engName, engVersion));

			List<String> engNames = factory.getNames();
			for (String name : engNames) {
				LOG.info(String.format("\tEngine Alias: %s", name));
			}
			LOG.info(String.format("\tLanguage: %s (%s)", langName, langVersion));
		}
	}

	public static Date toDate(String iso, Date defaultDate) throws ParseException {
		if (iso == null)
			return defaultDate;
		String dateString = iso.substring(0, 23);
		return timeFormatter.parse(dateString);
	}

	public static Date getStartOfDay() {
		return getStartOfDay(0);
	}
	
	public static Date getStartOfDay(int offesetSec) {
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DATE);
		calendar.set(year, month, day, 0, 0, 0);
		return new Date(calendar.getTime().getTime()+(offesetSec*1000));
	}

	/**
	 * Returns the indicated environment variable or if it does not exist the
	 * system property.
	 * 
	 * @param property
	 *            property name
	 * @param defaultValue
	 *            value if no property is defined
	 * @return
	 */
	public static String getProperty(String property, String defaultValue) {
		String value = System.getenv(property);
		return value == null ? System.getProperty(property, defaultValue) : value;
	}
	
	

}
