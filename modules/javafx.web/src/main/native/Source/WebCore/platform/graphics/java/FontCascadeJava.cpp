/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include <wkj_constants.h>

#include "Font.h"
#include "GlyphBuffer.h"
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"
#include "PlatformContextJava.h"
#include "RenderingQueue.h"
#include "WKJPlatformJava.h"

#include <wtf/Vector.h>

namespace WebCore {

void FontCascade::drawGlyphs(GraphicsContext& context, const Font& font, std::span<const GlyphBufferGlyph> glyphs, std::span<const GlyphBufferAdvance> advances,
const FloatPoint& point, FontSmoothingMode)
{
    const unsigned numGlyphs = glyphs.size();
    // we need to call freeSpace() before refIntArr() and refFloatArr(), see JDK-8127455.
    RenderingQueue& rq = context.platformContext()->rq().freeSpace(24);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb)
        return;

    // The two arrays used to be built as Java int[] and float[] objects and handed over as
    // objects. They are now plain buffers that Java copies out of; the two upcalls, their
    // order relative to freeSpace(), and the two ids they return are unchanged.
    Vector<int32_t, 64> glyphBuffer(numGlyphs);
    for (unsigned i = 0; i < numGlyphs; ++i)
        glyphBuffer[i] = static_cast<int32_t>(glyphs[i]); // glyphs[i] is a GlyphBufferGlyph

    int32_t sid = 0;
    if (cb->rq_ref_int_array) {
        sid = cb->rq_ref_int_array(rq.getWCRenderingQueue(), glyphBuffer.span().data(),
                                   static_cast<int32_t>(numGlyphs));
        wkjCheckAndClearException();
    }

    Vector<float, 64> advanceBuffer(numGlyphs);
    for (unsigned i = 0; i < numGlyphs; ++i)
        advanceBuffer[i] = static_cast<float>(advances[i].width());

    int32_t aid = 0;
    if (cb->rq_ref_float_array) {
        aid = cb->rq_ref_float_array(rq.getWCRenderingQueue(), advanceBuffer.span().data(),
                                     static_cast<int32_t>(numGlyphs));
        wkjCheckAndClearException();
    }

    rq << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_DRAWSTRING_FAST
       << font.platformData().nativeFontData()
       << sid
       << aid
       << static_cast<float>(point.x())
       << static_cast<float>(point.y());
}

bool FontCascade::canReturnFallbackFontsForComplexText()
{
    return false;
}

bool FontCascade::canExpandAroundIdeographsInComplexText()
{
    return false;
}

bool FontCascade::canUseGlyphDisplayList(const RenderStyle&)
{
    return true;
}
}
