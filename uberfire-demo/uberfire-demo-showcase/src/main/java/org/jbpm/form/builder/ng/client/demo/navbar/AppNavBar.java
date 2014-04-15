package org.jbpm.form.builder.ng.client.demo.navbar;

import static java.lang.Integer.MAX_VALUE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.uberfire.client.workbench.Header;
import org.uberfire.client.workbench.widgets.menu.WorkbenchMenuBarPresenter;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

@ApplicationScoped
public class AppNavBar extends Composite implements Header {

    @Inject
    private WorkbenchMenuBarPresenter menuBarPresenter;

    @Override
    public Widget asWidget() {
        return menuBarPresenter.getView().asWidget();
    }

    @Override
    public int getOrder() {
        return MAX_VALUE;
    }

}
