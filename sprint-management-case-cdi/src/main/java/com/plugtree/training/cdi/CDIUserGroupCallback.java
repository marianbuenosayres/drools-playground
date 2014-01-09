package com.plugtree.training.cdi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.kie.internal.task.api.UserGroupCallback;

@ApplicationScoped
public class CDIUserGroupCallback implements UserGroupCallback {

	private Map<String, List<String>> groupStore = new HashMap<String, List<String>>();
	private Set<String> allgroups = new HashSet<String>();
	
	public CDIUserGroupCallback() {
		init(getUserGroups());
	}
	
	protected void init(Properties userGroups) {
		if (userGroups == null) {
			throw new IllegalArgumentException("UserGroups properties cannot be null");
		}
		List<String> groups = null;
		Iterator<Object> it = userGroups.keySet().iterator();
		
		while (it.hasNext()) {
			String userId = (String) it.next();
			
			groups = Arrays.asList(userGroups.getProperty(userId, "").split(","));
			groupStore.put(userId, groups);
			allgroups.addAll(groups);
			
		}
		
		// always add Administrator if not already present
		if (!groupStore.containsKey("Administrator")) {
			groupStore.put("Administrator", Collections.singletonList("Administrators"));
			allgroups.add("Administrators");
		}
	}

	public boolean existsUser(String userId) {
		return groupStore.containsKey(userId);
	}
	
	public boolean existsGroup(String groupId) {
	
		return allgroups.contains(groupId);
	}
	
	public List<String> getGroupsForUser(String userId, List<String> groupIds,
			List<String> allExistingGroupIds) {
		
		List<String> groups = groupStore.get(userId);
		return groups;
	}
	
	private static Properties getUserGroups() {
		Properties userGroups = new Properties();
		userGroups.put("john", "developers");
		userGroups.put("jean", "developers");
		userGroups.put("charles", "developers");
		userGroups.put("mary", "testers");
		userGroups.put("mark", "testers");
		return userGroups;
	}
}
