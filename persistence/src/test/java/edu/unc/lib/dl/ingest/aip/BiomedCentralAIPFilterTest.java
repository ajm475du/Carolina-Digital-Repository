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
package edu.unc.lib.dl.ingest.aip;

import java.io.File;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jrdf.graph.Graph;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.METSPackageSIPProcessor;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class BiomedCentralAIPFilterTest extends Assert {
	private static Logger LOG = Logger.getLogger(BiomedCentralAIPFilterTest.class);

	@Resource
	private METSPackageSIPProcessor metsPackageSIPProcessor = null;
	@Resource
	private SchematronValidator schematronValidator;

	@Test
	public void xmlParseTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/biomedWithSupplements.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);

		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);

		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		Graph graph = aip.getGraph();

		PID articlePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702.pdf");
		PID aggregatePID = JRDFGraphUtil.getPIDRelationshipSubject(graph,
				ContentModelHelper.Relationship.contains.getURI(), articlePID);
		PID xmlPID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702.xml");
		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S1.PDF");
		PID s2PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S2.PDF");
		PID s3PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S3.PDF");
		assertNotNull(articlePID);
		assertNotNull(aggregatePID);
		assertNotNull(xmlPID);
		assertNotNull(s1PID);
		assertNotNull(s2PID);
		assertNotNull(s3PID);

		String defaultWebPID = JRDFGraphUtil
				.getRelationshipObjectURIs(graph, aggregatePID, ContentModelHelper.CDRProperty.defaultWebObject.getURI())
				.get(0).toString();
		assertTrue(defaultWebPID.equals(articlePID.getURI()));

		String supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s1PID));
		assertTrue("Technical assistance checklist. PDF of list of commonly provided technical assistance subjects."
				.equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s2PID));
		assertTrue("Survey. PDF of survey used for participating member (i.e., client) survey.".equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s3PID));
		assertTrue("Interview protocol. PDF of interview protocol used for staff interviews.".equals(supplementTitle));

		String allowIndexing = JRDFGraphUtil.getRelatedLiteralObject(graph, xmlPID,
				ContentModelHelper.CDRProperty.allowIndexing.getURI());
		assertTrue("no".equals(allowIndexing));

		assertTrue(aip.getDepositRecord().getPackagingSubType().equals("BiomedCentral"));

		// Make sure it passes dc filter
		DublinCoreCrosswalkFilter dcFilter = new DublinCoreCrosswalkFilter();
		dcFilter.doFilter(aip);

		// Make sure if passes mods test
		MODSValidationFilter modsFilter = new MODSValidationFilter();
		modsFilter.setSchematronValidator(schematronValidator);
		modsFilter.doFilter(aip);

		logXML(aip.getFOXMLDocument(aggregatePID));
	}

	@Test
	public void inconsistentFilenameCaseParseTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/biomedInconsistentFileCase.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);

		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);

		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		Graph graph = aip.getGraph();

		PID articlePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"bcr3152.pdf");
		PID aggregatePID = JRDFGraphUtil.getPIDRelationshipSubject(graph,
				ContentModelHelper.Relationship.contains.getURI(), articlePID);
		PID xmlPID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"bcr3152.xml");
		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"BCR3152-S1.XLSX");
		PID s2PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"BCR3152-S2.XLSX");
		PID s3PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"BCR3152-S3.PDF");
		assertNotNull(articlePID);
		assertNotNull(aggregatePID);
		assertNotNull(xmlPID);
		assertNotNull(s1PID);
		assertNotNull(s2PID);
		assertNotNull(s3PID);

		String defaultWebPID = JRDFGraphUtil
				.getRelationshipObjectURIs(graph, aggregatePID, ContentModelHelper.CDRProperty.defaultWebObject.getURI())
				.get(0).toString();
		assertTrue(defaultWebPID.equals(articlePID.getURI()));
	}

	@Test
	public void rejectedAgentTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/dspaceMets.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord("nobiomed", "notbiomed", DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		assertNull(aip.getDepositRecord().getPackagingSubType());
	}

	@Test
	public void rejectedPackageTypeTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/simple.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.SIMPLE_ZIP);
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		assertNull(aip.getDepositRecord().getPackagingSubType());
	}

	@Test
	public void noAggregateTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/simple.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_1);
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		assertNull(aip.getDepositRecord().getPackagingSubType());
	}

	@Test
	public void invalidAIPTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_1);
		AIPImpl aip = new AIPImpl(record);

		try {
			filter.doFilter(aip);
			fail();
		} catch (AIPException e) {
			// Excepted
		}

		assertNull(aip.getDepositRecord().getPackagingSubType());
	}

	@Test
	public void missingTitlesTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/biomedMissingSupplementTitle.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);

		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);

		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		Graph graph = aip.getGraph();

		PID articlePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702.pdf");
		PID aggregatePID = JRDFGraphUtil.getPIDRelationshipSubject(graph,
				ContentModelHelper.Relationship.contains.getURI(), articlePID);
		PID xmlPID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702.xml");
		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S1.PDF");
		PID s2PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S2.PDF");
		PID s3PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S3.PDF");
		assertNotNull(articlePID);
		assertNotNull(aggregatePID);
		assertNotNull(xmlPID);
		assertNotNull(s1PID);
		assertNotNull(s2PID);
		assertNotNull(s3PID);

		String defaultWebPID = JRDFGraphUtil
				.getRelationshipObjectURIs(graph, aggregatePID, ContentModelHelper.CDRProperty.defaultWebObject.getURI())
				.get(0).toString();
		assertTrue(defaultWebPID.equals(articlePID.getURI()));

		String supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s1PID));
		assertTrue("Technical assistance checklist. PDF of list of commonly provided technical assistance subjects."
				.equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s2PID));
		assertTrue("1471-2458-11-702-S2.PDF".equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s3PID));
		assertTrue("1471-2458-11-702-S3.PDF".equals(supplementTitle));

		String allowIndexing = JRDFGraphUtil.getRelatedLiteralObject(graph, xmlPID,
				ContentModelHelper.CDRProperty.allowIndexing.getURI());
		assertTrue("no".equals(allowIndexing));
	}

	@Test
	public void missingMainSupplTitle() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/biomedMissingSupplementMainTitle.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);

		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);

		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		Graph graph = aip.getGraph();

		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1471-2458-11-702-S1.PDF");

		String supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s1PID));
		LOG.debug(supplementTitle);
		assertEquals(
				("Technical assistance checklist. PDF of list of commonly provided technical assistance subjects for this exceedingly long "
						+ "title that should be more than the allowable character limit since I am simply adding more and more and more and more.  This title "
						+ "really is much longer than it has any rights to be so we will have to cut it off here.").substring(
						0, 249), supplementTitle);

	}

	@Test
	public void tooLongTitleTest() throws Exception {
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.init();

		File ingestPackage = new File("src/test/resources/biomedTooLongSupplTitle.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);

		DepositRecord record = new DepositRecord(BiomedCentralAIPFilter.BIOMED_ONYEN,
				BiomedCentralAIPFilter.BIOMED_ONYEN, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);

		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);

		filter.doFilter(aip);

		Graph graph = aip.getGraph();

		PID articlePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1745-6150-6-63.pdf");
		PID aggregatePID = JRDFGraphUtil.getPIDRelationshipSubject(graph,
				ContentModelHelper.Relationship.contains.getURI(), articlePID);
		PID xmlPID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1745-6150-6-63.xml");
		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1745-6150-6-63-S1.XLSX");
		PID s2PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(),
				"1745-6150-6-63-S2.ZIP");
		assertNotNull(articlePID);
		assertNotNull(aggregatePID);
		assertNotNull(xmlPID);
		assertNotNull(s1PID);
		assertNotNull(s2PID);

		String defaultWebPID = JRDFGraphUtil
				.getRelationshipObjectURIs(graph, aggregatePID, ContentModelHelper.CDRProperty.defaultWebObject.getURI())
				.get(0).toString();
		assertTrue(defaultWebPID.equals(articlePID.getURI()));

		String supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s1PID));
		// This one is cut off
		assertTrue("Spreadsheet with gene locus tags for all purine biosynthesizing genes identified"
				.equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s2PID));
		assertTrue("Zip file containing additional phylogenetic trees. A set of phylogenetic trees generated as described in the Methods section. Locus tags were used for archaeal proteins, while species names were used for non-archaeal proteins used for comparisons."
				.equals(supplementTitle));

		String allowIndexing = JRDFGraphUtil.getRelatedLiteralObject(graph, xmlPID,
				ContentModelHelper.CDRProperty.allowIndexing.getURI());
		assertTrue("no".equals(allowIndexing));
	}

	private void logXML(Document xml) {
		XMLOutputter outputter = new XMLOutputter();
		LOG.debug(outputter.outputString(xml));
	}

	public METSPackageSIPProcessor getMetsPackageSIPProcessor() {
		return metsPackageSIPProcessor;
	}

	public void setMetsPackageSIPProcessor(METSPackageSIPProcessor metsPackageSIPProcessor) {
		this.metsPackageSIPProcessor = metsPackageSIPProcessor;
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}
}
