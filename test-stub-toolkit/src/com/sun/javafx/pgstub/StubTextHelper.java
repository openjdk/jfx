/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * StubTextHelper.fx
 */

package com.sun.javafx.pgstub;

import javafx.scene.Scene;
import javafx.scene.text.Text;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.tk.TextHelper;

/**
 * @author Jan
 */
public class StubTextHelper extends TextHelper {

    private Text text;
    public StubTextHelper(Text text) {
        this.text = text;
    }

    @Override
    public BaseBounds computeBounds(BaseBounds bounds, BaseTransform tx) {
        Scene.impl_setAllowPGAccess(true);
        StubText ng = (StubText) text.impl_getPGNode();
        text.impl_syncPGNodeDirect();
        Scene.impl_setAllowPGAccess(false);
        return tx.transform(computeLayoutBounds(bounds), new RectBounds());
    }

    @Override
    public BaseBounds computeLayoutBounds(BaseBounds bounds) {
        Scene.impl_setAllowPGAccess(true);
        StubText ng = (StubText) text.impl_getPGNode();
        text.impl_syncPGNodeDirect();
        Scene.impl_setAllowPGAccess(false);
        return ng.computeLayoutBounds((RectBounds)bounds);
    }

    @Override
    public Object getCaretShape(int charIndex, boolean isLeading) {
        return null;
    }

    @Override
    public Object getSelectionShape() {
        return null;
    }

    @Override
    public Object getRangeShape(int start, int end) {
        return null;
    }

    @Override
    public Object getUnderlineShape(int start, int end) {
        return null;
    }

    @Override
    public Object getShape() {
        return null;
    }

    @Override
    public Object getHitInfo(float localX, float localY) {
        Scene.impl_setAllowPGAccess(true);
        StubText ng = (StubText) text.impl_getPGNode();
        text.impl_syncPGNodeDirect();
        Scene.impl_setAllowPGAccess(false);
        return ng.getHitInfo(localX, localY);
    }

    @Override
    public boolean contains(float localX, float localY) {
        return false;
    }
}
