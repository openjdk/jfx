/*
 * Copyright (C) 2008-2019 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "ExceptionHelpers.h"

#include "CallFrame.h"
#include "CatchScope.h"
#include "CodeBlock.h"
#include "ErrorHandlingScope.h"
#include "Exception.h"
#include "Interpreter.h"
#include "JSCInlines.h"
#include "JSGlobalObjectFunctions.h"
#include "RuntimeType.h"
#include <wtf/text/StringBuilder.h>
#include <wtf/text/StringView.h>

namespace JSC {

STATIC_ASSERT_IS_TRIVIALLY_DESTRUCTIBLE(TerminatedExecutionError);

const ClassInfo TerminatedExecutionError::s_info = { "TerminatedExecutionError", &Base::s_info, nullptr, nullptr, CREATE_METHOD_TABLE(TerminatedExecutionError) };

JSValue TerminatedExecutionError::defaultValue(const JSObject*, ExecState* exec, PreferredPrimitiveType hint)
{
    if (hint == PreferString)
        return jsNontrivialString(exec->vm(), String("JavaScript execution terminated."_s));
    return JSValue(PNaN);
}

JSObject* createTerminatedExecutionException(VM* vm)
{
    return TerminatedExecutionError::create(*vm);
}

bool isTerminatedExecutionException(VM& vm, Exception* exception)
{
    if (!exception->value().isObject())
        return false;

    return exception->value().inherits<TerminatedExecutionError>(vm);
}

JSObject* createStackOverflowError(ExecState* exec)
{
    return createStackOverflowError(exec, exec->lexicalGlobalObject());
}

JSObject* createStackOverflowError(ExecState* exec, JSGlobalObject* globalObject)
{
    auto* error = createRangeError(exec, globalObject, "Maximum call stack size exceeded."_s);
    jsCast<ErrorInstance*>(error)->setStackOverflowError();
    return error;
}

JSObject* createUndefinedVariableError(ExecState* exec, const Identifier& ident)
{
    if (ident.isPrivateName())
        return createReferenceError(exec, makeString("Can't find private variable: PrivateSymbol.", ident.string()));
    return createReferenceError(exec, makeString("Can't find variable: ", ident.string()));
}

String errorDescriptionForValue(ExecState* exec, JSValue v)
{
    if (v.isString()) {
        String string = asString(v)->value(exec);
        if (!string)
            return string;
        return tryMakeString('"', string, '"');
    }

    if (v.isSymbol())
        return asSymbol(v)->descriptiveString();
    if (v.isObject()) {
        VM& vm = exec->vm();
        CallData callData;
        JSObject* object = asObject(v);
        if (object->methodTable(vm)->getCallData(object, callData) != CallType::None)
            return vm.smallStrings.functionString()->value(exec);
        return JSObject::calculatedClassName(object);
    }
    return v.toString(exec)->value(exec);
}

static String defaultApproximateSourceError(const String& originalMessage, const String& sourceText)
{
    return makeString(originalMessage, " (near '...", sourceText, "...')");
}

String defaultSourceAppender(const String& originalMessage, const String& sourceText, RuntimeType, ErrorInstance::SourceTextWhereErrorOccurred occurrence)
{
    if (occurrence == ErrorInstance::FoundApproximateSource)
        return defaultApproximateSourceError(originalMessage, sourceText);

    ASSERT(occurrence == ErrorInstance::FoundExactSource);
    return makeString(originalMessage, " (evaluating '", sourceText, "')");
}

static String functionCallBase(const String& sourceText)
{
    // This function retrieves the 'foo.bar' substring from 'foo.bar(baz)'.
    // FIXME: This function has simple processing of /* */ style comments.
    // It doesn't properly handle embedded comments of string literals that contain
    // parenthesis or comment constructs, e.g. foo.bar("/abc\)*/").
    // https://bugs.webkit.org/show_bug.cgi?id=146304

    unsigned sourceLength = sourceText.length();
    unsigned idx = sourceLength - 1;
    if (sourceLength < 2 || sourceText[idx] != ')') {
        // For function calls that have many new lines in between their open parenthesis
        // and their closing parenthesis, the text range passed into the message appender
        // will not inlcude the text in between these parentheses, it will just be the desired
        // text that precedes the parentheses.
        return String();
    }

    unsigned parenStack = 1;
    bool isInMultiLineComment = false;
    idx -= 1;
    // Note that we're scanning text right to left instead of the more common left to right,
    // so syntax detection is backwards.
    while (parenStack && idx) {
        UChar curChar = sourceText[idx];
        if (isInMultiLineComment) {
            if (curChar == '*' && sourceText[idx - 1] == '/') {
                isInMultiLineComment = false;
                --idx;
            }
        } else if (curChar == '(')
            --parenStack;
        else if (curChar == ')')
            ++parenStack;
        else if (curChar == '/' && sourceText[idx - 1] == '*') {
            isInMultiLineComment = true;
            --idx;
        }

        if (idx)
            --idx;
    }

    if (parenStack) {
        // As noted in the FIXME at the top of this function, there are bugs
        // in the above string processing. This algorithm is mostly best effort
        // and it works for most JS text in practice. However, if we determine
        // that the algorithm failed, we should just return the empty value.
        return String();
    }

    // Don't display the ?. of an optional call.
    if (idx > 1 && sourceText[idx] == '.' && sourceText[idx - 1] == '?')
        idx -= 2;

    return sourceText.left(idx + 1);
}

static String notAFunctionSourceAppender(const String& originalMessage, const String& sourceText, RuntimeType type, ErrorInstance::SourceTextWhereErrorOccurred occurrence)
{
    ASSERT(type != TypeFunction);

    if (occurrence == ErrorInstance::FoundApproximateSource)
        return defaultApproximateSourceError(originalMessage, sourceText);

    ASSERT(occurrence == ErrorInstance::FoundExactSource);
    auto notAFunctionIndex = originalMessage.reverseFind("is not a function");
    RELEASE_ASSERT(notAFunctionIndex != notFound);
    StringView displayValue;
    if (originalMessage.is8Bit())
        displayValue = StringView(originalMessage.characters8(), notAFunctionIndex - 1);
    else
        displayValue = StringView(originalMessage.characters16(), notAFunctionIndex - 1);

    String base = functionCallBase(sourceText);
    if (!base)
        return defaultApproximateSourceError(originalMessage, sourceText);
    StringBuilder builder(StringBuilder::OverflowHandler::RecordOverflow);
    builder.append(base, " is not a function. (In '", sourceText, "', '", base, "' is ");
    if (type == TypeSymbol)
        builder.appendLiteral("a Symbol");
    else {
        if (type == TypeObject)
            builder.appendLiteral("an instance of ");
        builder.append(displayValue);
    }
    builder.append(')');

    if (builder.hasOverflowed())
        return "object is not a function."_s;

    return builder.toString();
}

static String invalidParameterInSourceAppender(const String& originalMessage, const String& sourceText, RuntimeType type, ErrorInstance::SourceTextWhereErrorOccurred occurrence)
{
    ASSERT_UNUSED(type, type != TypeObject);

    if (occurrence == ErrorInstance::FoundApproximateSource)
        return defaultApproximateSourceError(originalMessage, sourceText);

    ASSERT(occurrence == ErrorInstance::FoundExactSource);
    auto inIndex = sourceText.reverseFind("in");
    if (inIndex == notFound) {
        // This should basically never happen, since JS code must use the literal
        // text "in" for the `in` operation. However, if we fail to find "in"
        // for any reason, just fail gracefully.
        return originalMessage;
    }
    if (sourceText.find("in") != inIndex)
        return makeString(originalMessage, " (evaluating '", sourceText, "')");

    static const unsigned inLength = 2;
    String rightHandSide = sourceText.substring(inIndex + inLength).simplifyWhiteSpace();
    return makeString(rightHandSide, " is not an Object. (evaluating '", sourceText, "')");
}

inline String invalidParameterInstanceofSourceAppender(const String& content, const String& originalMessage, const String& sourceText, RuntimeType, ErrorInstance::SourceTextWhereErrorOccurred occurrence)
{
    if (occurrence == ErrorInstance::FoundApproximateSource)
        return defaultApproximateSourceError(originalMessage, sourceText);

    ASSERT(occurrence == ErrorInstance::FoundExactSource);
    auto instanceofIndex = sourceText.reverseFind("instanceof");
    RELEASE_ASSERT(instanceofIndex != notFound);
    if (sourceText.find("instanceof") != instanceofIndex)
        return makeString(originalMessage, " (evaluating '", sourceText, "')");

    static const unsigned instanceofLength = 10;
    String rightHandSide = sourceText.substring(instanceofIndex + instanceofLength).simplifyWhiteSpace();
    return makeString(rightHandSide, content, ". (evaluating '", sourceText, "')");
}

static String invalidParameterInstanceofNotFunctionSourceAppender(const String& originalMessage, const String& sourceText, RuntimeType runtimeType, ErrorInstance::SourceTextWhereErrorOccurred occurrence)
{
    return invalidParameterInstanceofSourceAppender(" is not a function"_s, originalMessage, sourceText, runtimeType, occurrence);
}

static String invalidParameterInstanceofhasInstanceValueNotFunctionSourceAppender(const String& originalMessage, const String& sourceText, RuntimeType runtimeType, ErrorInstance::SourceTextWhereErrorOccurred occurrence)
{
    return invalidParameterInstanceofSourceAppender("[Symbol.hasInstance] is not a function, undefined, or null"_s, originalMessage, sourceText, runtimeType, occurrence);
}

JSObject* createError(ExecState* exec, JSValue value, const String& message, ErrorInstance::SourceAppender appender)
{
    VM& vm = exec->vm();
    auto scope = DECLARE_CATCH_SCOPE(vm);

    String valueDescription = errorDescriptionForValue(exec, value);
    ASSERT(scope.exception() || !!valueDescription);
    if (!valueDescription) {
        scope.clearException();
        return createOutOfMemoryError(exec);
    }
    String errorMessage = tryMakeString(valueDescription, ' ', message);
    if (!errorMessage)
        return createOutOfMemoryError(exec);
    scope.assertNoException();
    JSObject* exception = createTypeError(exec, errorMessage, appender, runtimeTypeForValue(vm, value));
    ASSERT(exception->isErrorInstance());

    return exception;
}

JSObject* createInvalidFunctionApplyParameterError(ExecState* exec, JSValue value)
{
    return createTypeError(exec, "second argument to Function.prototype.apply must be an Array-like object"_s, defaultSourceAppender, runtimeTypeForValue(exec->vm(), value));
}

JSObject* createInvalidInParameterError(ExecState* exec, JSValue value)
{
    return createError(exec, value, "is not an Object."_s, invalidParameterInSourceAppender);
}

JSObject* createInvalidInstanceofParameterErrorNotFunction(ExecState* exec, JSValue value)
{
    return createError(exec, value, " is not a function"_s, invalidParameterInstanceofNotFunctionSourceAppender);
}

JSObject* createInvalidInstanceofParameterErrorHasInstanceValueNotFunction(ExecState* exec, JSValue value)
{
    return createError(exec, value, "[Symbol.hasInstance] is not a function, undefined, or null"_s, invalidParameterInstanceofhasInstanceValueNotFunctionSourceAppender);
}

JSObject* createNotAConstructorError(ExecState* exec, JSValue value)
{
    return createError(exec, value, "is not a constructor"_s, defaultSourceAppender);
}

JSObject* createNotAFunctionError(ExecState* exec, JSValue value)
{
    return createError(exec, value, "is not a function"_s, notAFunctionSourceAppender);
}

JSObject* createNotAnObjectError(ExecState* exec, JSValue value)
{
    return createError(exec, value, "is not an object"_s, defaultSourceAppender);
}

JSObject* createErrorForInvalidGlobalAssignment(ExecState* exec, const String& propertyName)
{
    return createReferenceError(exec, makeString("Strict mode forbids implicit creation of global property '", propertyName, '\''));
}

JSObject* createTDZError(ExecState* exec)
{
    return createReferenceError(exec, "Cannot access uninitialized variable.");
}

Exception* throwOutOfMemoryError(ExecState* exec, ThrowScope& scope)
{
    return throwException(exec, scope, createOutOfMemoryError(exec));
}

Exception* throwStackOverflowError(ExecState* exec, ThrowScope& scope)
{
    VM& vm = exec->vm();
    ErrorHandlingScope errorScope(vm);
    return throwException(exec, scope, createStackOverflowError(exec));
}

Exception* throwTerminatedExecutionException(ExecState* exec, ThrowScope& scope)
{
    VM& vm = exec->vm();
    ErrorHandlingScope errorScope(vm);
    return throwException(exec, scope, createTerminatedExecutionException(&vm));
}

} // namespace JSC
