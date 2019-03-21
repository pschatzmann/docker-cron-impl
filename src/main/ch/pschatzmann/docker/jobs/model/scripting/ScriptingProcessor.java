package ch.pschatzmann.docker.jobs.model.scripting;

import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptingProcessor implements IProcessor {
	private ScriptEngine engine;
		
	public ScriptingProcessor(Map<String,String> map, String engineName) {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName(engineName);
        if (engine==null) {
        	throw new RuntimeException("The scripting engine could not be found: "+engineName);
        }
        for (Entry<String,String> e : map.entrySet()) {
            engine.put(e.getKey(),e.getValue());       	
        }
	}
	
	public String getValue(String expression) throws Exception {
		return  engine.eval(expression).toString();
	}

}
