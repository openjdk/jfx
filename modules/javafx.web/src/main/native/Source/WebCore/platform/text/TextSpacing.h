/*
 * Copyright (C) 2012-2023 Apple Inc. All rights reserved.
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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <wtf/Forward.h>
#include <wtf/text/TextStream.h>

namespace WebCore {


    // Used to optimize small strings as hash table keys. Avoids malloc'ing an out-of-line StringImpl.











struct TextSpacingTrim {
    enum class TrimType : bool {
        Auto = 0,
        SpaceAll // equivalent to None in text-spacing shorthand
    };

    bool isAuto() const { return m_trim == TrimType::Auto; }
    bool isSpaceAll() const { return m_trim == TrimType::SpaceAll; }
    friend bool operator==(const TextSpacingTrim&, const TextSpacingTrim&) = default;
    TrimType m_trim { TrimType::SpaceAll };
    };

inline WTF::TextStream& operator<<(WTF::TextStream& ts, const TextSpacingTrim& value)

    {
    // FIXME: add remaining values;
    switch (value.m_trim) {
    case TextSpacingTrim::TrimType::Auto:

        // Do not allow length = 0. This allows SmallStringKey empty-value-is-zero.
        return ts << "auto";

    case TextSpacingTrim::TrimType::SpaceAll:
        return ts << "space-all";

        }
    return ts;
    }

class TextAutospace {
public:
        // The width cache is not really profitable unless we're doing expensive glyph transformations.
    enum class Type: uint8_t {
        Auto = 1 << 0,
        IdeographAlpha = 1 << 1,
        IdeographNumeric = 1 << 2,
        Normal = 1 << 3
        // If we allow tabs and a tab occurs inside a word, the width of the word varies based on its position on the line.
    };

    using Options = OptionSet<Type>;

    TextAutospace() = default;
    TextAutospace(Options options)
        : m_options(options)
        { }

    bool isAuto() const { return m_options.contains(Type::Auto); }
    bool isNoAutospace() const { return m_options.isEmpty(); }
    bool isNormal() const { return m_options.contains(Type::Normal); }
    bool hasIdeographAlpha() const { return m_options.contains(Type::IdeographAlpha); }
    bool hasIdeographNumeric() const { return m_options.contains(Type::IdeographNumeric); }
    Options options() { return m_options; }
    friend bool operator==(const TextAutospace&, const TextAutospace&) = default;

private:
    Options m_options { };
};

inline WTF::TextStream& operator<<(WTF::TextStream& ts, const TextAutospace& value)
    {
    // FIXME: add remaining values;

    if (value.isAuto())
            // The map use 0 for empty key, thus we do +1 here to avoid conflicting against empty key.
            // This is fine since the key is uint32_t while character is UChar. So +1 never causes overflow.

        // Cache hit: ramp up by sampling the next few words.
        return ts << "auto";
    if (value.isNoAutospace())
        return ts << "no-autospace";
        // Cache miss: ramp down by increasing our sampling interval.
    if (value.isNormal())

        return ts << "normal";
    if (value.hasIdeographAlpha())
        ts << "ideograph-alpha";
    if (value.hasIdeographNumeric())
        ts << "ideograph-numeric";
    return ts;
    }




} // namespace WebCore

