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

package org.archive.spring;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;

/**
 * Bean to fixup all configuration-relative ConfigPath instances, and
 * maintain an inventory of referenced paths. 
 * 
 * For each bean, at BeanPostProcessor time, the bean is remembered for
 * later fixup. Then, at ApplicationListener ContextRefreshedEvent time, 
 * fixup occurs at the latest possible time. This allows other intervening 
 * Spring post-processors -- most notably, PropertyOverrideConfigurer -- to 
 * replace values before fixup, and fixup will still affect the right
 * final values. 
 * 
 * @contributor gojomo
 */
public class ConfigPathConfigurer 
implements 
    BeanPostProcessor, 
    ApplicationListener,
    ApplicationContextAware, 
    Ordered {
    Map<String,Object> allBeans = new HashMap<String,Object>();
    
    
    //// BEAN PROPERTIES
    
    /** 'home' directory for all other paths to be resolved 
     * relative to; defaults to directory of primary XML config file */
    ConfigPath path; 
    public ConfigPath getPath() {
        return path;
    }
    
    //// BEANPOSTPROCESSOR IMPLEMENTATION
    /**
     * Remember all beans for later fixup.
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
     */
    public Object postProcessAfterInitialization(Object bean, String beanName)
    throws BeansException {
        allBeans.put(beanName,bean);
        return bean;
    }
    
    // APPLICATIONLISTENER IMPLEMENTATION
    /**
     * Fix all beans with ConfigPath properties that lack a base path
     * or a name, to use a job-implied base path and name. 
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextRefreshedEvent) {
            for(String k : allBeans.keySet()) {
                fixupPaths(allBeans.get(k),k);
            }
            allBeans.clear(); // forget 
        }
        // ignore all others
    }
    
    /**
     * Find any ConfigPath properties in the passed bean; ensure that
     * if they have a null 'base', that is replaced with the job home
     * directory. Also, remember all ConfigPaths so fixed-up for later
     * reference. 
     * 
     * @param bean
     * @param beanName
     * @return Same bean as passed in, fixed as necessary
     */
    protected Object fixupPaths(Object bean, String beanName) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        for(PropertyDescriptor d : wrapper.getPropertyDescriptors()) {
            if(d.getPropertyType().isAssignableFrom(ConfigPath.class)
                || d.getPropertyType().isAssignableFrom(ConfigFile.class)) {
                Object value = wrapper.getPropertyValue(d.getName());
                if(ConfigPath.class.isInstance(value)) {
                    ConfigPath cp = (ConfigPath) value;
                    if(cp==null) {
                        continue;
                    }
                    if(cp.getBase()==null && cp != path) {
                        cp.setBase(path);
                    }
                    String beanPath = beanName+"."+d.getName();
                    if(StringUtils.isEmpty(cp.getName())) {
                        cp.setName(beanPath);
                    }
                    remember(beanPath, cp);
                }
            }
        }
        if(bean instanceof PathFixupListener) {
            ((PathFixupListener)bean).pathsFixedUp();
        }
        return bean;
    }

    //// APPLICATIONCONTEXTAWARE IMPLEMENTATION

    AbstractApplicationContext appCtx;
    /**
     * Remember ApplicationContext, and if possible primary 
     * configuration file's home directory. 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
        this.appCtx = (AbstractApplicationContext)appCtx;
        String basePath;
        if(appCtx instanceof PathSharingContext) {
            String primaryConfigurationPath = ((PathSharingContext)appCtx).getPrimaryConfigurationPath();
            if(primaryConfigurationPath.startsWith("file:")) {
                // strip URI-scheme if present (as is usual)
                primaryConfigurationPath = primaryConfigurationPath.substring(5);
            }
            File configFile = new File(primaryConfigurationPath);
            basePath = configFile.getParent();
        } else {
            basePath = ".";
        }
        path = new ConfigPath("job base",basePath); 
    }

    // REMEMBERED CONFIGPATHS
    Map<String,ConfigPath> allConfigPaths = new HashMap<String,ConfigPath>();
    protected void remember(String key, ConfigPath cp) {
        allConfigPaths.put(key, cp);
    }
    public Map<String,ConfigPath> getAllConfigPaths() {
        return allConfigPaths; 
    }
    
    // noop
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
    throws BeansException {
        return bean;
    }

    /** 
     * Act as late as possible.
     * 
     * @see org.springframework.core.Ordered#getOrder()
     */
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
