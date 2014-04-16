/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.form.builder.ng.server.impl;

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jbpm.console.ng.bd.service.AdministrationService;
import org.uberfire.backend.repositories.Repository;
import org.uberfire.backend.server.config.ConfigGroup;
import org.uberfire.backend.server.config.ConfigType;
import org.uberfire.backend.server.config.ConfigurationFactory;
import org.uberfire.backend.server.config.ConfigurationService;
import org.uberfire.commons.services.cdi.ApplicationStarted;
import org.uberfire.io.FileSystemType;
import org.uberfire.io.IOService;

@Singleton
public class AppSetup {

	@Inject
	@Named("ioStrategy")
    private IOService         ioService;
	
    @Inject
    @Named("system")
    private Repository systemRepository;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private ConfigurationFactory configurationFactory;
    
    @Inject
    private AdministrationService administrationService;

    @Inject
    private Event<ApplicationStarted> applicationStartedEvent;

    private boolean done = false;
    
    @PostConstruct
    public void onStartup() {
    	if (!done) {
            done = true;

            if (ioService.getFileSystem( URI.create( systemRepository.getUri() ) ) == null) {
            	ioService.newFileSystem( URI.create( systemRepository.getUri() ), 
                        systemRepository.getEnvironment(),
                        FileSystemType.Bootstrap.BOOTSTRAP_INSTANCE
                );
            }

            administrationService.bootstrapRepository( "example", "repository1", null, "", ""); 
            administrationService.bootstrapProject( "repository1", "org.kie.example", "project1", "1.0.0-SNAPSHOT" );
            administrationService.bootstrapConfig();
            administrationService.bootstrapDeployments();
            configurationService.addConfiguration( getGlobalConfiguration() );
            
            // notify cluster service that bootstrap is completed to start synchronization
            applicationStartedEvent.fire(new ApplicationStarted());
       }
    }

    private ConfigGroup getGlobalConfiguration() {
        final ConfigGroup group = configurationFactory.newConfigGroup( ConfigType.GLOBAL, "settings", "" );
        group.addConfigItem( configurationFactory.newConfigItem( "build.enable-incremental", "true" ) );

        return group;
    }
}

