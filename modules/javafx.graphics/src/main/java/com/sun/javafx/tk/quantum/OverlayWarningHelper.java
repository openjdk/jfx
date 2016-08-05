/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.GroupHelper;
import com.sun.javafx.scene.shape.TextHelper;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;

/**
 * Used to access internal methods of OverlayWarning.
 */
public class OverlayWarningHelper extends GroupHelper {

    private static final OverlayWarningHelper theInstance;
    private static OverlayWarningAccessor overlayWarningAccessor;

    static {
        theInstance = new OverlayWarningHelper();
        Utils.forceInit(OverlayWarning.class);
    }

    private static OverlayWarningHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(OverlayWarning overlayWarning) {
        setHelper(overlayWarning, getInstance());
    }

    @Override
    protected void updatePeerImpl(Node node) {
        overlayWarningAccessor.doUpdatePeer(node);
        super.updatePeerImpl(node);
    }

    @Override
    protected void markDirtyImpl(Node node, DirtyBits dirtyBit) {
        super.markDirtyImpl(node, dirtyBit);
        overlayWarningAccessor.doMarkDirty(node, dirtyBit);
    }

    public static void setOverlayWarningAccessor(final OverlayWarningAccessor newAccessor) {
        if (overlayWarningAccessor != null) {
            throw new IllegalStateException();
        }

        overlayWarningAccessor = newAccessor;
    }

    public interface OverlayWarningAccessor {
        void doMarkDirty(Node node, DirtyBits dirtyBit);
        void doUpdatePeer(Node node);
    }

}
