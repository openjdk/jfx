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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * This class allows to pick objects at a given position in scene graph.
 * 
 * 
 */
public class Picker {
    
    private final Set<Node> excludes = new HashSet<>();
    private final List<Node> matches = new ArrayList<>();
    
    /**
     * Returns the list of nodes below (sceneX, sceneY).
     * Topmost node is at index 0.
     * Search starts from startNode.
     * 
     * @param startNode
     * @param sceneX
     * @param sceneY
     * @return the list of nodes below (sceneX, sceneY).
     */
    public List<Node> pick(Node startNode, double sceneX, double sceneY) {
        assert startNode != null;
        assert startNode.getScene() != null;
        assert Double.isNaN(sceneX) == false;
        assert Double.isNaN(sceneY) == false;
        
        final Point2D localXY = startNode.sceneToLocal(sceneX, sceneY);
        return pickInLocal(startNode, localXY.getX(), localXY.getY());
    }
    
    public List<Node> pickInLocal(Node startNode, double localX, double localY) {
        assert startNode != null;
        assert startNode.getScene() != null;
        assert Double.isNaN(localX) == false;
        assert Double.isNaN(localY) == false;
        
        this.matches.clear();
        performPick(startNode, localX, localY);
        return matches.isEmpty() ? null : Collections.unmodifiableList(matches);
    }

    public Set<Node> getExcludes() {
        return excludes;
    }
    

    private void performPick(Node startNode, double localX, double localY) {

        if ((excludes.contains(startNode) == false) && startNode.isVisible()){
            if (startNode.getLayoutBounds().contains(localX, localY)) {
                matches.add(0, startNode);
            }
        
            if (startNode instanceof Parent) {
                final Parent startParent = (Parent) startNode;
                for (Node child : startParent.getChildrenUnmodifiable()) {
                    final Point2D childLocalXY = child.parentToLocal(localX, localY);
                    // Note : childLocalXY may be null.
                    // For example, child is a Button with scaleX == 0.
                    if (childLocalXY != null) {
                        performPick(child, childLocalXY.getX(), childLocalXY.getY());
                    }
                }
            }
        }
    }
}
