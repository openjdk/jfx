/*
 * Copyright (C) 2012, 2013 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#import <Foundation/Foundation.h>
#import <objc/message.h>
#import <wtf/MallocSpan.h>
#import <wtf/StdLibExtras.h>
#import <wtf/SystemMalloc.h>
#import <wtf/text/StringCommon.h>

#ifdef __cplusplus

template<typename ReturnType, typename... ArgumentTypes>
ReturnType wtfObjCMsgSend(id target, SEL selector, ArgumentTypes... arguments)
{
    return reinterpret_cast<ReturnType (*)(id, SEL, ArgumentTypes...)>(objc_msgSend)(target, selector, arguments...);
}

template<typename ReturnType, typename... ArgumentTypes>
ReturnType wtfCallIMP(IMP implementation, id target, SEL selector, ArgumentTypes... arguments)
{
    return reinterpret_cast<ReturnType (*)(id, SEL, ArgumentTypes...)>(implementation)(target, selector, arguments...);
}

namespace WTF {

WTF_EXPORT_PRIVATE MallocSpan<Method, SystemMalloc> class_copyMethodListSpan(Class);
WTF_EXPORT_PRIVATE MallocSpan<__unsafe_unretained Protocol *, SystemMalloc> class_copyProtocolListSpan(Class);
WTF_EXPORT_PRIVATE MallocSpan<objc_property_t, SystemMalloc> class_copyPropertyListSpan(Class);
WTF_EXPORT_PRIVATE MallocSpan<Ivar, SystemMalloc> class_copyIvarListSpan(Class);
WTF_EXPORT_PRIVATE MallocSpan<objc_method_description, SystemMalloc> protocol_copyMethodDescriptionListSpan(Protocol *, BOOL isRequiredMethod, BOOL isInstanceMethod);
WTF_EXPORT_PRIVATE MallocSpan<objc_property_t, SystemMalloc> protocol_copyPropertyListSpan(Protocol *);
WTF_EXPORT_PRIVATE MallocSpan<__unsafe_unretained Protocol *, SystemMalloc> protocol_copyProtocolListSpan(Protocol *);

template<typename Type>
std::span<const char> objcEncode()
{
    return unsafeSpan(@encode(Type));
}

template<typename Type>
bool nsValueHasObjCType(NSValue *value)
{
    return equalSpans(unsafeSpan([value objCType]), objcEncode<Type>());
}

template<typename ReturnType>
bool methodHasReturnType(NSMethodSignature *signature)
{
    return equalSpans(unsafeSpan(signature.methodReturnType), objcEncode<ReturnType>());
}

} // namespace WTF

using WTF::class_copyIvarListSpan;
using WTF::class_copyMethodListSpan;
using WTF::class_copyPropertyListSpan;
using WTF::class_copyProtocolListSpan;
using WTF::methodHasReturnType;
using WTF::nsValueHasObjCType;
using WTF::objcEncode;
using WTF::protocol_copyMethodDescriptionListSpan;
using WTF::protocol_copyPropertyListSpan;
using WTF::protocol_copyProtocolListSpan;

#endif // __cplusplus
