/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.null3d;

import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseTexture;
import com.sun.prism.impl.ManagedResource;
import com.sun.prism.impl.PrismTrace;
import java.nio.Buffer;

class DummyTexture extends BaseTexture  {
    static class DummyManagedResource extends ManagedResource<Object> {
        DummyManagedResource() {
            super(new Object(), DummyTexturePool.instance);
        }
    };

    DummyContext context;

    DummyTexture(DummyContext context, PixelFormat format, WrapMode wrapMode,
                 int contentWidth, int contentHeight)
    {
        this(context, format, wrapMode, contentWidth, contentHeight, false);
    }

    DummyTexture(DummyContext context, PixelFormat format, WrapMode wrapMode,
                 int contentWidth, int contentHeight, boolean isRTT)
    {
        super(new DummyManagedResource(), format, wrapMode,
              contentWidth, contentHeight,
              0, 0, contentWidth, contentHeight, false);

        this.context = context;

        if (isRTT) {
            PrismTrace.rttCreated(0, contentWidth, contentWidth, 4);
        } else {
            PrismTrace.textureCreated(0, contentWidth, contentWidth,
                                      format.getBytesPerPixelUnit());
        }
    }

    public DummyContext getContext() {
        return context;
    }

    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy, int srcw, int srch, int srcscan, boolean skipFlush) {

    }

    public void update(MediaFrame frame, boolean skipFlush) {
    }

    @Override
    protected Texture createSharedTexture(WrapMode newMode) {
        return this;
    }

}
