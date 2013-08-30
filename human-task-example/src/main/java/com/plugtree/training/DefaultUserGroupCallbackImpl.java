package com.plugtree.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.kie.internal.task.api.UserGroupCallback;

public class DefaultUserGroupCallbackImpl implements UserGroupCallback {

	private final Map<UserImpl, List<GroupImpl>> userGroupMapping = new HashMap<UserImpl, List<GroupImpl>>();

    public DefaultUserGroupCallbackImpl() {
    }

    public void addUser(String userId, String... groupIds) {
    	UserImpl user = new UserImpl(userId);
    	List<GroupImpl> groups = new ArrayList<GroupImpl>();
    	if (groupIds != null) {
    		for (String groupId : groupIds) {
    			groups.add(new GroupImpl(groupId));
    		}
    	}
    	userGroupMapping.put(user, groups);
    }
    
    public void removeUser(String userId) {
    	userGroupMapping.remove(new UserImpl(userId));
    }
    
    @Override
    public boolean existsUser(String userId) {
        Iterator<UserImpl> iter = userGroupMapping.keySet().iterator();
        while (iter.hasNext()) {
            UserImpl u = iter.next();
            if (u.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean existsGroup(String groupId) {
        Iterator<UserImpl> iter = userGroupMapping.keySet().iterator();
        while (iter.hasNext()) {
            UserImpl u = iter.next();
            List<GroupImpl> groups = userGroupMapping.get(u);
            for (GroupImpl g : groups) {
                if (g.getId().equals(groupId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getGroupsForUser(String userId, List<String> groupIds) {
        return getGroupsForUser(userId);
    }

    public List<String> getGroupsForUser(String userId) {
        Iterator<UserImpl> iter = userGroupMapping.keySet().iterator();
        while (iter.hasNext()) {
            UserImpl u = iter.next();
            if (u.getId().equals(userId)) {
                List<String> groupList = new ArrayList<String>();
                List<GroupImpl> userGroupList = userGroupMapping.get(u);
                for (GroupImpl g : userGroupList) {
                    groupList.add(g.getId());
                }
                return groupList;
            }
        }
        return null;
    }

    @Override
    public List<String> getGroupsForUser(String userId, List<String> groupIds,
            List<String> allExistingGroupIds) {
        return getGroupsForUser(userId);
    }
}
