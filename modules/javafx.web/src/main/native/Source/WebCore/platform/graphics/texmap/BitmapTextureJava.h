/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include "BitmapTexture.h"
#include "ImageBuffer.h"
#include "IntRect.h"
#include "IntSize.h"

namespace WebCore {

class GraphicsContext;

class BitmapTextureJava : public ThreadSafeRefCounted<BitmapTextureJava> {
public:
    enum Flag {
            NoFlag = 0,
            SupportsAlpha = 1 << 0,
            DepthBuffer = 1 << 1,
        };

    typedef unsigned Flags;
    static Ref<BitmapTextureJava> create() { return adoptRef(*new BitmapTextureJava); }
    IntSize size() const { return m_image->backendSize(); }
    void didReset();
    bool isValid() const { return m_image.get(); }
    inline GraphicsContext* graphicsContext() { return m_image ? &(m_image->context()) : nullptr; }
    void updateContents(NativeImage*, const IntRect&, const IntPoint&);
    void updateContents(const void*, const IntRect& target, const IntPoint& sourceOffset, int bytesPerLine);
    ImageBuffer* image() const { return m_image.get(); }
    void reset(const IntSize& size, Flags flags = 0)
    {
            m_flags = flags;
            m_contentSize = size;
            didReset();
    }
    inline IntSize contentSize() const { return m_contentSize; }

private:
    BitmapTextureJava(): m_flags(0) { }
    RefPtr<ImageBuffer> m_image;
    IntSize m_contentSize;
    Flags m_flags;
};

}
