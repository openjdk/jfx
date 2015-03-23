/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
    m_platformCursor = c.platformCursor();
}

Cursor& Cursor::operator=(const Cursor& c)
{
    m_type = c.m_type;
    m_image = c.m_image;
    m_hotSpot = c.m_hotSpot;
    m_platformCursor = c.platformCursor();
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

void Cursor::setPlatformCursor(const Cursor& c) const
{
    m_platformCursor = c.m_platformCursor;
}

void Cursor::ensurePlatformCursor() const
{
    if (m_platformCursor)
        return;

    switch (m_type) {
    case Cursor::Pointer:
    case Cursor::Cell:
    case Cursor::ContextMenu:
    case Cursor::Alias:
    case Cursor::Copy:
    case Cursor::None:
    case Cursor::Grab:
    case Cursor::Grabbing:
        setPlatformCursor(pointerCursor());
        break;
    case Cursor::Cross:
        setPlatformCursor(crossCursor());
        break;
    case Cursor::Hand:
        setPlatformCursor(handCursor());
        break;
    case Cursor::IBeam:
        setPlatformCursor(iBeamCursor());
        break;
    case Cursor::Wait:
        setPlatformCursor(waitCursor());
        break;
    case Cursor::Help:
        setPlatformCursor(helpCursor());
        break;
    case Cursor::Move:
        setPlatformCursor(moveCursor());
        break;
    case Cursor::MiddlePanning:
        setPlatformCursor(middlePanningCursor());
        break;
    case Cursor::EastResize:
        setPlatformCursor(eastResizeCursor());
        break;
    case Cursor::EastPanning:
        setPlatformCursor(eastPanningCursor());
        break;
    case Cursor::NorthResize:
        setPlatformCursor(northResizeCursor());
        break;
    case Cursor::NorthPanning:
        setPlatformCursor(northPanningCursor());
        break;
    case Cursor::NorthEastResize:
        setPlatformCursor(northEastResizeCursor());
        break;
    case Cursor::NorthEastPanning:
        setPlatformCursor(northEastPanningCursor());
        break;
    case Cursor::NorthWestResize:
        setPlatformCursor(northWestResizeCursor());
        break;
    case Cursor::NorthWestPanning:
        setPlatformCursor(northWestPanningCursor());
        break;
    case Cursor::SouthResize:
        setPlatformCursor(southResizeCursor());
        break;
    case Cursor::SouthPanning:
      setPlatformCursor(southPanningCursor());
        break;
    case Cursor::SouthEastResize:
      setPlatformCursor(southEastResizeCursor());
        break;
    case Cursor::SouthEastPanning:
      setPlatformCursor(southEastPanningCursor());
        break;
    case Cursor::SouthWestResize:
      setPlatformCursor(southWestResizeCursor());
        break;
    case Cursor::SouthWestPanning:
      setPlatformCursor(southWestPanningCursor());
        break;
    case Cursor::WestResize:
      setPlatformCursor(westResizeCursor());
        break;
    case Cursor::NorthSouthResize:
      setPlatformCursor(northSouthResizeCursor());
        break;
    case Cursor::EastWestResize:
      setPlatformCursor(eastWestResizeCursor());
        break;
    case Cursor::WestPanning:
      setPlatformCursor(westPanningCursor());
        break;
    case Cursor::NorthEastSouthWestResize:
      setPlatformCursor(northEastSouthWestResizeCursor());
        break;
    case Cursor::NorthWestSouthEastResize:
      setPlatformCursor(northWestSouthEastResizeCursor());
        break;
    case Cursor::ColumnResize:
      setPlatformCursor(columnResizeCursor());
        break;
    case Cursor::RowResize:
      setPlatformCursor(rowResizeCursor());
        break;
    case Cursor::VerticalText:
      setPlatformCursor(verticalTextCursor());
        break;
    case Cursor::Progress:
      setPlatformCursor(progressCursor());
        break;
    case Cursor::NoDrop:
      setPlatformCursor(noDropCursor());
        break;
    case Cursor::NotAllowed:
      setPlatformCursor(notAllowedCursor());
        break;
    case Cursor::ZoomIn:
      setPlatformCursor(zoomInCursor());
        break;
    case Cursor::ZoomOut:
      setPlatformCursor(zoomOutCursor());
        break;
    case Cursor::Custom:
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
