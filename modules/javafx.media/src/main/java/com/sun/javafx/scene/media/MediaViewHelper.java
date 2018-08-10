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

package com.sun.javafx.scene.media;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.media.MediaView;

/**
 * Used to access internal methods of MediaView.
 */
public class MediaViewHelper extends NodeHelper {

    private static final MediaViewHelper theInstance;
    private static MediaViewAccessor mediaViewAccessor;

    static {
        theInstance = new MediaViewHelper();
        Utils.forceInit(MediaView.class);
    }

    private static MediaViewHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(MediaView mediaView) {
        setHelper(mediaView, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return mediaViewAccessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node) {
        super.updatePeerImpl(node);
        mediaViewAccessor.doUpdatePeer(node);
    }

    protected void transformsChangedImpl(Node node) {
        super.transformsChangedImpl(node);
        mediaViewAccessor.doTransformsChanged(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds,
            BaseTransform tx) {
        return mediaViewAccessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY) {
        return mediaViewAccessor.doComputeContains(node, localX, localY);
    }

    public static void setMediaViewAccessor(final MediaViewAccessor newAccessor) {
        if (mediaViewAccessor != null) {
            throw new IllegalStateException();
        }

        mediaViewAccessor = newAccessor;
    }

    public interface MediaViewAccessor {
        NGNode doCreatePeer(Node node);
        void doUpdatePeer(Node node);
        void doTransformsChanged(Node node);
        boolean doComputeContains(Node node, double localX, double localY);
        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);
    }

}
