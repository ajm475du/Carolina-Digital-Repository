package edu.unc.lib.dl.search.solr.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;

/**
 * Facet used for case insensitive facet searching. Requires two fields, a display field and a search field. The display
 * field is the "default" facet for the field.
 * 
 * @author bbpennel
 * 
 */
public class CaseInsensitiveFacet extends GenericFacet {

	private static final String SEARCH_FIELD_SUFFIX = "_LC";
	// Name of the facet field that supples the
	private String searchName;
	
	/**
	 * 
	 * @param fieldName Field name of the display field
	 * @param countObject Solr facet count object, the value coming from the display field for this facet
	 */
	public CaseInsensitiveFacet(String fieldName, FacetField.Count countObject){
		super(fieldName, countObject);
		this.searchName = this.fieldName + SEARCH_FIELD_SUFFIX; 
		if (countObject != null && countObject.getName() != null)
			this.value = countObject.getName().toLowerCase();
	}
	
	public CaseInsensitiveFacet(String fieldName, String facetValue){
		super(fieldName, facetValue);
		this.searchName = this.fieldName + SEARCH_FIELD_SUFFIX;
		if (facetValue != null)
			this.value = facetValue.toLowerCase();
	}

	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}
	
	public static void deduplicateCaseInsensitiveValues(FacetFieldObject facetFieldObject) {
		Map<String, GenericFacet> rollupMap = new LinkedHashMap<String, GenericFacet>(facetFieldObject.getValues().size());
		for (GenericFacet genericFacet: facetFieldObject.getValues()) {
			CaseInsensitiveFacet deptFacet = (CaseInsensitiveFacet) genericFacet;
			GenericFacet existingFacet = rollupMap.get(deptFacet.getSearchValue());
			if (existingFacet == null) {
				rollupMap.put(deptFacet.getSearchValue(), deptFacet);
			} else {
				existingFacet.setCount(existingFacet.getCount() + deptFacet.getCount());
			}
		}
		if (rollupMap.size() < facetFieldObject.getValues().size()) {
			facetFieldObject.setValues(rollupMap.values());
		}
	}
}
