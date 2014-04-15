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

package com.plugtree.training.demo.client.home;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.security.Identity;
import org.uberfire.security.Role;
import org.uberfire.workbench.events.NotificationEvent;

import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;

@Dependent
@Templated(value = "HomeViewImpl.html")
public class HomeViewImpl extends Composite implements HomePresenter.HomeView {

    private HomePresenter presenter;

    @Inject
    private PlaceManager placeManager;

    @Inject
    public Identity identity;
    
    @DataField
    public Image uberfireLogo;
    
    @Inject
    @DataField
    public IconAnchor messageListAnchor;
    
    @Inject
    @DataField
    public IconAnchor multiplePerspectiveAnchor;
    
    

    @Inject
    private Event<NotificationEvent> notification;

    public HomeViewImpl() {
        uberfireLogo = new Image();
    }

    @Override
    public void init( final HomePresenter presenter ) {
        this.presenter = presenter;
        String url = GWT.getHostPageBaseURL();
        List<Role> roles = identity.getRoles();
        
        uberfireLogo.setUrl( url + "images/uf-logo.png" );
        messageListAnchor.setText( "Message List (Remote Procedure Calls (RPC))" );
        multiplePerspectiveAnchor.setText( "Multiples Perspectives" );
        

        messageListAnchor.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                PlaceRequest placeRequestImpl = new DefaultPlaceRequest( "Message List" );
                placeManager.goTo( placeRequestImpl );
            }
        } );
        
        multiplePerspectiveAnchor.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                PlaceRequest placeRequestImpl = new DefaultPlaceRequest( "Multiples Perspectives" );
                placeManager.goTo( placeRequestImpl );
            }
        } );
        
    }

    @Override
    public void displayNotification( String text ) {
        notification.fire( new NotificationEvent( text ) );
    }

}
