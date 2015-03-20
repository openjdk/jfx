/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ChromeClientJava_h
#define ChromeClientJava_h

#include "ChromeClient.h"
#include "JavaEnv.h"

namespace WebCore {

    class ChromeClientJava : public ChromeClient {
    public:
        ChromeClientJava(const JLObject &webPage);
    virtual void chromeDestroyed() override;

    virtual void setWindowRect(const FloatRect&) override;
    virtual FloatRect windowRect() override;

    virtual FloatRect pageRect() override;

    virtual void focus() override;
    virtual void unfocus() override;

    virtual bool canTakeFocus(FocusDirection) override;
    virtual void takeFocus(FocusDirection) override;

    virtual void focusedElementChanged(Element*) override;
    virtual void focusedFrameChanged(Frame*) override;

    // The Frame pointer provides the ChromeClient with context about which
    // Frame wants to create the new Page. Also, the newly created window
    // should not be shown to the user until the ChromeClient of the newly
    // created Page has its show method called.
    // The FrameLoadRequest parameter is only for ChromeClient to check if the
    // request could be fulfilled. The ChromeClient should not load the request.
    virtual Page* createWindow(Frame*, const FrameLoadRequest&, const WindowFeatures&, const NavigationAction&) override;
    virtual void show() override;

    virtual bool canRunModal() override;
    virtual void runModal() override;

    virtual void setToolbarsVisible(bool) override;
    virtual bool toolbarsVisible() override;

    virtual void setStatusbarVisible(bool) override;
    virtual bool statusbarVisible() override;

    virtual void setScrollbarsVisible(bool) override;
    virtual bool scrollbarsVisible() override;

    virtual void setMenubarVisible(bool) override;
    virtual bool menubarVisible() override;

    virtual void setResizable(bool) override;

    virtual void addMessageToConsole(MessageSource, MessageLevel, const String& message, unsigned lineNumber, unsigned columnNumber, const String& sourceID) override;
    virtual bool canRunBeforeUnloadConfirmPanel() override;
    virtual bool runBeforeUnloadConfirmPanel(const String& message, Frame*) override;

    virtual void closeWindowSoon() override;

    virtual void runJavaScriptAlert(Frame*, const String&) override;
    virtual bool runJavaScriptConfirm(Frame*, const String&) override;
    virtual bool runJavaScriptPrompt(Frame*, const String& message, const String& defaultValue, String& result) override;
    virtual void setStatusbarText(const String&) override;
    virtual bool shouldInterruptJavaScript() override;
    virtual KeyboardUIMode keyboardUIMode() override;

    virtual IntRect windowResizerRect() const override;

    // Methods used by HostWindow.
    //
    virtual void invalidateRootView(const IntRect&, bool immediate) override;
    virtual void invalidateContentsAndRootView(const IntRect&, bool immediate) override;
    virtual void invalidateContentsForSlowScroll(const IntRect&, bool immediate) override;
    virtual void scroll(const IntSize&, const IntRect&, const IntRect&) override;
#if USE(TILED_BACKING_STORE)
    virtual void delegatedScrollRequested(const IntPoint&) override;
#endif
    virtual IntPoint screenToRootView(const IntPoint&) const override;
    virtual IntRect rootViewToScreen(const IntRect&) const override;
    virtual PlatformPageClient platformPageClient() const override;
    virtual void scrollbarsModeDidChange() const override;
    virtual void setCursor(const Cursor&) override;
    virtual void setCursorHiddenUntilMouseMoves(bool) override;
    // End methods used by HostWindow.

    virtual void contentsSizeChanged(Frame*, const IntSize&) const override;
    virtual void mouseDidMoveOverElement(const HitTestResult&, unsigned modifierFlags) override;

    virtual void setToolTip(const String&, TextDirection) override;

    virtual void print(Frame*) override;

#if ENABLE(SQL_DATABASE)
    virtual void exceededDatabaseQuota(Frame*, const String& databaseName, DatabaseDetails) override;
#endif

    // Callback invoked when the application cache fails to save a cache object
    // because storing it would grow the database file past its defined maximum
    // size or past the amount of free space on the device. 
    // The chrome client would need to take some action such as evicting some
    // old caches.
    virtual void reachedMaxAppCacheSize(int64_t spaceNeeded) override;

    // Callback invoked when the application cache origin quota is reached. This
    // means that the resources attempting to be cached via the manifest are
    // more than allowed on this origin. This callback allows the chrome client
    // to take action, such as prompting the user to ask to increase the quota
    // for this origin. The totalSpaceNeeded parameter is the total amount of
    // storage, in bytes, needed to store the new cache along with all of the
    // other existing caches for the origin that would not be replaced by
    // the new cache.
    virtual void reachedApplicationCacheOriginQuota(SecurityOrigin*, int64_t totalSpaceNeeded) override;

#if ENABLE(INPUT_TYPE_COLOR)
    virtual PassOwnPtr<ColorChooser> createColorChooser(ColorChooserClient*, const Color&) override;
#endif

#if ENABLE(DATE_AND_TIME_INPUT_TYPES)
    // This function is used for:
    //  - Mandatory date/time choosers if !ENABLE(INPUT_MULTIPLE_FIELDS_UI)
    //  - <datalist> UI for date/time input types regardless of
    //    ENABLE(INPUT_MULTIPLE_FIELDS_UI)
    virtual PassRefPtr<DateTimeChooser> openDateTimeChooser(DateTimeChooserClient*, const DateTimeChooserParameters&) override;
#endif

    virtual void runOpenPanel(Frame*, PassRefPtr<FileChooser>) override;
        // Asynchronous request to load an icon for specified filenames.
    virtual void loadIconForFiles(const Vector<String>&, FileIconLoader*) override;

#if ENABLE(DIRECTORY_UPLOAD)
    // Asychronous request to enumerate all files in a directory chosen by the user.
    virtual void enumerateChosenDirectory(FileChooser*) override;
#endif

#if USE(ACCELERATED_COMPOSITING)
    // Allows ports to customize the type of graphics layers created by this page.
    virtual GraphicsLayerFactory* graphicsLayerFactory() const { return 0; }

    // Pass 0 as the GraphicsLayer to detatch the root layer.
    virtual void attachRootGraphicsLayer(Frame*, GraphicsLayer*) override;
    // Sets a flag to specify that the next time content is drawn to the window,
    // the changes appear on the screen in synchrony with updates to GraphicsLayers.
    virtual void setNeedsOneShotDrawingSynchronization() override;
    // Sets a flag to specify that the view needs to be updated, so we need
    // to do an eager layout before the drawing.
    virtual void scheduleCompositingLayerFlush() override;
#endif

#if ENABLE(TOUCH_EVENTS)
    virtual void needTouchEvents(bool) override {};
#endif

        virtual bool selectItemWritingDirectionIsNatural() override;
        virtual bool selectItemAlignmentFollowsMenuWritingDirection() override;
    // Checks if there is an opened popup, called by RenderMenuList::showPopup().
        virtual bool hasOpenedPopup() const override { return false; }
    virtual PassRefPtr<PopupMenu> createPopupMenu(PopupMenuClient*) const override;
    virtual PassRefPtr<SearchPopupMenu> createSearchPopupMenu(PopupMenuClient*) const override;

    virtual void numWheelEventHandlersChanged(unsigned) override {};

        JLObject platformPage() { return m_webPage; } //utatodo:check usage


    private:
        void repaint(const IntRect&);

        JGObject m_webPage;
    };

} // namespace WebCore

#endif // ChromeClientJava_h
