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
@Templated(value = "EastSecondChildViewImpl.html")
public class EastSecondChildViewImpl extends Composite implements EastSecondChildPresenter.EastSecondChildView, RequiresResize {

    private EastSecondChildPresenter presenter;

    @Inject
    private Event<NotificationEvent> notification;

    public EastSecondChildViewImpl() {
    }
    
    @Override
    public void onResize() {
    }
    
    @Override
    public void init(final EastSecondChildPresenter presenter ) {
        this.presenter = presenter;
        
    }


    @Override
    public void displayNotification( String text ) {
        notification.fire( new NotificationEvent( text ) );
    }

}
