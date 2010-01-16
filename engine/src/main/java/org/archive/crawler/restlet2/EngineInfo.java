/**
 *  Copyright 2010 Daniel Truemper <truemped@googlemail.com>.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.archive.crawler.restlet2;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Truemper <truemped@googlemail.com>
 *
 */
public class EngineInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2485888135962287733L;

	/**
	 * This version of heritrix.
	 */
	private String heritrixVersion;
	
	/**
	 * The main jobs directory.
	 */
	private String jobsDir;
	
	/**
	 * URL for the jobs directory.
	 */
	private String jobsUrl;
	
	/**
	 * Actions that may be performed on this resource.
	 */
	private List<String> availableActions;
	
	/**
	 * Report on the Java heap.
	 */
	private Map<String, Object> heapReport;
	
	/**
	 * A list of {@link JobInfo}s.
	 */
	private List<JobInfo> jobs;

	/**
	 * @return the heritrixVersion
	 */
	public String getHeritrixVersion() {
		return heritrixVersion;
	}

	/**
	 * @param heritrixVersion the heritrixVersion to set
	 */
	public void setHeritrixVersion(String heritrixVersion) {
		this.heritrixVersion = heritrixVersion;
	}

	/**
	 * @return the jobsDir
	 */
	public String getJobsDir() {
		return jobsDir;
	}

	/**
	 * @param jobsDir the jobsDir to set
	 */
	public void setJobsDir(String jobsDir) {
		this.jobsDir = jobsDir;
	}

	/**
	 * @return the jobsUrl
	 */
	public String getJobsUrl() {
		return jobsUrl;
	}

	/**
	 * @param jobsUrl the jobsUrl to set
	 */
	public void setJobsUrl(String jobsUrl) {
		this.jobsUrl = jobsUrl;
	}

	/**
	 * @return the availableActions
	 */
	public List<String> getAvailableActions() {
		return availableActions;
	}

	/**
	 * @param availableActions the availableActions to set
	 */
	public void setAvailableActions(List<String> availableActions) {
		this.availableActions = availableActions;
	}

	/**
	 * @return the heapReport
	 */
	public Map<String, Object> getHeapReport() {
		return heapReport;
	}

	/**
	 * @param heapReport the heapReport to set
	 */
	public void setHeapReport(Map<String, Object> heapReport) {
		this.heapReport = heapReport;
	}

	/**
	 * @return the jobs
	 */
	public List<JobInfo> getJobs() {
		return jobs;
	}

	/**
	 * @param jobs the jobs to set
	 */
	public void setJobs(List<JobInfo> jobs) {
		this.jobs = jobs;
	}
	
}
