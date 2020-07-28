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


#include "ChromeClientJava.h"
#if ENABLE(INPUT_TYPE_COLOR)
#include "ColorChooserJava.h"
#endif
#include <WebCore/ContextMenu.h>
#if ENABLE(DATE_AND_TIME_INPUT_TYPES)
#include <WebCore/DateTimeChooser.h>
#endif
#include "PopupMenuJava.h"
#include "SearchPopupMenuJava.h"
#include "WebPage.h"
#include <WebCore/DocumentLoader.h>
#include <WebCore/DragController.h>
#include <WebCore/FileChooser.h>
#include <WebCore/FileIconLoader.h>
#include <WebCore/FloatRect.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameLoadRequest.h>
#include <WebCore/FrameLoader.h>
#include <WebCore/FrameView.h>
#include <WebCore/HitTestResult.h>
#include <WebCore/Icon.h>
#include <WebCore/IntRect.h>
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <WebCore/ResourceRequest.h>
#include <WebCore/Widget.h>
#include <WebCore/WindowFeatures.h>
#include <wtf/URL.h>
#include <wtf/text/StringBuilder.h>

namespace ChromeClientJavaInternal {
//MVM -ready initialization
#define DECLARE_STATIC_CLASS(getFunctionName, sClassPath) \
static jclass getFunctionName() { \
    static JGClass cls(WTF::GetJavaEnv()->FindClass(sClassPath)); \
    ASSERT(cls); \
    return cls; \
}

DECLARE_STATIC_CLASS(getWebPageCls,   "com/sun/webkit/WebPage")
DECLARE_STATIC_CLASS(getRectangleCls, "com/sun/webkit/graphics/WCRectangle")
DECLARE_STATIC_CLASS(getPointCls,     "com/sun/webkit/graphics/WCPoint")

static jfieldID rectxFID = NULL; // Rectangle
static jfieldID rectyFID = NULL; // Rectangle
static jfieldID rectwFID = NULL; // Rectangle
static jfieldID recthFID = NULL; // Rectangle

static jmethodID pointCTOR = NULL; //Point
static jmethodID pointGetXMID = NULL; //Point
static jmethodID pointGetYMID = NULL; //Point

static jmethodID getHostWindowMID = NULL; // WebPage

static jmethodID getWindowBoundsMID = NULL; // WebPage
static jmethodID setWindowBoundsMID = NULL; // WebPage
static jmethodID getPageBoundsMID = NULL; // WebPage
static jmethodID setCursorMID = NULL; // WebPage
static jmethodID setFocusMID = NULL; // WebPage
static jmethodID transferFocusMID = NULL;
static jmethodID setTooltipMID = NULL;

static jmethodID createWindowMID = NULL;
static jmethodID showWindowMID = NULL;
static jmethodID closeWindowMID = NULL;

static jmethodID setScrollbarsVisibleMID = NULL;
static jmethodID setStatusbarTextMID = NULL;

static jmethodID alertMID = NULL;
static jmethodID confirmMID = NULL;
static jmethodID promptMID = NULL;

static jmethodID addMessageToConsoleMID = NULL; // WebPage

static jmethodID canRunBeforeUnloadConfirmPanelMID = NULL; // WebPage
static jmethodID runBeforeUnloadConfirmPanelMID = NULL; // WebPage

static jmethodID screenToWindowMID = NULL; // WebPage
static jmethodID windowToScreenMID = NULL; // WebPage


static jmethodID chooseFileMID = NULL; // WebPage

static void initRefs(JNIEnv* env)
{
    if (!getHostWindowMID) {
        getHostWindowMID = env->GetMethodID(getWebPageCls(), "getHostWindow",
                                            "()Lcom/sun/webkit/WCWidget;");
        ASSERT(getHostWindowMID);

        getWindowBoundsMID = env->GetMethodID(getWebPageCls(), "fwkGetWindowBounds",
                                              "()Lcom/sun/webkit/graphics/WCRectangle;");
        ASSERT(getWindowBoundsMID);
        setWindowBoundsMID = env->GetMethodID(getWebPageCls(), "fwkSetWindowBounds", "(IIII)V");
        ASSERT(setWindowBoundsMID);
        getPageBoundsMID = env->GetMethodID(getWebPageCls(), "fwkGetPageBounds",
                                            "()Lcom/sun/webkit/graphics/WCRectangle;");
        ASSERT(getPageBoundsMID);
        setCursorMID = env->GetMethodID(getWebPageCls(), "fwkSetCursor", "(J)V");
        ASSERT(setCursorMID);
        setFocusMID = env->GetMethodID(getWebPageCls(), "fwkSetFocus", "(Z)V");
        ASSERT(setFocusMID);
        transferFocusMID = env->GetMethodID(getWebPageCls(), "fwkTransferFocus", "(Z)V");
        ASSERT(transferFocusMID);
        setTooltipMID = env->GetMethodID(getWebPageCls(), "fwkSetTooltip",
                                         "(Ljava/lang/String;)V");
        ASSERT(setTooltipMID);

        createWindowMID = env->GetMethodID(getWebPageCls(), "fwkCreateWindow",
            "(ZZZZ)Lcom/sun/webkit/WebPage;");
        ASSERT(createWindowMID);
        closeWindowMID = env->GetMethodID(getWebPageCls(), "fwkCloseWindow", "()V");
        ASSERT(closeWindowMID);
        showWindowMID = env->GetMethodID(getWebPageCls(), "fwkShowWindow", "()V");
        ASSERT(showWindowMID);

        setScrollbarsVisibleMID = env->GetMethodID(getWebPageCls(), "fwkSetScrollbarsVisible", "(Z)V");
        ASSERT(setScrollbarsVisibleMID);
        setStatusbarTextMID = env->GetMethodID(getWebPageCls(), "fwkSetStatusbarText",
                                               "(Ljava/lang/String;)V");
        ASSERT(setStatusbarTextMID);
        alertMID = env->GetMethodID(getWebPageCls(), "fwkAlert", "(Ljava/lang/String;)V");
        ASSERT(alertMID);
        confirmMID = env->GetMethodID(getWebPageCls(), "fwkConfirm", "(Ljava/lang/String;)Z");
        ASSERT(confirmMID);
        promptMID = env->GetMethodID(getWebPageCls(), "fwkPrompt",
                                     "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(promptMID);

        addMessageToConsoleMID = env->GetMethodID(getWebPageCls(),
                "fwkAddMessageToConsole",
                "(Ljava/lang/String;ILjava/lang/String;)V");
        ASSERT(addMessageToConsoleMID);


        canRunBeforeUnloadConfirmPanelMID = env->GetMethodID(getWebPageCls(),
                "fwkCanRunBeforeUnloadConfirmPanel",
                "()Z");
        ASSERT(canRunBeforeUnloadConfirmPanelMID);

        runBeforeUnloadConfirmPanelMID = env->GetMethodID(getWebPageCls(),
                "fwkRunBeforeUnloadConfirmPanel",
                "(Ljava/lang/String;)Z");
        ASSERT(runBeforeUnloadConfirmPanelMID);

        screenToWindowMID = env->GetMethodID(getWebPageCls(), "fwkScreenToWindow",
            "(Lcom/sun/webkit/graphics/WCPoint;)Lcom/sun/webkit/graphics/WCPoint;");
        ASSERT(screenToWindowMID);

        windowToScreenMID = env->GetMethodID(getWebPageCls(), "fwkWindowToScreen",
            "(Lcom/sun/webkit/graphics/WCPoint;)Lcom/sun/webkit/graphics/WCPoint;");
        ASSERT(windowToScreenMID);

        chooseFileMID = env->GetMethodID(getWebPageCls(), "fwkChooseFile",
            "(Ljava/lang/String;ZLjava/lang/String;)[Ljava/lang/String;");
        ASSERT(chooseFileMID);
    }
    if (!rectxFID) {
        rectxFID = env->GetFieldID(getRectangleCls(), "x", "F");
        ASSERT(rectxFID);
        rectyFID = env->GetFieldID(getRectangleCls(), "y", "F");
        ASSERT(rectyFID);
        rectwFID = env->GetFieldID(getRectangleCls(), "w", "F");
        ASSERT(rectwFID);
        recthFID = env->GetFieldID(getRectangleCls(), "h", "F");
        ASSERT(recthFID);
    }
    if (!pointGetXMID) {
        pointGetXMID = env->GetMethodID(getPointCls(), "getX", "()F");
        ASSERT(pointGetXMID);
        pointGetYMID = env->GetMethodID(getPointCls(), "getY", "()F");
        ASSERT(pointGetYMID);
        pointCTOR = env->GetMethodID(getPointCls(), "<init>", "(FF)V");
        ASSERT(pointCTOR);
    }
}
}

namespace WebCore {

ChromeClientJava::ChromeClientJava(const JLObject &webPage)
    : m_webPage(webPage)
{
}

void ChromeClientJava::chromeDestroyed()
{
    delete this;
}

#if ENABLE(INPUT_TYPE_COLOR)
std::unique_ptr<ColorChooser> ChromeClientJava::createColorChooser(ColorChooserClient& client, const Color& initialColor)
{
    return std::make_unique<ColorChooserJava>(m_webPage, &client, initialColor);
}
#endif

FloatRect ChromeClientJava::windowRect()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLObject rect(env->CallObjectMethod(m_webPage, getWindowBoundsMID));
    WTF::CheckAndClearException(env);

    if (rect) {
        jfloat x = env->GetFloatField(rect, rectxFID);
        jfloat y = env->GetFloatField(rect, rectyFID);
        jfloat width = env->GetFloatField(rect, rectwFID);
        jfloat height = env->GetFloatField(rect, recthFID);
        return FloatRect(float(x), float(y), float(width), float(height));
    } else {
        return IntRect(0, 0, 0, 0);
    }
}

void ChromeClientJava::setWindowRect(const FloatRect &r)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallObjectMethod(m_webPage, setWindowBoundsMID,
                          (int)(r.x()), (int)(r.y()), (int)(r.width()), (int)(r.height()));
    WTF::CheckAndClearException(env);
}

FloatRect ChromeClientJava::pageRect()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    JLObject rect(env->CallObjectMethod(m_webPage, getPageBoundsMID));
    WTF::CheckAndClearException(env);

    if (rect) {
        jfloat x = env->GetFloatField(rect, rectxFID);
        jfloat y = env->GetFloatField(rect, rectyFID);
        jfloat width = env->GetFloatField(rect, rectwFID);
        jfloat height = env->GetFloatField(rect, recthFID);
        return FloatRect(float(x), float(y), float(width), float(height));
    } else {
        return FloatRect(0, 0, 0, 0);
    }
}

void ChromeClientJava::focus()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, setFocusMID, JNI_TRUE);
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::unfocus()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, setFocusMID, JNI_FALSE);
    WTF::CheckAndClearException(env);
}

bool ChromeClientJava::canTakeFocus(FocusDirection)
{
    return true;
}

void ChromeClientJava::takeFocus(FocusDirection direction)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, transferFocusMID, direction == FocusDirectionForward);
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::focusedElementChanged(Element*)
{
    notImplemented();
}

void ChromeClientJava::focusedFrameChanged(Frame*)
{
    notImplemented();
}

Page* ChromeClientJava::createWindow(
    Frame&,
    const FrameLoadRequest& req,
    const WindowFeatures& features,
    const NavigationAction& na)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLObject newWebPage(
        env->CallObjectMethod(
            m_webPage, createWindowMID,
            bool_to_jbool(features.menuBarVisible),
            bool_to_jbool(features.statusBarVisible),
            bool_to_jbool(features.toolBarVisible || features.locationBarVisible),
            bool_to_jbool(features.resizable)));
    WTF::CheckAndClearException(env);

    if (!newWebPage) {
        return 0;
    }

    Page* p = WebPage::pageFromJObject(newWebPage);
    if (!req.isEmpty()) {
        p->mainFrame().loader().load(
            FrameLoadRequest(p->mainFrame(), ResourceRequest(na.url()), req.shouldOpenExternalURLsPolicy()));
    }

    return p;
}

void ChromeClientJava::closeWindowSoon()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, closeWindowMID);
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::show()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, showWindowMID);
    WTF::CheckAndClearException(env);
}

bool ChromeClientJava::canRunModal()
{
    notImplemented();
    return false;
}

void ChromeClientJava::runModal()
{
    notImplemented();
}

void ChromeClientJava::setResizable(bool)
{
    notImplemented();
}

void ChromeClientJava::setToolbarsVisible(bool)
{
    notImplemented();
}

bool ChromeClientJava::toolbarsVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setStatusbarVisible(bool)
{
    notImplemented();
}

bool ChromeClientJava::statusbarVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setScrollbarsVisible(bool v)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, setScrollbarsVisibleMID, bool_to_jbool(v));
    WTF::CheckAndClearException(env);
}

bool ChromeClientJava::scrollbarsVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setMenubarVisible(bool)
{
    notImplemented();
}

bool ChromeClientJava::menubarVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setStatusbarText(const String& text)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, setStatusbarTextMID, (jstring)text.toJavaString(env));
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::setCursor(const Cursor& c)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, setCursorMID, c.platformCursor());
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::setCursorHiddenUntilMouseMoves(bool)
{
    notImplemented();
}

void ChromeClientJava::runJavaScriptAlert(Frame&, const String& text)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, alertMID, (jstring)text.toJavaString(env));
    WTF::CheckAndClearException(env);
}

bool ChromeClientJava::runJavaScriptConfirm(Frame&, const String& text)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    jboolean res = env->CallBooleanMethod(m_webPage, confirmMID, (jstring)text.toJavaString(env));
    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
}

bool ChromeClientJava::runJavaScriptPrompt(Frame&, const String& text,
                                           const String& defaultValue, String& result)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    bool resb = false;

    JLString resJ(static_cast<jstring>(
        env->CallObjectMethod(m_webPage, promptMID,
            (jstring)text.toJavaString(env),
            (jstring)defaultValue.toJavaString(env))
    ));
    WTF::CheckAndClearException(env);
    if (resJ) {
        result = String(env, resJ);
        resb = true;
    }

    return resb;
}

void ChromeClientJava::runOpenPanel(Frame&, FileChooser& fileChooser)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    StringBuilder builder;
    const Vector<String>& acceptTypeList = fileChooser.settings().acceptMIMETypes;
    for (unsigned i = 0; i < acceptTypeList.size(); ++i) {
        if (i > 0)
            builder.append(',');
        builder.append(acceptTypeList[i]);
    }

    JLString initialFilename;
    const Vector<String> &filenames = fileChooser.settings().selectedFiles;
    if (filenames.size() > 0) {
        initialFilename = filenames[0].toJavaString(env);
    }

    bool multiple = fileChooser.settings().allowsMultipleFiles;
    JLocalRef<jobjectArray> jfiles(static_cast<jobjectArray>(
        env->CallObjectMethod(m_webPage, chooseFileMID,
                              (jstring)initialFilename, multiple,
                              (jstring)(builder.toString().toJavaString(env)))));
    WTF::CheckAndClearException(env);

    if (jfiles) {
        Vector<String> files;
        jsize length = env->GetArrayLength(jfiles);
        for (int i = 0; i < length; i++) {
            JLString f((jstring) env->GetObjectArrayElement(jfiles, i));
            files.append(String(env, f));
        }
        fileChooser.chooseFiles(files);
    }
}

void ChromeClientJava::loadIconForFiles(const Vector<String>& filenames, FileIconLoader& loader)
{
    loader.iconLoaded(Icon::createIconForFiles(filenames));
}

bool ChromeClientJava::canRunBeforeUnloadConfirmPanel()
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    auto result = env->CallBooleanMethod(m_webPage, canRunBeforeUnloadConfirmPanelMID);
    WTF::CheckAndClearException(env);
    return result;
}

bool ChromeClientJava::runBeforeUnloadConfirmPanel(const String& message, Frame&)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    auto result = env->CallBooleanMethod(m_webPage, runBeforeUnloadConfirmPanelMID, (jstring)message.toJavaString(env));
    WTF::CheckAndClearException(env);
    return result;
}

void ChromeClientJava::addMessageToConsole(MessageSource, MessageLevel, const String& message,
    unsigned lineNumber, unsigned, const String& sourceID)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, addMessageToConsoleMID,
            (jstring)message.toJavaString(env),
            (jint)lineNumber,
            (jstring)sourceID.toJavaString(env));
    WTF::CheckAndClearException(env);
}

KeyboardUIMode ChromeClientJava::keyboardUIMode()
{
    return KeyboardAccessTabsToLinks;
}

void ChromeClientJava::mouseDidMoveOverElement(const HitTestResult& htr, unsigned, const String& toolTip, TextDirection)
{
    static Node* mouseOverNode = 0;
    Element* urlElement = htr.URLElement();
    if (urlElement && isDraggableLink(*urlElement)) {
        Node* overNode = htr.innerNode();
        URL url = htr.absoluteLinkURL();
        if (!url.isEmpty() && (overNode != mouseOverNode)) {
            setStatusbarText(url.string());
            mouseOverNode = overNode;
        }
    } else {
        if (mouseOverNode) {
            setStatusbarText("");
            mouseOverNode = 0;
        }
    }
    setToolTip(toolTip);
}

void ChromeClientJava::setToolTip(const String& toolTip)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLString toolTipStr(NULL);
    if (toolTip.length() > 0) {
        toolTipStr = toolTip.toJavaString(env);
    }
    env->CallVoidMethod(m_webPage, setTooltipMID, (jstring)toolTipStr);
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::print(Frame&)
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    static jmethodID mid =  env->GetMethodID(
        getWebPageCls(),
        "fwkPrint",
        "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_webPage, mid);
    WTF::CheckAndClearException(env);
}

void ChromeClientJava::exceededDatabaseQuota(Frame&, const String&, DatabaseDetails) {
    notImplemented();
}

void ChromeClientJava::reachedMaxAppCacheSize(int64_t)
{
    // FIXME: Free some space.
    notImplemented();
}

void ChromeClientJava::reachedApplicationCacheOriginQuota(SecurityOrigin&, int64_t)
{
    notImplemented();
}

void ChromeClientJava::attachRootGraphicsLayer(Frame&, GraphicsLayer* layer)
{
    WebPage::webPageFromJObject(m_webPage)->setRootChildLayer(layer);
}

void ChromeClientJava::setNeedsOneShotDrawingSynchronization()
{
    WebPage::webPageFromJObject(m_webPage)
            ->setNeedsOneShotDrawingSynchronization();
}

void ChromeClientJava::scheduleCompositingLayerFlush()
{
    WebPage::webPageFromJObject(m_webPage)->scheduleCompositingLayerSync();
}

void ChromeClientJava::attachViewOverlayGraphicsLayer(GraphicsLayer*)
{
    notImplemented();
}

// HostWindow interface
void ChromeClientJava::scroll(const IntSize& scrollDelta, const IntRect& rectToScroll, const IntRect& clipRect)
{
    WebPage::webPageFromJObject(m_webPage)->scroll(scrollDelta, rectToScroll, clipRect);
}

IntPoint ChromeClientJava::screenToRootView(const IntPoint& p) const
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);
    JLObject ptScreen(env->NewObject(
        getPointCls(),
        pointCTOR,
        jfloat(p.x()),
        jfloat(p.y())
    ));
    JLObject ptWindows(env->CallObjectMethod(
        m_webPage,
        screenToWindowMID,
        jobject(ptScreen)));
    return IntPoint(
        int(env->CallFloatMethod(
            ptWindows,
            pointGetXMID)),
        int(env->CallFloatMethod(
            ptWindows,
            pointGetYMID))
    );
}

IntRect ChromeClientJava::rootViewToScreen(const IntRect& r) const
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);
    JLObject ptWindow(env->NewObject(
        getPointCls(),
        pointCTOR,
        jfloat(r.x()),
        jfloat(r.y())
    ));
    JLObject ptScreen(env->CallObjectMethod(
        m_webPage,
        windowToScreenMID,
        jobject(ptWindow)));
    return IntRect(
        int(env->CallFloatMethod(
            ptScreen,
            pointGetXMID)),
        int(env->CallFloatMethod(
            ptScreen,
            pointGetYMID)),
        r.width(),
        r.height()
    );
}

IntPoint ChromeClientJava::accessibilityScreenToRootView(const WebCore::IntPoint& point) const
{
    return screenToRootView(point);
}

IntRect ChromeClientJava::rootViewToAccessibilityScreen(const WebCore::IntRect& rect) const
{
    return rootViewToScreen(rect);
}

void ChromeClientJava::intrinsicContentsSizeChanged(const IntSize&) const
{
    notImplemented();
}

PlatformPageClient ChromeClientJava::platformPageClient() const
{
    using namespace ChromeClientJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLObject hostWindow(env->CallObjectMethod(m_webPage, getHostWindowMID));
    ASSERT(hostWindow);
    WTF::CheckAndClearException(env);

    return hostWindow;
}

void ChromeClientJava::contentsSizeChanged(Frame&, const IntSize&) const
{
    notImplemented();
}

void ChromeClientJava::invalidateRootView(const IntRect&)
{
    // Nothing to do here as all necessary repaints are scheduled
    // by ChromeClientJava::scroll(). See also RT-29123.
}

void ChromeClientJava::invalidateContentsAndRootView(const IntRect& updateRect)
{
    repaint(updateRect);
}

void ChromeClientJava::invalidateContentsForSlowScroll(const IntRect& updateRect)
{
    repaint(updateRect);
}

void ChromeClientJava::repaint(const IntRect& r)
{
    WebPage::webPageFromJObject(m_webPage)->repaint(r);
}

bool ChromeClientJava::selectItemWritingDirectionIsNatural()
{
    return false;
}

bool ChromeClientJava::selectItemAlignmentFollowsMenuWritingDirection()
{
    return true;
}


RefPtr<PopupMenu> ChromeClientJava::createPopupMenu(PopupMenuClient& client) const
{
    return adoptRef(new PopupMenuJava(&client));
}

RefPtr<SearchPopupMenu> ChromeClientJava::createSearchPopupMenu(PopupMenuClient& client) const
{
    return adoptRef(new SearchPopupMenuJava(&client));
}

// End of HostWindow methods

RefPtr<Icon> ChromeClientJava::createIconForFiles(const Vector<String>& filenames)
{
    return Icon::createIconForFiles(filenames);
}

void ChromeClientJava::didFinishLoadingImageForElement(WebCore::HTMLImageElement&)
{
}

} // namespace WebCore
