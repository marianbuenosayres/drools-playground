package com.plugtree.training.model;

public class Location {

	private String container;
	private String contained;
	
	public Location(String container, String contained) {
		this();
		this.container = container;
		this.contained = contained;
	}

	public Location() {
	}
	
	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public String getContained() {
		return contained;
	}

	public void setContained(String contained) {
		this.contained = contained;
	}
}
