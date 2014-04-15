package com.plugtree.training.demo.client.multiplePerspective;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.uberfire.workbench.events.NotificationEvent;

import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;

@Dependent
@Templated(value = "EastFirstChildViewImpl.html")
public class EastFirstChildViewImpl extends Composite implements EastFirstChildPresenter.EastFirstChildView, RequiresResize {

    private EastFirstChildPresenter presenter;

    @Inject
    private Event<NotificationEvent> notification;

    public EastFirstChildViewImpl() {
    }
    
    @Override
    public void onResize() {
    }
    
    @Override
    public void init(final EastFirstChildPresenter presenter ) {
        this.presenter = presenter;
        
    }


    @Override
    public void displayNotification( String text ) {
        notification.fire( new NotificationEvent( text ) );
    }

}
