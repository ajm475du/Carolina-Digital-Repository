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
package edu.unc.lib.dl.search.solr.model;

import org.apache.solr.client.solrj.response.FacetField;

import edu.unc.lib.dl.search.solr.exception.InvalidFacetException;

public class RoleGroupFacet extends GenericFacet {

	private String roleName;
	private String roleUri;
	private String groupName;
	
	public RoleGroupFacet(String fieldName, FacetField.Count countObject){
		super(fieldName, countObject);
		this.setFieldName(fieldName);
		 
		if (countObject != null && countObject.getName() != null)
			this.setValue(countObject.getName());
	}
	
	public RoleGroupFacet(String fieldName, String facetValue){
		super(fieldName, facetValue);
		this.setFieldName(fieldName);
		if (facetValue != null)
			this.setValue(facetValue);
	}
	
	public RoleGroupFacet(RoleGroupFacet facet) {
		super(facet);
		this.roleName = facet.roleName;
		this.roleUri = facet.roleUri;
		this.groupName = facet.groupName;
	}
	
	public void setValue(String value) {
		this.value = value;
		if (this.value != null) {
			String[] components = this.value.split("\\|");
			if (components.length < 2)
				throw new InvalidFacetException("Facet value " + value + " was invalid, it must contain a role URI and a group.");
			this.roleUri = components[0];
			try {
				this.roleName = this.roleUri.split("#")[1];
			} catch (IndexOutOfBoundsException e) {
				this.roleName = this.roleUri;
			}
			this.groupName = components[1];
			this.displayValue = this.groupName + " as " + this.roleName;
		}
	}
}
