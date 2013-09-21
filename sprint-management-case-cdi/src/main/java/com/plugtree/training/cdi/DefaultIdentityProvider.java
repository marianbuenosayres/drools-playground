package com.plugtree.training.cdi;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jbpm.kie.services.api.IdentityProvider;

@ApplicationScoped
public class DefaultIdentityProvider implements IdentityProvider {

	@Override
	public String getName() {
		return "Administrator";
	}

	@Override
	public List<String> getRoles() {
		return Arrays.asList("developers", "testers");
	}

}
