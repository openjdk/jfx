/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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


#if COMPILER(GCC) || COMPILER(CLANG)
#pragma GCC diagnostic ignored "-Wunused-parameter"
#endif

#include "WebPage.h"

#include "BackForwardList.h"
#include "ChromeClientJava.h"
#include "ContextMenuClientJava.h"
#include "ContextMenuJava.h"
#include "DragClientJava.h"
#include "EditorClientJava.h"
#include "FrameLoaderClientJava.h"
#include "InspectorClientJava.h"
#include "PageStorageSessionProvider.h"
#include "PlatformStrategiesJava.h"
#include "ProgressTrackerClientJava.h"
#include "VisitedLinkStoreJava.h"
#include "WebKitLegacy/Storage/StorageNamespaceImpl.h"
#include "WebKitLegacy/Storage/WebDatabaseProvider.h"
#include "WebKitVersion.h" //generated
#include "WebPageConfig.h"
#include <WebCore/WebCoreTestSupport.h>
#include <JavaScriptCore/APICast.h>
#include <JavaScriptCore/InitializeThreading.h>
#include <JavaScriptCore/JSContextRef.h>
#include <JavaScriptCore/JSContextRefPrivate.h>
#include <JavaScriptCore/JSStringRef.h>
#include <JavaScriptCore/Options.h>
#include <WebCore/BackForwardController.h>
#include <WebCore/BridgeUtils.h>
#include <WebCore/CharacterData.h>
#include <WebCore/Chrome.h>
#include <WebCore/ColorTypes.h>
#include <WebCore/CompositionHighlight.h>
#include <WebCore/ContextMenu.h>
#include <WebCore/ContextMenuController.h>
#include <WebCore/CookieJar.h>
#include <WebCore/DeprecatedGlobalSettings.h>
#include <WebCore/Document.h>
#include <WebCore/DragController.h>
#include <WebCore/DragData.h>
#include <WebCore/Editor.h>
#include <WebCore/EmptyClients.h>
#include <WebCore/EventHandler.h>
#include <WebCore/FloatRect.h>
#include <WebCore/FloatSize.h>
#include <WebCore/FocusController.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameLoadRequest.h>
#include <WebCore/FrameTree.h>
#include <WebCore/FrameView.h>
#include <WebCore/GCController.h>
#include <WebCore/GeolocationClientMock.h>
#include <WebCore/GraphicsContext.h>
#include <WebCore/GraphicsLayerTextureMapper.h>
#include <WebCore/InspectorController.h>
#include <WebCore/KeyboardEvent.h>
#include <WebCore/LogInitialization.h>
#include <WebCore/NodeTraversal.h>
#include <WebCore/Page.h>
#include <WebCore/PageConfiguration.h>
#include <WebCore/PageSupplementJava.h>
#include <WebCore/PlatformContextJava.h>
#include <WebCore/PlatformJavaClasses.h>
#include <WebCore/PlatformKeyboardEvent.h>
#include <WebCore/PlatformMouseEvent.h>
#include <WebCore/PlatformTouchEvent.h>
#include <WebCore/PlatformWheelEvent.h>
#include <WebCore/RenderTreeAsText.h>
#include <WebCore/RenderView.h>
#include <WebCore/ResourceRequest.h>
#include <WebCore/RuntimeEnabledFeatures.h>
#include <WebCore/ScriptController.h>
#include <WebCore/SecurityPolicy.h>
#include <WebCore/Settings.h>
#include <WebCore/StorageNamespaceProvider.h>
#include <WebCore/TextIterator.h>
#include <WebCore/TextureMapperJava.h>
#include <WebCore/TextureMapperLayer.h>
#include <WebCore/WorkerThread.h>
#include <wtf/Ref.h>
#include <wtf/RunLoop.h>
#include <wtf/java/JavaRef.h>
#include <wtf/text/WTFString.h>

// FIXME: Move dependency of runtime_root to BridgeUtils
#include <WebCore/runtime_root.h>
#if OS(UNIX)
#include <sys/utsname.h>
#endif
#if OS(WINDOWS)
#include <WebCore/SystemInfo.h>
#endif


#include "com_sun_webkit_WebPage.h"
#include "com_sun_webkit_event_WCFocusEvent.h"
#include "com_sun_webkit_event_WCKeyEvent.h"
#include "com_sun_webkit_event_WCMouseEvent.h"

#if ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
#include <WebCore/NotificationController.h>
#include "NotificationClientJava.h"
#endif

namespace WebCore {

WebPage::WebPage(std::unique_ptr<Page> page)
    : m_page(WTFMove(page))
{
#if ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
    if(!NotificationController::from(m_page.get())) {
        provideNotification(m_page.get(), NotificationClientJava::instance());
    }
#endif
}

WebPage::~WebPage()
{
    debugEnded();
}

WebPage* WebPage::webPageFromJObject(const JLObject& oWebPage)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midGetPageMethod = env->GetMethodID(
        PG_GetWebPageClass(env),
        "getPage",
        "()J");
    ASSERT(midGetPageMethod);

    jlong p = env->CallLongMethod(oWebPage, midGetPageMethod);
    WTF::CheckAndClearException(env);

    return webPageFromJLong(p);
}

JLObject WebPage::jobjectFromPage(Page* page)
{
    if (!page)
        return nullptr;

    auto pageSupplement = PageSupplementJava::from(page);
    return pageSupplement ? pageSupplement->jWebPage() : nullptr;
}

void WebPage::setSize(const IntSize& size)
{
    Frame* mainFrame = (Frame*)&m_page->mainFrame();

    FrameView* frameView = mainFrame->view();
    if (!frameView) {
        return;
    }

    frameView->resize(size);
    frameView->layoutContext().scheduleLayout();

    if (m_rootLayer) {
        m_rootLayer->setSize(size);
        m_rootLayer->setNeedsDisplay();
    }
}

static void drawDebugLed(GraphicsContext& context,
                         const IntRect& rect,
                         const Color& color)
{
    const int w = 50;
    const int h = 50;
    FloatRect ledRect(
            rect.x() + rect.width() / 2 - w / 2,
            rect.y() + rect.height() / 2 - h / 2,
            w,
            h);
    context.fillRect(ledRect, color);
}

static void drawDebugBorder(GraphicsContext& context,
                            const IntRect& rect,
                            const Color& color,
                            int width)
{
    int x = rect.x();
    int y = rect.y();
    int w = rect.width();
    int h = rect.height();
    context.fillRect(FloatRect(x, y, w, width));
    context.fillRect(FloatRect(x, y + h - width, w, width), color);
    context.fillRect(FloatRect(x, y, width, h), color);
    context.fillRect(FloatRect(x + w - width, y, width, h), color);
}

void WebPage::prePaint() {
    if (m_rootLayer) {
        if (m_syncLayers) {
            m_syncLayers = false;
            syncLayers();
        }
        return;
    }

    Frame* mainFrame = (Frame*)&m_page->mainFrame();
    FrameView* frameView = mainFrame->view();
    if (frameView) {
        // Updating layout & styles precedes normal painting.
        frameView->updateLayoutAndStyleIfNeededRecursive();
    }
}

RefPtr<RQRef> WebPage::jRenderTheme()
{
    if (!m_jRenderTheme) {
        JNIEnv* env = WTF::GetJavaEnv();
        m_jRenderTheme = RQRef::create(PG_GetRenderThemeObjectFromPage(env, jobjectFromPage(m_page.get())));
    }
    return m_jRenderTheme;
}

void WebPage::paint(jobject rq, jint x, jint y, jint w, jint h)
{
    if (m_rootLayer) {
        return;
    }

    // DBG_CHECKPOINTEX("twkUpdateContent", 15, 100);

    RefPtr<Frame> mainFrame((Frame*)&m_page->mainFrame());
    RefPtr<FrameView> frameView(mainFrame->view());
    if (!frameView) {
        return;
    }

    // Will be deleted by GraphicsContext destructor
    PlatformContextJava* ppgc = new PlatformContextJava(rq, jRenderTheme());
    GraphicsContext gc(ppgc);

    // TODO: Following JS synchronization is not necessary for single thread model
    JSGlobalContextRef globalContext = toGlobalRef(mainFrame->script().globalObject(mainThreadNormalWorld()));
    JSC::JSLockHolder sw(toJS(globalContext)); // TODO-java: was JSC::APIEntryShim sw( toJS(globalContext) );

    frameView->paint(gc, IntRect(x, y, w, h));
    if (m_page->settings().showDebugBorders()) {
        drawDebugLed(gc, IntRect(x, y, w, h), SRGBA<uint8_t> { 0, 0, 255, 128 });
    }

    gc.platformContext()->rq().flushBuffer();
}

void WebPage::postPaint(jobject rq, jint x, jint y, jint w, jint h)
{
    if (!m_page->inspectorController().highlightedNode()
            && !m_rootLayer
    ) {
        return;
    }

    // Will be deleted by GraphicsContext destructor
    PlatformContextJava* ppgc = new PlatformContextJava(rq, jRenderTheme());
    GraphicsContext gc(ppgc);

    if (m_rootLayer) {
        if (m_syncLayers) {
            m_syncLayers = false;
            syncLayers();
        }
        renderCompositedLayers(gc, IntRect(x, y, w, h));
        if (m_page->settings().showDebugBorders()) {
            drawDebugLed(gc, IntRect(x, y, w, h), SRGBA<uint8_t> { 0, 192, 0, 128 });
        }
        if (downcast<GraphicsLayerTextureMapper>(m_rootLayer.get())->layer().descendantsOrSelfHaveRunningAnimations()) {
            requestJavaRepaint(pageRect());
        }
    }

    if (m_page->inspectorController().highlightedNode()) {
        m_page->inspectorController().drawHighlight(gc);
    }

    gc.platformContext()->rq().flushBuffer();
}

void WebPage::scroll(const IntSize& scrollDelta,
                     const IntRect& rectToScroll,
                     const IntRect& clipRect)
{
    if (m_rootLayer) {
        m_rootLayer->setNeedsDisplayInRect(rectToScroll);
        return;
    }

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            PG_GetWebPageClass(env),
            "fwkScroll",
            "(IIIIII)V");
    ASSERT(mid);

    env->CallVoidMethod(
            jobjectFromPage(m_page.get()),
            mid,
            rectToScroll.x(),
            rectToScroll.y(),
            rectToScroll.width(),
            rectToScroll.height(),
            scrollDelta.width(),
            scrollDelta.height());
    WTF::CheckAndClearException(env);
}

void WebPage::repaint(const IntRect& rect)
{
    if (m_rootLayer) {
        m_rootLayer->setNeedsDisplayInRect(rect);
    }
    requestJavaRepaint(rect);
}

void WebPage::requestJavaRepaint(const IntRect& rect)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            PG_GetWebPageClass(env),
            "fwkRepaint",
            "(IIII)V");
    ASSERT(mid);

    env->CallVoidMethod(
            jobjectFromPage(m_page.get()),
            mid,
            rect.x(),
            rect.y(),
            rect.width(),
            rect.height());
    WTF::CheckAndClearException(env);
}

void WebPage::setRootChildLayer(GraphicsLayer* layer)
{
    if (layer) {
        m_rootLayer = GraphicsLayer::create(nullptr, *this);
        m_rootLayer->setDrawsContent(true);
        m_rootLayer->setContentsOpaque(true);
        m_rootLayer->setSize(pageRect().size());
        m_rootLayer->setNeedsDisplay();
        m_rootLayer->addChild(*layer);

        m_textureMapper = TextureMapper::create();
        downcast<GraphicsLayerTextureMapper>(m_rootLayer.get())->layer()
                .setTextureMapper(m_textureMapper.get());
    } else {
        m_rootLayer = nullptr;
        m_textureMapper.reset();
    }
}

void WebPage::setNeedsOneShotDrawingSynchronization()
{
}

void WebPage::scheduleRenderingUpdate()
{
    markForSync();
}

void WebPage::markForSync()
{
    if (!m_rootLayer) {
        m_page->updateRendering();
        return;
    }
    m_syncLayers = true;
    requestJavaRepaint(pageRect());
}

void WebPage::syncLayers()
{
    if (!m_rootLayer) {
        return;
    }

    FrameView* frameView = m_page->mainFrame().view();
    if (!m_page->mainFrame().contentRenderer() || !frameView)
        return;

    frameView->updateLayoutAndStyleIfNeededRecursive();
    // Updating layout might have taken us out of compositing mode
    if (m_rootLayer) {
        m_rootLayer->flushCompositingStateForThisLayerOnly();
    }

    if (!frameView->flushCompositingStateIncludingSubframes())
        return;
}

IntRect WebPage::pageRect()
{
    ChromeClient& client = m_page->chrome().client();
    return IntRect(client.pageRect());
}

void WebPage::renderCompositedLayers(GraphicsContext& context, const IntRect& clip)
{
    ASSERT(m_rootLayer);
    ASSERT(m_textureMapper);

    TextureMapperLayer& rootTextureMapperLayer = downcast<GraphicsLayerTextureMapper>(*m_rootLayer).layer();

    static_cast<TextureMapperJava&>(*m_textureMapper).setGraphicsContext(&context);
    TransformationMatrix matrix;
    m_textureMapper->beginPainting();
    m_textureMapper->beginClip(matrix, FloatRoundedRect(clip));
    rootTextureMapperLayer.applyAnimationsRecursively(MonotonicTime::now());
    downcast<GraphicsLayerTextureMapper>(*m_rootLayer).updateBackingStoreIncludingSubLayers();
    rootTextureMapperLayer.paint();
    m_textureMapper->endClip();
    m_textureMapper->endPainting();
}

void WebPage::notifyAnimationStarted(const GraphicsLayer*, const String& /*animationKey*/, MonotonicTime /*time*/)
{
    ASSERT_NOT_REACHED();
}

void WebPage::notifyFlushRequired(const GraphicsLayer*)
{
    markForSync();
}

void WebPage::paintContents(const GraphicsLayer*, GraphicsContext& context, const FloatRect& inClip, GraphicsLayerPaintBehavior)
{
    context.save();
    context.clip(inClip);
    m_page->mainFrame().view()->paint(context, enclosingIntRect(inClip));
    if (m_page->settings().showDebugBorders()) {
        drawDebugBorder(context, roundedIntRect(inClip), SRGBA<uint8_t> { 0, 192, 0 }, 20);
    }
    context.restore();
}

bool WebPage::processKeyEvent(const PlatformKeyboardEvent& event)
{
    return event.type() == PlatformKeyboardEvent::Char
        ? charEvent(event)
        : keyEvent(event);
}

//
// The below keyboard event handling code was adapted from
// WebKit/chromium/src/WebViewImpl.cpp
//

static const int VKEY_PRIOR = com_sun_webkit_event_WCKeyEvent_VK_PRIOR;
static const int VKEY_NEXT = com_sun_webkit_event_WCKeyEvent_VK_NEXT;
static const int VKEY_END = com_sun_webkit_event_WCKeyEvent_VK_END;
static const int VKEY_HOME = com_sun_webkit_event_WCKeyEvent_VK_HOME;
static const int VKEY_LEFT = com_sun_webkit_event_WCKeyEvent_VK_LEFT;
static const int VKEY_UP = com_sun_webkit_event_WCKeyEvent_VK_UP;
static const int VKEY_RIGHT = com_sun_webkit_event_WCKeyEvent_VK_RIGHT;
static const int VKEY_DOWN = com_sun_webkit_event_WCKeyEvent_VK_DOWN;

bool WebPage::keyEvent(const PlatformKeyboardEvent& event)
{
    ASSERT((event.type() == PlatformKeyboardEvent::RawKeyDown)
        || (event.type() == PlatformKeyboardEvent::KeyDown)
        || (event.type() == PlatformKeyboardEvent::KeyUp));

    // Please refer to the comments explaining the m_suppressNextKeypressEvent
    // member.
    // The m_suppressNextKeypressEvent is set if the KeyDown is handled by
    // Webkit. A keyDown event is typically associated with a keyPress(char)
    // event and a keyUp event. We reset this flag here as this is a new keyDown
    // event.
    m_suppressNextKeypressEvent = false;

    RefPtr<Frame> frame = focusedWebCoreFrame();
    if (!frame)
        return false;

    EventHandler& handler = frame->eventHandler();

    if (handler.keyEvent(event)) {
        if (event.type() == PlatformKeyboardEvent::RawKeyDown) {
            // Suppress the next keypress event unless the focused node
            // is a plug-in node. (Flash needs these keypress events to
            // handle non-US keyboards.)
            Node* node = focusedWebCoreNode();
            if (!node || !node->renderer()
                    || !node->renderer()->isEmbeddedObject())
                m_suppressNextKeypressEvent = true;
        }
        return true;
    }

    return keyEventDefault(event);
}

bool WebPage::charEvent(const PlatformKeyboardEvent& event)
{
    ASSERT(event.type() == PlatformKeyboardEvent::Char);

    // Please refer to the comments explaining the m_suppressNextKeypressEvent
    // member.  The m_suppressNextKeypressEvent is set if the KeyDown is
    // handled by Webkit. A keyDown event is typically associated with a
    // keyPress(char) event and a keyUp event. We reset this flag here as it
    // only applies to the current keyPress event.
    bool suppress = m_suppressNextKeypressEvent;
    m_suppressNextKeypressEvent = false;

    Frame* frame = focusedWebCoreFrame();
    if (!frame)
        return suppress;

    EventHandler& handler = frame->eventHandler();

    if (!suppress && !handler.keyEvent(event))
        return keyEventDefault(event);

    return true;
}

bool WebPage::keyEventDefault(const PlatformKeyboardEvent& event)
{
    Frame* frame = focusedWebCoreFrame();
    if (!frame)
        return false;

    switch (event.type()) {
    case PlatformKeyboardEvent::RawKeyDown:
        if (event.modifiers() == PlatformKeyboardEvent::Modifier::ControlKey) {
            switch (event.windowsVirtualKeyCode()) {
            // Match FF behavior in the sense that Ctrl+home/end are the only
            // Ctrl // key combinations which affect scrolling. Safari is buggy
            // in the sense that it scrolls the page for all Ctrl+scrolling key
            // combinations. For e.g. Ctrl+pgup/pgdn/up/down, etc.
            case VKEY_HOME:
            case VKEY_END:
                break;
            default:
                return false;
            }
        }
        if (!event.shiftKey())
            return scrollViewWithKeyboard(event.windowsVirtualKeyCode(), event);
        break;
    default:
        break;
    }
    return false;
}

bool WebPage::scrollViewWithKeyboard(int keyCode, const PlatformKeyboardEvent& event)
{
    ScrollDirection scrollDirection;
    ScrollGranularity scrollGranularity;
#if OS(DARWIN)
    if (event.metaKey()) {
        if (keyCode == VKEY_UP)
            keyCode = VKEY_HOME;
        else if (keyCode == VKEY_DOWN)
            keyCode = VKEY_END;
    }
    if (event.altKey()) {
        if (keyCode == VKEY_UP)
            keyCode = VKEY_PRIOR;
        else if (keyCode == VKEY_DOWN)
            keyCode = VKEY_NEXT;
    }
#endif
    if (!mapKeyCodeForScroll(keyCode, &scrollDirection, &scrollGranularity))
        return false;
    return propagateScroll(scrollDirection, scrollGranularity);
}

bool WebPage::mapKeyCodeForScroll(int keyCode,
                                  ScrollDirection* scrollDirection,
                                  ScrollGranularity* scrollGranularity)
{
    switch (keyCode) {
    case VKEY_LEFT:
        *scrollDirection = ScrollLeft;
        *scrollGranularity = ScrollByLine;
        break;
    case VKEY_RIGHT:
        *scrollDirection = ScrollRight;
        *scrollGranularity = ScrollByLine;
        break;
    case VKEY_UP:
        *scrollDirection = ScrollUp;
        *scrollGranularity = ScrollByLine;
        break;
    case VKEY_DOWN:
        *scrollDirection = ScrollDown;
        *scrollGranularity = ScrollByLine;
        break;
    case VKEY_HOME:
        *scrollDirection = ScrollUp;
        *scrollGranularity = ScrollByDocument;
        break;
    case VKEY_END:
        *scrollDirection = ScrollDown;
        *scrollGranularity = ScrollByDocument;
        break;
    case VKEY_PRIOR:  // page up
        *scrollDirection = ScrollUp;
        *scrollGranularity = ScrollByPage;
        break;
    case VKEY_NEXT:  // page down
        *scrollDirection = ScrollDown;
        *scrollGranularity = ScrollByPage;
        break;
    default:
        return false;
    }

    return true;
}

bool WebPage::propagateScroll(ScrollDirection scrollDirection,
                              ScrollGranularity scrollGranularity)
{
    Frame* frame = focusedWebCoreFrame();
    if (!frame)
        return false;

    bool scrollHandled = frame->eventHandler().scrollOverflow(
            scrollDirection,
            scrollGranularity);
    Frame* currentFrame = frame;
    while (!scrollHandled && currentFrame) {
        scrollHandled = currentFrame->view()->scroll(scrollDirection,
                                                     scrollGranularity);
        currentFrame = currentFrame->tree().parent();
    }
    return scrollHandled;
}

Frame* WebPage::focusedWebCoreFrame()
{
    return &m_page->focusController().focusedOrMainFrame();
}

Node* WebPage::focusedWebCoreNode()
{
    Frame* frame = m_page->focusController().focusedFrame();
    if (!frame)
        return 0;

    Document* document = frame->document();
    if (!document)
        return 0;

    return (Node*)document->focusedElement();
}

//implemented in customized WebCore/page/java/DragControllerJava.cpp
void setCopyKeyState(bool _copyKeyIsDown);

static String agentOS()
{
#if OS(DARWIN)
#if CPU(X86) || CPU(X86_64)
    return "Macintosh; Intel Mac OS X";
#else
    return "Macintosh; PPC Mac OS X";
#endif
#elif OS(UNIX)
    struct utsname name;
    if (uname(&name) != -1)
        return makeString(name.sysname, ' ', name.machine);
#elif OS(WINDOWS)
    return windowsVersionForUAString();
#else
    notImplemented();
#endif
    return "Unknown";
}

static String defaultUserAgent()
{
    static const auto userAgentString = makeNeverDestroyed([] {
        String wkVersion = makeString(
                              WEBKIT_MAJOR_VERSION, ".", WEBKIT_MINOR_VERSION,
                              " (KHTML, like Gecko) JavaFX/", JAVAFX_RELEASE_VERSION,
                              " Safari/", WEBKIT_MAJOR_VERSION, ".",  WEBKIT_MINOR_VERSION);
        return makeString("Mozilla/5.0 (", agentOS(), ") AppleWebKit/", wkVersion);
    }());
    return userAgentString;
}

int WebPage::beginPrinting(float width, float height)
{
    Frame* frame = (Frame*)&m_page->mainFrame();
    if (!frame->document() || !frame->view())
        return 0;
    frame->document()->updateLayout();

    ASSERT(!m_printContext);
    m_printContext = std::unique_ptr<PrintContext>(new PrintContext(frame));
    m_printContext->begin(width, height);
    m_printContext->computePageRects(FloatRect(0, 0, width, height), 0, 0, 1, height);
    return m_printContext->pageCount();
}

void WebPage::endPrinting()
{
    ASSERT(m_printContext);
    if (!m_printContext)
        return;

    m_printContext->end();
    m_printContext.reset();
}

void WebPage::print(GraphicsContext& gc, int pageIndex, float pageWidth)
{
    ASSERT(m_printContext);
    ASSERT(pageIndex >= 0 && pageIndex < m_printContext->pageCount());

    if (!m_printContext || pageIndex < 0 || pageIndex >= (int)m_printContext->pageCount())
        return;

    gc.save();
    gc.translate(0, 0);
    m_printContext->spoolPage(gc, pageIndex, pageWidth);
    gc.restore();
    gc.platformContext()->rq().flushBuffer();
}

int WebPage::globalDebugSessionCounter = 0;

void WebPage::debugStarted() {
    if (!m_isDebugging) {
        m_isDebugging = true;
        globalDebugSessionCounter++;

        disableWatchdog();
    }
}
void WebPage::debugEnded() {
    if (m_isDebugging) {
        m_isDebugging = false;
        globalDebugSessionCounter--;

        enableWatchdog();
    }
}
void WebPage::enableWatchdog() {
    if (globalDebugSessionCounter == 0) {
        JSContextGroupRef contextGroup = toRef(&mainThreadNormalWorld().vm());
        JSContextGroupSetExecutionTimeLimit(contextGroup, 10, 0, 0);
    }
}

void WebPage::disableWatchdog() {
    if (globalDebugSessionCounter > 0) {
        JSContextGroupRef contextGroup = toRef(&(mainThreadNormalWorld().vm()));
        JSContextGroupClearExecutionTimeLimit(contextGroup);
    }
}

} // namespace WebCore

using namespace WebCore;
using namespace WTF;

class WebStorageNamespaceProviderJava final : public WebCore::StorageNamespaceProvider {
public:
    void setLocalStorageDatabasePath(const String& path) {
        m_localStorageDatabasePath = path;
    }
private:
    String m_localStorageDatabasePath;

    Ref<StorageNamespace> createSessionStorageNamespace(Page& page, unsigned quota) override
    {
        return WebKit::StorageNamespaceImpl::createSessionStorageNamespace(quota, page.sessionID());
    }

    Ref<StorageNamespace> createLocalStorageNamespace(unsigned quota, PAL::SessionID sessionID) override
    {
        return WebKit::StorageNamespaceImpl::getOrCreateLocalStorageNamespace(m_localStorageDatabasePath, quota, sessionID);
    }

    Ref<StorageNamespace> createTransientLocalStorageNamespace(SecurityOrigin&, unsigned quota, PAL::SessionID sessionID) override
    {
        // FIXME: A smarter implementation would create a special namespace type instead of just piggy-backing off
        // SessionStorageNamespace here.
        return WebKit::StorageNamespaceImpl::createSessionStorageNamespace(quota, sessionID);
    }
};

namespace {

bool s_useJIT;
bool s_useDFGJIT;
bool s_useCSS3D;

}  // namespace

extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkInitWebCore
    (JNIEnv* env, jclass self, jboolean useJIT, jboolean useDFGJIT, jboolean useCSS3D) {
    s_useJIT = useJIT;
    s_useDFGJIT = useDFGJIT;
    s_useCSS3D = useCSS3D;
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_WebPage_twkCreatePage
    (JNIEnv* env, jobject self, jboolean editable)
{
    // FIXME-java(JDK-8169950): Refactor the following WebCore module
    // initialization flow.
    JSC::initialize();
    WTF::initializeMainThread();
    // RT-17330: Allow local loads for substitute data, that is,
    // for content loaded with twkLoad
    WebCore::SecurityPolicy::setLocalLoadPolicy(
            WebCore::SecurityPolicy::AllowLocalLoadsForLocalAndSubstituteData);

    //DBG_CHECKPOINTEX("twkCreatePage", 3, 5);

    VisitedLinkStoreJava::setShouldTrackVisitedLinks(true);

#if !LOG_DISABLED
    initializeLogChannelsIfNecessary();
#endif
    WebCore::PlatformStrategiesJava::initialize();

    static std::once_flag initializeJSCOptions;
    std::call_once(initializeJSCOptions, [] {
        JSC::Options::useJIT() = s_useJIT;
        // Enable DFG only if JIT is enabled.
        JSC::Options::useDFGJIT() = s_useJIT && s_useDFGJIT;
    });

    JLObject jlself(self, true);

    //utaTODO: history agent implementation

    auto pc = pageConfigurationWithEmptyClients(PAL::SessionID::defaultSessionID());
    auto pageStorageSessionProvider = PageStorageSessionProvider::create();
    pc.cookieJar = CookieJar::create(pageStorageSessionProvider.copyRef());
    pc.chromeClient = new ChromeClientJava(jlself);
    pc.contextMenuClient = new ContextMenuClientJava(jlself);
    pc.editorClient = makeUniqueRef<EditorClientJava>(jlself);
    pc.dragClient = makeUnique<DragClientJava>(jlself);
    pc.inspectorClient = new InspectorClientJava(jlself);
    pc.databaseProvider = &WebDatabaseProvider::singleton();
    pc.storageNamespaceProvider = adoptRef(new WebStorageNamespaceProviderJava());
    pc.visitedLinkStore = VisitedLinkStoreJava::create();

    pc.loaderClientForMainFrame = makeUniqueRef<FrameLoaderClientJava>(jlself);
    pc.progressTrackerClient = makeUniqueRef<ProgressTrackerClientJava>(jlself);

    pc.backForwardClient = BackForwardList::create();
    auto page = std::make_unique<Page>(WTFMove(pc));
    // Associate PageSupplementJava instance which has WebPage java object.
    page->provideSupplement(PageSupplementJava::supplementName(), std::make_unique<PageSupplementJava>(self));
    pageStorageSessionProvider->setPage(*page);
#if ENABLE(GEOLOCATION)
    WebCore::provideGeolocationTo(page.get(), *new GeolocationClientMock());
#endif
    return ptr_to_jlong(new WebPage(WTFMove(page)));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkInit
    (JNIEnv* env, jobject self, jlong pPage, jboolean usePlugins, jfloat devicePixelScale)
{
    Page* page = WebPage::pageFromJLong(pPage);

    /* Initialization of the default settings */
    Settings& settings = page->settings();
    settings.setTextAreasAreResizable(true);
    settings.setLoadsImagesAutomatically(true);
    settings.setMinimumFontSize(0);
    settings.setMinimumLogicalFontSize(5);
    settings.setAcceleratedCompositingEnabled(s_useCSS3D);
    settings.setScriptEnabled(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setPluginsEnabled(usePlugins);
    settings.setDefaultFixedFontSize(13);
    settings.setDefaultFontSize(16);
    settings.setContextMenuEnabled(true);
    settings.setInputTypeColorEnabled(true);
    settings.setUserAgent(defaultUserAgent());
    settings.setMaximumHTMLParserDOMTreeDepth(180);
    settings.setXSSAuditorEnabled(true);
    settings.setInteractiveFormValidationEnabled(true);

    /* Using java logical fonts as defaults */
    settings.setSerifFontFamily("Serif");
    settings.setSansSerifFontFamily("SansSerif");
    settings.setFixedFontFamily("Monospaced");
    page->setDeviceScaleFactor(devicePixelScale);

    RuntimeEnabledFeatures::sharedFeatures().setLinkPrefetchEnabled(true);
    static_cast<FrameLoaderClientJava&>(page->mainFrame().loader().client())
                                            .setFrame(&page->mainFrame());

    page->mainFrame().init();

    JSContextGroupRef contextGroup = toRef(&(mainThreadNormalWorld().vm()));
    JSContextGroupSetExecutionTimeLimit(contextGroup, 10, 0, 0);

    WebPage::webPageFromJLong(pPage)->enableWatchdog();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkDestroyPage
    (JNIEnv* env, jobject self, jlong pPage)
{
    WebPage* webPage = WebPage::webPageFromJLong(pPage);
    if (!webPage) {
        return;
    }

    Frame* mainFrame = (Frame*)&webPage->page()->mainFrame();
    if (mainFrame) {
        mainFrame->loader().stopAllLoaders();
        mainFrame->loader().detachFromParent();
    }

    delete webPage;
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_WebPage_twkGetMainFrame
    (JNIEnv* env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return 0;
    }
    Frame* mainFrame = (Frame*)&page->mainFrame();
    if (!mainFrame) {
        return 0;
    }
    return ptr_to_jlong(mainFrame);
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_WebPage_twkGetParentFrame
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return 0;
    }
    Frame* parentFrame = frame->tree().parent();
    if (!parentFrame) {
        return 0;
    }
    return ptr_to_jlong(parentFrame);
}

JNIEXPORT jlongArray JNICALL Java_com_sun_webkit_WebPage_twkGetChildFrames
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return 0;
    }

    FrameTree& tree = frame->tree();

    jlongArray jArray = env->NewLongArray(tree.childCount());
    jlong *arr = env->GetLongArrayElements(jArray, 0);
    int i = 0;
    for (Frame* child = tree.firstChild(); child; child = child->tree().nextSibling()) {
        arr[i++] = ptr_to_jlong(child);
    }
    env->ReleaseLongArrayElements(jArray, arr, 0);

    return jArray;
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetName
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return 0;
    }
    return frame->tree().uniqueName().string().toJavaString(env).releaseLocal();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetURL
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->document()) {
        return 0;
    }
    Document* doc = frame->document();
    if (!doc) {
        return 0;
    }
    return doc->url().string().toJavaString(env).releaseLocal();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetInnerText
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return 0;
    }

    Document* document = frame->document();
    if (!document) {
        return 0;
    }

    Element* documentElement = document->documentElement();
    if (!documentElement) {
        return 0;
    }

    FrameView* frameView = frame->view();
    if (frameView && frameView->layoutContext().isLayoutPending()) {
        frameView->layoutContext().layout();
    }

    return documentElement->innerText().toJavaString(env).releaseLocal();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetRenderTree
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->contentRenderer()) {
        return 0;
    }

    FrameView* frameView = frame->view();
    if (frameView && frameView->layoutContext().isLayoutPending()) {
        frameView->layoutContext().layout();
    }

    return externalRepresentation(frame).toJavaString(env).releaseLocal();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetContentType
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->loader().documentLoader()) {
        return 0;
    }
    return frame->loader().documentLoader()->responseMIMEType().toJavaString(env).releaseLocal();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetTitle
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->document()) {
        return 0;
    }
    return frame->document()->title().toJavaString(env).releaseLocal();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetIconURL
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return 0;
    }
#if ENABLE(ICONDATABASE)
    return frame->loader()->icon()->url().string().toJavaString(env).releaseLocal();
#else
    return 0;
#endif
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkOpen
    (JNIEnv* env, jobject self, jlong pFrame, jstring url)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return;
    }

    static const URL emptyParent;

    FrameLoadRequest frameLoadRequest(
        *frame, ResourceRequest(URL(emptyParent, String(env, url))));
    frameLoadRequest.setIsRequestFromClientOrUserInput();
    frame->loader().load(WTFMove(frameLoadRequest));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkLoad
    (JNIEnv* env, jobject self, jlong pFrame, jstring text, jstring contentType)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return;
    }

    const char* stringChars = env->GetStringUTFChars(text, JNI_FALSE);
    size_t stringLen = (size_t)env->GetStringUTFLength(text);
    RefPtr<SharedBuffer> buffer = SharedBuffer::create(stringChars, (int)stringLen);

    static const URL emptyUrl({ }, "");
    ResourceResponse response(URL(), String(env, contentType), stringLen, "UTF-8");
    FrameLoadRequest frameLoadRequest(
        *frame,
        ResourceRequest(emptyUrl),
        SubstituteData(
            WTFMove(buffer),
            URL(),
            response,
            SubstituteData::SessionHistoryVisibility::Visible) // TODO-java: or Hidden?
    );
    frameLoadRequest.setIsRequestFromClientOrUserInput();
    frame->loader().load(WTFMove(frameLoadRequest));

    env->ReleaseStringUTFChars(text, stringChars);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkIsLoading
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));

    return bool_to_jbool(frame && frame->loader().isLoading());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkStop
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return;
    }

    frame->loader().stopAllLoaders();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkStopAll
    (JNIEnv* env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return;
    }

    page->mainFrame().loader().stopAllLoaders();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkRefresh
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return;
    }

    frame->loader().reload(ReloadOption::FromOrigin);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkGoBackForward
    (JNIEnv* env, jobject self, jlong pPage, jint distance)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return JNI_FALSE;
    }

    if (page->backForward().canGoBackOrForward(distance)) {
        page->backForward().goBackOrForward(distance);
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkCopy
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return JNI_FALSE;
    }

    if (frame->editor().canCopy()) {
        frame->editor().copy();
        return JNI_TRUE;
    }

    return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkFindInPage
    (JNIEnv* env, jobject self, jlong pPage,
     jstring toFind, jboolean forward, jboolean wrap, jboolean matchCase)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (page) {
        FindOptions opts;
        if (!matchCase)
            opts.add(CaseInsensitive);
        if (!forward)
            opts.add(Backwards);
        if (wrap)
            opts.add(WrapAround);
        return bool_to_jbool(page->findString(String(env, toFind), opts));
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkFindInFrame
    (JNIEnv* env, jobject self, jlong pFrame,
     jstring toFind, jboolean forward, jboolean wrap, jboolean matchCase)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (frame) {
        //utatodo: support for the rest of FindOptionFlag
        FindOptions opts;
        if (!matchCase)
            opts.add(CaseInsensitive);
        if (!forward)
            opts.add(Backwards);
        if (wrap)
            opts.add(WrapAround);
        return bool_to_jbool(frame->page()->findString(
            String(env, toFind), opts | StartInSelection));
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkOverridePreference
    (JNIEnv* env, jobject self, jlong pPage, jstring propertyName, jstring propertyValue)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return;
    }

    Settings& settings = page->settings();
    String nativePropertyName(env, propertyName);
    String nativePropertyValue(env, propertyValue);

    if (nativePropertyName == "WebKitTextAreasAreResizable") {
        settings.setTextAreasAreResizable(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitLoadsImagesAutomatically") {
        settings.setLoadsImagesAutomatically(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitMinimumFontSize") {
        settings.setMinimumFontSize(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitMinimumLogicalFontSize") {
        settings.setMinimumLogicalFontSize(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitAcceleratedCompositingEnabled") {
        settings.setAcceleratedCompositingEnabled(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitScriptEnabled") {
        settings.setScriptEnabled(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitJavaScriptCanOpenWindowsAutomatically") {
        settings.setJavaScriptCanOpenWindowsAutomatically(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitPluginsEnabled") {
        settings.setPluginsEnabled(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitDefaultFixedFontSize") {
        settings.setDefaultFixedFontSize(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitContextMenuEnabled") {
        settings.setContextMenuEnabled(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitUserAgent") {
        settings.setUserAgent(nativePropertyValue);
    } else if (nativePropertyName == "WebKitMaximumHTMLParserDOMTreeDepth") {
        settings.setMaximumHTMLParserDOMTreeDepth(nativePropertyValue.toUInt());
    } else if (nativePropertyName == "WebKitXSSAuditorEnabled")  {
        settings.setXSSAuditorEnabled(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitSerifFontFamily") {
        settings.setSerifFontFamily(nativePropertyValue);
    } else if (nativePropertyName == "WebKitSansSerifFontFamily") {
        settings.setSansSerifFontFamily(nativePropertyValue);
    } else if (nativePropertyName == "WebKitFixedFontFamily") {
        settings.setFixedFontFamily(nativePropertyValue);
    } else if (nativePropertyName == "WebKitShowsURLsInToolTips") {
        settings.setShowsURLsInToolTips(nativePropertyValue.toInt());
    } else if (nativePropertyName == "WebKitJavaScriptCanAccessClipboardPreferenceKey") {
        settings.setJavaScriptCanAccessClipboard(nativePropertyValue.toInt() != 0);
    } else if (nativePropertyName == "allowTopNavigationToDataURLs") {
        settings.setAllowTopNavigationToDataURLs(nativePropertyValue == "true");
    } else if (nativePropertyName == "enableBackForwardCache") {
        settings.setUsesBackForwardCache(nativePropertyValue == "true");
    } else if (nativePropertyName == "enableColorFilter") {
        settings.setColorFilterEnabled(nativePropertyValue == "true");
    } else if (nativePropertyName == "enableKeygenElement") {
        // removed from Chrome, Firefox, and the HTML specification in 2017.
        // https://trac.webkit.org/changeset/248960/webkit
        RuntimeEnabledFeatures::sharedFeatures().setKeygenElementEnabled(nativePropertyValue == "true");
    } else if (nativePropertyName == "experimental:WebAnimationsCSSIntegrationEnabled") {
        RuntimeEnabledFeatures::sharedFeatures().setWebAnimationsCSSIntegrationEnabled(nativePropertyValue == "true");
    } else if (nativePropertyName == "experimental:CSSCustomPropertiesAndValuesEnabled") {
        RuntimeEnabledFeatures::sharedFeatures().setCSSCustomPropertiesAndValuesEnabled(nativePropertyValue == "true");
    } else if (nativePropertyName == "enableIntersectionObserver") {
#if ENABLE(INTERSECTION_OBSERVER)
        RuntimeEnabledFeatures::sharedFeatures().setIntersectionObserverEnabled(nativePropertyValue == "true");
#endif
    } else if (nativePropertyName == "experimental:RequestIdleCallbackEnabled") {
        settings.setRequestIdleCallbackEnabled(nativePropertyValue == "true");
    } else if (nativePropertyName == "jscOptions" && !nativePropertyValue.isEmpty()) {
        JSC::Options::setOptions(nativePropertyValue.utf8().data());
    }
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkResetToConsistentStateBeforeTesting
    (JNIEnv* env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return;
    }

    Settings& settings = page->settings();

    settings.setAllowUniversalAccessFromFileURLs(true);
    settings.setAllowFileAccessFromFileURLs(true);
    // settings.setStandardFontFamily(standardFamily);
    // settings.setFixedFontFamily(fixedFamily);
    // settings.setSerifFontFamily(standardFamily);
    // settings.setSansSerifFontFamily(sansSerifFamily);
    // settings.setCursiveFontFamily(cursiveFamily);
    // settings.setFantasyFontFamily(fantasyFamily);
    // settings.setPictographFontFamily(pictographFamily);
    settings.setDefaultFontSize(16);
    settings.setDefaultFixedFontSize(13);
    settings.setMinimumFontSize(0);
    settings.setDefaultTextEncodingName("ISO-8859-1");
    settings.setJavaEnabled(false);
    settings.setFullScreenEnabled(true);
    settings.setScriptEnabled(true);
    settings.setEditableLinkBehavior(EditableLinkOnlyLiveWithShiftKey);
    // settings.setTabsToLinks(false);
    settings.setDOMPasteAllowed(true);
    settings.setShouldPrintBackgrounds(true);
    // settings.setCacheModel(WebCacheModelDocumentBrowser);
    settings.setXSSAuditorEnabled(false);
    settings.setExperimentalNotificationsEnabled(false);
    settings.setPluginsEnabled(true);
    settings.setTextAreasAreResizable(true);
    settings.setUsesBackForwardCache(false);
    settings.setCSSOMViewScrollingAPIEnabled(true);
    settings.setRequestIdleCallbackEnabled(true);

    // settings.setPrivateBrowsingEnabled(false);
    settings.setAllowTopNavigationToDataURLs(true);
    settings.setAuthorAndUserStylesEnabled(true);
    // Shrinks standalone images to fit: YES
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setJavaScriptCanAccessClipboard(true);
    settings.setOfflineWebApplicationCacheEnabled(true);
    // settings.setDeveloperExtrasEnabled(false);
    settings.setJavaScriptRuntimeFlags(JSC::RuntimeFlags(0));
    // Set JS experiments enabled: YES
    settings.setLoadsImagesAutomatically(true);
    settings.setLoadsSiteIconsIgnoringImageLoadingSetting(false);
    settings.setFrameFlattening(FrameFlattening::Disabled);
    settings.setFontRenderingMode(FontRenderingMode::Normal);
    // Doesn't work well with DRT
    settings.setScrollAnimatorEnabled(false);
    // Set spatial navigation enabled: NO

    // Set WebGL Enabled: NO
    // settings.setCSSRegionsEnabled(true);
    // Set uses HTML5 parser quirks: NO
    // Async spellcheck: NO
    DeprecatedGlobalSettings::setMockScrollbarsEnabled(true);

    RuntimeEnabledFeatures::sharedFeatures().setHighlightAPIEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setFetchAPIEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setShadowDOMEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setCustomElementsEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setModernMediaControlsEnabled(false);
    RuntimeEnabledFeatures::sharedFeatures().setResourceTimingEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setUserTimingEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setDataTransferItemsEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setInspectorAdditionsEnabled(true);
    RuntimeEnabledFeatures::sharedFeatures().setWebAnimationsEnabled(true);
    // RuntimeEnabledFeatures::sharedFeatures().clearNetworkLoaderSession();

    Frame& coreFrame = page->mainFrame();
    auto globalContext = toGlobalRef(coreFrame.script().globalObject(mainThreadNormalWorld()));
    WebCoreTestSupport::resetInternalsObject(globalContext);
}

JNIEXPORT jfloat JNICALL Java_com_sun_webkit_WebPage_twkGetZoomFactor
    (JNIEnv* env, jobject self, jlong pFrame, jboolean textOnly)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    ASSERT(frame);
    if (!frame) {
        return 1.0;
    }
    return textOnly
        ? frame->textZoomFactor()
        : frame->pageZoomFactor();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetZoomFactor
    (JNIEnv* env, jobject self, jlong pFrame, jfloat zoomFactor, jboolean textOnly)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    ASSERT(frame);
    if (!frame) {
        return;
    }
    if (textOnly) {
        frame->setTextZoomFactor(zoomFactor);
    } else {
        frame->setPageZoomFactor(zoomFactor);
    }
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_WebPage_twkExecuteScript
    (JNIEnv* env, jobject self, jlong pFrame, jstring script)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return nullptr;
    }
    JSGlobalContextRef globalContext = getGlobalContext(&frame->script());
    RefPtr<JSC::Bindings::RootObject> rootObject(frame->script().createRootObject(frame));
    return WebCore::executeScript(
        env,
        nullptr,
        globalContext,
        rootObject.get(),
        script);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkAddJavaScriptBinding
    (JNIEnv* env, jobject self, jlong pFrame, jstring name, jobject value, jobject accessControlContext)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return;
    }
    JSGlobalContextRef globalContext = getGlobalContext(&frame->script());
    JSObjectRef window = JSContextGetGlobalObject(globalContext);
    RefPtr<JSC::Bindings::RootObject> rootObject(frame->script().createRootObject(frame));

    JSValueRef jsval = WebCore::Java_Object_to_JSValue(
        env,
        globalContext,
        rootObject.get(),
        value, accessControlContext);

    JSStringRef jsname = asJSStringRef(env, name);
    JSValueRef exception;
    if (JSValueIsUndefined(globalContext, jsval)) {
        JSObjectDeleteProperty(globalContext, window, jsname, &exception);
    } else {
        JSPropertyAttributes attributes = 0;
        JSObjectSetProperty(globalContext, window, jsname, jsval, attributes, &exception);
    }
    JSStringRelease(jsname);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkReset
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return;
    }

    frame->tree().clearName();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkBeginPrinting
    (JNIEnv* env, jobject self, jlong pPage, jfloat width, jfloat height)
{
    return WebPage::webPageFromJLong(pPage)->beginPrinting(width, height);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkEndPrinting
    (JNIEnv* env, jobject self, jlong pPage)
{
    return WebPage::webPageFromJLong(pPage)->endPrinting();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkPrint
    (JNIEnv* env, jobject self, jlong pPage, jobject rq, jint pageIndex, jfloat width)
{
    auto webPage = WebPage::webPageFromJLong(pPage);
    PlatformContextJava* ppgc = new PlatformContextJava(rq, webPage->jRenderTheme());
    GraphicsContext gc(ppgc);
    webPage->print(gc, pageIndex, width);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkGetFrameHeight
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->contentRenderer()) {
        return 0;
    }

    return frame->contentRenderer()->viewLogicalHeight();
/*
    bool isFrameSet = frame->document() && frame->document()->isFrameSet();
    if (isFrameSet) {
        RenderView* root = static_cast<RenderView*>(frame->document()->renderer());
        return root->bottomLayoutOverflow();
    } else {
        return frame->contentRenderer()->bottomLayoutOverflow();
    }
*/
}

JNIEXPORT jfloat JNICALL Java_com_sun_webkit_WebPage_twkAdjustFrameHeight
    (JNIEnv* env, jobject self, jlong pFrame,
     jfloat oldTop, jfloat oldBottom, jfloat bottomLimit)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->view()) {
        return oldBottom;
    }

    float result;
    frame->view()->adjustPageHeightDeprecated(&result, oldTop, oldBottom, bottomLimit);
    return result;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetBounds
    (JNIEnv* env, jobject self, jlong pPage, jint x, jint y, jint w, jint h)
{
    WebPage::webPageFromJLong(pPage)->setSize(IntSize(w, h));
}

JNIEXPORT jintArray JNICALL Java_com_sun_webkit_WebPage_twkGetVisibleRect
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->view()) {
        return nullptr;
    }
    IntRect rect = frame->view()->visibleContentRect();

    jintArray result = env->NewIntArray(4);
    WTF::CheckAndClearException(env);

    jint* arr = (jint*)env->GetPrimitiveArrayCritical(result, nullptr);
    arr[0] = rect.x();
    arr[1] = rect.y();
    arr[2] = rect.width();
    arr[3] = rect.height();
    env->ReleasePrimitiveArrayCritical(result, arr, 0);

    return result;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkScrollToPosition
    (JNIEnv* env, jobject self, jlong pFrame, jint x, jint y)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->view()) {
        return;
    }
    frame->view()->setScrollPosition(IntPoint(x, y));
}

JNIEXPORT jintArray JNICALL Java_com_sun_webkit_WebPage_twkGetContentSize
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->view()) {
        return nullptr;
    }
    IntSize size = frame->view()->contentsSize();

    jintArray result = env->NewIntArray(2);
    WTF::CheckAndClearException(env);

    jint* arr = (jint*)env->GetPrimitiveArrayCritical(result, nullptr);
    arr[0] = size.width();
    arr[1] = size.height();
    env->ReleasePrimitiveArrayCritical(result, arr, 0);

    return result;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetTransparent
(JNIEnv* env, jobject self, jlong pFrame, jboolean isTransparent)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->view()) {
        return;
    }
    frame->view()->setTransparent(isTransparent);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetBackgroundColor
(JNIEnv* env, jobject self, jlong pFrame, jint backgroundColor)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame || !frame->view()) {
        return;
    }
    frame->view()->setBaseBackgroundColor(asSRGBA(WebCore::Packed::RGBA { static_cast<uint32_t>(backgroundColor) }));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkPrePaint
  (JNIEnv*, jobject, jlong pPage)
{
    WebPage::webPageFromJLong(pPage)->prePaint();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkUpdateContent
    (JNIEnv* env, jobject self, jlong pPage, jobject rq, jint x, jint y, jint w, jint h)
{
    WebPage::webPageFromJLong(pPage)->paint(rq, x, y, w, h);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkUpdateRendering
    (JNIEnv*, jobject, jlong pPage)
{
    WebPage::pageFromJLong(pPage)->updateRendering();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkPostPaint
  (JNIEnv*, jobject, jlong pPage, jobject rq, jint x, jint y, jint w, jint h)
{
    WebPage::webPageFromJLong(pPage)->postPaint(rq, x, y, w, h);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetEncoding
    (JNIEnv* env, jobject self, jlong pPage)
{
    Page* p = WebPage::pageFromJLong(pPage);
    ASSERT(p);
    Frame* mainFrame = (Frame*)&p->mainFrame();
    ASSERT(mainFrame);

    return mainFrame->document()->charset().toJavaString(env).releaseLocal();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetEncoding
    (JNIEnv* env, jobject self, jlong pPage, jstring encoding)
{
    Page* p = WebPage::pageFromJLong(pPage);
    ASSERT(p);
    Frame* mainFrame = (Frame*)&p->mainFrame();
    ASSERT(mainFrame);

    mainFrame->loader().reloadWithOverrideEncoding(String(env, encoding));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkProcessFocusEvent
    (JNIEnv* env, jobject self, jlong pPage,
     jint id, jint direction)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* mainFrame = (Frame*)&page->mainFrame();

    FocusController& focusController = page->focusController();

    Frame* focusedFrame = focusController.focusedFrame();
    switch (id) {
        case com_sun_webkit_event_WCFocusEvent_FOCUS_GAINED:
            focusController.setActive(true); // window activation
            focusController.setFocused(true); // focus gained
            if (!focusedFrame) {
                focusController.setFocusedFrame(mainFrame);
                focusedFrame = mainFrame;
            }
            if (direction == com_sun_webkit_event_WCFocusEvent_FORWARD) {
                // comment out the following line to get focus to the last
                // focused node instead of the first focusable one
                focusedFrame->document()->setFocusedElement(0);
                focusController.advanceFocus(FocusDirectionForward, nullptr);
            } else if (direction == com_sun_webkit_event_WCFocusEvent_BACKWARD) {
                // comment out the following line to get focus to the last
                // focused node instead of the last focusable one
                focusedFrame->document()->setFocusedElement(0);
                focusController.advanceFocus(FocusDirectionBackward, nullptr);
            }
            break;
        case com_sun_webkit_event_WCFocusEvent_FOCUS_LOST:
            focusController.setFocused(false); // focus lost
            focusController.setActive(false); // window deactivation
            break;
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkProcessKeyEvent
    (JNIEnv* env, jobject self, jlong pPage,
     jint type, jstring text, jstring keyIdentifier, jint windowsVirtualKeyCode,
     jboolean shift, jboolean ctrl, jboolean alt, jboolean meta, jdouble timestamp)
{
    WebPage* webPage = WebPage::webPageFromJLong(pPage);

    PlatformKeyboardEvent event(type, text, keyIdentifier,
                                windowsVirtualKeyCode,
                                shift, ctrl, alt, meta, timestamp);

    return bool_to_jbool(webPage->processKeyEvent(event));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkProcessMouseEvent
    (JNIEnv* env, jobject self, jlong pPage,
     jint id, jint button, jint clickCount,
     jint x, jint y, jint screenX, jint screenY,
     jboolean shift, jboolean ctrl, jboolean alt, jboolean meta,
     jboolean popupTrigger, jdouble timestamp)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    // Uncomment to debug mouse events
    // fprintf(stderr, "twkProcessKeyEvent: "
    //         "id=%d button=%d clickCount=%d x=%d y=%d"
    //         "screenX=%d screenY=%d \n",
    //         id, button, clickCount, x, y, screenX, screenY);

    EventHandler& eventHandler = frame->eventHandler();

    FrameView* frameView = frame->view();
    if (!frameView) {
        return false;
    }

    bool consumeEvent = false;
    IntPoint loc(x, y);
    PlatformMouseEvent mouseEvent = PlatformMouseEvent(loc,
                                                       IntPoint(screenX, screenY),
                                                       getWebCoreMouseButton(button),
                                                       getWebCoreMouseEventType(id),
                                                       clickCount,
                                                       shift, ctrl, alt, meta,
                                                       WallTime::fromRawSeconds(timestamp), ForceAtClick, NoTap); // TODO-java: handle force?
    switch (id) {
    case com_sun_webkit_event_WCMouseEvent_MOUSE_PRESSED:
        //frame->focusWindow();
        page->chrome().focus();
        consumeEvent = eventHandler.handleMousePressEvent(mouseEvent);
        break;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_RELEASED:
        consumeEvent = eventHandler.handleMouseReleaseEvent(mouseEvent);
        break;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_MOVED:
    case com_sun_webkit_event_WCMouseEvent_MOUSE_DRAGGED:
        consumeEvent = eventHandler.mouseMoved(mouseEvent);
        break;
    }

    if (popupTrigger && page->settings().isContextMenuEnabled()) {
        ContextMenuController& cmc = page->contextMenuController();
        cmc.clearContextMenu();
        bool handleEvent = eventHandler.sendContextMenuEvent(mouseEvent);
        if (!handleEvent) {
            return consumeEvent;
        }

        ContextMenu* contextMenu = cmc.contextMenu();
        // right-click in disabled text area (and probably many other
        // scenarios) result in nullptr contextMenu here
        if (!contextMenu) {
            return consumeEvent;
        }

        Node* node = cmc.hitTestResult().innerNonSharedNode();
        if (!node) {
            return consumeEvent;
        }

        Frame* frame = node->document().frame();
        // we do not want to show context menu for frameset (see 6648628)
        if (frame && !frame->document()->isFrameSet()) {
            ContextMenuJava(contextMenu->items()).show(&cmc, self, loc);
        }
        return JNI_TRUE;
    }

    return bool_to_jbool(consumeEvent);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkProcessMouseWheelEvent
    (JNIEnv* env, jobject self, jlong pPage,
     jint x, jint y, jint screenX, jint screenY,
     jfloat deltaX, jfloat deltaY,
     jboolean shift, jboolean ctrl, jboolean alt, jboolean meta,
     jdouble timestamp)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    PlatformWheelEvent wheelEvent = PlatformWheelEvent(IntPoint(x, y),
                                                       IntPoint(screenX, screenY),
                                                       deltaX, deltaY,
                                                       shift, ctrl, alt, meta);
    bool consumeEvent = frame->eventHandler().handleWheelEvent(wheelEvent);

    return bool_to_jbool(consumeEvent);
}

#if ENABLE(TOUCH_EVENTS)
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkProcessTouchEvent
    (JNIEnv* env, jobject self, jlong pPage, jint id, jobject touchData,
     jboolean shift, jboolean ctrl, jboolean alt, jboolean meta, jfloat timestamp)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = page->mainFrame();

    ASSERT(frame->eventHandler());
    if (!frame->eventHandler()) {
        return JNI_FALSE;
    }

    PlatformTouchEvent ev(env, id, touchData, shift, ctrl, alt, meta, timestamp);
    bool consumeEvent = frame->eventHandler().handleTouchEvent(ev);
    return bool_to_jbool(consumeEvent);
}
#endif

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkProcessInputTextChange
    (JNIEnv* env, jobject self, jlong pPage,
     jstring jcommitted, jstring jcomposed, jintArray jattributes, jint caretPosition)
{
    Page* page = WebPage::pageFromJLong(pPage);

    Frame* frame = (Frame*)&page->focusController().focusedOrMainFrame();
    ASSERT(frame);

    if (!frame || !frame->editor().canEdit()) {
        // There's no client to deliver the event. Consume the event
        // so that it won't be delivered to a wrong webkit client.
        return JNI_TRUE;
    }

    // Process committed text first
    if (env->GetStringLength(jcommitted) > 0 ||
            // if both committed and composed are empty, confirm with an empty text
            (env->GetStringLength(jcomposed) == 0)) {
        String committed = String(env, jcommitted);
        frame->editor().confirmComposition(committed);
    }

    // Process composed (composition) text here
    if (env->GetStringLength(jcomposed) > 0) {
        jsize length = env->GetArrayLength(jattributes);
        Vector<CompositionUnderline> underlines;
        underlines.resize(length / 3); // 3 members per element
        jint* attrs = env->GetIntArrayElements(jattributes, nullptr);
        if (attrs) {
            for (int i = 0; i < length;) {
                int x = i / 3;
                underlines[x].startOffset = attrs[i++];
                underlines[x].endOffset = attrs[i++];
                underlines[x].thick = (attrs[i++] == 1);
                underlines[x].color = Color::black;
            }
            env->ReleaseIntArrayElements(jattributes, attrs, JNI_ABORT);
        }
        String composed = String(env, jcomposed);
        frame->editor().setComposition(composed, underlines, { }, caretPosition, 0);
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkProcessCaretPositionChange
    (JNIEnv* env, jobject self, jlong pPage,
     jint caretPosition)
{
    Page* page = WebPage::pageFromJLong(pPage);

    Frame* frame = (Frame*)&page->focusController().focusedOrMainFrame();

    ASSERT(frame);

    Text* text = frame->editor().compositionNode();
    if (!text) {
        return JNI_FALSE;
    }

    // FIXME: the following code may not work with having committed text
    Position position(text, caretPosition);
    VisibleSelection selection(position, DOWNSTREAM);
    frame->selection().setSelection(selection /*, default is CharacterGranularity*/);//true, false, false
    return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL Java_com_sun_webkit_WebPage_twkGetTextLocation
    (JNIEnv* env, jobject self, jlong pPage, jint charindex)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame& frame = page->mainFrame();

    jintArray result = env->NewIntArray(4);
    WTF::CheckAndClearException(env); // OOME

    FrameView* frameView = frame.view();
    if (frameView) {
        IntRect caret = frame.selection().absoluteCaretBounds();
        caret = frameView->contentsToWindow(caret);
        jint* ints = (jint*) env->GetPrimitiveArrayCritical(result, nullptr);
        ints[0] = caret.x();
        ints[1] = caret.y();
        ints[2] = caret.width();
        ints[3] = caret.height();
        env->ReleasePrimitiveArrayCritical(result, ints, JNI_ABORT);
    }

    return result;
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkGetLocationOffset
    (JNIEnv* env, jobject self, jlong pPage, jint x, jint y)
{
    // Returns -1 if there's no composition text or the given
    // coordinate is out of the composition text range.

    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    FrameView* frameView = frame->view();
    if (!frameView) {
        return 0;
    }

    jint offset = -1;
    IntPoint point {x, y};
    point = frameView->windowToContents(point);

    Editor &editor = frame->editor();
    if (editor.hasComposition()) {
        auto range = editor.compositionRange();
        for (Node* node = &range->startContainer(); node; node = NodeTraversal::next(*node)) {
            RenderObject* renderer = node->renderer();
            IntRect content = renderer->absoluteBoundingBoxRect();
            VisiblePosition targetPosition(renderer->positionForPoint(LayoutPoint(point.x() - content.x(),
                                                                            point.y() - content.y()), nullptr)); // TODO-java: recheck nullptr
            offset = targetPosition.deepEquivalent().offsetInContainerNode();
            if (offset >= (jint)editor.compositionStart() && offset < (jint)editor.compositionEnd()) {
                offset -= editor.compositionStart();
                break;
            }
        }
    }
    return offset;
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkGetInsertPositionOffset
    (JNIEnv *env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    jint position = 0;
    Editor &editor = frame->editor();
    if (editor.canEdit()) {
        VisibleSelection selection = frame->selection().selection();
        if (selection.isCaret()) {
            VisiblePosition caret = selection.visibleStart();
            position = caret.deepEquivalent().offsetInContainerNode();
            if (editor.hasComposition()) {
                int start = editor.compositionStart();
                int end = editor.compositionEnd();
                if (start < position && position <= end) {
                    position = start;
                } else if (position > end) {
                    position -= end - start;
                }
            }
        }
    }
    return position;
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkGetCommittedTextLength
    (JNIEnv *env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    jint length = 0;
    Editor &editor = frame->editor();
    if (editor.canEdit()) {
        SimpleRange range = makeRangeSelectingNodeContents(*(Node*)frame->selection().selection().start().element());
        for (auto& node : intersectingNodes(range)) {
            if (node.nodeType() == Node::TEXT_NODE || node.nodeType() == Node::CDATA_SECTION_NODE) {
                length += downcast<CharacterData>(node).data().length();
            }
        }
        // Exclude the composition part if any
        if (editor.hasComposition()) {
            int start = editor.compositionStart();
            int end = editor.compositionEnd();
            length -= end - start;
        }
    }
    return length;
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetCommittedText
    (JNIEnv *env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    jstring text = 0;

    Editor &editor = frame->editor();
    if (editor.canEdit()) {
        auto range = makeRangeSelectingNodeContents(*(Node*)frame->selection().selection().start().element());
        if (!range.collapsed()) {
            String t = plainText(range);
            // Exclude the composition text if any
            if (editor.hasComposition()) {
                String s;
                int start = editor.compositionStart();
                int end = editor.compositionEnd();
                unsigned int length = t.length() - (end - start);
                if (start > 0) {
                    s = t.substring(0, start);
                }
                if (s.length() == length) {
                    t = s;
                } else {
                    t = s + t.substring(end, length - start);
                }
            }
            text = t.toJavaString(env).releaseLocal();
            WTF::CheckAndClearException(env); // OOME
        }
    }
    return text;
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetSelectedText
    (JNIEnv *env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    Frame* frame = (Frame*)&page->mainFrame();

    jstring text = 0;

    String t = frame->editor().selectedText();
    text = t.toJavaString(env).releaseLocal();
    WTF::CheckAndClearException(env); // OOME

    return text;
}

//java.awt.dnd.DConstants
enum JAVA_DND_ACTION {
    ACTION_NONE = 0x0,
    ACTION_COPY = 0x1,
    ACTION_MOVE = 0x2,
    ACTION_LINK = 0x40000000
};

static jint dragOperationToDragCursor(Optional<DragOperation> operation) {
    unsigned int res = ACTION_NONE;
    if (operation == DragOperation::Copy)
        res = ACTION_COPY;
    else if (operation == DragOperation::Link)
        res = ACTION_LINK;
    else if (operation == DragOperation::Move)
        res = ACTION_MOVE;
    else if (operation == DragOperation::Generic)
        res = ACTION_MOVE; //This appears to be the Firefox behaviour
    return res;
}

static OptionSet<DragOperation> keyStateToDragOperation(jint javaAction) {
    OptionSet<DragOperation> action = { };
    if(javaAction & ACTION_COPY)
        action = { DragOperation::Copy };
    else if(javaAction & ACTION_LINK)
        action = { DragOperation::Link };
    else if(javaAction & ACTION_MOVE)
        action = { DragOperation::Move };
    return action;
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkProcessDrag
(JNIEnv* env,
 jobject self,
 jlong pPage,
 jint actionId,
 jobjectArray jMimes, jobjectArray jValues,
 jint x, jint y,
 jint screenX, jint screenY,
 jint javaAction) {
    if (jMimes) {
        //TRAGET
        RefPtr<DataObjectJava> pr = DataObjectJava::create();
        jint n = env->GetArrayLength(jMimes);
        for( jint j=0; j<n; ++j ){
            jstring value = (jstring)env->GetObjectArrayElement(jValues, j);
            if(value){
                pr->setData(
                    String(env, JLString((jstring)env->GetObjectArrayElement(jMimes, j))),
                    String(env, JLString(value)));
            }
        }
        DragData dragData(
            pr.get(),
            IntPoint(x, y),
            IntPoint(screenX, screenY),
            keyStateToDragOperation(javaAction));
        DragController& dc = WebPage::pageFromJLong(pPage)->dragController();

        setCopyKeyState(ACTION_COPY == javaAction);
        switch(actionId){
        case com_sun_webkit_WebPage_DND_DST_EXIT:
            dc.dragExited(dragData);
            return 0;
        case com_sun_webkit_WebPage_DND_DST_ENTER:
            return dragOperationToDragCursor(dc.dragEntered(dragData));
        case com_sun_webkit_WebPage_DND_DST_OVER:
        case com_sun_webkit_WebPage_DND_DST_CHANGE:
            return dragOperationToDragCursor(dc.dragUpdated(dragData));
        case com_sun_webkit_WebPage_DND_DST_DROP:
            {
                int ret = dc.performDragOperation(dragData) ? 1 : 0;
                WebPage::pageFromJLong(pPage)->dragController().dragEnded();
                return ret;
            }
        }
    } else {
        //SOURCE
        EventHandler& eventHandler =
                WebPage::pageFromJLong(pPage)->mainFrame().eventHandler();
        PlatformMouseEvent mouseEvent = PlatformMouseEvent(
            IntPoint(x, y),
            IntPoint(screenX, screenY),
            com_sun_webkit_WebPage_DND_SRC_DROP!=actionId
                ? LeftButton
                : NoButton,
            PlatformEvent::MouseMoved,
            0,
            false, false, false, false, WallTime {}, ForceAtClick, NoTap); // TODO-java: handle force?
        switch(actionId){
        case com_sun_webkit_WebPage_DND_SRC_EXIT:
        case com_sun_webkit_WebPage_DND_SRC_ENTER:
        case com_sun_webkit_WebPage_DND_SRC_OVER:
        case com_sun_webkit_WebPage_DND_SRC_CHANGE:
//            The method has been removed. See the changeset #de77cc97972d for the details.
//            eventHandler->dragSourceMovedTo(mouseEvent);
            break;
        case com_sun_webkit_WebPage_DND_SRC_DROP:
            eventHandler.dragSourceEndedAt(mouseEvent, keyStateToDragOperation(javaAction));
            break;
        }
    }
    return 0;
}

static Editor* getEditor(Page* page) {
    ASSERT(page);
    Frame& frame = page->focusController().focusedOrMainFrame();
    return &frame.editor();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkExecuteCommand
    (JNIEnv* env, jobject self, jlong pPage, jstring command, jstring value)
{
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return JNI_FALSE;
    }
    Editor::Command cmd = editor->command(String(env, command));
    return bool_to_jbool(cmd.execute(value ? String(env, value) : String()));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkQueryCommandEnabled
    (JNIEnv* env, jobject self, jlong pPage, jstring command)
{
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return JNI_FALSE;
    }
    Editor::Command cmd = editor->command(String(env, command));
    return bool_to_jbool(cmd.isEnabled());
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkQueryCommandState
    (JNIEnv* env, jobject self, jlong pPage, jstring command)
{
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return JNI_FALSE;
    }
    Editor::Command cmd = editor->command(String(env, command));
    return bool_to_jbool(cmd.state() == TriState::True);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkQueryCommandValue
    (JNIEnv* env, jobject self, jlong pPage, jstring command)
{
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return nullptr;
    }
    Editor::Command cmd = editor->command(String(env, command));
    return cmd.value().toJavaString(env).releaseLocal();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkIsEditable
    (JNIEnv* env, jobject self, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    if (!page) {
        return JNI_FALSE;
    }
    return bool_to_jbool(page->isEditable());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetEditable
    (JNIEnv* env, jobject self, jlong pPage, jboolean editable)
{
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    if (!page) {
        return;
    }
    page->setEditable(jbool_to_bool(editable));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetHtml
    (JNIEnv* env, jobject self, jlong pFrame)
{
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    if (!frame) {
        return 0;
    }

    Document* document = frame->document();
    if (!document || !document->isHTMLDocument()) {
        return 0;
    }

    HTMLElement* documentElement =
            static_cast<HTMLElement*>(document->documentElement());
    if (!documentElement) {
        return 0;
    }

    return documentElement->outerHTML().toJavaString(env).releaseLocal();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkGetUsePageCache
    (JNIEnv*, jobject, jlong pPage)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    return bool_to_jbool(page->settings().usesBackForwardCache());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetUsePageCache
    (JNIEnv*, jobject, jlong pPage, jboolean usePageCache)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    page->settings().setUsesBackForwardCache(jbool_to_bool(usePageCache));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkIsJavaScriptEnabled
    (JNIEnv*, jobject, jlong pPage)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    return bool_to_jbool(page->mainFrame().script().canExecuteScripts(NotAboutToExecuteScript));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetJavaScriptEnabled
    (JNIEnv*, jobject, jlong pPage, jboolean enable)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    page->settings().setScriptEnabled(jbool_to_bool(enable));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkIsContextMenuEnabled
    (JNIEnv*, jobject, jlong pPage)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    return bool_to_jbool(page->settings().isContextMenuEnabled());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetContextMenuEnabled
    (JNIEnv*, jobject, jlong pPage, jboolean enable)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    page->settings().setContextMenuEnabled(jbool_to_bool(enable));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetUserStyleSheetLocation
    (JNIEnv* env, jobject, jlong pPage, jstring url)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    page->settings().setUserStyleSheetLocation(URL(URL(), String(env, url)));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_WebPage_twkGetUserAgent
    (JNIEnv* env, jobject, jlong pPage)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    return page->settings().userAgent().toJavaString(env).releaseLocal();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetUserAgent
    (JNIEnv* env, jobject, jlong pPage, jstring userAgent)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    page->settings().setUserAgent(String(env, userAgent));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetLocalStorageDatabasePath
  (JNIEnv* env, jobject, jlong pPage, jstring path)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    Settings& settings = page->settings();
    settings.setLocalStorageDatabasePath(String(env, path));
    static_cast<WebStorageNamespaceProviderJava*>(
      &page->storageNamespaceProvider())
        ->setLocalStorageDatabasePath(settings.localStorageDatabasePath());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetLocalStorageEnabled
  (JNIEnv*, jobject, jlong pPage, jboolean enabled)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    Settings& settings = page->settings();
    settings.setLocalStorageEnabled(jbool_to_bool(enabled));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_WebPage_twkGetDeveloperExtrasEnabled
  (JNIEnv *, jobject, jlong pPage)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    return bool_to_jbool(page->settings().developerExtrasEnabled());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkSetDeveloperExtrasEnabled
  (JNIEnv *, jobject, jlong pPage, jboolean enabled)
{
    ASSERT(pPage);
    Page* page = WebPage::pageFromJLong(pPage);
    ASSERT(page);
    page->settings().setDeveloperExtrasEnabled(jbool_to_bool(enabled));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkGetUnloadEventListenersCount
    (JNIEnv*, jobject, jlong pFrame)
{
    ASSERT(pFrame);
    Frame* frame = static_cast<Frame*>(jlong_to_ptr(pFrame));
    ASSERT(frame);
    return (jint)frame->document()->domWindow()->pendingUnloadEventListeners();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkConnectInspectorFrontend
  (JNIEnv *, jobject, jlong pPage)
{
    Page *page = WebPage::pageFromJLong(pPage);
    if (page) {
        InspectorController& ic = page->inspectorController();
        InspectorClientJava* icj = static_cast<InspectorClientJava*>(ic.inspectorClient());
        if (icj) {
            ic.connectFrontend(*icj, false);
        }

    }
    WebPage::webPageFromJLong(pPage)->debugStarted();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkDisconnectInspectorFrontend
  (JNIEnv *, jobject, jlong pPage)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return;
    }

    InspectorController& ic = page->inspectorController();
    InspectorClientJava* icj = static_cast<InspectorClientJava*>(ic.inspectorClient());
    if (icj) {
        ic.disconnectFrontend(*icj);
    }

    WebPage::webPageFromJLong(pPage)->debugEnded();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkDispatchInspectorMessageFromFrontend
  (JNIEnv* env, jobject, jlong pPage, jstring message)
{
    Page* page = WebPage::pageFromJLong(pPage);
    if (!page) {
        return;
    }
    //utatodo: seems that RT-21428 will back again
    //JSDOMWindowBase::commonVM()->timeoutChecker.reset(); // RT-21428
    page->inspectorController().dispatchMessageFromFrontend(
            String(env, message));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_WebPage_twkWorkerThreadCount
  (JNIEnv* env, jclass)
{
    return WorkerThread::workerThreadCount();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_WebPage_twkDoJSCGarbageCollection
  (JNIEnv*, jclass)
{
    GCController::singleton().garbageCollectNow();
}

}
