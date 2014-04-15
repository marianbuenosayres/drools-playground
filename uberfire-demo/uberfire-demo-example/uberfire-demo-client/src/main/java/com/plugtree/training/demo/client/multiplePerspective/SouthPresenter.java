package com.plugtree.training.demo.client.multiplePerspective;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberView;
import org.uberfire.lifecycle.OnOpen;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.Menus;

import com.google.gwt.core.client.GWT;
import com.plugtree.training.demo.client.i18n.Constants;

@Dependent
@WorkbenchScreen(identifier = "uberFireDemo.South")
public class SouthPresenter {

    public interface SouthView extends UberView<SouthPresenter> {

        void displayNotification(String text);

    }

    private Constants constants = GWT.create(Constants.class);

    @Inject
    private PlaceManager placeManager;

    @Inject
    private SouthView view;


    public SouthPresenter() {
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "South panel";
    }

    @WorkbenchPartView
    public UberView<SouthPresenter> getView() {
        return view;
    }

    @OnOpen
    public void onOpen() {
    }


}
