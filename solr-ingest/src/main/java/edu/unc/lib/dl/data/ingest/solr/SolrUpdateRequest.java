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
package edu.unc.lib.dl.data.ingest.solr;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Represents a request to update an object identified by pid.
 * @author bbpennel
 */
public class SolrUpdateRequest extends UpdateNodeRequest {
	private static final long serialVersionUID = 1L;
	protected PID pid;
	protected String targetLabel;
	protected IndexingActionType action; 
	protected SolrUpdateRequest linkedRequest;
	
	public SolrUpdateRequest(String pid, IndexingActionType action) {
		this(pid, action, null, null);
	}
	
	public SolrUpdateRequest(String pid, IndexingActionType action, String messageID) {
		this(pid, action, messageID, null);
	}
	
	public SolrUpdateRequest(PID pid, IndexingActionType action, String messageID, UpdateNodeRequest parent){
		super(messageID, parent);
		if (pid == null || action == null)
			throw new IllegalArgumentException("Both a target pid and an action are required.");
		this.pid = pid;
		this.action = action;
		linkedRequest = null;
	}
	
	public SolrUpdateRequest(String pid, IndexingActionType action, String messageID, UpdateNodeRequest parent){
		this(new PID(pid), action, messageID, parent);
	}
	
	public SolrUpdateRequest(String pid, IndexingActionType action, SolrUpdateRequest linkedRequest, String messageID, UpdateNodeRequest parent){
		this(new PID(pid), action, linkedRequest, messageID, parent);
	}
	
	public SolrUpdateRequest(PID pid, IndexingActionType action, SolrUpdateRequest linkedRequest, String messageID, UpdateNodeRequest parent){
		this(pid, action, messageID, parent);
		this.setLinkedRequest(linkedRequest);
	}
	
	public PID getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = new PID(pid);
	}
	
	public IndexingActionType getUpdateAction() {
		return action;
	}

	public void setUpdateAction(IndexingActionType action) {
		this.action = action;
	}
	
	public boolean isBlocked(){
		return false;
	}
	
	public void linkedRequestEstablished(SolrUpdateRequest linkerRequest){
	}
	
	public void setLinkedRequest(SolrUpdateRequest linkedRequest){
		this.linkedRequest = linkedRequest;
		if (linkedRequest != null)
			linkedRequest.linkedRequestEstablished(this);
	}
	
	public SolrUpdateRequest getLinkedRequest(){
		return this.linkedRequest;
	}
	
	public void linkedRequestCompleted(SolrUpdateRequest completedRequest){
	}
	
	public void requestCompleted(){
		super.requestCompleted();
		if (linkedRequest != null){
			linkedRequest.linkedRequestCompleted(this);
		}
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}
	
	@Override
	public String getTargetID() {
		return pid.getPid();
	}

	@Override
	public String getTargetLabel() {
		return targetLabel;
	}

	@Override
	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}

	@Override
	public String getAction() {
		return this.action.getName();
	}

	@Override
	public String getNamespace() {
		return IndexingActionType.namespace;
	}

	@Override
	public String getQualifiedAction() {
		return this.action.getURI().toString();
	}

	@Override
	public long getTimeCreated() {
		return timeCreated;
	}
}
