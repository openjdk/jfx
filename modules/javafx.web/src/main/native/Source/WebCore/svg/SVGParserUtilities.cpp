/*
 * Copyright (C) 2002, 2003 The Karbon Developers
 * Copyright (C) 2006 Alexander Kellett <lypanov@kde.org>
 * Copyright (C) 2006, 2007 Rob Buis <buis@kde.org>
 * Copyright (C) 2007-2018 Apple Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#include "config.h"
#include "SVGParserUtilities.h"

#include "Document.h"
#include "FloatRect.h"
#include <limits>
#include <wtf/ASCIICType.h>
#include <wtf/text/StringView.h>

namespace WebCore {

template <typename FloatType> static inline bool isValidRange(const FloatType& x)
{
    static const FloatType max = std::numeric_limits<FloatType>::max();
    return x >= -max && x <= max;
}

// We use this generic parseNumber function to allow the Path parsing code to work
// at a higher precision internally, without any unnecessary runtime cost or code
// complexity.
template <typename CharacterType, typename FloatType> static bool genericParseNumber(const CharacterType*& ptr, const CharacterType* end, FloatType& number, bool skip)
{
    FloatType integer, decimal, frac, exponent;
    int sign, expsign;
    const CharacterType* start = ptr;

    exponent = 0;
    integer = 0;
    frac = 1;
    decimal = 0;
    sign = 1;
    expsign = 1;

    // read the sign
    if (ptr < end && *ptr == '+')
        ptr++;
    else if (ptr < end && *ptr == '-') {
        ptr++;
        sign = -1;
    }

    if (ptr == end || (!isASCIIDigit(*ptr) && *ptr != '.'))
        return false;

    // read the integer part, build right-to-left
    const CharacterType* ptrStartIntPart = ptr;
    while (ptr < end && isASCIIDigit(*ptr))
        ++ptr; // Advance to first non-digit.

    if (ptr != ptrStartIntPart) {
        const CharacterType* ptrScanIntPart = ptr - 1;
        FloatType multiplier = 1;
        while (ptrScanIntPart >= ptrStartIntPart) {
            integer += multiplier * static_cast<FloatType>(*(ptrScanIntPart--) - '0');
            multiplier *= 10;
        }
        // Bail out early if this overflows.
        if (!isValidRange(integer))
            return false;
    }

    if (ptr < end && *ptr == '.') { // read the decimals
        ptr++;

        // There must be a least one digit following the .
        if (ptr >= end || !isASCIIDigit(*ptr))
            return false;

        while (ptr < end && isASCIIDigit(*ptr))
            decimal += (*(ptr++) - '0') * (frac *= static_cast<FloatType>(0.1));
    }

    // read the exponent part
    if (ptr != start && ptr + 1 < end && (*ptr == 'e' || *ptr == 'E')
        && (ptr[1] != 'x' && ptr[1] != 'm')) {
        ptr++;

        // read the sign of the exponent
        if (*ptr == '+')
            ptr++;
        else if (*ptr == '-') {
            ptr++;
            expsign = -1;
        }

        // There must be an exponent
        if (ptr >= end || !isASCIIDigit(*ptr))
            return false;

        while (ptr < end && isASCIIDigit(*ptr)) {
            exponent *= static_cast<FloatType>(10);
            exponent += *ptr - '0';
            ptr++;
        }
        // Make sure exponent is valid.
        if (!isValidRange(exponent) || exponent > std::numeric_limits<FloatType>::max_exponent)
            return false;
    }

    number = integer + decimal;
    number *= sign;

    if (exponent)
        number *= static_cast<FloatType>(pow(10.0, expsign * static_cast<int>(exponent)));

    // Don't return Infinity() or NaN().
    if (!isValidRange(number))
        return false;

    if (start == ptr)
        return false;

    if (skip)
        skipOptionalSVGSpacesOrDelimiter(ptr, end);

    return true;
}

template <typename CharacterType>
bool parseSVGNumber(CharacterType* begin, size_t length, double& number)
{
    const CharacterType* ptr = begin;
    const CharacterType* end = ptr + length;
    return genericParseNumber(ptr, end, number, false);
}

// Explicitly instantiate the two flavors of parseSVGNumber() to satisfy external callers
template bool parseSVGNumber(LChar* begin, size_t length, double&);
template bool parseSVGNumber(UChar* begin, size_t length, double&);

bool parseNumber(const LChar*& ptr, const LChar* end, float& number, bool skip)
{
    return genericParseNumber(ptr, end, number, skip);
}

bool parseNumber(const UChar*& ptr, const UChar* end, float& number, bool skip)
{
    return genericParseNumber(ptr, end, number, skip);
}

bool parseNumberFromString(const String& string, float& number, bool skip)
{
    auto upconvertedCharacters = StringView(string).upconvertedCharacters();
    const UChar* ptr = upconvertedCharacters;
    const UChar* end = ptr + string.length();
    return genericParseNumber(ptr, end, number, skip) && ptr == end;
}

// only used to parse largeArcFlag and sweepFlag which must be a "0" or "1"
// and might not have any whitespace/comma after it
template <typename CharacterType>
bool genericParseArcFlag(const CharacterType*& ptr, const CharacterType* end, bool& flag)
{
    if (ptr >= end)
        return false;
    const CharacterType flagChar = *ptr++;
    if (flagChar == '0')
        flag = false;
    else if (flagChar == '1')
        flag = true;
    else
        return false;

    skipOptionalSVGSpacesOrDelimiter(ptr, end);

    return true;
}

bool parseArcFlag(const LChar*& ptr, const LChar* end, bool& flag)
{
    return genericParseArcFlag(ptr, end, flag);
}

bool parseArcFlag(const UChar*& ptr, const UChar* end, bool& flag)
{
    return genericParseArcFlag(ptr, end, flag);
}

bool parseNumberOptionalNumber(const String& s, float& x, float& y)
{
    if (s.isEmpty())
        return false;

    auto upconvertedCharacters = StringView(s).upconvertedCharacters();
    const UChar* cur = upconvertedCharacters;
    const UChar* end = cur + s.length();

    if (!parseNumber(cur, end, x))
        return false;

    if (cur == end)
        y = x;
    else if (!parseNumber(cur, end, y, false))
        return false;

    return cur == end;
}

bool parsePoint(const String& s, FloatPoint& point)
{
    if (s.isEmpty())
        return false;
    auto upconvertedCharacters = StringView(s).upconvertedCharacters();
    const UChar* cur = upconvertedCharacters;
    const UChar* end = cur + s.length();

    if (!skipOptionalSVGSpaces(cur, end))
        return false;

    float x = 0;
    if (!parseNumber(cur, end, x))
        return false;

    float y = 0;
    if (!parseNumber(cur, end, y))
        return false;

    point = FloatPoint(x, y);

    // Disallow anything except spaces at the end.
    return !skipOptionalSVGSpaces(cur, end);
}

bool parseRect(const String& string, FloatRect& rect)
{
    auto upconvertedCharacters = StringView(string).upconvertedCharacters();
    const UChar* ptr = upconvertedCharacters;
    const UChar* end = ptr + string.length();
    skipOptionalSVGSpaces(ptr, end);

    float x = 0;
    float y = 0;
    float width = 0;
    float height = 0;
    bool valid = parseNumber(ptr, end, x) && parseNumber(ptr, end, y) && parseNumber(ptr, end, width) && parseNumber(ptr, end, height, false);
    rect = FloatRect(x, y, width, height);
    return valid;
}

bool parseGlyphName(const String& input, HashSet<String>& values)
{
    // FIXME: Parsing error detection is missing.
    values.clear();

    auto upconvertedCharacters = StringView(input).upconvertedCharacters();
    const UChar* ptr = upconvertedCharacters;
    const UChar* end = ptr + input.length();
    skipOptionalSVGSpaces(ptr, end);

    while (ptr < end) {
        // Leading and trailing white space, and white space before and after separators, will be ignored.
        const UChar* inputStart = ptr;
        while (ptr < end && *ptr != ',')
            ++ptr;

        if (ptr == inputStart)
            break;

        // walk backwards from the ; to ignore any whitespace
        const UChar* inputEnd = ptr - 1;
        while (inputStart < inputEnd && isSVGSpace(*inputEnd))
            --inputEnd;

        values.add(String(inputStart, inputEnd - inputStart + 1));
        skipOptionalSVGSpacesOrDelimiter(ptr, end, ',');
    }

    return true;
}

static bool parseUnicodeRange(const UChar* characters, unsigned length, UnicodeRange& range)
{
    if (length < 2 || characters[0] != 'U' || characters[1] != '+')
        return false;

    // Parse the starting hex number (or its prefix).
    unsigned startRange = 0;
    unsigned startLength = 0;

    const UChar* ptr = characters + 2;
    const UChar* end = characters + length;
    while (ptr < end) {
        if (!isASCIIHexDigit(*ptr))
            break;
        ++startLength;
        if (startLength > 6)
            return false;
        startRange = (startRange << 4) | toASCIIHexValue(*ptr);
        ++ptr;
    }

    // Handle the case of ranges separated by "-" sign.
    if (2 + startLength < length && *ptr == '-') {
        if (!startLength)
            return false;

        // Parse the ending hex number (or its prefix).
        unsigned endRange = 0;
        unsigned endLength = 0;
        ++ptr;
        while (ptr < end) {
            if (!isASCIIHexDigit(*ptr))
                break;
            ++endLength;
            if (endLength > 6)
                return false;
            endRange = (endRange << 4) | toASCIIHexValue(*ptr);
            ++ptr;
        }

        if (!endLength)
            return false;

        range.first = startRange;
        range.second = endRange;
        return true;
    }

    // Handle the case of a number with some optional trailing question marks.
    unsigned endRange = startRange;
    while (ptr < end) {
        if (*ptr != '?')
            break;
        ++startLength;
        if (startLength > 6)
            return false;
        startRange <<= 4;
        endRange = (endRange << 4) | 0xF;
        ++ptr;
    }

    if (!startLength)
        return false;

    range.first = startRange;
    range.second = endRange;
    return true;
}

bool parseKerningUnicodeString(const String& input, UnicodeRanges& rangeList, HashSet<String>& stringList)
{
    // FIXME: Parsing error detection is missing.
    auto upconvertedCharacters = StringView(input).upconvertedCharacters();
    const UChar* ptr = upconvertedCharacters;
    const UChar* end = ptr + input.length();

    while (ptr < end) {
        const UChar* inputStart = ptr;
        while (ptr < end && *ptr != ',')
            ++ptr;

        if (ptr == inputStart)
            break;

        // Try to parse unicode range first
        UnicodeRange range;
        if (parseUnicodeRange(inputStart, ptr - inputStart, range))
            rangeList.append(range);
        else
            stringList.add(String(inputStart, ptr - inputStart));
        ++ptr;
    }

    return true;
}

Vector<String> parseDelimitedString(const String& input, const char seperator)
{
    Vector<String> values;

    auto upconvertedCharacters = StringView(input).upconvertedCharacters();
    const UChar* ptr = upconvertedCharacters;
    const UChar* end = ptr + input.length();
    skipOptionalSVGSpaces(ptr, end);

    while (ptr < end) {
        // Leading and trailing white space, and white space before and after semicolon separators, will be ignored.
        const UChar* inputStart = ptr;
        while (ptr < end && *ptr != seperator) // careful not to ignore whitespace inside inputs
            ptr++;

        if (ptr == inputStart)
            break;

        // walk backwards from the ; to ignore any whitespace
        const UChar* inputEnd = ptr - 1;
        while (inputStart < inputEnd && isSVGSpace(*inputEnd))
            inputEnd--;

        values.append(String(inputStart, inputEnd - inputStart + 1));
        skipOptionalSVGSpacesOrDelimiter(ptr, end, seperator);
    }

    return values;
}

template <typename CharacterType>
bool parseFloatPoint(const CharacterType*& current, const CharacterType* end, FloatPoint& point)
{
    float x;
    float y;
    if (!parseNumber(current, end, x)
        || !parseNumber(current, end, y))
        return false;
    point = FloatPoint(x, y);
    return true;
}

template bool parseFloatPoint(const LChar*& current, const LChar* end, FloatPoint& point1);
template bool parseFloatPoint(const UChar*& current, const UChar* end, FloatPoint& point1);

template <typename CharacterType>
inline bool parseFloatPoint2(const CharacterType*& current, const CharacterType* end, FloatPoint& point1, FloatPoint& point2)
{
    float x1;
    float y1;
    float x2;
    float y2;
    if (!parseNumber(current, end, x1)
        || !parseNumber(current, end, y1)
        || !parseNumber(current, end, x2)
        || !parseNumber(current, end, y2))
        return false;
    point1 = FloatPoint(x1, y1);
    point2 = FloatPoint(x2, y2);
    return true;
}

template bool parseFloatPoint2(const LChar*& current, const LChar* end, FloatPoint& point1, FloatPoint& point2);
template bool parseFloatPoint2(const UChar*& current, const UChar* end, FloatPoint& point1, FloatPoint& point2);

template <typename CharacterType>
bool parseFloatPoint3(const CharacterType*& current, const CharacterType* end, FloatPoint& point1, FloatPoint& point2, FloatPoint& point3)
{
    float x1;
    float y1;
    float x2;
    float y2;
    float x3;
    float y3;
    if (!parseNumber(current, end, x1)
        || !parseNumber(current, end, y1)
        || !parseNumber(current, end, x2)
        || !parseNumber(current, end, y2)
        || !parseNumber(current, end, x3)
        || !parseNumber(current, end, y3))
        return false;
    point1 = FloatPoint(x1, y1);
    point2 = FloatPoint(x2, y2);
    point3 = FloatPoint(x3, y3);
    return true;
}

template bool parseFloatPoint3(const LChar*& current, const LChar* end, FloatPoint& point1, FloatPoint& point2, FloatPoint& point3);
template bool parseFloatPoint3(const UChar*& current, const UChar* end, FloatPoint& point1, FloatPoint& point2, FloatPoint& point3);

}
