/*
 * Copyright (C) 2024 Samuel Weinig <sam@webkit.org>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "CSSColor.h"

namespace WebCore {

class Color;
struct PlatformColorResolutionState;

namespace CSS {

struct ContrastColor {
    WTF_MAKE_STRUCT_FAST_ALLOCATED;

    Color color;

    bool operator==(const ContrastColor&) const;
};

WebCore::Color createColor(const ContrastColor&, PlatformColorResolutionState&);

bool containsCurrentColor(const ContrastColor&);
bool containsColorSchemeDependentColor(const ContrastColor&);

template<> struct Serialize<ContrastColor> { void operator()(StringBuilder&, const SerializationContext&, const ContrastColor&); };
template<> struct ComputedStyleDependenciesCollector<ContrastColor> { void operator()(ComputedStyleDependencies&, const ContrastColor&); };
template<> struct CSSValueChildrenVisitor<ContrastColor> { IterationStatus operator()(NOESCAPE const Function<IterationStatus(CSSValue&)>&, const ContrastColor&); };

} // namespace CSS
} // namespace WebCore
