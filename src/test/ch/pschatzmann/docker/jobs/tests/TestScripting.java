package ch.pschatzmann.docker.jobs.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.pschatzmann.docker.jobs.model.Utils;
import ch.pschatzmann.docker.jobs.model.scripting.CurlyBracesSubstitutionProcessor;
import ch.pschatzmann.docker.jobs.model.scripting.IProcessor;
import ch.pschatzmann.docker.jobs.model.scripting.ScriptingProcessor;

/**
 * Testing of el expressions
 * 
 * @author pschatzmann
 *
 */
public class TestScripting {
	
	
	@Test
	public void testScriping() throws Exception {
		Utils.printScriptingInfo();
	}
	

	@Test
	public void testJS() throws Exception {
		Map<String,String> map = new HashMap<String, String>();
		map.put("test1", "value1");
		map.put("test2", "value2");
		
		IProcessor el = new ScriptingProcessor(map, "nashorn");
		Assert.assertEquals("value1",el.getValue("test1"));
		Assert.assertEquals("value1-value2",el.getValue("test1+'-'+test2"));			
		Assert.assertEquals("true",el.getValue("2 > 1"));			
		Assert.assertEquals("false",el.getValue("2 < 1"));			
		Assert.assertEquals("3",el.getValue("2 + 1"));			
	}
	
	@Test
	public void testjexl() throws Exception {
		Map<String,String> map = new HashMap<String, String>();
		map.put("test1", "value1");
		map.put("test2", "value2");
		
		IProcessor el = new ScriptingProcessor(map,"jexl");
		Assert.assertEquals("value1",el.getValue("test1"));
		Assert.assertEquals("value1-value2",el.getValue("test1+'-'+test2"));			
		Assert.assertEquals("true",el.getValue("2 > 1"));			
		Assert.assertEquals("false",el.getValue("2 < 1"));			
		Assert.assertEquals("3",el.getValue("2 + 1"));			
	}

	@Test
	public void testGroovy() throws Exception {
		Map<String,String> map = new HashMap<String, String>();
		map.put("test1", "value1");
		map.put("test2", "value2");
		
		IProcessor el = new ScriptingProcessor(map,"groovy");
		Assert.assertEquals("value1",el.getValue("test1"));
		Assert.assertEquals("value1-value2",el.getValue("test1+'-'+test2"));			
		Assert.assertEquals("true",el.getValue("2 > 1"));			
		Assert.assertEquals("false",el.getValue("2 < 1"));			
		Assert.assertEquals("3",el.getValue("2 + 1"));			
	}
	
	
	@Test
	public void testCurlyBraces() throws Exception {
		Map<String,String> map = new HashMap<String, String>();
		map.put("test1", "value1");
		map.put("test2", "value2");
		
		IProcessor el = new CurlyBracesSubstitutionProcessor(map);
		Assert.assertEquals("value1",el.getValue("{test1}"));
		Assert.assertEquals("value1-value2",el.getValue("{test1}-{test2}"));			
		Assert.assertEquals("{test9}-value2",el.getValue("{test9}-{test2}"));			
	}
	
}
