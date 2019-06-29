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

bool EventHandler::eventActivatedView(const PlatformMouseEvent &) const
{
    // Implementation below is probably incorrect/incomplete,
    // so leaving 'notImplemented()' here.
    notImplemented();

    // Return false here as activation is handled separately from
    // mouse events
    return false;
}

bool EventHandler::passMouseMoveEventToSubframe(MouseEventWithHitTestResults& event, Frame* subFrame, HitTestResult* hoveredNode)
{
    if (m_mouseDownMayStartDrag && !m_mouseDownWasInSubframe)
        return false;
    subFrame->eventHandler().handleMouseMoveEvent(event.event(), hoveredNode);
    return true;
}

} // namespace WebCore
