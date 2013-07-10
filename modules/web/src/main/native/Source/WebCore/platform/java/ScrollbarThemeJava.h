/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef ScrollbarThemeJava_h
#define ScrollbarThemeJava_h

#include "JavaEnv.h"
#include "ScrollbarTheme.h"

namespace WebCore {

class ScrollbarThemeJava : public ScrollbarTheme {
public:
    virtual bool paint(ScrollbarThemeClient*, GraphicsContext*, const IntRect& /*damageRect*/);

    virtual ScrollbarPart hitTest(ScrollbarThemeClient*, const IntPoint&);

    virtual void invalidatePart(ScrollbarThemeClient*, ScrollbarPart);

    virtual int thumbPosition(ScrollbarThemeClient*);
    virtual int thumbLength(ScrollbarThemeClient*);
    virtual int trackPosition(ScrollbarThemeClient*);
    virtual int trackLength(ScrollbarThemeClient*);

    virtual int scrollbarThickness(ScrollbarControlSize = RegularScrollbar);
};

}
#endif
