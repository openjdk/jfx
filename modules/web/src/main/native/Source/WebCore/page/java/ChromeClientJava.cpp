/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "ChromeClientJava.h"
#if ENABLE(INPUT_TYPE_COLOR)
#include "ColorChooser.h"
#endif
#include "ContextMenu.h"
#if ENABLE(DATE_AND_TIME_INPUT_TYPES)
#include "DateTimeChooser.h"
#endif
#include "DocumentLoader.h"
#include "FileChooser.h"
#include "FileIconLoader.h"
#include "FloatRect.h"
#include "Frame.h"
#include "MainFrame.h"
#include "FrameLoadRequest.h"
#include "FrameLoader.h"
#include "FrameView.h"
#include "HitTestResult.h"
#include "Icon.h"
#include "IntRect.h"
#include "URL.h"
#include "NotImplemented.h"
#include "Page.h"
#include "PopupMenuJava.h"
#include "ResourceRequest.h"
#include "SearchPopupMenuJava.h"
#include "WebPage.h"
#include "Widget.h"
#include "WindowFeatures.h"

//MVM -ready initialization
#define DECLARE_STATIC_CLASS(getFunctionName, sClassPath) \
static jclass getFunctionName() { \
    static JGClass cls(WebCore_GetJavaEnv()->FindClass(sClassPath)); \
    ASSERT(cls); \
    return cls; \
}

DECLARE_STATIC_CLASS(getWebPageCls,   "com/sun/webkit/WebPage")
DECLARE_STATIC_CLASS(getRectangleCls, "com/sun/webkit/graphics/WCRectangle")
DECLARE_STATIC_CLASS(getPointCls,     "com/sun/webkit/graphics/WCPoint")
DECLARE_STATIC_CLASS(getUtilitiesCls, "com/sun/webkit/Utilities")

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

        screenToWindowMID = env->GetMethodID(getWebPageCls(), "fwkScreenToWindow",
            "(Lcom/sun/webkit/graphics/WCPoint;)Lcom/sun/webkit/graphics/WCPoint;");
        ASSERT(screenToWindowMID);

        windowToScreenMID = env->GetMethodID(getWebPageCls(), "fwkWindowToScreen",
            "(Lcom/sun/webkit/graphics/WCPoint;)Lcom/sun/webkit/graphics/WCPoint;");
        ASSERT(windowToScreenMID);

        chooseFileMID = env->GetMethodID(getWebPageCls(), "fwkChooseFile",
            "(Ljava/lang/String;Z)[Ljava/lang/String;");
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
PassOwnPtr<ColorChooser> ChromeClientJava::createColorChooser(ColorChooserClient*, const Color&) 
{
    return nullptr;
}
#endif

#if ENABLE(DATE_AND_TIME_INPUT_TYPES)
PassRefPtr<DateTimeChooser> ChromeClientJava::openDateTimeChooser(DateTimeChooserClient*, const DateTimeChooserParameters&)
{
    //return nullptr;
    return NULL;
}
#endif


FloatRect ChromeClientJava::windowRect()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLObject rect(env->CallObjectMethod(m_webPage, getWindowBoundsMID));
    CheckAndClearException(env);

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
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallObjectMethod(m_webPage, setWindowBoundsMID,
                          (int)(r.x()), (int)(r.y()), (int)(r.width()), (int)(r.height()));
    CheckAndClearException(env);
}

FloatRect ChromeClientJava::pageRect()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    JLObject rect(env->CallObjectMethod(m_webPage, getPageBoundsMID));
    CheckAndClearException(env);

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
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, setFocusMID, JNI_TRUE);
    CheckAndClearException(env);
}

void ChromeClientJava::unfocus()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, setFocusMID, JNI_FALSE);
    CheckAndClearException(env);
}

bool ChromeClientJava::canTakeFocus(FocusDirection)
{
    return true;
}

void ChromeClientJava::takeFocus(FocusDirection direction)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, transferFocusMID, direction == FocusDirectionForward);
    CheckAndClearException(env);
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
    Frame* f,
    const FrameLoadRequest& req,
    const WindowFeatures& features,
    const NavigationAction& action)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLObject newWebPage(
        env->CallObjectMethod(
            m_webPage, createWindowMID,
            bool_to_jbool(features.menuBarVisible),
            bool_to_jbool(features.statusBarVisible),
            bool_to_jbool(features.toolBarVisible || features.locationBarVisible),
            bool_to_jbool(features.resizable)));
    CheckAndClearException(env);

    if (!newWebPage) {
        return 0;
    }

    Page* p = WebPage::pageFromJObject(newWebPage);
    if (!req.isEmpty()) {
        p->mainFrame().loader().load(req);
    }

    return p;
}

void ChromeClientJava::closeWindowSoon()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, closeWindowMID);
    CheckAndClearException(env);
}

void ChromeClientJava::show()
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, showWindowMID);
    CheckAndClearException(env);
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

void ChromeClientJava::setResizable(bool r)
{
}

void ChromeClientJava::setToolbarsVisible(bool v)
{
}

bool ChromeClientJava::toolbarsVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setStatusbarVisible(bool v)
{
}

bool ChromeClientJava::statusbarVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setScrollbarsVisible(bool v)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, setScrollbarsVisibleMID, bool_to_jbool(v));
    CheckAndClearException(env);
}

bool ChromeClientJava::scrollbarsVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::scrollbarsModeDidChange() const
{
    notImplemented();
}

void ChromeClientJava::setMenubarVisible(bool v)
{
}

bool ChromeClientJava::menubarVisible()
{
    notImplemented();
    return false;
}

void ChromeClientJava::setStatusbarText(const String& text)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, setStatusbarTextMID, (jstring)text.toJavaString(env));
    CheckAndClearException(env);
}

void ChromeClientJava::setCursor(const Cursor& c)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    ASSERT(m_webPage);

    env->CallVoidMethod(m_webPage, setCursorMID, c.platformCursor());
    CheckAndClearException(env);
}

void ChromeClientJava::setCursorHiddenUntilMouseMoves(bool)
{
    notImplemented();
}

void ChromeClientJava::runJavaScriptAlert(Frame* frame, const String& text)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, alertMID, (jstring)text.toJavaString(env));
    CheckAndClearException(env);
}

bool ChromeClientJava::runJavaScriptConfirm(Frame* frame, const String& text)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    jboolean res = env->CallBooleanMethod(m_webPage, confirmMID, (jstring)text.toJavaString(env));
    CheckAndClearException(env);

    return jbool_to_bool(res);
}

bool ChromeClientJava::runJavaScriptPrompt(Frame* frame, const String& text,
                                           const String& defaultValue, String& result)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    bool resb = false;

    JLString resJ(static_cast<jstring>(
        env->CallObjectMethod(m_webPage, promptMID,
            (jstring)text.toJavaString(env),
            (jstring)defaultValue.toJavaString(env))
    ));
    CheckAndClearException(env);
    if (resJ) {
        result = String(env, resJ);
        resb = true;
    }

    return resb;
}

void ChromeClientJava::runOpenPanel(Frame* frame, PassRefPtr<FileChooser> fileChooser)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

/*
    WebFileChooserParams params;
    params.multiSelect = fileChooser->settings().allowsMultipleFiles;
#if ENABLE(DIRECTORY_UPLOAD)
    params.directory = fileChooser->settings().allowsDirectoryUpload;
#else
    params.directory = false;
#endif
    params.acceptMIMETypes = fileChooser->settings().acceptMIMETypes;
    // FIXME: Remove WebFileChooserParams::acceptTypes.
    StringBuilder builder;
    const Vector<String>& acceptTypeList = fileChooser->settings().acceptMIMETypes;
    for (unsigned i = 0; i < acceptTypeList.size(); ++i) {
        if (i > 0)
            builder.append(',');
        builder.append(acceptTypeList[i]);
    }
    params.acceptTypes = builder.toString();
    params.selectedFiles = fileChooser->settings().selectedFiles;
    if (params.selectedFiles.size() > 0)
        params.initialValue = params.selectedFiles[0];

*/
    JLString initialFilename;
    const Vector<String> &filenames = fileChooser->settings().selectedFiles;
    if (filenames.size() > 0) {
        initialFilename = filenames[0].toJavaString(env);
    }

    bool multiple = fileChooser->settings().allowsMultipleFiles;
    JLocalRef<jobjectArray> jfiles(static_cast<jobjectArray>(
        env->CallObjectMethod(m_webPage, chooseFileMID,
                              (jstring)initialFilename, multiple)));
    CheckAndClearException(env);

    if (jfiles) {
        Vector<String> files;
        jsize length = env->GetArrayLength(jfiles);
        for (int i = 0; i < length; i++) {
            JLString f((jstring) env->GetObjectArrayElement(jfiles, i));
            files.append(String(env, f));
        }
        fileChooser->chooseFiles(files);
    }
}

void ChromeClientJava::loadIconForFiles(const Vector<String>& filenames, FileIconLoader* loader)
{
    loader->notifyFinished(Icon::createIconForFiles(filenames));
}

bool ChromeClientJava::canRunBeforeUnloadConfirmPanel()
{
    notImplemented();
    return false;
}

void ChromeClientJava::addMessageToConsole(MessageSource, MessageLevel, const String& message,
    unsigned lineNumber, unsigned columnNumber, const String& sourceID) 
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    env->CallVoidMethod(m_webPage, addMessageToConsoleMID,
            (jstring)message.toJavaString(env),
            (jint)lineNumber,
            (jstring)sourceID.toJavaString(env));
    CheckAndClearException(env);
}

bool ChromeClientJava::runBeforeUnloadConfirmPanel(const String&, Frame*)
{
    notImplemented();
    return false;
}

bool ChromeClientJava::shouldInterruptJavaScript()
{
    notImplemented();
    return true;
}

KeyboardUIMode ChromeClientJava::keyboardUIMode()
{
    return KeyboardAccessTabsToLinks;
}

IntRect ChromeClientJava::windowResizerRect() const
{
    notImplemented();
    return IntRect();
}

void ChromeClientJava::mouseDidMoveOverElement(const HitTestResult& htr, unsigned modifierFlags)
{
    static Node* mouseOverNode = 0;
    if (htr.isLiveLink()) {
        Node* overNode = htr.innerNode();
        URL url = htr.absoluteLinkURL();
        if (!url.isEmpty() && (overNode != mouseOverNode)) {
            setStatusbarText(url.deprecatedString());
            mouseOverNode = overNode;
        }
    } else {
        if (mouseOverNode) {
            setStatusbarText("");
            mouseOverNode = 0;
        }
    }
}

void ChromeClientJava::setToolTip(const String& tooltip, TextDirection td)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLString tooltipStr(NULL);
    if (tooltip.length() > 0) {
        tooltipStr = tooltip.toJavaString(env);
    }
    env->CallVoidMethod(m_webPage, setTooltipMID, (jstring)tooltipStr);
    CheckAndClearException(env);
}

void ChromeClientJava::print(Frame*)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    static jmethodID mid =  env->GetMethodID(
        getWebPageCls(),
        "fwkPrint",
        "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_webPage, mid);
    CheckAndClearException(env);
}

void ChromeClientJava::reachedMaxAppCacheSize(int64_t spaceNeeded)
{
    // FIXME: Free some space.
    notImplemented();
}

void ChromeClientJava::reachedApplicationCacheOriginQuota(SecurityOrigin*, int64_t)
{
    notImplemented();
}

#if USE(ACCELERATED_COMPOSITING)
void ChromeClientJava::attachRootGraphicsLayer(Frame*, GraphicsLayer* layer)
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
#endif // USE(ACCELERATED_COMPOSITING)


// HostWindow interface
void ChromeClientJava::scroll(const IntSize& scrollDelta, const IntRect& rectToScroll, const IntRect& clipRect)
{
    WebPage::webPageFromJObject(m_webPage)->scroll(scrollDelta, rectToScroll, clipRect);
}

IntPoint ChromeClientJava::screenToRootView(const IntPoint& p) const
{
    JNIEnv* env = WebCore_GetJavaEnv();
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
    JNIEnv* env = WebCore_GetJavaEnv();
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

PlatformPageClient ChromeClientJava::platformPageClient() const
{
    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLObject hostWindow(env->CallObjectMethod(m_webPage, getHostWindowMID));
    ASSERT(hostWindow);
    CheckAndClearException(env);

    return hostWindow;
}

void ChromeClientJava::contentsSizeChanged(Frame* frame, const IntSize& size) const
{
    notImplemented();
}

void ChromeClientJava::invalidateRootView(const IntRect& updateRect, bool immediate)
{
    // Nothing to do here as all necessary repaints are scheduled
    // by ChromeClientJava::scroll(). See also RT-29123.
}

void ChromeClientJava::invalidateContentsAndRootView(const IntRect& updateRect, bool immediate)
{
    repaint(updateRect);
}

void ChromeClientJava::invalidateContentsForSlowScroll(const IntRect& updateRect, bool immediate)
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


PassRefPtr<PopupMenu> ChromeClientJava::createPopupMenu(PopupMenuClient* client) const
{
    return adoptRef(new PopupMenuJava(client));
}

PassRefPtr<SearchPopupMenu> ChromeClientJava::createSearchPopupMenu(PopupMenuClient* client) const
{
    return adoptRef(new SearchPopupMenuJava(client));
}

// End of HostWindow methods

} // namespace WebCore
