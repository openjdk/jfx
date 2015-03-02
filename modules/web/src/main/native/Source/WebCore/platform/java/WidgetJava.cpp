/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "wtf/Assertions.h"
#include "ContextMenu.h"
#include "ContextMenuController.h"
#include "Cursor.h"
#include "Editor.h"
#include "EventHandler.h"
#include "FocusController.h"
#include "Frame.h"
#include "FrameView.h"
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "HostWindow.h"
#include "IntRect.h"
#include "JavaEnv.h"
#include "KeyboardEvent.h"
#include "Logging.h"
#include "Page.h"
#include "PlatformKeyboardEvent.h"
#include "PlatformMouseEvent.h"
#include "PlatformWheelEvent.h"
#include "ScrollView.h"
#include "Widget.h"

#include <wtf/PassRefPtr.h>

static jmethodID wcWidgetSetBoundsMID;
static jmethodID wcWidgetRequestFocusMID;
static jmethodID wcWidgetSetCursorMID;
static jmethodID wcWidgetSetVisibleMID;
static jmethodID wcWidgetDestroyMID;


using namespace WebCore;

// some helper methods defined below

// MouseButton getWebKitMouseButton(jint javaButton);
// MouseEventType getWebKitMouseEventType(jint eventID);

namespace WebCore {

class WidgetPrivate {
public:
    WidgetPrivate():cRef(0){}
    IntRect bounds;
    long    cRef;
};

Widget::Widget(PlatformWidget widget)
    : m_data(new WidgetPrivate)
{
    init(widget);
}

Widget::~Widget()
{
    if (m_widget) {
        releasePlatformWidget();
    }
    delete m_data;
}

void Widget::retainPlatformWidget()
{
    if (m_widget) {
        //add counter
        ++m_data->cRef;
    }
}

void Widget::releasePlatformWidget()
{
    if( m_widget ){
        //drop counter
        --m_data->cRef;
        if( 0==m_data->cRef ) {
            JNIEnv* env = WebCore_GetJavaEnv();
            env->CallVoidMethod(m_widget, wcWidgetDestroyMID);
            CheckAndClearException(env);
            m_widget.clear();
        }
    }
}

IntRect Widget::frameRect() const
{
    return m_data->bounds;
}

void Widget::setFrameRect(const IntRect &r)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (r == m_data->bounds) {
        return;
    }
    m_data->bounds = r;
    if (!m_widget) {
        return;
    }

    env->CallVoidMethod(m_widget, wcWidgetSetBoundsMID, r.x(), r.y(), r.width(), r.height());
    CheckAndClearException(env);
}

void Widget::setFocus(bool focused)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    PlatformWidget j(platformWidget());
    if (!j) {
        j = root()->hostWindow()->platformPageClient();
    }
    if (!j) {
        return;
    }

    if (focused) {
        env->CallVoidMethod(j, wcWidgetRequestFocusMID);
    }
    CheckAndClearException(env);
}

void Widget::setCursor(const Cursor& cursor)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    PlatformWidget j(platformWidget());
    if (!j) {
        j = root()->hostWindow()->platformPageClient();
    }
    if (!j) {
        return;
    }

    env->CallVoidMethod(j, wcWidgetSetCursorMID, cursor.platformCursor());
    CheckAndClearException(env);
}

void Widget::show()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    // do we need to cache the 'visible' value?
    if (!m_widget) {
        return;
    }

    env->CallVoidMethod(m_widget, wcWidgetSetVisibleMID, JNI_TRUE);
    CheckAndClearException(env);
}

void Widget::hide()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    // do we need to cache the 'visible' value?
    if (!m_widget) {
        return;
    }

    env->CallVoidMethod(m_widget, wcWidgetSetVisibleMID, JNI_FALSE);
    CheckAndClearException(env);
}

void Widget::setIsSelected(bool)
{
    notImplemented();
}

void Widget::paint(GraphicsContext* gc, IntRect const& r)
{
/*
    JNIEnv* env = WebCore_GetJavaEnv();

    if (!gc ||
        !gc->platformContext() ||
        gc->paintingDisabled() ||
        r.isEmpty())
    {
        return;
    }

    Widget* widget = this;
    int px = r.x();
    int py = r.y();
    while (widget->parent() && !widget->isFrameView()) {
        px += widget->x();
        py += widget->y();
        widget = widget->parent();
    }

    if (!widget || !widget->isFrameView()) {
        return;
    }

    FrameView* frameView = dynamic_cast<FrameView*>(widget);
    if (!frameView->frame() || !frameView->frame()->contentRenderer()) {
        return;
    }
    if (frameView->needsLayout()) {
        frameView->layout();
    }

    if (frameView->isPainting()) {
        return;
    }

    frameView->frame()->paint(gc, IntRect(px, py, r.width(), r.height()));
    frameView->paintContents(gc, toPaint);
*/
}

} // namespace WebCore

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_WCWidget_initIDs
    (JNIEnv* env, jclass wcWidgetClass)
{
    wcWidgetSetBoundsMID = env->GetMethodID(wcWidgetClass, "fwkSetBounds",
                                             "(IIII)V");
    ASSERT(wcWidgetSetBoundsMID);
    wcWidgetRequestFocusMID = env->GetMethodID(wcWidgetClass,
                                               "fwkRequestFocus", "()V");
    ASSERT(wcWidgetRequestFocusMID);
    wcWidgetSetCursorMID = env->GetMethodID(wcWidgetClass, "fwkSetCursor",
                                            "(J)V");
    ASSERT(wcWidgetSetCursorMID);
    wcWidgetSetVisibleMID = env->GetMethodID(wcWidgetClass, "fwkSetVisible",
                                             "(Z)V");
    ASSERT(wcWidgetSetVisibleMID);
    wcWidgetDestroyMID = env->GetMethodID(wcWidgetClass, "fwkDestroy",
                                             "()V");
    ASSERT(wcWidgetDestroyMID);

}
#ifdef __cplusplus
}
#endif
