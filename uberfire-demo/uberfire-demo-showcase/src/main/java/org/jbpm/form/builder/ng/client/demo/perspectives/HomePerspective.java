package org.jbpm.form.builder.ng.client.demo.perspectives;

import javax.enterprise.context.ApplicationScoped;

import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PanelType;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;



@ApplicationScoped
@WorkbenchPerspective(identifier = "Home Perspective", isDefault = true)
public class HomePerspective {

    @Perspective
    public PerspectiveDefinition getPerspective() {
        final PerspectiveDefinition p = new PerspectiveDefinitionImpl(PanelType.ROOT_STATIC);
        p.setName("Home Perspective");
        p.getRoot().addPart(new PartDefinitionImpl(new DefaultPlaceRequest("Home Screen")));
        p.setTransient(true);
        return p;
    }
    
    
}