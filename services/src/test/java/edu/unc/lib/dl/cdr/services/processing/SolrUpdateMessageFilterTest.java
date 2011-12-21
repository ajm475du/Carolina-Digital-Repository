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

import java.io.InputStreamReader;

import javax.annotation.Resource;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-unit.xml" })
public class SolrUpdateMessageFilterTest extends Assert {

	@Resource
	private SolrUpdateMessageFilter solrUpdateMessageFilter;
	
	@Test
	public void nullMessage(){
		assertFalse(solrUpdateMessageFilter.filter(null));
		String pid = null;
		try {
			solrUpdateMessageFilter.filter(new PIDMessage(pid, null, null));
			assertTrue(false);
		} catch (IllegalArgumentException e){
			assertTrue(true);
		}
		PIDMessage emptyMessage = new PIDMessage("", "", "");
		assertFalse(solrUpdateMessageFilter.filter(emptyMessage));
	}
	
	@Test
	public void solrMessages() throws Exception{
		//All should pass
		PIDMessage message = new PIDMessage("cdr:test", SolrUpdateAction.namespace, SolrUpdateAction.ADD.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(SolrUpdateAction.DELETE_SOLR_TREE.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(SolrUpdateAction.CLEAN_REINDEX.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(SolrUpdateAction.RECURSIVE_REINDEX.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(SolrUpdateAction.RECURSIVE_ADD.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
	}
	
	@Test
	public void fedoraMessages() throws Exception{
		Document doc = readFileAsString("ingestMessage.xml");
		PIDMessage message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertFalse(solrUpdateMessageFilter.filter(message));
		
		//Purge object passes
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_OBJECT.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		doc = readFileAsString("modifyDSDataFile.xml");
		message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertFalse(solrUpdateMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.getName());
		assertFalse(solrUpdateMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.ADD_DATASTREAM.getName());
		assertFalse(solrUpdateMessageFilter.filter(message));		
		
		//md descriptive, passes some of the time
		doc = readFileAsString("modifyDSMDDescriptive.xml");
		message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.getName());
		assertTrue(solrUpdateMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.ADD_DATASTREAM.getName());
		assertFalse(solrUpdateMessageFilter.filter(message));
		
		//Relationships
		doc = readFileAsString("addRelSourceData.xml");
		message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertFalse(solrUpdateMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertFalse(solrUpdateMessageFilter.filter(message));
	}
	
	@Test
	public void servicesMessages() throws Exception{
		PIDMessage message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), "");
		assertFalse(solrUpdateMessageFilter.filter(message));
		message.setServiceName(TechnicalMetadataEnhancementService.class.getName());
		assertFalse(solrUpdateMessageFilter.filter(message));
		message.setServiceName(null);
		assertFalse(solrUpdateMessageFilter.filter(message));
		message.setServiceName("does.not.exist.Service");
		assertFalse(solrUpdateMessageFilter.filter(message));
		message.setServiceName("");
		assertFalse(solrUpdateMessageFilter.filter(message));
		//Full stack run
		message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		assertFalse(solrUpdateMessageFilter.filter(message));
		message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), "");
		assertFalse(solrUpdateMessageFilter.filter(message));
		
	}
	
	private Document readFileAsString(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}
}