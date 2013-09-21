package com.plugtree.training.cdi;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;

@ApplicationScoped
public class CDIUserGroupCallback extends JBossUserGroupCallbackImpl {

	public CDIUserGroupCallback() {
		super(getUserGroups());
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
