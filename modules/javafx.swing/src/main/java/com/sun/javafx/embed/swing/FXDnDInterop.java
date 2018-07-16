/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import javafx.embed.swing.SwingNode;

public abstract class FXDnDInterop {
    public abstract Component findComponentAt(Object frame, int x, int y,
                                      boolean ignoreEnabled);

    public abstract boolean isCompEqual(Component c, Object frame);

    public abstract int convertModifiersToDropAction(int modifiers,
                                             int supportedActions);

    public abstract Object createDragSourceContext(DragGestureEvent dge);

    public abstract <T extends DragGestureRecognizer> T
        createDragGestureRecognizer(DragSource ds, Component c, int srcActions,
                DragGestureListener dgl);

    public abstract void addDropTarget(DropTarget dt, SwingNode node);

    public abstract void removeDropTarget(DropTarget dt, SwingNode node);

    public abstract void setNode(SwingNode node);
}
