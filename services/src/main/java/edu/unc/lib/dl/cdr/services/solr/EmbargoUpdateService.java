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
package edu.unc.lib.dl.cdr.services.solr;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.fedora.PID;

/**
 * Service which determines items that have exited their embargo period within the last
 * window period.
 * @author bbpennel
 *
 */
public class EmbargoUpdateService extends AbstractSolrObjectEnhancementService {
	private static final Logger LOG = LoggerFactory.getLogger(EmbargoUpdateService.class);
	
	private Integer windowSizeHours;
	
	public EmbargoUpdateService(){
		windowSizeHours = 24;
	}
	
	public void updateEmbargoes(){
		List<PID> candidates = this.findCandidateObjects(-1);
		if (candidates != null){
			for (PID candidate: candidates){
				getMessageDirector().direct(new PIDMessage(candidate.getPid(), 
						SolrUpdateAction.namespace, SolrUpdateAction.RECURSIVE_ADD.getName()));
			}
		}	
	}
	
	@Override
	public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<PID> findCandidateObjects(int maxResults) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String windowEnd = formatter.format(calendar.getTime());
		
		calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - windowSizeHours);
		String windowStart = formatter.format(calendar.getTime());
		
		String query = null;
		try {
			// replace model URI and date tokens
			query = super.readFileAsString("embargo-update-candidates.sparql");
			query = String.format(query, this.getTripleStoreQueryService().getResourceIndexModelUri(), windowStart, windowEnd);
			
			List<PID> expiringEmbargoes = new ArrayList<PID>();
			List<Map> bindings = (List<Map>) ((Map) this.getTripleStoreQueryService().sendSPARQL(query).get("results")).get("bindings");
			for (Map binding: bindings){
				expiringEmbargoes.add(new PID((String) ((Map) binding.get("pid")).get("value")));
			}
			return expiringEmbargoes;
		} catch (IOException e) {
			LOG.error("Failed to retrieve candidates.", e);
		}
		
		return null;
	}

	@Override
	public Enhancement<Element> getEnhancement(PIDMessage pid) {
		return null;
	}
	
	@Override
	public boolean prefilterMessage(PIDMessage pid) throws EnhancementException {
		return false;
	}

	@Override
	public boolean isApplicable(PIDMessage pid) {
		return true;
	}

	@Override
	public boolean isStale(PID pid) {
		return false;
	}

	public Integer getWindowSizeHours() {
		return windowSizeHours;
	}

	public void setWindowSizeHours(Integer windowSizeHours) {
		this.windowSizeHours = windowSizeHours;
	}

}
