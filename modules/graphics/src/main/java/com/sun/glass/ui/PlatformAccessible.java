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

import static com.sun.javafx.scene.accessibility.Attribute.PARENT;
import static com.sun.javafx.scene.accessibility.Attribute.ROLE;
import javafx.scene.Node;
import com.sun.javafx.scene.accessibility.Accessible;
import com.sun.javafx.scene.accessibility.Action;
import com.sun.javafx.scene.accessibility.Attribute;
import com.sun.javafx.scene.accessibility.Role;

public abstract class PlatformAccessible {

    private Accessible accessible;
    private View view;

    protected PlatformAccessible(Accessible accessible) {
        this.accessible = accessible;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void dispose() {
        if (accessible != null) accessible = null;
        view = null;
    }

    protected boolean isDisposed() {
        return accessible == null;
    }

    public String toString() {
         return getClass().getSimpleName() + " (" + accessible + ")";
    }

    protected boolean isIgnored() {
        Role role = (Role)getAttribute(ROLE);
        if (role == null) return true;
        return role == Role.NODE || role == Role.PARENT;
    }

    @SuppressWarnings("deprecation")
    protected long getAccessible(Node node) {
        if (node == null) return 0L;
        Accessible acc = null;//node.getAccessible();
        if (acc == null) return 0L;
        PlatformAccessible pAcc = acc.impl_getDelegate();
        return pAcc != null ? pAcc.getNativeAccessible() : 0;
    }

    protected Node getContainerNode(Node node, Role targetRole) {
        while (node != null) {
            Accessible acc = null;//node.getAccessible();
            Role role = (Role)acc.getAttribute(ROLE);
            if (role == targetRole) return node;
            node = (Node)acc.getAttribute(PARENT);
        }
        return null;
    }

    protected Node getContainerNode(Role targetRole) {
        return getContainerNode((Node)getAttribute(PARENT), targetRole);
    }

    protected Object getAttribute(Attribute attribute, Object... parameters) {
        Object result = accessible.getAttribute(attribute, parameters);
        if (result != null) {
            Class<?> clazz = attribute.getReturnType();
            if (clazz != null) {
                try {
                    clazz.cast(result);
                } catch (Exception e) {
                    String msg = "The expected return type for the " + attribute +
                                 " attribute is " + clazz.getSimpleName() +
                                 " but found " + result.getClass().getSimpleName();
//                    throw new IllegalArgumentException(msg);
                    System.err.println(msg);
                    return null; //Fail no exception
                }
            }
        }
        return result;
    }

    protected void executeAction(Action action, Object... parameters) {
        accessible.executeAction(action, parameters);
    }

    public abstract void sendNotification(Attribute notification);

    protected abstract long getNativeAccessible(); 
}
