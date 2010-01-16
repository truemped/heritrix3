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

/**
 * @author Daniel Truemper <truemped@googlemail.com>
 * 
 */
public class OperationStatusResponse {

	/**
	 * Determine, if the operation successful.
	 */
	private boolean success = false;

	/**
	 * Detailed message.
	 */
	private String msg;

	/**
	 * @param success
	 * @param msg
	 */
	public OperationStatusResponse(final boolean success, final String msg) {
		this.success = success;
		this.msg = msg;
	}

	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @param success
	 *            the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * @return the msg
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * @param msg
	 *            the msg to set
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

}
