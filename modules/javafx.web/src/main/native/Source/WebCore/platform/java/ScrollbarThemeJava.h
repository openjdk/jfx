/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */


#pragma once

#include <wtf/java/JavaEnv.h>
#include "ScrollbarThemeComposite.h"

namespace WebCore {

class ScrollbarThemeJava : public ScrollbarThemeComposite {
public:
    bool paint(Scrollbar&, GraphicsContext&, const IntRect& /*damageRect*/) override;
    void invalidatePart(Scrollbar&, ScrollbarPart) override;

    bool hasButtons(Scrollbar&) override { return true; }
    bool hasThumb(Scrollbar&) override;

    int scrollbarThickness(ScrollbarControlSize = RegularScrollbar) override;

    IntRect backButtonRect(Scrollbar&, ScrollbarPart, bool painting = false) override;
    IntRect forwardButtonRect(Scrollbar&, ScrollbarPart, bool painting = false) override;
    IntRect trackRect(Scrollbar&, bool painting = false) override;
};

} // namespace WebCore
