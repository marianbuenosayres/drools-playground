package org.jbpm.form.builder.ng.server.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.uberfire.backend.server.IOWatchServiceNonDotImpl;
import org.uberfire.backend.server.config.ConfigurationService;
import org.uberfire.backend.server.io.IOSecurityAuth;
import org.uberfire.backend.server.io.IOSecurityAuthz;
import org.uberfire.commons.cluster.ClusterServiceFactory;
import org.uberfire.commons.services.cdi.Startup;
import org.uberfire.commons.services.cdi.StartupType;
import org.uberfire.io.IOService;
import org.uberfire.io.impl.IOServiceDotFileImpl;
import org.uberfire.io.impl.cluster.IOServiceClusterImpl;
import org.uberfire.security.auth.AuthenticationManager;
import org.uberfire.security.authz.AuthorizationManager;
import org.uberfire.security.impl.authz.RuntimeAuthorizationManager;
import org.uberfire.security.server.cdi.SecurityFactory;

@Startup(StartupType.BOOTSTRAP)
@ApplicationScoped
public class AppServiceProvider {

	@Inject
    @IOSecurityAuth
    private AuthenticationManager authenticationManager;

    @Inject
    @IOSecurityAuthz
    private AuthorizationManager authorizationManager;

    @Inject
    private IOWatchServiceNonDotImpl watchService;

	@Inject
    @Named("clusterServiceFactory")
    private ClusterServiceFactory clusterServiceFactory;
	
	@Inject ConfigurationService configurationService;

    private IOService ioService;
    
    @ApplicationScoped
	@Produces
	@Named("ioStrategy")
	public IOService getIoService() {
        System.out.println("********************* AppServiceProvider");
        configurationService.toString(); // this line ensures the ConfigurationService bean is really instantiated. do not remove!
        
		if (ioService == null) {
	    	SecurityFactory.setAuthzManager( new RuntimeAuthorizationManager() );
	        if ( clusterServiceFactory == null ) {
	            ioService = new IOServiceDotFileImpl( watchService );
	        } else {
	            ioService = new IOServiceClusterImpl( new IOServiceDotFileImpl( watchService ), clusterServiceFactory, false );
	        }
	        ioService.setAuthenticationManager( authenticationManager );
	        ioService.setAuthorizationManager( authorizationManager );
		}
        return ioService;
	}
}
