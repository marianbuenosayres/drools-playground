package com.plugtree.haksession.registries;

import java.util.List;

public class RuleExecution {
	
	private final String ruleName;
	private final List<Object> facts;
	
	public RuleExecution(String ruleName, List<Object> facts) {
		super();
		this.ruleName = ruleName;
		this.facts = facts;
	}
	
	public List<Object> getFacts() {
		return facts;
	}
	
	public String getRuleName() {
		return ruleName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RuleExecution)) {
			return false;
		}
		RuleExecution other = (RuleExecution) obj;
		if (ruleName == null) {
			if (other.getRuleName() != null) {
				return false;
			}
		} else if (!ruleName.equals(other.getRuleName())) {
			return false;
		}
		if (facts == null) {
			if (other.getFacts() != null) {
				return false;
			}
		} else if (other.getFacts() != null) {
			for (Object fact : facts) {
				if (!other.getFacts().contains(fact)) {
					return false;
				}
			}
			for (Object fact : other.getFacts()) {
				if (!facts.contains(fact)) {
					return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}
}
