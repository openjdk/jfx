/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>

#include "PlatformJavaClasses.h"
#include "RenderingQueue.h"

namespace WebCore {

class RenderingQueue;

// Used as PlatformImagePtr

class ImageJava : public RefCounted<ImageJava> {
public:
    static RefPtr<ImageJava> create(RefPtr<RQRef> rqoImage, RefPtr<RenderingQueue> rq, int w, int h)
    {
        return adoptRef(new ImageJava(rqoImage, rq, w, h));
    }

    FloatSize size() const { return FloatSize(m_width, m_height); }

    RefPtr<RQRef> getImage() const { return m_rqoImage; }

    RefPtr<RenderingQueue> getRenderingQueue() const { return m_rq; }

    ~ImageJava() = default;

private:
    ImageJava(RefPtr<RQRef> rqoImage, RefPtr<RenderingQueue> rq, int w, int h)
        : m_width(w), m_height(h), m_rq(rq), m_rqoImage(rqoImage)
    {}

    int m_width, m_height;
    RefPtr<RenderingQueue> m_rq;
    RefPtr<RQRef> m_rqoImage;
};

} // namespace WebCore