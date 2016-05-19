/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef ScrollbarThemeJava_h
#define ScrollbarThemeJava_h

#include "JavaEnv.h"
#include "ScrollbarTheme.h"

namespace WebCore {

class ScrollbarThemeJava : public ScrollbarTheme {
public:
    virtual bool paint(Scrollbar&, GraphicsContext&, const IntRect& /*damageRect*/);

    virtual ScrollbarPart hitTest(Scrollbar&, const IntPoint&);

    virtual void invalidatePart(Scrollbar&, ScrollbarPart);

    virtual int thumbPosition(Scrollbar&);
    virtual int thumbLength(Scrollbar&);
    virtual int trackPosition(Scrollbar&);
    virtual int trackLength(Scrollbar&);

    virtual int scrollbarThickness(ScrollbarControlSize = RegularScrollbar);
};

}
#endif
