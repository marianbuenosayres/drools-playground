package com.plugtree.haksession;

import java.util.List;

import org.kie.api.runtime.KieSession;

public interface HAKieSessionRegistry {

	/**
	 * Check if a rule has already been fired by another node.
	 * @param ruleName The rule to check if it was fired.
	 * @param facts The objects that could have fired the rule.
	 * @return true if it has already been fired by another node, false otherwise
	 */
	boolean hasFiredRuleForObjects(String ruleName, List<Object> facts);

	/**
	 * Creates a record that this particular node has fired a rule, to
	 * prevent other nodes from firing it.
	 * @param ruleName The rule fired
	 * @param facts The objects that fired the rule.
	 */
	void ruleFiredForObjects(String ruleName, List<Object> facts);
	
	/**
	 * Insert a fact in the other nodes.
	 * @param fact the fact to be inserted.
	 */
	void factInserted(Object fact);
	
	/**
	 * Retract a fact from the other nodes.
	 * @param fact The fact to be retracted.
	 */
	void factRetracted(Object fact);
	
	/**
	 * Update a fact from the other nodes.
	 * @param oldFact The fact as it exists in other nodes.
	 * @param newFact The fact as it should be updated.
	 */
	void factUpdated(Object oldFact, Object newFact);

	/**
	 * Stores the KieSession in the registry.
	 * @param ksession the session to keep track of.
	 */
	void setKieSession(KieSession ksession);
}
