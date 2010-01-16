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

}
