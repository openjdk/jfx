/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.content;

import com.sun.javafx.scene.NodeHelper;
import javafx.beans.WeakListener;
import javafx.collections.ListChangeListener;
import javafx.content.ContentParent;
import javafx.content.ContentParentBase;
import javafx.scene.Node;
import java.lang.ref.WeakReference;

public final class ContentParentChangedListener implements ListChangeListener<Object>, WeakListener {

    private final WeakReference<ContentParent> parent;

    public ContentParentChangedListener(ContentParent parent) {
        this.parent = new WeakReference<>(parent);
    }

    @Override
    public boolean wasGarbageCollected() {
        return parent.get() != null;
    }

    @Override
    public void onChanged(Change<?> c) {
        while (c.next()) {
            if (c.wasRemoved()) {
                for (var node : c.getRemoved()) {
                    if (node instanceof Node n) {
                        NodeHelper.setContentParent(n, null);
                    } else if (node instanceof ContentParentBase n) {
                        ContentParentBaseHelper.setContentParent(n, null);
                    }
                }
            }

            ContentParent parent = this.parent.get();

            if (parent == null) {
                c.getList().removeListener(this);
            } else if (c.wasAdded()) {
                for (var node : c.getAddedSubList()) {
                    if (node instanceof Node n) {
                        NodeHelper.setContentParent(n, parent);
                    } else if (node instanceof ContentParentBase n) {
                        ContentParentBaseHelper.setContentParent(n, parent);
                    }
                }
            }
        }
    }

}
