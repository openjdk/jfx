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


#include "ChromeClientJava.h"
#include "DataListSuggestionPicker.h"
#include <WebCore/DateTimeChooser.h>
#if ENABLE(INPUT_TYPE_COLOR)
#include "ColorChooserJava.h"
#endif
#include <WebCore/ContextMenu.h>
#include "PopupMenuJava.h"
#include "SearchPopupMenuJava.h"
#include <WebCore/PlatformJavaClasses.h>
#include "WebPage.h"
#include "Cursor.h"
#include <WebCore/DocumentLoader.h>
#include <WebCore/DragController.h>
#include <WebCore/FileChooser.h>
#include <WebCore/FileIconLoader.h>
#include <WebCore/FloatRect.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameLoadRequest.h>
#include <WebCore/FrameLoader.h>
#include <WebCore/FrameView.h>
#include <WebCore/HitTestResult.h>
#include <WebCore/Icon.h>
#include <WebCore/IntRect.h>
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <WebCore/ResourceRequest.h>
#include <WebCore/Widget.h>
#include <WebCore/WindowFeatures.h>
#include <span>
#include <wtf/URL.h>
#include <wtf/text/StringBuilder.h>

namespace WebCore {

void ChromeClientJava::chromeDestroyed()
{
}

#if ENABLE(INPUT_TYPE_COLOR)
RefPtr<ColorChooser> ChromeClientJava::createColorChooser(ColorChooserClient& client, const Color& initialColor)
{
    return adoptRef(new ColorChooserJava(m_pageRef, &client, initialColor));
}
#endif

FloatRect ChromeClientJava::windowRect() const
{
    if (m_callbacks && m_callbacks->get_window_bounds) {
        float x = 0, y = 0, width = 0, height = 0;
        if (m_callbacks->get_window_bounds(m_pageRef, &x, &y, &width, &height))
            return FloatRect(x, y, width, height);
    }
    return IntRect(0, 0, 0, 0);
}

void ChromeClientJava::setWindowRect(const FloatRect &r)
{
    if (!m_callbacks || !m_callbacks->set_window_bounds)
        return;

    m_callbacks->set_window_bounds(m_pageRef,
                          (int)(r.x()), (int)(r.y()), (int)(r.width()), (int)(r.height()));
}

FloatRect ChromeClientJava::pageRect() const
{
    if (m_callbacks && m_callbacks->get_page_bounds) {
        float x = 0, y = 0, width = 0, height = 0;
        if (m_callbacks->get_page_bounds(m_pageRef, &x, &y, &width, &height))
            return FloatRect(x, y, width, height);
    }
    return FloatRect(0, 0, 0, 0);
}

void ChromeClientJava::focus()
{
    if (m_callbacks && m_callbacks->set_focus)
        m_callbacks->set_focus(m_pageRef, 1);
}

void ChromeClientJava::unfocus()
{
    if (m_callbacks && m_callbacks->set_focus)
        m_callbacks->set_focus(m_pageRef, 0);
}

bool ChromeClientJava::canTakeFocus(FocusDirection) const
{
    return true;
}

void ChromeClientJava::takeFocus(FocusDirection direction)
{
    if (!m_callbacks || !m_callbacks->transfer_focus)
        return;

    m_callbacks->transfer_focus(m_pageRef, direction == FocusDirection::Forward ? 1 : 0);
}

void ChromeClientJava::focusedElementChanged(Element*, LocalFrame*, FocusOptions, BroadcastFocusedElement)
{
    notImplemented();
}

void ChromeClientJava::focusedFrameChanged(Frame*)
{
    notImplemented();
}

void ChromeClientJava::rootFrameAdded(const LocalFrame&)
{
   notImplemented();
}

void ChromeClientJava::rootFrameRemoved(const LocalFrame&)
{
    notImplemented();
}

RefPtr<Page> ChromeClientJava::createWindow(
    LocalFrame& frame, const String& openedMainFrameName,
    const WindowFeatures& features,
    const NavigationAction& na)
{
    if (!m_callbacks || !m_callbacks->create_window)
        return nullptr;

    /*
     * The slot returns the new page handle - the long the Java WebPage holds as pPage -
     * rather than a registry id for the Java object, because what is needed here is the
     * WebCore::Page. That is what removes WebPage::pageFromJObject and the getPage upcall.
     */
    int64_t newWebPage = m_callbacks->create_window(m_pageRef,
            features.menuBarVisible ? 1 : 0,
            features.statusBarVisible ? 1 : 0,
            (features.toolBarVisible || features.locationBarVisible) ? 1 : 0,
            features.resizable ? 1 : 0);

    if (!newWebPage) {
        return nullptr;
    }

    Page* p = WebPage::pageFromPeer(newWebPage);
    auto localFrame =  dynamicDowncast<LocalFrame>(p->mainFrame());
    //set opened frame name
    if (!openedMainFrameName.isEmpty())
        localFrame->tree().setSpecifiedName(AtomString(openedMainFrameName));
    localFrame->loader().load(FrameLoadRequest(*localFrame, ResourceRequest(na.url())));
    return RefPtr<Page>(p);
}

void ChromeClientJava::closeWindow()
{
    if (m_callbacks && m_callbacks->close_window)
        m_callbacks->close_window(m_pageRef);
}

void ChromeClientJava::show()
{
    if (m_callbacks && m_callbacks->show_window)
        m_callbacks->show_window(m_pageRef);
}

bool ChromeClientJava::canRunModal() const
{
    notImplemented();
    return false;
}

void ChromeClientJava::runModal()
{
    notImplemented();
}

void ChromeClientJava::setResizable(bool)
{
    notImplemented();
}

void ChromeClientJava::setToolbarsVisible(bool)
{
    notImplemented();
}

bool ChromeClientJava::toolbarsVisible() const
{
    notImplemented();
    return false;
}

void ChromeClientJava::setStatusbarVisible(bool)
{
    notImplemented();
}

bool ChromeClientJava::statusbarVisible() const
{
    notImplemented();
    return false;
}

void ChromeClientJava::setScrollbarsVisible(bool v)
{
    if (m_callbacks && m_callbacks->set_scrollbars_visible)
        m_callbacks->set_scrollbars_visible(m_pageRef, v ? 1 : 0);
}

bool ChromeClientJava::scrollbarsVisible() const
{
    notImplemented();
    return false;
}

void ChromeClientJava::setMenubarVisible(bool)
{
    notImplemented();
}

bool ChromeClientJava::menubarVisible() const
{
    notImplemented();
    return false;
}

void ChromeClientJava::setStatusbarText(const String& text)
{
    if (!m_callbacks || !m_callbacks->set_statusbar_text)
        return;

    WKJStringArg textArg(text);
    m_callbacks->set_statusbar_text(m_pageRef, textArg.data(), textArg.length());
}

void ChromeClientJava::setCursor(const Cursor& c)
{
    if (!m_callbacks || !m_callbacks->set_cursor)
        return;

    /* PlatformCursor is a jlong for this port (Source/WebCore/platform/Cursor.h:79). */
    m_callbacks->set_cursor(m_pageRef, static_cast<int64_t>(c.platformCursor()));
}

void ChromeClientJava::setCursorHiddenUntilMouseMoves(bool)
{
    notImplemented();
}

void ChromeClientJava::runJavaScriptAlert(LocalFrame&, const String& text)
{
    if (!m_callbacks || !m_callbacks->alert)
        return;

    WKJStringArg textArg(text);
    m_callbacks->alert(m_pageRef, textArg.data(), textArg.length());
}

bool ChromeClientJava::runJavaScriptConfirm(LocalFrame&, const String& text)
{
    if (!m_callbacks || !m_callbacks->confirm)
        return false;

    WKJStringArg textArg(text);
    return m_callbacks->confirm(m_pageRef, textArg.data(), textArg.length()) != 0;
}

bool ChromeClientJava::runJavaScriptPrompt(LocalFrame&, const String& text,
                                           const String& defaultValue, String& result)
{
    if (!m_callbacks || !m_callbacks->prompt)
        return false;

    WKJStringArg textArg(text);
    WKJStringArg defaultArg(defaultValue);

    Vector<char16_t, 256> buffer(256);
    int32_t length = 0;
    int32_t status = m_callbacks->prompt(m_pageRef, textArg.data(), textArg.length(),
        defaultArg.data(), defaultArg.length(),
        reinterpret_cast<uint16_t*>(buffer.data()), static_cast<int32_t>(buffer.size()), &length);

    if (status == WKJ_STR_OVERFLOW) {
        /* The callee serves the retry from the result it already has; the dialog is modal
           and must not be shown a second time. */
        buffer.resize(static_cast<size_t>(length));
        status = m_callbacks->prompt(m_pageRef, textArg.data(), textArg.length(),
            defaultArg.data(), defaultArg.length(),
            reinterpret_cast<uint16_t*>(buffer.data()), static_cast<int32_t>(buffer.size()), &length);
    }

    /* A cancelled prompt is WKJ_STR_NULL, which is the null string the JNI code tested. */
    if (status != WKJ_STR_OK)
        return false;

    result = String(std::span<const char16_t>(buffer.data(), static_cast<size_t>(length)));
    return true;
}

void ChromeClientJava::runOpenPanel(LocalFrame&, FileChooser& fileChooser)
{
    if (!m_callbacks || !m_callbacks->choose_file)
        return;

    StringBuilder builder;
    const Vector<String>& acceptTypeList = fileChooser.settings().acceptMIMETypes;
    for (unsigned i = 0; i < acceptTypeList.size(); ++i) {
        if (i > 0)
            builder.append(',');
        builder.append(acceptTypeList[i]);
    }

    String initialFilename;
    const Vector<String> &filenames = fileChooser.settings().selectedFiles;
    if (filenames.size() > 0) {
        initialFilename = filenames[0];
    }

    bool multiple = fileChooser.settings().allowsMultipleFiles;

    WKJStringArg initialArg(initialFilename);
    WKJStringArg mimeFiltersArg(builder.toString());

    /*
     * The chosen paths come back end to end in `chars`, with their lengths in `lengths`.
     * The first call runs the dialog; if either buffer was too small the callee reports
     * the sizes it needs and serves the retry from the result it already has, without
     * showing the modal dialog a second time.
     */
    Vector<char16_t> chars(1024);
    Vector<int32_t> lengths(16);
    int32_t requiredUnits = 0;
    int32_t count = m_callbacks->choose_file(m_pageRef,
        initialArg.data(), initialArg.length(), multiple ? 1 : 0,
        mimeFiltersArg.data(), mimeFiltersArg.length(),
        reinterpret_cast<uint16_t*>(chars.data()), static_cast<int32_t>(chars.size()),
        lengths.data(), static_cast<int32_t>(lengths.size()), &requiredUnits);

    /* A negative count is the null array for which the JNI code skipped chooseFiles. */
    if (count < 0)
        return;

    if (count > static_cast<int32_t>(lengths.size())
            || requiredUnits > static_cast<int32_t>(chars.size())) {
        chars.resize(static_cast<size_t>(requiredUnits));
        lengths.resize(static_cast<size_t>(count));
        count = m_callbacks->choose_file(m_pageRef,
            initialArg.data(), initialArg.length(), multiple ? 1 : 0,
            mimeFiltersArg.data(), mimeFiltersArg.length(),
            reinterpret_cast<uint16_t*>(chars.data()), static_cast<int32_t>(chars.size()),
            lengths.data(), static_cast<int32_t>(lengths.size()), &requiredUnits);
        if (count < 0)
            return;
    }

    Vector<String> files;
    files.reserveInitialCapacity(static_cast<size_t>(count));
    size_t offset = 0;
    for (int32_t i = 0; i < count; i++) {
        size_t length = static_cast<size_t>(lengths[i]);
        files.append(String(std::span<const char16_t>(chars.data() + offset, length)));
        offset += length;
    }
    fileChooser.chooseFiles(files);
}

void ChromeClientJava::loadIconForFiles(const Vector<String>& filenames, FileIconLoader& loader)
{
    loader.iconLoaded(Icon::createIconForFiles(filenames));
}

bool ChromeClientJava::canRunBeforeUnloadConfirmPanel()
{
    if (!m_callbacks || !m_callbacks->can_run_before_unload)
        return false;

    return m_callbacks->can_run_before_unload(m_pageRef) != 0;
}

bool ChromeClientJava::runBeforeUnloadConfirmPanel(String&& message, LocalFrame&)
{
    if (!m_callbacks || !m_callbacks->run_before_unload)
        return false;

    WKJStringArg messageArg(message);
    return m_callbacks->run_before_unload(m_pageRef, messageArg.data(), messageArg.length()) != 0;
}

void ChromeClientJava::addMessageToConsole(JSC::MessageSource, JSC::MessageLevel, const String& message,
    unsigned lineNumber, unsigned, const String& sourceID)
{
    if (!m_callbacks || !m_callbacks->add_message_to_console)
        return;

    WKJStringArg messageArg(message);
    WKJStringArg sourceArg(sourceID);
    m_callbacks->add_message_to_console(m_pageRef,
            messageArg.data(), messageArg.length(),
            static_cast<int32_t>(lineNumber),
            sourceArg.data(), sourceArg.length());
}

KeyboardUIMode ChromeClientJava::keyboardUIMode()
{
    return KeyboardAccessTabsToLinks;
}

void ChromeClientJava::mouseDidMoveOverElement(const HitTestResult&, OptionSet<PlatformEventModifier>, const String& toolTip, TextDirection)
{
    /*static Node* mouseOverNode = 0;
    Element* urlElement = htr.URLElement();
    if (urlElement && isDraggableLink(*urlElement)) {
        Node* overNode = htr.innerNode();
        URL url = htr.absoluteLinkURL();
        if (!url.isEmpty() && (overNode != mouseOverNode)) {
            setStatusbarText(url.string());
            mouseOverNode = overNode;
        }
    } else {
        if (mouseOverNode) {
            setStatusbarText(""_s);
            mouseOverNode = 0;
        }
    }*/ //REVISIT
    setToolTip(toolTip);
}

void ChromeClientJava::setToolTip(const String& toolTip)
{
    if (!m_callbacks || !m_callbacks->set_tooltip)
        return;

    /* An empty tooltip was passed as a null string, and that is what clears it. */
    WKJStringArg toolTipArg(toolTip.length() > 0 ? toolTip : String());
    m_callbacks->set_tooltip(m_pageRef, toolTipArg.data(), toolTipArg.length());
}

void ChromeClientJava::print(LocalFrame&, const StringWithDirection&)
{
    if (m_callbacks && m_callbacks->print)
        m_callbacks->print(m_pageRef);
}

void ChromeClientJava::exceededDatabaseQuota(LocalFrame&, const String&, DatabaseDetails) {
    notImplemented();
}

void ChromeClientJava::reachedMaxAppCacheSize(int64_t)
{
    // FIXME: Free some space.
    notImplemented();
}

void ChromeClientJava::attachRootGraphicsLayer(LocalFrame&, GraphicsLayer* layer)
{
    m_webPagePeer->setRootChildLayer(layer);
}

void ChromeClientJava::setNeedsOneShotDrawingSynchronization()
{
    m_webPagePeer->setNeedsOneShotDrawingSynchronization();
}

void ChromeClientJava::triggerRenderingUpdate()
{
    m_webPagePeer->scheduleRenderingUpdate();
}

void ChromeClientJava::attachViewOverlayGraphicsLayer(GraphicsLayer*)
{
    notImplemented();
}

// HostWindow interface
void ChromeClientJava::scroll(const IntSize& scrollDelta, const IntRect& rectToScroll, const IntRect& clipRect)
{
    m_webPagePeer->scroll(scrollDelta, rectToScroll, clipRect);
}

IntPoint ChromeClientJava::screenToRootView(const IntPoint& p) const
{
    /*
     * The WCPoint the JNI code allocated for the argument and read back for the result
     * collapses into two float in-parameters and two out-parameters. An absent slot leaves
     * the point unchanged, which is the documented default.
     */
    float outX = float(p.x());
    float outY = float(p.y());
    if (m_callbacks && m_callbacks->screen_to_window)
        m_callbacks->screen_to_window(m_pageRef, float(p.x()), float(p.y()), &outX, &outY);
    return IntPoint(int(outX), int(outY));
}

IntPoint ChromeClientJava::rootViewToScreen(const IntPoint& point) const
{
    return IntPoint();
}

bool ChromeClientJava::canShowDataListSuggestionLabels() const
{
    return false;
}

RefPtr<DateTimeChooser> ChromeClientJava::createDateTimeChooser(DateTimeChooserClient& client)
{
    return nullptr;
}

RefPtr<DataListSuggestionPicker> ChromeClientJava::createDataListSuggestionPicker(DataListSuggestionsClient& client)
{
    return nullptr;
}

IntRect ChromeClientJava::rootViewToScreen(const IntRect& r) const
{
    float outX = float(r.x());
    float outY = float(r.y());
    if (m_callbacks && m_callbacks->window_to_screen)
        m_callbacks->window_to_screen(m_pageRef, float(r.x()), float(r.y()), &outX, &outY);
    return IntRect(
        int(outX),
        int(outY),
        r.width(),
        r.height()
    );
}

IntPoint ChromeClientJava::accessibilityScreenToRootView(const WebCore::IntPoint& point) const
{
    return screenToRootView(point);
}

IntRect ChromeClientJava::rootViewToAccessibilityScreen(const WebCore::IntRect& rect) const
{
    return rootViewToScreen(rect);
}

void ChromeClientJava::intrinsicContentsSizeChanged(const IntSize&) const
{
    notImplemented();
}

/*
 * PlatformPageClient is a WKJHandle now (Source/WebCore/platform/Widget.h), so the last
 * JNI in this class is gone: get_host_window returns a new id for the WCWidget and the
 * handle owns it, which is what the local reference this used to return did.
 */
PlatformPageClient ChromeClientJava::platformPageClient() const
{
    if (!m_callbacks || !m_callbacks->get_host_window)
        return PlatformPageClient();

    PlatformPageClient hostWindow { m_callbacks->get_host_window(m_pageRef) };
    ASSERT(hostWindow);
    return hostWindow;
}

void ChromeClientJava::contentsSizeChanged(LocalFrame&, const IntSize&) const
{
    notImplemented();
}

void ChromeClientJava::invalidateRootView(const IntRect&)
{
    // Nothing to do here as all necessary repaints are scheduled
    // by ChromeClientJava::scroll(). See also JDK-8124810.
}

void ChromeClientJava::invalidateContentsAndRootView(const IntRect& updateRect)
{
    repaint(updateRect);
}

void ChromeClientJava::invalidateContentsForSlowScroll(const IntRect& updateRect)
{
    repaint(updateRect);
}

void ChromeClientJava::repaint(const IntRect& r)
{
    m_webPagePeer->repaint(r);
}

bool ChromeClientJava::selectItemWritingDirectionIsNatural()
{
    return false;
}

bool ChromeClientJava::selectItemAlignmentFollowsMenuWritingDirection()
{
    return true;
}


RefPtr<PopupMenu> ChromeClientJava::createPopupMenu(PopupMenuClient& client) const
{
    return adoptRef(new PopupMenuJava(&client));
}

RefPtr<SearchPopupMenu> ChromeClientJava::createSearchPopupMenu(PopupMenuClient& client) const
{
    return adoptRef(new SearchPopupMenuJava(&client));
}

// End of HostWindow methods

RefPtr<Icon> ChromeClientJava::createIconForFiles(const Vector<String>& filenames)
{
    return Icon::createIconForFiles(filenames);
}

void ChromeClientJava::requestCookieConsent(CompletionHandler<void(CookieConsentDecisionResult)>&&)
{
}

void ChromeClientJava::didFinishLoadingImageForElement(WebCore::HTMLImageElement&)
{
}

} // namespace WebCore
