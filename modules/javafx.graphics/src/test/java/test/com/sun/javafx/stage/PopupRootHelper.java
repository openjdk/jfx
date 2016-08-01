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

package test.com.sun.javafx.stage;

import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.util.Utils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import test.javafx.stage.PopupTest.PopupRoot;

public class PopupRootHelper extends ParentHelper {
    private static final PopupRootHelper theInstance;
    private static PopupRootAccessor popupRootAccessor;

    static {
        theInstance = new PopupRootHelper();
        Utils.forceInit(PopupRoot.class);
    }

    private static PopupRootHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(PopupRoot popupRoot) {
        setHelper(popupRoot, getInstance());
    }

    @Override
    protected Bounds computeLayoutBoundsImpl(Node node) {
        return popupRootAccessor.doComputeLayoutBounds(node);
    }

    public static void setPopupRootAccessor(final PopupRootAccessor newAccessor) {
        if (popupRootAccessor != null) {
            throw new IllegalStateException();
        }

        popupRootAccessor = newAccessor;
    }

    public interface PopupRootAccessor {
        Bounds doComputeLayoutBounds(Node node);
    }

}
