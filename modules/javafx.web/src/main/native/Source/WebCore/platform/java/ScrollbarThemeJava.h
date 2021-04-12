/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include "PlatformJavaClasses.h"
#include "ScrollbarThemeComposite.h"

namespace WebCore {

class ScrollbarThemeJava : public ScrollbarThemeComposite {
public:
    bool paint(Scrollbar&, GraphicsContext&, const IntRect& /*damageRect*/) override;
    void invalidatePart(Scrollbar&, ScrollbarPart) override;

    bool hasButtons(Scrollbar&) override { return true; }
    bool hasThumb(Scrollbar&) override;

    int scrollbarThickness(ScrollbarControlSize = ScrollbarControlSize::Regular, ScrollbarExpansionState = ScrollbarExpansionState::Expanded) override;

    IntRect backButtonRect(Scrollbar&, ScrollbarPart, bool painting = false) override;
    IntRect forwardButtonRect(Scrollbar&, ScrollbarPart, bool painting = false) override;
    IntRect trackRect(Scrollbar&, bool painting = false) override;
    bool usesOverlayScrollbars() const final { return true; }
    // When using overlay scrollbars, always invalidate the whole scrollbar when entering/leaving.
    bool invalidateOnMouseEnterExit() override { return usesOverlayScrollbars(); }
};

} // namespace WebCore
