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
package edu.unc.lib.dl.update;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class FedoraObjectUIPProcessorTest extends Assert {

	@Test
	public void invalidDatastreamAndMissingContent() throws Exception {
		AccessClient accessClient = mock(AccessClient.class);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);
		
		UIPUpdatePipeline pipeline = mock(UIPUpdatePipeline.class);
		DigitalObjectManager digitalObjectManager = mock(DigitalObjectManager.class);
		
		FedoraObjectUIPProcessor uipProcessor = new FedoraObjectUIPProcessor();
		uipProcessor.setAccessClient(accessClient);
		uipProcessor.setDigitalObjectManager(digitalObjectManager);
		uipProcessor.setPipeline(pipeline);
		
		
		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");
		
		Map<String,File> modifiedFiles = new HashMap<String,File>();
		modifiedFiles.put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), mock(File.class));
		modifiedFiles.put(ContentModelHelper.Datastream.MD_TECHNICAL.getName(), null);
		modifiedFiles.put(ContentModelHelper.Datastream.AUDIT.getName(), mock(File.class));
		modifiedFiles.put("INVALID", mock(File.class));
		
		FedoraObjectUIP uip = mock(FedoraObjectUIP.class);
		when(uip.getModifiedFiles()).thenReturn(modifiedFiles);
		when(uip.getPID()).thenReturn(pid);
		when(uip.getUser()).thenReturn(user);
		when(uip.getOperation()).thenReturn(UpdateOperation.ADD);
		
		when(pipeline.processUIP(uip)).thenReturn(uip);
		
		uipProcessor.process(uip);
		
		verify(uip, times(1)).getModifiedFiles();
		verify(uip, times(1)).storeOriginalDatastreams(any(AccessClient.class));
		verify(digitalObjectManager, times(2)).addOrReplaceDatastream(any(PID.class), any(Datastream.class), any(File.class), anyString(), any(Agent.class), anyString());
		verify(digitalObjectManager, times(1)).addOrReplaceDatastream(any(PID.class), eq(Datastream.AUDIT), any(File.class), anyString(), any(Agent.class), anyString());
		verify(digitalObjectManager, times(1)).addOrReplaceDatastream(any(PID.class), eq(Datastream.MD_DESCRIPTIVE), any(File.class), anyString(), any(Agent.class), anyString());
		
		//Check reaction to null modified files, shouldn't do any updates
		when(uip.getModifiedFiles()).thenReturn(null);
		uipProcessor.process(uip);
		verify(digitalObjectManager, times(2)).addOrReplaceDatastream(any(PID.class), any(Datastream.class), any(File.class), anyString(), any(Agent.class), anyString());
	}
	
	@Test
	public void test() throws Exception {
		FedoraObjectUIPProcessor processor = new FedoraObjectUIPProcessor();
		
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUnpublish.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		Map<String, org.jdom.Element> originalMap = new HashMap<String, org.jdom.Element>();
		org.jdom.Element rdfElement = new org.jdom.Element("RDF", JDOMNamespaceUtil.RDF_NS);
		org.jdom.Element descElement = new org.jdom.Element("Description", JDOMNamespaceUtil.RDF_NS);
		rdfElement.addContent(descElement);
		org.jdom.Element relElement = new org.jdom.Element(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
				JDOMNamespaceUtil.CDR_NS);
		relElement.setText("yes");
		descElement.addContent(relElement);
		relElement = new org.jdom.Element(ContentModelHelper.CDRProperty.embargoUntil.getPredicate(),
				JDOMNamespaceUtil.CDR_ACL_NS);
		relElement.setText("2013-02-01");
		descElement.addContent(relElement);
		relElement = new org.jdom.Element(ContentModelHelper.FedoraProperty.hasModel.name(),
				JDOMNamespaceUtil.FEDORA_MODEL_NS);
		relElement.setText(ContentModelHelper.Model.SIMPLE.name());
		descElement.addContent(relElement);

		originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), rdfElement);
		Map<String, org.jdom.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

		MetadataUIP uip = mock(MetadataUIP.class);
		when(uip.getPID()).thenReturn(new PID("uuid:test/ACL"));
		when(uip.getOperation()).thenReturn(UpdateOperation.REPLACE);
		when(uip.getOriginalData()).thenReturn(originalMap);
		when(uip.getModifiedData()).thenReturn(originalMap);
		when(uip.getIncomingData()).thenReturn(datastreamMap);
		when(uip.getModifiedFiles()).thenReturn(getModifiedFiles(originalMap));
		
		UIPUpdatePipeline pipeline = mock(UIPUpdatePipeline.class);
		when(pipeline.processUIP(any(UpdateInformationPackage.class))).thenReturn(uip);
		processor.setPipeline(pipeline);
		
		processor.process(uip);
	}
	
	public Map<String, File> getModifiedFiles(Map<String, org.jdom.Element> modifiedData) {
		Map<String, File> modifiedFiles = new HashMap<String, File>();
		for (java.util.Map.Entry<String, ?> modified : modifiedData.entrySet()) {
			Element modifiedElement = (Element)modified.getValue();
			try {
				File temp = ClientUtils.writeXMLToTempFile(modifiedElement);
				modifiedFiles.put(modified.getKey(), temp);
			} catch (IOException e) {
				System.err.println("Failed to create temp file" + e);
			}
		}
		return modifiedFiles;
	}
}
