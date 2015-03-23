/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef JSC_UNICODE_JAVA_H
#define JSC_UNICODE_JAVA_H

#include <wtf/Assertions.h>
#include <wtf/ASCIICType.h>
//#include <wtf/unicode/ScriptCodesFromICU.h>
//#include <wtf/unicode/UnicodeMacrosFromICU.h>

#include <stdint.h>

#include "java_lang_Character.h"

#define CharProp(p) java_lang_Character_##p

#if PLATFORM(JAVA) && OS(WINDOWS)
typedef wchar_t UChar;
#else
typedef uint16_t UChar;
#endif

#ifndef __UMACHINE_H__
typedef uint32_t UChar32;
#endif

#define U_MASK(x) ((uint32_t)1<<(x))
#define USE_FAST_PATH(c, fast, slow) ((c) <= 0x7F ? fast((char)c) : slow(c))

#define CHECK_PROPERTY(c, mask, isSet) \
    (((category(c) & (mask)) == 0) == ((isSet) == 0))


namespace WTF {
  namespace Unicode {
    namespace Java {
      bool isSpaceChar(uint32_t);
      bool isLetterOrDigit(uint32_t);

      uint32_t toLowerCase(uint32_t);
      uint32_t toUpperCase(uint32_t);
      uint32_t toTitleCase(uint32_t);

      int getType(uint32_t);
      int getNumericValue(uint32_t);
      int getDirectionality(uint32_t);

      int toLowerCase(uint16_t*, int, const uint16_t*, int, bool*);
      int toUpperCase(uint16_t*, int, const uint16_t*, int, bool*);
      int foldCase(uint16_t*, int, const uint16_t*, int, bool*);
    }

    enum Direction {
      LeftToRight = CharProp(DIRECTIONALITY_LEFT_TO_RIGHT),
      RightToLeft = CharProp(DIRECTIONALITY_RIGHT_TO_LEFT),
      EuropeanNumber = CharProp(DIRECTIONALITY_EUROPEAN_NUMBER),
      EuropeanNumberSeparator = CharProp(DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR),
      EuropeanNumberTerminator = CharProp(DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR),
      ArabicNumber = CharProp(DIRECTIONALITY_ARABIC_NUMBER),
      CommonNumberSeparator = CharProp(DIRECTIONALITY_COMMON_NUMBER_SEPARATOR),
      BlockSeparator = CharProp(DIRECTIONALITY_PARAGRAPH_SEPARATOR),
      SegmentSeparator = CharProp(DIRECTIONALITY_SEGMENT_SEPARATOR),
      WhiteSpaceNeutral = CharProp(DIRECTIONALITY_WHITESPACE),
      OtherNeutral = CharProp(DIRECTIONALITY_OTHER_NEUTRALS),
      LeftToRightEmbedding = CharProp(DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING),
      LeftToRightOverride = CharProp(DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE),
      RightToLeftArabic = CharProp(DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC),
      RightToLeftEmbedding = CharProp(DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING),
      RightToLeftOverride = CharProp(DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE),
      PopDirectionalFormat = CharProp(DIRECTIONALITY_POP_DIRECTIONAL_FORMAT),
      NonSpacingMark = CharProp(DIRECTIONALITY_NONSPACING_MARK),
      BoundaryNeutral = CharProp(DIRECTIONALITY_BOUNDARY_NEUTRAL),
    };

    // Unused: Java doesn't support decomposition types.
    enum DecompositionType {
      DecompositionNone,
      DecompositionCanonical,
      DecompositionCompat,
      DecompositionCircle,
      DecompositionFinal,
      DecompositionFont,
      DecompositionFraction,
      DecompositionInitial,
      DecompositionIsolated,
      DecompositionMedial,
      DecompositionNarrow,
      DecompositionNoBreak,
      DecompositionSmall,
      DecompositionSquare,
      DecompositionSub,
      DecompositionSuper,
      DecompositionVertical,
      DecompositionWide,
    };

    enum CharCategory {
      NoCategory = 0,
      Other_NotAssigned = U_MASK(CharProp(UNASSIGNED)),
      Letter_Uppercase = U_MASK(CharProp(UPPERCASE_LETTER)),
      Letter_Lowercase = U_MASK(CharProp(LOWERCASE_LETTER)),
      Letter_Titlecase = U_MASK(CharProp(TITLECASE_LETTER)),
      Letter_Modifier = U_MASK(CharProp(MODIFIER_LETTER)),
      Letter_Other = U_MASK(CharProp(OTHER_LETTER)),

      Mark_NonSpacing = U_MASK(CharProp(NON_SPACING_MARK)),
      Mark_Enclosing = U_MASK(CharProp(ENCLOSING_MARK)),
      Mark_SpacingCombining = U_MASK(CharProp(COMBINING_SPACING_MARK)),

      Number_DecimalDigit = U_MASK(CharProp(DECIMAL_DIGIT_NUMBER)),
      Number_Letter = U_MASK(CharProp(LETTER_NUMBER)),
      Number_Other = U_MASK(CharProp(OTHER_NUMBER)),

      Separator_Space = U_MASK(CharProp(SPACE_SEPARATOR)),
      Separator_Line = U_MASK(CharProp(LINE_SEPARATOR)),
      Separator_Paragraph = U_MASK(CharProp(PARAGRAPH_SEPARATOR)),

      Other_Control = U_MASK(CharProp(CONTROL)),
      Other_Format = U_MASK(CharProp(FORMAT)),
      Other_PrivateUse = U_MASK(CharProp(PRIVATE_USE)),
      Other_Surrogate = U_MASK(CharProp(SURROGATE)),

      Punctuation_Dash = U_MASK(CharProp(DASH_PUNCTUATION)),
      Punctuation_Open = U_MASK(CharProp(START_PUNCTUATION)),
      Punctuation_Close = U_MASK(CharProp(END_PUNCTUATION)),
      Punctuation_Connector = U_MASK(CharProp(CONNECTOR_PUNCTUATION)),
      Punctuation_Other = U_MASK(CharProp(OTHER_PUNCTUATION)),

      Symbol_Math = U_MASK(CharProp(MATH_SYMBOL)),
      Symbol_Currency = U_MASK(CharProp(CURRENCY_SYMBOL)),
      Symbol_Modifier = U_MASK(CharProp(MODIFIER_SYMBOL)),
      Symbol_Other = U_MASK(CharProp(OTHER_SYMBOL)),

      Punctuation_InitialQuote = U_MASK(CharProp(INITIAL_QUOTE_PUNCTUATION)),
      Punctuation_FinalQuote = U_MASK(CharProp(FINAL_QUOTE_PUNCTUATION)),
    };

    inline UChar32 toLower(UChar32 c) {
        return USE_FAST_PATH(c, WTF::toASCIILower, Java::toLowerCase);
    }

    inline int toLower(UChar* result, int resultLength,
                       const UChar* src, int srcLength, bool* error) {
        return Java::toLowerCase(
              reinterpret_cast<uint16_t*>(result), resultLength,
              reinterpret_cast<const uint16_t*>(src), srcLength, error);
    }

    inline UChar32 toUpper(UChar32 c) {
      return USE_FAST_PATH(c, WTF::toASCIIUpper, Java::toUpperCase);
    }

    inline int toUpper(UChar* result, int resultLength,
                       const UChar* src, int srcLength, bool* error) {
        return Java::toUpperCase(
              reinterpret_cast<uint16_t*>(result), resultLength,
              reinterpret_cast<const uint16_t*>(src), srcLength, error);
    }

    // Java doesn't support "true" case folding.
    inline UChar32 foldCase(UChar32 c) {
        return toLower(toUpper(c));
    }

    inline int foldCase(UChar* result, int resultLength,
                        const UChar* src, int srcLength, bool* error) {
        return Java::foldCase(
              reinterpret_cast<uint16_t*>(result), resultLength,
              reinterpret_cast<const uint16_t*>(src), srcLength, error);
    }

    inline UChar32 toTitleCase(UChar32 c) {
        return Java::toTitleCase(c);
    }

    inline CharCategory category(UChar32 c) {
        return static_cast<CharCategory>(U_MASK(Java::getType(c)));
    }

    inline bool isFormatChar(UChar32 c) {
        return CHECK_PROPERTY(c, Other_Format, 1);
    }

    inline bool isPrintableChar(UChar32 c) {
        return CHECK_PROPERTY(c, Other_NotAssigned | Other_Control, 0);
    }

    inline bool isSeparatorSpace(UChar32 c) {
        return CHECK_PROPERTY(c, Separator_Space, 1);
    }

    inline bool isPunct(UChar32 c) {
        return CHECK_PROPERTY(c,
                              Punctuation_Connector |
                              Punctuation_Dash |
                              Punctuation_Open |
                              Punctuation_Close |
                              Punctuation_InitialQuote |
                              Punctuation_FinalQuote |
                              Punctuation_Other,
                              1);
    }

    inline bool isDigit(UChar32 c) {
        return CHECK_PROPERTY(c, Number_DecimalDigit, 1);
    }

    inline bool isLower(UChar32 c) {
        return CHECK_PROPERTY(c, Letter_Lowercase, 1);
    }

    inline bool isUpper(UChar32 c) {
        return CHECK_PROPERTY(c, Letter_Uppercase, 1);
    }

    inline int digitValue(UChar32 c) {
        return Java::getNumericValue(c);
    }

    inline Direction direction(UChar32 c) {
        return static_cast<Direction>(Java::getDirectionality(c));
    }

    inline int umemcasecmp(const UChar* a, const UChar* b, int len) {
        for (int i = 0; i < len; i++) {
            UChar32 c1 = foldCase(a[i]);
            UChar32 c2 = foldCase(b[i]);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return 0;
    }

    inline bool isAlphanumeric(UChar32 c) {
        return Java::isLetterOrDigit(c);
    }

    // The rest is not provided by Java.
    UChar32 mirroredChar(UChar32 c);

    inline uint8_t combiningClass(UChar32) {
        return 0;
    }

    inline DecompositionType decompositionType(UChar32) {
        return DecompositionNone;
    }

    inline bool hasLineBreakingPropertyComplexContext(UChar32 c) {
        return false;
    }

    inline bool isArabicChar(UChar32 c)
    {
        return c >= 0x0600 && c <= 0x06FF;
    }

  }
}

#endif // JSC_UNICODE_JAVA_H
