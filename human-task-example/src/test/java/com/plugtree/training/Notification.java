package com.plugtree.training;

public class Notification {

	private final String subject;
	private final String recipient;
	
	public Notification(String subject, String recipient) {
		super();
		this.subject = subject;
		this.recipient = recipient;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public String getRecipient() {
		return recipient;
	}
}
