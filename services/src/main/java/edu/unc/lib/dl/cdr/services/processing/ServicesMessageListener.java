/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.processing;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.fedora.ClientUtils;

public class ServicesMessageListener implements MessageListener {
	private static final Logger LOG = LoggerFactory.getLogger(ServicesMessageListener.class);
	
	private MessageDirector messageDirector = null;
	
	private String messageNamespace = null;
	
	public ServicesMessageListener(){
		
	}
	
	@Override
	public void onMessage(Message message) {
		LOG.debug("I HAVE A MESSAGE!!!!!");
		if (message instanceof TextMessage) {
			try {
				String msgText = ((TextMessage) message).getText();
				LOG.debug(msgText);
				Document msgXML = ClientUtils.parseXML(msgText.getBytes());
				messageDirector.direct(msgXML, messageNamespace);
			} catch (JMSException e) {
				LOG.error("onMessage failed", e);
			} catch (SAXException e) {
				LOG.error("onMessage failed", e);
			}
		} else {
			throw new IllegalArgumentException("Message must be of type TextMessage");
		}
	}

	
	public MessageDirector getMessageDirector() {
		return messageDirector;
	}

	public void setMessageDirector(MessageDirector messageDirector) {
		this.messageDirector = messageDirector;
	}

	public String getMessageNamespace() {
		return messageNamespace;
	}

	public void setMessageNamespace(String messageNamespace) {
		this.messageNamespace = messageNamespace;
	}
}
