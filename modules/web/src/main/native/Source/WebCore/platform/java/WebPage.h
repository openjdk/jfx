/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef WebPage_h
#define WebPage_h

#include <wtf/OwnPtr.h>
#include <wtf/PassOwnPtr.h>
#if USE(ACCELERATED_COMPOSITING)
#include "GraphicsLayerClient.h"
#endif
#include "IntRect.h"
#include "PrintContext.h"
#include "ScrollTypes.h"

#include <jni.h> // todo tav remove when building w/ pch
#include <JavaRef.h>
#include <UnicodeJava.h>

namespace WebCore {

class Frame;
class GraphicsContext;
class GraphicsLayer;
class IntRect;
class IntSize;
class Node;
class Page;
class PlatformKeyboardEvent;
class TextureMapper;

class WebPage
#if USE(ACCELERATED_COMPOSITING)
    : GraphicsLayerClient
#endif
{
public:
    WebPage(PassOwnPtr<Page> page);
    ~WebPage();

    inline Page* page()
    {
        return m_page.get();
    }

    static inline WebPage* webPageFromJLong(jlong p)
    {
        return static_cast<WebPage*>(jlong_to_ptr(p));
    }

    static WebPage* webPageFromJObject(const JLObject& obj);

    static inline Page* pageFromJLong(jlong p)
    {
        WebPage* webPage = webPageFromJLong(p);
        return webPage ? webPage->page() : NULL;
    }

    static inline Page* pageFromJObject(const JLObject& obj)
    {
        WebPage* webPage = webPageFromJObject(obj);
        return webPage ? webPage->page() : NULL;
    }

    static JLObject jobjectFromPage(Page* page);

    void setSize(const IntSize&);
    void prePaint();
    void paint(jobject, jint, jint, jint, jint);
    void postPaint(jobject, jint, jint, jint, jint);
    bool processKeyEvent(const PlatformKeyboardEvent& event);

    void scroll(const IntSize& scrollDelta, const IntRect& rectToScroll,
                const IntRect& clipRect);
    void repaint(const IntRect&);
    int beginPrinting(float width, float height);
    void print(GraphicsContext& gc, int pageIndex, float pageWidth);
    void endPrinting();
#if USE(ACCELERATED_COMPOSITING)
    void setRootChildLayer(GraphicsLayer*);
    void setNeedsOneShotDrawingSynchronization();
    void scheduleCompositingLayerSync();
#endif
    void debugStarted();
    void debugEnded();
    void enableWatchdog();
    void disableWatchdog();

private:
    void requestJavaRepaint(const IntRect&);
#if USE(ACCELERATED_COMPOSITING)
    void markForSync();
    void syncLayers();
    IntRect pageRect();
    void renderCompositedLayers(GraphicsContext&, const IntRect&);

    // GraphicsLayerClient
    virtual void notifyAnimationStarted(const GraphicsLayer*, double);
    virtual void notifyFlushRequired(const GraphicsLayer*);
    virtual void paintContents(const GraphicsLayer*,
                               GraphicsContext&,
                               GraphicsLayerPaintingPhase,
                               const FloatRect&);
    virtual bool showDebugBorders(const GraphicsLayer*) const;
    virtual bool showRepaintCounter(const GraphicsLayer*) const;
#endif

    bool keyEvent(const PlatformKeyboardEvent& event);
    bool charEvent(const PlatformKeyboardEvent& event);
    bool keyEventDefault(const PlatformKeyboardEvent& event);
    bool scrollViewWithKeyboard(int keyCode, int modifiers);
    static bool mapKeyCodeForScroll(int keyCode,
                                    ScrollDirection* scrollDirection,
                                    ScrollGranularity* scrollGranularity);
    bool propagateScroll(ScrollDirection scrollDirection,
                         ScrollGranularity scrollGranularity);
    Frame* focusedWebCoreFrame();
    Node* focusedWebCoreNode();

    OwnPtr<Page> m_page;
    OwnPtr<PrintContext> m_printContext;

#if USE(ACCELERATED_COMPOSITING)
    OwnPtr<GraphicsLayer> m_rootLayer;
    OwnPtr<TextureMapper> m_textureMapper;
    bool m_syncLayers;
#endif

    // Webkit expects keyPress events to be suppressed if the associated keyDown
    // event was handled. Safari implements this behavior by peeking out the
    // associated WM_CHAR event if the keydown was handled. We emulate
    // this behavior by setting this flag if the keyDown was handled.
    bool m_suppressNextKeypressEvent;

    bool m_isDebugging;
    static int globalDebugSessionCounter;
};

} // namespace WebCore

#endif // WebPage_h
