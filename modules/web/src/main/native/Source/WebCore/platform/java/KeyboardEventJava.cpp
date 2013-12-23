/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaEnv.h"
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
    jboolean metaKey)
: PlatformEvent(
    toPlatformKeyboardEventType(type),
    shiftKey,
    ctrlKey,
    altKey,
    metaKey,
    0.0)
, m_autoRepeat(false)
, m_windowsVirtualKeyCode(windowsVirtualKeyCode)
, m_nativeVirtualKeyCode(0)
, m_isKeypad(false)
{
    JNIEnv* env = WebCore_GetJavaEnv();

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

void PlatformKeyboardEvent::disambiguateKeyDownEvent(Type type, bool backwardsCompatibility)
{
    ASSERT_NOT_REACHED();
}

static const unsigned short HIGH_BIT_MASK_SHORT = 0x8000;
void PlatformKeyboardEvent::getCurrentModifierState(bool& shiftKey, bool& ctrlKey, bool& altKey, bool& metaKey)
{
    //utaTODO: realize it in Java
/*
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
