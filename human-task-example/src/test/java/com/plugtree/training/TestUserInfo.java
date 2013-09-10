package com.plugtree.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.internal.task.api.UserInfo;

public class TestUserInfo implements UserInfo {

	private Map<OrganizationalEntity, String> languages = new HashMap<OrganizationalEntity, String>();
	private Map<OrganizationalEntity, String> emails = new HashMap<OrganizationalEntity, String>();
	private DefaultUserGroupCallbackImpl callback = null;
	
	@Override
	public String getDisplayName(OrganizationalEntity entity) {
		return entity.getId();
	}

	@Override
	public Iterator<OrganizationalEntity> getMembersForGroup(Group group) {
		List<OrganizationalEntity> groups = new ArrayList<OrganizationalEntity>();
		if (callback != null) {
			Map<UserImpl, List<GroupImpl>> userGroupMapping = callback.getUserGroupMapping();
			for (Map.Entry<UserImpl, List<GroupImpl>> entry : userGroupMapping.entrySet()) {
				if (entry.getValue().contains(group)) {
					groups.add(entry.getKey());
				}
			}
		}
		return groups.iterator();
	}

	@Override
	public boolean hasEmail(Group group) {
		return emails.containsKey(group);
	}

	@Override
	public String getEmailForEntity(OrganizationalEntity entity) {
		return emails.get(entity);
	}

	@Override
	public String getLanguageForEntity(OrganizationalEntity entity) {
		return languages.get(entity);
	}

	public void setLanguages(Map<OrganizationalEntity, String> languages) {
		this.languages.clear();
		this.languages.putAll(languages);
	}

	public void setEmails(Map<OrganizationalEntity, String> emails) {
		this.emails.clear();
		this.emails.putAll(emails);
	}

	public void setCallback(DefaultUserGroupCallbackImpl callback) {
		this.callback = callback;
	}
}
