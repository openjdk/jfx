/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "ClipboardJava.h"
#include "EventHandler.h"
#include "FocusController.h"
#include "Frame.h"
#include "FrameView.h"
#include "MouseEventWithHitTestResults.h"
#include "Page.h"
#include "PlatformKeyboardEvent.h"
#include "Widget.h"

namespace WebCore {

double const EventHandler::TextDragDelay = 0;

unsigned EventHandler::accessKeyModifiers()
{
    return PlatformKeyboardEvent::AltKey;
}

PassRefPtr<Clipboard> EventHandler::createDraggingClipboard() const
{
    return ClipboardJava::create(ClipboardWritable, Clipboard::DragAndDrop, DataObjectJava::create(), &m_frame);
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

bool EventHandler::passWidgetMouseDownEventToWidget(const MouseEventWithHitTestResults& event)
{
    notImplemented();
    return false;
}

bool EventHandler::passWheelEventToWidget(const PlatformWheelEvent& ev, Widget* widget)
{
    if (!widget->isFrameView()) {
        return false;
    }

    FrameView* frameView = static_cast<FrameView*>(widget);
    return frameView->frame().eventHandler().handleWheelEvent(ev);
}

bool EventHandler::tabsToAllFormControls(KeyboardEvent *) const
{
    return true;
}

} // namespace WebCore
