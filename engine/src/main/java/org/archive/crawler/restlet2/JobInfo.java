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
public class JobInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 90698043182984469L;

	/**
	 * This jobs short name.
	 */
	private String shortName;

	/**
	 * URL for the job.
	 */
	private String url;

	/**
	 * Is this job a profile?
	 */
	private boolean profile;

	/**
	 * How many times has the job been launched.
	 */
	private int launchCount;

	/**
	 * Last time the job has been launched.
	 */
	private long lastLaunch;

	/**
	 * Filename of the primary config.
	 */
	private String primaryConfig;

	/**
	 * URL for the primary config.
	 */
	private String primaryConfigUrl;

	/**
	 * Current state of the crawl controller.
	 */
	private String crawlControllerState;

	/**
	 * If the job is finished this field contains the exit status.
	 */
	private String crawlExitStatus;

	/**
	 * 
	 */
	private List<String> jobLogTail;

	/**
	 * 
	 */
	private String statusDescription;
	
	/**
	 * 
	 */
	private List<String> availableActions;
	
	/**
	 * URI report.
	 */
	private Map<String, Long> uriTotalsReport;

	/**
	 * Size report.
	 */
	private Map<String, Long> sizeTotalsReport;

	/**
	 * Rate report.
	 */
	private Map<String, Number> rateReport;

	/**
	 * Load report.
	 */
	private Map<String, Number> loadReport;

	/**
	 * Elapsed report.
	 */
	private Map<String, Object> elapsedReport;

	/**
	 * TOE thread report.
	 */
	private Map<String, Object> threadReport;

	/**
	 * Frontier report.
	 */
	private Map<String, Object> frontierReport;

	/**
	 * Heap report.
	 */
	private Map<String, Object> heapReport;

	/**
	 * 
	 */
	private List<String> crawlLogTail;

	/**
	 * All config files.
	 */
	private List<Map<String, String>> configFiles;

	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * @param shortName
	 *            the shortName to set
	 */
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the profile
	 */
	public boolean isProfile() {
		return profile;
	}

	/**
	 * @param profile
	 *            the profile to set
	 */
	public void setProfile(boolean profile) {
		this.profile = profile;
	}

	/**
	 * @return the launchCount
	 */
	public int getLaunchCount() {
		return launchCount;
	}

	/**
	 * @param launchCount
	 *            the launchCount to set
	 */
	public void setLaunchCount(int launchCount) {
		this.launchCount = launchCount;
	}

	/**
	 * @return the lastLaunch
	 */
	public long getLastLaunch() {
		return lastLaunch;
	}

	/**
	 * @param lastLaunch
	 *            the lastLaunch to set
	 */
	public void setLastLaunch(long lastLaunch) {
		this.lastLaunch = lastLaunch;
	}

	/**
	 * @return the primaryConfig
	 */
	public String getPrimaryConfig() {
		return primaryConfig;
	}

	/**
	 * @param primaryConfig
	 *            the primaryConfig to set
	 */
	public void setPrimaryConfig(String primaryConfig) {
		this.primaryConfig = primaryConfig;
	}

	/**
	 * @return the primaryConfigUrl
	 */
	public String getPrimaryConfigUrl() {
		return primaryConfigUrl;
	}

	/**
	 * @param primaryConfigUrl
	 *            the primaryConfigUrl to set
	 */
	public void setPrimaryConfigUrl(String primaryConfigUrl) {
		this.primaryConfigUrl = primaryConfigUrl;
	}

	/**
	 * @return the crawlControllerState
	 */
	public String getCrawlControllerState() {
		return crawlControllerState;
	}

	/**
	 * @param crawlControllerState
	 *            the crawlControllerState to set
	 */
	public void setCrawlControllerState(String crawlControllerState) {
		this.crawlControllerState = crawlControllerState;
	}

	/**
	 * @return the crawlExitStatus
	 */
	public String getCrawlExitStatus() {
		return crawlExitStatus;
	}

	/**
	 * @param crawlExitStatus
	 *            the crawlExitStatus to set
	 */
	public void setCrawlExitStatus(String crawlExitStatus) {
		this.crawlExitStatus = crawlExitStatus;
	}

	/**
	 * @return the jobLogTail
	 */
	public List<String> getJobLogTail() {
		return jobLogTail;
	}

	/**
	 * @param jobLogTail
	 *            the jobLogTail to set
	 */
	public void setJobLogTail(List<String> jobLogTail) {
		this.jobLogTail = jobLogTail;
	}

	/**
	 * @return the uriTotalsReport
	 */
	public Map<String, Long> getUriTotalsReport() {
		return uriTotalsReport;
	}

	/**
	 * @param uriTotalsReport
	 *            the uriTotalsReport to set
	 */
	public void setUriTotalsReport(Map<String, Long> uriTotalsReport) {
		this.uriTotalsReport = uriTotalsReport;
	}

	/**
	 * @return the sizeTotalsReport
	 */
	public Map<String, Long> getSizeTotalsReport() {
		return sizeTotalsReport;
	}

	/**
	 * @param sizeTotalsReport
	 *            the sizeTotalsReport to set
	 */
	public void setSizeTotalsReport(Map<String, Long> sizeTotalsReport) {
		this.sizeTotalsReport = sizeTotalsReport;
	}

	/**
	 * @return the rateReport
	 */
	public Map<String, Number> getRateReport() {
		return rateReport;
	}

	/**
	 * @param rateReport
	 *            the rateReport to set
	 */
	public void setRateReport(Map<String, Number> rateReport) {
		this.rateReport = rateReport;
	}

	/**
	 * @return the loadReport
	 */
	public Map<String, Number> getLoadReport() {
		return loadReport;
	}

	/**
	 * @param loadReport
	 *            the loadReport to set
	 */
	public void setLoadReport(Map<String, Number> loadReport) {
		this.loadReport = loadReport;
	}

	/**
	 * @return the elapsedReport
	 */
	public Map<String, Object> getElapsedReport() {
		return elapsedReport;
	}

	/**
	 * @param elapsedReport
	 *            the elapsedReport to set
	 */
	public void setElapsedReport(Map<String, Object> elapsedReport) {
		this.elapsedReport = elapsedReport;
	}

	/**
	 * @return the threadReport
	 */
	public Map<String, Object> getThreadReport() {
		return threadReport;
	}

	/**
	 * @param threadReport
	 *            the threadReport to set
	 */
	public void setThreadReport(Map<String, Object> threadReport) {
		this.threadReport = threadReport;
	}

	/**
	 * @return the frontierReport
	 */
	public Map<String, Object> getFrontierReport() {
		return frontierReport;
	}

	/**
	 * @param frontierReport
	 *            the frontierReport to set
	 */
	public void setFrontierReport(Map<String, Object> frontierReport) {
		this.frontierReport = frontierReport;
	}

	/**
	 * @return the heapReport
	 */
	public Map<String, Object> getHeapReport() {
		return heapReport;
	}

	/**
	 * @param heapReport
	 *            the heapReport to set
	 */
	public void setHeapReport(Map<String, Object> heapReport) {
		this.heapReport = heapReport;
	}

	/**
	 * @return the crawlLogTail
	 */
	public List<String> getCrawlLogTail() {
		return crawlLogTail;
	}

	/**
	 * @param crawlLogTail
	 *            the crawlLogTail to set
	 */
	public void setCrawlLogTail(List<String> crawlLogTail) {
		this.crawlLogTail = crawlLogTail;
	}

	/**
	 * @param configFiles
	 *            the configFiles to set
	 */
	public void setConfigFiles(List<Map<String, String>> configFiles) {
		this.configFiles = configFiles;
	}

	/**
	 * @return the configFiles
	 */
	public List<Map<String, String>> getConfigFiles() {
		return configFiles;
	}

	/**
	 * @param statusDescription the statusDescription to set
	 */
	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}

	/**
	 * @return the statusDescription
	 */
	public String getStatusDescription() {
		return statusDescription;
	}

	/**
	 * @param availableActions the availableActions to set
	 */
	public void setAvailableActions(List<String> availableActions) {
		this.availableActions = availableActions;
	}

	/**
	 * @return the availableActions
	 */
	public List<String> getAvailableActions() {
		return availableActions;
	}

}
