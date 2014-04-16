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
@WorkbenchScreen(identifier = "uberFireDemo.EastFirstChild")
public class EastFirstChildPresenter {

    public interface EastFirstChildView extends UberView<EastFirstChildPresenter> {

        void displayNotification(String text);

    }

    private Constants constants = GWT.create(Constants.class);

    @Inject
    private PlaceManager placeManager;

    @Inject
    private EastFirstChildView view;


    public EastFirstChildPresenter() {
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "EastFirstChild panel";
    }

    @WorkbenchPartView
    public UberView<EastFirstChildPresenter> getView() {
        return view;
    }

    @OnOpen
    public void onOpen() {
    }


}
