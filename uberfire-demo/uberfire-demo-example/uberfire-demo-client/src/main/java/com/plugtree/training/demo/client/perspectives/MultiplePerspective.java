package com.plugtree.training.demo.client.perspectives;

import static org.uberfire.workbench.model.PanelType.ROOT_STATIC;

import javax.enterprise.context.ApplicationScoped;

import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PanelDefinition;
import org.uberfire.workbench.model.PanelType;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.Position;
import org.uberfire.workbench.model.impl.PanelDefinitionImpl;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;

/**
 * A Perspective to show Messages
 */
@ApplicationScoped
@WorkbenchPerspective(identifier = "Multiples Perspectives", isDefault = false)
public class MultiplePerspective {
    
    private static final String UBERFIRE_CENTER = "uberFireDemo.Center";

    private PerspectiveDefinition perspective;
    
    private static final int MIN_WIDTH_PANEL = 200;
    private static final int WIDTH_PANEL = 300;
    
    @Perspective
    public PerspectiveDefinition getPerspective() {
        this.perspective = new PerspectiveDefinitionImpl(ROOT_STATIC);

        final PerspectiveDefinition p = new PerspectiveDefinitionImpl(PanelType.ROOT_LIST);
        perspective.setName("Multiples Perspectives");
        
        perspective.getRoot().addPart(new PartDefinitionImpl(new DefaultPlaceRequest(UBERFIRE_CENTER)));
        
        
        this.createPanelEastWithChild(perspective, Position.EAST);
        this.drawPanel(perspective, Position.SOUTH, "uberFireDemo.South");
        this.drawPanel(perspective, Position.WEST, "uberFireDemo.West");

        perspective.setTransient(true);

        
        return perspective;
        
    }
    
    private void createPanelEastWithChild(PerspectiveDefinition p, Position position) {
        final PanelDefinition firstChild = newPanel(p, position, "uberFireDemo.EastFirstChild");
        firstChild.setHeight(150);
        firstChild.setMinHeight(80);
        
        
        final PanelDefinition secondChild = newPanel(p, position, "uberFireDemo.EastSecondChild");
        secondChild.setHeight(380);
        secondChild.setMinHeight(250);
        
        final PanelDefinition parentPanel = newPanel(p, position, "uberFireDemo.East");
        parentPanel.setHeight(180);
        parentPanel.setMinHeight(150);
        parentPanel.appendChild(Position.SOUTH, firstChild);
        parentPanel.appendChild(Position.SOUTH, secondChild);
        p.getRoot().insertChild(position, parentPanel);
        
    }
    
    private void drawPanel(PerspectiveDefinition p, Position position, String identifierPanel) {
        p.getRoot().insertChild(position, newPanel(p, position, identifierPanel));
    }
    
    private PanelDefinition newPanel(PerspectiveDefinition p, Position position, String identifierPanel) {
        final PanelDefinition panel = new PanelDefinitionImpl(PanelType.MULTI_LIST);
        panel.setWidth(WIDTH_PANEL);
        panel.setMinWidth(MIN_WIDTH_PANEL);
        panel.addPart(new PartDefinitionImpl(new DefaultPlaceRequest(identifierPanel)));
        return panel;
    }

    
}
