/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "PlatformWheelEvent.h"

namespace WebCore {

PlatformWheelEvent::PlatformWheelEvent(
    const IntPoint& pos,
    const IntPoint& globalPos,
    float deltaX,
    float deltaY,
    bool shiftKey,
    bool ctrlKey,
    bool altKey,
    bool metaKey)
: PlatformEvent(
        PlatformEvent::Wheel,
        shiftKey,
        ctrlKey,
        altKey,
        metaKey,
        0.0)
, m_position(pos)
, m_globalPosition(globalPos)
      // For some unknown reason, EventHandler expects deltaX/deltaY < 0 for
      // ScrollRight/ScrollDown, and deltaX/deltaY > 0 for ScrollLeft/ScrollUp.
      // Java mouse wheel events behave in reverse way, so need a negation here.
, m_deltaX(-deltaX)
, m_deltaY(-deltaY)
, m_wheelTicksX(-deltaX)
, m_wheelTicksY(-deltaY)
, m_granularity(ScrollByPixelWheelEvent)
, m_directionInvertedFromDevice(false)
{
}

} // namespace WebCore
