/*
 * Copyright (C) 2024 Apple Inc. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "URLPatternComponent.h"

#include "ScriptExecutionContext.h"
#include "URLPatternCanonical.h"
#include "URLPatternParser.h"
#include "URLPatternResult.h"
#include <JavaScriptCore/JSCJSValue.h>
#include <JavaScriptCore/JSString.h>
#include <JavaScriptCore/RegExpObject.h>

namespace WebCore {
using namespace JSC;
namespace URLPatternUtilities {

URLPatternComponent::URLPatternComponent(String&& patternString, JSC::Strong<JSC::RegExp>&& regex, Vector<String>&& groupNameList, bool hasRegexpGroupsFromPartsList)
    : m_patternString(WTFMove(patternString))
    , m_regularExpression(WTFMove(regex))
    , m_groupNameList(WTFMove(groupNameList))
    , m_hasRegexGroupsFromPartList(hasRegexpGroupsFromPartsList)
{
}

// https://urlpattern.spec.whatwg.org/#compile-a-component
ExceptionOr<URLPatternComponent> URLPatternComponent::compile(Ref<JSC::VM> vm, StringView input, EncodingCallbackType type, const URLPatternStringOptions& options)
{
    auto maybePartList = URLPatternParser::parse(input, options, type);
    if (maybePartList.hasException())
        return maybePartList.releaseException();
    Vector<Part> partList = maybePartList.releaseReturnValue();

    auto [regularExpressionString, nameList] = generateRegexAndNameList(partList, options);

    OptionSet<JSC::Yarr::Flags> flags = { JSC::Yarr::Flags::UnicodeSets };
    if (options.ignoreCase)
        flags.add(JSC::Yarr::Flags::IgnoreCase);

    JSC::RegExp* regularExpression = JSC::RegExp::create(vm, regularExpressionString, flags);
    if (!regularExpression->isValid())
        return Exception { ExceptionCode::TypeError, "Unable to create RegExp object regular expression from provided URLPattern string."_s };

    String patternString = generatePatternString(partList, options);

    bool hasRegexGroups = partList.containsIf([](auto& part) {
        return part.type == PartType::Regexp;
    });

    return URLPatternComponent { WTFMove(patternString), JSC::Strong<JSC::RegExp> { vm, regularExpression }, WTFMove(nameList), hasRegexGroups };
}

// https://urlpattern.spec.whatwg.org/#protocol-component-matches-a-special-scheme
bool URLPatternComponent::matchSpecialSchemeProtocol(ScriptExecutionContext& context) const
{
    Ref vm = context.vm();
    JSC::JSLockHolder lock(vm);

    static constexpr std::array specialSchemeList { "ftp"_s, "file"_s, "http"_s, "https"_s, "ws"_s, "wss"_s };
    auto contextObject = context.globalObject();
    if (!contextObject)
        return false;
    auto protocolRegex = JSC::RegExpObject::create(vm, contextObject->regExpStructure(), m_regularExpression.get(), true);

    auto isSchemeMatch = std::find_if(specialSchemeList.begin(), specialSchemeList.end(), [context = Ref { context }, &vm, &protocolRegex](const String& scheme) {
        auto maybeMatch = protocolRegex->exec(context->globalObject(), JSC::jsString(vm, scheme));
        return !maybeMatch.isNull();
    });

    return isSchemeMatch != specialSchemeList.end();
}

JSC::JSValue URLPatternComponent::componentExec(ScriptExecutionContext& context, StringView comparedString) const
{
    Ref vm = context.vm();
    JSC::JSLockHolder lock(vm);

    auto contextObject = context.globalObject();
    if (!contextObject)
        return JSC::JSValue::JSFalse;
    auto regex = JSC::RegExpObject::create(vm, contextObject->regExpStructure(), m_regularExpression.get(), true);
    return regex->exec(contextObject, JSC::jsString(vm, comparedString));
}

// https://urlpattern.spec.whatwg.org/#create-a-component-match-result
URLPatternComponentResult URLPatternComponent::createComponentMatchResult(JSC::JSGlobalObject* globalObject, String&& input, const JSC::JSValue& execResult) const
{
    URLPatternComponentResult::GroupsRecord groups;

    Ref vm = globalObject->vm();

    auto length = execResult.get(globalObject, vm->propertyNames->length).toIntegerOrInfinity(globalObject);
    ASSERT(length >= 0 && std::isfinite(length));

    for (unsigned index = 1; index < length; ++index) {
        auto match = execResult.get(globalObject, index);

        std::variant<std::monostate, String> value;
        if (!match.isNull() && !match.isUndefined())
            value = match.toWTFString(globalObject);

        groups.append(URLPatternComponentResult::NameMatchPair { m_groupNameList[index - 1], WTFMove(value) });
    }

    return URLPatternComponentResult { !input.isEmpty() ? WTFMove(input) : emptyString(), WTFMove(groups) };
}

}
}
