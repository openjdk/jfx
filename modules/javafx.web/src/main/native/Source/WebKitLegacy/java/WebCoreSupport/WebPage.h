/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/OptionSet.h>
#include <WebCore/GraphicsLayerClient.h>
#include <WebCore/IntRect.h>
#include <WebCore/PrintContext.h>
#include <WebCore/ScrollTypes.h>
#include <WebCore/HandleUserInputEventResult.h>

#include "MediaPlayerPrivateJava.h"
#include "TextureMapperJavaAdapter.h"

#include <webkit_java_api_page.h>
#include <wtf/java/WKJHandle.h>

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
    : GraphicsLayerClient
{
public:
    WebPage(RefPtr<Page> page);
    ~WebPage();

    inline Page* page()
    {
        return m_page.get();
    }

    /* The `long pPage` the Java WebPage holds, as the WebPage it names. */
    static inline WebPage* webPageFromPeer(int64_t p)
    {
        return static_cast<WebPage*>(wkj_to_ptr(p));
    }

    static inline Page* pageFromPeer(int64_t p)
    {
        WebPage* webPage = webPageFromPeer(p);
        return webPage ? webPage->page() : NULL;
    }

    /*
     * A NEW id for the Java WebPage of `page`, owned by the caller - which is what the
     * local reference this used to return was. It comes from PageSupplementJava, which is
     * also where ScrollbarThemeJava, the URLLoader and the socket stream handle read it.
     */
    static WKJHandle jobjectFromPage(Page* page);

    /*
     * The callback tables and the registry id of the Java WebPage, installed once by
     * wkj_page_create. The id is retained here and released when the page is destroyed or
     * the tables are detached, which is the one retention that replaces the eight JNI
     * global references the clients used to hold on the same object.
     */
    void setCallbacks(const WKJPageCallbacks* callbacks, wkj_ref webPage);

    const WKJPageCallbacks* callbacks() const { return m_callbacks; }
    wkj_ref javaPage() const { return m_javaPage.get(); }

    void setSize(const IntSize&);
    void prePaint();
    /* `renderQueue` is a com.sun.webkit.graphics.WCRenderQueue registry id. */
    void paint(wkj_ref renderQueue, int32_t, int32_t, int32_t, int32_t);
    void postPaint(wkj_ref renderQueue, int32_t, int32_t, int32_t, int32_t);
    bool processKeyEvent(const PlatformKeyboardEvent& event);

    void scroll(const IntSize& scrollDelta, const IntRect& rectToScroll,
                const IntRect& clipRect);
    void repaint(const IntRect&);
    int beginPrinting(float width, float height);
    void print(GraphicsContext& gc, int pageIndex, float pageWidth);
    void endPrinting();
    void setRootChildLayer(GraphicsLayer*);
    void setNeedsOneShotDrawingSynchronization();
    void scheduleRenderingUpdate();
    void debugStarted();
    void debugEnded();
    void enableWatchdog();
    void disableWatchdog();

    RefPtr<RQRef> jRenderTheme();

private:
    void requestJavaRepaint(const IntRect&);
    void markForSync();
    void syncLayers();
    IntRect pageRect();
    void renderCompositedLayers(GraphicsContext&, const IntRect&);

    // GraphicsLayerClient
    void notifyAnimationStarted(const GraphicsLayer*, const String& /*animationKey*/, MonotonicTime /*time*/) override;
    void notifyFlushRequired(const GraphicsLayer*) override;
    void paintContents(const GraphicsLayer&, GraphicsContext&, const FloatRect& /* inClip */,  OptionSet<GraphicsLayerPaintBehavior>) override;

    bool keyEvent(const PlatformKeyboardEvent& event);
    bool charEvent(const PlatformKeyboardEvent& event);
    bool keyEventDefault(const PlatformKeyboardEvent& event);
    bool scrollViewWithKeyboard(int keyCode, const PlatformKeyboardEvent& event);
    static bool mapKeyCodeForScroll(int keyCode,
                                    ScrollDirection* scrollDirection,
                                    ScrollGranularity* scrollGranularity);
    bool propagateScroll(ScrollDirection scrollDirection,
                         ScrollGranularity scrollGranularity);
    LocalFrame* focusedWebCoreFrame();
    Node* focusedWebCoreNode();

    RefPtr<Page> m_page;
    const WKJPageCallbacks* m_callbacks { nullptr };
    WKJHandle m_javaPage;
    RefPtr<PrintContext> m_printContext;
    RefPtr<RQRef> m_jRenderTheme;

    RefPtr<GraphicsLayer> m_rootLayer;
    std::unique_ptr<TextureMapper> m_textureMapper;
    bool m_syncLayers { false };

    // Webkit expects keyPress events to be suppressed if the associated keyDown
    // event was handled. Safari implements this behavior by peeking out the
    // associated WM_CHAR event if the keydown was handled. We emulate
    // this behavior by setting this flag if the keyDown was handled.
    bool m_suppressNextKeypressEvent { false };

    bool m_isDebugging { false };
    static int globalDebugSessionCounter;
};

} // namespace WebCore
