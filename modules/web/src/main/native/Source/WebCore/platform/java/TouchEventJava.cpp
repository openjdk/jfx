/*
 * This file is part of the WebKit project.
 *
 * Copyright (C) 2009 Nokia Corporation and/or its subsidiary(-ies)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 */

#include "Platform.h" // todo tav remove when building w/ pch

#if ENABLE(TOUCH_EVENTS)

#include "com_sun_webkit_event_WCTouchEvent.h"
#include "PlatformTouchEvent.h"

namespace WebCore {

PlatformTouchEvent::PlatformTouchEvent(JNIEnv* env, jint id, jobject touchData,
        jboolean shift, jboolean ctrl, jboolean alt, jboolean meta, jfloat timestamp)
{
    switch (id) {
    case com_sun_webkit_event_WCTouchEvent_TOUCH_START:
        m_type = PlatformEvent::TouchStart;
        break;
    case com_sun_webkit_event_WCTouchEvent_TOUCH_MOVE:
        m_type = PlatformEvent::TouchMove;
        break;
    case com_sun_webkit_event_WCTouchEvent_TOUCH_END:
        m_type = PlatformEvent::TouchEnd;
        break;
    }

    ASSERT(touchData);

    jint* p(static_cast<jint*>(env->GetDirectBufferAddress(touchData)));
    // each touch datum has 6 int (4-byte) items
    long count(env->GetDirectBufferCapacity(touchData) / 6 / 4);
    for (long i=0; i<count; i++) {
        jint id = *p++;
        jint state = *p++;
        jint x = *p++;
        jint y = *p++;
        jint sx = *p++;
        jint sy = *p++;
        m_touchPoints.append(PlatformTouchPoint(id,
                PlatformTouchPoint::State(state),
                IntPoint(x, y),
                IntPoint(sx, sy)));
    }

    m_timestamp = timestamp;
    m_modifiers = 0;
    if (shift)
        m_modifiers |= ShiftKey;
    if (ctrl)
        m_modifiers |= CtrlKey;
    if (alt)
        m_modifiers |= AltKey;
    if (meta)
        m_modifiers |= MetaKey;
}

PlatformTouchPoint::PlatformTouchPoint(unsigned id, State state,
        const IntPoint& pos, const IntPoint& screenPos)
    : m_id(id)
    , m_state(state)
    , m_pos(pos)
    , m_screenPos(screenPos)
{
}

}

#endif
