/*******************************************************************************
 * Copyright 2013 Likya Teknoloji
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.likya.myra.jef.model;

import java.io.Serializable;

import com.likya.xsd.myra.model.xbeans.stateinfo.StateInfosDocument.StateInfos;
import com.likya.xsd.myra.model.xbeans.wlagen.StartTimeDocument.StartTime;
import com.likya.xsd.myra.model.xbeans.wlagen.StopTimeDocument.StopTime;

public class OutputData implements Serializable {

	private static final long serialVersionUID = -6075964245318352700L;
	
	private String jobId;
	private String handleUri;
	private String treeId;
	private String groupName;
	
	
	private StartTime startTime;
	private StopTime stopTime;
	
	private StateInfos stateInfos;

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getHandleUri() {
		return handleUri;
	}

	public void setHandleUri(String handleUri) {
		this.handleUri = handleUri;
	}

	public String getTreeId() {
		return treeId;
	}

	public void setTreeId(String treeId) {
		this.treeId = treeId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public StartTime getStartTime() {
		return startTime;
	}

	public void setStartTime(StartTime startTime) {
		this.startTime = startTime;
	}

	public StopTime getStopTime() {
		return stopTime;
	}

	public void setStopTime(StopTime stopTime) {
		this.stopTime = stopTime;
	}

	public StateInfos getStateInfos() {
		return stateInfos;
	}

	public void setStateInfos(StateInfos stateInfos) {
		this.stateInfos = stateInfos;
	}
}