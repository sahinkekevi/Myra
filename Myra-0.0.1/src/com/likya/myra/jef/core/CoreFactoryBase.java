package com.likya.myra.jef.core;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.likya.myra.commons.utils.XMLValidations;
import com.likya.myra.jef.controller.ControllerInterface;
import com.likya.myra.jef.controller.SchedulerController;
import com.likya.myra.jef.jobs.JobImpl;
import com.likya.myra.jef.model.CoreStateInfo;
import com.likya.myra.jef.utils.JobQueueOperations;
import com.likya.xsd.myra.model.xbeans.joblist.JobListDocument;


public class CoreFactoryBase {
	
	private static final String version = "0.0.1";
	
	private int executionState = CoreStateInfo.STATE_STARTING;
	
	
	private static final Logger logger = Logger.getLogger(CoreFactoryBase.class);
	
	/**
	 * For current version it is limited to one
	 * For future releases, it may be extended
	 * according to distribution strategy
	 * @author serkan taş 
	 */
	
	protected int numOfSchedulerControllers = 1;
	
	protected HashMap<String, ControllerInterface> controllerContainer;
	
	protected JobListDocument jobListDocument;
	
	protected boolean validateFactory() throws Exception {
		if (!XMLValidations.validateWithXSDAndLog(getLogger(), jobListDocument)) {
			throw new Exception("JobList.xml is null or damaged !");
		}
		return true;
	}
	
	private void initializeFactory() {
		
		for (int counter = 0; counter < numOfSchedulerControllers; counter++) {
			HashMap<String, JobImpl> jobQueue = JobQueueOperations.transformJobQueue(jobListDocument);
			controllerContainer.put(counter + "", new SchedulerController((CoreFactoryInterface) this, jobQueue));
		}
	}
	
	protected void startControllers() {
		
		initializeFactory();

		// TODO Evaluate distribution strategy

		// if (tlosParameters.isNormalizable() && !TlosServer.isRecovered()) {
		logger.info("nomalizing !"/* LocaleMessages.getString("TlosServer.40") */); //$NON-NLS-1$
		// JobQueueOperations.normalizeJobQueueForStartup(jobQueue);
		// schedulerLogger.info(LocaleMessages.getString("TlosServer.41")); //$NON-NLS-1$
		// }

		// if (tlosParameters.isPersistent()) {
		// 	JobQueueOperations.recoverDisabledJobQueue(tlosParameters, disabledJobQueue, jobQueue);
		// }

		for (String key : controllerContainer.keySet()) {
			
			Thread controller = new Thread(controllerContainer.get(key));
			controller.setName(this.getClass().getName() + "_" + key);
			controller.start();
			
		}
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	protected HashMap<String, ControllerInterface> getControllerContainer() {
		return controllerContainer;
	}

	public static String getVersion() {
		return version;
	}

	protected int getExecutionState() {
		return executionState;
	}

	protected synchronized void setExecutionState(int executionState) {
		this.executionState = executionState;
	}

	public int getNumOfSchedulerControllers() {
		return numOfSchedulerControllers;
	}
}