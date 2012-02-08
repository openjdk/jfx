/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.scene.traversal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;

import com.sun.javafx.Logging;
import com.sun.javafx.logging.PlatformLogger;

public class TraversalEngine {
    
    private final Algorithm algorithm;
    
    private final Parent root;
    
    private final boolean isScene;
    
    private final Bounds initialBounds;
    
    public final List<Node> registeredNodes;
    
    private final List<TraverseListener> listeners;
    
    PlatformLogger focusLogger;

    protected boolean isScene() {
        return isScene;
    }
    protected Parent getRoot() {
        return root;
    }
    
    public TraversalEngine(Parent root, boolean isScene) {
        this.root = root;
        this.isScene = isScene;
        /*
         * for 2d behaviour from TAB use :
         *    algorithm = new WeightedClosestCorner();
         * for Container sequence TAB behaviour and 2d arrow behaviour use :
         *    algorithm = new ContainerTabOrder();
         */
        algorithm = new ContainerTabOrder();
        initialBounds = new BoundingBox(0, 0, 1, 1);
        registeredNodes = new ArrayList<Node>();
        listeners = new LinkedList<TraverseListener>(); 
        focusLogger = Logging.getFocusLogger();
        if (focusLogger.isLoggable(PlatformLogger.FINER)) {
            focusLogger.finer("TraversalEngine constructor");
        }
    }
    
    public void addTraverseListener(TraverseListener listener) {
        listeners.add(listener);
    }
    
    public void removeTraverseListener(TraverseListener listener) {
        listeners.remove(listener);
    }
    
    public void reg(Node n) {
        registeredNodes.add(n);
    }

    public void unreg(Node n) {
        registeredNodes.remove(n);
    }

    public void trav(Node owner, Direction dir) {

        Node newNode = algorithm.traverse(owner, dir, this);
        if (newNode == null) {
            if (focusLogger.isLoggable(PlatformLogger.FINE)) {
                focusLogger.fine("new node is null, focus not moved");
            }
        } else {
            if (focusLogger.isLoggable(PlatformLogger.FINER)) {
                focusLogger.finer("new focus owner : "+newNode);
            }
            newNode.requestFocus();
            for (TraverseListener listener : listeners) {
                listener.onTraverse(newNode, getBounds(newNode));
            }
        }
    }    

    public int getTopLeftFocusableNode() {
        List<Node> nodes = getTargetNodes();
        final int target = 0;
        Point2D zeroZero = new Point2D(0,0);

        if (nodes.size() > 0) {
            int nodeIndex;
            Node nearestNode = nodes.get(0);
            double nearestDistance = zeroZero.distance(getBounds(nodes.get(0)).getMinX(), getBounds(nodes.get(0)).getMinY());
            double distance;

            for (nodeIndex = 1; nodeIndex < nodes.size(); nodeIndex++) {
                
                if (focusLogger.isLoggable(PlatformLogger.FINEST)) {
                    focusLogger.finest("getTopLeftFocusableNode(), distance : "+zeroZero.distance(getBounds(nodes.get(nodeIndex)).getMinX(), getBounds(nodes.get(nodeIndex)).getMinY())+" to  : "+nodes.get(nodeIndex)+". @ : "+getBounds(nodes.get(nodeIndex)).getMinX()+":"+getBounds(nodes.get(nodeIndex)).getMinY());
                }
                distance = zeroZero.distance(getBounds(nodes.get(nodeIndex)).getMinX(), getBounds(nodes.get(nodeIndex)).getMinY());
                if (nearestDistance > distance) {
                    nearestDistance = distance;
                    nearestNode = nodes.get(nodeIndex);
                }

            }
            if (focusLogger.isLoggable(PlatformLogger.FINER)) {
                focusLogger.finer("getTopLeftFocusableNode(), nearest  : "+nearestNode+", at : "+nearestDistance);
            }

            nearestNode.requestFocus();
            for (TraverseListener listener : listeners) {
                listener.onTraverse(nearestNode, getBounds(nearestNode));
            }

        }
        nodes.clear();

        return target;
    }

    /**
     * Returns nodes to be processed.
     */
    protected List<Node> getTargetNodes() {
        final List<Node> targetNodes = new ArrayList<Node>();
        for (Node n : registeredNodes) {
            if (!n.isFocused() && n.impl_isTreeVisible() && !n.isDisabled()) {
                targetNodes.add(n);
            }
        }
        return targetNodes;
    }

    /**
     * Gets a list of bounds for a list of nodes.
     */
    protected List<Bounds> getTargetBounds(List<Node> nodes) {
        final List<Bounds> targetBounds = new ArrayList<Bounds>();
        for (Node n : nodes) {
            targetBounds.add(getBounds(n));
        }
        return targetBounds;
    }
    
    /**
     * Gets the appropriate bounds for the given node, transformed into
     * the scene's or the traversal root's coordinates.
     */
    protected Bounds getBounds(Node n) {
        final Bounds bounds;
        if (n != null) {
            if (isScene) {
                bounds = n.localToScene(n.getLayoutBounds());
            } else {
                bounds = root.sceneToLocal(n.localToScene(n.getLayoutBounds()));
            }
        } else {
            bounds = initialBounds;
        }        
        return bounds;
    }
}
