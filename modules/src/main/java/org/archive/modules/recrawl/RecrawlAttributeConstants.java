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

package org.archive.modules.recrawl;

/**
 * 
 * @author pjack
 *
 */
public interface RecrawlAttributeConstants {

    /* Duplication-reduction / recrawl / history constants */
    
    /** fetch history array */ 
    public static final String A_FETCH_HISTORY = "fetch-history";
    /** content digest */
    public static final String A_CONTENT_DIGEST = "content-digest";
        /** header name (and AList key) for last-modified timestamp */
    public static final String A_LAST_MODIFIED_HEADER = "last-modified";
        /** header name (and AList key) for ETag */
    public static final String A_ETAG_HEADER = "etag"; 
    /** key for status (when in history) */
    public static final String A_STATUS = "status"; 
    /** reference length (content length or virtual length */
    public static final String A_REFERENCE_LENGTH = "reference-length";

}
