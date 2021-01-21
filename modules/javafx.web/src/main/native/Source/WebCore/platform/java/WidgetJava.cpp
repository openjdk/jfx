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
#include "PlatformJavaClasses.h"
#include "KeyboardEvent.h"
#include "Logging.h"
#include "Page.h"
#include "PlatformKeyboardEvent.h"
#include "PlatformMouseEvent.h"
#include "PlatformWheelEvent.h"
#include "ScrollView.h"
#include "Widget.h"


// some helper methods defined below

// MouseButton getWebKitMouseButton(jint javaButton);
// MouseEventType getWebKitMouseEventType(jint eventID);

namespace WebCore {

static jmethodID wcWidgetSetBoundsMID;
static jmethodID wcWidgetRequestFocusMID;
static jmethodID wcWidgetSetCursorMID;
static jmethodID wcWidgetSetVisibleMID;
static jmethodID wcWidgetDestroyMID;


class WidgetPrivate {
public:
    IntRect bounds;
};

Widget::Widget(PlatformWidget widget)
    : m_data(new WidgetPrivate)
{
    init(widget);
}

Widget::~Widget()
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (m_widget && env) {
        env->CallVoidMethod(m_widget, wcWidgetDestroyMID);
        WTF::CheckAndClearException(env);
        m_widget.clear();
    }
    delete m_data;
}

IntRect Widget::frameRect() const
{
    return m_data->bounds;
}

void Widget::setFrameRect(const IntRect &r)
{
    JNIEnv* env = WTF::GetJavaEnv();

    if (r == m_data->bounds) {
        return;
    }
    m_data->bounds = r;
    if (!m_widget) {
        return;
    }

    env->CallVoidMethod(m_widget, wcWidgetSetBoundsMID, r.x(), r.y(), r.width(), r.height());
    WTF::CheckAndClearException(env);
}

void Widget::setFocus(bool focused)
{
    JNIEnv* env = WTF::GetJavaEnv();

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
    WTF::CheckAndClearException(env);
}

void Widget::setCursor(const Cursor& cursor)
{
    JNIEnv* env = WTF::GetJavaEnv();

    PlatformWidget j(platformWidget());
    if (!j) {
        j = root()->hostWindow()->platformPageClient();
    }
    if (!j) {
        return;
    }

    env->CallVoidMethod(j, wcWidgetSetCursorMID, cursor.platformCursor());
    WTF::CheckAndClearException(env);
}

void Widget::show()
{
    JNIEnv* env = WTF::GetJavaEnv();

    // do we need to cache the 'visible' value?
    if (!m_widget) {
        return;
    }

    env->CallVoidMethod(m_widget, wcWidgetSetVisibleMID, JNI_TRUE);
    WTF::CheckAndClearException(env);
}

void Widget::hide()
{
    JNIEnv* env = WTF::GetJavaEnv();

    // do we need to cache the 'visible' value?
    if (!m_widget) {
        return;
    }

    env->CallVoidMethod(m_widget, wcWidgetSetVisibleMID, JNI_FALSE);
    WTF::CheckAndClearException(env);
}

void Widget::setIsSelected(bool)
{
    notImplemented();
}

IntRect Widget::convertFromRootToContainingWindow(const Widget*, const IntRect& rect)
{
    return rect;
}

IntRect Widget::convertFromContainingWindowToRoot(const Widget*, const IntRect& rect)
{
    return rect;
}

IntPoint Widget::convertFromRootToContainingWindow(const Widget*, const IntPoint& point)
{
    return point;
}

IntPoint Widget::convertFromContainingWindowToRoot(const Widget*, const IntPoint& point)
{
    return point;
}

void Widget::paint(GraphicsContext&, const IntRect&, SecurityOriginPaintPolicy, EventRegionContext*)
{
/*
    JNIEnv* env = WTF::GetJavaEnv();

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

extern "C" {

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
}
} // namespace WebCore

