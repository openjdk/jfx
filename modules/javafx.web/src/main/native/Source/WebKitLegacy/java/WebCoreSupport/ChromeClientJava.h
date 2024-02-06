/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include <WebCore/ChromeClient.h>
#include <WebCore/PlatformJavaClasses.h>

namespace WebCore {

class ChromeClientJava final : public ChromeClient {
    WTF_MAKE_FAST_ALLOCATED;
public:
    ChromeClientJava(const JLObject &webPage);
    void chromeDestroyed() override;

    void setWindowRect(const FloatRect&) override;
    FloatRect windowRect() const override;

    FloatRect pageRect() const override;

    void focus() override;
    void unfocus() override;

    bool canTakeFocus(FocusDirection) const override;
    void takeFocus(FocusDirection) override;

    void focusedElementChanged(Element*) override;
    void focusedFrameChanged(LocalFrame*) override;

    // The Frame pointer provides the ChromeClient with context about which
    // Frame wants to create the new Page. Also, the newly created window
    // should not be shown to the user until the ChromeClient of the newly
    // created Page has its show method called.
    // The FrameLoadRequest parameter is only for ChromeClient to check if the
    // request could be fulfilled. The ChromeClient should not load the request.
    Page* createWindow(LocalFrame&, const WindowFeatures&, const NavigationAction&) override;
    void show() override;

    bool canRunModal() const override;
    void runModal() override;

    void setToolbarsVisible(bool) override;
    bool toolbarsVisible() const override;

    void setStatusbarVisible(bool) override;
    bool statusbarVisible() const override;

    void setScrollbarsVisible(bool) override;
    bool scrollbarsVisible() const override;

    void setMenubarVisible(bool) override;
    bool menubarVisible() const override;

    void setResizable(bool) override;

    void addMessageToConsole(MessageSource, MessageLevel, const String& message, unsigned lineNumber, unsigned columnNumber, const String& sourceID) override;
    bool canRunBeforeUnloadConfirmPanel() override;
    bool runBeforeUnloadConfirmPanel(const String& message, LocalFrame& Frame) override;

    void closeWindow() override;

    void runJavaScriptAlert(LocalFrame&, const String&) override;
    bool runJavaScriptConfirm(LocalFrame&, const String&) override;
    bool runJavaScriptPrompt(LocalFrame&, const String& message, const String& defaultValue, String& result) override;
    void setStatusbarText(const String&) override;
    KeyboardUIMode keyboardUIMode() override;

    bool hoverSupportedByPrimaryPointingDevice() const override { return true; }
    bool hoverSupportedByAnyAvailablePointingDevice() const override { return true; }
    std::optional<PointerCharacteristics> pointerCharacteristicsOfPrimaryPointingDevice() const override { return PointerCharacteristics::Fine; }
    OptionSet<PointerCharacteristics> pointerCharacteristicsOfAllAvailablePointingDevices() const override { return PointerCharacteristics::Fine; }

    // Methods used by HostWindow.
    //
    void invalidateRootView(const IntRect&) override;
    void invalidateContentsAndRootView(const IntRect&) override;
    void invalidateContentsForSlowScroll(const IntRect&) override;
    void scroll(const IntSize&, const IntRect&, const IntRect&) override;
#if USE(TILED_BACKING_STORE)
    void delegatedScrollRequested(const IntPoint&) override;
#endif
    IntPoint screenToRootView(const IntPoint&) const override;
    IntRect rootViewToScreen(const IntRect&) const override;
    IntPoint accessibilityScreenToRootView(const IntPoint&) const final;
    IntRect rootViewToAccessibilityScreen(const IntRect&) const final;
    void intrinsicContentsSizeChanged(const IntSize&) const final;
    PlatformPageClient platformPageClient() const override;
    void setCursor(const Cursor&) override;
    void setCursorHiddenUntilMouseMoves(bool) override;
    void setTextIndicator(const TextIndicatorData&) const override {}
    // End methods used by HostWindow.

    void contentsSizeChanged(LocalFrame&, const IntSize&) const override;
    void mouseDidMoveOverElement(const HitTestResult&, OptionSet<PlatformEventModifier>, const String& toolTip, TextDirection) override;

    void setToolTip(const String&) override;

    void print(LocalFrame&, const StringWithDirection&) override;

    void exceededDatabaseQuota(LocalFrame&, const String& databaseName, DatabaseDetails) override;

    // Callback invoked when the application cache fails to save a cache object
    // because storing it would grow the database file past its defined maximum
    // size or past the amount of free space on the device.
    // The chrome client would need to take some action such as evicting some
    // old caches.
    void reachedMaxAppCacheSize(int64_t spaceNeeded) override;

    // Callback invoked when the application cache origin quota is reached. This
    // means that the resources attempting to be cached via the manifest are
    // more than allowed on this origin. This callback allows the chrome clieMediaPlayerPrivateJava.cpp:314nt
    // to take action, such as prompting the user to ask to increase the quota
    // for this origin. The totalSpaceNeeded parameter is the total amount of
    // storage, in bytes, needed to store the new cache along with all of the
    // other existing caches for the origin that would not be replaced by
    // the new cache.
    void reachedApplicationCacheOriginQuota(SecurityOrigin&, int64_t totalSpaceNeeded) override;

#if ENABLE(INPUT_TYPE_COLOR)
    std::unique_ptr<ColorChooser> createColorChooser(ColorChooserClient&, const Color&) override;
#endif

    void runOpenPanel(LocalFrame&, FileChooser&) override;
    // Asynchronous request to load an icon for specified filenames.
    void loadIconForFiles(const Vector<String>&, FileIconLoader&) override;

#if ENABLE(DIRECTORY_UPLOAD)
    // Asychronous request to enumerate all files in a directory chosen by the user.
    void enumerateChosenDirectory(FileChooser*) override;
#endif

    GraphicsLayerFactory* graphicsLayerFactory() const override { return nullptr; }

    // Pass 0 as the GraphicsLayer to detatch the root layer.
    void attachRootGraphicsLayer(LocalFrame&, GraphicsLayer*) override;
    // Sets a flag to specify that the next time content is drawn to the window,
    // the changes appear on the screen in synchrony with updates to GraphicsLayers.
    void setNeedsOneShotDrawingSynchronization() override;
    // Sets a flag to specify that the view needs to be updated, so we need
    // to do an eager layout before the drawing.
    void triggerRenderingUpdate() override;
    void attachViewOverlayGraphicsLayer(GraphicsLayer*) override;

#if ENABLE(TOUCH_EVENTS)
    void needTouchEvents(bool) override {};
#endif

    bool selectItemWritingDirectionIsNatural() override;
    bool selectItemAlignmentFollowsMenuWritingDirection() override;
    RefPtr<PopupMenu> createPopupMenu(PopupMenuClient&) const override;
    RefPtr<SearchPopupMenu> createSearchPopupMenu(PopupMenuClient&) const override;

    void wheelEventHandlersChanged(bool) override {};

    RefPtr<Icon> createIconForFiles(const Vector<String>&) override;
    void didFinishLoadingImageForElement(HTMLImageElement&) override;
    void requestCookieConsent(CompletionHandler<void(CookieConsentDecisionResult)>&&) override;

private:
    void repaint(const IntRect&);
    JGObject m_webPage;
};

} // namespace WebCore
