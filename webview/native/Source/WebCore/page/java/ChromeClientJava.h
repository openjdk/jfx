/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ChromeClientJava_h
#define ChromeClientJava_h

#include "ChromeClient.h"
#include "ResourceRequest.h"
#include "FloatRect.h"
#include "IntRect.h"
#include "JavaEnv.h"
#include "Page.h"
#if ENABLE(INPUT_COLOR)
#include "ColorChooser.h"
#endif

namespace WebCore {

    class ChromeClientJava : public ChromeClient {
    public:
        ChromeClientJava(const JLObject &webPage);
        virtual void chromeDestroyed();

        virtual void* webView() const { return reinterpret_cast<void *>((jobject)m_webPage); }
        virtual void setWindowRect(const FloatRect &);
        virtual FloatRect windowRect();
        virtual FloatRect pageRect();

        virtual float scaleFactor();

        virtual void focus();
        virtual void unfocus();
        virtual bool canTakeFocus(FocusDirection);
        virtual void takeFocus(FocusDirection);
        virtual void focusedFrameChanged(Frame*);
        virtual void focusedNodeChanged(Node*);

        virtual Page* createWindow(Frame*, const FrameLoadRequest&, const WindowFeatures&, const NavigationAction&);
        virtual void show();

        virtual bool canRunModal();
        virtual void runModal();

        virtual void setToolbarsVisible(bool);
        virtual bool toolbarsVisible();

        virtual void setStatusbarVisible(bool);
        virtual bool statusbarVisible();

        virtual void setScrollbarsVisible(bool);
        virtual bool scrollbarsVisible();

        virtual void setMenubarVisible(bool);
        virtual bool menubarVisible();

        virtual void setStatusbarText(const String &);

        virtual void setResizable(bool);

        virtual void addMessageToConsole(MessageSource, MessageType, MessageLevel, const String& message,
                                         unsigned int lineNumber, const String& sourceID);

        virtual bool canRunBeforeUnloadConfirmPanel();
        virtual bool runBeforeUnloadConfirmPanel(const String& message, Frame *frame);

        virtual void closeWindowSoon();

        virtual void runJavaScriptAlert(Frame*, const String&);
        virtual bool runJavaScriptConfirm(Frame*, const String&);
        virtual bool runJavaScriptPrompt(Frame*, const String& message, const String& defaultValue, String& result);
        virtual bool shouldInterruptJavaScript();
        virtual KeyboardUIMode keyboardUIMode();

        virtual IntRect windowResizerRect() const;
        virtual void scrollbarsModeDidChange() const;
        virtual void mouseDidMoveOverElement(const HitTestResult&, unsigned modifierFlags);
        virtual void setToolTip(const String &, TextDirection);
        virtual void print(Frame *);

        virtual void exceededDatabaseQuota(Frame*, const String& databaseName);
        virtual void reachedMaxAppCacheSize(int64_t spaceNeeded);
        virtual void reachedApplicationCacheOriginQuota(SecurityOrigin*, int64_t totalSpaceNeeded);
        // HostWindow methods
        virtual void scroll(const IntSize& scrollDelta, const IntRect& rectToScroll, const IntRect& clipRect);
        virtual IntPoint screenToRootView(const IntPoint&) const;
        virtual IntRect rootViewToScreen(const IntRect&) const;
        virtual PlatformPageClient platformPageClient() const;
        virtual void contentsSizeChanged(Frame*, const IntSize&) const;
        virtual void scrollRectIntoView(const IntRect&, const ScrollView*) const;

        virtual void invalidateRootView(const IntRect&, bool);
        virtual void invalidateContentsAndRootView(const IntRect&, bool);
        virtual void invalidateContentsForSlowScroll(const IntRect&, bool);
        // End of HostWindow methods

        // This can be either a synchronous or asynchronous call. The ChromeClient can display UI asking the user for permission
        // to use Geolocation.
        virtual void requestGeolocationPermissionForFrame(Frame*, Geolocation*);
        virtual void cancelGeolocationPermissionRequestForFrame(Frame*, Geolocation*);

        virtual void runOpenPanel(Frame*, PassRefPtr<FileChooser>) ;
        // Asynchronous request to load an icon for specified filenames.
        virtual void loadIconForFiles(const Vector<String>&, FileIconLoader*);
        virtual void formStateDidChange(const Node*);

#if USE(ACCELERATED_COMPOSITING)
        virtual void attachRootGraphicsLayer(Frame*, GraphicsLayer*);
        virtual void setNeedsOneShotDrawingSynchronization();
        virtual void scheduleCompositingLayerSync();
#endif

        virtual void setCursor(const Cursor&);
        virtual void setCursorHiddenUntilMouseMoves(bool);

        virtual bool selectItemWritingDirectionIsNatural() OVERRIDE;
        virtual bool selectItemAlignmentFollowsMenuWritingDirection() OVERRIDE;
        virtual bool hasOpenedPopup() const OVERRIDE { return false; }

        virtual PassRefPtr<PopupMenu> createPopupMenu(PopupMenuClient*) const;
        virtual PassRefPtr<SearchPopupMenu> createSearchPopupMenu(PopupMenuClient*) const;

#if ENABLE(CONTEXT_MENUS)
        virtual void showContextMenu() { }
#endif

#if ENABLE(INPUT_COLOR)
        virtual PassOwnPtr<ColorChooser> createColorChooser(ColorChooserClient*, const Color&) { return nullptr; };
#endif

#if ENABLE(TOUCH_EVENTS)
        virtual void needTouchEvents(bool) { }
        virtual void numTouchEventHandlersChanged(unsigned) OVERRIDE { }
#endif
        virtual bool shouldRunModalDialogDuringPageDismissal(const DialogType&, const String& dialogMessage, FrameLoader::PageDismissalType) const;

        virtual bool shouldRubberBandInDirection(ScrollDirection) const;
        virtual void numWheelEventHandlersChanged(unsigned);

        JLObject platformPage() { return m_webPage; } //utatodo:check usage

        //virtual PassOwnPtr<HTMLParserQuirks> createHTMLParserQuirks();
    private:
        void repaint(const IntRect&);

        JGObject m_webPage;
    };

} // namespace WebCore

#endif // ChromeClientJava_h
