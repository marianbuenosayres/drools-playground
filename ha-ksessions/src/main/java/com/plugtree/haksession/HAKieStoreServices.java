package com.plugtree.haksession;

import org.kie.api.persistence.jpa.KieStoreServices;

public interface HAKieStoreServices extends KieStoreServices {

	void setRegistry(HAKieSessionRegistry registry);

}
