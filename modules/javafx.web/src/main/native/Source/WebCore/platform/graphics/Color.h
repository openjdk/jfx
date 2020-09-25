/*
 * Copyright (C) 2003-2020 Apple Inc. All rights reserved.
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

#include "ColorSpace.h"
#include "ExtendedColor.h"
#include <algorithm>
#include <cmath>
#include <unicode/uchar.h>
#include <wtf/Forward.h>
#include <wtf/HashFunctions.h>
#include <wtf/Optional.h>
#include <wtf/text/LChar.h>

#if USE(CG)
typedef struct CGColor* CGColorRef;
#endif

#if PLATFORM(WIN)
struct _D3DCOLORVALUE;
typedef _D3DCOLORVALUE D3DCOLORVALUE;
typedef D3DCOLORVALUE D2D_COLOR_F;
typedef D2D_COLOR_F D2D1_COLOR_F;
struct D2D_VECTOR_4F;
typedef D2D_VECTOR_4F D2D1_VECTOR_4F;
#endif

#if PLATFORM(GTK)
typedef struct _GdkRGBA GdkRGBA;
#endif

namespace WebCore {

// Color value with 8-bit components for red, green, blue, and alpha.
// For historical reasons, stored as a 32-bit integer, with alpha in the high bits: ARGB.
class SimpleColor {
public:
    constexpr SimpleColor(uint32_t value = 0) : m_value { value } { }

    constexpr uint32_t value() const { return m_value; }

    constexpr uint8_t redComponent() const { return m_value >> 16; }
    constexpr uint8_t greenComponent() const { return m_value >> 8; }
    constexpr uint8_t blueComponent() const { return m_value; }
    constexpr uint8_t alphaComponent() const { return m_value >> 24; }

    constexpr bool isOpaque() const { return alphaComponent() == 0xFF; }
    constexpr bool isVisible() const { return alphaComponent(); }

    String serializationForHTML() const;
    String serializationForCSS() const;
    String serializationForRenderTreeAsText() const;

private:
    uint32_t m_value { 0 };
};

bool operator==(SimpleColor, SimpleColor);
bool operator!=(SimpleColor, SimpleColor);

// FIXME: Remove this after migrating to the new name.
using RGBA32 = SimpleColor;

WEBCORE_EXPORT RGBA32 makeRGB(int r, int g, int b);
WEBCORE_EXPORT RGBA32 makeRGBA(int r, int g, int b, int a);

RGBA32 makePremultipliedRGBA(int r, int g, int b, int a, bool ceiling = true);
RGBA32 makeUnPremultipliedRGBA(int r, int g, int b, int a);

WEBCORE_EXPORT RGBA32 colorWithOverrideAlpha(RGBA32 color, float overrideAlpha);
RGBA32 colorWithOverrideAlpha(RGBA32 color, Optional<float> overrideAlpha);

WEBCORE_EXPORT RGBA32 makeRGBA32FromFloats(float r, float g, float b, float a);
RGBA32 makeRGBAFromHSLA(float h, float s, float l, float a);
RGBA32 makeRGBAFromCMYKA(float c, float m, float y, float k, float a);

uint8_t roundAndClampColorChannel(int);
uint8_t roundAndClampColorChannel(float);

class Color {
    WTF_MAKE_FAST_ALLOCATED;
public:
    Color() { }

    // FIXME: Remove all these constructors and creation functions and replace the ones that are still needed with free functions.

    Color(RGBA32 color, bool valid = true)
    {
        if (valid)
            setRGB(color);
    }

    enum SemanticTag { Semantic };
    Color(RGBA32 color, SemanticTag)
    {
        setRGB(color);
        setIsSemantic();
    }

    Color(int r, int g, int b)
    {
        setRGB(r, g, b);
    }

    Color(int r, int g, int b, int a)
    {
        setRGB(makeRGBA(r, g, b, a));
    }

    Color(float r, float g, float b, float a)
    {
        setRGB(makeRGBA32FromFloats(r, g, b, a));
    }

    // Creates a new color from the specific CMYK and alpha values.
    Color(float c, float m, float y, float k, float a)
    {
        setRGB(makeRGBAFromCMYKA(c, m, y, k, a));
    }

    WEBCORE_EXPORT explicit Color(const String&);
    explicit Color(const char*);

    explicit Color(WTF::HashTableDeletedValueType)
    {
        static_assert(deletedHashValue & invalidRGBAColor, "Color's deleted hash value must not look like an ExtendedColor");
        static_assert(!(deletedHashValue & validRGBAColorBit), "Color's deleted hash value must not look like a valid RGBA32 Color");
        static_assert(deletedHashValue & (1 << 4), "Color's deleted hash value must have some bits set that an RGBA32 Color wouldn't have");
        m_colorData.rgbaAndFlags = deletedHashValue;
        ASSERT(!isExtended());
    }

    bool isHashTableDeletedValue() const
    {
        return m_colorData.rgbaAndFlags == deletedHashValue;
    }

    explicit Color(WTF::HashTableEmptyValueType)
    {
        static_assert(emptyHashValue & invalidRGBAColor, "Color's empty hash value must not look like an ExtendedColor");
        static_assert(emptyHashValue & (1 << 4), "Color's deleted hash value must have some bits set that an RGBA32 Color wouldn't have");
        m_colorData.rgbaAndFlags = emptyHashValue;
        ASSERT(!isExtended());
    }

    // This creates an ExtendedColor.
    // FIXME: If the colorSpace is sRGB and the values can all be
    // converted exactly to integers, we should make a normal Color.
    WEBCORE_EXPORT Color(float r, float g, float b, float a, ColorSpace colorSpace);

    WEBCORE_EXPORT Color(const Color&);
    WEBCORE_EXPORT Color(Color&&);

    ~Color()
    {
        if (isExtended())
            m_colorData.extendedColor->deref();
    }

    // Returns the color serialized according to HTML5
    // <https://html.spec.whatwg.org/multipage/scripting.html#fill-and-stroke-styles> (10 September 2015)
    WEBCORE_EXPORT String serialized() const;

    WEBCORE_EXPORT String cssText() const;

    // Returns the color serialized as either #RRGGBB or #RRGGBBAA
    String nameForRenderTreeAsText() const;

    bool isValid() const { return isExtended() || (m_colorData.rgbaAndFlags & validRGBAColorBit); }

    bool isOpaque() const { return isExtended() ? asExtended().alpha() == 1.0 : rgb().isOpaque(); }
    bool isVisible() const { return isExtended() ? asExtended().alpha() > 0.0 : rgb().isVisible(); }

    int red() const { return rgb().redComponent(); }
    int green() const { return rgb().greenComponent(); }
    int blue() const { return rgb().blueComponent(); }
    int alpha() const { return rgb().alphaComponent(); }

    float alphaAsFloat() const { return isExtended() ? asExtended().alpha() : static_cast<float>(rgb().alphaComponent()) / 0xFF; }

    RGBA32 rgb() const;

    // FIXME: Like operator==, this will give different values for ExtendedColors that
    // should be identical, since the respective pointer will be different.
    unsigned hash() const { return WTF::intHash(m_colorData.rgbaAndFlags); }

    // FIXME: ExtendedColor - these should be renamed (to be clear about their parameter types, or
    // replaced with alternative accessors.
    WEBCORE_EXPORT void getRGBA(float& r, float& g, float& b, float& a) const;
    WEBCORE_EXPORT void getRGBA(double& r, double& g, double& b, double& a) const;
    WEBCORE_EXPORT void getHSL(double& h, double& s, double& l) const;
    WEBCORE_EXPORT void getHSV(double& h, double& s, double& v) const;

    Color light() const;
    Color dark() const;

    bool isDark() const;

    // This is an implementation of Porter-Duff's "source-over" equation
    Color blend(const Color&) const;
    Color blendWithWhite() const;

    Color colorWithAlphaMultipliedBy(float) const;

    // Returns a color that has the same RGB values, but with the given A.
    Color colorWithAlpha(float) const;
    Color opaqueColor() const { return colorWithAlpha(1.0f); }

    // True if the color originated from a CSS semantic color name.
    bool isSemantic() const { return !isExtended() && (m_colorData.rgbaAndFlags & isSemanticRBGAColorBit); }

#if PLATFORM(GTK)
    Color(const GdkRGBA&);
    operator GdkRGBA() const;
#endif

#if USE(CG)
    WEBCORE_EXPORT Color(CGColorRef);
    WEBCORE_EXPORT Color(CGColorRef, SemanticTag);
#endif

#if PLATFORM(WIN)
    WEBCORE_EXPORT Color(D2D1_COLOR_F);
    WEBCORE_EXPORT operator D2D1_COLOR_F() const;
    WEBCORE_EXPORT operator D2D1_VECTOR_4F() const;
#endif

    static bool parseHexColor(const String&, RGBA32&);
    static bool parseHexColor(const StringView&, RGBA32&);
    static bool parseHexColor(const LChar*, unsigned, RGBA32&);
    static bool parseHexColor(const UChar*, unsigned, RGBA32&);

    static constexpr SimpleColor black { 0xFF000000 };
    static constexpr SimpleColor white { 0xFFFFFFFF };
    static constexpr SimpleColor darkGray { 0xFF808080 };
    static constexpr SimpleColor gray { 0xFFA0A0A0 };
    static constexpr SimpleColor lightGray { 0xFFC0C0C0 };
    static constexpr SimpleColor transparent { 0x00000000 };
    static constexpr SimpleColor cyan { 0xFF00FFFF };
    static constexpr SimpleColor yellow { 0xFFFFFF00 };

#if PLATFORM(IOS_FAMILY)
    static constexpr SimpleColor compositionFill { 0x3CAFC0E3 };
#else
    static constexpr SimpleColor compositionFill { 0xFFE1DD55 };
#endif

    bool isExtended() const
    {
        return !(m_colorData.rgbaAndFlags & invalidRGBAColor);
    }
    WEBCORE_EXPORT ExtendedColor& asExtended() const;

    WEBCORE_EXPORT Color& operator=(const Color&);
    WEBCORE_EXPORT Color& operator=(Color&&);

    friend bool operator==(const Color& a, const Color& b);
    friend bool equalIgnoringSemanticColor(const Color& a, const Color& b);

    static bool isBlackColor(const Color&);
    static bool isWhiteColor(const Color&);

private:
    void setRGB(int r, int g, int b) { setRGB(makeRGB(r, g, b)); }
    void setRGB(RGBA32);
    void setIsSemantic() { m_colorData.rgbaAndFlags |= isSemanticRBGAColorBit; }

    // 0x_______00 is an ExtendedColor pointer.
    // 0x_______01 is an invalid RGBA32.
    // 0x_______11 is a valid RGBA32.
    static const uint64_t extendedColor = 0x0;
    static const uint64_t invalidRGBAColor = 0x1;
    static const uint64_t validRGBAColorBit = 0x2;
    static const uint64_t validRGBAColor = 0x3;
    static const uint64_t isSemanticRBGAColorBit = 0x4;

    static const uint64_t deletedHashValue = 0xFFFFFFFFFFFFFFFD;
    static const uint64_t emptyHashValue = 0xFFFFFFFFFFFFFFFB;

    WEBCORE_EXPORT void tagAsValid();

    union {
        uint64_t rgbaAndFlags { invalidRGBAColor };
        ExtendedColor* extendedColor;
    } m_colorData;
};

// FIXME: These do not work for ExtendedColor because
// they become just pointer comparison.
bool operator==(const Color&, const Color&);
bool operator!=(const Color&, const Color&);

Color colorFromPremultipliedARGB(RGBA32);
RGBA32 premultipliedARGBFromColor(const Color&);

Color blend(const Color& from, const Color& to, double progress, bool blendPremultiplied = true);

int differenceSquared(const Color&, const Color&);

uint16_t fastMultiplyBy255(uint16_t value);
uint16_t fastDivideBy255(uint16_t);

#if USE(CG)
WEBCORE_EXPORT CGColorRef cachedCGColor(const Color&);
#endif

WEBCORE_EXPORT WTF::TextStream& operator<<(WTF::TextStream&, const Color&);
WEBCORE_EXPORT WTF::TextStream& operator<<(WTF::TextStream&, ColorSpace);

inline bool operator==(SimpleColor a, SimpleColor b)
{
    return a.value() == b.value();
}

inline bool operator!=(SimpleColor a, SimpleColor b)
{
    return !(a == b);
}

inline bool operator==(const Color& a, const Color& b)
{
    return a.m_colorData.rgbaAndFlags == b.m_colorData.rgbaAndFlags;
}

inline bool operator!=(const Color& a, const Color& b)
{
    return !(a == b);
}

inline bool equalIgnoringSemanticColor(const Color& a, const Color& b)
{
    return (a.m_colorData.rgbaAndFlags & ~Color::isSemanticRBGAColorBit) == (b.m_colorData.rgbaAndFlags & ~Color::isSemanticRBGAColorBit);
}

inline uint8_t roundAndClampColorChannel(int value)
{
    return std::max(0, std::min(255, value));
}

inline uint8_t roundAndClampColorChannel(float value)
{
    return std::max(0.f, std::min(255.f, std::round(value)));
}

inline uint16_t fastMultiplyBy255(uint16_t value)
{
    return (value << 8) - value;
}

inline uint16_t fastDivideBy255(uint16_t value)
{
    // While this is an approximate algorithm for division by 255, it gives perfectly accurate results for 16-bit values.
    // FIXME: Since this gives accurate results for 16-bit values, we should get this optimization into compilers like clang.
    uint16_t approximation = value >> 8;
    uint16_t remainder = value - (approximation * 255) + 1;
    return approximation + (remainder >> 8);
}

inline RGBA32 colorWithOverrideAlpha(RGBA32 color, Optional<float> overrideAlpha)
{
    return overrideAlpha ? colorWithOverrideAlpha(color, overrideAlpha.value()) : color;
}

inline RGBA32 Color::rgb() const
{
    // FIXME: We should ASSERT(!isExtended()) here, or produce
    // an RGBA32 equivalent for an ExtendedColor. Ideally the former,
    // so we can audit all the rgb() call sites to handle extended.
    return { static_cast<uint32_t>(m_colorData.rgbaAndFlags >> 32) };
}

inline void Color::setRGB(RGBA32 rgb)
{
    m_colorData.rgbaAndFlags = static_cast<uint64_t>(rgb.value()) << 32;
    tagAsValid();
}

inline bool Color::isBlackColor(const Color& color)
{
    if (color.isExtended()) {
        const ExtendedColor& extendedColor = color.asExtended();
        return !extendedColor.red() && !extendedColor.green() && !extendedColor.blue() && extendedColor.alpha() == 1;
    }

    return color.rgb() == Color::black;
}

inline bool Color::isWhiteColor(const Color& color)
{
    if (color.isExtended()) {
        const ExtendedColor& extendedColor = color.asExtended();
        return extendedColor.red() == 1 && extendedColor.green() == 1 && extendedColor.blue() == 1 && extendedColor.alpha() == 1;
    }

    return color.rgb() == Color::white;
}

} // namespace WebCore

namespace WTF {
template<> struct DefaultHash<WebCore::Color>;
template<> struct HashTraits<WebCore::Color>;
}
