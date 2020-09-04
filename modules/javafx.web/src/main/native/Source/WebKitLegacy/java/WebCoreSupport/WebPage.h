/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <wtf/java/JavaRef.h>
#include <WebCore/GraphicsLayerClient.h>
#include <WebCore/IntRect.h>
#include <WebCore/PrintContext.h>
#include <WebCore/RQRef.h>
#include <WebCore/ScrollTypes.h>

#include <jni.h> // todo tav remove when building w/ pch

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
    WebPage(std::unique_ptr<Page> page);
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
    void setRootChildLayer(GraphicsLayer*);
    void setNeedsOneShotDrawingSynchronization();
    void scheduleCompositingLayerSync();
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
    void paintContents(const GraphicsLayer*, GraphicsContext&, const FloatRect& /* inClip */, GraphicsLayerPaintBehavior) override;

    bool keyEvent(const PlatformKeyboardEvent& event);
    bool charEvent(const PlatformKeyboardEvent& event);
    bool keyEventDefault(const PlatformKeyboardEvent& event);
    bool scrollViewWithKeyboard(int keyCode, const PlatformKeyboardEvent& event);
    static bool mapKeyCodeForScroll(int keyCode,
                                    ScrollDirection* scrollDirection,
                                    ScrollGranularity* scrollGranularity);
    bool propagateScroll(ScrollDirection scrollDirection,
                         ScrollGranularity scrollGranularity);
    Frame* focusedWebCoreFrame();
    Node* focusedWebCoreNode();

    std::unique_ptr<Page> m_page;
    std::unique_ptr<PrintContext> m_printContext;
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

