/*
 * Copyright (C) 2015-2018 Apple Inc. All rights reserved.
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
#include "B3StackmapValue.h"

#if ENABLE(B3_JIT)

#include "B3ValueInlines.h"

namespace JSC { namespace B3 {

namespace B3StackmapValueInternal {
constexpr bool dumpRegisters = false;
}

StackmapValue::~StackmapValue() = default;

void StackmapValue::append(Value* value, const ValueRep& rep)
{
    if (rep == ValueRep::ColdAny) {
        childrenVector().append(value);
        return;
    }

    while (m_reps.size() < numChildren())
        m_reps.append(ValueRep::ColdAny);

    childrenVector().append(value);
    m_reps.append(rep);
}

void StackmapValue::appendSomeRegister(Value* value)
{
    append(ConstrainedValue(value, ValueRep::SomeRegister));
}

void StackmapValue::appendSomeRegisterWithClobber(Value* value)
{
    append(ConstrainedValue(value, ValueRep::SomeRegisterWithClobber));
}

void StackmapValue::setConstrainedChild(unsigned index, const ConstrainedValue& constrainedValue)
{
    child(index) = constrainedValue.value();
    setConstraint(index, constrainedValue.rep());
}

void StackmapValue::setConstraint(unsigned index, const ValueRep& rep)
{
    if (rep == ValueRep(ValueRep::ColdAny))
        return;

    while (m_reps.size() <= index)
        m_reps.append(ValueRep::ColdAny);

    m_reps[index] = rep;
}

void StackmapValue::dumpChildren(CommaPrinter& comma, PrintStream& out) const
{
    for (ConstrainedValue value : constrainedChildren())
        out.print(comma, value);
}

void StackmapValue::dumpMeta(CommaPrinter& comma, PrintStream& out) const
{
    out.print(
        comma, "generator = ", RawPointer(m_generator.get()));
    if constexpr (B3StackmapValueInternal::dumpRegisters) {
        out.print(", earlyClobbered = ", m_earlyClobbered,
        ", lateClobbered = ", m_lateClobbered, ", usedRegisters = ", m_usedRegisters);
    }
}

StackmapValue::StackmapValue(CheckedOpcodeTag, Kind kind, Type type, Origin origin)
    : Value(CheckedOpcode, kind, type, VarArgs, origin)
{
    ASSERT(accepts(kind));
}

} } // namespace JSC::B3

#endif // ENABLE(B3_JIT)

