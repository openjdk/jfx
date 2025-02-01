/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#import "common.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_ui_mac_MacApplication.h"

#import "GlassApplication.h"
#import "GlassMacros.h"
#import "GlassKey.h"

#import <Carbon/Carbon.h>

#pragma clang diagnostic ignored "-Wdeprecated-declarations"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

struct KeyMapEntry
{
    unsigned short        keyCode;
    jint                  jKeyCode;
    BOOL                  sendJavaChars;
};

static const struct KeyMapEntry gKeyMap[] =
{
    {0x00, com_sun_glass_events_KeyEvent_VK_A, YES},
    {0x01, com_sun_glass_events_KeyEvent_VK_S, YES},
    {0x02, com_sun_glass_events_KeyEvent_VK_D, YES},
    {0x03, com_sun_glass_events_KeyEvent_VK_F, YES},
    {0x04, com_sun_glass_events_KeyEvent_VK_H, YES},
    {0x05, com_sun_glass_events_KeyEvent_VK_G, YES},
    {0x06, com_sun_glass_events_KeyEvent_VK_Z, YES},
    {0x07, com_sun_glass_events_KeyEvent_VK_X, YES},
    {0x08, com_sun_glass_events_KeyEvent_VK_C, YES},
    {0x09, com_sun_glass_events_KeyEvent_VK_V, YES},
    {0x0A, com_sun_glass_events_KeyEvent_VK_BACK_QUOTE, YES},
    {0x0B, com_sun_glass_events_KeyEvent_VK_B, YES},
    {0x0C, com_sun_glass_events_KeyEvent_VK_Q, YES},
    {0x0D, com_sun_glass_events_KeyEvent_VK_W, YES},
    {0x0E, com_sun_glass_events_KeyEvent_VK_E, YES},
    {0x0F, com_sun_glass_events_KeyEvent_VK_R, YES},
    {0x10, com_sun_glass_events_KeyEvent_VK_Y, YES},
    {0x11, com_sun_glass_events_KeyEvent_VK_T, YES},
    {0x12, com_sun_glass_events_KeyEvent_VK_1, YES},
    {0x13, com_sun_glass_events_KeyEvent_VK_2, YES},
    {0x14, com_sun_glass_events_KeyEvent_VK_3, YES},
    {0x15, com_sun_glass_events_KeyEvent_VK_4, YES},
    {0x16, com_sun_glass_events_KeyEvent_VK_6, YES},
    {0x17, com_sun_glass_events_KeyEvent_VK_5, YES},
    {0x18, com_sun_glass_events_KeyEvent_VK_EQUALS, YES},
    {0x19, com_sun_glass_events_KeyEvent_VK_9, YES},
    {0x1A, com_sun_glass_events_KeyEvent_VK_7, YES},
    {0x1B, com_sun_glass_events_KeyEvent_VK_MINUS, YES},
    {0x1C, com_sun_glass_events_KeyEvent_VK_8, YES},
    {0x1D, com_sun_glass_events_KeyEvent_VK_0, YES},
    {0x1E, com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET, YES},
    {0x1F, com_sun_glass_events_KeyEvent_VK_O, YES},
    {0x20, com_sun_glass_events_KeyEvent_VK_U, YES},
    {0x21, com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET, YES},
    {0x22, com_sun_glass_events_KeyEvent_VK_I, YES},
    {0x23, com_sun_glass_events_KeyEvent_VK_P, YES},
    {0x24, com_sun_glass_events_KeyEvent_VK_ENTER, YES},
    {0x25, com_sun_glass_events_KeyEvent_VK_L, YES},
    {0x26, com_sun_glass_events_KeyEvent_VK_J, YES},
    {0x27, com_sun_glass_events_KeyEvent_VK_QUOTE, YES},
    {0x28, com_sun_glass_events_KeyEvent_VK_K, YES},
    {0x29, com_sun_glass_events_KeyEvent_VK_SEMICOLON, YES},
    {0x2A, com_sun_glass_events_KeyEvent_VK_BACK_SLASH, YES},
    {0x2B, com_sun_glass_events_KeyEvent_VK_COMMA, YES},
    {0x2C, com_sun_glass_events_KeyEvent_VK_SLASH, YES},
    {0x2D, com_sun_glass_events_KeyEvent_VK_N, YES},
    {0x2E, com_sun_glass_events_KeyEvent_VK_M, YES},
    {0x2F, com_sun_glass_events_KeyEvent_VK_PERIOD, YES},
    {0x30, com_sun_glass_events_KeyEvent_VK_TAB, YES},
    {0x31, com_sun_glass_events_KeyEvent_VK_SPACE, YES},
    {0x32, com_sun_glass_events_KeyEvent_VK_BACK_QUOTE, YES},
    {0x33, com_sun_glass_events_KeyEvent_VK_BACKSPACE, NO},
    {0x34, com_sun_glass_events_KeyEvent_VK_ENTER, YES},
    {0x35, com_sun_glass_events_KeyEvent_VK_ESCAPE, NO},
    {0x36, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x37, com_sun_glass_events_KeyEvent_VK_COMMAND, NO},
    {0x38, com_sun_glass_events_KeyEvent_VK_SHIFT, NO},
    {0x39, com_sun_glass_events_KeyEvent_VK_CAPS_LOCK, NO},
    {0x3A, com_sun_glass_events_KeyEvent_VK_ALT, NO},
    {0x3B, com_sun_glass_events_KeyEvent_VK_CONTROL, NO},
    {0x3C, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x3D, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x3E, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x3F, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x40, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x41, com_sun_glass_events_KeyEvent_VK_DECIMAL, YES},
    {0x42, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x43, com_sun_glass_events_KeyEvent_VK_MULTIPLY, YES},
    {0x44, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x45, com_sun_glass_events_KeyEvent_VK_ADD, YES},
    {0x46, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x47, com_sun_glass_events_KeyEvent_VK_CLEAR, NO},
    {0x48, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x49, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x4A, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x4B, com_sun_glass_events_KeyEvent_VK_DIVIDE, YES},
    {0x4C, com_sun_glass_events_KeyEvent_VK_ENTER, YES},
    {0x4D, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x4E, com_sun_glass_events_KeyEvent_VK_SUBTRACT, YES},
    {0x4F, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x50, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x51, com_sun_glass_events_KeyEvent_VK_EQUALS, YES},
    {0x52, com_sun_glass_events_KeyEvent_VK_NUMPAD0, YES},
    {0x53, com_sun_glass_events_KeyEvent_VK_NUMPAD1, YES},
    {0x54, com_sun_glass_events_KeyEvent_VK_NUMPAD2, YES},
    {0x55, com_sun_glass_events_KeyEvent_VK_NUMPAD3, YES},
    {0x56, com_sun_glass_events_KeyEvent_VK_NUMPAD4, YES},
    {0x57, com_sun_glass_events_KeyEvent_VK_NUMPAD5, YES},
    {0x58, com_sun_glass_events_KeyEvent_VK_NUMPAD6, YES},
    {0x59, com_sun_glass_events_KeyEvent_VK_NUMPAD7, YES},
    {0x5A, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x5B, com_sun_glass_events_KeyEvent_VK_NUMPAD8, YES},
    {0x5C, com_sun_glass_events_KeyEvent_VK_NUMPAD9, YES},
    {0x5D, com_sun_glass_events_KeyEvent_VK_BACK_SLASH, YES},
    {0x5E, com_sun_glass_events_KeyEvent_VK_UNDERSCORE, YES},
    {0x5F, com_sun_glass_events_KeyEvent_VK_COMMA, YES},
    {0x60, com_sun_glass_events_KeyEvent_VK_F5, NO},
    {0x61, com_sun_glass_events_KeyEvent_VK_F6, NO},
    {0x62, com_sun_glass_events_KeyEvent_VK_F7, NO},
    {0x63, com_sun_glass_events_KeyEvent_VK_F3, NO},
    {0x64, com_sun_glass_events_KeyEvent_VK_F8, NO},
    {0x65, com_sun_glass_events_KeyEvent_VK_F9, NO},
//        {0x66, com_sun_glass_events_KeyEvent_VK_ALPHANUMERIC, NO},
    {0x67, com_sun_glass_events_KeyEvent_VK_F11, NO},
//        {0x68, com_sun_glass_events_KeyEvent_VK_KATAKANA, NO},
//        {0x69, com_sun_glass_events_KeyEvent_VK_F13, NO},
//        {0x6A, com_sun_glass_events_KeyEvent_VK_F16, NO},
//        {0x6B, com_sun_glass_events_KeyEvent_VK_F14, NO},
    {0x6C, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x6D, com_sun_glass_events_KeyEvent_VK_F10, NO},
    {0x6E, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
    {0x6F, com_sun_glass_events_KeyEvent_VK_F12, NO},
    {0x70, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
//        {0x71, com_sun_glass_events_KeyEvent_VK_F15, NO},
    {0x72, com_sun_glass_events_KeyEvent_VK_HELP, NO},
    {0x73, com_sun_glass_events_KeyEvent_VK_HOME, NO},
    {0x74, com_sun_glass_events_KeyEvent_VK_PAGE_UP, NO},
    {0x75, com_sun_glass_events_KeyEvent_VK_DELETE, NO},
    {0x76, com_sun_glass_events_KeyEvent_VK_F4, NO},
    {0x77, com_sun_glass_events_KeyEvent_VK_END, NO},
    {0x78, com_sun_glass_events_KeyEvent_VK_F2, NO},
    {0x79, com_sun_glass_events_KeyEvent_VK_PAGE_DOWN, NO},
    {0x7A, com_sun_glass_events_KeyEvent_VK_F1, NO},
    {0x7B, com_sun_glass_events_KeyEvent_VK_LEFT, NO},
    {0x7C, com_sun_glass_events_KeyEvent_VK_RIGHT, NO},
    {0x7D, com_sun_glass_events_KeyEvent_VK_DOWN, NO},
    {0x7E, com_sun_glass_events_KeyEvent_VK_UP, NO},
    {0x7F, com_sun_glass_events_KeyEvent_VK_UNDEFINED, NO},
};
static const int gKeyMapSize = sizeof(gKeyMap) / sizeof(struct KeyMapEntry);

static BOOL macKeyCodeIsLayoutSensitive(unsigned int keyCode)
{
    // Mac key codes that generate different characters based on the keyboard layout
    // lie in two ranges with a few exceptions. Details can be found in the Events.h
    // header in the HIToolbox framework inside the Carbon framework.
    switch (keyCode)
    {
        case 0x24: // Enter
        case 0x30: // Tab
        case 0x31: // Space
            return NO;
    }

    // kVK_ANSI_A through kVK_ANSI_Grave
    if (keyCode >= 0x00 && keyCode <= 0x32) {
        return YES;
    }

    // kVK_JIS_Yen through kVK_JIS_KeypadComma. The other JIS keys (0x66 and
    // 0x68) were commented out of the table above so they are not included
    // here.
    if (keyCode >= 0x5D && keyCode <= 0x5F) {
        return YES;
    }

    return NO;
}

static jint getJavaCodeForASCII(UniChar ascii)
{
    if (ascii >= L'0' && ascii <= L'9') {
        return ascii;
    }
    if (ascii >= L'a' && ascii <= L'z') {
        return ascii + (L'A' - L'a');
    }
    if (ascii >= L'A' && ascii <= L'Z') {
        return ascii;
    }

    switch (ascii)
    {
        case L' ': return com_sun_glass_events_KeyEvent_VK_SPACE;
        case L'!': return com_sun_glass_events_KeyEvent_VK_EXCLAMATION;
        case L'"': return com_sun_glass_events_KeyEvent_VK_DOUBLE_QUOTE;
        case L'#': return com_sun_glass_events_KeyEvent_VK_NUMBER_SIGN;
        case L'$': return com_sun_glass_events_KeyEvent_VK_DOLLAR;
        case L'&': return com_sun_glass_events_KeyEvent_VK_AMPERSAND;
        case L'\'':return com_sun_glass_events_KeyEvent_VK_QUOTE;
        case L'(': return com_sun_glass_events_KeyEvent_VK_LEFT_PARENTHESIS;
        case L')': return com_sun_glass_events_KeyEvent_VK_RIGHT_PARENTHESIS;
        case L'*': return com_sun_glass_events_KeyEvent_VK_ASTERISK;
        case L'+': return com_sun_glass_events_KeyEvent_VK_PLUS;
        case L',': return com_sun_glass_events_KeyEvent_VK_COMMA;
        case L'-': return com_sun_glass_events_KeyEvent_VK_MINUS;
        case L'.': return com_sun_glass_events_KeyEvent_VK_PERIOD;
        case L'/': return com_sun_glass_events_KeyEvent_VK_SLASH;
        case L':': return com_sun_glass_events_KeyEvent_VK_COLON;
        case L';': return com_sun_glass_events_KeyEvent_VK_SEMICOLON;
        case L'<': return com_sun_glass_events_KeyEvent_VK_LESS;
        case L'=': return com_sun_glass_events_KeyEvent_VK_EQUALS;
        case L'>': return com_sun_glass_events_KeyEvent_VK_GREATER;
        case L'@': return com_sun_glass_events_KeyEvent_VK_AT;
        case L'[': return com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET;
        case L'\\':return com_sun_glass_events_KeyEvent_VK_BACK_SLASH;
        case L']': return com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET;
        case L'^': return com_sun_glass_events_KeyEvent_VK_CIRCUMFLEX;
        case L'_': return com_sun_glass_events_KeyEvent_VK_UNDERSCORE;
        case L'`': return com_sun_glass_events_KeyEvent_VK_BACK_QUOTE;
        case L'{': return com_sun_glass_events_KeyEvent_VK_BRACELEFT;
        case L'}': return com_sun_glass_events_KeyEvent_VK_BRACERIGHT;
        case 0x00A1: return com_sun_glass_events_KeyEvent_VK_INV_EXCLAMATION;
        case 0x20A0: return com_sun_glass_events_KeyEvent_VK_EURO_SIGN;
    }

    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
};

static UniCharCount queryKeyboard(TISInputSourceRef keyboard, unsigned short keyCode, UInt32 modifiers,
                                  UniChar* buffer, UniCharCount bufferSize)
{
    CFDataRef uchr = (CFDataRef)TISGetInputSourceProperty(keyboard,
                                                          kTISPropertyUnicodeKeyLayoutData);
    if (uchr == NULL) {
        return 0;
    }
    const UCKeyboardLayout *layout = (const UCKeyboardLayout*)CFDataGetBytePtr(uchr);

    UInt32 deadKeyState = 0;
    UniCharCount actualLength = 0;
    OSStatus status = UCKeyTranslate(layout,
                                     keyCode, kUCKeyActionDown,
                                     modifiers >> 8,
                                     LMGetKbdType(),
                                     kUCKeyTranslateNoDeadKeysMask, &deadKeyState,
                                     bufferSize, &actualLength,
                                     buffer);
    if (status != noErr) {
        actualLength = 0;
    }

    // The Unicode Hex layout can yield a string of length 1 consisting of a
    // code point of 0.
    if (actualLength == 1 && buffer[0] == 0) {
        actualLength = 0;
    }

    return actualLength;
}

static BOOL isLetterOrDigit(jint javaKeyCode)
{
    if (javaKeyCode >= '0' && javaKeyCode <= '9') {
        return YES;
    }
    if (javaKeyCode >= 'A' && javaKeyCode <= 'Z') {
        return YES;
    }
    return NO;
}

// This is only valid for keys in the layout-sensitive area.
static jint getJavaCodeForMacKeyAndModifiers(TISInputSourceRef keyboard, unsigned short keyCode, UInt32 modifiers)
{
    jint result = com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    UniChar unicode[8];
    UniCharCount length = queryKeyboard(keyboard, keyCode, modifiers, unicode, 8);
    if (length == 1) {
        result = getJavaCodeForASCII(unicode[0]);
    }
    return result;
}

// This is only valid for keys in the layout-sensitive area.
static jint getJavaCodeForMacKey(unsigned short keyCode)
{
    jint result = com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    TISInputSourceRef keyboard = TISCopyCurrentKeyboardLayoutInputSource();
    if (keyboard == NULL) {
        return result;
    }

    // Java key codes are used in accelerator processing so we will try to match them
    // the same way Apple handles key equivalents e.g. by asking for the Cmd character.
    // This is necessary on non-ASCII layouts such as Cyrillic or Arabic to ensure the
    // translation produces an ASCII key. Exactly how this is done is specific to the
    // keyboard but for non-ASCII keyboards it generally generates some variant of
    // QWERTY.

    // First just Cmd.
    result = getJavaCodeForMacKeyAndModifiers(keyboard, keyCode, cmdKey);

    // If we didn't get a hit try looking at the Shifted variant. Even if we did get a
    // hit we favor numerals and letters over punctuation. This brings the French
    // keyboard in line with Windows; the digits are shifted but are still considered
    // the canonical key codes.
    if (!isLetterOrDigit(result)) {
        jint trial = getJavaCodeForMacKeyAndModifiers(keyboard, keyCode, cmdKey | shiftKey);
        if (isLetterOrDigit(trial)) {
            result = trial;
        }
        else if (result == com_sun_glass_events_KeyEvent_VK_UNDEFINED) {
            result = trial;
        }

        // A handful of keyboards (Azeri, Turkmen, and Sami variants) can only access
        // critical letters like Q by using the Option key in conjunction with Cmd.
        // In this API the Cmd flag suppresses the Option flag so we omit Cmd.
        if (!isLetterOrDigit(result)) {
            jint trial = getJavaCodeForMacKeyAndModifiers(keyboard, keyCode, optionKey);
            if (isLetterOrDigit(trial)) {
                result = trial;
            }
        }
    }

    CFRelease(keyboard);
    return result;
}

jint GetJavaKeyModifiers(NSEvent *event)
{
    jint jModifiers = 0;

    NSUInteger modifierFlags = ([event modifierFlags] & NSDeviceIndependentModifierFlagsMask);
    if (((modifierFlags & NSShiftKeyMask) != 0) == YES)
    {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_SHIFT;
    }
    if (((modifierFlags & NSFunctionKeyMask) != 0) == YES)
    {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_FUNCTION;
    }
    if (((modifierFlags & NSControlKeyMask) != 0) == YES)
    {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_CONTROL;
    }
    if (((modifierFlags & NSAlternateKeyMask) != 0) == YES)
    {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_OPTION;
    }
    if (((modifierFlags & NSCommandKeyMask) != 0) == YES)
    {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_COMMAND;
    }
    return jModifiers;
}

jint GetJavaMouseModifiers(NSUInteger buttons)
{
    jint jModifiers = 0;
    if (buttons & (1 << 0)) {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY;
    }
    if (buttons & (1 << 1)) {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY;
    }
    if (buttons & (1 << 2)) {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE;
    }
    if (buttons & (1 << 3)) {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK;
    }
    if (buttons & (1 << 4)) {
        jModifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD;
    }
    return jModifiers;
}

jint GetJavaModifiers(NSEvent *event)
{
    return GetJavaKeyModifiers(event) | GetJavaMouseModifiers([NSEvent pressedMouseButtons]);
}

jint GetJavaKeyCodeFor(unsigned short keyCode)
{
    if (macKeyCodeIsLayoutSensitive(keyCode))
    {
        return getJavaCodeForMacKey(keyCode);
    }

    // Not the fastest implementation...
    for (int i=0; i<gKeyMapSize; i++)
    {
        if (gKeyMap[i].keyCode == keyCode)
        {
            return gKeyMap[i].jKeyCode;
        }
    }
    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
}

jint GetJavaKeyCode(NSEvent *event)
{
    return GetJavaKeyCodeFor([event keyCode]);
}

jcharArray GetJavaKeyChars(JNIEnv *env, NSEvent *event)
{
    NSString *chars = [event characters];

    unsigned short keyCode = [event keyCode];
    BOOL needChars = NO;
    // Not the fastest implementation...
    for (int i=0; i<gKeyMapSize; i++)
    {
        if (gKeyMap[i].keyCode == keyCode)
        {
            needChars = gKeyMap[i].sendJavaChars;
            break;
        }
    }
    if (needChars == NO)
    {
        // Return an empty array instead of NULL
        return (*env)->NewCharArray(env, 0);
    }

    jchar jc[16];
    [chars getCharacters:jc range:NSMakeRange(0, [chars length])];
    jcharArray jChars = (*env)->NewCharArray(env, (jsize)[chars length]);
    if (jChars == NULL) {
        return NULL;
    }
    (*env)->SetCharArrayRegion(env, jChars, 0, (jsize)[chars length], jc);
    GLASS_CHECK_EXCEPTION(env);
    return jChars;
}

BOOL GetMacKey(jint javaKeyCode, unsigned short *outMacKeyCode)
{
    if (javaKeyCode == com_sun_glass_events_KeyEvent_VK_UNDEFINED) {
        return NO;
    }

    BOOL found = NO;
    // Find a key code based on the US QWERTY layout
    for (int index=0; index<gKeyMapSize; index++)
    {
        if (gKeyMap[index].jKeyCode == javaKeyCode)
        {
            *outMacKeyCode = gKeyMap[index].keyCode;
            found = YES;
            break;
        }
    }

    // The table only covers US QWERTY so it's missing entries like PLUS that
    // don't appear on that layout.
    if (found && !macKeyCodeIsLayoutSensitive(*outMacKeyCode)) {
        return YES;
    }

    // If the QWERTY key is in the layout sensitive area search the other keys in that
    // area. We may not find a key so returning NO is possible.
    for (unsigned short trialKey = 0x00; trialKey <= 0x5F; ++trialKey)
    {
        if (macKeyCodeIsLayoutSensitive(trialKey))
        {
            jint trialCode = getJavaCodeForMacKey(trialKey);
            if (trialCode == javaKeyCode)
            {
                *outMacKeyCode = trialKey;
                return YES;
            }
        }
    }

    return NO;
}

NSString* GetStringForJavaKey(jchar jKeyCode) {
    if (jKeyCode == '\0')
      return @"";

    if (islower(jKeyCode))
    {
        return [[NSString stringWithFormat:@"%c", jKeyCode] lowercaseString];
    }

    unichar   unicode = 0;
    switch (jKeyCode)
    {
        case com_sun_glass_events_KeyEvent_VK_LEFT:
            unicode = 0x2190; break;
        case com_sun_glass_events_KeyEvent_VK_RIGHT:
            unicode = 0x2192; break;
        case com_sun_glass_events_KeyEvent_VK_UP:
            unicode = 0x2191; break;
        case com_sun_glass_events_KeyEvent_VK_DOWN:
            unicode = 0x2193; break;
        case com_sun_glass_events_KeyEvent_VK_ESCAPE:
            unicode = 0x238B; break;
        case com_sun_glass_events_KeyEvent_VK_LEFT_PARENTHESIS:
            unicode = 0x0028; break;
        case com_sun_glass_events_KeyEvent_VK_RIGHT_PARENTHESIS:
            unicode = 0x0029; break;
        case com_sun_glass_events_KeyEvent_VK_COLON:
            unicode = 0x003A; break;
        case com_sun_glass_events_KeyEvent_VK_DOLLAR:
            unicode = 0x0024; break;
        case com_sun_glass_events_KeyEvent_VK_EURO_SIGN:
            unicode = 0x20AC; break;
        case com_sun_glass_events_KeyEvent_VK_EXCLAMATION:
            unicode = 0x0021; break;
        case com_sun_glass_events_KeyEvent_VK_UNDERSCORE:
            unicode = 0x005F; break;
        case com_sun_glass_events_KeyEvent_VK_AT:
            unicode = 0x0040; break;
        case com_sun_glass_events_KeyEvent_VK_BRACELEFT:
            unicode = 0x007B; break;
        case com_sun_glass_events_KeyEvent_VK_BRACERIGHT:
            unicode = 0x007D; break;
        case com_sun_glass_events_KeyEvent_VK_LESS:
            unicode = 0x003C; break;
        case com_sun_glass_events_KeyEvent_VK_GREATER:
            unicode = 0x003E; break;
        case com_sun_glass_events_KeyEvent_VK_QUOTE:
            unicode = 0x0027; break;
        case com_sun_glass_events_KeyEvent_VK_BACK_QUOTE:
            unicode = 0x0060; break;
        case com_sun_glass_events_KeyEvent_VK_AMPERSAND:
            unicode = 0x0026; break;
        case com_sun_glass_events_KeyEvent_VK_MULTIPLY:
        case com_sun_glass_events_KeyEvent_VK_ASTERISK:
            unicode = 0x002A; break;
        case com_sun_glass_events_KeyEvent_VK_DOUBLE_QUOTE:
            unicode = 0x0022; break;
        case com_sun_glass_events_KeyEvent_VK_NUMBER_SIGN:
            unicode = 0x0023; break;
        case com_sun_glass_events_KeyEvent_VK_PLUS:
        case com_sun_glass_events_KeyEvent_VK_ADD:
            unicode = 0x002B; break;
        case com_sun_glass_events_KeyEvent_VK_MINUS:
        case com_sun_glass_events_KeyEvent_VK_SUBTRACT:
            unicode = 0x2013; break;
        case com_sun_glass_events_KeyEvent_VK_DIVIDE:
            unicode = 0x002F; break;
        case com_sun_glass_events_KeyEvent_VK_CIRCUMFLEX:
            unicode = 0x005E; break;
        case com_sun_glass_events_KeyEvent_VK_DECIMAL:
            unicode = 0x002E; break;
        case com_sun_glass_events_KeyEvent_VK_DELETE:
            unicode = 0x2326; break;
        default:
            break;
    }

    if (unicode != 0)
    {
        LOG("GetStringForJavaKey: unicode %x", unicode);
        return [NSString stringWithCharacters:&unicode length:1];
    }
    else
    {
        return [[NSString stringWithFormat:@"%c", jKeyCode] lowercaseString];
    }

}

NSString* GetStringForMacKey(unsigned short keyCode, bool shifted)
{
    // Restrict to printable characters. UCKeyTranslate can produce
    // odd results with keys like Home, Up Arrow, etc.
    if (!macKeyCodeIsLayoutSensitive(keyCode)) return nil;

    TISInputSourceRef keyboard = TISCopyCurrentKeyboardLayoutInputSource();
    if (keyboard == NULL) return nil;

    UInt32 modifiers = (shifted ? shiftKey : 0);
    UniChar unicode[8];
    UniCharCount length = queryKeyboard(keyboard, keyCode, modifiers, unicode, 8);
    CFRelease(keyboard);

    if (length == 1) {
        return [NSString stringWithCharacters: &unicode[0] length: 1];
    }

    return nil;
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _getKeyCodeForChar
 * Signature: (CI)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacApplication__1getKeyCodeForChar
(JNIEnv * env, jobject jApplication, jchar c, jint hint)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1getKeyCodeForChar");

    return [GlassApplication getKeyCodeForChar:c];
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _isKeyLocked
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacApplication__1isKeyLocked
  (JNIEnv * env, jobject obj, jint keyCode)
{
    NSUInteger mask = 0;
    switch (keyCode) {
        case com_sun_glass_events_KeyEvent_VK_CAPS_LOCK:
            mask = NSEventModifierFlagCapsLock;
            break;

        // Caps lock is the only locking key supported on macOS
        default:
            return com_sun_glass_events_KeyEvent_KEY_LOCK_UNKNOWN;
    }
    NSUInteger modifierFlags = [NSEvent modifierFlags];
    return (modifierFlags & mask) ? com_sun_glass_events_KeyEvent_KEY_LOCK_ON
                                  : com_sun_glass_events_KeyEvent_KEY_LOCK_OFF;
}
