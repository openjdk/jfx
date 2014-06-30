/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui;

import static javafx.scene.accessibility.Attribute.PARENT;
import static javafx.scene.accessibility.Attribute.ROLE;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneHelper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.accessibility.Action;
import javafx.scene.accessibility.Attribute;
import javafx.scene.accessibility.Role;

public abstract class Accessible {

    private EventHandler eventHandler;
    private View view;

    public static class EventHandler {

        /**
         * This method is called by the AT to request the value for the given attribute.
         *
         * @see Attribute
         * @param attribute the requested attribute
         * @param parameters optional list of parameters
         * @return the value for the requested attribute
         */
        public Object getAttribute(Attribute attribute, Object... parameters) {
            return null;
        }

        /**
         * This method is called by the AT to indicate the accessible to execute
         * the given action.
         *
         * @see Action
         * @param action the action to execute
         * @param parameters optional list of parameters
         */
        public void executeAction(Action action, Object... parameters) {
        }
    }

    public EventHandler getEventHandler() {
        return this.eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void dispose() {
        eventHandler = null;
        view = null;
    }

    public boolean isDisposed() {
        return eventHandler == null;
    }

    public String toString() {
         return getClass().getSimpleName() + " (" + eventHandler + ")";
    }

    protected boolean isIgnored() {
        Role role = (Role)getAttribute(ROLE);
        if (role == null) return true;
        return role == Role.NODE || role == Role.PARENT;
    }

    protected Accessible getAccessible(Scene scene) {
        if (scene == null) return null;
        return SceneHelper.getAccessible(scene);
    }

    protected Accessible getAccessible(Node node) {
        if (node == null) return null;
        return NodeHelper.getAccessible(node);
    }

    protected long getNativeAccessible(Node node) {
        if (node == null) return 0L;
        Accessible acc = getAccessible(node);
        if (acc == null) return 0L;
        return acc.getNativeAccessible();
    }

    protected Node getContainerNode(Node node, Role targetRole) {
        while (node != null) {
            Accessible acc = getAccessible(node);
            Role role = (Role)acc.getAttribute(ROLE);
            if (role == targetRole) return node;
            node = (Node)acc.getAttribute(PARENT);
        }
        return null;
    }

    protected Node getContainerNode(Role targetRole) {
        return getContainerNode((Node)getAttribute(PARENT), targetRole);
    }

    public Object getAttribute(Attribute attribute, Object... parameters) {
        Object result = eventHandler.getAttribute(attribute, parameters);
        if (result != null) {
            Class<?> clazz = attribute.getReturnType();
            if (clazz != null) {
                try {
                    clazz.cast(result);
                } catch (Exception e) {
                    String msg = "The expected return type for the " + attribute +
                                 " attribute is " + clazz.getSimpleName() +
                                 " but found " + result.getClass().getSimpleName();
                    System.err.println(msg);
                    return null; //Fail no exception
                }
            }
        }
        return result;
    }

    public void executeAction(Action action, Object... parameters) {
        eventHandler.executeAction(action, parameters);
    }

    /**
     * This method is called by Accessible to notify the AT that
     * the value for the given attribute has changed.
     *
     * @see Attribute
     * @param notification the attribute which value has changed
     */
    public abstract void sendNotification(Attribute notification);

    protected abstract long getNativeAccessible(); 
}
