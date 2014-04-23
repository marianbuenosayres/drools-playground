package org.jbpm.form.builder.ng.server.impl;

import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.uberfire.backend.repositories.Repository;
import org.uberfire.backend.repositories.RepositoryService;

import org.uberfire.commons.services.cdi.Startup;
import org.uberfire.commons.services.cdi.StartupType;

@Startup(StartupType.BOOTSTRAP)
@ApplicationScoped
public class AppSetup {

    public AppSetup() {
    }

    private static final String PLAYGROUND_SCHEME = "git";
    private static final String PLAYGROUND_ALIAS = "wires-playground";
    private static final String PLAYGROUND_ORIGIN = "https://github.com/hernsys/demo-playground.git";
    private static final String PLAYGROUND_UID = "mock";
    private static final String PLAYGROUND_PWD = "mock";

    @Inject
    private RepositoryService repositoryService;

    @PostConstruct
    public void assertPlayground() {
        final Repository repository = repositoryService.getRepository(PLAYGROUND_ALIAS);
        if (repository == null) {
            repositoryService.createRepository(PLAYGROUND_SCHEME, PLAYGROUND_ALIAS, new HashMap<String, Object>() {
                {
                    put("origin", PLAYGROUND_ORIGIN);
                    put("username", PLAYGROUND_UID);
                    put("crypt:password", PLAYGROUND_PWD);
                }
            });
        }
    }

}


