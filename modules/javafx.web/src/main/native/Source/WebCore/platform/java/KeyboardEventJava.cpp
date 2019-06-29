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

#include <wtf/java/JavaEnv.h>
#include "KeyboardEvent.h"
#include "PlatformKeyboardEvent.h"
#include "NotImplemented.h"

#include <wtf/Assertions.h>

#include "com_sun_webkit_event_WCKeyEvent.h"

namespace WebCore {

static PlatformKeyboardEvent::Type toPlatformKeyboardEventType(jint type)
{
    switch (type) {
        case com_sun_webkit_event_WCKeyEvent_KEY_PRESSED:
            return PlatformKeyboardEvent::RawKeyDown;
        case com_sun_webkit_event_WCKeyEvent_KEY_TYPED:
            return PlatformKeyboardEvent::Char;
        case com_sun_webkit_event_WCKeyEvent_KEY_RELEASED:
            return PlatformKeyboardEvent::KeyUp;
        default:
            ASSERT_NOT_REACHED();
            return PlatformKeyboardEvent::RawKeyDown;
    }
}

PlatformKeyboardEvent::PlatformKeyboardEvent(
    jint type,
    jstring text,
    jstring keyIdentifier,
    jint windowsVirtualKeyCode,
    jboolean shiftKey,
    jboolean ctrlKey,
    jboolean altKey,
    jboolean metaKey,
    jdouble timestamp)
        : PlatformEvent(
            toPlatformKeyboardEventType(type),
            shiftKey,
            ctrlKey,
            altKey,
            metaKey,
            WallTime::fromRawSeconds(timestamp))
        , m_windowsVirtualKeyCode(windowsVirtualKeyCode)
        , m_autoRepeat(false)
        , m_isKeypad(false)
{
    JNIEnv* env = WTF::GetJavaEnv();

    m_text = text
        ? String(env, text)
        : String();
    m_unmodifiedText = m_text;
    m_keyIdentifier = keyIdentifier
        ? String(env, keyIdentifier)
        : String();
}

bool PlatformKeyboardEvent::currentCapsLockState()
{
    notImplemented();
    return false;
}

void PlatformKeyboardEvent::disambiguateKeyDownEvent(Type, bool)
{
    ASSERT_NOT_REACHED();
}

void PlatformKeyboardEvent::getCurrentModifierState(bool&, bool&, bool&, bool&)
{
    //utaTODO: realize it in Java
/*
static const unsigned short HIGH_BIT_MASK_SHORT = 0x8000;
#if OS(WINDOWS) || PLATFORM(JAVA_WIN)
    shiftKey = GetKeyState(VK_SHIFT) & HIGH_BIT_MASK_SHORT;
    ctrlKey = GetKeyState(VK_CONTROL) & HIGH_BIT_MASK_SHORT;
    altKey = GetKeyState(VK_MENU) & HIGH_BIT_MASK_SHORT;
    metaKey = false;
#elif OS(DARWIN)
    UInt32 currentModifiers = GetCurrentKeyModifiers();
    shiftKey = currentModifiers & ::shiftKey;
    ctrlKey = currentModifiers & ::controlKey;
    altKey = currentModifiers & ::optionKey;
    metaKey = currentModifiers & ::cmdKey;
#else
    notImplemented();
#endif
*/
    notImplemented();
}


} // namespace WebCore
