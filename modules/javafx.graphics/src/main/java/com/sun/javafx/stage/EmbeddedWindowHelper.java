/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import com.sun.javafx.util.Utils;
import javafx.stage.Window;


/**
 * Used to access internal window methods.
 */
public class EmbeddedWindowHelper extends WindowHelper {
    private static final EmbeddedWindowHelper theInstance;
    private static EmbeddedWindowAccessor embeddedWindowAccessor;

    static {
        theInstance = new EmbeddedWindowHelper();
        Utils.forceInit(EmbeddedWindow.class);
    }

    private static WindowHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(EmbeddedWindow embeddedWindow) {
        setHelper(embeddedWindow, getInstance());
    }

    @Override
    protected void visibleChangingImpl(Window window, boolean visible) {
        super.visibleChangingImpl(window, visible);
        embeddedWindowAccessor.doVisibleChanging(window, visible);
    }

    public static void setEmbeddedWindowAccessor(EmbeddedWindowAccessor newAccessor) {
        if (embeddedWindowAccessor != null) {
            throw new IllegalStateException();
        }

        embeddedWindowAccessor = newAccessor;
    }

    public interface EmbeddedWindowAccessor {
        void doVisibleChanging(Window window, boolean visible);
    }
}
