/*
 * Copyright (C) 2007-2025 Apple Inc. All rights reserved.
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

#include <unicode/utypes.h>

namespace WTF::Unicode {

// Names here are taken from the Unicode standard.

// Most of these are UChar constants, not char32_t, which makes them
// more convenient for WebCore code that mostly uses UTF-16.

constexpr UChar HiraganaLetterSmallA = 0x3041;
constexpr UChar activateArabicFormShaping = 0x206D;
constexpr UChar activateSymmetricSwapping = 0x206B;
constexpr char32_t aegeanWordSeparatorDot = 0x10101;
constexpr char32_t aegeanWordSeparatorLine = 0x10100;
constexpr UChar apostrophe = 0x0027;
constexpr UChar arabicIndicPerMilleSign = 0x0609;
constexpr UChar arabicIndicPerTenThousandSign = 0x060A;
constexpr UChar arabicPercentSign = 0x066A;
constexpr UChar blackCircle = 0x25CF;
constexpr UChar blackDownPointingSmallTriangle = 0x25BE;
constexpr UChar blackLeftPointingSmallTriangle = 0x25C2;
constexpr UChar blackRightPointingSmallTriangle = 0x25B8;
constexpr UChar blackSquare = 0x25A0;
constexpr UChar blackUpPointingSmallTriangle = 0x25B4;
constexpr UChar blackUpPointingTriangle = 0x25B2;
constexpr UChar bullet = 0x2022;
constexpr UChar bullseye = 0x25CE;
constexpr UChar byteOrderMark = 0xFEFF;
constexpr UChar carriageReturn = 0x000D;
constexpr UChar cjkWater = 0x6C34;
constexpr UChar combiningEnclosingKeycap = 0x20E3;
constexpr UChar deleteCharacter = 0x007F;
constexpr UChar doubleHighReversed9QuotationMark = 0x201F;
constexpr UChar doubleLowReversed9QuotationMark = 0x2E42;
constexpr UChar doublePrimeQuotationMark = 0x301E;
constexpr UChar emSpace = 0x2003;
constexpr char32_t emojiCat = 0x1F408;
constexpr UChar emojiVariationSelector = 0xFE0F; // Technical name is "VARIATION SELECTOR-16"
constexpr UChar enDash = 0x2013;
constexpr UChar ethiopicPrefaceColon = 0x1366;
constexpr UChar ethiopicWordspace = 0x1361;
constexpr UChar firstStrongIsolate = 0x2068;
constexpr UChar fisheye = 0x25C9;
constexpr UChar formFeed = 0x000C;
constexpr UChar fullwidthAmpersand = 0xFF06;
constexpr UChar fullwidthApostrophe = 0xFF07;
constexpr UChar fullwidthCommercialAt = 0xFF20;
constexpr UChar fullwidthNumberSign = 0xFF03;
constexpr UChar fullwidthPercentSign = 0xFF05;
constexpr UChar fullwidthQuotationMark = 0xFF02;
constexpr UChar functionApplication = 0x2061;
constexpr UChar halfwidthLeftCornerBracket = 0xFF62;
constexpr UChar halfwidthRightCornerBracket = 0xFF63;
constexpr UChar hebrewPunctuationGeresh = 0x05F3;
constexpr UChar hebrewPunctuationGershayim = 0x05F4;
constexpr UChar horizontalEllipsis = 0x2026;
constexpr UChar hyphen = 0x2010;
constexpr UChar hyphenMinus = 0x002D;
constexpr UChar ideographicComma = 0x3001;
constexpr UChar ideographicFullStop = 0x3002;
constexpr UChar ideographicSpace = 0x3000;
constexpr UChar inhibitArabicFormShaping = 0x206C;
constexpr UChar inhibitSymmetricSwapping = 0x206A;
constexpr UChar invisibleSeparator = 0x2063;
constexpr UChar invisibleTimes = 0x2062;
constexpr UChar leftCornerBracket = 0x300C;
constexpr UChar leftDoubleQuotationMark = 0x201C;
constexpr UChar leftLowDoubleQuotationMark = 0x201E;
constexpr UChar leftLowSingleQuotationMark = 0x201A;
constexpr UChar leftPointingDoubleAngleQuotationMark = 0x00AB;
constexpr UChar leftSingleQuotationMark = 0x2018;
constexpr UChar leftToRightEmbed = 0x202A;
constexpr UChar leftToRightIsolate = 0x2066;
constexpr UChar leftToRightMark = 0x200E;
constexpr UChar leftToRightOverride = 0x202D;
constexpr UChar leftWhiteCornerBracket = 0x300E;
constexpr UChar lineSeparator = 0x2028;
constexpr UChar lowDoublePrimeQuotationMark = 0x301F;
constexpr UChar lowLine = 0x005F;
constexpr UChar mediumShade = 0x2592;
constexpr UChar minusSign = 0x2212;
constexpr UChar multiplicationSign = 0x00D7;
constexpr UChar narrowNoBreakSpace = 0x202F;
constexpr UChar nationalDigitShapes = 0x206E;
constexpr UChar newlineCharacter = 0x000A;
constexpr UChar noBreakSpace = 0x00A0;
constexpr UChar nominalDigitShapes = 0x206F;
constexpr UChar nullCharacter = 0x0;
constexpr UChar objectReplacementCharacter = 0xFFFC;
constexpr UChar optionKey = 0x2325;
constexpr UChar paragraphSeparator = 0x2029;
constexpr UChar partAlternationMark = 0x303D;
constexpr UChar perMilleSign = 0x2030;
constexpr UChar perTenThousandSign = 0x2031;
constexpr UChar pilcrowSign = 0x00B6;
constexpr UChar popDirectionalFormatting = 0x202C;
constexpr UChar popDirectionalIsolate = 0x2069;
constexpr UChar presentationFormForVerticalLeftCornerBracket = 0xFE41;
constexpr UChar presentationFormForVerticalLeftWhiteCornerBracket = 0xFE43;
constexpr UChar presentationFormForVerticalRightCornerBracket = 0xFE42;
constexpr UChar presentationFormForVerticalRightWhiteCornerBracket = 0xFE44;
constexpr UChar quotationMark = 0x0022;
constexpr UChar replacementCharacter = 0xFFFD;
constexpr UChar reverseSolidus = 0x005C;
constexpr UChar reversedDoublePrimeQuotationMark = 0x301D;
constexpr UChar reversedPilcrowSign = 0x204B;
constexpr UChar rightCornerBracket = 0x300D;
constexpr UChar rightDoubleQuotationMark = 0x201D;
constexpr UChar rightPointingDoubleAngleQuotationMark = 0x00BB;
constexpr UChar rightSingleQuotationMark = 0x2019;
constexpr UChar rightToLeftEmbed = 0x202B;
constexpr UChar rightToLeftIsolate = 0x2067;
constexpr UChar rightToLeftMark = 0x200F;
constexpr UChar rightToLeftOverride = 0x202E;
constexpr UChar rightWhiteCornerBracket = 0x300F;
constexpr UChar sectionSign = 0x00A7;
constexpr UChar sesameDot = 0xFE45;
constexpr UChar singleLeftPointingAngleQuotationMark = 0x2039;
constexpr UChar singleLow9QuotationMark = 0x201B;
constexpr UChar singleRightPointingAngleQuotationMark = 0x203A;
constexpr UChar smallAmpersand = 0xFE60;
constexpr UChar smallCommercialAt = 0xFE6B;
constexpr UChar smallLetterSharpS = 0x00DF;
constexpr UChar smallNumberSign = 0xFE5F;
constexpr UChar smallPercentSign = 0xFE6A;
constexpr UChar softHyphen = 0x00AD;
constexpr UChar space = 0x0020;
constexpr UChar swungDash = 0x2053;
constexpr UChar tabCharacter = 0x0009;
constexpr UChar textVariationSelector = 0xFE0E; // Technical name is "VARIATION SELECTOR-15"
constexpr UChar thinSpace = 0x2009;
constexpr UChar tibetanMarkDelimiterTshegBstar = 0x0F0C;
constexpr UChar tibetanMarkIntersyllabicTsheg = 0x0F0B;
constexpr UChar tironianSignEt = 0x204A;
constexpr char32_t ugariticWordDivider = 0x1039F;
constexpr UChar upArrowhead = 0x2303;
constexpr UChar verticalEllipsis = 0x22EE;
constexpr UChar verticalTabulation = 0x000b;
constexpr UChar whiteBullet = 0x25E6;
constexpr UChar whiteCircle = 0x25CB;
constexpr UChar whiteSesameDot = 0xFE46;
constexpr UChar whiteUpPointingTriangle = 0x25B3;
constexpr UChar wordJoiner = 0x2060;
constexpr UChar yenSign = 0x00A5;
constexpr UChar zeroWidthJoiner = 0x200D;
constexpr UChar zeroWidthNoBreakSpace = 0xFEFF;
constexpr UChar zeroWidthNonJoiner = 0x200C;
constexpr UChar zeroWidthSpace = 0x200B;

} // namespace WTF::Unicode

using WTF::Unicode::HiraganaLetterSmallA;
using WTF::Unicode::activateArabicFormShaping;
using WTF::Unicode::activateSymmetricSwapping;
using WTF::Unicode::aegeanWordSeparatorDot;
using WTF::Unicode::aegeanWordSeparatorLine;
using WTF::Unicode::arabicIndicPerMilleSign;
using WTF::Unicode::arabicIndicPerTenThousandSign;
using WTF::Unicode::arabicPercentSign;
using WTF::Unicode::blackCircle;
using WTF::Unicode::blackDownPointingSmallTriangle;
using WTF::Unicode::blackLeftPointingSmallTriangle;
using WTF::Unicode::blackRightPointingSmallTriangle;
using WTF::Unicode::blackSquare;
using WTF::Unicode::blackUpPointingSmallTriangle;
using WTF::Unicode::blackUpPointingTriangle;
using WTF::Unicode::bullet;
using WTF::Unicode::bullseye;
using WTF::Unicode::byteOrderMark;
using WTF::Unicode::carriageReturn;
using WTF::Unicode::cjkWater;
using WTF::Unicode::combiningEnclosingKeycap;
using WTF::Unicode::deleteCharacter;
using WTF::Unicode::doubleHighReversed9QuotationMark;
using WTF::Unicode::doubleLowReversed9QuotationMark;
using WTF::Unicode::doublePrimeQuotationMark;
using WTF::Unicode::emSpace;
using WTF::Unicode::emojiCat;
using WTF::Unicode::emojiVariationSelector;
using WTF::Unicode::enDash;
using WTF::Unicode::ethiopicPrefaceColon;
using WTF::Unicode::ethiopicWordspace;
using WTF::Unicode::firstStrongIsolate;
using WTF::Unicode::fisheye;
using WTF::Unicode::formFeed;
using WTF::Unicode::fullwidthAmpersand;
using WTF::Unicode::fullwidthApostrophe;
using WTF::Unicode::fullwidthCommercialAt;
using WTF::Unicode::fullwidthNumberSign;
using WTF::Unicode::fullwidthPercentSign;
using WTF::Unicode::fullwidthQuotationMark;
using WTF::Unicode::functionApplication;
using WTF::Unicode::halfwidthLeftCornerBracket;
using WTF::Unicode::halfwidthRightCornerBracket;
using WTF::Unicode::hebrewPunctuationGeresh;
using WTF::Unicode::hebrewPunctuationGershayim;
using WTF::Unicode::horizontalEllipsis;
using WTF::Unicode::hyphen;
using WTF::Unicode::hyphenMinus;
using WTF::Unicode::ideographicComma;
using WTF::Unicode::ideographicFullStop;
using WTF::Unicode::ideographicSpace;
using WTF::Unicode::inhibitArabicFormShaping;
using WTF::Unicode::inhibitSymmetricSwapping;
using WTF::Unicode::invisibleSeparator;
using WTF::Unicode::invisibleTimes;
using WTF::Unicode::leftCornerBracket;
using WTF::Unicode::leftDoubleQuotationMark;
using WTF::Unicode::leftLowDoubleQuotationMark;
using WTF::Unicode::leftLowSingleQuotationMark;
using WTF::Unicode::leftPointingDoubleAngleQuotationMark;
using WTF::Unicode::leftSingleQuotationMark;
using WTF::Unicode::leftToRightEmbed;
using WTF::Unicode::leftToRightIsolate;
using WTF::Unicode::leftToRightMark;
using WTF::Unicode::leftToRightOverride;
using WTF::Unicode::leftWhiteCornerBracket;
using WTF::Unicode::lineSeparator;
using WTF::Unicode::lowDoublePrimeQuotationMark;
using WTF::Unicode::lowLine;
using WTF::Unicode::mediumShade;
using WTF::Unicode::minusSign;
using WTF::Unicode::multiplicationSign;
using WTF::Unicode::narrowNoBreakSpace;
using WTF::Unicode::nationalDigitShapes;
using WTF::Unicode::newlineCharacter;
using WTF::Unicode::noBreakSpace;
using WTF::Unicode::nominalDigitShapes;
using WTF::Unicode::nullCharacter;
using WTF::Unicode::objectReplacementCharacter;
using WTF::Unicode::paragraphSeparator;
using WTF::Unicode::partAlternationMark;
using WTF::Unicode::perMilleSign;
using WTF::Unicode::perTenThousandSign;
using WTF::Unicode::pilcrowSign;
using WTF::Unicode::popDirectionalFormatting;
using WTF::Unicode::popDirectionalIsolate;
using WTF::Unicode::presentationFormForVerticalLeftCornerBracket;
using WTF::Unicode::presentationFormForVerticalLeftWhiteCornerBracket;
using WTF::Unicode::presentationFormForVerticalRightCornerBracket;
using WTF::Unicode::presentationFormForVerticalRightWhiteCornerBracket;
using WTF::Unicode::quotationMark;
using WTF::Unicode::replacementCharacter;
using WTF::Unicode::reverseSolidus;
using WTF::Unicode::reversedDoublePrimeQuotationMark;
using WTF::Unicode::reversedPilcrowSign;
using WTF::Unicode::rightCornerBracket;
using WTF::Unicode::rightDoubleQuotationMark;
using WTF::Unicode::rightPointingDoubleAngleQuotationMark;
using WTF::Unicode::rightSingleQuotationMark;
using WTF::Unicode::rightToLeftEmbed;
using WTF::Unicode::rightToLeftIsolate;
using WTF::Unicode::rightToLeftMark;
using WTF::Unicode::rightToLeftOverride;
using WTF::Unicode::rightWhiteCornerBracket;
using WTF::Unicode::sectionSign;
using WTF::Unicode::sesameDot;
using WTF::Unicode::singleLeftPointingAngleQuotationMark;
using WTF::Unicode::singleLow9QuotationMark;
using WTF::Unicode::singleRightPointingAngleQuotationMark;
using WTF::Unicode::smallAmpersand;
using WTF::Unicode::smallCommercialAt;
using WTF::Unicode::smallNumberSign;
using WTF::Unicode::smallPercentSign;
using WTF::Unicode::softHyphen;
using WTF::Unicode::space;
using WTF::Unicode::swungDash;
using WTF::Unicode::tabCharacter;
using WTF::Unicode::textVariationSelector;
using WTF::Unicode::thinSpace;
using WTF::Unicode::tibetanMarkDelimiterTshegBstar;
using WTF::Unicode::tibetanMarkIntersyllabicTsheg;
using WTF::Unicode::tironianSignEt;
using WTF::Unicode::ugariticWordDivider;
using WTF::Unicode::upArrowhead;
using WTF::Unicode::verticalEllipsis;
using WTF::Unicode::verticalTabulation;
using WTF::Unicode::whiteBullet;
using WTF::Unicode::whiteCircle;
using WTF::Unicode::whiteSesameDot;
using WTF::Unicode::whiteUpPointingTriangle;
using WTF::Unicode::wordJoiner;
using WTF::Unicode::yenSign;
using WTF::Unicode::zeroWidthJoiner;
using WTF::Unicode::zeroWidthNoBreakSpace;
using WTF::Unicode::zeroWidthNonJoiner;
using WTF::Unicode::zeroWidthSpace;
