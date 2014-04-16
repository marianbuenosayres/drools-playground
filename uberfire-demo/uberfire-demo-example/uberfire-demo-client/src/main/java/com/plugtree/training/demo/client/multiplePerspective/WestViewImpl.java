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
@Templated(value = "WestViewImpl.html")
public class WestViewImpl extends Composite implements WestPresenter.WestView, RequiresResize {

    private WestPresenter presenter;

    @Inject
    private Event<NotificationEvent> notification;

    public WestViewImpl() {
    }
    
    @Override
    public void onResize() {
    }
    
    @Override
    public void init(final WestPresenter presenter ) {
        this.presenter = presenter;
        
    }


    @Override
    public void displayNotification( String text ) {
        notification.fire( new NotificationEvent( text ) );
    }

}
