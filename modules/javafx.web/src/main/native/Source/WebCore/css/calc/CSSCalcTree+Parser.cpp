/*
 * Copyright (C) 2024-2025 Samuel Weinig <sam@webkit.org>
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

#include "config.h"
#include "CSSCalcTree+Parser.h"

#include "AnchorPositionEvaluator.h"
#include "CSSCalcSymbolTable.h"
#include "CSSCalcTree+Serialization.h"
#include "CSSCalcTree+Simplification.h"
#include "CSSCalcTree.h"
#include "CSSParserContext.h"
#include "CSSParserIdioms.h"
#include "CSSParserTokenRange.h"
#include "CSSPropertyParserConsumer+Conditional.h"
#include "CSSPropertyParserConsumer+Ident.h"
#include "CSSPropertyParserConsumer+Primitives.h"
#include "CSSPropertyParsing.h"
#include "CSSSerializationContext.h"
#include "CSSUnits.h"
#include "CalculationCategory.h"
#include "CalculationOperator.h"
#include "ContainerQueryFeatures.h"
#include "ContainerQueryParser.h"
#include "Logging.h"
#include "MediaQueryFeatures.h"
#include "MediaQueryParser.h"
#include <wtf/SortedArrayMap.h>

namespace WebCore {
namespace CSSCalc {

// MARK: - Constants

static constexpr int maxExpressionDepth = 100;

static std::optional<std::pair<Number, Type>> lookupConstantNumber(CSSValueID symbol)
{
    static constexpr std::pair<CSSValueID, double> constantMappings[] {
        { CSSValueE,                     eDouble                                  },
        { CSSValuePi,                    piDouble                                 },
        { CSSValueInfinity,              std::numeric_limits<double>::infinity()  },
        { CSSValueNegativeInfinity, -1 * std::numeric_limits<double>::infinity()  },
        { CSSValueNaN,                   std::numeric_limits<double>::quiet_NaN() },
    };
    static constexpr SortedArrayMap constantMap { constantMappings };
    if (auto value = constantMap.tryGet(symbol))
        return std::make_pair(Number { .value = *value }, Type { });
    return std::nullopt;
}

// MARK: - Parser State

enum class ParseStatus { Ok, TooDeep };

struct ParserState {
    // CSSParserContext used to initiate the parse.
    const CSSParserContext& parserContext;

    // ParserOptions used to initiate the parse.
    const ParserOptions& parserOptions;

    // SimplificationOptions used to initiate the parse, if provided.
    const SimplificationOptions* simplificationOptions;

    // Tracks whether the parse tree contains any non-canonical dimension units that require conversion data (e.g. em, vh, etc.).
    bool requiresConversionData = false;

    // Tracks whether the parse tree contains any nodes that disqualify the tree from style sharing.
    bool unique = false;
};

static ParseStatus checkDepth(int depth)
{
    if (depth > maxExpressionDepth)
        return ParseStatus::TooDeep;
    return ParseStatus::Ok;
}

// MARK: - Parser

struct TypedChild {
    Child child;
    Type type;
};

static std::optional<TypedChild> parseCalcFunction(CSSParserTokenRange&, CSSValueID functionID, int depth, ParserState&);
static std::optional<TypedChild> parseCalcSum(CSSParserTokenRange&, int depth, ParserState&);
static std::optional<TypedChild> parseCalcProduct(CSSParserTokenRange&, int depth, ParserState&);
static std::optional<TypedChild> parseCalcValue(CSSParserTokenRange&, int depth, ParserState&);
static std::optional<TypedChild> parseCalcKeyword(const CSSParserToken&, ParserState&);
static std::optional<TypedChild> parseCalcNumber(const CSSParserToken&, ParserState&);
static std::optional<TypedChild> parseCalcPercentage(const CSSParserToken&, ParserState&);
static std::optional<TypedChild> parseCalcDimension(const CSSParserToken&, ParserState&);

std::optional<Tree> parseAndSimplify(CSSParserTokenRange& range, const CSSParserContext& parserContext, const ParserOptions& parserOptions, const SimplificationOptions& simplificationOptions)
{
    auto function = range.peek().functionId();
    if (!isCalcFunction(function, parserContext))
        return std::nullopt;

    auto tokens = CSSPropertyParserHelpers::consumeFunction(range);

    LOG_WITH_STREAM(Calc, stream << "Starting top level parse/simplification of function " << nameLiteralForSerialization(function) << "(" << tokens.serialize() << ") with expected type " << parserOptions.category);

    // -- Parsing --

    ParserState state {
        .parserContext = parserContext,
        .parserOptions = parserOptions,
        .simplificationOptions = &simplificationOptions
    };

    auto root = parseCalcFunction(tokens, function, 0, state);

    if (!root || !tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed top level parse/simplification of function '" << nameLiteralForSerialization(function) << "'");
        return std::nullopt;
    }

    // -- Type Checking --

    if (!root->type.matches(parserOptions.category)) {
        LOG_WITH_STREAM(Calc, stream << "Failed top level parse/simplification due to type check for function '" << nameLiteralForSerialization(function) << "', type=" << root->type << ", expected category=" << parserOptions.category);

        return std::nullopt;
    }

    auto result = Tree {
        .root = WTFMove(root->child),
        .type = root->type,
        .stage = CSSCalc::Stage::Specified,
        .requiresConversionData = state.requiresConversionData,
        .unique = state.unique,
    };

    LOG_WITH_STREAM(Calc, stream << "Completed top level parse/simplification for function '" << nameLiteralForSerialization(function) << "': " << serializationForCSS(result, { parserOptions.range, CSS::defaultSerializationContext() }) << ", type: " << getType(result.root) << ", category=" << parserOptions.category << ", requires-conversion-data: " << result.requiresConversionData << ", unique: " << result.unique);

    return result;
}

bool isCalcFunction(CSSValueID functionId, const CSSParserContext&)
{
    switch (functionId) {
    case CSSValueCalc:
    case CSSValueWebkitCalc:
    case CSSValueMin:
    case CSSValueMax:
    case CSSValueClamp:
    case CSSValuePow:
    case CSSValueSqrt:
    case CSSValueHypot:
    case CSSValueSin:
    case CSSValueCos:
    case CSSValueTan:
    case CSSValueExp:
    case CSSValueLog:
    case CSSValueAsin:
    case CSSValueAcos:
    case CSSValueAtan:
    case CSSValueAtan2:
    case CSSValueAbs:
    case CSSValueSign:
    case CSSValueRound:
    case CSSValueMod:
    case CSSValueRem:
    case CSSValueProgress:
    case CSSValueMediaProgress:
    case CSSValueContainerProgress:
    case CSSValueRandom:
    case CSSValueAnchor:
    case CSSValueAnchorSize:
        return true;
    default:
        return false;
    }
    return false;
}


template<typename Op> static std::optional<TypedChild> consumeExactlyOneArgument(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    auto sum = parseCalcSum(tokens, depth, state);
    if (!sum) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument failed to parse");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return std::nullopt;
    }

    if (!validateType<Op::input>(sum->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument has invalid type: " << sum->type);
        return std::nullopt;
    }

    auto outputType = transformType<Op::output>(sum->type);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - output transform failed for type: " << sum->type);
        return std::nullopt;
    }

    Op op { WTFMove(sum->child) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

template<typename Op> static std::optional<TypedChild> consumeOneOrMoreArguments(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    std::optional<Type> mergedType;
    Vector<Child> children;

    bool requireComma = false;
    unsigned argumentCount = 0;

    while (!tokens.atEnd()) {
        tokens.consumeWhitespace();
        if (requireComma && !CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
            return std::nullopt;
        }

        auto sum = parseCalcSum(tokens, depth, state);
        if (!sum) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #" << argumentCount);
            return std::nullopt;
        }

        if (!validateType<Op::input>(sum->type)) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #" << argumentCount << " has invalid type: " << sum->type);
            return std::nullopt;
        }

        if (!mergedType)
            mergedType = sum->type;
        else {
            auto mergeResult = mergeTypes<Op::merge>(*mergedType, sum->type);
            if (!mergeResult) {
                LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #" << argumentCount << " failed to merge type with other arguments: existing type " << *mergedType << " & argument type " << sum->type);
                return std::nullopt;
            }
            mergedType = *mergeResult;
        }

        ++argumentCount;
        children.append(WTFMove(sum->child));
        requireComma = true;
    }

    if (argumentCount < 1) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - no arguments found");
        return std::nullopt;
    }

    auto outputType = transformType<Op::output>(*mergedType);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - output transform failed for type: " << *mergedType);
        return std::nullopt;
    }

    Op op { WTFMove(children) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

template<typename Op> static std::optional<TypedChild> consumeExactlyTwoArguments(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    auto sumA = parseCalcSum(tokens, depth, state);
    if (!sumA) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1");
        return std::nullopt;
    }

    if (!validateType<Op::input>(sumA->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #1 has invalid type: " << sumA->type);
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto sumB = parseCalcSum(tokens, depth, state);
    if (!sumB) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #2");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return std::nullopt;
    }

    if (!validateType<Op::input>(sumB->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #2 has invalid type:  " << sumB->type);
        return std::nullopt;
    }

    auto mergedType = mergeTypes<Op::merge>(sumA->type, sumB->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge type with other arguments: argument #1 type " << sumA->type << " & argument #2 type" << sumB->type);
        return std::nullopt;
    }

    auto outputType = transformType<Op::output>(*mergedType);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - output transform failed for type: " << *mergedType);
        return std::nullopt;
    }

    Op op { WTFMove(sumA->child), WTFMove(sumB->child) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

template<typename Op> static std::optional<TypedChild> consumeOneOrTwoArguments(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    auto sumA = parseCalcSum(tokens, depth, state);
    if (!sumA) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1");
        return std::nullopt;
    }

    if (!validateType<Op::input>(sumA->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #1 has invalid type: " << sumA->type);
        return std::nullopt;
    }

    if (tokens.atEnd()) {
        auto outputType = transformType<Op::output>(sumA->type);
        if (!outputType) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' (one argument) function - output transform failed for type: " << sumA->type);
            return std::nullopt;
        }

        Op op { WTFMove(sumA->child), std::nullopt };

        if (auto* simplificationOptions = state.simplificationOptions) {
            if (auto replacement = simplify(op, *simplificationOptions))
                return TypedChild { WTFMove(*replacement), *outputType };
        }
        return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto sumB = parseCalcSum(tokens, depth, state);
    if (!sumB) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' (two arguments) function - failed parse of argument #2");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' (two arguments) function - extraneous tokens found");
        return std::nullopt;
    }

    if (!validateType<Op::input>(sumB->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' (two arguments) function - argument #2 has invalid type: " << sumB->type);
        return std::nullopt;
    }

    auto mergedType = mergeTypes<Op::merge>(sumA->type, sumB->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' (two arguments) function - failed to merge type with other arguments: argument #1 type " << sumA->type << " & argument #2 type" << sumB->type);
        return std::nullopt;
    }

    auto outputType = transformType<Op::output>(*mergedType);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' (two arguments) function - output transform failed for type: " << *mergedType);
        return std::nullopt;
    }

    Op op { WTFMove(sumA->child), WTFMove(sumB->child) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

static std::optional<TypedChild> consumeClamp(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <clamp()> = clamp( [ <calc-sum> | none ], <calc-sum>, [ <calc-sum> | none ] )

    using Op = Clamp;

    struct TypedChildOrNone {
        ChildOrNone child;
        Type type;
    };
    auto parseCalcSumOrNone = [](auto& tokens, auto depth, auto& state) -> std::optional<TypedChildOrNone> {
        if (tokens.peek().id() == CSSValueNone) {
            tokens.consume();
            return TypedChildOrNone { ChildOrNone { CSS::Keyword::None { } }, Type { } };
        }
        auto sum = parseCalcSum(tokens, depth, state);
        if (!sum)
            return std::nullopt;

        return TypedChildOrNone { ChildOrNone { WTFMove(sum->child) }, sum->type };
    };

    auto min = parseCalcSumOrNone(tokens, depth, state);
    if (!min) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument 'min' failed to parse");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma after argument 'min'");
        return std::nullopt;
    }

    auto val = parseCalcSum(tokens, depth, state);
    if (!val) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument 'val' failed to parse");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma after argument 'val'");
        return std::nullopt;
    }

    auto max = parseCalcSumOrNone(tokens, depth, state);
    if (!max) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument 'max' failed to parse");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return std::nullopt;
    }

    auto computeType = [&] -> std::optional<Type> {
        bool minIsNone = WTF::holdsAlternative<CSS::Keyword::None>(min->child);
        bool maxIsNone = WTF::holdsAlternative<CSS::Keyword::None>(max->child);

        if (minIsNone && maxIsNone)
            return val->type;

        if (minIsNone) {
            auto valAndMaxType = mergeTypes<MergePolicy::Consistent>(val->type, max->type);
            if (!valAndMaxType) {
                LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge argument 'val' type " << val->type << " & argument 'max' type " << max->type);
                return std::nullopt;
            }
            return *valAndMaxType;
        }

        if (maxIsNone) {
            auto minAndValType = mergeTypes<Op::merge>(min->type, val->type);
            if (!minAndValType) {
                LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge argument 'min' type " << min->type << " & argument 'val' type " << val->type);
                return std::nullopt;
            }
            return *minAndValType;
        }

        auto minAndValType = mergeTypes<Op::merge>(min->type, val->type);
        if (!minAndValType) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge argument 'min' type " << min->type << " & argument 'val' type " << val->type);
            return std::nullopt;
        }
        auto minAndValAndMaxType = mergeTypes<Op::merge>(*minAndValType, max->type);
        if (!minAndValAndMaxType) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge already merged type " << *minAndValAndMaxType << " & argument 'max' type " << max->type);
            return std::nullopt;
        }
        return *minAndValAndMaxType;
    };

    auto outputType = computeType();
    if (!outputType)
        return std::nullopt;

    Op op { WTFMove(min->child), WTFMove(val->child), WTFMove(max->child) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

template<typename Op> static std::optional<TypedChild> consumeRoundArguments(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    auto sumA = parseCalcSum(tokens, depth, state);
    if (!sumA) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' function - failed parse of argument #1");
        return std::nullopt;
    }

    if (tokens.atEnd()) {
        if (!validateType<AllowedTypes::Number>(sumA->type)) {
            LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' function - argument #1 has invalid type: " << sumA->type);
            return std::nullopt;
        }

        auto outputType = transformType<Op::output>(sumA->type);
        if (!outputType) {
            LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' (one argument) function - output transform failed for type: " << sumA->type);
            return std::nullopt;
        }

        Op op { WTFMove(sumA->child), std::nullopt };

        if (auto* simplificationOptions = state.simplificationOptions) {
            if (auto replacement = simplify(op, *simplificationOptions))
                return TypedChild { WTFMove(*replacement), *outputType };
        }
        return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' function - missing comma");
        return std::nullopt;
    }

    auto sumB = parseCalcSum(tokens, depth, state);
    if (!sumB) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' (two arguments) function - failed parse of argument #2");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' (two arguments) function - extraneous tokens found");
        return std::nullopt;
    }

    auto mergedType = mergeTypes<Op::merge>(sumA->type, sumB->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' (two arguments) function - failed to merge type with other arguments: argument #1 type " << sumA->type << " & argument #2 type" << sumB->type);
        return std::nullopt;
    }

    auto outputType = transformType<Op::output>(*mergedType);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(Op::id) << ")' (two arguments) function - output transform failed for type: " << *mergedType);
        return std::nullopt;
    }

    Op op { WTFMove(sumA->child), WTFMove(sumB->child) };

    LOG_WITH_STREAM(Calc, stream << "Succeeded 'round(" << nameLiteralForSerialization(Op::id) << ")' (two arguments) function: type is " << *outputType);

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }

    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

static std::optional<TypedChild> consumeRound(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <round()> = round( <rounding-strategy>?, <calc-sum>, <calc-sum>? )

    auto roundingStrategy = CSSPropertyParserHelpers::consumeIdentRaw<CSSValueNearest, CSSValueToZero, CSSValueUp, CSSValueDown>(tokens);
    if (!roundingStrategy)
        return consumeRoundArguments<RoundNearest>(tokens, depth, state);

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed 'round(" << nameLiteralForSerialization(*roundingStrategy) << ") function - missing comma after <rounding-strategy>");
        return std::nullopt;
    }

    switch (*roundingStrategy) {
    case CSSValueNearest:
        return consumeRoundArguments<RoundNearest>(tokens, depth, state);
    case CSSValueToZero:
        return consumeRoundArguments<RoundToZero>(tokens, depth, state);
    case CSSValueUp:
        return consumeRoundArguments<RoundUp>(tokens, depth, state);
    case CSSValueDown:
        return consumeRoundArguments<RoundDown>(tokens, depth, state);
    default:
        break;
    }

    return std::nullopt;
}

static std::optional<Random::CachingOptions> consumeOptionalRandomCachingOptions(CSSParserTokenRange& tokens)
{
    // <random-caching-options> = <dashed-ident> || per-element

    std::optional<AtomString> identifier;
    std::optional<CSS::Keyword::PerElement> perElement;

    auto consumeIdentifier = [&] -> bool {
        if (identifier || tokens.peek().type() != IdentToken || !isValidCustomIdentifier(tokens.peek().id()) || !tokens.peek().value().startsWith("--"_s))
            return false;
        identifier = tokens.consumeIncludingWhitespace().value().toAtomString();
        return true;
    };
    auto consumePerElement = [&] -> bool {
        if (perElement || tokens.peek().id() != CSSValuePerElement)
            return false;
        tokens.consumeIncludingWhitespace();
        perElement = CSS::Keyword::PerElement { };
        return true;
    };

    for (unsigned i = 0; i < 2; ++i) {
        if (consumeIdentifier() || consumePerElement())
            continue;
        break;
    }

    if (!identifier && !perElement)
        return { };

    return Random::CachingOptions {
        .identifier = identifier.value_or(AtomString { }),
        .perElement = perElement.has_value()
    };
}

static std::optional<TypedChild> consumeRandom(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <random()> = random( <random-caching-options>? , <calc-sum>, <calc-sum>, [by <calc-sum>]? )

    if (!state.parserContext.cssRandomFunctionEnabled)
        return { };

    using Op = Random;

    Random::CachingOptions cachingOptions;
    if (auto optionalCachingOptions = consumeOptionalRandomCachingOptions(tokens)) {
        if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma after <random-caching-options>");
            return { };
        }

        cachingOptions = WTFMove(*optionalCachingOptions);
    }

    auto min = parseCalcSum(tokens, depth, state);
    if (!min) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument `min`");
        return { };
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma after argument `min`");
        return { };
    }

    auto max = parseCalcSum(tokens, depth, state);
    if (!max) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument `max`");
        return { };
    }

    if (tokens.atEnd()) {
        // - Validate arguments

        if (!validateType<Op::input>(min->type)) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument `min` has invalid type: " << min->type);
            return { };
        }

        if (!validateType<Op::input>(max->type)) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument `max` has invalid type: " << max->type);
            return { };
        }

        // - Merge arguments

        auto mergedType = mergeTypes<Op::merge>(min->type, max->type);
        if (!mergedType) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge types");
            return { };
        }

        auto outputType = transformType<Op::output>(*mergedType);
        if (!outputType) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - output transform failed for type: " << *mergedType);
            return { };
        }

        state.requiresConversionData = true;
        if (cachingOptions.perElement)
            state.unique = true;

        Op op { WTFMove(cachingOptions), WTFMove(min->child), WTFMove(max->child), std::nullopt };

        if (auto* simplificationOptions = state.simplificationOptions) {
            if (auto replacement = simplify(op, *simplificationOptions))
                return TypedChild { WTFMove(*replacement), *outputType };
        }

        return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma after argument `max`");
        return { };
    }

    if (!CSSPropertyParserHelpers::consumeIdentRaw<CSSValueBy>(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing literal 'by'");
        return { };
    }

    auto step = parseCalcSum(tokens, depth, state);
    if (!step) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument `step`");
        return { };
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return { };
    }

    // - Validate arguments

    if (!validateType<Op::input>(step->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument `step` has invalid type: " << step->type);
        return { };
    }

    // - Merge arguments

    auto mergedType = mergeTypes<Op::merge>(min->type, max->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge types");
        return { };
    }

    mergedType = mergeTypes<Op::merge>(*mergedType, step->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge types");
        return { };
    }

    auto outputType = transformType<Op::output>(*mergedType);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - output transform failed for type: " << *mergedType);
        return { };
    }

    state.requiresConversionData = true;
    if (cachingOptions.perElement)
        state.unique = true;

    Op op { WTFMove(cachingOptions), WTFMove(min->child), WTFMove(max->child), WTFMove(step->child) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

static std::optional<TypedChild> consumeProgress(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <progress()> = progress( <calc-sum>, <calc-sum>, <calc-sum> )

    if (!state.parserContext.cssProgressFunctionEnabled)
        return { };

    using Op = Progress;

    auto value = parseCalcSum(tokens, depth, state);
    if (!value) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto start = parseCalcSum(tokens, depth, state);
    if (!start) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #2");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto end = parseCalcSum(tokens, depth, state);
    if (!end) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #3");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return std::nullopt;
    }

    // - Validate arguments

    if (!validateType<Op::input>(value->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #1 has invalid type: " << value->type);
        return std::nullopt;
    }

    if (!validateType<Op::input>(start->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #2 has invalid type: " << start->type);
        return std::nullopt;
    }

    if (!validateType<Op::input>(end->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - argument #3 has invalid type: " << end->type);
        return std::nullopt;
    }

    // - Merge arguments

    auto mergedType = mergeTypes<Op::merge>(value->type, start->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge types: argument #1 type " << value->type << ", argument #2 type" << start->type << ", argument #3 type" << end->type);
        return std::nullopt;
    }

    mergedType = mergeTypes<Op::merge>(*mergedType, end->type);
    if (!mergedType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed to merge types: argument #1 type " << value->type << ", argument #2 type" << start->type << ", argument #3 type" << end->type);
        return std::nullopt;
    }

    auto outputType = transformType<Op::output>(*mergedType);
    if (!outputType) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - output transform failed for type: " << *mergedType);
        return std::nullopt;
    }

    Op op { WTFMove(value->child), WTFMove(start->child), WTFMove(end->child) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), *outputType };
    }
    return TypedChild { makeChild(WTFMove(op), *outputType), *outputType };
}

static std::optional<AtomString> consumeMediaFeatureName(CSSParserTokenRange& tokens)
{
    if (tokens.peek().type() != IdentToken)
        return std::nullopt;
    return AtomString { tokens.consumeIncludingWhitespace().value().convertToASCIILowercase() };
}

static std::optional<TypedChild> consumeMediaProgress(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <media-progress()> = media-progress( <mf-name>, <calc-sum>, <calc-sum> )

    if (!state.parserContext.cssMediaProgressFunctionEnabled)
        return { };

    using Op = MediaProgress;

    auto featureName = consumeMediaFeatureName(tokens);
    if (!featureName) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1");
        return std::nullopt;
    }

    auto* schema = MQ::MediaQueryParser::mediaProgressProvidingSchemaForFeatureName(*featureName, state.parserContext);
    if (!schema) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1 - invalid media feature");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto schemaCategory = schema->category();

    ParserState nestedState {
        .parserContext = state.parserContext,
        .parserOptions = ParserOptions {
            .category = schemaCategory,
            .range = CSS::All,
            .allowedSymbols = { },
            .propertyOptions = { }
        },
        .simplificationOptions = nullptr
    };
    SimplificationOptions nestedSimplificationOptions = {
        .category = schemaCategory,
        .range = CSS::All,
        .conversionData = state.simplificationOptions->conversionData,
        .symbolTable = state.simplificationOptions->symbolTable,
        .allowZeroValueLengthRemovalFromSum = state.simplificationOptions->allowZeroValueLengthRemovalFromSum,
    };
    if (state.simplificationOptions)
        nestedState.simplificationOptions = &nestedSimplificationOptions;

    auto start = parseCalcSum(tokens, depth, nestedState);
    if (!start) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #2");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto end = parseCalcSum(tokens, depth, nestedState);
    if (!end) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #3");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return std::nullopt;
    }

    // - Validate arguments

    if (!Type::consistentType(start->type, end->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - inconsistent types");
        return std::nullopt;
    }

    if (!start->type.matches(schemaCategory)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - inconsistent types");
        return std::nullopt;
    }

    // `media-progress() always evaluates to a <number>.
    auto outputType = Type { };

    Op op { schema, WTFMove(start->child), WTFMove(end->child) };

    if (auto* simplificationOptions = nestedState.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), outputType };
    }
    return TypedChild { makeChild(WTFMove(op), outputType), outputType };
}

static std::optional<TypedChild> consumeContainerProgress(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <container-progress()> = container-progress( <mf-name> [ of <container-name> ]?, <calc-sum>, <calc-sum> )

    if (!state.parserContext.cssContainerProgressFunctionEnabled)
        return { };

    using Op = ContainerProgress;

    auto featureName = consumeMediaFeatureName(tokens);
    if (!featureName) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1");
        return std::nullopt;
    }

    AtomString container;
    if (CSSPropertyParserHelpers::consumeIdentRaw<CSSValueOf>(tokens)) {
        if (tokens.peek().type() != IdentToken || !isValidCustomIdentifier(tokens.peek().id()) || !CSSPropertyParserHelpers::isValidContainerNameIdentifier(tokens.peek().id())) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1");
            return std::nullopt;
        }
        container = tokens.consumeIncludingWhitespace().value().toAtomString();
    }

    auto* schema = CQ::ContainerQueryParser::containerProgressProvidingSchemaForFeatureName(*featureName, state.parserContext);
    if (!schema) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #1 - invalid media feature");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto schemaCategory = schema->category();

    ParserState nestedState {
        .parserContext = state.parserContext,
        .parserOptions = ParserOptions {
            .category = schemaCategory,
            .range = CSS::All,
            .allowedSymbols = { },
            .propertyOptions = { }
        },
        .simplificationOptions = nullptr
    };
    SimplificationOptions nestedSimplificationOptions = {
        .category = schemaCategory,
        .range = CSS::All,
        .conversionData = state.simplificationOptions->conversionData,
        .symbolTable = state.simplificationOptions->symbolTable,
        .allowZeroValueLengthRemovalFromSum = state.simplificationOptions->allowZeroValueLengthRemovalFromSum,
    };
    if (state.simplificationOptions)
        nestedState.simplificationOptions = &nestedSimplificationOptions;

    auto start = parseCalcSum(tokens, depth, nestedState);
    if (!start) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #2");
        return std::nullopt;
    }

    if (!CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - missing comma");
        return std::nullopt;
    }

    auto end = parseCalcSum(tokens, depth, nestedState);
    if (!end) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - failed parse of argument #3");
        return std::nullopt;
    }

    if (!tokens.atEnd()) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - extraneous tokens found");
        return std::nullopt;
    }

    // - Validate arguments

    if (!Type::consistentType(start->type, end->type)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - inconsistent types");
        return std::nullopt;
    }

    if (!start->type.matches(schemaCategory)) {
        LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(Op::id) << "' function - inconsistent types");
        return std::nullopt;
    }

    // `container-progress() always evaluates to a <number>.
    auto outputType = Type { };

    Op op { schema, WTFMove(container), WTFMove(start->child), WTFMove(end->child) };

    if (auto* simplificationOptions = nestedState.simplificationOptions) {
        if (auto replacement = simplify(op, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), outputType };
    }
    return TypedChild { makeChild(WTFMove(op), outputType), outputType };
}

static std::optional<TypedChild> consumeValueWithoutSimplifyingCalc(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // Complex arguments need to be surrounded by a math function.
    if (tokens.peek().type() == LeftParenthesisToken)
        return { };

    auto isFunction = !!tokens.peek().functionId();

    auto typedValue = parseCalcValue(tokens, depth, state);
    if (!typedValue)
        return { };

    auto isLeafValue = WTF::switchOn(typedValue->child,
        [](Leaf auto&) { return true; },
        [](auto&) { return false; }
    );

    if (isFunction && isLeafValue) {
        // Wrap in Sum to keep top level calc() function in serialization.
        Vector<Child> children;
        children.append(WTFMove(typedValue->child));

        return TypedChild { makeChild(Sum { WTFMove(children) }, typedValue->type), typedValue->type };
    }

    return typedValue;
}

static std::optional<TypedChild> consumeAnchor(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <anchor()> = anchor( <anchor-element>? && <anchor-side>, <length-percentage>? )

    if (state.parserOptions.propertyOptions.anchorPolicy != AnchorPolicy::Allow)
        return { };

    if (!state.parserContext.propertySettings.cssAnchorPositioningEnabled)
        return { };

    auto anchorElement = CSSPropertyParserHelpers::consumeDashedIdentRaw(tokens);

    // <anchor-side> = inside | outside | top | left | right | bottom | start | end | self-start | self-end | <percentage> | center
    auto anchorSide = [&]() -> std::optional<AnchorSide> {
        auto sideIdent = CSSPropertyParserHelpers::consumeIdentRaw<CSSValueInside, CSSValueOutside, CSSValueTop, CSSValueLeft, CSSValueRight, CSSValueBottom, CSSValueStart, CSSValueEnd, CSSValueSelfStart, CSSValueSelfEnd, CSSValueCenter>(tokens);
        if (sideIdent)
            return AnchorSide { *sideIdent };

        auto percentageOptions = ParserOptions {
            .category = Calculation::Category::Percentage,
            .range = CSS::All,
            .allowedSymbols = { },
            .propertyOptions = { },
        };
        auto percentageState = ParserState {
            .parserContext = state.parserContext,
            .parserOptions = percentageOptions,
            .simplificationOptions = { },
        };

        auto percentage = consumeValueWithoutSimplifyingCalc(tokens, depth, percentageState);
        if (!percentage)
            return { };

        auto category = percentage->type.calculationCategory();
        if (!category || category != Calculation::Category::Percentage)
            return { };

        return AnchorSide { WTFMove(percentage->child) };
    }();

    if (!anchorSide)
        return { };

    if (anchorElement.isNull())
        anchorElement = CSSPropertyParserHelpers::consumeDashedIdentRaw(tokens);

    auto type = Type::makeLength();
    std::optional<Child> fallback;

    if (CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
        auto typedFallback = consumeValueWithoutSimplifyingCalc(tokens, depth, state);
        if (!typedFallback)
            return { };

        auto category = typedFallback->type.calculationCategory();
        if (!category)
            return { };
        if (*category != Calculation::Category::Length && *category != Calculation::Category::LengthPercentage)
            return { };

        fallback = WTFMove(typedFallback->child);
        type.percentHint = Type::determinePercentHint(*category);
    }

    state.requiresConversionData = true;

    auto anchor = Anchor {
        .elementName = AtomString { WTFMove(anchorElement) },
        .side = WTFMove(*anchorSide),
        .fallback = WTFMove(fallback)
    };

    return TypedChild { makeChild(WTFMove(anchor), type), type };
}

static std::optional<Style::AnchorSizeDimension> cssValueIDToAnchorSizeDimension(CSSValueID value)
{
    switch (value) {
    case CSSValueWidth:
        return Style::AnchorSizeDimension::Width;
    case CSSValueHeight:
        return Style::AnchorSizeDimension::Height;
    case CSSValueBlock:
        return Style::AnchorSizeDimension::Block;
    case CSSValueInline:
        return Style::AnchorSizeDimension::Inline;
    case CSSValueSelfBlock:
        return Style::AnchorSizeDimension::SelfBlock;
    case CSSValueSelfInline:
        return Style::AnchorSizeDimension::SelfInline;
    default:
        return { };
    }
}

static std::optional<TypedChild> consumeAnchorSize(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // anchor-size() = anchor-size( [ <anchor-element> || <anchor-size> ]? , <length-percentage>? )
    // <anchor-element> = <dashed-ident>
    // <anchor-size> = width | height | block | inline | self-block | self-inline

    if (state.parserOptions.propertyOptions.anchorSizePolicy != AnchorSizePolicy::Allow)
        return { };

    if (!state.parserContext.propertySettings.cssAnchorPositioningEnabled)
        return { };

    // parse <anchor-element>
    auto maybeAnchorElement = CSSPropertyParserHelpers::consumeDashedIdentRaw(tokens);

    // then parse <anchor-size>
    auto maybeAnchorSize = CSSPropertyParserHelpers::consumeIdentRaw<CSSValueWidth, CSSValueHeight, CSSValueBlock, CSSValueInline, CSSValueSelfBlock, CSSValueSelfInline>(tokens);

    // if we could parse <anchor-size> but not <anchor-element>, it's possible <anchor-element> is specified
    // after <anchor-size>, so re-parse <anchor-element>
    if (maybeAnchorSize && maybeAnchorElement.isNull())
        maybeAnchorElement = CSSPropertyParserHelpers::consumeDashedIdentRaw(tokens);

    std::optional<TypedChild> fallback;

    // if either <anchor-element> or <anchor-size> is present
    if (maybeAnchorSize || !maybeAnchorElement.isNull()) {
        // if a comma follows...
        if (CSSPropertyParserHelpers::consumeCommaIncludingWhitespace(tokens)) {
            // it must be followed by the fallback value.
            fallback = consumeValueWithoutSimplifyingCalc(tokens, depth, state);
            if (!fallback)
                return { };
        }
        // if a comma does not follow, then there's no fallback value.
    } else {
        // if <anchor-element> and <anchor-size> is not present
        // then an optional fallback value follows
        fallback = consumeValueWithoutSimplifyingCalc(tokens, depth, state);
    }

    auto type = Type::makeLength();

    // anchor-size() resolves to a <length> if it can be resolved, otherwise the fallback
    // value is resolved, which is of type <length-percentage>. Therefore the overall type
    // of anchor-size() is <length> or <length-percentage>, depending on the type of the
    // fallback value.
    if (fallback) {
        auto category = fallback->type.calculationCategory();
        if (!category)
            return { };
        if (*category != Calculation::Category::Length && *category != Calculation::Category::LengthPercentage)
            return { };

        type.percentHint = Type::determinePercentHint(*category);
    }

    state.requiresConversionData = true;

    auto anchorSize = AnchorSize {
        .elementName = AtomString { WTFMove(maybeAnchorElement) },
        .dimension = maybeAnchorSize ? cssValueIDToAnchorSizeDimension(*maybeAnchorSize) : std::nullopt,
        .fallback = fallback ? std::make_optional(WTFMove(fallback->child)) : std::nullopt
    };

    return TypedChild {
        .child = makeChild(WTFMove(anchorSize), type),
        .type = type
    };
}

std::optional<TypedChild> parseCalcFunction(CSSParserTokenRange& tokens, CSSValueID functionID, int depth, ParserState& state)
{
    if (checkDepth(depth) != ParseStatus::Ok)
        return std::nullopt;

    switch (functionID) {
    case CSSValueWebkitCalc:
    case CSSValueCalc:
        // <calc()>  = calc( <calc-sum> )
        return parseCalcSum(tokens, depth, state);

    case CSSValueMin:
        // <min()>   = min( <calc-sum># )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: consistent type
        return consumeOneOrMoreArguments<Min>(tokens, depth, state);

    case CSSValueMax:
        // <max()>   = max( <calc-sum># )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: consistent type
        return consumeOneOrMoreArguments<Max>(tokens, depth, state);

    case CSSValueClamp:
        // <clamp()> = clamp( [ <calc-sum> | none ], <calc-sum>, [ <calc-sum> | none ] )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: consistent type
        return consumeClamp(tokens, depth, state);

    case CSSValueRound:
        // <round()> = round( <rounding-strategy>?, <calc-sum>, <calc-sum>? )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: consistent type
        return consumeRound(tokens, depth, state);

    case CSSValueMod:
        // <mod()>   = mod( <calc-sum>, <calc-sum> )
        //     - INPUT: "same" <number>, <dimension>, or <percentage>
        //     - OUTPUT: same type
        return consumeExactlyTwoArguments<Mod>(tokens, depth, state);

    case CSSValueRem:
        // <rem()>   = rem( <calc-sum>, <calc-sum> )
        //     - INPUT: "same" <number>, <dimension>, or <percentage>
        //     - OUTPUT: same type
        return consumeExactlyTwoArguments<Rem>(tokens, depth, state);

    case CSSValueSin:
        // <sin()>   = sin( <calc-sum> )
        //     - INPUT: <number> or <angle>
        //     - OUTPUT: <number> "made consistent"
        return consumeExactlyOneArgument<Sin>(tokens, depth, state);

    case CSSValueCos:
        // <cos()>   = cos( <calc-sum> )
        //     - INPUT: <number> or <angle>
        //     - OUTPUT: <number> "made consistent"
        return consumeExactlyOneArgument<Cos>(tokens, depth, state);

    case CSSValueTan:
        // <tan()>   = tan( <calc-sum> )
        //     - INPUT: <number> or <angle>
        //     - OUTPUT: <number> "made consistent"
        return consumeExactlyOneArgument<Tan>(tokens, depth, state);

    case CSSValueAsin:
        // <asin()>  = asin( <calc-sum> )
        //     - INPUT: <number>
        //     - OUTPUT: <angle> "made consistent"
        return consumeExactlyOneArgument<Asin>(tokens, depth, state);

    case CSSValueAcos:
        // <acos()>  = acos( <calc-sum> )
        //     - INPUT: <number>
        //     - OUTPUT: <angle> "made consistent"
        return consumeExactlyOneArgument<Acos>(tokens, depth, state);

    case CSSValueAtan:
        // <atan()>  = atan( <calc-sum> )
        //     - INPUT: <number>
        //     - OUTPUT: <angle> "made consistent"
        return consumeExactlyOneArgument<Atan>(tokens, depth, state);

    case CSSValueAtan2:
        // <atan2()> = atan2( <calc-sum>, <calc-sum> )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: <angle> "made consistent"
        return consumeExactlyTwoArguments<Atan2>(tokens, depth, state);

    case CSSValuePow:
        // <pow()>   = pow( <calc-sum>, <calc-sum> )
        //     - INPUT: "consistent" <number>
        //     - OUTPUT: consistent type
        return consumeExactlyTwoArguments<Pow>(tokens, depth, state);

    case CSSValueSqrt:
        // <sqrt()>  = sqrt( <calc-sum> )
        //     - INPUT: <number>
        //     - OUTPUT: <number> "made consistent"
        return consumeExactlyOneArgument<Sqrt>(tokens, depth, state);

    case CSSValueHypot:
        // <hypot()> = hypot( <calc-sum># )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: consistent type
        return consumeOneOrMoreArguments<Hypot>(tokens, depth, state);

    case CSSValueLog:
        // <log()>   = log( <calc-sum>, <calc-sum>? )
        //     - INPUT: <number>
        //     - OUTPUT: <number> "made consistent"
        return consumeOneOrTwoArguments<Log>(tokens, depth, state);

    case CSSValueExp:
        // <exp()>   = exp( <calc-sum> )
        //     - INPUT: <number>
        //     - OUTPUT: <number> "made consistent"
        return consumeExactlyOneArgument<Exp>(tokens, depth, state);

    case CSSValueAbs:
        // <abs()>   = abs( <calc-sum> )
        //     - INPUT: any
        //     - OUTPUT: input type
        return consumeExactlyOneArgument<Abs>(tokens, depth, state);

    case CSSValueSign:
        // <sign()>  = sign( <calc-sum> )
        //     - INPUT: any
        //     - OUTPUT: <number> "made consistent"
        return consumeExactlyOneArgument<Sign>(tokens, depth, state);

    case CSSValueRandom:
        // <random()> = random( <random-caching-options>? , <calc-sum>, <calc-sum>, [by <calc-sum>]? )
        //     - INPUT: "same" <number>, <dimension>, or <percentage>
        //     - OUTPUT: same type
        return consumeRandom(tokens, depth, state);

    case CSSValueProgress:
        // <progress()> = progress( <calc-sum>, <calc-sum>, <calc-sum> )
        //     - INPUT: "consistent" <number>, <dimension>, or <percentage>
        //     - OUTPUT: <number> "made consistent"
        return consumeProgress(tokens, depth, state);

    case CSSValueMediaProgress:
        // <media-progress()> = media-progress( <mf-name>, <calc-sum>, <calc-sum> )
        //     - INPUT: dependent on type of <mf-name> feature.
        //     - OUTPUT: <number>
        return consumeMediaProgress(tokens, depth, state);

    case CSSValueContainerProgress:
        // <container-progress()> = container-progress( <mf-name> [ of <container-name> ]?, <calc-sum>, <calc-sum> )
        //     - INPUT: dependent on type of <mf-name> feature.
        //     - OUTPUT: <number>
        return consumeContainerProgress(tokens, depth, state);

    case CSSValueAnchor:
        return consumeAnchor(tokens, depth, state);

    case CSSValueAnchorSize:
        return consumeAnchorSize(tokens, depth, state);

    default:
        break;
    }

    return std::nullopt;
}

std::optional<TypedChild> parseCalcSum(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <calc-sum> = <calc-product> [ [ '+' | '-' ] <calc-product> ]*

    if (checkDepth(depth) != ParseStatus::Ok)
        return std::nullopt;

    auto originalTokens = tokens.span();

    auto firstValue = parseCalcProduct(tokens, depth, state);
    if (!firstValue)
        return std::nullopt;

    auto sumType = firstValue->type;
    Vector<Child> children;

    while (!tokens.atEnd()) {
        auto& token = tokens.peek();
        char operatorCharacter = token.type() == DelimiterToken ? token.delimiter() : 0;
        if (operatorCharacter != static_cast<char>(Calculation::Operator::Sum) && operatorCharacter != static_cast<char>(Calculation::Operator::Negate))
            break;

        auto previousToken = originalTokens[tokens.begin() - originalTokens.data() - 1];
        if (!CSSTokenizer::isWhitespace(previousToken.type()))
            return std::nullopt; // calc(1px+ 2px) is invalid

        tokens.consume();
        if (!CSSTokenizer::isWhitespace(tokens.peek().type()))
            return std::nullopt; // calc(1px +2px) is invalid

        tokens.consumeIncludingWhitespace();

        auto nextValue = parseCalcProduct(tokens, depth, state);
        if (!nextValue)
            return std::nullopt;

        if (operatorCharacter == static_cast<char>(Calculation::Operator::Negate)) {
            auto negate = [](TypedChild& next, ParserState& state) -> std::optional<TypedChild> {
                Negate negate { WTFMove(next.child) };
                auto negateType = next.type;

                if (auto* simplificationOptions = state.simplificationOptions) {
                    if (auto replacement = simplify(negate, *simplificationOptions))
                        return TypedChild { WTFMove(*replacement), negateType };
                }
                return TypedChild { makeChild(WTFMove(negate), negateType), negateType };
            };

            nextValue = negate(*nextValue, state);
        }

        if (firstValue) {
            children.append(WTFMove(firstValue->child));
            firstValue = std::nullopt;
        }

        auto newType = Type::add(sumType, nextValue->type);
        if (!newType)
            return std::nullopt;

        sumType = *newType;
        children.append(WTFMove(nextValue->child));
    }

    if (children.isEmpty())
        return firstValue;

    Sum sum { WTFMove(children) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(sum, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), sumType };
    }

    return TypedChild { makeChild(WTFMove(sum), sumType), sumType };
}

std::optional<TypedChild> parseCalcProduct(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <calc-product> = <calc-value> [ [ '*' | '/' ] <calc-value> ]*

    if (checkDepth(depth) != ParseStatus::Ok)
        return std::nullopt;

    auto firstValue = parseCalcValue(tokens, depth, state);
    if (!firstValue)
        return std::nullopt;

    auto productType = firstValue->type;
    Vector<Child> children;

    while (!tokens.atEnd()) {
        auto& token = tokens.peek();
        char operatorCharacter = token.type() == DelimiterToken ? token.delimiter() : 0;
        if (operatorCharacter != static_cast<char>(Calculation::Operator::Product) && operatorCharacter != static_cast<char>(Calculation::Operator::Invert))
            break;
        tokens.consumeIncludingWhitespace();

        auto nextValue = parseCalcValue(tokens, depth, state);
        if (!nextValue)
            return std::nullopt;

        if (operatorCharacter == static_cast<char>(Calculation::Operator::Invert)) {
            auto invert = [](TypedChild& next, ParserState& state) -> std::optional<TypedChild> {
                Invert invert { WTFMove(next.child) };
                auto invertType = Type::invert(next.type);

                if (auto* simplificationOptions = state.simplificationOptions) {
                    if (auto replacement = simplify(invert, *simplificationOptions))
                        return TypedChild { WTFMove(*replacement), invertType };
                }
                return TypedChild { makeChild(WTFMove(invert), invertType), invertType };
            };

            nextValue = invert(*nextValue, state);
        }

        if (firstValue) {
            children.append(WTFMove(firstValue->child));
            firstValue = std::nullopt;
        }

        auto newType = Type::multiply(productType, nextValue->type);
        if (!newType)
            return std::nullopt;

        productType = *newType;
        children.append(WTFMove(nextValue->child));
    }

    if (children.isEmpty())
        return firstValue;

    Product product { WTFMove(children) };

    if (auto* simplificationOptions = state.simplificationOptions) {
        if (auto replacement = simplify(product, *simplificationOptions))
            return TypedChild { WTFMove(*replacement), productType };
    }
    return TypedChild { makeChild(WTFMove(product), productType), productType };
}

std::optional<TypedChild> parseCalcValue(CSSParserTokenRange& tokens, int depth, ParserState& state)
{
    // <calc-value> = <number> | <dimension> | <percentage> | <calc-keyword> | ( <calc-sum> )
    // <calc-keyword> = e | pi | infinity | -infinity | NaN
    //
    // NOTE: <calc-keyword> is extended for identifiers specified via CSSCalcSymbolsAllowed.

    if (checkDepth(depth) != ParseStatus::Ok)
        return std::nullopt;

    auto findBlock = [&](auto& tokens) -> std::optional<CSSValueID> {
        if (tokens.peek().type() == LeftParenthesisToken) {
            // Simple blocks (e.g. parenthesis around additional expressions) can be treated just like a nested calc().
            return CSSValueCalc;
        }

        if (auto functionId = tokens.peek().functionId(); isCalcFunction(functionId, state.parserContext))
            return functionId;
        return std::nullopt;
    };

    if (auto functionId = findBlock(tokens)) {
        CSSParserTokenRange innerRange = tokens.consumeBlock();
        tokens.consumeWhitespace();
        innerRange.consumeWhitespace();

        auto function = parseCalcFunction(innerRange, *functionId, depth + 1, state);
        if (!function)
            return std::nullopt;

        if (!innerRange.atEnd()) {
            LOG_WITH_STREAM(Calc, stream << "Failed '" << nameLiteralForSerialization(*functionId) << "' function - extraneous tokens found");
            return std::nullopt;
        }

        return function;
    }

    auto token = tokens.consumeIncludingWhitespace();

    switch (token.type()) {
    case IdentToken:
        return parseCalcKeyword(token, state);
    case NumberToken:
        return parseCalcNumber(token, state);
    case PercentageToken:
        return parseCalcPercentage(token, state);
    case DimensionToken:
        return parseCalcDimension(token, state);
    default:
        break;
    }

    return std::nullopt;
}

std::optional<TypedChild> parseCalcKeyword(const CSSParserToken& token, ParserState& state)
{
    if (auto unit = state.parserOptions.allowedSymbols.get(token.id())) {
        auto child = Symbol { token.id(), *unit };
        auto type = Type::determineType(*unit);

        if (conversionToCanonicalUnitRequiresConversionData(*unit))
            state.requiresConversionData = true;

        if (auto* simplificationOptions = state.simplificationOptions) {
            if (auto replacement = simplify(child, *simplificationOptions))
                return TypedChild { WTFMove(*replacement), type };
        }

        return TypedChild { makeChild(WTFMove(child)), type };
    }

    if (auto constant = lookupConstantNumber(token.id())) {
        auto [child, type] = *constant;
        return TypedChild { makeChild(WTFMove(child)), type };
    }

    return std::nullopt;
}

std::optional<TypedChild> parseCalcNumber(const CSSParserToken& token, ParserState&)
{
    auto child = Number { .value = token.numericValue() };
    auto type = Type { };

    return TypedChild { makeChild(WTFMove(child)), type };
}

std::optional<TypedChild> parseCalcPercentage(const CSSParserToken& token, ParserState& state)
{
    auto child = Percentage { .value = token.numericValue(), .hint = Type::determinePercentHint(state.parserOptions.category) };
    auto type = getType(child);

    return TypedChild { makeChild(WTFMove(child)), type };
}

std::optional<TypedChild> parseCalcDimension(const CSSParserToken& token, ParserState& state)
{
    if (token.unitType() == CSSUnitType::CSS_UNKNOWN)
        return std::nullopt;

    auto child = makeNumeric(token.numericValue(), token.unitType());
    auto type = Type::determineType(token.unitType());

    if (conversionToCanonicalUnitRequiresConversionData(token.unitType()))
        state.requiresConversionData = true;

    if (auto* simplificationOptions = state.simplificationOptions)
        return TypedChild { copyAndSimplify(WTFMove(child), *simplificationOptions), type };
    return TypedChild { WTFMove(child), type };
}

} // namespace CSSCalc
} // namespace WebCore
