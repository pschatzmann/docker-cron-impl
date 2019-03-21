package ch.pschatzmann.docker.jobs.model.scripting;

import java.util.Map;

import org.apache.log4j.Logger;

import ch.pschatzmann.docker.jobs.model.JobCommandGroup;
import ch.pschatzmann.docker.jobs.model.Utils;


/**
 * Determination logic for the scripting and templating engines
 * 
 * @author pschatzmann
 *
 */
public class EvaluationEngines {
	private static final Logger LOG = Logger.getLogger(EvaluationEngines.class);
	private String scriptingEngine;
	private boolean isScriptingAsTemplate;;
	private IProcessor scriptingProcessor;
	private IProcessor templateProcessor;
	
	/**
	 * Constructor
	 * @param jobInfo
	 * @param parameters
	 */
	public EvaluationEngines(JobCommandGroup jobInfo, Map<String,String> parameters) {
		setScriptingEngine(jobInfo.getScriptEngine());
		setScriptingAsTemplate(jobInfo.isScriptingAsTemplates());
		
		scriptingProcessor = new ScriptingProcessor(parameters, scriptingEngine);
		if (isScriptingAsTemplate) {
			templateProcessor = scriptingProcessor;
		} else {
			templateProcessor = new CurlyBracesSubstitutionProcessor(parameters);
		}
	}

	/**
	 * Determines the scripting engine from the input, the system properites or
	 * use javascript as default
	 * 
	 * @param scriptEnigine
	 * @return
	 */
	private void setScriptingEngine(String scriptingEngine) {
		String result = scriptingEngine;
		if (Utils.isEmpty(result)) {
			result = System.getProperty("ScriptEngine");
			if (Utils.isEmpty(result)) {
				result = "javascript";
			}
		}
		this.scriptingEngine = result;
	}

	/**
	 * Determines the templating engine from the input, the system properites or
	 * use t
	 * 
	 * @param scriptEnigine
	 * @return
	 */
	private void setScriptingAsTemplate(Boolean templateEngine) {
		Boolean result = templateEngine;
		if (result==null) {
			result = "true".equalsIgnoreCase(System.getProperty("ScriptingAsTemplate"));
		}
		this.isScriptingAsTemplate = result;
	}

	/**
	 * Returns the processor which will be used for scripting (evaluation of conditions)
	 * @return
	 */
	private IProcessor getScriptingProcessor() {
		return scriptingProcessor;
	}

	/**
	 * Returns the processor which will be used for templating. (evaluation of job commands)
	 * @return
	 */
	private IProcessor getTemplateEngineProcessor() {
		return templateProcessor;
	}

	/**
	 * Evaluate agaist the scripting engine
	 * @param condition
	 * @return
	 */
	public boolean isValid(String condition)  {
		boolean result = true;
		if (!Utils.isEmpty(condition)) {
			try {
				result = "true".equalsIgnoreCase(this.getScriptingProcessor().getValue(condition));
			} catch (Exception e) {
				LOG.error("Could not evaluate the expression "+condition);
				result =false;
			}
		}
		return result;
	}

	/**
	 * Evaluate against the emplating engine
	 * @param command
	 * @return
	 * @throws Exception 
	 */
	public String templateValue(String command) throws Exception {
		return this.getTemplateEngineProcessor().getValue(command);
	}
	

}
