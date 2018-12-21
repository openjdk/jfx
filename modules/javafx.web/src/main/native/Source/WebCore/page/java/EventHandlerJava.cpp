/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "EventHandler.h"
#include "FocusController.h"
#include "Frame.h"
#include "FrameView.h"
#include "MouseEventWithHitTestResults.h"
#include "Page.h"
#include "PlatformKeyboardEvent.h"
#include "Widget.h"
#include "DataTransfer.h"

namespace WebCore {

#if ENABLE(DRAG_SUPPORT)
const Seconds EventHandler::TextDragDelay { 0_s };
#endif

OptionSet<PlatformEvent::Modifier> EventHandler::accessKeyModifiers()
{
    return PlatformEvent::Modifier::AltKey;
}

void EventHandler::focusDocumentView()
{
    Page* page = m_frame.page();
    if (page) {
        page->focusController().setFocusedFrame(&m_frame);
    }
}

bool EventHandler::eventActivatedView(const PlatformMouseEvent &) const
{
    // Implementation below is probably incorrect/incomplete,
    // so leaving 'notImplemented()' here.
    notImplemented();

    // Return false here as activation is handled separately from
    // mouse events
    return false;
}

bool EventHandler::passMousePressEventToSubframe(MouseEventWithHitTestResults& event, Frame* subFrame)
{
    subFrame->eventHandler().handleMousePressEvent(event.event());
    return true;
}

bool EventHandler::widgetDidHandleWheelEvent(const PlatformWheelEvent& wheelEvent, Widget& widget)
{
    if (!is<FrameView>(widget))
        return false;

    return downcast<FrameView>(widget).frame().eventHandler().handleWheelEvent(wheelEvent);
}

bool EventHandler::passMouseMoveEventToSubframe(MouseEventWithHitTestResults& event, Frame* subFrame, HitTestResult* hoveredNode)
{
    if (m_mouseDownMayStartDrag && !m_mouseDownWasInSubframe)
        return false;
    subFrame->eventHandler().handleMouseMoveEvent(event.event(), hoveredNode);
    return true;
}

bool EventHandler::passMouseReleaseEventToSubframe(MouseEventWithHitTestResults& event, Frame* subFrame)
{
    subFrame->eventHandler().handleMouseReleaseEvent(event.event());
    return true;
}

bool EventHandler::passWidgetMouseDownEventToWidget(const MouseEventWithHitTestResults&)
{
    notImplemented();
    return false;
}

bool EventHandler::tabsToAllFormControls(KeyboardEvent*) const
{
    return true;
}

} // namespace WebCore
