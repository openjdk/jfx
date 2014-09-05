/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.sun.javafx.scene.control.skin.TabPaneSkin;
import java.util.Iterator;
import java.util.Set;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * @treatAsPrivate
 * A temporary class that should extend TabDesignInfo and adds
 * some additional verbs for managing TabPane at design time.
 * This could potentially move to TabDesignInfo some day.
 *
 */
public class TabPaneDesignInfoX /* extends TabDesignInfo */ {


    /**
     * Returns the node representing the tab header in the TabPane skin.
     * @param tabPane
     * @param tab
     * @return 
     */
    public Node getTabNode(TabPane tabPane, Tab tab) {
        assert tabPane != null;
        assert tabPane.getTabs().contains(tab);
        
        // Looks for the sub nodes which match the .tab CSS selector
        final Set<Node> set = tabPane.lookupAll(".tab"); //NOI18N
        
        // Searches the result for the node associated to 'tab'.
        // This item has (Tab.class, tab) in its property list.
        Node result = null;
        final Iterator<Node> it = set.iterator();
        while ((result == null) && it.hasNext()) {
            Node n = it.next();
            if (n.getProperties().get(Tab.class) == tab) {
                result = n;
            }
        }

        return result;
    }
    
    /**
     * Returns the node representing the content area in the TabPane skin.
     * @param tabPane
     * @param tab
     * @return 
     */
    public Node getContentNode(TabPane tabPane) {
        assert tabPane != null;
        
        final Node result;
       
        if (tabPane.getSkin() != null) {
            assert tabPane.getSkin() instanceof TabPaneSkin;
            final TabPaneSkin tabPaneSkin = (TabPaneSkin) tabPane.getSkin();
            result = tabPaneSkin.getSelectedTabContentRegion();
        } else {
            result = null;
        }

        return result;
    }
    
    
    /**
     * Returns the node representing the pulldown menu in the TabPane skin.
     */
    public Node getControlMenuNode(TabPane tabPane) {
        assert tabPane != null;
        
        // Looks for the sub node which matches the '.control-buttons-tab' selector
        return tabPane.lookup(".control-buttons-tab"); //NOI18N
    }
    
    
    /**
     * Returns the tab below (sceneX, sceneY) if any.
     * If (sceneX, sceneY) is over a tab header, returns the matching Tab.
     * If (sceneX, sceneY) is over the tab content area, returns the Tab
     * currently selected.
     * 
     * @param tabPane a tab pane
     * @param sceneX x in scene coordinate space
     * @param sceneY y in scene coordinate space
     * @return null or the tab below (sceneX, sceneY).
     */
    public Tab lookupTab(TabPane tabPane, double sceneX, double sceneY) {
        Tab result = null;
        
        // The control menu may cover a tab header.
        // So we check first if (sceneX, sceneY) is in the control menu.
        // If yes, we return null because the control menu is considered
        // as a piece of the tab pane.
        final Node controlMenuNode = getControlMenuNode(tabPane);
        final boolean insideControlMenu;
        if (controlMenuNode == null) {
            insideControlMenu = false;
        } else {
            Point2D p = controlMenuNode.sceneToLocal(sceneX, sceneY, true /* rootScene */);
            insideControlMenu = controlMenuNode.contains(p);
        }
        
        // If not inside the control menu, then checks:
        //  1) (sceneX, sceneY) is over a tab header => returns the matching tab
        //  2) (sceneX, sceneY) is over the content area => returns the selected tab
        if (insideControlMenu == false) {
            
            // Checks the headers.
            final Iterator<Tab> it = tabPane.getTabs().iterator();
            while ((result == null) && it.hasNext()) {
                Tab tab = it.next();
                Node tabNode = getTabNode(tabPane, tab);
                assert tabNode != null;
                Point2D p = tabNode.sceneToLocal(sceneX, sceneY, true /* rootScene */);
                if (tabNode.contains(p)) {
                    result = tab;
                }
            }

            // Checks the content area
            if (result == null) {
                final Node contentNode = getContentNode(tabPane);
                if (contentNode != null) {
                    final Point2D p = contentNode.sceneToLocal(sceneX, sceneY, true /* rootScene */);
                    if (contentNode.contains(p)) {
                        result = tabPane.getSelectionModel().getSelectedItem();
                    }
                }
            }
        }
        
        
        return result;
    }

    
    public Bounds computeTabBounds(TabPane tabPane, Tab tab) {
        final Node tabNode = getTabNode(tabPane, tab);
        final Bounds b = tabNode.getLayoutBounds();

        // Convert b from tabNode local space to tabPane local space
        final Point2D min = tabPane.sceneToLocal(tabNode.localToScene(b.getMinX(), b.getMinY()));
        final Point2D max = tabPane.sceneToLocal(tabNode.localToScene(b.getMaxX(), b.getMaxY()));
        return makeBoundingBox(min, max);
    }
    
    
    public Bounds computeContentAreaBounds(TabPane tabPane) {
        final Node contentNode = getContentNode(tabPane);
        assert contentNode != null;
        final Bounds b = contentNode.getLayoutBounds();
        
        // Convert b from contentNode local space to tabPane local space
        final Point2D min = tabPane.sceneToLocal(contentNode.localToScene(b.getMinX(), b.getMinY()));
        final Point2D max = tabPane.sceneToLocal(contentNode.localToScene(b.getMaxX(), b.getMaxY()));
        return makeBoundingBox(min, max);
    }
    
    private static BoundingBox makeBoundingBox(Point2D p1, Point2D p2) {
        return new BoundingBox(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.abs(p2.getX() - p1.getX()),
                Math.abs(p2.getY() - p1.getY()));
    }
}
