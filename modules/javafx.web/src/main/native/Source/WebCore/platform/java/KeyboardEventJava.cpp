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

#include "KeyboardEvent.h"
#include "PlatformJavaClasses.h"
#include "PlatformKeyboardEvent.h"
#include "NotImplemented.h"

#include <stdint.h>

#include <wtf/Assertions.h>


namespace WebCore {

static PlatformEvent::Type toPlatformKeyboardEventType(int32_t type)
{
    switch (type) {
        case com_sun_webkit_event_WCKeyEvent_KEY_PRESSED:
            return PlatformEvent::Type::RawKeyDown;
        case com_sun_webkit_event_WCKeyEvent_KEY_TYPED:
            return PlatformEvent::Type::Char;
        case com_sun_webkit_event_WCKeyEvent_KEY_RELEASED:
            return PlatformEvent::Type::KeyUp;
        default:
            ASSERT_NOT_REACHED();
            return PlatformEvent::Type::RawKeyDown;
    }
}

/*
 * The two strings arrive as (pointer, length) pairs rather than as Java string objects.
 *
 * The null test is load-bearing and is kept exactly where it was: a NULL pointer produces the
 * NULL String, while a non-NULL pointer with length 0 produces the empty one. That is not the
 * inbound collapse of contract 11.1, which applies to WKJString and to the Java string
 * constructor it replaces; this call site tested for null itself, before ever building a
 * String, so both halves of the distinction have always been visible here.
 */
PlatformKeyboardEvent::PlatformKeyboardEvent(
    int32_t type,
    const uint16_t* text,
    int32_t textLength,
    const uint16_t* keyIdentifier,
    int32_t keyIdentifierLength,
    int32_t windowsVirtualKeyCode,
    int32_t shiftKey,
    int32_t ctrlKey,
    int32_t altKey,
    int32_t metaKey,
    double timestamp)
        : PlatformEvent(
            toPlatformKeyboardEventType(type),
            shiftKey != 0,
            ctrlKey != 0,
            altKey != 0,
            metaKey != 0,
            MonotonicTime::fromRawSeconds(timestamp))
        , m_autoRepeat(false)
        , m_isKeypad(false)
        , m_windowsVirtualKeyCode(windowsVirtualKeyCode)
{
    m_text = text
        ? wkjMakeString(text, textLength)
        : String();
    m_unmodifiedText = m_text;
    m_keyIdentifier = keyIdentifier
        ? wkjMakeString(keyIdentifier, keyIdentifierLength)
        : String();
}


void PlatformKeyboardEvent::disambiguateKeyDownEvent(Type, bool)
{
    ASSERT_NOT_REACHED();
}
#if 0
bool PlatformKeyboardEvent::currentCapsLockState()
{
    notImplemented();
    return false;
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
#endif

OptionSet<PlatformEvent::Modifier> PlatformKeyboardEvent::currentStateOfModifierKeys()
{
    return { }; // FIXME: Implement.
}
} // namespace WebCore
