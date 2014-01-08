package com.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.services.task.deadlines.NotificationListener;
import org.kie.internal.task.api.model.NotificationEvent;

public class TestNotificationListener implements NotificationListener{

	private List<Notification> eventsReceived = new ArrayList<Notification>();

	@Override
	public void onNotification(NotificationEvent event) {
		Notification myNotif = new Notification(
				event.getNotification().getSubjects().get(0).getText(),
				event.getNotification().getRecipients().get(0).getId());
		eventsReceived .add(myNotif);
	}

	public List<Notification> getEventsReceived() {
		return eventsReceived;
	}
}
