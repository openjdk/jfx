/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed.swing;

import java.awt.Component;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javafx.embed.swing.SwingNode;
import com.sun.javafx.embed.swing.newimpl.FXDnDInteropN;

/**
 * A utility class to connect DnD mechanism of Swing and FX.
 * It allows Swing content to use the FX machinery for performing DnD.
 */
final public class FXDnD {
    public static boolean fxAppThreadIsDispatchThread;
    private FXDnDInteropN fxdndiop;

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                fxAppThreadIsDispatchThread =
                        "true".equals(System.getProperty("javafx.embed.singleThread"));
                return null;
            }
        });

    }

    public FXDnD(SwingNode node) {
        fxdndiop = new FXDnDInteropN();
        fxdndiop.setNode(node);
    }

    public Object createDragSourceContext(DragGestureEvent dge)
            throws InvalidDnDOperationException {
        return fxdndiop.createDragSourceContext(dge);
    }

    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass,
            DragSource ds, Component c, int srcActions,
            DragGestureListener dgl)
    {
        return fxdndiop.createDragGestureRecognizer(ds, c, srcActions, dgl);
    }

    public void addDropTarget(DropTarget dt) {
        SwingNode node = fxdndiop.getNode();
        if (node != null) {
            fxdndiop.addDropTarget(dt, node);
        }
    }

    public void removeDropTarget(DropTarget dt) {
        SwingNode node = fxdndiop.getNode();
        if (node != null) {
            fxdndiop.removeDropTarget(dt, node);
        }
    }
}
