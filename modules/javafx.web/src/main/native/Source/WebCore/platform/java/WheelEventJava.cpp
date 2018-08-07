/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "PlatformWheelEvent.h"

namespace WebCore {

PlatformWheelEvent::PlatformWheelEvent(
    const IntPoint& pos,
    const IntPoint& globalPos,
    float deltaX,
    float deltaY,
    bool shiftKey,
    bool ctrlKey,
    bool altKey,
    bool metaKey)
        : PlatformEvent(
        PlatformEvent::Wheel,
        shiftKey,
        ctrlKey,
        altKey,
        metaKey,
                WallTime {})
        , m_position(pos)
        , m_globalPosition(globalPos)
      // For some unknown reason, EventHandler expects deltaX/deltaY < 0 for
      // ScrollRight/ScrollDown, and deltaX/deltaY > 0 for ScrollLeft/ScrollUp.
      // Java mouse wheel events behave in reverse way, so need a negation here.
        , m_deltaX(-deltaX)
        , m_deltaY(-deltaY)
        , m_wheelTicksX(-deltaX)
        , m_wheelTicksY(-deltaY)
        , m_granularity(ScrollByPixelWheelEvent)
        , m_directionInvertedFromDevice(false)
{
}

} // namespace WebCore
