/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ChromeClientJava_h
#define ChromeClientJava_h

#include "ChromeClient.h"
#include "JavaEnv.h"

namespace WebCore {

class ChromeClientJava : public ChromeClient {
public:
    ChromeClientJava(const JLObject &webPage);
    void chromeDestroyed() override;

    bool isJavaChromeClient() const override { return true; }
    void setWindowRect(const FloatRect&) override;
    FloatRect windowRect() override;

    FloatRect pageRect() override;

    void focus() override;
    void unfocus() override;

    bool canTakeFocus(FocusDirection) override;
    void takeFocus(FocusDirection) override;

    void focusedElementChanged(Element*) override;
    void focusedFrameChanged(Frame*) override;

    // The Frame pointer provides the ChromeClient with context about which
    // Frame wants to create the new Page. Also, the newly created window
    // should not be shown to the user until the ChromeClient of the newly
    // created Page has its show method called.
    // The FrameLoadRequest parameter is only for ChromeClient to check if the
    // request could be fulfilled. The ChromeClient should not load the request.
    Page* createWindow(Frame*, const FrameLoadRequest&, const WindowFeatures&, const NavigationAction&) override;
    void show() override;

    bool canRunModal() override;
    void runModal() override;

    void setToolbarsVisible(bool) override;
    bool toolbarsVisible() override;

    void setStatusbarVisible(bool) override;
    bool statusbarVisible() override;

    void setScrollbarsVisible(bool) override;
    bool scrollbarsVisible() override;

    void setMenubarVisible(bool) override;
    bool menubarVisible() override;

    void setResizable(bool) override;

    void addMessageToConsole(MessageSource, MessageLevel, const String& message, unsigned lineNumber, unsigned columnNumber, const String& sourceID) override;
    bool canRunBeforeUnloadConfirmPanel() override;
    bool runBeforeUnloadConfirmPanel(const String& message, Frame*) override;

    void closeWindowSoon() override;

    void runJavaScriptAlert(Frame*, const String&) override;
    bool runJavaScriptConfirm(Frame*, const String&) override;
    bool runJavaScriptPrompt(Frame*, const String& message, const String& defaultValue, String& result) override;
    void setStatusbarText(const String&) override;
    KeyboardUIMode keyboardUIMode() override;

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
    PlatformPageClient platformPageClient() const override;
    void scrollbarsModeDidChange() const override;
    void setCursor(const Cursor&) override;
    void setCursorHiddenUntilMouseMoves(bool) override;
    // End methods used by HostWindow.

    void contentsSizeChanged(Frame*, const IntSize&) const override;
    void mouseDidMoveOverElement(const HitTestResult&, unsigned modifierFlags) override;

    void setToolTip(const String&, TextDirection) override;

    void print(Frame*) override;

    void exceededDatabaseQuota(Frame*, const String& databaseName, DatabaseDetails) override;

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
    void reachedApplicationCacheOriginQuota(SecurityOrigin*, int64_t totalSpaceNeeded) override;

#if ENABLE(INPUT_TYPE_COLOR)
    std::unique_ptr<ColorChooser> createColorChooser(ColorChooserClient*, const Color&) override;
#endif

    void runOpenPanel(Frame*, PassRefPtr<FileChooser>) override;
    // Asynchronous request to load an icon for specified filenames.
    void loadIconForFiles(const Vector<String>&, FileIconLoader*) override;

#if ENABLE(DIRECTORY_UPLOAD)
    // Asychronous request to enumerate all files in a directory chosen by the user.
    void enumerateChosenDirectory(FileChooser*) override;
#endif

#if USE(ACCELERATED_COMPOSITING)
    // Allows ports to customize the type of graphics layers created by this page.
    GraphicsLayerFactory* graphicsLayerFactory() const { return 0; }

    // Pass 0 as the GraphicsLayer to detatch the root layer.
    void attachRootGraphicsLayer(Frame*, GraphicsLayer*) override;
    // Sets a flag to specify that the next time content is drawn to the window,
    // the changes appear on the screen in synchrony with updates to GraphicsLayers.
    void setNeedsOneShotDrawingSynchronization() override;
    // Sets a flag to specify that the view needs to be updated, so we need
    // to do an eager layout before the drawing.
    void scheduleCompositingLayerFlush() override;
#else //XXX: implement?
    void attachRootGraphicsLayer(Frame*, GraphicsLayer*) override {}
    void setNeedsOneShotDrawingSynchronization() override {}
    void scheduleCompositingLayerFlush() override {}
#endif
    void attachViewOverlayGraphicsLayer(Frame*, GraphicsLayer*) override;

#if ENABLE(TOUCH_EVENTS)
    void needTouchEvents(bool) override {};
#endif

    bool selectItemWritingDirectionIsNatural() override;
    bool selectItemAlignmentFollowsMenuWritingDirection() override;
    // Checks if there is an opened popup, called by RenderMenuList::showPopup().
    bool hasOpenedPopup() const override { return false; }
    RefPtr<PopupMenu> createPopupMenu(PopupMenuClient*) const override;
    RefPtr<SearchPopupMenu> createSearchPopupMenu(PopupMenuClient*) const override;

    void wheelEventHandlersChanged(bool) override {};

    JLObject platformPage() { return m_webPage; } //utatodo:check usage


private:
    void repaint(const IntRect&);
    JGObject m_webPage;
};

} // namespace WebCore

#endif // ChromeClientJava_h
