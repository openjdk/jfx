/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "Cursor.h"
#include "IntPoint.h"
#include "PlatformJavaClasses.h"
#include "Image.h"
#include "com_sun_webkit_CursorManager.h"

namespace WebCore {

jclass getJCursorManagerClass()
{
    static JGClass jCursorManagerClass(
        WTF::GetJavaEnv()->FindClass("com/sun/webkit/CursorManager"));
    ASSERT(jCursorManagerClass);
    return jCursorManagerClass;
}

JLObject getJCursorManager()
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(getJCursorManagerClass(), "getCursorManager",
                                                  "()Lcom/sun/webkit/CursorManager;");
    ASSERT(mid);

    JLObject jCursorManager(env->CallStaticObjectMethod(getJCursorManagerClass(), mid));
    WTF::CheckAndClearException(env);

    return jCursorManager;
}

Cursor::Cursor(Image* image, const IntPoint& hotspot)
    : m_platformCursor(0)
{
    JNIEnv* env = WTF::GetJavaEnv();

    if (!image) {
        return;
    }

    JLObject jCursorManager(getJCursorManager());
    if (!jCursorManager) {
        return;
    }

    static jmethodID mid = env->GetMethodID(getJCursorManagerClass(), "getCustomCursorID",
                                            "(Lcom/sun/webkit/graphics/WCImageFrame;II)J");
    ASSERT(mid);

    RefPtr<NativeImage> cursorImageFrame = image->javaImage();
    if (!cursorImageFrame) {
        return;
    }

    m_platformCursor = env->CallLongMethod(jCursorManager, mid, (jobject)(*cursorImageFrame->platformImage()->getImage()),
                                         hotspot.x(), hotspot.y());
    WTF::CheckAndClearException(env);
}

Cursor::Cursor(PlatformCursor c)
{
    m_platformCursor = c;
}

void Cursor::setPlatformCursor(const Cursor& c) const
{
    m_platformCursor = c.m_platformCursor;
}

void Cursor::ensurePlatformCursor() const
{
    if (m_platformCursor)
        return;

    switch (m_type) {
    case PlatformCursorType::Pointer:
    case PlatformCursorType::Cell:
    case PlatformCursorType::ContextMenu:
    case PlatformCursorType::Alias:
    case PlatformCursorType::Copy:
    case PlatformCursorType::None:
    case PlatformCursorType::Grab:
    case PlatformCursorType::Grabbing:
        setPlatformCursor(pointerCursor());
        break;
    case PlatformCursorType::Cross:
        setPlatformCursor(crossCursor());
        break;
    case PlatformCursorType::Hand:
        setPlatformCursor(handCursor());
        break;
    case PlatformCursorType::IBeam:
        setPlatformCursor(iBeamCursor());
        break;
    case PlatformCursorType::Wait:
        setPlatformCursor(waitCursor());
        break;
    case PlatformCursorType::Help:
        setPlatformCursor(helpCursor());
        break;
    case PlatformCursorType::Move:
        setPlatformCursor(moveCursor());
        break;
    case PlatformCursorType::MiddlePanning:
        setPlatformCursor(middlePanningCursor());
        break;
    case PlatformCursorType::EastResize:
        setPlatformCursor(eastResizeCursor());
        break;
    case PlatformCursorType::EastPanning:
        setPlatformCursor(eastPanningCursor());
        break;
    case PlatformCursorType::NorthResize:
        setPlatformCursor(northResizeCursor());
        break;
    case PlatformCursorType::NorthPanning:
        setPlatformCursor(northPanningCursor());
        break;
    case PlatformCursorType::NorthEastResize:
        setPlatformCursor(northEastResizeCursor());
        break;
    case PlatformCursorType::NorthEastPanning:
        setPlatformCursor(northEastPanningCursor());
        break;
    case PlatformCursorType::NorthWestResize:
        setPlatformCursor(northWestResizeCursor());
        break;
    case PlatformCursorType::NorthWestPanning:
        setPlatformCursor(northWestPanningCursor());
        break;
    case PlatformCursorType::SouthResize:
        setPlatformCursor(southResizeCursor());
        break;
    case PlatformCursorType::SouthPanning:
      setPlatformCursor(southPanningCursor());
        break;
    case PlatformCursorType::SouthEastResize:
      setPlatformCursor(southEastResizeCursor());
        break;
    case PlatformCursorType::SouthEastPanning:
      setPlatformCursor(southEastPanningCursor());
        break;
    case PlatformCursorType::SouthWestResize:
      setPlatformCursor(southWestResizeCursor());
        break;
    case PlatformCursorType::SouthWestPanning:
      setPlatformCursor(southWestPanningCursor());
        break;
    case PlatformCursorType::WestResize:
      setPlatformCursor(westResizeCursor());
        break;
    case PlatformCursorType::NorthSouthResize:
      setPlatformCursor(northSouthResizeCursor());
        break;
    case PlatformCursorType::EastWestResize:
      setPlatformCursor(eastWestResizeCursor());
        break;
    case PlatformCursorType::WestPanning:
      setPlatformCursor(westPanningCursor());
        break;
    case PlatformCursorType::NorthEastSouthWestResize:
      setPlatformCursor(northEastSouthWestResizeCursor());
        break;
    case PlatformCursorType::NorthWestSouthEastResize:
      setPlatformCursor(northWestSouthEastResizeCursor());
        break;
    case PlatformCursorType::ColumnResize:
      setPlatformCursor(columnResizeCursor());
        break;
    case PlatformCursorType::RowResize:
      setPlatformCursor(rowResizeCursor());
        break;
    case PlatformCursorType::VerticalText:
      setPlatformCursor(verticalTextCursor());
        break;
    case PlatformCursorType::Progress:
      setPlatformCursor(progressCursor());
        break;
    case PlatformCursorType::NoDrop:
      setPlatformCursor(noDropCursor());
        break;
    case PlatformCursorType::NotAllowed:
      setPlatformCursor(notAllowedCursor());
        break;
    case PlatformCursorType::ZoomIn:
      setPlatformCursor(zoomInCursor());
        break;
    case PlatformCursorType::ZoomOut:
      setPlatformCursor(zoomOutCursor());
        break;
    case PlatformCursorType::Custom:
      setPlatformCursor(Cursor(m_image.get(), m_hotSpot));
        break;
    default:
      setPlatformCursor(pointerCursor());
        break;
    }
}

const Cursor getPredefinedCursor(jint type)
{
    JLObject jCursorManager(getJCursorManager());
    if (!jCursorManager) {
        return Cursor(0);
    }

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJCursorManagerClass(),
                                            "getPredefinedCursorID", "(I)J");
    ASSERT(mid);

    jlong cursorID = env->CallLongMethod(jCursorManager, mid, type);
    WTF::CheckAndClearException(env);

    return Cursor(cursorID);
}

const Cursor& pointerCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_POINTER);
    return c;
}

const Cursor& crossCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_CROSS);
    return c;
}

const Cursor& handCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_HAND);
    return c;
}

const Cursor& moveCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_MOVE);
    return c;
}

const Cursor& iBeamCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_TEXT);
    return c;
}

const Cursor& waitCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_WAIT);
    return c;
}

const Cursor& helpCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_HELP);
    return c;
}

const Cursor& eastResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_EAST_RESIZE);
    return c;
}

const Cursor& northResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_RESIZE);
    return c;
}

const Cursor& northEastResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_EAST_RESIZE);
    return c;
}

const Cursor& northWestResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_WEST_RESIZE);
    return c;
}

const Cursor& southResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_SOUTH_RESIZE);
    return c;
}

const Cursor& southEastResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_SOUTH_EAST_RESIZE);
    return c;
}

const Cursor& southWestResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_SOUTH_WEST_RESIZE);
    return c;
}

const Cursor& westResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_WEST_RESIZE);
    return c;
}

const Cursor& northSouthResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_SOUTH_RESIZE);
    return c;
}

const Cursor& eastWestResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_EAST_WEST_RESIZE);
    return c;
}

const Cursor& northEastSouthWestResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_EAST_SOUTH_WEST_RESIZE);
    return c;
}

const Cursor& northWestSouthEastResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_WEST_SOUTH_EAST_RESIZE);
    return c;
}

const Cursor& columnResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_COLUMN_RESIZE);
    return c;
}

const Cursor& rowResizeCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_ROW_RESIZE);
    return c;
}

const Cursor& verticalTextCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_VERTICAL_TEXT);
    return c;
}

const Cursor& cellCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_CELL);
    return c;
}

const Cursor& contextMenuCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_CONTEXT_MENU);
    return c;
}

const Cursor& noDropCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NO_DROP);
    return c;
}

const Cursor& notAllowedCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NOT_ALLOWED);
    return c;
}

const Cursor& progressCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_PROGRESS);
    return c;
}

const Cursor& aliasCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_ALIAS);
    return c;
}

const Cursor& zoomInCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_ZOOM_IN);
    return c;
}

const Cursor& zoomOutCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_ZOOM_OUT);
    return c;
}

const Cursor& copyCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_COPY);
    return c;
}

const Cursor& noneCursor()
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NONE);
    return c;
}

const Cursor& middlePanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_MIDDLE_PANNING);
    return c;
}

const Cursor& westPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_WEST_PANNING);
    return c;
}

const Cursor& eastPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_EAST_PANNING);
    return c;
}

const Cursor& southPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_SOUTH_PANNING);
    return c;
}

const Cursor& southWestPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_SOUTH_WEST_PANNING);
    return c;
}

const Cursor& southEastPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_SOUTH_EAST_PANNING);
    return c;
}

const Cursor& northPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_PANNING);
    return c;
}

const Cursor& northWestPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_WEST_PANNING);
    return c;
}

const Cursor& northEastPanningCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_NORTH_EAST_PANNING);
    return c;
}

const Cursor& grabCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_GRAB);
    return c;
}

const Cursor& grabbingCursor(void)
{
    static Cursor c = getPredefinedCursor(com_sun_webkit_CursorManager_GRABBING);
    return c;
}
} // namespace WebCore
