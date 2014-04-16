package com.plugtree.training.demo.client.multiplePerspective;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.uberfire.workbench.events.NotificationEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;

@Dependent
@Templated(value = "EastViewImpl.html")
public class EastViewImpl extends Composite implements EastPresenter.EastView, RequiresResize {

    private EastPresenter presenter;

    @Inject
    private Event<NotificationEvent> notification;
    
    

    public EastViewImpl() {
    }
    
    
    
    @Override
    public void onResize() {
    }
    
    @Override
    public void init(final EastPresenter presenter ) {
        this.presenter = presenter;
        String url = GWT.getHostPageBaseURL();
        
    }


    @Override
    public void displayNotification( String text ) {
        notification.fire( new NotificationEvent( text ) );
    }

}
