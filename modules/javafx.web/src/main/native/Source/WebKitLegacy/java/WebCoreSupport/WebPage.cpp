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


#if COMPILER(GCC) || COMPILER(CLANG)
#pragma GCC diagnostic ignored "-Wunused-parameter"
#endif

#include "WebPage.h"

#include <WebCore/WKJDOMUtils.h>
#include <wkj_constants.h>

#include "BackForwardList.h"
#include "ChromeClientJava.h"
#include "ContextMenuClientJava.h"
#include "ContextMenuJava.h"
#include "DragClientJava.h"
#include "EditorClientJava.h"
#include "GarbageCollectionController.h"
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
#include <WebCore/CharacterData.h>
#include <WebCore/Chrome.h>
#include <WebCore/ColorTypes.h>
#include <WebCore/CompositionHighlight.h>
#include <WebCore/ContextMenu.h>
#include <WebCore/ContextMenuController.h>
#include <WebCore/CookieJar.h>
#include <WebCore/DeprecatedGlobalSettings.h>
#include <WebCore/Document.h>
#include <WebCore/DocumentInlines.h>
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
#include <WebCore/GeolocationClientMock.h>
#include <WebCore/GraphicsContext.h>
#include <WebCore/GraphicsLayerTextureMapper.h>
#include <WebCore/PageInspectorController.h>
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
#include <WebCore/PlatformWheelEvent.h>
#include <WebCore/ProgressTracker.h>
#include <WebCore/RenderTreeAsText.h>
#include <WebCore/RenderView.h>
#include <WebCore/ResourceRequest.h>
#include <WebCore/ScriptController.h>
#include <WebCore/ScrollingCoordinatorTypes.h>
#include <WebCore/SecurityPolicy.h>
#include <WebCore/Settings.h>
#include <WebCore/StorageNamespaceProvider.h>
#include <WebCore/TextIterator.h>
#include <WebCore/TextureMapperJava.h>
#include <WebCore/TextureMapperLayer.h>
#include <WebCore/WorkerThread.h>
#include <WebCore/platform/graphics/java/GraphicsContextJava.h>
#include <wtf/Ref.h>
#include <wtf/RunLoop.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/MakeString.h>
#include <wtf/text/StringToIntegerConversion.h>
#include "LocalDOMWindow.h"
#include "DocumentView.h"
#include "LocalFrameInlines.h"
#include "DocumentPage.h"
#include "NodeDocument.h"

// FIXME: Move dependency of runtime_root to BridgeUtils
#include <WebCore/runtime_root.h>
#if OS(UNIX)
#include <sys/utsname.h>
#endif
#if OS(WINDOWS)
#include <WebCore/SystemInfo.h>
#endif



#if ENABLE(NOTIFICATIONS) || ENABLE(LEGACY_NOTIFICATIONS)
#include <WebCore/NotificationController.h>
#include "NotificationClientJava.h"
#endif

namespace WebCore {

WebPage::WebPage(RefPtr<Page> page)
    : m_page(WTF::move(page))
    , m_printContext(PrintContext::create(m_page->localMainFrame()))
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

/*
 * Installs the per-page callback tables and retains the id of the Java WebPage. Called by
 * wkj_page_create as the last step of building a page, and by wkj_page_set_callbacks to
 * detach or re-attach one.
 *
 * The clients are handed borrowed copies: the id lives here, in one place, for the life
 * of the page. That is the whole replacement for the eight JNI global references that
 * used to pin one Java WebPage - ChromeClientJava, ContextMenuClientJava, EditorClientJava,
 * DragClientJava, InspectorClientJava, ProgressTrackerClientJava, PageSupplementJava and
 * one FrameLoaderClientJava per frame - so the Java object becomes collectable as soon as
 * the page is destroyed rather than when the last client is.
 *
 * Passing a null table detaches the page: no callback is made afterwards, which is what
 * the Java dispose path does before closing the arena that owns the upcall stubs. It is
 * also what preserves the shutdown early-out the frame loader client destructor used to
 * rely on when the JVM was going away.
 */
void WebPage::setCallbacks(const WKJPageCallbacks* callbacks, wkj_ref webPage)
{
    m_callbacks = callbacks;
    m_javaPage = WKJHandle::retained(webPage);

    Page* page = m_page.get();
    if (!page)
        return;

    static_cast<ChromeClientJava&>(page->chrome().client()).setJavaPage(
        webPage, callbacks ? callbacks->chrome : nullptr, this);

    static_cast<EditorClientJava&>(page->editorClient()).setJavaPage(
        webPage, callbacks ? callbacks->editor : nullptr);

    static_cast<DragClientJava&>(page->dragController().client()).setJavaPage(
        webPage, callbacks ? callbacks->drag : nullptr, page);

    if (auto* inspectorClient = static_cast<InspectorClientJava*>(
            page->inspectorController().inspectorBackendClient())) {
        inspectorClient->setJavaPage(webPage, callbacks ? callbacks->inspector : nullptr);
    }

    static_cast<ProgressTrackerClientJava&>(page->progress().client()).setJavaPage(
        webPage, callbacks ? callbacks->progress : nullptr);

    if (auto* localFrame = dynamicDowncast<LocalFrame>(&page->mainFrame())) {
        static_cast<FrameLoaderClientJava&>(localFrame->loader().client()).setJavaPage(
            webPage, callbacks ? callbacks->frame_loader : nullptr, page);
    }
}

WKJHandle WebPage::jobjectFromPage(Page* page)
{
    if (!page)
        return WKJHandle();

    auto pageSupplement = PageSupplementJava::from(page);
    return pageSupplement ? pageSupplement->jWebPage() : WKJHandle();
}

void WebPage::setSize(const IntSize& size)
{
    Frame* mainFrame = (Frame*)&m_page->mainFrame();
    auto* localFrame = dynamicDowncast<LocalFrame>(mainFrame);
    LocalFrameView* frameView = localFrame->view();
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

   if(!m_page) return;
   auto* localFrame = dynamicDowncast<LocalFrame>(&m_page->mainFrame());
   if (!localFrame)
       return;

   if (auto* frameView = localFrame->view())
       frameView->updateLayoutAndStyleIfNeededRecursive();
}

RefPtr<RQRef> WebPage::jRenderTheme()
{
    if (!m_jRenderTheme) {
        WKJHandle theme = wkjRenderThemeForPage(jobjectFromPage(m_page.get()).get());
        m_jRenderTheme = RQRef::create(theme.get());
    }
    return m_jRenderTheme;
}

void WebPage::paint(wkj_ref rq, int32_t x, int32_t y, int32_t w, int32_t h)
{
    if (m_rootLayer) {
        return;
    }

    // DBG_CHECKPOINTEX("twkUpdateContent", 15, 100);

    Frame* mainFrame = (Frame*)&m_page->mainFrame();
    //RefPtr<Frame> mainFrame((Frame*)&m_page->mainFrame());
    auto* localFrame = dynamicDowncast<LocalFrame>(mainFrame);
    LocalFrameView* frameView = localFrame->view();
    if (!frameView) {
        return;
    }

    // Will be deleted by GraphicsContext destructor
    PlatformContextJava* ppgc = new PlatformContextJava(rq, jRenderTheme());
    GraphicsContextJava gc(ppgc);

    // TODO: Following JS synchronization is not necessary for single thread model
    JSGlobalContextRef globalContext = toGlobalRef(localFrame->script().globalObject(mainThreadNormalWorldSingleton()));
    JSC::JSLockHolder sw(toJS(globalContext)); // TODO-java: was JSC::APIEntryShim sw( toJS(globalContext) );

    frameView->paint(gc, IntRect(x, y, w, h));
    if (m_page->settings().showDebugBorders()) {
        drawDebugLed(gc, IntRect(x, y, w, h), SRGBA<uint8_t> { 0, 0, 255, 128 });
    }

    gc.platformContext()->rq().flushBuffer();
}

void WebPage::postPaint(wkj_ref rq, int32_t x, int32_t y, int32_t w, int32_t h)
{
    if (!m_page->inspectorController().highlightedNode()
            && !m_rootLayer
    ) {
        return;
    }

    // Will be deleted by GraphicsContext destructor
    PlatformContextJava* ppgc = new PlatformContextJava(rq, jRenderTheme());
    GraphicsContextJava gc(ppgc);

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

    if (!m_callbacks || !m_callbacks->notify || !m_callbacks->notify->scroll)
        return;

    m_callbacks->notify->scroll(
            m_javaPage.get(),
            rectToScroll.x(),
            rectToScroll.y(),
            rectToScroll.width(),
            rectToScroll.height(),
            scrollDelta.width(),
            scrollDelta.height());
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
    if (!m_callbacks || !m_callbacks->notify || !m_callbacks->notify->repaint)
        return;

    m_callbacks->notify->repaint(
            m_javaPage.get(),
            rect.x(),
            rect.y(),
            rect.width(),
            rect.height());
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

        m_textureMapper = std::make_unique<TextureMapperJavaAdapter>();
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
        m_page->isolatedUpdateRendering();
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
        Frame* mainFrame = (Frame*)&m_page->mainFrame();
    auto* localFrame = dynamicDowncast<LocalFrame>(mainFrame);
    LocalFrameView* frameView = localFrame->view();

    if (!localFrame->contentRenderer() || !frameView)
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

    if (m_textureMapper)
        static_cast<TextureMapperJavaAdapter*>(m_textureMapper.get())->setGraphicsContext(&context);

    TransformationMatrix matrix;
    m_textureMapper->beginPainting();
    m_textureMapper->beginClip(matrix, FloatRoundedRect(clip));
    rootTextureMapperLayer.applyAnimationsRecursively(MonotonicTime::now());
    downcast<GraphicsLayerTextureMapper>(*m_rootLayer).updateBackingStoreIncludingSubLayers(*m_textureMapper);
    rootTextureMapperLayer.paint(*m_textureMapper);
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

void WebPage::paintContents(const GraphicsLayer& glc, GraphicsContext& context, const FloatRect& inClip, OptionSet<GraphicsLayerPaintBehavior>)
{
    context.save();
    context.clip(inClip);
    Frame* mainFrame = (Frame*)&m_page->mainFrame();
    auto* localFrame = dynamicDowncast<LocalFrame>(mainFrame);
    LocalFrameView* frameView = localFrame->view();
    frameView->paint(context, enclosingIntRect(inClip));
    if (m_page->settings().showDebugBorders()) {
        drawDebugBorder(context, roundedIntRect(inClip), SRGBA<uint8_t> { 0, 192, 0 }, 20);
    }
    context.restore();
}

bool WebPage::processKeyEvent(const PlatformKeyboardEvent& event)
{
    return event.type() == PlatformEvent::Type::Char
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
    ASSERT((event.type() == PlatformEvent::Type::RawKeyDown)
        || (event.type() == PlatformEvent::Type::KeyDown)
        || (event.type() == PlatformEvent::Type::KeyUp));

    // Please refer to the comments explaining the m_suppressNextKeypressEvent
    // member.
    // The m_suppressNextKeypressEvent is set if the KeyDown is handled by
    // Webkit. A keyDown event is typically associated with a keyPress(char)
    // event and a keyUp event. We reset this flag here as this is a new keyDown
    // event.
    m_suppressNextKeypressEvent = false;

    RefPtr<LocalFrame> frame = focusedWebCoreFrame();
    if (!frame)
        return false;
    EventHandler& handler = frame->eventHandler();

    if (handler.keyEvent(event)) {
        if (event.type() == PlatformEvent::Type::RawKeyDown) {
            // Suppress the next keypress event unless the focused node
            // is a plug-in node. (Flash needs these keypress events to
            // handle non-US keyboards.)
            Node* node = focusedWebCoreNode();
            if (!node || !node->renderer()
                    || !node->renderer()->isRenderEmbeddedObject())
                m_suppressNextKeypressEvent = true;
        }
        return true;
    }

    return keyEventDefault(event);
}

bool WebPage::charEvent(const PlatformKeyboardEvent& event)
{
    ASSERT(event.type() == PlatformEvent::Type::Char);

    // Please refer to the comments explaining the m_suppressNextKeypressEvent
    // member.  The m_suppressNextKeypressEvent is set if the KeyDown is
    // handled by Webkit. A keyDown event is typically associated with a
    // keyPress(char) event and a keyUp event. We reset this flag here as it
    // only applies to the current keyPress event.
    bool suppress = m_suppressNextKeypressEvent;
    m_suppressNextKeypressEvent = false;

    LocalFrame* frame = focusedWebCoreFrame();
    if (!frame)
        return suppress;

    EventHandler& handler = frame->eventHandler();

    if (!suppress && !handler.keyEvent(event))
        return keyEventDefault(event);

    return true;
}

bool WebPage::keyEventDefault(const PlatformKeyboardEvent& event)
{
    LocalFrame* frame = focusedWebCoreFrame();
    if (!frame)
        return false;

    switch (event.type()) {
    case PlatformEvent::Type::RawKeyDown:
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
        *scrollDirection = ScrollDirection::ScrollLeft;
        *scrollGranularity = ScrollGranularity::Line;
        break;
    case VKEY_RIGHT:
        *scrollDirection = ScrollDirection::ScrollRight;
        *scrollGranularity = ScrollGranularity::Line;
        break;
    case VKEY_UP:
        *scrollDirection = ScrollDirection::ScrollUp;
        *scrollGranularity = ScrollGranularity::Line;
        break;
    case VKEY_DOWN:
        *scrollDirection = ScrollDirection::ScrollDown;
        *scrollGranularity = ScrollGranularity::Line;
        break;
    case VKEY_HOME:
        *scrollDirection = ScrollDirection::ScrollUp;
        *scrollGranularity = ScrollGranularity::Document;
        break;
    case VKEY_END:
        *scrollDirection = ScrollDirection::ScrollDown;
        *scrollGranularity = ScrollGranularity::Document;
        break;
    case VKEY_PRIOR:  // page up
        *scrollDirection = ScrollDirection::ScrollUp;
        *scrollGranularity = ScrollGranularity::Page;
        break;
    case VKEY_NEXT:  // page down
        *scrollDirection = ScrollDirection::ScrollDown;
        *scrollGranularity = ScrollGranularity::Page;
        break;
    default:
        return false;
    }

    return true;
}

bool WebPage::propagateScroll(ScrollDirection scrollDirection,
                              ScrollGranularity scrollGranularity)
{
    LocalFrame* frame = focusedWebCoreFrame();
    if (!frame)
        return false;

    bool scrollHandled = frame->eventHandler().scrollOverflow(
            scrollDirection,
            scrollGranularity);
    LocalFrame* currentFrame = frame;
    while (!scrollHandled && currentFrame) {
        scrollHandled = currentFrame->view()->scroll(scrollDirection,
                                                     scrollGranularity);
        currentFrame = dynamicDowncast<LocalFrame>(currentFrame->tree().parent());
    }
    return scrollHandled;
}

LocalFrame* WebPage::focusedWebCoreFrame()
{
    return m_page->focusController().focusedOrMainFrame();
}

Node* WebPage::focusedWebCoreNode()
{
    LocalFrame* frame = m_page->focusController().focusedLocalFrame();
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
    return "Macintosh; Intel Mac OS X"_s;
#else
    return "Macintosh; PPC Mac OS X"_s;
#endif
#elif OS(UNIX)
    struct utsname name;
    if (uname(&name) != -1) {
    const char* sysname = name.sysname;
        const char* machine = name.machine;
        // Convert to std::span<const char8_t>
        auto sysnameSpan = std::span<const char8_t>(reinterpret_cast<const char8_t*>(sysname), std::strlen(sysname));
        auto machineSpan = std::span<const char8_t>(reinterpret_cast<const char8_t*>(machine), std::strlen(machine));

        // Use fromUTF8 to convert to String
        String sysnameString = String::fromUTF8(sysnameSpan);
        String machineString = String::fromUTF8(machineSpan);
        return makeString(sysnameString, ' ', machineString);
    }
#elif OS(WINDOWS)
    return windowsVersionForUAString();
#else
    notImplemented();
#endif
    return "Unknown"_s;
}

static String defaultUserAgent()
{
    static const NeverDestroyed userAgentString = [] {
        String wkVersion = makeString(
                              WTF::String::number(WEBKIT_MAJOR_VERSION), WTF::String::fromLatin1("."), WTF::String::number(WEBKIT_MINOR_VERSION),
                              WTF::String::fromLatin1(" (KHTML, like Gecko) JavaFX/"), WTF::String::fromLatin1(JAVAFX_RELEASE_VERSION),
                              WTF::String::fromLatin1(" Version/"), WTF::String::fromLatin1(WEBVIEW_BROWSER_VERSION),
                              WTF::String::fromLatin1(" Safari/"), WTF::String::number(WEBKIT_MAJOR_VERSION), WTF::String::fromLatin1("."),  WTF::String::number(WEBKIT_MINOR_VERSION));
        return makeString(WTF::String::fromLatin1("Mozilla/5.0 ("), agentOS(), WTF::String::fromLatin1(") AppleWebKit/"), wkVersion);
    }();
    return userAgentString;
}

int WebPage::beginPrinting(float width, float height)
{
    Frame* mainFrame = (Frame*)&m_page->mainFrame();
    auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame->document() || !frame->view())
        return 0;
    frame->document()->updateLayout();

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
        JSContextGroupRef contextGroup = toRef(&mainThreadNormalWorldSingleton().vm());
        JSContextGroupSetExecutionTimeLimit(contextGroup, 10, 0, 0);
    }
}

void WebPage::disableWatchdog() {
    if (globalDebugSessionCounter > 0) {
        JSContextGroupRef contextGroup = toRef(&(mainThreadNormalWorldSingleton().vm()));
        JSContextGroupClearExecutionTimeLimit(contextGroup);
    }
}

} // namespace WebCore

using namespace WebCore;
using namespace WTF;

extern "C" WKJ_EXPORT void WebPage_doJSCGarbageCollection()
{
    WebCore::GarbageCollectionController::singleton().garbageCollectNow();
}

class WebStorageNamespaceProviderJava final : public WebCore::StorageNamespaceProvider {
public:
    void setLocalStorageDatabasePath(const String& path) {
        m_localStorageDatabasePath = path.isNull() ? emptyString() : path;
    }
private:
    String m_localStorageDatabasePath { emptyString() };
        WeakHashMap<WebCore::Page, HashMap<WebCore::SecurityOriginData, RefPtr<WebCore::StorageNamespace>>> m_sessionStorageNamespaces;

        RefPtr<StorageNamespace> sessionStorageNamespace(const SecurityOrigin& topLevelOrigin, Page& page, ShouldCreateNamespace shouldCreate) override{
            if (m_sessionStorageNamespaces.find(page) == m_sessionStorageNamespaces.end()) {
        if (shouldCreate == ShouldCreateNamespace::No)
            return nullptr;
        HashMap<SecurityOriginData, RefPtr<StorageNamespace>> map;
        m_sessionStorageNamespaces.set(page, map);
    }
    auto& sessionStorageNamespaces = m_sessionStorageNamespaces.find(page)->value;

    auto sessionStorageNamespaceIt = sessionStorageNamespaces.find(topLevelOrigin.data());
    if (sessionStorageNamespaceIt == sessionStorageNamespaces.end()) {
        if (shouldCreate == ShouldCreateNamespace::No)
            return nullptr;
        return sessionStorageNamespaces.add(topLevelOrigin.data(), WebKit::StorageNamespaceImpl::createSessionStorageNamespace(sessionStorageQuota(), page.sessionID())).iterator->value;
    }
        return sessionStorageNamespaceIt->value;
    }

    void cloneSessionStorageNamespaceForPage(WebCore::Page& srcPage, WebCore::Page& dstPage) override
    {
        auto& srcSessionStorageNamespaces = static_cast<WebStorageNamespaceProviderJava&>(srcPage.storageNamespaceProvider()).m_sessionStorageNamespaces;
        auto srcPageIt = srcSessionStorageNamespaces.find(srcPage);
        if (srcPageIt == srcSessionStorageNamespaces.end())
            return;

        auto& srcPageSessionStorageNamespaces = srcPageIt->value;
        HashMap<SecurityOriginData, RefPtr<StorageNamespace>> dstPageSessionStorageNamespaces;
        for (auto& [origin, srcNamespace] : srcPageSessionStorageNamespaces)
        dstPageSessionStorageNamespaces.set(origin, srcNamespace->copy(dstPage));

        auto& dstSessionStorageNamespaces = static_cast<WebStorageNamespaceProviderJava&>(dstPage.storageNamespaceProvider()).m_sessionStorageNamespaces;
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

WKJ_EXPORT void wkj_set_startup_options(int32_t useJIT, int32_t useDFGJIT, int32_t useCSS3D)
{
    WKJCallScope wkjScope;
    s_useJIT = useJIT;
    s_useDFGJIT = useDFGJIT;
    s_useCSS3D = useCSS3D;
}

/*
 * Was twkCreatePage, which took the Java WebPage as a raw Java reference and stored it in
 * PageSupplementJava. The supplement holds a wkj_ref now, so the id is all that crosses,
 * and installing the callback tables became the tail of this function rather than a
 * separate call.
 */
WKJ_EXPORT int64_t wkj_page_create(int32_t editable, const WKJPageCallbacks* callbacks,
                                   wkj_ref web_page)
{
    WKJCallScope wkjScope;
    // FIXME-java(JDK-8169950): Refactor the following WebCore module
    // initialization flow.
    JSC::initialize();
    WTF::initializeMainThread();
    // JDK-8128763: Allow local loads for substitute data, that is,
    // for content loaded with twkLoad
    WebCore::SecurityPolicy::setLocalLoadPolicy(
            WebCore::SecurityPolicy::AllowLocalLoadsForLocalAndSubstituteData);

    //DBG_CHECKPOINTEX("twkCreatePage", 3, 5);

    VisitedLinkStoreJava::setShouldTrackVisitedLinks(true);

#if !LOG_DISABLED
    logChannels().initializeLogChannelsIfNecessary();
#endif
    WebCore::PlatformStrategiesJava::initialize();

    static std::once_flag initializeJSCOptions;
    std::call_once(initializeJSCOptions, [] {
        JSC::Options::useJIT() = s_useJIT;
        // Enable DFG only if JIT is enabled.
        JSC::Options::useDFGJIT() = s_useJIT && s_useDFGJIT;
    });

    //utaTODO: history agent implementation
    auto identifier = PageIdentifier::generate();
    auto pc = pageConfigurationWithEmptyClients(identifier, PAL::SessionID::defaultSessionID());
    auto pageStorageSessionProvider = PageStorageSessionProvider::create();
    pc.cookieJar = CookieJar::create(pageStorageSessionProvider.copyRef());
    pc.chromeClient = makeUniqueRef<ChromeClientJava>();
    pc.contextMenuClient = makeUniqueRef<ContextMenuClientJava>();
    pc.editorClient = makeUniqueRef<EditorClientJava>();
    pc.dragClient = makeUnique<DragClientJava>();
    pc.inspectorBackendClient = makeUnique<InspectorClientJava>();
    pc.databaseProvider = &WebDatabaseProvider::singleton();
    pc.storageNamespaceProvider = adoptRef(new WebStorageNamespaceProviderJava());
    pc.visitedLinkStore = VisitedLinkStoreJava::create();

    pc.mainFrameCreationParameters = PageConfiguration::LocalMainFrameCreationParameters {
        CompletionHandler<UniqueRef<LocalFrameLoaderClient>(LocalFrame&, FrameLoader&)>(
            [](LocalFrame& frame, FrameLoader& loader) -> UniqueRef<LocalFrameLoaderClient> {
                return makeUniqueRefWithoutRefCountedCheck<FrameLoaderClientJava>(loader);
            }
        ),
        SandboxFlags { }
    };

    pc.progressTrackerClient = makeUniqueRef<ProgressTrackerClientJava>();

    pc.backForwardClient = BackForwardList::create();
    auto page = Page::create(WTF::move(pc));

    // Associate PageSupplementJava instance which has WebPage java object.
    page->provideSupplement(PageSupplementJava::supplementName(), std::make_unique<PageSupplementJava>(web_page));
    pageStorageSessionProvider->setPage(page);
#if ENABLE(GEOLOCATION)
    WebCore::provideGeolocationTo(&page.get(), GeolocationClientMock::create());
#endif
    WebPage* webPage = new WebPage(WTF::move(page));
    webPage->setCallbacks(callbacks, web_page);
    return wkj_from_ptr(webPage);
}

/*
 * Detaches or re-attaches the tables of a live page. Passing a null table stops every
 * callback, which is what a Java dispose does before closing the arena holding the upcall
 * stubs; it is also what replaces the shutdown guard the frame loader client destructor
 * relied on when the JVM was going away.
 */
WKJ_EXPORT void wkj_page_set_callbacks(int64_t pPage, const WKJPageCallbacks* callbacks,
                                       wkj_ref web_page)
{
    WKJCallScope wkjScope;
    WebPage* webPage = WebPage::webPageFromPeer(pPage);
    if (!webPage)
        return;
    webPage->setCallbacks(callbacks, web_page);
}

WKJ_EXPORT void wkj_page_init(int64_t pPage, int32_t usePlugins, float devicePixelScale)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);

    /* Initialization of the default settings */
    Settings& settings = page->settings();
    settings.setTextAreasAreResizable(true);
    settings.setLoadsImagesAutomatically(true);
    settings.setMinimumFontSize(0);
    settings.setMinimumLogicalFontSize(5);
    settings.setAcceleratedCompositingEnabled(s_useCSS3D);
    settings.setScriptEnabled(true);
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setDefaultFixedFontSize(13);
    settings.setDefaultFontSize(16);
    settings.setContextMenuEnabled(true);
    settings.setInputTypeColorEnabled(true);
    settings.setLocalStorageEnabled(true);
    settings.setSessionStorageEnabled(true);
    settings.setUserAgent(defaultUserAgent());
    settings.setMaximumHTMLParserDOMTreeDepth(180);
    //settings.setXSSAuditorEnabled(true);
    settings.setInteractiveFormValidationEnabled(true);

    /* Using java logical fonts as defaults */
    settings.setSerifFontFamily("Serif"_s);
    settings.setSansSerifFontFamily("SansSerif"_s);
    settings.setFixedFontFamily("Monospaced"_s);
    page->setDeviceScaleFactor(devicePixelScale);

    settings.setLinkPrefetchEnabled(true);

        Frame* mainFrame = (Frame*)&page->mainFrame();
    auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    FrameLoaderClientJava& client =
        static_cast<FrameLoaderClientJava&>(frame->loader().client());
    client.init();
    client.setFrame(frame);

    frame->init();

    JSContextGroupRef contextGroup = toRef(&(mainThreadNormalWorldSingleton().vm()));
    JSContextGroupSetExecutionTimeLimit(contextGroup, 10, 0, 0);

    WebPage::webPageFromPeer(pPage)->enableWatchdog();
}

WKJ_EXPORT void wkj_page_destroy(int64_t pPage)
{
    WKJCallScope wkjScope;
    WebPage* webPage = WebPage::webPageFromPeer(pPage);
    if (!webPage) {
        return;
    }
        Frame* frame = (Frame*)&webPage->page()->mainFrame();
    auto* mainFrame = dynamicDowncast<LocalFrame>(frame);
    if (mainFrame) {
        mainFrame->loader().stopAllLoaders();
        mainFrame->loader().detachFromParent();
    }

    delete webPage;
}

WKJ_EXPORT int64_t wkj_page_main_frame(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (!page) {
        return 0;
    }
        Frame* frame = (Frame*)&page->mainFrame();
    auto* mainFrame = dynamicDowncast<LocalFrame>(frame);
    if (!mainFrame) {
        return 0;
    }
    return wkj_from_ptr(mainFrame);
}

WKJ_EXPORT int64_t wkj_frame_parent(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return 0;
    }
    Frame* parentFrame = dynamicDowncast<LocalFrame>(frame->tree().parent());
    if (!parentFrame) {
        return 0;
    }
    return wkj_from_ptr(parentFrame);
}

/*
 * Returns the number of local child frames, writing up to out_cap of them into out.
 *
 * The count-returning shape fixes two defects of the array-returning one by construction,
 * which is a behaviour change and is called out rather than hidden: the JNI version
 * returned a null array for a non-local frame, which WebPage.getChildFrames iterated
 * without a null check, and it sized the array with FrameTree::childCount() while
 * skipping non-local children, so trailing zeroes reached Java as frame handle 0. Neither
 * is reachable with site isolation off, which is how this port is built.
 */
WKJ_EXPORT int32_t wkj_frame_children(int64_t pFrame, int64_t* out, int32_t out_cap)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return 0;
    }

    FrameTree& tree = frame->tree();

    int32_t count = 0;
    for (auto* child = tree.firstChild(); child; child = child->tree().nextSibling()) {
        auto* localChild = dynamicDowncast<LocalFrame>(child);
        if (!localChild)
               continue;
        if (out && count < out_cap)
            out[count] = wkj_from_ptr(child);
        count++;
    }

    return count;
}

WKJ_EXPORT int32_t wkj_frame_name(int64_t pFrame, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }
    return WKJReturnString(result_buf, result_cap, result_length, frame->tree().uniqueName().string());
}

WKJ_EXPORT int32_t wkj_frame_url(int64_t pFrame, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->document()) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }
    Document* doc = frame->document();
    if (!doc) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }
    return WKJReturnString(result_buf, result_cap, result_length, doc->url().string());
}

WKJ_EXPORT int32_t wkj_frame_inner_text(int64_t pFrame, uint16_t* result_buf, int32_t result_cap,
                                        int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    Document* document = frame->document();
    if (!document) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    Element* documentElement = document->documentElement();
    if (!documentElement) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    LocalFrameView* frameView = frame->view();
    if (frameView && frameView->layoutContext().isLayoutPending()) {
        frameView->layoutContext().layout();
    }

    return WKJReturnString(result_buf, result_cap, result_length, documentElement->innerText());
}

WKJ_EXPORT int32_t wkj_frame_render_tree(int64_t pFrame, uint16_t* result_buf, int32_t result_cap,
                                         int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->contentRenderer()) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    LocalFrameView* frameView = frame->view();
    if (frameView && frameView->layoutContext().isLayoutPending()) {
        frameView->layoutContext().layout();
    }

    return WKJReturnString(result_buf, result_cap, result_length, externalRepresentation(frame));
}

WKJ_EXPORT int32_t wkj_frame_content_type(int64_t pFrame, uint16_t* result_buf, int32_t result_cap,
                                          int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->loader().documentLoader()) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }
    return WKJReturnString(result_buf, result_cap, result_length, frame->loader().documentLoader()->responseMIMEType());
}

WKJ_EXPORT int32_t wkj_frame_title(int64_t pFrame, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->document()) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }
    return WKJReturnString(result_buf, result_cap, result_length, frame->document()->title());
}

/*
 * twkGetIconURL is gone rather than converted. ENABLE(ICONDATABASE) is never defined for
 * this port, so its body was a plain 0 for every input, and the native-necessity triage
 * rules it PURE with exact parity: the Java side returns null directly. WebPage.getIcon
 * and its native declaration go with it.
 */

WKJ_EXPORT void wkj_frame_open(int64_t pFrame, const uint16_t* url, int32_t url_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return;
    }

    static const URL emptyParent;

    FrameLoadRequest frameLoadRequest(
        *frame, ResourceRequest(URL(emptyParent, WKJString(url, url_length))));
    frameLoadRequest.setIsRequestFromClientOrUserInput();
    frame->loader().load(WTF::move(frameLoadRequest));
}

/*
 * ENCODING HAZARD, PRESERVED DELIBERATELY.
 *
 * The JNI version read the HTML with GetStringUTFChars, which produces *modified* UTF-8,
 * and handed those bytes to a SharedBuffer inside a ResourceResponse that declares the
 * charset "UTF-8". For U+0000 and for every supplementary character the two encodings
 * differ - modified UTF-8 writes a surrogate pair as two three-byte sequences - so
 * WebEngine.loadContent of an astral character has always fed CESU-8 to a decoder that
 * was told it was UTF-8.
 *
 * `content` is therefore documented as modified UTF-8 and the Java side encodes it that
 * way, because a migration commit may not change behaviour. Switching to standard UTF-8
 * fixes a real latent bug and belongs in its own commit with its own test.
 */
WKJ_EXPORT void wkj_frame_load(int64_t pFrame, const uint8_t* content, int32_t content_length,
                               const uint16_t* contentType, int32_t contentType_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return;
    }

    size_t stringLen = content_length > 0 ? static_cast<size_t>(content_length) : 0;
    std::span<const uint8_t> byteSpan(content, stringLen);
    RefPtr<SharedBuffer> buffer = SharedBuffer::create(byteSpan);

    ResourceResponse response(URL(), WKJString(contentType, contentType_length), stringLen, "UTF-8"_s);
    FrameLoadRequest frameLoadRequest(
        *frame,
        ResourceRequest(URL({ }, ""_s)),
        SubstituteData(
            WTF::move(buffer),
            URL(),
            WTF::move(response),
            SubstituteData::SessionHistoryVisibility::Visible) // TODO-java: or Hidden?
    );
    frameLoadRequest.setIsRequestFromClientOrUserInput();
    frame->loader().load(WTF::move(frameLoadRequest));
}

WKJ_EXPORT int32_t wkj_frame_is_loading(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    return (frame && frame->loader().isLoading()) ? 1 : 0;
}

WKJ_EXPORT void wkj_frame_stop(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return;
    }

    frame->loader().stopAllLoaders();
}

WKJ_EXPORT void wkj_page_stop_all(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (!page) {
        return;
    }
    Frame* mainFrame = (Frame*)&page->mainFrame();
    auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    frame->loader().stopAllLoaders();
}

WKJ_EXPORT void wkj_frame_refresh(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return;
    }

    frame->loader().reload(ReloadOption::FromOrigin);
}

WKJ_EXPORT int32_t wkj_page_go_back_forward(int64_t pPage, int32_t distance)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (!page) {
        return 0;
    }

    if (page->backForward().canGoBackOrForward(distance)) {
        page->backForward().goBackOrForward(distance);
        return 1;
    }

    return 0;
}

WKJ_EXPORT int32_t wkj_frame_copy(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return 0;
    }

    if (frame->editor().canCopy()) {
        frame->editor().copy();
        return 1;
    }

    return 0;
}


WKJ_EXPORT int32_t wkj_page_find(int64_t pPage, const uint16_t* toFind, int32_t toFind_length, int32_t forward,
                                 int32_t wrap, int32_t matchCase)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (page) {
        FindOptions opts;
        if (!matchCase)
            opts.add(FindOption::CaseInsensitive);
        if (!forward)
            opts.add(FindOption::Backwards);
        if (wrap)
            opts.add(FindOption::WrapAround);

        auto findResult = page->findString(WKJString(toFind, toFind_length), opts);
        bool found = findResult.range.has_value();
        return (found) ? 1 : 0;
    }
    return 0;
}

WKJ_EXPORT int32_t wkj_frame_find(int64_t pFrame, const uint16_t* toFind, int32_t toFind_length, int32_t forward,
                                  int32_t wrap, int32_t matchCase)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (frame) {
        //utatodo: support for the rest of FindOptionFlag
        FindOptions opts;
        if (!matchCase)
            opts.add(FindOption::CaseInsensitive);
        if (!forward)
            opts.add(FindOption::Backwards);
        if (wrap)
            opts.add(FindOption::WrapAround);
        auto result = frame->page()->findString(
            WKJString(toFind, toFind_length), opts | FindOption::StartInSelection);
        return (result.range.has_value()) ? 1 : 0;
    }
    return 0;
}

WKJ_EXPORT void wkj_page_override_preference(int64_t pPage, const uint16_t* propertyName,
                                             int32_t propertyName_length, const uint16_t* propertyValue,
                                             int32_t propertyValue_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (!page) {
        return;
    }

    Settings& settings = page->settings();
    String nativePropertyName = WKJString(propertyName, propertyName_length);
    String nativePropertyValue = WKJString(propertyValue, propertyValue_length);
    StringView nativePropertyString(nativePropertyValue);

    if (nativePropertyName == "CSSCounterStyleAtRuleImageSymbolsEnabled"_s) {
        settings.setCSSCounterStyleAtRuleImageSymbolsEnabled(nativePropertyValue == "true"_s);
    }
    else if (nativePropertyName == "WebKitTextAreasAreResizable"_s) {
        settings.setTextAreasAreResizable(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitLoadsImagesAutomatically"_s) {
        settings.setLoadsImagesAutomatically(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitMinimumFontSize"_s) {
        settings.setMinimumFontSize(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitMinimumLogicalFontSize"_s) {
        settings.setMinimumLogicalFontSize(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitAcceleratedCompositingEnabled"_s) {
        settings.setAcceleratedCompositingEnabled(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitScriptEnabled"_s) {
        settings.setScriptEnabled(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitJavaScriptCanOpenWindowsAutomatically"_s) {
        settings.setJavaScriptCanOpenWindowsAutomatically(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitDefaultFixedFontSize"_s) {
        settings.setDefaultFixedFontSize(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitContextMenuEnabled"_s) {
        settings.setContextMenuEnabled(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "WebKitUserAgent"_s) {
        settings.setUserAgent(nativePropertyValue);
    } else if (nativePropertyName == "WebKitMaximumHTMLParserDOMTreeDepth"_s) {
        settings.setMaximumHTMLParserDOMTreeDepth(parseIntegerAllowingTrailingJunk<uint32_t>(nativePropertyString).value());
    } /*else if (nativePropertyName == "WebKitXSSAuditorEnabled"_s)  {
       settings.setXSSAuditorEnabled(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    }*/
     else if (nativePropertyName == "WebKitSerifFontFamily"_s) {
        settings.setSerifFontFamily(nativePropertyValue);
    } else if (nativePropertyName == "WebKitSansSerifFontFamily"_s) {
        settings.setSansSerifFontFamily(nativePropertyValue);
    } else if (nativePropertyName == "WebKitFixedFontFamily"_s) {
        settings.setFixedFontFamily(nativePropertyValue);
    } else if (nativePropertyName == "WebKitShowsURLsInToolTips"_s) {
        settings.setShowsURLsInToolTips(parseIntegerAllowingTrailingJunk<int>(nativePropertyString).value());
    } else if (nativePropertyName == "JavaScriptCanAccessClipboard"_s) {
        settings.setJavaScriptCanAccessClipboard(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "allowTopNavigationToDataURLs"_s) {
        settings.setAllowTopNavigationToDataURLs(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "UsesBackForwardCache"_s) {
        settings.setUsesBackForwardCache(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "enableColorFilter"_s) {
        settings.setColorFilterEnabled(nativePropertyValue == "true"_s);
    } /*else if (nativePropertyName == "KeygenElementEnabled"_s) {
        // removed from Chrome, Firefox, and the HTML specification in 2017.
        // https://trac.webkit.org/changeset/248960/webkit
        DeprecatedGlobalSettings::setKeygenElementEnabled(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "CSSCustomPropertiesAndValuesEnabled"_s) {
        settings.setCSSCustomPropertiesAndValuesEnabled(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "experimental:CSSCustomPropertiesAndValuesEnabled"_s) {
        settings.setCSSCustomPropertiesAndValuesEnabled(nativePropertyValue == "true"_s);
    } */else if (nativePropertyName == "IntersectionObserverEnabled"_s) {
#if ENABLE(INTERSECTION_OBSERVER)
        settings.setIntersectionObserverEnabled(nativePropertyValue == "true"_s);
#endif
    } else if (nativePropertyName == "enableIntersectionObserver"_s) {
#if ENABLE(INTERSECTION_OBSERVER)
        settings.setIntersectionObserverEnabled(nativePropertyValue == "true"_s);
#endif
    } else if (nativePropertyName == "ResizeObserverEnabled"_s) {
#if ENABLE(RESIZE_OBSERVER)
        settings.setResizeObserverEnabled(nativePropertyValue == "true"_s);
#endif
    } else if (nativePropertyName == "RequestIdleCallbackEnabled"_s) {
        settings.setRequestIdleCallbackEnabled(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "FontFaceSetConstructorEnabled"_s) {
        settings.setFontFaceSetConstructorEnabled(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "ContactPickerAPIEnabled"_s) {
        settings.setContactPickerAPIEnabled(nativePropertyValue == "true"_s);
    } else if (nativePropertyName == "AttachmentElementEnabled"_s) {
#if ENABLE(ATTACHMENT_ELEMENT)
        DeprecatedGlobalSettings::setAttachmentElementEnabled(nativePropertyValue == "true"_s);
#endif
    } else if (nativePropertyName == "jscOptions"_s && !nativePropertyValue.isEmpty()) {
        JSC::Options::setOptions(nativePropertyValue.utf8().data());
    }
}

WKJ_EXPORT void wkj_page_reset_for_testing(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
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
    settings.setDefaultTextEncodingName("ISO-8859-1"_s);
    //settings.setJavaEnabled(false);
    settings.setFullScreenEnabled(true);
    settings.setScriptEnabled(true);
    settings.setEditableLinkBehavior(EditableLinkBehavior::OnlyLiveWithShiftKey);
    // settings.setTabsToLinks(false);
    settings.setDOMPasteAllowed(true);
    settings.setShouldPrintBackgrounds(true);
    // settings.setCacheModel(WebCacheModelDocumentBrowser);
    //settings.setXSSAuditorEnabled(false); //
   // settings.setPluginsEnabled(true);
    settings.setTextAreasAreResizable(true);
    settings.setUsesBackForwardCache(false);
    settings.setRequestIdleCallbackEnabled(true);
    settings.setFontFaceSetConstructorEnabled(false);

    // settings.setPrivateBrowsingEnabled(false);
    settings.setAllowTopNavigationToDataURLs(true);
    settings.setAuthorAndUserStylesEnabled(true);
    // Shrinks standalone images to fit: YES
    settings.setJavaScriptCanOpenWindowsAutomatically(true);
    settings.setJavaScriptCanAccessClipboard(true);
   // settings.setOfflineWebApplicationCacheEnabled(true);
    settings.setDataTransferItemsEnabled(true);
    // settings.setDeveloperExtrasEnabled(false);
    settings.setJavaScriptRuntimeFlags(JSC::RuntimeFlags(0));
    // Set JS experiments enabled: YES
    //settings.setLoadsImagesAutomatically(true);
    //settings.setLoadsSiteIconsIgnoringImageLoadingSetting(false);
    //settings.setFrameFlattening(FrameFlattening::Disabled);
    //settings.setFontRenderingMode(FontRenderingMode::Normal);
    // Doesn't work well with DRT
    settings.setScrollAnimatorEnabled(false);
    // Set spatial navigation enabled: NO

    // Set WebGL Enabled: NO
    // settings.setCSSRegionsEnabled(true);
    // Set uses HTML5 parser quirks: NO
    // Async spellcheck: NO
    DeprecatedGlobalSettings::setMockScrollbarsEnabled(true);

    //DeprecatedGlobalSettings::setHighlightAPIEnabled(true);
    // RuntimeEnabledFeatures::sharedFeatures().setModernMediaControlsEnabled(false);
    //DeprecatedGlobalSettings::setInspectorAdditionsEnabled(true); // deprecated and not enable
    // RuntimeEnabledFeatures::sharedFeatures().clearNetworkLoaderSession();

        Frame* mainFrame = (Frame*)&page->mainFrame();
    auto* coreFrame = dynamicDowncast<LocalFrame>(mainFrame);
    auto globalContext = toGlobalRef(coreFrame->script().globalObject(mainThreadNormalWorldSingleton()));
    WebCoreTestSupport::resetInternalsObject(globalContext);
}

WKJ_EXPORT float wkj_frame_get_zoom(int64_t pFrame, int32_t textOnly)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    ASSERT(frame);
    if (!frame) {
        return 1.0;
    }
    return textOnly
        ? frame->textZoomFactor()
        : frame->pageZoomFactor();
}

WKJ_EXPORT void wkj_frame_set_zoom(int64_t pFrame, float zoomFactor, int32_t textOnly)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
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

WKJ_EXPORT void wkj_frame_clear_name(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return;
    }

    frame->tree().clearName();
}

WKJ_EXPORT int32_t wkj_page_begin_printing(int64_t pPage, float width, float height)
{
    WKJCallScope wkjScope;
    return WebPage::webPageFromPeer(pPage)->beginPrinting(width, height);
}

WKJ_EXPORT void wkj_page_end_printing(int64_t pPage)
{
    WKJCallScope wkjScope;
    return WebPage::webPageFromPeer(pPage)->endPrinting();
}

WKJ_EXPORT void wkj_page_print(int64_t pPage, wkj_ref rq, int32_t pageIndex, float width)
{
    WKJCallScope wkjScope;
    auto webPage = WebPage::webPageFromPeer(pPage);
    PlatformContextJava* ppgc = new PlatformContextJava(rq, webPage->jRenderTheme());
    GraphicsContextJava gc(ppgc);
    webPage->print(gc, pageIndex, width);
}

WKJ_EXPORT int32_t wkj_frame_height(int64_t pFrame)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
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

WKJ_EXPORT float wkj_frame_adjust_height(int64_t pFrame, float oldTop, float oldBottom, float bottomLimit)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->view()) {
        return oldBottom;
    }

    float result;
    frame->view()->adjustPageHeightDeprecated(&result, oldTop, oldBottom, bottomLimit);
    return result;
}

WKJ_EXPORT void wkj_page_set_bounds(int64_t pPage, int32_t x, int32_t y, int32_t w, int32_t h)
{
    WKJCallScope wkjScope;
    WebPage::webPageFromPeer(pPage)->setSize(IntSize(w, h));
}

/* out_xywh receives x, y, width, height. 0 where the JNI version returned a null array. */
WKJ_EXPORT int32_t wkj_frame_visible_rect(int64_t pFrame, int32_t* out_xywh)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->view() || !out_xywh) {
        return 0;
    }
    IntRect rect = frame->view()->visibleContentRect();

    out_xywh[0] = rect.x();
    out_xywh[1] = rect.y();
    out_xywh[2] = rect.width();
    out_xywh[3] = rect.height();

    return 1;
}

WKJ_EXPORT void wkj_frame_scroll_to(int64_t pFrame, int32_t x, int32_t y)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->view()) {
        return;
    }
    frame->view()->setScrollPosition(IntPoint(x, y));
}

/* out_wh receives width, height. 0 where the JNI version returned a null array. */
WKJ_EXPORT int32_t wkj_frame_content_size(int64_t pFrame, int32_t* out_wh)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->view() || !out_wh) {
        return 0;
    }
    IntSize size = frame->view()->contentsSize();

    out_wh[0] = size.width();
    out_wh[1] = size.height();

    return 1;
}

WKJ_EXPORT void wkj_frame_set_transparent(int64_t pFrame, int32_t isTransparent)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->view()) {
        return;
    }
    frame->view()->setTransparent(isTransparent);
}

WKJ_EXPORT void wkj_frame_set_background_color(int64_t pFrame, int32_t backgroundColor)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame || !frame->view()) {
        return;
    }
    frame->view()->setBaseBackgroundColor(asSRGBA(WebCore::PackedColor::RGBA { static_cast<uint32_t>(backgroundColor) }));
}

WKJ_EXPORT void wkj_page_pre_paint(int64_t pPage)
{
    WKJCallScope wkjScope;
    WebPage::webPageFromPeer(pPage)->prePaint();
}

WKJ_EXPORT void wkj_page_update_content(int64_t pPage, wkj_ref rq, int32_t x, int32_t y,
                                       int32_t w, int32_t h)
{
    WKJCallScope wkjScope;
    WebPage::webPageFromPeer(pPage)->paint(rq, x, y, w, h);
}

WKJ_EXPORT void wkj_page_update_rendering(int64_t pPage)
{
    WKJCallScope wkjScope;
    WebPage::pageFromPeer(pPage)->isolatedUpdateRendering();
}

WKJ_EXPORT void wkj_page_post_paint(int64_t pPage, wkj_ref rq, int32_t x, int32_t y,
                                   int32_t w, int32_t h)
{
    WKJCallScope wkjScope;
    WebPage::webPageFromPeer(pPage)->postPaint(rq, x, y, w, h);
}

WKJ_EXPORT int32_t wkj_page_get_encoding(int64_t pPage, uint16_t* result_buf, int32_t result_cap,
                                         int32_t* result_length)
{
    WKJCallScope wkjScope;
    Page* p = WebPage::pageFromPeer(pPage);
    ASSERT(p);
        Frame* mainFrame = (Frame*)&p->mainFrame();
    ASSERT(mainFrame);

    auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    return WKJReturnString(result_buf, result_cap, result_length, String::fromUTF8(frame->document()->charset().span()));
}

WKJ_EXPORT void wkj_page_set_encoding(int64_t pPage, const uint16_t* encoding, int32_t encoding_length)
{
    WKJCallScope wkjScope;
    Page* p = WebPage::pageFromPeer(pPage);
    ASSERT(p);
        Frame* mainFrame = (Frame*)&p->mainFrame();

    ASSERT(mainFrame);
    auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    frame->loader().reloadWithOverrideEncoding(WKJString(encoding, encoding_length));
}

WKJ_EXPORT void wkj_page_focus_event(int64_t pPage, int32_t id, int32_t direction)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* frame = (Frame*)&page->mainFrame();
        auto* mainFrame = dynamicDowncast<LocalFrame>(frame);

    FocusController& focusController = page->focusController();

    LocalFrame* focusedFrame = focusController.focusedLocalFrame();
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
                focusController.advanceFocus(FocusDirection::Backward, nullptr);
            } else if (direction == com_sun_webkit_event_WCFocusEvent_BACKWARD) {
                // comment out the following line to get focus to the last
                // focused node instead of the last focusable one
                focusedFrame->document()->setFocusedElement(0);
                focusController.advanceFocus(FocusDirection::Backward, nullptr);
            }
            break;
        case com_sun_webkit_event_WCFocusEvent_FOCUS_LOST:
            focusController.setFocused(false); // focus lost
            focusController.setActive(false); // window deactivation
            break;
    }
}

WKJ_EXPORT int32_t wkj_page_key_event(int64_t pPage, int32_t type,
                                      const uint16_t* text, int32_t text_length,
                                      const uint16_t* keyIdentifier,
                                      int32_t keyIdentifier_length,
                                      int32_t windowsVirtualKeyCode,
                                      int32_t shift, int32_t ctrl, int32_t alt,
                                      int32_t meta, double timestamp)
{
    WKJCallScope wkjScope;
    WebPage* webPage = WebPage::webPageFromPeer(pPage);

    PlatformKeyboardEvent event(type, text, text_length,
                                keyIdentifier, keyIdentifier_length,
                                windowsVirtualKeyCode,
                                shift, ctrl, alt, meta, timestamp);

    return (webPage->processKeyEvent(event)) ? 1 : 0;
}

WKJ_EXPORT int32_t wkj_page_mouse_event(int64_t pPage, int32_t id, int32_t button, int32_t buttonMask,
                                        int32_t clickCount, int32_t x, int32_t y, int32_t screenX, int32_t screenY,
                                        int32_t shift, int32_t ctrl, int32_t alt, int32_t meta, int32_t popupTrigger,
                                        double timestamp)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    // Uncomment to debug mouse events
    // fprintf(stderr, "twkProcessKeyEvent: "
    //         "id=%d button=%d clickCount=%d x=%d y=%d"
    //         "screenX=%d screenY=%d \n",
    //         id, button, clickCount, x, y, screenX, screenY);

    EventHandler& eventHandler = frame->eventHandler();

    LocalFrameView* frameView = frame->view();
    if (!frameView) {
        return false;
    }

    bool consumeEvent = false;
    IntPoint loc(x, y);
    PlatformMouseEvent mouseEvent = PlatformMouseEvent(loc,
                                                       IntPoint(screenX, screenY),
                                                       getWebCoreMouseButton(button),
                                                       getWebCoreMouseButtons(buttonMask),
                                                       getWebCoreMouseEventType(id),
                                                       clickCount,
                                                       shift, ctrl, alt, meta,
                                                       MonotonicTime::fromRawSeconds(timestamp), ForceAtClick, SyntheticClickType::NoTap); // TODO-java: handle force?
    switch (id) {
    case com_sun_webkit_event_WCMouseEvent_MOUSE_PRESSED:
        //frame->focusWindow();
        page->chrome().focus();
        consumeEvent = eventHandler.handleMousePressEvent(mouseEvent).wasHandled();
        break;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_RELEASED:
        consumeEvent = eventHandler.handleMouseReleaseEvent(mouseEvent).wasHandled();
        break;
    case com_sun_webkit_event_WCMouseEvent_MOUSE_MOVED:
    case com_sun_webkit_event_WCMouseEvent_MOUSE_DRAGGED:
        consumeEvent = eventHandler.mouseMoved(mouseEvent).wasHandled();
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

        LocalFrame* frame = node->document().frame();
        // we do not want to show context menu for frameset (see 6648628)
        if (frame && !frame->document()->isFrameSet()) {
            /*
             * The Java WebPage the context menu belongs to comes from PageSupplementJava
             * rather than from the `self` parameter twkProcessMouseEvent used to have.
             * Same object, same call.
             */
            WKJHandle menuPage = WebPage::jobjectFromPage(page);
            ContextMenuJava(contextMenu->items()).show(&cmc, menuPage.get(), loc);
        }
        return 1;
    }

    return (consumeEvent) ? 1 : 0;
}

WKJ_EXPORT int32_t wkj_page_wheel_event(int64_t pPage, int32_t x, int32_t y, int32_t screenX, int32_t screenY,
                                        float deltaX, float deltaY, int32_t shift, int32_t ctrl, int32_t alt,
                                        int32_t meta, double timestamp)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    PlatformWheelEvent wheelEvent = PlatformWheelEvent(IntPoint(x, y),
                                                       IntPoint(screenX, screenY),
                                                       deltaX, deltaY,
                                                       shift, ctrl, alt, meta);
    OptionSet<WheelEventProcessingSteps> processingSteps =
    {
        WheelEventProcessingSteps::SynchronousScrolling,
        WheelEventProcessingSteps::BlockingDOMEventDispatch
    };

    bool consumeEvent = frame->eventHandler().handleWheelEvent(wheelEvent, processingSteps).first.wasHandled();

    return (consumeEvent) ? 1 : 0;
}

/*
 * `attributes` is the flat int[] of (startOffset, endOffset, thick) triples the Java side
 * already builds and attribute_count is its length, not the number of triples - the same
 * numbers GetArrayLength returned. The JNI version released the array with JNI_ABORT,
 * which was correct because it only read it.
 */
WKJ_EXPORT int32_t wkj_page_input_text_change(int64_t pPage,
     const uint16_t* jcommitted, int32_t jcommitted_length,
     const uint16_t* jcomposed, int32_t jcomposed_length,
     const int32_t* attributes, int32_t attribute_count,
     int32_t caretPosition)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);

    LocalFrame* frame = page->focusController().focusedOrMainFrame();
    ASSERT(frame);

    if (!frame || !frame->editor().canEdit()) {
        // There's no client to deliver the event. Consume the event
        // so that it won't be delivered to a wrong webkit client.
        return 1;
    }

    // Process committed text first
    if (jcommitted_length > 0 ||
            // if both committed and composed are empty, confirm with an empty text
            (jcomposed_length == 0)) {
        String committed = WKJString(jcommitted, jcommitted_length);
        frame->editor().confirmComposition(committed);
    }

    // Process composed (composition) text here
    if (jcomposed_length > 0) {
        int32_t length = attribute_count;
        Vector<CompositionUnderline> underlines;
        underlines.resize(length / 3); // 3 members per element
        const int32_t* attrs = attributes;
        if (attrs) {
            for (int i = 0; i < length;) {
                int x = i / 3;
                underlines[x].startOffset = attrs[i++];
                underlines[x].endOffset = attrs[i++];
                underlines[x].thick = (attrs[i++] == 1);
                underlines[x].color = Color::black;
            }
        }
        String composed = WKJString(jcomposed, jcomposed_length);
        frame->editor().setComposition(composed, underlines, { }, { }, caretPosition, 0);
    }
    return 1;
}

WKJ_EXPORT int32_t wkj_page_caret_position_change(int64_t pPage, int32_t caretPosition)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);

    LocalFrame* frame = page->focusController().focusedOrMainFrame();

    ASSERT(frame);

    Text* text = frame->editor().compositionNode();
    if (!text) {
        return 0;
    }

    // FIXME: the following code may not work with having committed text
    Position position(text, caretPosition);
    VisibleSelection selection(position, Affinity::Downstream);
    frame->selection().setSelection(selection /*, default is CharacterGranularity*/);//true, false, false
    return 1;
}

/*
 * out_xywh receives x, y, width, height; the return value says whether they were written.
 *
 * Worth recording: the JNI version filled a fresh int[] under GetPrimitiveArrayCritical
 * and released it with JNI_ABORT, which discards the writes unless the VM pinned rather
 * than copied. It worked only because HotSpot pins. The out-parameter removes that
 * dependence on unspecified behaviour; the values Java sees are the same.
 */
WKJ_EXPORT int32_t wkj_page_text_location(int64_t pPage, int32_t charindex, int32_t* out_xywh)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    if (!out_xywh)
        return 0;
    /* The JNI version handed back a freshly allocated, zero filled array, so a caller
       that ignored the null-view case still saw four zeroes. Keep that. */
    out_xywh[0] = 0;
    out_xywh[1] = 0;
    out_xywh[2] = 0;
    out_xywh[3] = 0;


    LocalFrameView* frameView = frame->view();
    if (!frameView)
        return 0;

    IntRect caret = frame->selection().absoluteCaretBounds();
    caret = frameView->contentsToWindow(caret);
    out_xywh[0] = caret.x();
    out_xywh[1] = caret.y();
    out_xywh[2] = caret.width();
    out_xywh[3] = caret.height();

    return 1;
}

WKJ_EXPORT int32_t wkj_page_insert_position_offset(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    int32_t position = 0;
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

WKJ_EXPORT int32_t wkj_page_committed_text_length(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    int32_t length = 0;
    Editor &editor = frame->editor();
    if (editor.canEdit()) {
        SimpleRange range = makeRangeSelectingNodeContents(*(Node*)frame->selection().selection().start().anchorElementAncestor().get());
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

WKJ_EXPORT int32_t wkj_page_committed_text(int64_t pPage, uint16_t* result_buf, int32_t result_cap,
                                           int32_t* result_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    String text;

    Editor &editor = frame->editor();
    if (editor.canEdit()) {
        auto range = makeRangeSelectingNodeContents(*(Node*)frame->selection().selection().start().anchorElementAncestor().get());
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
                    t = makeString(s, t.substring(end, length - start));
                }
            }
            text = t;
        }
    }
    return WKJReturnString(result_buf, result_cap, result_length, text);
}

WKJ_EXPORT int32_t wkj_page_selected_text(int64_t pPage, uint16_t* result_buf, int32_t result_cap,
                                          int32_t* result_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    String t = frame->editor().selectedText();
    return WKJReturnString(result_buf, result_cap, result_length, t);
}

//java.awt.dnd.DConstants
enum JAVA_DND_ACTION {
    ACTION_NONE = 0x0,
    ACTION_COPY = 0x1,
    ACTION_MOVE = 0x2,
    ACTION_LINK = 0x40000000
};

static int32_t dragOperationToDragCursor(std::optional<DragOperation> operation) {
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

static OptionSet<DragOperation> keyStateToDragOperation(int32_t javaAction) {
    OptionSet<DragOperation> action = { };
    if(javaAction & ACTION_COPY)
        action = { DragOperation::Copy };
    else if(javaAction & ACTION_LINK)
        action = { DragOperation::Link };
    else if(javaAction & ACTION_MOVE)
        action = { DragOperation::Move };
    return action;
}

/*
 * mime_count selects the branch, replacing the "jMimes == NULL" test of the JNI version:
 * a count of zero or more is the drop-target branch and a negative count the drag-source
 * branch, in which the four array parameters are ignored and may be NULL. An entry whose
 * value pointer is NULL is skipped, which is what the loop did for a null array element.
 */
WKJ_EXPORT int32_t wkj_page_process_drag(int64_t pPage,
 int32_t actionId,
 const uint16_t* const* mimes, const int32_t* mime_lengths,
 const uint16_t* const* values, const int32_t* value_lengths,
 int32_t mime_count,
 int32_t x, int32_t y,
 int32_t screenX, int32_t screenY,
 int32_t javaAction) {
    WKJCallScope wkjScope;
    if (mime_count >= 0) {
        //TRAGET
        RefPtr<DataObjectJava> pr = DataObjectJava::create();
        /* The four arrays travel together; a caller that passes none supplies no data. */
        int32_t n = (mimes && mime_lengths && values && value_lengths) ? mime_count : 0;
        for( int32_t j=0; j<n; ++j ){
            const uint16_t* value = values ? values[j] : nullptr;
            if(value){
                pr->setData(
                    WKJString(mimes[j], mime_lengths[j]),
                    WKJString(value, value_lengths[j]));
            }
        }
        DragData dragData(
            pr.get(),
            IntPoint(x, y),
            IntPoint(screenX, screenY),
            keyStateToDragOperation(javaAction));
        DragController& dc = WebPage::pageFromPeer(pPage)->dragController();
        RefPtr localMainFrame = dynamicDowncast<WebCore::LocalFrame>(WebPage::pageFromPeer(pPage)->mainFrame());
        if (!localMainFrame)
        return 0;
        setCopyKeyState(ACTION_COPY == javaAction);
        switch(actionId){
        case com_sun_webkit_WebPage_DND_DST_EXIT:
            dc.dragExited(*localMainFrame,WTF::move(dragData));
            return 0;
        case com_sun_webkit_WebPage_DND_DST_ENTER:
        case com_sun_webkit_WebPage_DND_DST_OVER:
        case com_sun_webkit_WebPage_DND_DST_CHANGE:
            return dragOperationToDragCursor(std::get<std::optional<WebCore::DragOperation>>(dc.dragEnteredOrUpdated(*localMainFrame, WTF::move(dragData))));
        case com_sun_webkit_WebPage_DND_DST_DROP:
            {
                int ret = dc.performDragOperation(WTF::move(dragData)) ? 1 : 0;
                WebPage::pageFromPeer(pPage)->dragController().dragEnded();
                return ret;
            }
        }
    } else {
        //SOURCE
                Page* p = WebPage::pageFromPeer(pPage);
                Frame* mainFrame = (Frame*)&p->mainFrame();
            auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
        EventHandler& eventHandler =
                frame->eventHandler();
        PlatformMouseEvent mouseEvent = PlatformMouseEvent(
            IntPoint(x, y),
            IntPoint(screenX, screenY),
            com_sun_webkit_WebPage_DND_SRC_DROP!=actionId
                ? MouseButton::Left
                : MouseButton::None,
            PlatformEvent::Type::MouseMoved,
            0,
            { }, MonotonicTime {}, ForceAtClick, SyntheticClickType::NoTap); // TODO-java: handle force?
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
    LocalFrame* framePtr = page->focusController().focusedOrMainFrame();
    ASSERT(framePtr);

    if (framePtr) {
        return &framePtr->editor();
    }
    return nullptr;
}

WKJ_EXPORT int32_t wkj_page_execute_command(int64_t pPage, const uint16_t* command, int32_t command_length,
                                            const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return 0;
    }
    Editor::Command cmd = editor->command(WKJString(command, command_length));
    return (cmd.execute(value ? WKJString(value, value_length) : String())) ? 1 : 0;
}

WKJ_EXPORT int32_t wkj_page_query_command_enabled(int64_t pPage, const uint16_t* command, int32_t command_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return 0;
    }
    Editor::Command cmd = editor->command(WKJString(command, command_length));
    return (cmd.isEnabled()) ? 1 : 0;
}

WKJ_EXPORT int32_t wkj_page_query_command_state(int64_t pPage, const uint16_t* command, int32_t command_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return 0;
    }
    Editor::Command cmd = editor->command(WKJString(command, command_length));
    return (cmd.state() == TriState::True) ? 1 : 0;
}

WKJ_EXPORT int32_t wkj_page_query_command_value(int64_t pPage, const uint16_t* command, int32_t command_length,
                                                uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    Editor* editor = getEditor(page);
    if (!editor) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }
    Editor::Command cmd = editor->command(WKJString(command, command_length));
    return WKJReturnString(result_buf, result_cap, result_length, cmd.value());
}

WKJ_EXPORT int32_t wkj_page_is_editable(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    if (!page) {
        return 0;
    }
    return (page->isEditable()) ? 1 : 0;
}

WKJ_EXPORT void wkj_page_set_editable(int64_t pPage, int32_t editable)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    if (!page) {
        return;
    }
    page->setEditable((editable != 0));
}

WKJ_EXPORT int32_t wkj_frame_html(int64_t pFrame, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    if (!frame) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    Document* document = frame->document();
    if (!document || !document->isHTMLDocument()) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    HTMLElement* documentElement =
            static_cast<HTMLElement*>(document->documentElement());
    if (!documentElement) {
        return WKJReturnString(result_buf, result_cap, result_length, String());
    }

    return WKJReturnString(result_buf, result_cap, result_length, documentElement->outerHTML());
}

WKJ_EXPORT int32_t wkj_page_get_use_page_cache(int64_t pPage)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    return (page->settings().usesBackForwardCache()) ? 1 : 0;
}

WKJ_EXPORT void wkj_page_set_use_page_cache(int64_t pPage, int32_t usePageCache)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    page->settings().setUsesBackForwardCache((usePageCache != 0));
}

WKJ_EXPORT int32_t wkj_page_is_script_enabled(int64_t pPage)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
        Frame* mainFrame = (Frame*)&page->mainFrame();
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);
    return (frame->script().canExecuteScripts(ReasonForCallingCanExecuteScripts::NotAboutToExecuteScript)) ? 1 : 0;
}

WKJ_EXPORT void wkj_page_set_script_enabled(int64_t pPage, int32_t enable)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    page->settings().setScriptEnabled((enable != 0));
}

WKJ_EXPORT int32_t wkj_page_is_context_menu_enabled(int64_t pPage)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    return (page->settings().isContextMenuEnabled()) ? 1 : 0;
}

WKJ_EXPORT void wkj_page_set_context_menu_enabled(int64_t pPage, int32_t enable)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    page->settings().setContextMenuEnabled((enable != 0));
}

WKJ_EXPORT void wkj_page_set_user_stylesheet(int64_t pPage, const uint16_t* url, int32_t url_length)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    page->settings().setUserStyleSheetLocation(URL(URL(), WKJString(url, url_length)));
}

WKJ_EXPORT int32_t wkj_page_get_user_agent(int64_t pPage, uint16_t* result_buf, int32_t result_cap,
                                           int32_t* result_length)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    return WKJReturnString(result_buf, result_cap, result_length, page->settings().userAgent());
}

WKJ_EXPORT void wkj_page_set_user_agent(int64_t pPage, const uint16_t* userAgent, int32_t userAgent_length)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    page->settings().setUserAgent(WKJString(userAgent, userAgent_length));
}

WKJ_EXPORT void wkj_page_set_local_storage_path(int64_t pPage, const uint16_t* path, int32_t path_length)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    Settings& settings = page->settings();
    settings.setLocalStorageDatabasePath(WKJString(path, path_length));
    static_cast<WebStorageNamespaceProviderJava*>(
      &page->storageNamespaceProvider())
        ->setLocalStorageDatabasePath(settings.localStorageDatabasePath());
}

WKJ_EXPORT void wkj_page_set_local_storage_enabled(int64_t pPage, int32_t enabled)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    Settings& settings = page->settings();
    settings.setLocalStorageEnabled((enabled != 0));
}

WKJ_EXPORT int32_t wkj_page_get_developer_extras(int64_t pPage)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    return (page->settings().developerExtrasEnabled()) ? 1 : 0;
}

WKJ_EXPORT void wkj_page_set_developer_extras(int64_t pPage, int32_t enabled)
{
    WKJCallScope wkjScope;
    ASSERT(pPage);
    Page* page = WebPage::pageFromPeer(pPage);
    ASSERT(page);
    page->settings().setDeveloperExtrasEnabled((enabled != 0));
}

WKJ_EXPORT int32_t wkj_frame_unload_listener_count(int64_t pFrame)
{
    WKJCallScope wkjScope;
    ASSERT(pFrame);

        Frame* mainFrame = static_cast<Frame*>(wkj_to_ptr(pFrame));
        ASSERT(mainFrame);
        auto* frame = dynamicDowncast<LocalFrame>(mainFrame);

    return (int32_t)frame->document()->window()->pendingUnloadEventListeners();
}

WKJ_EXPORT void wkj_page_inspector_connect(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page *page = WebPage::pageFromPeer(pPage);
    if (page) {
        PageInspectorController& ic = page->inspectorController();
        InspectorClientJava* icj = static_cast<InspectorClientJava*>(ic.inspectorBackendClient());
        if (icj) {
            ic.connectFrontend(*icj, false);
        }

    }
    WebPage::webPageFromPeer(pPage)->debugStarted();
}

WKJ_EXPORT void wkj_page_inspector_disconnect(int64_t pPage)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (!page) {
        return;
    }

    PageInspectorController& ic = page->inspectorController();
    InspectorClientJava* icj = static_cast<InspectorClientJava*>(ic.inspectorBackendClient());
    if (icj) {
        ic.disconnectFrontend(*icj);
    }

    WebPage::webPageFromPeer(pPage)->debugEnded();
}

WKJ_EXPORT void wkj_page_inspector_dispatch(int64_t pPage, const uint16_t* message, int32_t message_length)
{
    WKJCallScope wkjScope;
    Page* page = WebPage::pageFromPeer(pPage);
    if (!page) {
        return;
    }
    //utatodo: seems that JDK-8126646 will back again
    //JSDOMWindowBase::commonVM()->timeoutChecker.reset(); // JDK-8126646
    page->inspectorController().dispatchMessageFromFrontend(
            WKJString(message, message_length));
}

WKJ_EXPORT int32_t wkj_worker_thread_count(void)
{
    WKJCallScope wkjScope;
    return WorkerThread::workerThreadCount();
}

/*
 * twkDoJSCGarbageCollection is gone rather than converted: its whole body was a call to
 * WebPage_doJSCGarbageCollection above, which is already an exported plain-C zero
 * argument function. The triage rules it WRAPPER, so Java binds that symbol directly
 * with FunctionDescriptor.ofVoid() instead of going through a second one.
 */

}
