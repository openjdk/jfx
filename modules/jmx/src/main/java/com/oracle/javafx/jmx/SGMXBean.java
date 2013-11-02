/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.javafx.jmx;

/**
 * The <code>SGMXBean</code> represents a standard JMX interface to the JavaFX application.
 * The interface is supposed to be used by tools (debugger) and provides functionality
 * to obtain Scene-graph related information and information about nodes.
 */
public interface SGMXBean {

    /**
     * Pauses the Scene-graph which means it pause all animations, media players, etc.
     * To resume the Scene-graph to normal operation call {@link #resume}.
     *
     * If the Scene-graph is already "PAUSED" then this method has no effect.
     */
    void pause();

    /**
     * Resumes the previously paused Scene-graph into the normal operation.
     *
     * If the Scene-graph is running normally (not "PAUSED") then this method has no effect.
     */
    void resume();

    /**
     * Produces single JavaFX pulse and pauses the Scene-graph again. It is
     * similar to STEP feature in debuggers.
     *
     * The Scene-graph should be already "PAUSED" (by calling {@link #pause()} method)
     * prior to calling this function otherwise the {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    void step() throws IllegalStateException;

    /**
     * Returns the list of JavaFX windows. Each window is identified by unique
     * number identifier. The identifier is used as an input to
     * {@link #getSGTree(int)} method.
     *
     * The {@link #pause()} method should be called prior to calling this method.
     * Otherwise the {@link IllegalStateException} is thrown.
     *
     * The result is in the format of JSON string.
     *
     * @return the list of all JavaFX windows in JSON format
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    String getWindows() throws IllegalStateException;

    /**
     * Returns the Scene-graph hierarchy in a simple tree-like model for
     * given window. Every node is identified by unique number identifier.
     * The identifier is used to query node's properties with methods like
     * {@link #getCSSInfo(int)}.
     *
     * The {@link #pause()} method should be called prior to calling this method.
     * Otherwise the {@link IllegalStateException} is thrown.
     *
     * The result is in the format of JSON string.
     *
     * @param windowId unique window identifier obtained by {@link #getWindows()}
     * @return the simple tree-like model of the Scene-graph in JSON format
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    String getSGTree(int windowId) throws IllegalStateException;

    /**
     * Retrieves the CSS information about the particular node.
     *
     * The {@link #pause()} method should be called prior to calling this method.
     * Otherwise the {@link IllegalStateException} is thrown.
     *
     * @param nodeId node identifier obtained by {@link #getSGTree(int)}
     * @return list of key-value pairs holding CSS information, result is in JSON format
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    String getCSSInfo(int nodeId) throws IllegalStateException;

    /**
     * Retrieves the bounds information about the particular node. Bounds are in scene's
     * coordinate system.
     *
     * @param nodeId node identifier obtained by {@link #getSGTree(int)}
     * @return bounds information (x, y, width and height) for given node in scene's coordinate system,
     * result is in JSON format
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    String getBounds(int nodeId) throws IllegalStateException;

    /**
     * Adds the node with the nodeId to the list of nodes that are to be
     * highlighted in the scene. The nodeId is to be retrieved using
     * the {@link #getSGTree(int)} method.
     *
     * @param nodeId the id of the node to be highlighted
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    void addHighlightedNode(int nodeId) throws IllegalStateException;

    /**
     * Removes the nodeId node from the list of nodes that are to be
     * highlighted in the scene. The nodeId is to be retrieved using
     * the {@link #getSGTree(int)} method.
     *
     * @param nodeId the id of the node to be removed
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    void removeHighlightedNode(int nodeId) throws IllegalStateException;

    /**
     * Adds the specified region to the list of regions to be highlighted
     * in the scene. The windowId is to be retrieved using the
     * {@link #getWindows()} method.
     *
     * @param windowId unique window identifier obtained by {@link #getWindows()}
     * @param x x coordinate of the region
     * @param y y coordinate of the region
     * @param w width of the region
     * @param h height of the region
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    void addHighlightedRegion(int windowId, double x, double y, double w, double h)
        throws IllegalStateException;

    /**
     * Removes the specified region from the list of regions to be highlighted
     * in the scene. The windowId is to be retrieved using the
     * {@link #getWindows()} method.
     *
     * @param windowId unique window identifier obtained by {@link #getWindows()}
     * @param x x coordinate of the region
     * @param y y coordinate of the region
     * @param w width of the region
     * @param h height of the region
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    void removeHighlightedRegion(int windowId, double x, double y, double w, double h)
        throws IllegalStateException;

    /**
     * Makes a screen-shot of the selected node in the scene's coordinates
     * and stores it into a temporary file in the PNG format.
     * Absolute path to this file is returned.
     *
     * The {@link #pause()} method should be called prior to calling this method.
     * Otherwise the {@link IllegalStateException} is thrown.
     *
     * @param nodeId node identifier obtained by {@link #getSGTree(int)}
     * @return the absolute path to the PNG file with the image
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    String makeScreenShot(int nodeId) throws IllegalStateException;

    /**
     * Makes a screen-shot of the specified region in the scene's coordinates
     * and stores it into a temporary file in the PNG format.
     * Absolute path to this file is returned.
     *
     * The {@link #pause()} method should be called prior to calling this method.
     * Otherwise the {@link IllegalStateException} is thrown.
     *
     * @param windowId unique window identifier obtained by {@link #getWindows()}
     * @param x x coordinate of the region
     * @param y y coordinate of the region
     * @param w width of the region
     * @param h height of the region
     * @return the absolute path to the PNG file with the image
     * @throws IllegalStateException when Scene-graph is not "PAUSED"
     */
    String makeScreenShot(int windowId, double x, double y, double w, double h)
        throws IllegalStateException;

}
