/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Cursor.h"
#include "IntPoint.h"
#include "JavaEnv.h"
#include "Image.h"
#include "com_sun_webkit_CursorManager.h"

namespace WebCore {

jclass getJCursorManagerClass()
{
    static JGClass jCursorManagerClass(
        WebCore_GetJavaEnv()->FindClass("com/sun/webkit/CursorManager"));
    ASSERT(jCursorManagerClass);
    return jCursorManagerClass;
}

JLObject getJCursorManager()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(getJCursorManagerClass(), "getCursorManager",
                                                  "()Lcom/sun/webkit/CursorManager;");
    ASSERT(mid);

    JLObject jCursorManager(env->CallStaticObjectMethod(getJCursorManagerClass(), mid));
    CheckAndClearException(env);

    return jCursorManager;
}

Cursor::Cursor(Image* image, const IntPoint& hotspot)
    : m_platformCursor(0)
{
    JNIEnv* env = WebCore_GetJavaEnv();

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
        
    RefPtr<RQRef> cursorImageFrame(image->javaImage());
    if (!cursorImageFrame) {
        return;
    }

    m_platformCursor = env->CallLongMethod(jCursorManager, mid, (jobject)(*cursorImageFrame),
                                         hotspot.x(), hotspot.y());
    CheckAndClearException(env);
}

Cursor::Cursor(const Cursor& c)
{
    m_platformCursor = c.impl();
}

Cursor& Cursor::operator=(const Cursor& c)
{
    m_platformCursor = c.impl();
    return (*this);
}

Cursor::Cursor(PlatformCursor c)
{
    m_platformCursor = c;
}

Cursor::~Cursor()
{
    m_platformCursor = 0;
}

const Cursor getPredefinedCursor(jint type)
{
    JLObject jCursorManager(getJCursorManager());
    if (!jCursorManager) {
        return Cursor(0);
    }

    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJCursorManagerClass(),
                                            "getPredefinedCursorID", "(I)J");
    ASSERT(mid);

    jlong cursorID = env->CallLongMethod(jCursorManager, mid, type);
    CheckAndClearException(env);

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
