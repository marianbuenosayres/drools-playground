package com.plugtree.training;

import javax.enterprise.event.Observes;

import org.jbpm.services.task.deadlines.notifications.impl.MockNotificationListener;
import org.jbpm.shared.services.impl.events.JbpmServicesEventListener;
import org.kie.internal.task.api.model.NotificationEvent;

public class MockEventListener extends JbpmServicesEventListener<NotificationEvent> {

	private final MockNotificationListener listener;
	
	public MockEventListener(MockNotificationListener listener) {
		this.listener = listener;
	}
	
	public void fire(@Observes NotificationEvent event) {
		this.listener.onNotification(event);
	}
}
