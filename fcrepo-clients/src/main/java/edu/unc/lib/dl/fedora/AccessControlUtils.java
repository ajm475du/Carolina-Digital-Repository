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
package edu.unc.lib.dl.fedora;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom.Content;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;

import edu.unc.lib.dl.util.TripleStoreQueryServiceMulgaraImpl;

public class AccessControlUtils {

	boolean initComplete = false;

	// Cache to hold UUID, map of permissions
	private Map<String, Map> permissionMap = null;

	// Cache to hold UUID, list of ancestor UUIDs
	private Map<String, List<PID>> ancestorMap = null;

	// Constants to indicate the type of resource being processed
	private int METADATA = 0;
	private int ORIGINAL = 1;
	private int DERIVATIVE = 2;

	// Strings used to check for type of resource
	private String[] metadataNames = { "MD_", "DC", "RELS-EXT", "FESLPOLICY" };
	private String[] originalNames = { "DATA_" };

	// CDR options loaded from configuration
	private String username;
	private String password;
	private String itqlEndpointURL;
	private String serverModelUri;
	private int cacheDepth;
	private int cacheLimit;
	private long cacheResetTime;
	private PID collectionsPid;

	private Properties accessControlProperties = null;

	private HashMap<String, List<String>> rolePermissions = null;

	private static final Logger LOG = LoggerFactory
			.getLogger(AccessControlUtils.class);

	private AttributeFactory attributeFactory = null;

	private edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService = null;

	public edu.unc.lib.dl.util.TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void setCacheDepth(int cacheDepth) {
		this.cacheDepth = cacheDepth;
	}

	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	public void setCacheResetTime(long cacheResetTime) {
		this.cacheResetTime = cacheResetTime;
	}

	public void setAccessControlProperties(Properties accessControlProperties) {
		this.accessControlProperties = accessControlProperties;
	}

	public void loadSettingsFromOptions(Map<String, String> options) {

		// load roles from config
		accessControlProperties = new Properties();

		String roleString = options.get("cdr.roles");
		String rolePrefix = options.get("cdr.role.prefix");
		String tripleStorePrefix = options.get("cdr.role.triplestore.prefix");

		String[] roles = roleString.split(" ");

		LOG.debug("loadSettingsFromOptions: roleString: " + roleString);
		LOG.debug("loadSettingsFromOptions: rolePrefix: " + rolePrefix);
		LOG.debug("loadSettingsFromOptions: tripleStorePrefix: "
				+ tripleStorePrefix);

		for (String role : roles) {
			LOG.debug("loadSettingsFromOptions: role: " + role);
			String permissionString = options.get(rolePrefix + role);
			accessControlProperties.put(tripleStorePrefix + role,
					permissionString);
		}

		// load caching settings and tripleStore
		username = options.get("cdr.username");
		password = options.get("cdr.password");
		itqlEndpointURL = options.get("cdr.itql.endpoint.url");
		serverModelUri = options.get("cdr.server.model.uri");
		cacheDepth = convertStringToIntOtherwiseReturnZero(options
				.get("cdr.cache.depth"));
		cacheLimit = convertStringToIntOtherwiseReturnZero(options
				.get("cdr.cache.limit"));
		cacheResetTime = convertStringToIntOtherwiseReturnZero(options
				.get("cdr.cache.reset.time.hours"));

		TripleStoreQueryServiceMulgaraImpl tripleStoreQueryService = new TripleStoreQueryServiceMulgaraImpl();
		tripleStoreQueryService.setName(username); // fedoraAdmin
		tripleStoreQueryService.setPass(password); // inst1repo
		tripleStoreQueryService.setItqlEndpointURL(itqlEndpointURL); // "http://nagin:8080/webservices/services/ItqlBeanService"
		tripleStoreQueryService.setServerModelUri(serverModelUri); // "rmi://nagin/server1#"

		if (tripleStoreQueryService == null) {
			LOG.error("tripleStore is NULL!");
		}

		this.tripleStoreQueryService = tripleStoreQueryService;

	}

	public void initForFedoraBasedAccessControl(boolean readPermissionOnly,
			AttributeFactory attributeFactory) {

		init(readPermissionOnly);

		this.attributeFactory = attributeFactory;
	}

	public void startCacheCleanupThreadForFedoraBasedAccessControl() {
		if (cacheResetTime > 0) {
			Timer timer = new Timer();

			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					clearAndPreloadCdrCaches(cacheDepth, cacheLimit,
							collectionsPid);
				}
			}, 0, cacheResetTime * 3600000);
		}
	}

	public void init(){
		init(false);
	}
	
	public void initReadPermissionOnly(){
		init(true);
	}
	
	public void init(boolean readPermissionOnly) {

		LOG.debug("init entry");

		if (initComplete) { // only initialize once

			LOG.debug("init exit; was already initialized");

			return;
		}

		// TODO: reset these every day
		permissionMap = new ConcurrentHashMap<String, Map>();
		ancestorMap = new ConcurrentHashMap<String, List<PID>>();

		if (tripleStoreQueryService == null) {
			LOG.error("tripleStoreQueryService is NULL");
		}

		collectionsPid = tripleStoreQueryService
				.fetchByRepositoryPath("/Collections");

		cachePermissionsForRoles(readPermissionOnly);

		initComplete = true;

		LOG.debug("init exit");
	}

	public void cacheCleanupForCdrBasedAccessControl() {
		clearAndPreloadCdrCaches(cacheDepth, cacheLimit, collectionsPid);
	}

	public AccessControlUtils() {
	}

	/*
	 * Needs to get all active embargoes
	 * 
	 * Needs to get role/group pairs from rels-ext
	 * 
	 * Needs to calculate access control based on no-inherit, etc.
	 * 
	 * Needs to build cache of access control
	 * 
	 * Needs to clear out cache every hour
	 * 
	 * Needs to return xml in the format described on March 14th.
	 * 
	 * 
	 * Get permissions for roles
	 * 
	 * put set of permissions for group in cache
	 * 
	 * process normally
	 */

	// Given a complete RELS-EXT, extract access control information
	private Map<String, List<String>> getAccessControlFromRelsExt(
			Map<String, List<String>> relsext) {

		LOG.debug("getAccessControlFromRelsExt entry");

		Map<String, List<String>> map = new HashMap<String, List<String>>();

		Set<String> roles = rolePermissions.keySet();

		Map<String, Set<String>> permissionGroupSets = new HashMap<String, Set<String>>();

		// need to pick out roles, noinherit, embargoes
		// <hasPatron
		// xmlns="http://cdr.unc.edu/definitions/1.0/basemodel.xml#">public</hasPatron>
		// convert roles to permissions entries

		// drop other entries

		Set<String> keys = relsext.keySet();

		Iterator<String> iterator = keys.iterator();

		while (iterator.hasNext()) {
			String key = iterator.next();

			LOG.debug("getAccessControlFromRelsExt Key: " + key);

			if (roles.contains(key)) { // found a role

				List<String> groups = relsext.get(key); // get groups for the
														// role
				List<String> permissions = rolePermissions.get(key); // get
																		// permissions
																		// for a
																		// role

				for (String permission : permissions) {

					LOG.debug("getAccessControlFromRelsExt permission: "
							+ permission);

					// need to build a cache of permissions with lists of groups
					// need to update the list of groups for each permission as
					// new roles are processed.
					if (!permissionGroupSets.containsKey(permission)) {
						Set<String> temp = new HashSet();
						permissionGroupSets.put(permission, temp);
					}

					Set<String> temp = permissionGroupSets.get(permission);

					for (String group : groups) {
						LOG.debug("getAccessControlFromRelsExt group: " + group);

						temp.add(group);
					}

					permissionGroupSets.put(permission, temp);
				}
			} else if ("http://cdr.unc.edu/definitions/acl#inheritPermissions"
					.equals(key)) {
				map.put(key, relsext.get(key));
			} else if ("http://cdr.unc.edu/definitions/acl#embargo".equals(key)) {
				map.put(key, relsext.get(key));
			}
		}

		// turn permission sets into lists

		Set permissionKeys = permissionGroupSets.keySet();

		Iterator<String> permissionIterator = permissionKeys.iterator();

		while (permissionIterator.hasNext()) {
			String permission = permissionIterator.next();

			List groups = Arrays.asList(permissionGroupSets.get(permission)
					.toArray());

			map.put(permission, groups);
		}

		if (LOG.isDebugEnabled()) {
			Set<String> mapKeys = map.keySet();

			Iterator<String> mapIterator = mapKeys.iterator();

			if (mapIterator.hasNext()) {
				LOG.debug("getAccessControlFromRelsExt info from rels-ext:");
			} else {
				LOG.debug("getAccessControlFromRelsExt no info from rels-ext");
			}

			while (mapIterator.hasNext()) {
				String permission = mapIterator.next();

				List<String> groups = map.get(permission);

				LOG.debug("getAccessControlFromRelsExt key: " + permission);

				for (String group : groups) {
					LOG.debug("getAccessControlFromRelsExt  value: " + group);
				}
			}
		}

		LOG.debug("getAccessControlFromRelsExt exit");

		return map;
	}

	private void cachePermissionsForRoles(boolean readPermissionsOnly) {

		LOG.debug("cachePermissionsForRoles entry");

		if (rolePermissions == null) {
			rolePermissions = new HashMap<String, List<String>>(16);

			Enumeration keys = accessControlProperties.keys();

			if (!keys.hasMoreElements()) {
				LOG.error("cachePermissionsForRoles: No permissions found for roles; cannot determine access control settings.");
			}

			// Extract roles and their permissions
			while (keys.hasMoreElements()) {
				String role = (String) keys.nextElement();

				String tempPermissions = (String) accessControlProperties
						.getProperty(role);
				if ((tempPermissions == null) || (tempPermissions.equals(""))) {
					LOG.error("cachePermissionsForRoles: No permissions found for role: "
							+ role);
				} else {
					String[] temp = tempPermissions.split(" ");
					if ((temp == null) || (temp.length < 1)) {
						LOG.error("cachePermissionsForRoles: No permissions found for role: "
								+ role);
					} else {
						List<String> tempList = (List<String>) (readPermissionsOnly ? new ArrayList<String>(
								1) : new ArrayList<String>(12));

						for (String permission : temp) {
							// Are we only interested in read permissions?
							if (readPermissionsOnly) {
								if ((permission != null)
										&& (permission.endsWith("Read"))) {
									tempList.add(permission);
								}
							} else // Add all permissions
							{
								tempList.add(permission);
							}
						}
						rolePermissions.put(role, tempList);
					}
				}
			}
		}

		LOG.debug("cachePermissionsForRoles exit");
	}

	/*
	 * Clear and reload the CDR-related caches for access control
	 */

	private synchronized void clearAndPreloadCdrCaches(int depth,
			int cacheLimit, PID collectionsPid) {
		LOG.debug("clearAndPreloadCdrPermissionCache entry depth: " + depth
				+ " cachelimit: " + cacheLimit);

		ancestorMap.clear();
		permissionMap.clear();

		if (depth > 0) {
			// getCollections
			List<PID> collections = tripleStoreQueryService
					.fetchChildContainers(collectionsPid);

			List<PID> parents = new ArrayList(collections.size());
			parents.addAll(collections);

			addContainersToPermissionMap(parents);

			int tempDepth = depth - 1;
			List<PID> tempContainers = new ArrayList<PID>(512);

			while (tempDepth > 0 && permissionMap.size() < cacheLimit) {

				for (PID pid : parents) {
					List<PID> containers = tripleStoreQueryService
							.fetchChildContainers(pid);
					tempContainers.addAll(containers);

					addContainersToPermissionMap(containers);
				}

				parents.clear();
				parents.addAll(tempContainers);
				tempContainers.clear();
				tempDepth = tempDepth - 1;
			}
		}

		LOG.debug("clearAndPreloadCdrPermissionCache exit");
	}

	private void addContainersToPermissionMap(List<PID> containers) {

		LOG.debug("addContainersToPermissionMap entry");

		Map<String, List<String>> map;

		for (PID pid : containers) {
			try {
				map = getAccessControlFromRelsExt(tripleStoreQueryService
						.fetchAllTriples(pid));

				permissionMap.put(pid.getPid(), map);
			} catch (NullPointerException npe) {
				// Some items in the repository will not have our access
				// control
				LOG.debug("addContainersToPermissionMap: Could not find permission for: "
						+ pid);
				LOG.debug(npe.getMessage());
			}
		}

		LOG.debug("addContainersToPermissionMap exit");
	}

	/*
	 * Given the pid, return the appropriate set of access control
	 */

	public synchronized EvaluationResult processCdrAccessControl(String pid,
			String attribute, URI type) {

		LOG.debug("processCdrAccessControl entry");

		EvaluationResult result = null;
		Map map = null;
		String datastream = null;
		String basePid = null;
		int resourceType = -1;

		Map<String, Set<String>> permissionGroupSets = new HashMap<String, Set<String>>();

		// String pid = inputPid.getPid();

		init(false); // initialize if necessary

		long processTotalTime = System.currentTimeMillis();

		enforceCacheLimit();

		LOG.debug("processCdrAccessControl pid: " + pid);

		resourceType = determineResourceType(pid, resourceType);

		// Now that we know what we're looking for, get permissions

		Set<AttributeValue> bagValues = new HashSet<AttributeValue>();

		// lookupRepositoryPathInfo/lookupRepositoryAncestorPids returns:
		// 0 Repository home, pid: admin:repository
		// 1 Collections, pid: varies
		// 2 individual collection, pid: varies
		// 3 something in a collection, pid: varies
		// access control checking will stop at 0, start at list.size() (the
		// current object)

		long repositoryPathInfoTime = System.currentTimeMillis();

		List<PID> ancestors = getListOfAncestors(pid);

		LOG.debug("processCdrAccessControl Time in tripleStore getting repository path: "
				+ (System.currentTimeMillis() - repositoryPathInfoTime)
				+ " milliseconds");

		// March backwards (upwards) through the resource and its ancestors to
		// get access
		// control

		Set<String> groups = getEmbargoGroups(ancestors);
		boolean embargo = !groups.isEmpty();

		boolean noInherit = false;

		// TODO: check boundary condition of size() == 0
		for (int i = (ancestors.size() - 1); i >= 0; i--) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("processCdrAccessControl ancestor and pid: " + i
						+ " " + ancestors.get(i).getPid());
			}

			if (noInherit) {
				break; // no need to look at further ancestors
			}

			long tripleStoreTime = System.currentTimeMillis();

			String ancestorPid = ancestors.get(i).getPid();

			map = getAncestorPermissions(map, ancestors, i, ancestorPid);

			LOG.debug("processCdrAccessControl Time in tripleStoreQuery for "
					+ ancestors.get(i).getPid() + ": "
					+ (System.currentTimeMillis() - tripleStoreTime)
					+ " milliseconds");

			// Look through the map for the requested permission
			if (map != null) {
				if (LOG.isDebugEnabled()) {
					logPermissions(map);
				}

				// Get the keys of the map
				Set keys = map.keySet();
				Object[] keyArray = keys.toArray();

				// For each entry in the permissionsMap
				for (int j = 0; j < keyArray.length; j++) {

					String foundPermission = (String) keyArray[j];

					LOG.debug("processCdrAccessControl foundPermission: "
							+ foundPermission);

					// Should we stop looking at ancestors above the current
					// ancestor?
					if (foundPermission
							.equals("http://cdr.unc.edu/definitions/acl#inheritPermissions")) {
						LOG.debug("processCdrAccessControl 'No Inherit' found");
						noInherit = true;
					} else if (foundPermission
							.equals("http://cdr.unc.edu/definitions/acl#embargo")) {
						// embargoes processed differently
					} else {

						// Only return permissions appropriate for the resource
						// type

						if ((resourceType == METADATA)
								&& (foundPermission != null)
								&& (!foundPermission.contains("Metadata"))) {
							LOG.debug("metadata if");
							continue;
						} else if ((resourceType == DERIVATIVE)
								&& (foundPermission != null)
								&& (!foundPermission.contains("Derivative"))) {
							LOG.debug("derivative if");
							continue;
						} else if ((resourceType == ORIGINAL)
								&& (foundPermission != null)
								&& (!foundPermission.contains("Original"))) {
							LOG.debug("original if");
							continue;
						}

						// need to convert groups to permissions
						// add read set of read permissions for each group to
						// xml

						List<String> groupsForPermission = (List<String>) map
								.get(foundPermission);

						if (groupsForPermission == null) {
							LOG.debug("processCdrAccessControl permissions list is NULL");
						} else {
							for (int k = 0; k < groupsForPermission.size(); k++) {
								// add groups to result
								boolean addPermission = true;

								if (embargo) {
									LOG.debug("processCdrAccessControl embargo in effect");
									String group = groupsForPermission.get(k);
									LOG.debug("processCdrAccessControl group: "
											+ group);
									if ((group != null)
											&& !groups.contains(group)) {
										LOG.debug("processCdrAccessControl "
												+ group
												+ " is not a group which can bypass the embargo");
										addPermission = false;
									}
								}

								if (addPermission) {
									LOG.debug("processCdrAccessControl adding permission for group: "
											+ groupsForPermission.get(k));
									try {

										AttributeValue attributeValue = null;
										attributeValue = attributeFactory
												.createValue(type,
														groupsForPermission
																.get(k));
										bagValues.add(attributeValue);
									} catch (Exception e) {
										LOG.error(
												"processCdrAccessControl Error creating attribute: "
														+ e.getMessage(), e);
										// continue;
									}
								}
							}
						}
					}
				}
			}
		}

		BagAttribute bag = new BagAttribute(type, bagValues);

		LOG.debug("processCdrAccessControl Total time in CDR function: "
				+ (System.currentTimeMillis() - processTotalTime)
				+ " milliseconds");

		LOG.debug("processCdrAccessControl exit");

		return new EvaluationResult(bag);

	}

	private Map getAncestorPermissions(Map map, List<PID> ancestors, int i,
			String ancestorPid) {
		// Check the permissions cache to see if it has the permissions for
		// the current UUID
		if (permissionMap.containsKey(ancestorPid)) {
			map = permissionMap.get(ancestorPid);
		} else {
			// Get the permissions for the current UUID and add them to the
			// cache with UUID as the key
			try {
				map = getAccessControlFromRelsExt(tripleStoreQueryService
						.fetchAllTriples(ancestors.get(i)));

				permissionMap.put(ancestorPid, map);
			} catch (NullPointerException npe) {
				// Some items in the repository will not have our access
				// control
				LOG.debug("getAncestorPermissions Could not find permission for: "
						+ ancestors.get(i));
				LOG.debug(npe.getMessage());
			}
		}
		return map;
	}

	private List<PID> getListOfAncestors(String pid) {
		List<PID> ancestors;

		// Check the cache to see if it has the list of ancestors for the
		// current UUID
		if (ancestorMap.containsKey(pid)) {
			ancestors = ancestorMap.get(pid);
		} else {
			// Get the list of ancestors for the current UUID and add them to
			// the cache with UUID as the key
			ancestors = tripleStoreQueryService
					.lookupRepositoryAncestorPids(new edu.unc.lib.dl.fedora.PID(
							pid));
			ancestorMap.put(pid, ancestors);
		}
		return ancestors;
	}

	private int determineResourceType(String pid, int resourceType) {
		String datastream;
		String basePid;
		// There are two cases to be handled, a UUID by itself, or a
		// UUID/datastream
		// This code determines which is which and assigns the correct type to
		// resourceType

		int index = pid.indexOf('/');

		// If the resource is of the form UUID/datastream
		if (index != -1) {
			// Strip off datastream name and save it
			datastream = pid.substring(index + 1);
			LOG.debug("datastream name: " + datastream);

			basePid = pid.substring(0, index);
			LOG.debug("basePid: " + basePid);

			// Determine type of resource
			for (int i = 0; i < metadataNames.length; i++) {
				if (datastream.startsWith(metadataNames[i])) {
					resourceType = METADATA;
					break;
				}
			}
			if (resourceType != METADATA) {
				for (int i = 0; i < originalNames.length; i++) {
					if (datastream.startsWith(originalNames[i])) {
						resourceType = ORIGINAL;
						break;
					}
				}
			} else if ((resourceType != METADATA) && (resourceType != ORIGINAL)) {
				resourceType = DERIVATIVE;
			}
		} else {
			basePid = pid;
			resourceType = METADATA;
		}

		if (resourceType == METADATA)
			LOG.debug("resourceType is METADATA");
		else if (resourceType == ORIGINAL)
			LOG.debug("resourceType is ORIGINAL");
		else if (resourceType == DERIVATIVE)
			LOG.debug("resourceType is DERIVATIVE");

		return resourceType;
	}

	/*
	 * Given the pid, return the appropriate set of access control
	 */

	public Content processCdrAccessControl(PID inputPid) {
		// EvaluationResult result = null;

		LOG.debug("processCdrAccessControl entry");

		Map map = null;

		Map<String, Set<String>> permissionGroupSets = new HashMap<String, Set<String>>();

		String pid = inputPid.getPid();

		init(true); // initialize if necessary, only record Read permissions

		long processTotalTime = System.currentTimeMillis();

		enforceCacheLimit();

		LOG.debug("processCdrAccessControl pid: " + pid);

		// lookupRepositoryPathInfo/lookupRepositoryAncestorPids returns:
		// 0 Repository home, pid: admin:repository
		// 1 Collections, pid: varies
		// 2 individual collection, pid: varies
		// 3 something in a collection, pid: varies
		// access control checking will stop at 0, start at list.size() (the
		// current object)

		long repositoryPathInfoTime = System.currentTimeMillis();

		List<PID> ancestors = getListOfAncestors(pid);

		LOG.debug("processCdrAccessControl Time in tripleStore getting repository path: "
				+ (System.currentTimeMillis() - repositoryPathInfoTime)
				+ " milliseconds");

		// March backwards (upwards) through the resource and its ancestors to
		// get access
		// control

		Set<String> groups = getEmbargoGroups(ancestors);
		boolean embargo = !groups.isEmpty();

		boolean noInherit = false;

		// TODO: check boundary condition of size() == 0
		for (int i = (ancestors.size() - 1); i >= 0; i--) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("processCdrAccessControl ancestor and pid: " + i
						+ " " + ancestors.get(i).getPid());
			}

			if (noInherit) {
				break; // no need to look at further ancestors
			}

			long tripleStoreTime = System.currentTimeMillis();

			String ancestorPid = ancestors.get(i).getPid();

			map = getAncestorPermissions(map, ancestors, i, ancestorPid);

			LOG.debug("processCdrAccessControl Time in tripleStoreQuery for "
					+ ancestors.get(i).getPid() + ": "
					+ (System.currentTimeMillis() - tripleStoreTime)
					+ " milliseconds");

			// Look through the map for the requested permission
			if (map != null) {
				if (LOG.isDebugEnabled()) {
					logPermissions(map);
				}

				// Get the keys of the map
				Set keys = map.keySet();
				Object[] keyArray = keys.toArray();

				// For each entry in the permissionsMap
				for (int j = 0; j < keyArray.length; j++) {

					String foundPermission = (String) keyArray[j];

					LOG.debug("processCdrAccessControl foundPermission: "
							+ foundPermission);

					// Should we stop looking at ancestors above the current
					// ancestor?
					if (foundPermission
							.equals("http://cdr.unc.edu/definitions/acl#inheritPermissions")) {
						LOG.debug("processCdrAccessControl 'No Inherit' found");
						noInherit = true;
					} else if (foundPermission
							.equals("http://cdr.unc.edu/definitions/acl#embargo")) {
						// embargoes processed differently
					} else {

						// Have a permission, need to write out access control
						// for it
						if (!permissionGroupSets.containsKey(foundPermission)) {
							Set<String> temp = new HashSet<String>();
							permissionGroupSets.put(foundPermission, temp);
						}

						// need to convert groups to permissions
						// add read set of read permissions for each group to
						// xml

						List<String> groupsForPermission = (List<String>) map
								.get(foundPermission);

						if (groupsForPermission == null) {
							LOG.debug("processCdrAccessControl permissions list is NULL");
						} else {
							for (String group : groupsForPermission) {
								// add groups to result
								LOG.debug("processCdrAccessControl group: "
										+ group);

								if (embargo) {
									LOG.debug("processCdrAccessControl embargo in effect");

									if ((group != null)
											&& !groups.contains(group)) {
										LOG.debug("processCdrAccessControl "
												+ group
												+ " is not a group which can bypass the embargo");
									}

									if (groups.contains(group)) {
										Set<String> temp = permissionGroupSets
												.get(foundPermission);

										temp.add(group);
										permissionGroupSets.put(
												foundPermission, temp);
									}
								} else { // no embargo
									Set<String> temp = permissionGroupSets
											.get(foundPermission);

									temp.add(group);
									permissionGroupSets.put(foundPermission,
											temp);
								}
							}
						}
					}
				}
			}
		}

		/*
		 * <permissions> <rights> <originalsRead>rla</originalsRead>
		 * <originalsRead>abc</originalsRead> </rights> </permissions>
		 */
		// turn permission sets into lists

		Set permissionKeys = permissionGroupSets.keySet();

		Iterator<String> permissionIterator = permissionKeys.iterator();

		Element permsEl = new Element("permissions");
		Element rightsEl = new Element("rights");
		permsEl.addContent(rightsEl);

		while (permissionIterator.hasNext()) {
			String permission = permissionIterator.next();
			LOG.debug("processCdrAccessControl permission element: "
					+ permission);
			List permittedGroups = Arrays.asList(permissionGroupSets.get(
					permission).toArray());

			for (Object group : permittedGroups) {
				String groupName = (String) group;

				LOG.debug("processCdrAccessControl group element: " + groupName);
				Element permissionEl = new Element(permission);
				permissionEl.addContent(groupName);
				rightsEl.addContent(permissionEl);
			}

		}

		// Element permsEl = new Element("permissions");
		// inputs.addContent(permsEl);
		// for (Map.Entry<String, List<String>> p : permissions.entrySet()) {
		// String property = p.getKey();
		// Element permEl = new Element("permission");
		// permsEl.addContent(permEl);
		// permEl.setAttribute("name", property);
		// for (String value : p.getValue()) {
		// Element valueEl = new Element("value");
		// permEl.addContent(valueEl);
		// valueEl.setText(value);
		// }
		// }

		LOG.debug("processCdrAccessControl Total time in CDR function: "
				+ (System.currentTimeMillis() - processTotalTime)
				+ " milliseconds");

		LOG.debug("processCdrAccessControl exit");
		
		return permsEl;
	}

	private Set<String> getEmbargoGroups(List<PID> ancestors) {
		Set<String> groups = new HashSet<String>();
		Map map = null;

		LOG.debug("getEmbargoGroups entry");

		long start = System.currentTimeMillis();

		// TODO: check boundary condition of size() == 0
		for (int i = (ancestors.size() - 1); i >= 0; i--) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getEmbargoGroups: ancestors " + i + " "
						+ ancestors.get(i).getPid());
			}

			long tripleStoreTime = System.currentTimeMillis();

			String ancestorPid = ancestors.get(i).getPid();

			// Check the permissions cache to see if it has the permissions for
			// the current UUID
			if (permissionMap.containsKey(ancestorPid)) {
				map = permissionMap.get(ancestorPid);
			} else {
				// Get the permissions for the current UUID and add them to the
				// cache with UUID as the key
				try {
					map = getAccessControlFromRelsExt(tripleStoreQueryService
							.fetchAllTriples(ancestors.get(i)));

					permissionMap.put(ancestorPid, map);
				} catch (NullPointerException npe) {
					// Some items in the repository will not have our access
					// control
					LOG.error("getEmbargoGroups: Could not find permission for: "
							+ ancestors.get(i));
					LOG.error(npe.getMessage());
				}
			}
			LOG.debug("getEmbargoGroups: Time in tripleStoreQuery for "
					+ ancestors.get(i).getPid() + ": "
					+ (System.currentTimeMillis() - tripleStoreTime)
					+ " milliseconds");

			// Look through the map for embargos
			if (map != null) {
				if (LOG.isDebugEnabled()) {
					logPermissions(map);
				}

				// Get the keys of the map
				Set keys = map.keySet();
				Object[] keyArray = keys.toArray();

				// For each entry in the permissionsMap
				for (int j = 0; j < keyArray.length; j++) {
					boolean embargoFound = false;

					// block permissions when they are not appropriate for the
					// content type
					String foundPermission = (String) keyArray[j];

					LOG.debug("getEmbargoGroups: foundPermission: "
							+ foundPermission);

					// Should we stop looking at ancestors above the current
					// ancestor?
					if ("http://cdr.unc.edu/definitions/acl#embargo"
							.equals(foundPermission)) {
						LOG.debug("getEmbargoGroups: found an embargo");
						embargoFound = true;
					}

					// if the requested permission equals the one in the map,
					// get the list of associated groups
					if (embargoFound) {
						List<String> list = (List<String>) map
								.get(foundPermission);

						if (list == null) {
							LOG.debug("getEmbargoGroups: list of groups for embargo is NULL");
						} else {

							for (int k = 0; k < list.size(); k++) {
								String temp = list.get(k);
								// parse embargo string by spaces: date group1
								// group2 ...
								if ((temp == null) || (temp.equals(""))) {
									LOG.debug("getEmbargoGroups: list is null/empty");
									continue;
								}

								String[] result = temp.split("\\s");

								LOG.debug("getEmbargoGroups: word count: "
										+ result.length);
								for (String word : result) {
									LOG.debug("getEmbargoGroups: list value: "
											+ word);
								}

								if (result.length < 2) {
									LOG.debug("getEmbargoGroups: list is not complete: "
											+ temp);
									continue;
								}

								// parse date. If date > today, continue
								String stringDate = result[0];
								DateFormat df = new SimpleDateFormat(
										"yyyy-MM-dd");
								Date embargo;

								try {
									embargo = df.parse(stringDate);
								} catch (ParseException e) {
									LOG.error("getEmbargoGroups: error parsing embargo date: "
											+ stringDate);
									e.printStackTrace();
									continue;
								}

								Date today = new Date();

								// if the embargo has passed
								if (!today.before(embargo)) {

									LOG.debug("getEmbargoGroups: embargo date of "
											+ embargo.toString()
											+ " has passed");
									continue;
								}

								for (int x = 1; x < result.length; x++) {
									// if active, parse groups and add to list
									// of
									// groups (de duplicate)
									LOG.debug("getEmbargoGroups: adding group to bypass embargo: "
											+ result[x]);
									groups.add(result[x]);
								}
							}
						}
					}
				}
			}
		}

		LOG.debug("getEmbargoGroupsTotal time in CDR embargo function: "
				+ (System.currentTimeMillis() - start) + " milliseconds");

		if (LOG.isDebugEnabled()) {
			for (String group : groups) {
				LOG.debug("getEmbargoGroups: returning group: " + group);
			}
		}

		LOG.debug("getEmbargoGroups exit");

		return groups;
	}

	private void enforceCacheLimit() {

		LOG.debug("enforceCacheLimit entry");

		if (permissionMap.size() >= cacheLimit) {
			LOG.debug("enforceCacheLimit: Enforcing cache limit for permissions");
			removeCacheEntries(permissionMap);
		}

		if (ancestorMap.size() >= cacheLimit) {
			LOG.debug("enforceCacheLimit: Enforcing cache limit for ancestors");
			removeCacheEntries(ancestorMap);
		}

		LOG.debug("enforceCacheEntry exit");
	}

	private void removeCacheEntries(Map map) {

		LOG.debug("removeCacheEntries entry");

		Set<String> keys = map.keySet();
		Iterator iterator = keys.iterator();

		for (int i = 0; i < 100; i++) {
			if (iterator.hasNext()) {
				permissionMap.remove(iterator.next());
			}
		}

		LOG.debug("removeCacheEntries exit");
	}

	private void logPermissions(Map map) {

		LOG.debug("logPermissions entry");

		Set keys = map.keySet();

		Object[] keyArray = keys.toArray();

		for (int i = 0; i < keyArray.length; i++) {
			LOG.debug("logPermissions: permissions: " + (String) keyArray[i]);

			List<String> list = (List<String>) map.get(keyArray[i]);

			if (list == null) {
				LOG.debug("logPermissions: permissions list is NULL");
			} else {
				for (int j = 0; j < list.size(); j++) {
					LOG.debug("logPermissions: " + list.get(j));
				}
			}
		}

		LOG.debug("logPermissions exit");
	}

	private int convertStringToIntOtherwiseReturnZero(String value) {
		int result = 0;

		try {
			result = Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}

		return result;
	}
}
