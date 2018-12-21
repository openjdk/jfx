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

#include "CookieStorage.h"
#include "Color.h"
#include "ChromiumBridge.h"
#include "DragData.h"
#include "DragImage.h"
#include "Editor.h"
#include "EventHandler.h"
#include "EventLoop.h"
#include "Font.h"
#include "Frame.h"
#include "FrameView.h"
#include "GraphicsContext.h"
#include "ImageBuffer.h"
#include "NotImplemented.h"
#include "RenderObject.h"
#include "ResourceHandle.h"
#include "SSLKeyGenerator.h"
#include "SearchPopupMenuJava.h"
#include "SmartReplace.h"

namespace WebCore
{

//----- copy from editing\qt\SmartReplaceQt.cpp ---------//

#if USE(JAVA_UNICODE)
bool isCharacterSmartReplaceExempt(UChar32 c, bool isPreviousCharacter)
{
    if (WTF::Unicode::Java::isSpaceChar(uint32_t(c)))
        return true;
    if (!isPreviousCharacter && WTF::Unicode::isPunct(c))
        return true;

    if ((c >= 0x1100 && c <= (0x1100 + 256))          // Hangul Jamo (0x1100 - 0x11FF)
        || (c >= 0x2E80 && c <= (0x2E80 + 352))       // CJK & Kangxi Radicals (0x2E80 - 0x2FDF)
        || (c >= 0x2FF0 && c <= (0x2FF0 + 464))       // Ideograph Deseriptions, CJK Symbols, Hiragana, Katakana, Bopomofo, Hangul Compatibility Jamo, Kanbun, & Bopomofo Ext (0x2FF0 - 0x31BF)
        || (c >= 0x3200 && c <= (0x3200 + 29392))     // Enclosed CJK, CJK Ideographs (Uni Han & Ext A), & Yi (0x3200 - 0xA4CF)
        || (c >= 0xAC00 && c <= (0xAC00 + 11183))     // Hangul Syllables (0xAC00 - 0xD7AF)
        || (c >= 0xF900 && c <= (0xF900 + 352))       // CJK Compatibility Ideographs (0xF900 - 0xFA5F)
        || (c >= 0xFE30 && c <= (0xFE30 + 32))        // CJK Compatibility From (0xFE30 - 0xFE4F)
        || (c >= 0xFF00 && c <= (0xFF00 + 240))       // Half/Full Width Form (0xFF00 - 0xFFEF)
        || (c >= 0x20000 && c <= (0x20000 + 0xA6D7))  // CJK Ideograph Exntension B
        || (c >= 0x2F800 && c <= (0x2F800 + 0x021E))) // CJK Compatibility Ideographs (0x2F800 - 0x2FA1D)
       return true;

    const char prev[] = "([\"\'#$/-`{\0";
    const char next[] = ")].,;:?\'!\"%*-/}\0";
    const char* str = (isPreviousCharacter) ? prev : next;
    for (int i = 0; i < strlen(str); ++i) {
        if (str[i] == c)
          return true;
    }

    return false;
}
#endif

// ---- CookieStorage.h ---- //
void setCookieStoragePrivateBrowsingEnabled(bool)
{
    notImplemented();
}


void getSupportedKeySizes(Vector<String>&)
{
    notImplemented();
}

String signedPublicKeyAndChallengeString(unsigned, const String&, const URL&)
{
    notImplemented();
    return String("signedPublicKeyAndChallengeString");
}

// ---- SearchPopupMenuJava.h ---- //

SearchPopupMenuJava::SearchPopupMenuJava(PopupMenuClient* client)
    : m_popup(adoptRef(new PopupMenuJava(client)))
{
}

PopupMenu* SearchPopupMenuJava::popupMenu()
{
    return m_popup.get();
}

bool SearchPopupMenuJava::enabled()
{
    return false;
}

void SearchPopupMenuJava::saveRecentSearches(const AtomicString&, const Vector<RecentSearch>&)
{
    notImplemented();
}

void SearchPopupMenuJava::loadRecentSearches(const AtomicString&, Vector<RecentSearch>&)
{
    notImplemented();
}


// ---- WebCore/page stubs ---- //

// ---- Frame.h ---- //

// ---- WebCore/platform/graphics stubs ---- //

// ---- Color.h ---- //

Color focusRingColor()
{
    notImplemented();
    return Color();
}

bool Path::strokeContains(StrokeStyleApplier*, const FloatPoint&) const
{
    notImplemented();
    return false;
}

} // namespace WebCore
