/*
 * Copyright (C) 2017-2022 Apple Inc. All rights reserved.
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

#include "config.h"
#include "GetterSetterAccessCase.h"

#if ENABLE(JIT)

#include "JSCJSValueInlines.h"

namespace JSC {

GetterSetterAccessCase::GetterSetterAccessCase(VM& vm, JSCell* owner, AccessType accessType, CacheableIdentifier identifier, PropertyOffset offset, Structure* structure, const ObjectPropertyConditionSet& conditionSet, bool viaGlobalProxy, WatchpointSet* additionalSet, JSObject* customSlotBase, RefPtr<PolyProtoAccessChain>&& prototypeAccessChain)
    : Base(vm, owner, accessType, identifier, offset, structure, conditionSet, viaGlobalProxy, additionalSet, WTFMove(prototypeAccessChain))
{
    m_customSlotBase.setMayBeNull(vm, owner, customSlotBase);
}

Ref<AccessCase> GetterSetterAccessCase::create(
    VM& vm, JSCell* owner, AccessType type, CacheableIdentifier identifier, PropertyOffset offset, Structure* structure, const ObjectPropertyConditionSet& conditionSet,
    bool viaGlobalProxy, WatchpointSet* additionalSet, CodePtr<CustomAccessorPtrTag> customGetter, JSObject* customSlotBase,
    std::optional<DOMAttributeAnnotation> domAttribute, RefPtr<PolyProtoAccessChain>&& prototypeAccessChain)
{
    ASSERT(type == Getter || type == CustomValueGetter || type == CustomAccessorGetter);
    auto result = adoptRef(*new GetterSetterAccessCase(vm, owner, type, identifier, offset, structure, conditionSet, viaGlobalProxy, additionalSet, customSlotBase, WTFMove(prototypeAccessChain)));
    result->m_domAttribute = domAttribute;
    if (customGetter)
        result->m_customAccessor = customGetter;
    return result;
}

Ref<AccessCase> GetterSetterAccessCase::create(VM& vm, JSCell* owner, AccessType type, Structure* structure, CacheableIdentifier identifier, PropertyOffset offset,
    const ObjectPropertyConditionSet& conditionSet, RefPtr<PolyProtoAccessChain>&& prototypeAccessChain, bool viaGlobalProxy,
    CodePtr<CustomAccessorPtrTag> customSetter, JSObject* customSlotBase)
{
    ASSERT(type == Setter || type == CustomValueSetter || type == CustomAccessorSetter);
    auto result = adoptRef(*new GetterSetterAccessCase(vm, owner, type, identifier, offset, structure, conditionSet, viaGlobalProxy, nullptr, customSlotBase, WTFMove(prototypeAccessChain)));
    if (customSetter)
        result->m_customAccessor = customSetter;
    return result;
}

GetterSetterAccessCase::GetterSetterAccessCase(const GetterSetterAccessCase& other)
    : Base(other)
    , m_customSlotBase(other.m_customSlotBase)
{
    m_customAccessor = other.m_customAccessor;
    m_domAttribute = other.m_domAttribute;
}

JSObject* GetterSetterAccessCase::tryGetAlternateBaseImpl() const
{
    if (auto* object = customSlotBase())
        return object;
    return Base::tryGetAlternateBaseImpl();
}

void GetterSetterAccessCase::dumpImpl(PrintStream& out, CommaPrinter& comma, Indenter& indent) const
{
    Base::dumpImpl(out, comma, indent);
    out.print(comma, "customSlotBase = ", RawPointer(customSlotBase()));
    out.print(comma, "customAccessor = ", RawPointer(m_customAccessor.taggedPtr()));
}

} // namespace JSC

#endif // ENABLE(JIT)
