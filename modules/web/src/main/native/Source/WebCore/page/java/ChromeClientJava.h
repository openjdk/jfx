/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ChromeClientJava_h
#define ChromeClientJava_h

#include "ChromeClient.h"
#include "JavaEnv.h"

namespace WebCore {

    class ChromeClientJava : public ChromeClient {
    public:
        ChromeClientJava(const JLObject &webPage);
    virtual void chromeDestroyed() OVERRIDE;

    virtual void setWindowRect(const FloatRect&) OVERRIDE;
    virtual FloatRect windowRect() OVERRIDE;

    virtual FloatRect pageRect() OVERRIDE;

    virtual void focus() OVERRIDE;
    virtual void unfocus() OVERRIDE;

    virtual bool canTakeFocus(FocusDirection) OVERRIDE;
    virtual void takeFocus(FocusDirection) OVERRIDE;

    virtual void focusedNodeChanged(Node*) OVERRIDE;
    virtual void focusedFrameChanged(Frame*) OVERRIDE;

    // The Frame pointer provides the ChromeClient with context about which
    // Frame wants to create the new Page. Also, the newly created window
    // should not be shown to the user until the ChromeClient of the newly
    // created Page has its show method called.
    // The FrameLoadRequest parameter is only for ChromeClient to check if the
    // request could be fulfilled. The ChromeClient should not load the request.
    virtual Page* createWindow(Frame*, const FrameLoadRequest&, const WindowFeatures&, const NavigationAction&) OVERRIDE;
    virtual void show() OVERRIDE;

    virtual bool canRunModal() OVERRIDE;
    virtual void runModal() OVERRIDE;

    virtual void setToolbarsVisible(bool) OVERRIDE;
    virtual bool toolbarsVisible() OVERRIDE;

    virtual void setStatusbarVisible(bool) OVERRIDE;
    virtual bool statusbarVisible() OVERRIDE;

    virtual void setScrollbarsVisible(bool) OVERRIDE;
    virtual bool scrollbarsVisible() OVERRIDE;

    virtual void setMenubarVisible(bool) OVERRIDE;
    virtual bool menubarVisible() OVERRIDE;

    virtual void setResizable(bool) OVERRIDE;

    virtual void addMessageToConsole(MessageSource, MessageLevel, const String& message, unsigned lineNumber, unsigned columnNumber, const String& sourceID) OVERRIDE;
    virtual bool canRunBeforeUnloadConfirmPanel() OVERRIDE;
    virtual bool runBeforeUnloadConfirmPanel(const String& message, Frame*) OVERRIDE;

    virtual void closeWindowSoon() OVERRIDE;

    virtual void runJavaScriptAlert(Frame*, const String&) OVERRIDE;
    virtual bool runJavaScriptConfirm(Frame*, const String&) OVERRIDE;
    virtual bool runJavaScriptPrompt(Frame*, const String& message, const String& defaultValue, String& result) OVERRIDE;
    virtual void setStatusbarText(const String&) OVERRIDE;
    virtual bool shouldInterruptJavaScript() OVERRIDE;
    virtual KeyboardUIMode keyboardUIMode() OVERRIDE;

    virtual IntRect windowResizerRect() const OVERRIDE;

    // Methods used by HostWindow.
    //
    virtual void invalidateRootView(const IntRect&, bool immediate) OVERRIDE;
    virtual void invalidateContentsAndRootView(const IntRect&, bool immediate) OVERRIDE;
    virtual void invalidateContentsForSlowScroll(const IntRect&, bool immediate) OVERRIDE;
    virtual void scroll(const IntSize&, const IntRect&, const IntRect&) OVERRIDE;
#if USE(TILED_BACKING_STORE)
    virtual void delegatedScrollRequested(const IntPoint&) OVERRIDE;
#endif
    virtual IntPoint screenToRootView(const IntPoint&) const OVERRIDE;
    virtual IntRect rootViewToScreen(const IntRect&) const OVERRIDE;
    virtual PlatformPageClient platformPageClient() const OVERRIDE;
    virtual void scrollbarsModeDidChange() const OVERRIDE;
    virtual void setCursor(const Cursor&) OVERRIDE;
    virtual void setCursorHiddenUntilMouseMoves(bool) OVERRIDE;
#if ENABLE(REQUEST_ANIMATION_FRAME) && !USE(REQUEST_ANIMATION_FRAME_TIMER)
    virtual void scheduleAnimation() OVERRIDE;
#endif
    // End methods used by HostWindow.

    virtual void contentsSizeChanged(Frame*, const IntSize&) const OVERRIDE;
    virtual void mouseDidMoveOverElement(const HitTestResult&, unsigned modifierFlags) OVERRIDE;

    virtual void setToolTip(const String&, TextDirection) OVERRIDE;

    virtual void print(Frame*) OVERRIDE;
    virtual bool shouldRubberBandInDirection(ScrollDirection) const OVERRIDE;

#if ENABLE(SQL_DATABASE)
    virtual void exceededDatabaseQuota(Frame*, const String& databaseName, DatabaseDetails) OVERRIDE;
#endif

    // Callback invoked when the application cache fails to save a cache object
    // because storing it would grow the database file past its defined maximum
    // size or past the amount of free space on the device. 
    // The chrome client would need to take some action such as evicting some
    // old caches.
    virtual void reachedMaxAppCacheSize(int64_t spaceNeeded) OVERRIDE;

    // Callback invoked when the application cache origin quota is reached. This
    // means that the resources attempting to be cached via the manifest are
    // more than allowed on this origin. This callback allows the chrome client
    // to take action, such as prompting the user to ask to increase the quota
    // for this origin. The totalSpaceNeeded parameter is the total amount of
    // storage, in bytes, needed to store the new cache along with all of the
    // other existing caches for the origin that would not be replaced by
    // the new cache.
    virtual void reachedApplicationCacheOriginQuota(SecurityOrigin*, int64_t totalSpaceNeeded) OVERRIDE;

#if ENABLE(INPUT_TYPE_COLOR)
    virtual PassOwnPtr<ColorChooser> createColorChooser(ColorChooserClient*, const Color&) OVERRIDE;
#endif

#if ENABLE(DATE_AND_TIME_INPUT_TYPES)
    // This function is used for:
    //  - Mandatory date/time choosers if !ENABLE(INPUT_MULTIPLE_FIELDS_UI)
    //  - <datalist> UI for date/time input types regardless of
    //    ENABLE(INPUT_MULTIPLE_FIELDS_UI)
    virtual PassRefPtr<DateTimeChooser> openDateTimeChooser(DateTimeChooserClient*, const DateTimeChooserParameters&) OVERRIDE;
#endif

    virtual void runOpenPanel(Frame*, PassRefPtr<FileChooser>) OVERRIDE;
        // Asynchronous request to load an icon for specified filenames.
    virtual void loadIconForFiles(const Vector<String>&, FileIconLoader*) OVERRIDE;

#if ENABLE(DIRECTORY_UPLOAD)
    // Asychronous request to enumerate all files in a directory chosen by the user.
    virtual void enumerateChosenDirectory(FileChooser*) OVERRIDE;
#endif

    // Notification that the given form element has changed. This function
    // will be called frequently, so handling should be very fast.
    virtual void formStateDidChange(const Node*) OVERRIDE;

#if USE(ACCELERATED_COMPOSITING)
    // Allows ports to customize the type of graphics layers created by this page.
    virtual GraphicsLayerFactory* graphicsLayerFactory() const { return 0; }

    // Pass 0 as the GraphicsLayer to detatch the root layer.
    virtual void attachRootGraphicsLayer(Frame*, GraphicsLayer*) OVERRIDE;
    // Sets a flag to specify that the next time content is drawn to the window,
    // the changes appear on the screen in synchrony with updates to GraphicsLayers.
    virtual void setNeedsOneShotDrawingSynchronization() OVERRIDE;
    // Sets a flag to specify that the view needs to be updated, so we need
    // to do an eager layout before the drawing.
    virtual void scheduleCompositingLayerFlush() OVERRIDE;
#endif

#if ENABLE(TOUCH_EVENTS)
    virtual void needTouchEvents(bool) OVERRIDE {};
#endif

        virtual bool selectItemWritingDirectionIsNatural() OVERRIDE;
        virtual bool selectItemAlignmentFollowsMenuWritingDirection() OVERRIDE;
    // Checks if there is an opened popup, called by RenderMenuList::showPopup().
        virtual bool hasOpenedPopup() const OVERRIDE { return false; }
    virtual PassRefPtr<PopupMenu> createPopupMenu(PopupMenuClient*) const OVERRIDE;
    virtual PassRefPtr<SearchPopupMenu> createSearchPopupMenu(PopupMenuClient*) const OVERRIDE;

    virtual void numWheelEventHandlersChanged(unsigned) OVERRIDE {};

        JLObject platformPage() { return m_webPage; } //utatodo:check usage


    private:
        void repaint(const IntRect&);

        JGObject m_webPage;
    };

} // namespace WebCore

#endif // ChromeClientJava_h
