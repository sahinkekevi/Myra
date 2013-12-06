package com.likya.myra.jef.jobs;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.jvnet.winp.WinProcess;

import com.likya.myra.LocaleMessages;
import com.likya.myra.commons.ValidPlatforms;
import com.likya.myra.commons.grabber.StreamGrabber;
import com.likya.myra.commons.utils.CommonDateUtils;
import com.likya.myra.commons.utils.LiveStateInfoUtils;
import com.likya.myra.jef.core.CoreFactory;
import com.likya.myra.jef.model.JobRuntimeInterface;
import com.likya.myra.jef.utils.DateUtils;
import com.likya.xsd.myra.model.xbeans.generics.JobTypeDefDocument.JobTypeDef;
import com.likya.xsd.myra.model.xbeans.jobprops.SimpleProperties;
import com.likya.xsd.myra.model.xbeans.stateinfo.LiveStateInfoDocument.LiveStateInfo;
import com.likya.xsd.myra.model.xbeans.stateinfo.StateNameDocument.StateName;
import com.likya.xsd.myra.model.xbeans.stateinfo.StatusNameDocument.StatusName;
import com.likya.xsd.myra.model.xbeans.stateinfo.SubstateNameDocument.SubstateName;
import com.likya.xsd.myra.model.xbeans.wlagen.JobAutoRetryDocument.JobAutoRetry;
import com.likyateknoloji.myraJoblist.AbstractJobType;

public class ExecuteInShell extends CommonShell {

	private static final long serialVersionUID = 1L;

	boolean isShell = true;
	
	private boolean retryFlag = true;
	private int retryCounter = 1;

	transient private WatchDogTimer watchDogTimer = null;
	
	public ExecuteInShell(AbstractJobType abstractJobType, JobRuntimeInterface jobRuntimeProperties) {
		super(abstractJobType, jobRuntimeProperties);
	}

	public void stopMyDogBarking() {
		if (watchDogTimer != null) {
			watchDogTimer.interrupt();
			watchDogTimer = null;
		}
	}

	@Override
	protected void localRun() {

		Calendar startTime = Calendar.getInstance();
		
		JobRuntimeInterface jobRuntimeInterface = getJobRuntimeProperties();
		
		SimpleProperties simpleProperties = getJobAbstractJobType();
		String jobId = simpleProperties.getId();
		
		String startLog = simpleProperties.getId() + LocaleMessages.getString("ExternalProgram.0") + CommonDateUtils.getDate(startTime.getTime());
		
		JobHelper.setJsRealTimeForStart(simpleProperties, startTime);

		CoreFactory.getLogger().info(startLog);

		while (true) {
			
			try {
				
				StringBuilder stringBufferForERROR = new StringBuilder();
				StringBuilder stringBufferForOUTPUT = new StringBuilder();

				jobRuntimeInterface.setRecentWorkDuration(jobRuntimeInterface.getWorkDuration());
				jobRuntimeInterface.setRecentWorkDurationNumeric(jobRuntimeInterface.getWorkDurationNumeric());

				startWathcDogTimer();

				ProcessBuilder processBuilder = null;
				
				String jobPath = simpleProperties.getBaseJobInfos().getJobInfos().getJobTypeDetails().getJobPath();
				String jobCommand = simpleProperties.getBaseJobInfos().getJobInfos().getJobTypeDetails().getJobCommand();
				
				jobCommand = JobHelper.removeSlashAtTheEnd(simpleProperties, jobPath, jobCommand);

				CoreFactory.getLogger().info(" >>" + " ExecuteInShell " + jobId + " Çalıştırılacak komut : " + jobCommand);
				
				if (isShell) {
					String[] cmd = ValidPlatforms.getCommand(jobCommand);
					processBuilder = new ProcessBuilder(cmd);
				} else {
					processBuilder = JobHelper.parsJobCmdArgs(jobCommand);
				}
				
				processBuilder.directory(new File(jobPath));

				Map<String, String> tempEnv = new HashMap<String, String>();

				 Map<String, String> environmentVariables = new HashMap<String, String>();
				 
				if (environmentVariables != null && environmentVariables.size() > 0) {
					tempEnv.putAll(environmentVariables);
				}

				// tempEnv.putAll(XmlBeansTransformer.entryToMap(jobProperties));

				processBuilder.environment().putAll(tempEnv);

				process = processBuilder.start();

				jobRuntimeInterface.getMessageBuffer().delete(0, jobRuntimeInterface.getMessageBuffer().capacity());
				
				initGrabbers(process, jobId, CoreFactory.getLogger(), temporaryConfig.getLogBufferSize());
//				// any error message?
//				StreamGrabber errorGobbler = new StreamGrabber(process.getErrorStream(), "ERROR", CoreFactory.getLogger(), temporaryConfig.getLogBufferSize()); //$NON-NLS-1$
//				errorGobbler.setName(jobId + ".ErrorGobbler.id." + errorGobbler.getId()); //$NON-NLS-1$
//
//				// any output?
//				StreamGrabber outputGobbler = new StreamGrabber(process.getInputStream(), "OUTPUT", CoreFactory.getLogger(), temporaryConfig.getLogBufferSize()); //$NON-NLS-1$
//				outputGobbler.setName(jobId + ".OutputGobbler.id." + outputGobbler.getId()); //$NON-NLS-1$
//
//				// kick them off
//				errorGobbler.start();
//				outputGobbler.start();

				try {

					process.waitFor();

					int processExitValue = process.exitValue();
					CoreFactory.getLogger().info(jobId + LocaleMessages.getString("ExternalProgram.6") + processExitValue); //$NON-NLS-1$

					String errStr = jobRuntimeInterface.getLogAnalyzeString();
					boolean hasErrorInLog = false;
//					
//					if (!getJobProperties().getLogFilePath().equals(ScenarioLoader.UNDEFINED_VALUE)) {
//						if (errStr != null) {
//							hasErrorInLog = FileUtils.analyzeFileForString(getJobProperties().getLogFilePath(), errStr);
//						}
//					} else if (errStr != null) {
//						CoreFactory.getLogger().error("jobFailString: \"" + errStr + "\" " + LocaleMessages.getString("ExternalProgram.1") + " !");
//					}

					if (watchDogTimer != null) {
						watchDogTimer.interrupt();
						watchDogTimer = null;
					}

					cleanUpFastEndings(errorGobbler, outputGobbler);

					stringBufferForERROR = errorGobbler.getOutputBuffer();
					stringBufferForOUTPUT = outputGobbler.getOutputBuffer();
					
					JobHelper.updateDescStr(jobRuntimeInterface.getMessageBuffer(), stringBufferForOUTPUT, stringBufferForERROR);
					
					StatusName.Enum statusName = JobHelper.searchReturnCodeInStates(simpleProperties, processExitValue, jobRuntimeInterface.getMessageBuffer());

					JobHelper.writetErrorLogFromOutputs(CoreFactory.getLogger(), this.getClass().getName(), stringBufferForOUTPUT, stringBufferForERROR);
					
					if (errStr != null && hasErrorInLog) {
						JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_FINISHED, SubstateName.INT_COMPLETED, StatusName.INT_FAILED, "Log yüzünden !");
					} else {
						JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_FINISHED, SubstateName.INT_COMPLETED, statusName.intValue(), jobRuntimeInterface.getMessageBuffer().toString());
					}

				} catch (InterruptedException e) {

					errorGobbler.interrupt();
					outputGobbler.interrupt();
					if (ValidPlatforms.getOSName() != null && ValidPlatforms.getOSName().contains(ValidPlatforms.OS_WINDOWS)) {
						try {
							// System.out.println("Killing windows process tree...");
							WinProcess winProcess = new WinProcess(process);
							winProcess.killRecursively();
							// System.out.println("Killed.");
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
					// Stop the process from running
					CoreFactory.getLogger().warn(LocaleMessages.getString("ExternalProgram.8") + jobId); //$NON-NLS-1$

					// process.waitFor() komutu thread'in interrupt statusunu temizlemedigi icin 
					// asagidaki sekilde temizliyoruz
					Thread.interrupted();

					process.destroy();
					JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_FINISHED, SubstateName.INT_COMPLETED, StatusName.INT_FAILED, e.getMessage());

				}

				errorGobbler.stopStreamGobbler();
				outputGobbler.stopStreamGobbler();
				errorGobbler = null;
				outputGobbler = null;
				watchDogTimer = null;

			} catch (Exception err) {
				if (watchDogTimer != null) {
					watchDogTimer.interrupt();
					watchDogTimer = null;
				}
				JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_FINISHED, SubstateName.INT_COMPLETED, StatusName.INT_FAILED, err.getMessage());
				err.printStackTrace();
			}

			LiveStateInfo liveStateInfo = simpleProperties.getStateInfos().getLiveStateInfos().getLiveStateInfoArray(0);
			
			if(/*if not in dependency chain kontrolü eklenecek !!!*/LiveStateInfoUtils.equalStates(liveStateInfo, StateName.FINISHED, SubstateName.COMPLETED, StatusName.SUCCESS)) {
				
				JobHelper.setWorkDurations(this, startTime);

				int jobType = simpleProperties.getBaseJobInfos().getJobInfos().getJobTypeDef().intValue();
				
				switch (jobType) {
				case JobTypeDef.INT_EVENT_BASED:
					// Not implemented yet
					break;
				case JobTypeDef.INT_TIME_BASED:
					DateUtils.iterateNextDate(simpleProperties);
					JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_PENDING, SubstateName.INT_READY, StatusName.INT_BYTIME);
					break;
				case JobTypeDef.INT_USER_BASED:
					JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_PENDING, SubstateName.INT_READY, StatusName.INT_BYUSER);
					break;

				default:
					break;
				}

				CoreFactory.getLogger().info(LocaleMessages.getString("ExternalProgram.9") + jobId + " => " + liveStateInfo.getStatusName().toString());

			} else {

				JobHelper.setWorkDurations(this, startTime);
				
				boolean stateCond = LiveStateInfoUtils.equalStates(liveStateInfo, StateName.FINISHED, SubstateName.STOPPED, StatusName.BYUSER); 
				
				if(simpleProperties.getCascadingConditions().getJobAutoRetry() == JobAutoRetry.YES && retryFlag && stateCond) {
					CoreFactory.getLogger().info(LocaleMessages.getString("ExternalProgram.11") + jobId);
					
					if(retryCounter < jobRuntimeInterface.getAutoRetryCount()) {
						retryCounter++;
						try {
							Thread.sleep(jobRuntimeInterface.getAutoRetryDelay());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						startTime = Calendar.getInstance();
						JobHelper.setJsRealTimeForStart(simpleProperties, startTime);

						JobHelper.insertNewLiveStateInfo(simpleProperties, StateName.INT_PENDING, SubstateName.INT_READY, StatusName.INT_BYTIME);

						continue;
					}
				}

				CoreFactory.getLogger().info(jobId + LocaleMessages.getString("ExternalProgram.12")); //$NON-NLS-1$
				CoreFactory.getLogger().debug(jobId + LocaleMessages.getString("ExternalProgram.13")); //$NON-NLS-1$

			}

			// restore to the value derived from sernayobilgileri file.
//			getJobProperties().setJobParamList(getJobProperties().getJobParamListPerm());

			retryFlag = false;

			break;
		}

		setMyExecuter(null);
		process = null;

	}

	public boolean isRetryFlag() {
		return retryFlag;
	}

	protected void cleanUpFastEndings(StreamGrabber errorGobbler, StreamGrabber outputGobbler) throws InterruptedException {
		if (errorGobbler.isAlive()) {
			errorGobbler.stopStreamGobbler();
			while (errorGobbler.isAlive()) {
				Thread.sleep(10);
			}
		}
		if (outputGobbler.isAlive()) {
			outputGobbler.stopStreamGobbler();
			while (outputGobbler.isAlive()) {
				Thread.sleep(10);
			}
		}
	}

	public boolean isShell() {
		return isShell;
	}

	public void setShell(boolean isShell) {
		this.isShell = isShell;
	}

}
