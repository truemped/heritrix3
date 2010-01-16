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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.reporting.AlertHandler;
import org.archive.crawler.reporting.AlertThreadGroup;
import org.archive.crawler.restlet2.JobInfo;
import org.archive.crawler.restlet2.JobResource;
import org.archive.crawler.restlet2.OperationStatusResponse;
import org.archive.util.FileUtils;
import org.archive.util.TextUtils;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author Daniel Truemper <truemped@googlemail.com>
 * 
 */
public class JobResourceImpl extends BaseResource implements JobResource {

	/**
	 * Get a usable HrefPath, relative to the JobResource, for the given file.
	 * Assumes usual helper resources ('jobdir/', 'anypath/') at the usual
	 * locations.
	 * 
	 * @param f
	 *            File to provide an href (suitable for clicking or redirection)
	 * @param cj
	 *            CrawlJob for calculating jobdir-relative path if possible
	 * @return String path suitable as href or Location header
	 */
	public static String getHrefPath(File f, CrawlJob cj) {
		String jobDirRelative = cj.jobDirRelativePath(f);
		if (jobDirRelative != null) {
			return "jobdir/" + jobDirRelative;
		}
		// TODO: delegate this to EngineApplication, or make
		// conditional on whether /anypath/ service is present?
		String fullPath = f.getAbsolutePath();
		fullPath = fullPath.replace(File.separatorChar, '/');
		return "../../anypath/" + fullPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.archive.crawler.restlet2.JobResource#getJobStatus()
	 */
	public JobInfo getJobStatus() {

		final String job = (String) getRequest().getAttributes().get("job");
		final CrawlJob cj = getEngine().getJob(TextUtils.urlUnescape(job));
		if (job == null || cj == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		return getJobInfo(cj);
	}

	/**
	 * @param cj
	 * @return The {@link CrawlJob}'s information.
	 */
	private JobInfo getJobInfo(final CrawlJob cj) {
		final JobInfo info = new JobInfo();

		info.setShortName(cj.getShortName());
		info.setStatusDescription(getJobStatusDescription(cj));
		info.setAvailableActions(getAvailableActions(cj));
		info.setLaunchCount(cj.getLaunchCount());
		info.setLastLaunch(cj.getLastLaunch().getMillis());
		info.setProfile(cj.isProfile());

		File primaryConfig = FileUtils.tryToCanonicalize(cj.getPrimaryConfig());
		info.setPrimaryConfig(primaryConfig.getAbsolutePath());
		info.setPrimaryConfigUrl(getBaseUrl() + "jobdir/"
				+ primaryConfig.getName());

		if (cj.getJobLog().exists()) {
			try {
				List<String> logLines = new LinkedList<String>();
				FileUtils.pagedLines(cj.getJobLog(), -1, -5, logLines);
				info.setJobLogTail(logLines);
			} catch (IOException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Error reading from the log file", e);
			}
		}

		if (cj.hasApplicationContext()) {
			info.setUriTotalsReport(cj.uriTotalsReportData());
			info.setSizeTotalsReport(cj.sizeTotalsReportData());
			info.setRateReport(cj.rateReportData());
			info.setLoadReport(cj.loadReportData());
			info.setElapsedReport(cj.elapsedReportData());
			info.setThreadReport(cj.threadReportData());
			info.setFrontierReport(cj.frontierReportData());
			info.setHeapReport(getEngine().heapReportData());

			if ((cj.isRunning() || (cj.hasApplicationContext() && !cj
					.isLaunchable()))
					&& cj.getCrawlController().getLoggerModule()
							.getCrawlLogPath().getFile().exists()) {

				try {
					List<String> logLines = new LinkedList<String>();
					FileUtils.pagedLines(cj.getCrawlController()
							.getLoggerModule().getCrawlLogPath().getFile(), -1,
							-10, logLines);
				} catch (IOException e) {
					throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
							"Error reading from the log file", e);
				}
			}
		}

		List<Map<String, String>> configFiles = new LinkedList<Map<String, String>>();
		for (String cppp : cj.getConfigPaths().keySet()) {
			Map<String, String> configFileInfo = new LinkedHashMap<String, String>();
			configFileInfo.put("key", cppp);
			File path = FileUtils.tryToCanonicalize(cj.getConfigPaths().get(
					cppp).getFile());
			configFileInfo.put("path", path.getAbsolutePath());
			Reference urlRef = new Reference(getBaseUrl(),
					getHrefPath(path, cj)).getTargetRef();
			configFileInfo.put("url", urlRef.toString());
			configFiles.add(configFileInfo);
		}
		info.setConfigFiles(configFiles);

		return info;
	}

	/**
	 * @param cj
	 * @return
	 */
	private String getJobStatusDescription(final CrawlJob cj) {
		if (!cj.hasApplicationContext()) {
			return "Unbuilt";
		} else if (cj.isRunning()) {
			return "Active: " + cj.getCrawlController().getState();
		} else if (cj.isLaunchable()) {
			return "Ready";
		} else {
			return "Finished: " + cj.getCrawlController().getCrawlExitStatus();
		}
	}

	/**
	 * @param cj
	 * @return The set of currently available actions.
	 */
	private List<String> getAvailableActions(final CrawlJob cj) {
		List<String> actions = new LinkedList<String>();

		if (!cj.hasApplicationContext()) {
			actions.add("build");
		}
		if (!cj.isProfile() && cj.isLaunchable()) {
			actions.add("launch");
		}
		if (cj.isPausable()) {
			actions.add("pause");
		}
		if (cj.isUnpausable()) {
			actions.add("unpause");
		}
		if (cj.getCheckpointService() != null && cj.isRunning()) {
			actions.add("checkpoint");
		}
		if (cj.isRunning()) {
			actions.add("terminate");
		}
		if (cj.hasApplicationContext()) {
			actions.add("teardown");
		}

		return actions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.archive.crawler.restlet2.JobResource#manipulateJob()
	 */
	public OperationStatusResponse manipulateJob() {

		final String job = (String) getRequest().getAttributes().get("job");
		final CrawlJob cj = getEngine().getJob(TextUtils.urlUnescape(job));
		if (job == null || cj == null) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		OperationStatusResponse status;

		// copy op?
		final Form form = getRequest().getEntityAsForm();
		final String copyTo = form.getFirstValue("copyTo");
		if (copyTo != null) {

			try {
				getEngine().copy(cj, copyTo,
						"on".equals(form.getFirstValue("asProfile")));
				getResponse().redirectSeeOther(copyTo);
				status = new OperationStatusResponse(true, "job copied");
			} catch (IOException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Cannot copy the job", e);
			}

		} else {

			AlertHandler.ensureStaticInitialization();
			AlertThreadGroup.setThreadLogger(cj.getJobLogger());
			String action = form.getFirstValue("action");
			if ("launch".equals(action)) {

				String selectedCheckpoint = form.getFirstValue("checkpoint");
				if (StringUtils.isNotEmpty(selectedCheckpoint)) {
					cj.getCheckpointService().setRecoveryCheckpointByName(
							selectedCheckpoint);
				}
				cj.launch();
				status = new OperationStatusResponse(true,
						"Job has been launched");

			} else if ("checkXML".equals(action)) {

				cj.checkXML();
				if (cj.isXmlOk()) {
					status = new OperationStatusResponse(true,
							"Job configuration valid");
				} else {
					status = new OperationStatusResponse(false,
							"Error in your configuration");
					getResponse().setStatus(Status.CLIENT_ERROR_PRECONDITION_FAILED);
				}

			} else if ("instantiate".equals(action)) {
				cj.instantiateContainer();
				status = new OperationStatusResponse(true,
						"Job has been instantiated");
			} else if ("build".equals(action) || "validate".equals(action)) {
				cj.validateConfiguration();
				status = new OperationStatusResponse(true, "Job is valid");
			} else if ("teardown".equals(action)) {
				if (!cj.teardown()) {
					status = new OperationStatusResponse(true,
							"waiting for job to finish");
				} else {
					status = new OperationStatusResponse(true,
							"job has been shut down");
				}
			} else if ("pause".equals(action)) {
				cj.getCrawlController().requestCrawlPause();
				status = new OperationStatusResponse(true, "job paused");
			} else if ("unpause".equals(action)) {
				cj.getCrawlController().requestCrawlResume();
				status = new OperationStatusResponse(true, "job unpaused");
			} else if ("checkpoint".equals(action)) {
				String cp = cj.getCheckpointService().requestCrawlCheckpoint();
				if (StringUtils.isNotEmpty(cp)) {
					status = new OperationStatusResponse(true, "Checkpoing "
							+ cp + " saved");
				} else {
					status = new OperationStatusResponse(false,
							"Checkpoint not made; check logs");
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				}
			} else if ("terminate".equals(action)) {
				cj.terminate();
				status = new OperationStatusResponse(true, "job terminated");
			} else {
				status = new OperationStatusResponse(true, "unknown action");
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}
			AlertThreadGroup.setThreadLogger(null);

		}

		return status;
	}

}
