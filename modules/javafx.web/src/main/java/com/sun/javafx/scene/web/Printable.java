/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.web;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.Graphics;
import com.sun.webkit.WebPage;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCGraphicsManager;
import javafx.scene.Node;

public final class Printable extends Node {
    static {
        PrintableHelper.setPrintableAccessor(new PrintableHelper.PrintableAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Printable) node).doCreatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Printable) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((Printable) node).doComputeContains(localX, localY);
            }
        });
    }

    private final WebPage page;
    private final NGNode peer;

    public Printable(WebPage page, int pageIndex, float width) {
        this.page = page;
        peer = new Peer(pageIndex, width);
        PrintableHelper.initHelper(this);
    }

    private NGNode doCreatePeer() {
        return peer;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        return bounds;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double d, double d1) {
        return false;
    }

    private final class Peer extends NGNode {
        private final int pageIndex;
        private final float width;

        Peer(int pageIndex, float width) {
            this.pageIndex = pageIndex;
            this.width = width;
        }

        @Override protected void renderContent(Graphics g) {
            WCGraphicsContext gc = WCGraphicsManager.getGraphicsManager().
                    createGraphicsContext(g);
            page.print(gc, pageIndex, width);
        }

        @Override protected boolean hasOverlappingContents() {
            return false;
        }
    }
}
