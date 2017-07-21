/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include <wtf/PassRefPtr.h>
#if USE(JAVA_UNICODE)
#include <wtf/unicode/java/UnicodeJava.h>
#elif USE(ICU_UNICODE)
#include <wtf/unicode/icu/UnicodeIcu.h>
#endif

#include "DataTransfer.h" // WebKit BUG: must be included from Editor.h
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
#include "URL.h" // WebKit BUG: must be included from Pasteboard.h
#include "Language.h"
#include "NotImplemented.h"
#include "RenderObject.h"
#include "ResourceHandle.h"
#include "SharedBuffer.h" // WebKit BUG: must be included from ResourceHandle.h
#include "SSLKeyGenerator.h"
#include "SearchPopupMenuJava.h"
#include "SmartReplace.h"
#include "JSCTestRunnerUtils.h"

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

#if OS(WINDOWS) || OS(LINUX)
// Reference these functions to make the linker include
// JSCTestRunnerUtils.obj into jfxwebkit.dll
// The functions are called from DumpRenderTreeJava.dll
void referenceJSCTestRunnerUtils()
{
    JSC::numberOfDFGCompiles(0, 0);
    JSC::setNeverInline(0, 0);
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

struct ScopedState {
    ScopedState(Frame* theFrame, RenderObject* theRenderer)
        : frame(theFrame)
        , renderer(theRenderer)
        , paintBehavior(frame->view()->paintBehavior())
        , backgroundColor(frame->view()->baseBackgroundColor())
    {
    }

    ~ScopedState()
    {
        if (renderer)
            renderer->updateDragState(false);
        frame->view()->setPaintBehavior(paintBehavior);
        frame->view()->setBaseBackgroundColor(backgroundColor);
        frame->view()->setNodeToDraw(0);
    }

    Frame* frame;
    RenderObject* renderer;
    PaintBehavior paintBehavior;
    Color backgroundColor;
};

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
