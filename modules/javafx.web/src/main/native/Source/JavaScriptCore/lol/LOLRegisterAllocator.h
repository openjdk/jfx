/*
 * Copyright (C) 2025 Apple Inc. All rights reserved.
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

#if ENABLE(JIT) && USE(JSVALUE64)

#include "BytecodeStructs.h"
#include "CodeBlock.h"
#include "Opcode.h"
#include "SimpleRegisterAllocator.h"
#include "VirtualRegister.h"

namespace JSC::LOL {

// TODO: Pack this.
struct Location {
    GPRReg gpr() const { return regs.gpr(); }
    void dumpInContext(PrintStream& out, const auto*) const
    {
        if (!isFlushed)
            out.print("!"_s);
    }

    JSValueRegs regs { InvalidGPRReg };
    bool isFlushed { false };
};

template<size_t useCount, size_t defCount, size_t scratchCount = 0>
struct AllocationBindings {
    std::array<JSValueRegs, useCount> uses;
    std::array<JSValueRegs, defCount> defs;
    std::array<JSValueRegs, scratchCount> scratches;
};

template<typename Backend>
class RegisterAllocator {
public:
#ifdef NDEBUG
    static constexpr bool verbose = false;
#else
    static constexpr bool verbose = true;
#endif

    static constexpr GPRReg s_scratch = GPRInfo::nonPreservedNonArgumentGPR0;

    struct GPRBank {
        using JITBackend = RegisterAllocator;
        using Register = GPRReg;
        static constexpr Register invalidRegister = InvalidGPRReg;
        // FIXME: Make this more precise
        static constexpr unsigned numberOfRegisters = 32;
        static constexpr Width defaultWidth = widthForBytes(sizeof(CPURegister));
    };
    using SpillHint = uint32_t;
    using RegisterBinding = VirtualRegister;
    template<typename> friend class JSC::SimpleRegisterAllocator;

    RegisterAllocator(Backend& backend, CodeBlock* codeBlock)
        : m_numVars(codeBlock->numVars())
        , m_constantsOffset(codeBlock->numCalleeLocals())
        , m_headersOffset(m_constantsOffset + codeBlock->constantRegisters().size())
        , m_locations(codeBlock->numCalleeLocals() + codeBlock->constantRegisters().size() + CallFrame::headerSizeInRegisters + codeBlock->numParameters())
        , m_backend(backend)
    {
        RegisterSetBuilder gprs = RegisterSetBuilder::allGPRs();
        gprs.exclude(RegisterSetBuilder::specialRegisters());
        gprs.exclude(RegisterSetBuilder::macroClobberedGPRs());
        gprs.exclude(RegisterSetBuilder::vmCalleeSaveRegisters());
        gprs.remove(s_scratch);
        m_allocator.initialize(gprs.buildAndValidate(), verbose ? "LOL"_s : ASCIILiteral());
    }

    RegisterSet allocatedRegisters() const { return m_allocator.allocatedRegisters(); }
    Location locationOf(VirtualRegister operand) const { return const_cast<RegisterAllocator<Backend>*>(this)->locationOfImpl(operand); }
    VirtualRegister bindingFor(GPRReg reg) const { return m_allocator.bindingFor(reg); }

    // In general, it's somewhat important that these don't change how they allocate based on profiling data as when someone
    // replays that profiling data could have changed and the register state they'd get would be out of sync with reality.
    // returns an AllocationBindings struct with the allocated registers + any scratches (if needed).
    // FIXME: We should be able to verify that the register allocation state is consistent by saving it on the CodeBlock somewhere and validating when we replay.
#define DECLARE_SPECIALIZATION(Op) ALWAYS_INLINE auto allocate(Backend& jit, const Op& instruction, BytecodeIndex);
    FOR_EACH_BYTECODE_STRUCT(DECLARE_SPECIALIZATION)
#undef DECLARE_SPECIALIZATION

    void flushAllRegisters(Backend&) { m_allocator.flushAllRegisters(*this); }

    void dump(PrintStream& out) const { m_allocator.dumpInContext(out, this); }

    // FIXME: Do we even need this, we could just unbind the scratches immediately after picking them since we can't add more allocations for the same instruction.
    template<size_t useCount, size_t defCount, size_t scratchCount>
    ALWAYS_INLINE void releaseScratches(const AllocationBindings<useCount, defCount, scratchCount>& allocations)
    {
        for (JSValueRegs scratch : allocations.scratches) {
            ASSERT(!bindingFor(scratch.gpr()).isValid());
            m_allocator.unbind(scratch.gpr());
        }
    }

private:
    template<size_t scratchCount, size_t useCount, size_t defCount>
    ALWAYS_INLINE AllocationBindings<useCount, defCount, scratchCount> allocateImpl(Backend& jit, const auto& instruction, BytecodeIndex index, const std::array<VirtualRegister, useCount>& uses, const std::array<VirtualRegister, defCount>& defs)
    {
        // TODO: Validation.
        UNUSED_PARAM(instruction);
        // Bump the spill count for our uses so we don't spill them when allocating below.
        for (auto operand : uses) {
            if (auto current = locationOf(operand).regs)
                m_allocator.setSpillHint(current.gpr(), index.offset());
        }

        auto doAllocate = [&](VirtualRegister operand, bool isDef) ALWAYS_INLINE_LAMBDA {
            ASSERT_IMPLIES(isDef, operand.isLocal() || operand.isArgument());
            Location& location = locationOfImpl(operand);
            if (location.regs) {
                // Uses might be dirty from a previous instruction, so don't touch them.
                if (isDef)
                    location.isFlushed = false;
                return location.regs;
            }

            // TODO: Consider LRU insertion policy here (i.e. 0 for hint). Might need locking so these don't spill on the next allocation in the same bytecode.
            location.regs = JSValueRegs(m_allocator.allocate(*this, operand, index.offset()));
            location.isFlushed = !isDef;

            if (!isDef)
                jit.fill(operand, location.regs.gpr());
            return location.regs;
        };

        AllocationBindings<useCount, defCount, scratchCount> result;
        for (size_t i = 0; i < uses.size(); ++i)
            result.uses[i] = doAllocate(uses[i], false);

        for (size_t i = 0; i < defs.size(); ++i)
            result.defs[i] = doAllocate(defs[i], true);

        // TODO: Maybe lock the register here for debugging purposes.
        for (size_t i = 0; i < result.scratches.size(); ++i)
            result.scratches[i] = JSValueRegs(m_allocator.allocate(*this, VirtualRegister(), 0));

        return result;
    }

    template<size_t scratchCount = 0>
    ALWAYS_INLINE auto allocateUnaryOp(Backend& jit, const auto& instruction, BytecodeIndex index, VirtualRegister source)
    {
        std::array<VirtualRegister, 1> uses = { source };
        std::array<VirtualRegister, 1> defs = { instruction.m_dst };
        return allocateImpl<scratchCount>(jit, instruction, index, uses, defs);
    }

    template<size_t scratchCount = 0>
    ALWAYS_INLINE auto allocateBinaryOp(Backend& jit, const auto& instruction, BytecodeIndex index)
    {
        std::array<VirtualRegister, 2> uses = { instruction.m_lhs, instruction.m_rhs };
        std::array<VirtualRegister, 1> defs = { instruction.m_dst };
        return allocateImpl<scratchCount>(jit, instruction, index, uses, defs);
    }

    friend class SimpleRegisterAllocator<GPRBank>;
    void flush(GPRReg gpr, VirtualRegister binding)
    {
        Location& location = locationOfImpl(binding);
        ASSERT(location.gpr() == gpr);
        m_backend.flush(location, gpr, binding);
        location = Location();
    }

    Location& locationOfImpl(VirtualRegister operand)
    {
        ASSERT(operand.isValid());
        // Locals are first since they are the most common and we want to be able to access them without loading offsets.
        if (operand.isLocal())
            return m_locations[operand.toLocal()];
        if (operand.isConstant())
            return m_locations[operand.toConstantIndex() + m_constantsOffset];
        ASSERT(operand.isArgument() || operand.isHeader());
        // arguments just naturally follow the headers.
        return m_locations[operand.offset() + m_headersOffset];
    }

    // Only used for debugging.
    const uint32_t m_numVars;
    const uint32_t m_constantsOffset;
    const uint32_t m_headersOffset;
    // This is laid out as [ locals, constants, headers, arguments ]
    FixedVector<Location> m_locations;
    SimpleRegisterAllocator<GPRBank> m_allocator;
    Backend& m_backend;
};

class ReplayBackend {
public:
    ReplayBackend() = default;
    ALWAYS_INLINE void flush(const Location&, GPRReg, VirtualRegister) { }
    ALWAYS_INLINE void fill(VirtualRegister, GPRReg) { }
};

using ReplayRegisterAllocator = RegisterAllocator<ReplayBackend>;

#define FOR_EACH_UNARY_OP(macro) \
    macro(OpToNumber, m_operand, 0) \
    macro(OpNegate, m_operand, 0) \
    macro(OpToString, m_operand, 0) \
    macro(OpToObject, m_operand, 0) \
    macro(OpToNumeric, m_operand, 0) \
    macro(OpBitnot, m_operand, 0) \
    macro(OpResolveScope, m_scope, 1) \
    macro(OpGetFromScope, m_scope, 1)

#define ALLOCATE_USE_DEFS_FOR_UNARY_OP(Struct, operand, scratchCount) \
template<typename Backend> \
auto RegisterAllocator<Backend>::allocate(Backend& jit, const Struct& instruction, BytecodeIndex index) \
{ \
    return allocateUnaryOp<scratchCount>(jit, instruction, index, instruction.operand); \
}

FOR_EACH_UNARY_OP(ALLOCATE_USE_DEFS_FOR_UNARY_OP)

#undef ALLOCATE_USE_DEFS_FOR_UNARY_OP
#undef FOR_EACH_UNARY_OP

#define FOR_EACH_BINARY_OP(macro) \
    macro(OpAdd) \
    macro(OpMul) \
    macro(OpSub) \
    macro(OpEq) \
    macro(OpNeq) \
    macro(OpLess) \
    macro(OpLesseq) \
    macro(OpGreater) \
    macro(OpGreatereq) \
    macro(OpLshift) \
    macro(OpRshift) \
    macro(OpUrshift) \
    macro(OpBitand) \
    macro(OpBitor) \
    macro(OpBitxor)

#define ALLOCATE_USE_DEFS_FOR_BINARY_OP(Struct) \
template<typename Backend> \
auto RegisterAllocator<Backend>::allocate(Backend& jit, const Struct& instruction, BytecodeIndex index) \
{ \
    return allocateBinaryOp(jit, instruction, index); \
}

FOR_EACH_BINARY_OP(ALLOCATE_USE_DEFS_FOR_BINARY_OP)

#undef ALLOCATE_USE_DEFS_FOR_BINARY_OP
#undef FOR_EACH_BINARY_OP

template<typename Backend>
auto RegisterAllocator<Backend>::allocate(Backend& jit, const OpPutToScope& instruction, BytecodeIndex index)
{
    std::array<VirtualRegister, 2> uses = { instruction.m_scope, instruction.m_value };
    std::array<VirtualRegister, 0> defs = { };
    return allocateImpl<1>(jit, instruction, index, uses, defs); // 1 scratch for metadata
}


} // namespace JSC

#endif
