package edu.unc.lib.dl.acl.util;

import org.jdom.Element;

import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class AccessControlTransformationUtil {
	/**
	 * Transforms an accessControl tag, which contains only ACL related settings, into a general RDF RELS-EXT element
	 * 
	 * @param element
	 * @return
	 */
	public static Element aclToRDF(Element element) {
		Element relsExt = new Element("RDF", JDOMNamespaceUtil.RDF_NS);
		Element description = new Element("Description", JDOMNamespaceUtil.RDF_NS);
		relsExt.addContent(description);

		String value = element.getAttributeValue("discoverable", JDOMNamespaceUtil.CDR_ACL_NS);
		if (value != null) {
			Boolean discoverable = Boolean.parseBoolean(value);
			Element relation = new Element(ContentModelHelper.CDRProperty.allowIndexing.getPredicate(),
					ContentModelHelper.CDRProperty.allowIndexing.getNamespace());
			relation.setText(discoverable ? "yes" : "no");
			description.addContent(relation);
		}

		value = element.getAttributeValue("published", JDOMNamespaceUtil.CDR_ACL_NS);
		if (value != null) {
			Boolean boolValue = Boolean.parseBoolean(value);
			Element relation = new Element(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
					ContentModelHelper.CDRProperty.isPublished.getNamespace());
			relation.setText(boolValue ? "yes" : "no");
			description.addContent(relation);
		}

		value = element.getAttributeValue("inherit", JDOMNamespaceUtil.CDR_ACL_NS);
		if (value != null) {
			Boolean boolValue = Boolean.parseBoolean(value);
			Element relation = new Element(ContentModelHelper.CDRProperty.inheritPermissions.getPredicate(),
					ContentModelHelper.CDRProperty.inheritPermissions.getNamespace());
			relation.setText(boolValue.toString());
			description.addContent(relation);
		}

		value = element.getAttributeValue("embargo-until", JDOMNamespaceUtil.CDR_ACL_NS);
		if (value != null) {
			Element relation = new Element(ContentModelHelper.CDRProperty.embargoUntil.getPredicate(),
					ContentModelHelper.CDRProperty.embargoUntil.getNamespace());
			relation.setText(value);
			description.addContent(relation);
		}

		for (Object childObject : element.getChildren()) {
			Element childElement = (Element) childObject;
			if (childElement.getNamespace().equals(JDOMNamespaceUtil.CDR_ACL_NS)) {
				String group = childElement.getAttributeValue("group", JDOMNamespaceUtil.CDR_ACL_NS);
				String role = childElement.getAttributeValue("role", JDOMNamespaceUtil.CDR_ACL_NS);

				// Validate the role is real
				UserRole userRole = UserRole.getUserRole(JDOMNamespaceUtil.CDR_ROLE_NS.getURI() + role);
				if (userRole != null) {
					Element relation = new Element(userRole.getPredicate(), JDOMNamespaceUtil.CDR_ROLE_NS);
					relation.setText(group);
					description.addContent(relation);
				}
			}
		}

		return relsExt;
	}

	/**
	 * Transforms an RDF tag representing RELS-EXT into an accessControl tag, containing only acl related relations
	 * 
	 * @param rdf
	 * @return
	 */
	public static Element rdfToACL(Element rdf) {
		Element accessControl = new Element("accessControl", JDOMNamespaceUtil.CDR_ACL_NS);
		Element description = rdf.getChild("Description", JDOMNamespaceUtil.RDF_NS);

		String relationValue = description.getChildText(ContentModelHelper.CDRProperty.allowIndexing.getPredicate(),
				JDOMNamespaceUtil.CDR_NS);
		if (relationValue != null)
			accessControl.setAttribute("discoverable", ("no".equals(relationValue)) ? "false" : "true",
					JDOMNamespaceUtil.CDR_ACL_NS);

		relationValue = description.getChildText(ContentModelHelper.CDRProperty.isPublished.getPredicate(),
				JDOMNamespaceUtil.CDR_NS);
		if (relationValue != null)
			accessControl.setAttribute("published", ("no".equals(relationValue)) ? "false" : "true",
					JDOMNamespaceUtil.CDR_ACL_NS);

		relationValue = description.getChildText(ContentModelHelper.CDRProperty.inheritPermissions.getPredicate(),
				JDOMNamespaceUtil.CDR_ACL_NS);
		if (relationValue != null)
			accessControl.setAttribute("inherit", ("no".equals(relationValue)) ? "false" : "true",
					JDOMNamespaceUtil.CDR_ACL_NS);

		relationValue = description.getChildText(ContentModelHelper.CDRProperty.embargoUntil.getPredicate(),
				JDOMNamespaceUtil.CDR_ACL_NS);
		if (relationValue != null)
			accessControl.setAttribute("embargo-until", relationValue, JDOMNamespaceUtil.CDR_ACL_NS);

		for (Object childObject : description.getChildren()) {
			Element childElement = (Element) childObject;
			if (childElement.getNamespace().equals(JDOMNamespaceUtil.CDR_ROLE_NS)) {
				String role = childElement.getName();
				String group = childElement.getTextTrim();

				Element grantElement = new Element(role, JDOMNamespaceUtil.CDR_ACL_NS);
				grantElement.setText(group);
				accessControl.addContent(grantElement);
			}
		}

		return accessControl;
	}
}
