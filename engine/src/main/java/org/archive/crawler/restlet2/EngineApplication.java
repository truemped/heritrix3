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

import org.archive.crawler.framework.Engine;
import org.archive.crawler.restlet2.impl.EngineResourceImpl;
import org.archive.crawler.restlet2.impl.JobResourceImpl;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;

/**
 * @author Daniel Truemper <truemped@googlemail.com>
 * 
 */
public class EngineApplication extends Application {

	/**
	 * This apps engine.
	 */
	private Engine engine;

	/**
	 * @param e
	 */
	public EngineApplication(final Engine e) {
		this.engine = e;
	}

	/**
	 * @return The underlying engine.
	 */
	public Engine getEngine() {
		return this.engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.Application#createInboundRoot()
	 */
	@Override
	public synchronized Restlet createInboundRoot() {

		Router router = new Router(getContext());
		router.attach("/", new Redirector(null, "/engine",
				Redirector.MODE_CLIENT_PERMANENT));
		router.attach("/engine", new Redirector(null, "/engine/",
				Redirector.MODE_CLIENT_PERMANENT));

		router.attach("/engine/", EngineResourceImpl.class);
		router.attach("/engine/job/{job}", JobResourceImpl.class);

		return router;
	}

}
