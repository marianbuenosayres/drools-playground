package com.plugtree.training.demo.client.perspectives;

import javax.enterprise.context.ApplicationScoped;

import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PanelType;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;

/**
 * A Perspective to show Messages
 */
@ApplicationScoped
@WorkbenchPerspective(identifier = "uberFireDemo.MessageListPerspective", isDefault = false)
public class MessageListPerspective {
    
    @Perspective
    public PerspectiveDefinition getPerspective() {
        final PerspectiveDefinition p = new PerspectiveDefinitionImpl(PanelType.ROOT_LIST);
        p.setName("My Customized Panel of Messages");
        p.getRoot().addPart(new PartDefinitionImpl(new DefaultPlaceRequest("uberFireDemo.MessageListScreen")));
        p.setTransient(true);
        return p;
    }

    @OnStartup
    public void init() {
        //Do nothing
    }
}
