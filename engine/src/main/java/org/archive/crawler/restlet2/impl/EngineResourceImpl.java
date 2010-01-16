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
package org.archive.crawler.restlet2.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.archive.crawler.framework.Engine;
import org.archive.crawler.restlet2.EngineInfo;
import org.archive.crawler.restlet2.EngineResource;
import org.archive.crawler.restlet2.OperationStatusResponse;
import org.archive.util.FileUtils;
import org.restlet.data.Form;
import org.restlet.data.Status;

/**
 * @author Daniel Truemper <truemped@googlemail.com>
 * 
 */
public class EngineResourceImpl extends BaseResource implements EngineResource {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.archive.crawler.restlet2.EngineResource#getInfo()
	 */
	public EngineInfo retrieveEngineInfo() {

		final EngineInfo info = new EngineInfo();
		final Engine engine = getEngine();

		info.setHeritrixVersion(engine.getHeritrixVersion());

		File jobsDir = FileUtils.tryToCanonicalize(engine.getJobsDir());
		info.setJobsDir(jobsDir.getAbsolutePath());
		info.setJobsUrl(getBaseUrl() + "jobsdir/");

		info.setHeapReport(engine.heapReportData());
		info.setAvailableActions(getActions());

		return info;
	}

	/**
	 * @return The list of available actions.
	 */
	private List<String> getActions() {
		List<String> actions = new ArrayList<String>();
		actions.add("rescan");
		actions.add("add");
		actions.add("create");
		return actions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.archive.crawler.restlet2.EngineResource#manipulateEngine()
	 */
	public OperationStatusResponse manipulateEngine() {
		OperationStatusResponse status;

		final Form form = getQuery();
		final String action = form.getFirstValue("action");

		if ("rescan".equals(action)) {

			getEngine().findJobConfigs();
			status = new OperationStatusResponse(true,
					"Successfully scanned the job directory");

		} else if ("add".equals(action)) {

			final String path = form.getFirstValue("addpath");
			if (path == null) {
				status = new OperationStatusResponse(false,
						"Cannot add null directory");
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			} else {
				final File jobFile = new File(path);
				final String jobName = jobFile.getName();
				if (!jobFile.isDirectory()) {
					status = new OperationStatusResponse(false,
							"Cannot add non-directory: " + path);
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				} else if (getEngine().getJobConfigs().containsKey(jobName)) {
					status = new OperationStatusResponse(false, "Job exists: "
							+ jobName);
					getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
				} else if (getEngine().addJobDirectory(jobFile, true)) {
					status = new OperationStatusResponse(true,
							"Added crawl job: " + path);
				} else {
					status = new OperationStatusResponse(false,
							"Could not add job");
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				}
			}

		} else if ("create".equals(action)) {

			final String path = form.getFirstValue("createpath");
			if (path == null) {
				// protect against null path
				status = new OperationStatusResponse(false,
						"Cannot create null path");
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			} else if (path.indexOf(File.separatorChar) != -1) {
				// prevent specifying sub-directories
				status = new OperationStatusResponse(false,
						"Sub-directories disallowed: " + path);
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			} else if (getEngine().getJobConfigs().containsKey(path)) {
				// protect existing jobs
				status = new OperationStatusResponse(false, "Job exists: "
						+ path);
				getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
			} else {
				// try to create new job dir
				final File newJobDir = new File(getEngine().getJobsDir(), path);
				if (newJobDir.exists()) {
					// protect existing directories
					status = new OperationStatusResponse(false,
							"Directory exists: " + path);
					getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT);
				} else {
					if (getEngine().createNewJobWithDefaults(newJobDir)) {
						status = new OperationStatusResponse(true,
								"Created new crawl job: " + path);
						getEngine().findJobConfigs();
					} else {
						status = new OperationStatusResponse(false,
								"Failed to create new job: " + path);
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					}
				}
			}

		} else {
			status = new OperationStatusResponse(false, "Unknown action");
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}

		return status;
	}
}
