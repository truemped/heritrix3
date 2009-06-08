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
 
package org.archive.crawler.framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.archive.crawler.event.CrawlStateEvent;
import org.archive.crawler.reporting.AlertThreadGroup;
import org.archive.crawler.reporting.CrawlStatSnapshot;
import org.archive.crawler.reporting.StatisticsTracker;
import org.archive.spring.ConfigPath;
import org.archive.spring.ConfigPathConfigurer;
import org.archive.spring.PathSharingContext;
import org.archive.util.ArchiveUtils;
import org.archive.util.TextUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * CrawlJob represents a crawl configuration, including its 
 * configuration files, instantiated/running ApplicationContext, and 
 * disk output, potentially across multiple runs.
 * 
 * CrawlJob provides convenience methods for an administrative 
 * interface to assemble, launch, monitor, and manage crawls. 
 * 
 * @contributor gojomo
 */
public class CrawlJob implements Comparable<CrawlJob>, ApplicationListener{
    File primaryConfig; 
    PathSharingContext ac; 
    int launchCount; 
    DateTime lastLaunch;
    AlertThreadGroup alertThreadGroup;
    
    DateTime xmlOkAt = new DateTime(0L);
    Logger jobLogger;
    
    public CrawlJob(File cxml) {
        primaryConfig = cxml; 
        scanJobLog(); 
        alertThreadGroup = new AlertThreadGroup(getShortName());
    }
    
    public File getPrimaryConfig() {
        return primaryConfig;
    }
    public File getJobDir() {
        return getPrimaryConfig().getParentFile();
    }
    public String getShortName() {
        return getJobDir().getName();
    }
    public File getJobLog() {
        return new File(getJobDir(),"job.log");
    }
    
    public PathSharingContext getJobContext() {
        return ac; 
    }

    
    /**
     * Get a logger to a distinguished file, job.log in the job's
     * directory, into which job-specific events may be reported.
     * 
     * @return Logger writing to the job-specific log
     */
    public Logger getJobLogger() {
        if(jobLogger == null) {
            jobLogger = Logger.getLogger(getShortName());
            try {
                Handler h = new FileHandler(getJobLog().getAbsolutePath(),true);
                h.setFormatter(new JobLogFormatter());
                jobLogger.addHandler(h);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            jobLogger.setLevel(Level.INFO);
        }
        return jobLogger;
    }
    
    public DateTime getLastLaunch() {
        return lastLaunch;
    }
    public int getLaunchCount() {
        return launchCount;
    }
    /**
     * Refresh knowledge of total launched and last launch by scanning
     * the job.log. 
     */
    protected void scanJobLog() {
        File jobLog = getJobLog();
        launchCount = 0; 
        if(!jobLog.exists()) return;
        
        try {
            LineIterator lines = FileUtils.lineIterator(jobLog);
            Pattern launchLine = Pattern.compile("(\\S+) (\\S+) Job launched");
            while(lines.hasNext()) {
                String line = lines.nextLine();
                Matcher m = launchLine.matcher(line);
                if(m.matches()) {
                    launchCount++;
                    lastLaunch = new DateTime(m.group(1));
                }
            }
            LineIterator.closeQuietly(lines);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
    /**
     * Is this job a 'profile' (or template), meaning it may be editted
     * or copied to another jobs, but should not be launched. Profiles
     * are marked with the convention that their short name 
     * (job directory name) begins "profile-".
     * 
     * @return true if this job is a 'profile'
     */
    public boolean isProfile() {
        return primaryConfig.getName().startsWith("profile-");
    }

    //
    // writing a basic HTML representation
    //

    public void writeHtmlTo(PrintWriter pw) {
        writeHtmlTo(pw,"./");
    }
    public void writeHtmlTo(PrintWriter pw, String uriPrefix) {
        pw.println("<span class='job'>");
        if(isRunning()) {
            pw.println("ACTIVE; "+getCrawlController().getState()+":");
        }
        pw.println("<a href='"+uriPrefix+TextUtils.urlEscape(getShortName())+"'>"+getShortName()+"</a>");
        if(isProfile()) {
            pw.println("(profile)");
        }
        pw.println(" " + getLaunchCount() + " launches");
        pw.println("<br/><span style='color:#666'>");
        pw.println(getPrimaryConfig());
        pw.println("</span><br/>");
        if(lastLaunch!=null) {
            pw.println("(last at "+lastLaunch+")");
        }
        pw.println("</span>");
    }

    /**
     * Is the primary XML config minimally well-formed? 
     */
    public void checkXML() {
        // TODO: suppress check if XML unchanged? job.log when XML changed? 

        DateTime testTime = new DateTime(getPrimaryConfig().lastModified());
        Document doc = getDomDocument(getPrimaryConfig());
        // TODO: check for other minimal requirements, like
        // presence of a few key components (CrawlController etc.)? 
        if(doc!=null) {
            xmlOkAt = testTime; 
        } else {
            xmlOkAt = new DateTime(0L);
        }

    }

    /**
     * Read a file to a DOM Document; return null if this isn't possible
     * for any reason.
     * 
     * @param f File of XML
     * @return org.w3c.dom.Document or null if problems encountered
     */
    protected Document getDomDocument(File f) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            return docBuilder.parse(f);
        } catch (ParserConfigurationException e) {
            return null; 
        } catch (SAXException e) {
            return null; 
        } catch (IOException e) {
            return null; 
        }
    }
    
    /**
     * Is the primary config file legal XML?
     * 
     * @return true if the primary configuration file passed XML testing
     */
    public boolean isXmlOk() {
        return xmlOkAt.getMillis() >= getPrimaryConfig().lastModified();
    }
    
    
    /**
     * Can the configuration yield an assembled ApplicationContext? 
     */
    public void instantiateContainer() {
        checkXML(); 
        if(ac==null) {
            try {
                ac = new PathSharingContext(new String[] {"file:"+primaryConfig.getAbsolutePath()},false,null);
//                ac = new PathSharingContext(new String[] {primaryConfig.getAbsolutePath()},false,null);
                ac.addApplicationListener(this);
                ac.refresh();
                getCrawlController(); // trigger NoSuchBeanDefinitionException if no CC
                getJobLogger().log(Level.INFO,"Job instantiated");
            } catch (BeansException be) {
//                if(ac!=null) {
//                    ac.close();
//                }
                ac = null; 
                beansException(be);
            }
        }
    }
    
    /**
     * Report a BeansException during instantiation; report chain in 
     * reverse order (so root cause is first); ignore non-BeansExceptions
     * or messages without a useful compact message. 
     * @param be BeansException
     */
    protected void beansException(BeansException be) {
        LinkedList<String> beMsgs = new LinkedList<String>();
        Throwable t = be; 
        while (t!=null) {
            if(t instanceof BeansException) {
                String msg = shortMessage((BeansException)t);
                if(msg!=null) {
                    beMsgs.add(msg);
                }
            }
            t = t.getCause();
        }
        Collections.reverse(beMsgs);
        String shortMessage = StringUtils.join(beMsgs,"; ");
        
        getJobLogger().log(Level.SEVERE,shortMessage,be);
    }
    
    /**
     * Return a short useful message for common BeansExceptions. 
     * @param ex BeansException
     * @return String short descriptive message
     */
    protected String shortMessage(BeansException ex) {
        if(ex instanceof NoSuchBeanDefinitionException) {
            NoSuchBeanDefinitionException nsbde = (NoSuchBeanDefinitionException)ex;
            return "Missing required bean: "
                + (nsbde.getBeanName()!=null ? "\""+nsbde.getBeanName()+"\" " : "")
                + (nsbde.getBeanType()!=null ? "\""+nsbde.getBeanType()+"\" " : "");
        }
        if(ex instanceof BeanCreationException) {
            BeanCreationException bce = (BeanCreationException)ex;
            return bce.getBeanName()== null 
                    ? ""
                    : "Can't create bean '"+bce.getBeanName()+"'";
        }
        return ex.getMessage().replace('\n', ' ');
    }

    public boolean isContainerOk() {
        return ac!=null;
    }
    
    /**
     * Does the assembled ApplicationContext self-validate? Any failures
     * are reported as WARNING log events in the job log. 
     * 
     * TODO: make these severe? 
     */
    public void validateConfiguration() {
        instantiateContainer();
        if(ac==null) {
            // fatal errors already encountered and reported
            return; 
        }
        ac.validate();
        HashMap<String,Errors> allErrors = ac.getAllErrors();
        for(String name : allErrors.keySet()) {
            for(Object err : allErrors.get(name).getAllErrors()) {
                getJobLogger().log(Level.WARNING,err.toString());
            }
        }
    }

    /**
     * Ddid the ApplicationContext self-validate? 
     * return true if validation passed without errors
     */
    public boolean isContainerValidated() {
        if(ac==null) {
            return false;
        }
        HashMap<String,Errors> allErrors = ac.getAllErrors();
        return allErrors != null && allErrors.isEmpty();
    }
    
    //
    // Valid job lifecycle operations
    //
    
    /**
     * Launch a crawl into 'running' status, assembling if necessary. 
     * 
     * (Note the crawl may have been configured to start in a 'paused'
     * state.) 
     */
    public void launch() {
        if (isProfile()) {
            throw new IllegalArgumentException("Can't launch profile" + this);
        }
        
        if(isRunning()) {
            getJobLogger().log(Level.SEVERE,"Can't relaunch running job");
            return;
        } else {
            CrawlController cc = getCrawlController();
            if(cc!=null && cc.hasStarted()) {
                getJobLogger().log(Level.SEVERE,"Can't relaunch previously-launched assembled job");
                return;
            }
        }
        
        validateConfiguration();
        if(!isContainerValidated()) {
            getJobLogger().log(Level.SEVERE,"Can't launch problem configuration");
            return;
        }

        //final String job = changeState(j, ACTIVE);
        
        // this temporary thread ensures all crawl-created threads
        // land in the AlertThreadGroup, to assist crawl-wide 
        // logging/alerting
        alertThreadGroup = new AlertThreadGroup(getShortName());
        alertThreadGroup.addLogger(getJobLogger());
        Thread launcher = new Thread(alertThreadGroup, getShortName()) {
            public void run() {
                startContext();
            }
        };
        launcher.start();
        
        try {
            launcher.join();
        } catch (InterruptedException e) {
            // do nothing
        }
        
        if(ac==null) {
            // unlaunchable
            return; 
        }
        getCrawlController().requestCrawlStart();
        getJobLogger().log(Level.INFO,"Job launched");
        scanJobLog();
    }
    
    /**
     * Start the context, catching and reporting any BeansExceptions.
     */
    protected void startContext() {
        try {
            ac.start(); 
        } catch (BeansException be) {
            ac.close();
            ac = null; 
            beansException(be);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            getJobLogger().log(Level.SEVERE,e.getMessage(),e);
            try {
                ac.close();
            } catch (Exception e2) {
                e2.printStackTrace(System.err);
            } finally {
                ac = null;
            }
        }
    }

    /** 
     * Sort for reverse-chronological listing.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(CrawlJob o) {
        // prefer reverse-chronological ordering
        return -((Long)getLastActivityTime()).compareTo(o.getLastActivityTime());
    }
    
    public long getLastActivityTime() {
        return Math.max(getPrimaryConfig().lastModified(), getJobLog().lastModified());
    }
    
    public boolean isRunning() {
        return this.ac != null && this.ac.isActive() && this.ac.isRunning();
    }

    public CrawlController getCrawlController() {
        if(ac==null) {
            return null;
        }
        return (CrawlController) ac.getBean("crawlController");
    }

    public boolean isPausable() {
        CrawlController cc = getCrawlController(); 
        if(cc==null) {
            return false;
        }
        return cc.isStateRunning(); 
    }
    
    public boolean isUnpausable() {
        CrawlController cc = getCrawlController(); 
        if(cc==null) {
            return false;
        }
        return cc.isPaused() || cc.isPausing();
    }
    
    /**
     * Ensure a fresh start for any configuration changes or relaunches,
     * by stopping and discarding an existing ApplicationContext.
     */
    public void reset() {
        if(ac!=null) {
            CrawlController cc = getCrawlController();
            if(cc!=null) {
                cc.requestCrawlStop();
                // TODO: wait for stop?
            }
            if(ac.isRunning()) {
                ac.stop(); 
            }
            ac = null;
        }
        xmlOkAt = new DateTime(0); 
        getJobLogger().log(Level.INFO,"Job instance discarded");
    }

    /**
     * Formatter for job.log
     */
    public class JobLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb
              .append(new DateTime(record.getMillis()))
              .append(" ")
              .append(record.getLevel())
              .append(" ")
              .append(record.getMessage())
              .append("\n");
            return  sb.toString();
        }
    }

    /**
     * Return all config files included via 'import' statements in the
     * primary config (or other included configs). 
     * 
     * @param xml File to examine
     * @return List<File> of all transitively-imported Files
     */
    @SuppressWarnings("unchecked")
    public List<File> getImportedConfigs(File xml) {
        List<File> imports = new LinkedList<File>(); 
        Document doc = getDomDocument(xml);
        if(doc==null) {
            return ListUtils.EMPTY_LIST;
        }
        NodeList importElements = doc.getElementsByTagName("import");
        for(int i = 0; i < importElements.getLength(); i++) {
            File imported = new File(
                    getJobDir(),
                    importElements.item(i).getAttributes().getNamedItem("resource").getTextContent());
            imports.add(imported);
            imports.addAll(getImportedConfigs(imported));
        }
        return imports; 
    }
    
    /**
     * Return all known ConfigPaths, as an aid to viewing or editting. 
     * 
     * @return all ConfigPaths known to the ApplicationContext, in a 
     * map by name, or an empty map if no ApplicationContext
     */
    @SuppressWarnings("unchecked")
    public Map<String, ConfigPath> getConfigPaths() {
        if(ac==null) {
            return MapUtils.EMPTY_MAP;
        }
        ConfigPathConfigurer cpc = 
            (ConfigPathConfigurer)ac.getBean("configPathConfigurer");
        return cpc.getPaths();        
    }

    /**
     * Compute a path relative to the job directory for all contained 
     * files, or null if the File is not inside the job directory. 
     * 
     * @param f File
     * @return path relative to the job directory, or null if File not 
     * inside job dir
     */
    public String jobDirRelativePath(File f) {
        try {
            String filePath = f.getCanonicalPath();
            String jobPath = getJobDir().getCanonicalPath();
            if(filePath.startsWith(jobPath)) {
                String jobRelative = filePath.substring(jobPath.length()).replace(File.separatorChar, '/');
                if(jobRelative.startsWith("/")) {
                    jobRelative = jobRelative.substring(1); 
                }
                return jobRelative;
            }
        } catch (IOException e) {
            getJobLogger().log(Level.WARNING,"bad file: "+f);
        }
        return null; 
    }

    /** 
     * Log note of all ApplicationEvents.
     * 
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof CrawlStateEvent) {
            getJobLogger().log(Level.INFO, ((CrawlStateEvent)event).getState().toString());
        }
    }

    /**
     * Is this launchable? (Has CrawlController and not yet been launched?)
     * @return true if launchable
     */
    public boolean isLaunchable() {
        CrawlController cc = getCrawlController();
        if(cc==null) {
            return true;
        }
        return !cc.hasStarted();
    }

    public int getAlertCount() {
        return alertThreadGroup.getAlertCount();
    }
    
    protected StatisticsTracker getStats() {
        CrawlController cc = getCrawlController();
        return cc!=null ? cc.getStatisticsTracker() : null;
    }

    public Object rateReport() {
        StatisticsTracker stats = getStats();
        if(stats==null) {
            return "<i>n/a</i>";
        }
        CrawlStatSnapshot snapshot = stats.getSnapshot();
        StringBuilder sb = new StringBuilder();
        sb
         .append(ArchiveUtils.doubleToString(snapshot.currentDocsPerSecond,2))
         .append(" URIs/sec (")
         .append(ArchiveUtils.doubleToString(snapshot.docsPerSecond,2))
         .append(" avg); ")
         .append(snapshot.currentKiBPerSec)
         .append(" KB/sec (")
         .append(snapshot.totalKiBPerSec)
         .append(" avg)");
        return sb.toString();
    }

    public Object loadReport() {
        StatisticsTracker stats = getStats();
        if(stats==null) {
            return "<i>n/a</i>";
        }
        CrawlStatSnapshot snapshot = stats.getSnapshot();
        StringBuilder sb = new StringBuilder();
        sb
         .append(snapshot.busyThreads)
         .append(" active of ")
         .append(stats.threadCount())
         .append(" threads; ")
         .append(ArchiveUtils.doubleToString(snapshot.congestionRatio,2))
         .append(" congestion ratio; ")
         .append(snapshot.deepestUri)
         .append("  deepest queue; ")
         .append(snapshot.averageDepth)
         .append("  average depth");
        return sb.toString();
    }

    public String uriTotalsReport() {
        StatisticsTracker stats = getStats();
        if(stats==null) {
            return "<i>n/a</i>";
        }
        CrawlStatSnapshot snapshot = stats.getSnapshot();
        long downloaded = snapshot.downloadedUriCount;
        long total = snapshot.totalCount();
        long queued = snapshot.queuedUriCount; 
        StringBuilder sb = new StringBuilder(64); 
        sb
         .append(downloaded)
         .append(" downloaded + ")
         .append(queued)
         .append(" queued = ")
         .append(total)
         .append(" total");
         return sb.toString(); 
    }

    public String sizeTotalsReport() {
        StatisticsTracker stats = getStats();
        if(stats==null) {
            return "<i>n/a</i>";
        }
        return stats.crawledBytesSummary();
    }

    public String elapsedReport() {
        StatisticsTracker stats = getStats();
        if(stats==null) {
            return "<i>n/a</i>";
        }
        long timeElapsed = stats.getCrawlElapsedTime();
        return ArchiveUtils.formatMillisecondsToConventional(timeElapsed,false);
    }

    public String threadReport() {
        CrawlController cc = getCrawlController();
        if(cc==null) {
            return "<i>n/a</i>";
        }
        return cc.getToeThreadReportShort();
    }

    public String frontierReport() {
        CrawlController cc = getCrawlController();
        if(cc==null) {
            return "<i>n/a</i>";
        }
        return cc.getFrontierReportShort();
    }

    public void terminate() {
        getCrawlController().requestCrawlStop();
    }
    
}//EOC
