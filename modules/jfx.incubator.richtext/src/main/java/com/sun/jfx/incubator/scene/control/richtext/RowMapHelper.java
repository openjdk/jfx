/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.function.Consumer;

import jfx.incubator.scene.control.richtext.model.ContentChange;
import jfx.incubator.scene.control.richtext.skin.RowMap;

/**
 * Manages RowMap Accessor.
 */
public class RowMapHelper {
    public interface Accessor {
        void dispose(RowMap rowMap);
        void onContentChange(RowMap rowMap, ContentChange change);
        void setOnChange(RowMap rowMap, Consumer<Boolean> callback);
    }

    private static Accessor accessor;

    public static void setAccessor(Accessor a) {
        if (accessor != null) {
            throw new IllegalStateException();
        }
        accessor = a;
    }

    public static void dispose(RowMap rowMap) {
        accessor.dispose(rowMap);
    }

    public static void onContentChange(RowMap rowMap, ContentChange change) {
        accessor.onContentChange(rowMap, change);
    }

    public static void setOnChange(RowMap rowMap, Consumer<Boolean> callback) {
        accessor.setOnChange(rowMap, callback);
    }

}
