/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "Icon.h"
#include "IntRect.h"
#include "PlatformJavaClasses.h"
#include <wtf/java/JavaRef.h>
#include "NotImplemented.h"

#include <wtf/RefPtr.h>
#include <wtf/text/WTFString.h>

#include "PlatformContextJava.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"


using namespace WebCore;

namespace WebCore {

Icon::Icon(const JLObject &jicon)
    : m_jicon(RQRef::create(jicon))
{
}

Icon::~Icon()
{
}

RefPtr<Icon> Icon::createIconForFiles(const Vector<String>&)
{
    notImplemented();
    return nullptr;
}

void Icon::paint(GraphicsContext& gc, const FloatRect& rect)
{
    gc.platformContext()->rq().freeSpace(16)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWICON
    << *m_jicon << (jint)rect.x() <<  (jint)rect.y();
}

} // namespace WebCore
