/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.archive.crawler.restlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.Engine;
import org.archive.crawler.reporting.AlertHandler;
import org.archive.crawler.reporting.AlertThreadGroup;
import org.archive.crawler.reporting.Report;
import org.archive.crawler.reporting.StatisticsTracker;
import org.archive.spring.ConfigPath;
import org.archive.util.ArchiveUtils;
import org.archive.util.FileUtils;
import org.archive.util.TextUtils;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.resource.WriterRepresentation;

/**
 * Restlet Resource representing a single local CrawlJob inside an
 * Engine.
 * 
 * @contributor gojomo
 */
public class JobResource extends Resource {
    public static final IOFileFilter EDIT_FILTER = 
        FileUtils.getRegexFileFilter(".*\\.((c?xml)|(txt))$");

    CrawlJob cj; 
    
    public JobResource(Context ctx, Request req, Response res) throws ResourceException {
        super(ctx, req, res);
        setModifiable(true);
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        cj = getEngine().getJob(TextUtils.urlUnescape((String)req.getAttributes().get("job")));
        if(cj==null) {
            throw new ResourceException(404);
        }
    }

    public Representation represent(Variant variant) throws ResourceException {
        Representation representation = new WriterRepresentation(
                MediaType.TEXT_HTML) {
            public void write(Writer writer) throws IOException {
                JobResource.this.writeHtml(writer);
            }
        };
        // TODO: remove if not necessary in future?
        representation.setCharacterSet(CharacterSet.UTF_8);
        return representation;
    }

    protected void writeHtml(Writer writer) {
        PrintWriter pw = new PrintWriter(writer); 
        String jobTitle = cj.getShortName()+ " - Job main page";
        String baseRef = getRequest().getResourceRef().getBaseRef().toString();
        if(!baseRef.endsWith("/")) {
            baseRef += "/";
        }
        // TODO: replace with use a templating system (FreeMarker?)
        pw.println("<head><title>"+jobTitle+"</title>");
        pw.println("<base href='"+baseRef+"'/>");
        pw.println("</head><body>");
        pw.print("<h1>Job <i>"+cj.getShortName()+"</i> (");
        
        pw.print(cj.getLaunchCount() + " launches");
        if(cj.getLastLaunch()!=null) {
            long ago = System.currentTimeMillis() - cj.getLastLaunch().getMillis();
            pw.println(", last "+ArchiveUtils.formatMillisecondsToConventional(ago, 2)+" ago");
        }
        pw.println(")</h1>");
        
        Flash.renderFlashesHTML(pw, getRequest());
        
        if(cj.isProfile()) {
            pw.print(
                "<p>As a <i>profile</i>, this job may be built for " +
                "testing purposes but not launched. Use the 'copy job to' " +
                "functionality at bottom to copy this profile to a " +
                "launchable job.</p>");
        }
        
        // button controls
        pw.println("<div style='white-space:nowrap'><form method='POST'>");
        // PREP, LAUNCH
        pw.print("<input type='submit' name='action' value='build' ");
        pw.print(cj.isContainerValidated()?"disabled='disabled' title='build job'":"");
        pw.println("/>");
        pw.print("<input type='submit' name='action' value='launch'");
        if(cj.isProfile()) {
            pw.print("disabled='disabled' title='profiles cannot be launched'");
        }
        if(!cj.isLaunchable()) {
            pw.print("disabled='disabled' title='launched OK'");
        }
        pw.println("/>&nbsp;&nbsp;&nbsp;");
        
        // PAUSE, UNPAUSE, CHECKPOINT
        pw.println("<input ");
        if(!cj.isPausable()) {
            pw.println(" disabled ");
        }
        pw.println(" type='submit' name='action' value='pause'/>");
        pw.println("<input ");
        if(!cj.isUnpausable()) {
            pw.println(" disabled ");
        }
        pw.println(" type='submit' name='action' value='unpause'/>");
        pw.println("<input ");
        if(true /*!cj.isUnpausable()*/) { // TODO: not yet implemented
            pw.println(" disabled ");
        }
        pw.println(" type='submit' name='action' value='checkpoint'/>&nbsp;&nbsp;&nbsp;");

        
        // TERMINATE, RESET
        pw.println("<input ");
        if(!cj.isRunning()) {
            pw.println(" disabled ");
        }
        pw.println(" type='submit' name='action' value='terminate'/>");
        pw.println("<input type='submit' name='action' value='teardown' ");
        pw.print(cj.isContainerOk()?"":"disabled='disabled' title='no instance'");
        pw.println("/><br/>");

        pw.println("</form></div>");
        
        // configuration 
        pw.println("configuration: ");
        printLinkedFile(pw, cj.getPrimaryConfig());
        for(File f : cj.getImportedConfigs(cj.getPrimaryConfig())) {
            pw.println("imported: ");
            printLinkedFile(pw,f);
        }
        
//        if(cj.isXmlOk()) {
//            pw.println("cxml ok<br/>");
//            if(cj.isContainerOk()) {
//                pw.println("container ok<br/>");
//                if(cj.isContainerValidated()) {
//                    pw.println("config valid<br/>");
//                } else {
//                    pw.println("CONFIG INVALID<br/>");
//                }
//            } else {
//                pw.println("CONTAINER BAD<br/>");
//            }
//        }else {
//            // pw.println("XML NOT WELL-FORMED<br/>");
//        }

        pw.println("<h2>Job Log ");
        pw.println("(<a href='jobdir/"
                +cj.getJobLog().getName()
                +"?format=paged&pos=-1&lines=-128&reverse=y'><i>more</i></a>)");
        pw.println("</h2>");
        pw.println("<div style='font-family:monospace; white-space:pre-wrap; white-space:normal; text-indent:-10px; padding-left:10px;'>");
        if(cj.getJobLog().exists()) {
            try {
                List<String> logLines = new LinkedList<String>();
                FileUtils.pagedLines(cj.getJobLog(), -1, -5, logLines);
                Collections.reverse(logLines);
                for(String line : logLines) {
                    pw.print("<p style='margin:0px'>");
                    StringEscapeUtils.escapeHtml(pw,line);
                    pw.print("</p>");
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe); 
            }
        }
        pw.println("</div>");
        
       
        if(!cj.isContainerOk()) {
            pw.println("<h2>Unbuilt Job</h2>");
        } else if(cj.isRunning()) {
            pw.println("<h2>Active Job: "+cj.getCrawlController().getState()+"</h2>");
        } else if(cj.isLaunchable()){
            pw.println("<h2>Ready Job</h2>");
        } else {
            pw.println("<h2>Finished Job: "+cj.getCrawlController().getCrawlExitStatus()+"</h2>");
        }

        if(cj.isContainerOk()) {
            pw.println("<b>Totals</b><br/>&nbsp;&nbsp;");
            pw.println(cj.uriTotalsReport());
            pw.println("<br/>&nbsp;&nbsp;");
            pw.println(cj.sizeTotalsReport());
                        
            pw.println("<br/><b>Alerts</b><br>&nbsp;&nbsp;");
            pw.println(cj.getAlertCount()==0 ? "<i>none</i>" : cj.getAlertCount()); 
            if(cj.getAlertCount()>0) {
                printLinkedFile(
                        pw, 
                        cj.getCrawlController().getLoggerModule().getAlertsLogPath().getFile(), 
                        "tail alert log...",
                        "format=paged&pos=-1&lines=-128");
            }
            
            pw.println("<br/><b>Rates</b><br/>&nbsp;&nbsp;");
            pw.println(cj.rateReport());
            
            pw.println("<br/><b>Load</b><br/>&nbsp;&nbsp;");
            pw.println(cj.loadReport());
            
            pw.println("<br/><b>Elapsed</b><br/>&nbsp;&nbsp;");
            pw.println(cj.elapsedReport());
            
            pw.println("<br/><a href='report/ToeThreadsReport'><b>Threads</b></a><br/>&nbsp;&nbsp;");
            pw.println(cj.threadReport());
    
            pw.println("<br/><a href='report/FrontierSummaryReport'><b>Frontier</b></a><br/>&nbsp;&nbsp;");
            pw.println(cj.frontierReport());
            
            pw.println("<br/><b>Memory</b><br/>&nbsp;&nbsp;");
            pw.println(getEngine().heapReport());
            
            if(cj.isRunning() || (cj.isContainerOk() && !cj.isLaunchable())) {
                // show crawl log for running or finished crawls
                pw.println("<h3>Crawl Log");
                printLinkedFile(
                        pw,
                        cj.getCrawlController().getLoggerModule().getCrawlLogPath().getFile(),
                        "<i>more</i>",
                        "format=paged&pos=-1&lines=-128&reverse=y");
                pw.println("</h3>");
                pw.println("<pre style='overflow:auto'>");
                try {
                    List<String> logLines = new LinkedList<String>();
                    FileUtils.pagedLines(
                            cj.getCrawlController().getLoggerModule().getCrawlLogPath().getFile(),
                            -1, 
                            -10, 
                            logLines);
                    Collections.reverse(logLines);
                    for(String line : logLines) {
                        StringEscapeUtils.escapeHtml(pw,line);
                        pw.println();
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe); 
                }
                pw.println("</pre>");
            }
            
        }
        
        if(cj.isContainerOk()) {
            pw.println("<h2>Reports</h2>");
            for(Class<Report> reportClass : StatisticsTracker.LIVE_REPORTS) {
                String className = reportClass.getSimpleName();
                String shortName = className.substring(0,className.length()-"Report".length());
                pw.println("<a href='report/"+className+"'>"+shortName+"</a>");
            }
        }
        
        pw.println("<h2>Files</h2>");
        pw.println("<h3>Browse <a href='jobdir'>Job Directory</a></h3>");
        // specific paths from wired context
        pw.println("<h3>Configuration-referenced Paths</h3>");
        if(cj.getConfigPaths().isEmpty()) {
            pw.println("<i>build the job to discover referenced paths</i>");
        } else {
            pw.println("<dl>");
            for(String cppp : cj.getConfigPaths().keySet()) {
                ConfigPath cp = cj.getConfigPaths().get(cppp);
                pw.println("<dt>"+cppp+": "+cp.getName()+"</dt>");
                pw.println("<dd>");
                if(!StringUtils.isEmpty(cp.getPath())) {
                    printLinkedFile(
                            pw, 
                            cp.getFile(), 
                            cp.getFile().toString(),
                            cp.getPath().endsWith(".log")?"format=paged&pos=-1&lines=-128&reverse=y":null);
                } else {
                    pw.println("<i>unset</i>");
                }
                pw.println("</dd>");
            }
            pw.println("</dl>");

        }
        
        pw.println("<h2>Advanced</h2>");
        pw.println("<h3><a href='script'>Scripting console</a></h3>");

        if(!cj.isContainerOk()) {
            pw.println("<i>build the job to browse bean instances</i>");
        } else {
            pw.println("<h3><a href='beans'>Browse beans</a></h3>");
        }

        pw.println("<h2>Copy</h2>");
        pw.println(
            "<form method='POST'>Copy job to <input name='copyTo'/>" +
            "<input value='copy' type='submit'/>" +
            "<input id='asProfile' type='checkbox' name='asProfile'/>" +
            "<label for='asProfile'>as profile</label></form>");
        pw.println("<hr/>");
        pw.close();
    }

    /**
     * Print a link to the given File
     * 
     * @param pw PrintWriter
     * @param f File
     */
    protected void printLinkedFile(PrintWriter pw, File f) { 
        printLinkedFile(pw,f,f.toString(),null);
    }
    
    /**
     * Print a link to the given File, using the given link text
     * 
     * @param pw PrintWriter
     * @param f File
     */
    protected void printLinkedFile(PrintWriter pw, File f, String linktext, String queryString) {      
        String relativePath = JobResource.getHrefPath(f,cj);
        pw.println("<a href='" 
                + relativePath 
                + ((queryString==null) ? "" : "?" + queryString)
                + "'>" 
                + linktext +"</a>");
        if(EDIT_FILTER.accept(f)) {
            pw.println("[<a href='" 
                    + relativePath 
                    +  "?format=textedit'>edit</a>]<br/>");
        }
    }

    /**
     * Get a usable HrefPath, relative to the JobResource, for the given
     * file. Assumes usual helper resources ('jobdir/', 'anypath/') at
     * the usual locations.
     * 
     * @param f File to provide an href (suitable for clicking or redirection)
     * @param cj CrawlJob for calculating jobdir-relative path if possible
     * @return String path suitable as href or Location header
     */
    public static String getHrefPath(File f, CrawlJob cj) {
        String jobDirRelative = cj.jobDirRelativePath(f);
        if(jobDirRelative!=null) {
            return "jobdir/"+jobDirRelative;
        }
        // TODO: delegate this to EngineApplication, or make
        // conditional on whether /anypath/ service is present?
        String fullPath = f.getAbsolutePath();
        fullPath = fullPath.replace(File.separatorChar, '/');
        return "../../anypath/"+fullPath;
    }

    protected Engine getEngine() {
        return ((EngineApplication)getApplication()).getEngine();
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        // copy op?
        Form form = getRequest().getEntityAsForm();
        String copyTo = form.getFirstValue("copyTo");
        if(copyTo!=null) {
            copyJob(copyTo,"on".equals(form.getFirstValue("asProfile")));
            return;
        }
        AlertHandler.ensureStaticInitialization();
        AlertThreadGroup.setThreadLogger(cj.getJobLogger());
        String action = form.getFirstValue("action");
        if("launch".equals(action)) {
            cj.launch(); 
        } else if("checkXML".equals(action)) {
            cj.checkXML();
        } else if("instantiate".equals(action)) {
            cj.instantiateContainer();
        } else if("build".equals(action)||"validate".equals(action)) {
            cj.validateConfiguration();
        } else if("teardown".equals(action)) {
            if(!cj.teardown()) {
                Flash.addFlash(getResponse(), "waiting for job to finish", Flash.Kind.NACK);
            }
        } else if("pause".equals(action)) {
            cj.getCrawlController().requestCrawlPause();
        } else if("unpause".equals(action)) {
            cj.getCrawlController().requestCrawlResume();
        } else if("terminate".equals(action)) {
            cj.terminate();
        }
        AlertThreadGroup.setThreadLogger(null);
        // default: redirect to GET self
        getResponse().redirectSeeOther(getRequest().getOriginalRef());
    }

    protected void copyJob(String copyTo, boolean asProfile) throws ResourceException {
        try {
            getEngine().copy(cj, copyTo, asProfile);
        } catch (IOException e) {
            throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,e);
        }
        // redirect to destination job page
        getResponse().redirectSeeOther(copyTo);
    }
    
    
}
