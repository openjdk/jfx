/*
 * Copyright (C) 2016-2019 Apple Inc. All rights reserved.
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
#include "WasmB3IRGenerator.h"

#if ENABLE(WEBASSEMBLY)

#include "AllowMacroScratchRegisterUsageIf.h"
#include "B3BasicBlockInlines.h"
#include "B3CCallValue.h"
#include "B3Compile.h"
#include "B3ConstPtrValue.h"
#include "B3FixSSA.h"
#include "B3Generate.h"
#include "B3InsertionSet.h"
#include "B3SlotBaseValue.h"
#include "B3StackmapGenerationParams.h"
#include "B3SwitchValue.h"
#include "B3UpsilonValue.h"
#include "B3Validate.h"
#include "B3ValueInlines.h"
#include "B3ValueKey.h"
#include "B3Variable.h"
#include "B3VariableValue.h"
#include "B3WasmAddressValue.h"
#include "B3WasmBoundsCheckValue.h"
#include "DisallowMacroScratchRegisterUsage.h"
#include "JSCInlines.h"
#include "JSWebAssemblyInstance.h"
#include "ScratchRegisterAllocator.h"
#include "VirtualRegister.h"
#include "WasmCallingConvention.h"
#include "WasmContextInlines.h"
#include "WasmExceptionType.h"
#include "WasmFunctionParser.h"
#include "WasmInstance.h"
#include "WasmMemory.h"
#include "WasmOMGPlan.h"
#include "WasmOSREntryData.h"
#include "WasmOpcodeOrigin.h"
#include "WasmOperations.h"
#include "WasmSignatureInlines.h"
#include "WasmThunks.h"
#include <limits>
#include <wtf/Optional.h>
#include <wtf/StdLibExtras.h>

void dumpProcedure(void* ptr)
{
    JSC::B3::Procedure* proc = static_cast<JSC::B3::Procedure*>(ptr);
    proc->dump(WTF::dataFile());
}

namespace JSC { namespace Wasm {

using namespace B3;

namespace {
namespace WasmB3IRGeneratorInternal {
static const bool verbose = false;
}
}

class B3IRGenerator {
public:
    struct ControlData {
        ControlData(Procedure& proc, Origin origin, Type signature, BlockType type, BasicBlock* continuation, BasicBlock* special = nullptr)
            : blockType(type)
            , continuation(continuation)
            , special(special)
        {
            if (signature != Void)
                result.append(proc.add<Value>(Phi, toB3Type(signature), origin));
        }

        ControlData()
        {
        }

        void dump(PrintStream& out) const
        {
            switch (type()) {
            case BlockType::If:
                out.print("If:       ");
                break;
            case BlockType::Block:
                out.print("Block:    ");
                break;
            case BlockType::Loop:
                out.print("Loop:     ");
                break;
            case BlockType::TopLevel:
                out.print("TopLevel: ");
                break;
            }
            out.print("Continuation: ", *continuation, ", Special: ");
            if (special)
                out.print(*special);
            else
                out.print("None");
        }

        BlockType type() const { return blockType; }

        bool hasNonVoidSignature() const { return result.size(); }

        BasicBlock* targetBlockForBranch()
        {
            if (type() == BlockType::Loop)
                return special;
            return continuation;
        }

        void convertIfToBlock()
        {
            ASSERT(type() == BlockType::If);
            blockType = BlockType::Block;
            special = nullptr;
        }

        using ResultList = Vector<Value*, 1>; // Value must be a Phi

        ResultList resultForBranch() const
        {
            if (type() == BlockType::Loop)
                return ResultList();
            return result;
        }

    private:
        friend class B3IRGenerator;
        BlockType blockType;
        BasicBlock* continuation;
        BasicBlock* special;
        ResultList result;
    };

    typedef Value* ExpressionType;
    typedef Vector<ExpressionType, 1> ExpressionList;

    friend class Stack;
    class Stack {
    public:
        Stack(B3IRGenerator* generator)
            : m_generator(generator)
        {
        }

        void append(ExpressionType expression)
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode) {
                Variable* variable = m_generator->m_proc.addVariable(expression->type());
                m_generator->m_currentBlock->appendNew<VariableValue>(m_generator->m_proc, Set, m_generator->origin(), variable, expression);
                m_stack.append(variable);
                return;
            }
            m_data.append(expression);
        }

        ExpressionType takeLast()
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode)
                return m_generator->m_currentBlock->appendNew<VariableValue>(m_generator->m_proc, B3::Get, m_generator->origin(), m_stack.takeLast());
            return m_data.takeLast();
        }

        ExpressionType last()
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode)
                return m_generator->m_currentBlock->appendNew<VariableValue>(m_generator->m_proc, B3::Get, m_generator->origin(), m_stack.last());
            return m_data.last();
        }

        unsigned size() const
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode)
                return m_stack.size();
            return m_data.size();
        }
        bool isEmpty() const { return size() == 0; }

        ExpressionList convertToExpressionList()
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode) {
                ExpressionList results;
                for (unsigned i = 0; i < m_stack.size(); ++i)
                    results.append(at(i));
                return results;
            }
            return m_data;
        }

        ExpressionType at(unsigned i) const
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode)
                return m_generator->m_currentBlock->appendNew<VariableValue>(m_generator->m_proc, B3::Get, m_generator->origin(), m_stack.at(i));
            return m_data.at(i);
        }

        Variable* variableAt(unsigned i) const
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode)
                return m_stack.at(i);
            return nullptr;
        }

        void shrink(unsigned i)
        {
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode) {
                m_stack.shrink(i);
                return;
            }
            m_data.shrink(i);
        }

        void swap(Stack& stack)
        {
            std::swap(m_generator, stack.m_generator);
            m_data.swap(stack.m_data);
            m_stack.swap(stack.m_stack);
        }

        void dump() const
        {
            CommaPrinter comma(", ", "");
            dataLog(comma, "ExpressionStack:");
            if (m_generator->m_compilationMode == CompilationMode::OMGForOSREntryMode) {
                for (const auto& variable : m_stack)
                    dataLog(comma, *variable);
                return;
            }
            for (const auto& expression : m_data)
                dataLog(comma, *expression);
        }

    private:
        B3IRGenerator* m_generator { nullptr };
        ExpressionList m_data;
        Vector<Variable*> m_stack;
    };
    Stack createStack() { return Stack(this); }

    using ControlType = ControlData;
    using ResultList = ControlData::ResultList;
    using ControlEntry = FunctionParser<B3IRGenerator>::ControlEntry;

    static constexpr ExpressionType emptyExpression() { return nullptr; }

    typedef String ErrorType;
    typedef Unexpected<ErrorType> UnexpectedResult;
    typedef Expected<std::unique_ptr<InternalFunction>, ErrorType> Result;
    typedef Expected<void, ErrorType> PartialResult;
    template <typename ...Args>
    NEVER_INLINE UnexpectedResult WARN_UNUSED_RETURN fail(Args... args) const
    {
        using namespace FailureHelper; // See ADL comment in WasmParser.h.
        return UnexpectedResult(makeString("WebAssembly.Module failed compiling: "_s, makeString(args)...));
    }
#define WASM_COMPILE_FAIL_IF(condition, ...) do { \
        if (UNLIKELY(condition))                  \
            return fail(__VA_ARGS__);             \
    } while (0)

    B3IRGenerator(const ModuleInformation&, Procedure&, InternalFunction*, Vector<UnlinkedWasmToWasmCall>&, unsigned& osrEntryScratchBufferSize, MemoryMode, CompilationMode, unsigned functionIndex, unsigned loopIndexForOSREntry, TierUpCount*, ThrowWasmException);

    PartialResult WARN_UNUSED_RETURN addArguments(const Signature&);
    PartialResult WARN_UNUSED_RETURN addLocal(Type, uint32_t);
    ExpressionType addConstant(Type, uint64_t);

    // References
    PartialResult WARN_UNUSED_RETURN addRefIsNull(ExpressionType& value, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addRefFunc(uint32_t index, ExpressionType& result);

    // Tables
    PartialResult WARN_UNUSED_RETURN addTableGet(unsigned, ExpressionType& index, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addTableSet(unsigned, ExpressionType& index, ExpressionType& value);
    PartialResult WARN_UNUSED_RETURN addTableSize(unsigned, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addTableGrow(unsigned, ExpressionType& fill, ExpressionType& delta, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addTableFill(unsigned, ExpressionType& offset, ExpressionType& fill, ExpressionType& count);
    // Locals
    PartialResult WARN_UNUSED_RETURN getLocal(uint32_t index, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN setLocal(uint32_t index, ExpressionType value);

    // Globals
    PartialResult WARN_UNUSED_RETURN getGlobal(uint32_t index, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN setGlobal(uint32_t index, ExpressionType value);

    // Memory
    PartialResult WARN_UNUSED_RETURN load(LoadOpType, ExpressionType pointer, ExpressionType& result, uint32_t offset);
    PartialResult WARN_UNUSED_RETURN store(StoreOpType, ExpressionType pointer, ExpressionType value, uint32_t offset);
    PartialResult WARN_UNUSED_RETURN addGrowMemory(ExpressionType delta, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addCurrentMemory(ExpressionType& result);

    // Basic operators
    template<OpType>
    PartialResult WARN_UNUSED_RETURN addOp(ExpressionType arg, ExpressionType& result);
    template<OpType>
    PartialResult WARN_UNUSED_RETURN addOp(ExpressionType left, ExpressionType right, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addSelect(ExpressionType condition, ExpressionType nonZero, ExpressionType zero, ExpressionType& result);

    // Control flow
    ControlData WARN_UNUSED_RETURN addTopLevel(Type signature);
    ControlData WARN_UNUSED_RETURN addBlock(Type signature);
    ControlData WARN_UNUSED_RETURN addLoop(Type signature, const Stack&, uint32_t);
    PartialResult WARN_UNUSED_RETURN addIf(ExpressionType condition, Type signature, ControlData& result);
    PartialResult WARN_UNUSED_RETURN addElse(ControlData&, const Stack&);
    PartialResult WARN_UNUSED_RETURN addElseToUnreachable(ControlData&);

    PartialResult WARN_UNUSED_RETURN addReturn(const ControlData&, const ExpressionList& returnValues);
    PartialResult WARN_UNUSED_RETURN addBranch(ControlData&, ExpressionType condition, const Stack& returnValues);
    PartialResult WARN_UNUSED_RETURN addSwitch(ExpressionType condition, const Vector<ControlData*>& targets, ControlData& defaultTargets, const Stack& expressionStack);
    PartialResult WARN_UNUSED_RETURN endBlock(ControlEntry&, Stack& expressionStack);
    PartialResult WARN_UNUSED_RETURN addEndToUnreachable(ControlEntry&);

    // Calls
    PartialResult WARN_UNUSED_RETURN addCall(uint32_t calleeIndex, const Signature&, Vector<ExpressionType>& args, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addCallIndirect(unsigned tableIndex, const Signature&, Vector<ExpressionType>& args, ExpressionType& result);
    PartialResult WARN_UNUSED_RETURN addUnreachable();

    void dump(const Vector<ControlEntry>& controlStack, const Stack* expressionStack);
    void setParser(FunctionParser<B3IRGenerator>* parser) { m_parser = parser; };

    Value* constant(B3::Type, uint64_t bits, Optional<Origin> = WTF::nullopt);
    void insertConstants();

    ALWAYS_INLINE void didKill(ExpressionType) { }

private:
    void emitExceptionCheck(CCallHelpers&, ExceptionType);

    void emitEntryTierUpCheck(int32_t incrementCount, B3::Origin);
    void emitLoopTierUpCheck(int32_t incrementCount, const Stack&, uint32_t, uint32_t, B3::Origin);

    void emitWriteBarrierForJSWrapper();
    ExpressionType emitCheckAndPreparePointer(ExpressionType pointer, uint32_t offset, uint32_t sizeOfOp);
    B3::Kind memoryKind(B3::Opcode memoryOp);
    ExpressionType emitLoadOp(LoadOpType, ExpressionType pointer, uint32_t offset);
    void emitStoreOp(StoreOpType, ExpressionType pointer, ExpressionType value, uint32_t offset);

    void unify(const ExpressionType phi, const ExpressionType source);
    void unifyValuesWithBlock(const Stack& resultStack, const ResultList& stack);

    void emitChecksForModOrDiv(B3::Opcode, ExpressionType left, ExpressionType right);

    int32_t WARN_UNUSED_RETURN fixupPointerPlusOffset(ExpressionType&, uint32_t);

    void restoreWasmContextInstance(Procedure&, BasicBlock*, Value*);
    enum class RestoreCachedStackLimit { No, Yes };
    void restoreWebAssemblyGlobalState(RestoreCachedStackLimit, const MemoryInformation&, Value* instance, Procedure&, BasicBlock*);

    Origin origin();

    uint32_t outerLoopIndex() const
    {
        if (m_outerLoops.isEmpty())
            return UINT32_MAX;
        return m_outerLoops.last();
    }

    FunctionParser<B3IRGenerator>* m_parser { nullptr };
    const ModuleInformation& m_info;
    const MemoryMode m_mode { MemoryMode::BoundsChecking };
    const CompilationMode m_compilationMode { CompilationMode::BBQMode };
    const unsigned m_functionIndex { UINT_MAX };
    const unsigned m_loopIndexForOSREntry { UINT_MAX };
    TierUpCount* m_tierUp { nullptr };

    Procedure& m_proc;
    BasicBlock* m_rootBlock { nullptr };
    BasicBlock* m_currentBlock { nullptr };
    Vector<uint32_t> m_outerLoops;
    Vector<Variable*> m_locals;
    Vector<UnlinkedWasmToWasmCall>& m_unlinkedWasmToWasmCalls; // List each call site and the function index whose address it should be patched with.
    unsigned& m_osrEntryScratchBufferSize;
    HashMap<ValueKey, Value*> m_constantPool;
    InsertionSet m_constantInsertionValues;
    GPRReg m_memoryBaseGPR { InvalidGPRReg };
    GPRReg m_memorySizeGPR { InvalidGPRReg };
    GPRReg m_wasmContextInstanceGPR { InvalidGPRReg };
    bool m_makesCalls { false };

    Value* m_instanceValue { nullptr }; // Always use the accessor below to ensure the instance value is materialized when used.
    bool m_usesInstanceValue { false };
    Value* instanceValue()
    {
        m_usesInstanceValue = true;
        return m_instanceValue;
    }

    uint32_t m_maxNumJSCallArguments { 0 };
    unsigned m_numImportFunctions;
};

// Memory accesses in WebAssembly have unsigned 32-bit offsets, whereas they have signed 32-bit offsets in B3.
int32_t B3IRGenerator::fixupPointerPlusOffset(ExpressionType& ptr, uint32_t offset)
{
    if (static_cast<uint64_t>(offset) > static_cast<uint64_t>(std::numeric_limits<int32_t>::max())) {
        ptr = m_currentBlock->appendNew<Value>(m_proc, Add, origin(), ptr, m_currentBlock->appendNew<Const64Value>(m_proc, origin(), offset));
        return 0;
    }
    return offset;
}

void B3IRGenerator::restoreWasmContextInstance(Procedure& proc, BasicBlock* block, Value* arg)
{
    if (Context::useFastTLS()) {
        PatchpointValue* patchpoint = block->appendNew<PatchpointValue>(proc, B3::Void, Origin());
        if (CCallHelpers::storeWasmContextInstanceNeedsMacroScratchRegister())
            patchpoint->clobber(RegisterSet::macroScratchRegisters());
        patchpoint->append(ConstrainedValue(arg, ValueRep::SomeRegister));
        patchpoint->setGenerator(
            [=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
                AllowMacroScratchRegisterUsageIf allowScratch(jit, CCallHelpers::storeWasmContextInstanceNeedsMacroScratchRegister());
                jit.storeWasmContextInstance(params[0].gpr());
            });
        return;
    }

    // FIXME: Because WasmToWasm call clobbers wasmContextInstance register and does not restore it, we need to restore it in the caller side.
    // This prevents us from using ArgumentReg to this (logically) immutable pinned register.
    PatchpointValue* patchpoint = block->appendNew<PatchpointValue>(proc, B3::Void, Origin());
    Effects effects = Effects::none();
    effects.writesPinned = true;
    effects.reads = B3::HeapRange::top();
    patchpoint->effects = effects;
    patchpoint->clobberLate(RegisterSet(m_wasmContextInstanceGPR));
    patchpoint->append(arg, ValueRep::SomeRegister);
    GPRReg wasmContextInstanceGPR = m_wasmContextInstanceGPR;
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& param) {
        jit.move(param[0].gpr(), wasmContextInstanceGPR);
    });
}

B3IRGenerator::B3IRGenerator(const ModuleInformation& info, Procedure& procedure, InternalFunction* compilation, Vector<UnlinkedWasmToWasmCall>& unlinkedWasmToWasmCalls, unsigned& osrEntryScratchBufferSize, MemoryMode mode, CompilationMode compilationMode, unsigned functionIndex, unsigned loopIndexForOSREntry, TierUpCount* tierUp, ThrowWasmException throwWasmException)
    : m_info(info)
    , m_mode(mode)
    , m_compilationMode(compilationMode)
    , m_functionIndex(functionIndex)
    , m_loopIndexForOSREntry(loopIndexForOSREntry)
    , m_tierUp(tierUp)
    , m_proc(procedure)
    , m_unlinkedWasmToWasmCalls(unlinkedWasmToWasmCalls)
    , m_osrEntryScratchBufferSize(osrEntryScratchBufferSize)
    , m_constantInsertionValues(m_proc)
    , m_numImportFunctions(info.importFunctionCount())
{
    m_rootBlock = m_proc.addBlock();
    m_currentBlock = m_rootBlock;

    // FIXME we don't really need to pin registers here if there's no memory. It makes wasm -> wasm thunks simpler for now. https://bugs.webkit.org/show_bug.cgi?id=166623
    const PinnedRegisterInfo& pinnedRegs = PinnedRegisterInfo::get();

    m_memoryBaseGPR = pinnedRegs.baseMemoryPointer;
    m_proc.pinRegister(m_memoryBaseGPR);

    m_wasmContextInstanceGPR = pinnedRegs.wasmContextInstancePointer;
    if (!Context::useFastTLS())
        m_proc.pinRegister(m_wasmContextInstanceGPR);

    if (mode != MemoryMode::Signaling) {
        m_memorySizeGPR = pinnedRegs.sizeRegister;
        m_proc.pinRegister(m_memorySizeGPR);
    }

    if (throwWasmException)
        Thunks::singleton().setThrowWasmException(throwWasmException);

    if (info.memory) {
        m_proc.setWasmBoundsCheckGenerator([=] (CCallHelpers& jit, GPRReg pinnedGPR) {
            AllowMacroScratchRegisterUsage allowScratch(jit);
            switch (m_mode) {
            case MemoryMode::BoundsChecking:
                ASSERT_UNUSED(pinnedGPR, m_memorySizeGPR == pinnedGPR);
                break;
            case MemoryMode::Signaling:
                ASSERT_UNUSED(pinnedGPR, InvalidGPRReg == pinnedGPR);
                break;
            }
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsMemoryAccess);
        });

        switch (m_mode) {
        case MemoryMode::BoundsChecking:
            break;
        case MemoryMode::Signaling:
            // Most memory accesses in signaling mode don't do an explicit
            // exception check because they can rely on fault handling to detect
            // out-of-bounds accesses. FaultSignalHandler nonetheless needs the
            // thunk to exist so that it can jump to that thunk.
            if (UNLIKELY(!Thunks::singleton().stub(throwExceptionFromWasmThunkGenerator)))
                CRASH();
            break;
        }
    }

    wasmCallingConvention().setupFrameInPrologue(&compilation->calleeMoveLocation, m_proc, Origin(), m_currentBlock);

    {
        B3::Value* framePointer = m_currentBlock->appendNew<B3::Value>(m_proc, B3::FramePointer, Origin());
        B3::PatchpointValue* stackOverflowCheck = m_currentBlock->appendNew<B3::PatchpointValue>(m_proc, pointerType(), Origin());
        m_instanceValue = stackOverflowCheck;
        stackOverflowCheck->appendSomeRegister(framePointer);
        stackOverflowCheck->clobber(RegisterSet::macroScratchRegisters());
        if (!Context::useFastTLS()) {
            // FIXME: Because WasmToWasm call clobbers wasmContextInstance register and does not restore it, we need to restore it in the caller side.
            // This prevents us from using ArgumentReg to this (logically) immutable pinned register.
            stackOverflowCheck->effects.writesPinned = false;
            stackOverflowCheck->effects.readsPinned = true;
            stackOverflowCheck->resultConstraints = { ValueRep::reg(m_wasmContextInstanceGPR) };
        }
        stackOverflowCheck->numGPScratchRegisters = 2;
        stackOverflowCheck->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams& params) {
            const Checked<int32_t> wasmFrameSize = params.proc().frameSize();
            const unsigned minimumParentCheckSize = WTF::roundUpToMultipleOf(stackAlignmentBytes(), 1024);
            const unsigned extraFrameSize = WTF::roundUpToMultipleOf(stackAlignmentBytes(), std::max<uint32_t>(
                // This allows us to elide stack checks for functions that are terminal nodes in the call
                // tree, (e.g they don't make any calls) and have a small enough frame size. This works by
                // having any such terminal node have its parent caller include some extra size in its
                // own check for it. The goal here is twofold:
                // 1. Emit less code.
                // 2. Try to speed things up by skipping stack checks.
                minimumParentCheckSize,
                // This allows us to elide stack checks in the Wasm -> Embedder call IC stub. Since these will
                // spill all arguments to the stack, we ensure that a stack check here covers the
                // stack that such a stub would use.
                (Checked<uint32_t>(m_maxNumJSCallArguments) * sizeof(Register) + jscCallingConvention().headerSizeInBytes()).unsafeGet()
            ));
            const int32_t checkSize = m_makesCalls ? (wasmFrameSize + extraFrameSize).unsafeGet() : wasmFrameSize.unsafeGet();
            bool needUnderflowCheck = static_cast<unsigned>(checkSize) > Options::reservedZoneSize();
            bool needsOverflowCheck = m_makesCalls || wasmFrameSize >= minimumParentCheckSize || needUnderflowCheck;

            GPRReg contextInstance = Context::useFastTLS() ? params[0].gpr() : m_wasmContextInstanceGPR;

            // This allows leaf functions to not do stack checks if their frame size is within
            // certain limits since their caller would have already done the check.
            if (needsOverflowCheck) {
                AllowMacroScratchRegisterUsage allowScratch(jit);
                GPRReg fp = params[1].gpr();
                GPRReg scratch1 = params.gpScratch(0);
                GPRReg scratch2 = params.gpScratch(1);

                if (Context::useFastTLS())
                    jit.loadWasmContextInstance(contextInstance);

                jit.loadPtr(CCallHelpers::Address(contextInstance, Instance::offsetOfCachedStackLimit()), scratch2);
                jit.addPtr(CCallHelpers::TrustedImm32(-checkSize), fp, scratch1);
                MacroAssembler::JumpList overflow;
                if (UNLIKELY(needUnderflowCheck))
                    overflow.append(jit.branchPtr(CCallHelpers::Above, scratch1, fp));
                overflow.append(jit.branchPtr(CCallHelpers::Below, scratch1, scratch2));
                jit.addLinkTask([overflow] (LinkBuffer& linkBuffer) {
                    linkBuffer.link(overflow, CodeLocationLabel<JITThunkPtrTag>(Thunks::singleton().stub(throwStackOverflowFromWasmThunkGenerator).code()));
                });
            } else if (m_usesInstanceValue && Context::useFastTLS()) {
                // No overflow check is needed, but the instance values still needs to be correct.
                AllowMacroScratchRegisterUsageIf allowScratch(jit, CCallHelpers::loadWasmContextInstanceNeedsMacroScratchRegister());
                jit.loadWasmContextInstance(contextInstance);
            } else {
                // We said we'd return a pointer. We don't actually need to because it isn't used, but the patchpoint conservatively said it had effects (potential stack check) which prevent it from getting removed.
            }
        });
    }

    emitEntryTierUpCheck(TierUpCount::functionEntryIncrement(), Origin());

    if (m_compilationMode == CompilationMode::OMGForOSREntryMode)
        m_currentBlock = m_proc.addBlock();
}

void B3IRGenerator::restoreWebAssemblyGlobalState(RestoreCachedStackLimit restoreCachedStackLimit, const MemoryInformation& memory, Value* instance, Procedure& proc, BasicBlock* block)
{
    restoreWasmContextInstance(proc, block, instance);

    if (restoreCachedStackLimit == RestoreCachedStackLimit::Yes) {
        // The Instance caches the stack limit, but also knows where its canonical location is.
        Value* pointerToActualStackLimit = block->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfPointerToActualStackLimit()));
        Value* actualStackLimit = block->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), pointerToActualStackLimit);
        block->appendNew<MemoryValue>(m_proc, Store, origin(), actualStackLimit, instanceValue(), safeCast<int32_t>(Instance::offsetOfCachedStackLimit()));
    }

    if (!!memory) {
        const PinnedRegisterInfo* pinnedRegs = &PinnedRegisterInfo::get();
        RegisterSet clobbers;
        clobbers.set(pinnedRegs->baseMemoryPointer);
        clobbers.set(pinnedRegs->sizeRegister);
        if (!isARM64())
            clobbers.set(RegisterSet::macroScratchRegisters());

        B3::PatchpointValue* patchpoint = block->appendNew<B3::PatchpointValue>(proc, B3::Void, origin());
        Effects effects = Effects::none();
        effects.writesPinned = true;
        effects.reads = B3::HeapRange::top();
        patchpoint->effects = effects;
        patchpoint->clobber(clobbers);
        patchpoint->numGPScratchRegisters = Gigacage::isEnabled(Gigacage::Primitive) ? 1 : 0;

        patchpoint->append(instance, ValueRep::SomeRegister);
        patchpoint->setGenerator([pinnedRegs] (CCallHelpers& jit, const B3::StackmapGenerationParams& params) {
            AllowMacroScratchRegisterUsage allowScratch(jit);
            GPRReg baseMemory = pinnedRegs->baseMemoryPointer;
            GPRReg scratchOrSize = Gigacage::isEnabled(Gigacage::Primitive) ? params.gpScratch(0) : pinnedRegs->sizeRegister;

            jit.loadPtr(CCallHelpers::Address(params[0].gpr(), Instance::offsetOfCachedMemorySize()), pinnedRegs->sizeRegister);
            jit.loadPtr(CCallHelpers::Address(params[0].gpr(), Instance::offsetOfCachedMemory()), baseMemory);

            jit.cageConditionally(Gigacage::Primitive, baseMemory, pinnedRegs->sizeRegister, scratchOrSize);
        });
    }
}

void B3IRGenerator::emitExceptionCheck(CCallHelpers& jit, ExceptionType type)
{
    jit.move(CCallHelpers::TrustedImm32(static_cast<uint32_t>(type)), GPRInfo::argumentGPR1);
    auto jumpToExceptionStub = jit.jump();

    jit.addLinkTask([jumpToExceptionStub] (LinkBuffer& linkBuffer) {
        linkBuffer.link(jumpToExceptionStub, CodeLocationLabel<JITThunkPtrTag>(Thunks::singleton().stub(throwExceptionFromWasmThunkGenerator).code()));
    });
}

Value* B3IRGenerator::constant(B3::Type type, uint64_t bits, Optional<Origin> maybeOrigin)
{
    auto result = m_constantPool.ensure(ValueKey(opcodeForConstant(type), type, static_cast<int64_t>(bits)), [&] {
        Value* result = m_proc.addConstant(maybeOrigin ? *maybeOrigin : origin(), type, bits);
        m_constantInsertionValues.insertValue(0, result);
        return result;
    });
    return result.iterator->value;
}

void B3IRGenerator::insertConstants()
{
    m_constantInsertionValues.execute(m_proc.at(0));
}

auto B3IRGenerator::addLocal(Type type, uint32_t count) -> PartialResult
{
    size_t newSize = m_locals.size() + count;
    ASSERT(!(CheckedUint32(count) + m_locals.size()).hasOverflowed());
    ASSERT(newSize <= maxFunctionLocals);
    WASM_COMPILE_FAIL_IF(!m_locals.tryReserveCapacity(newSize), "can't allocate memory for ", newSize, " locals");

    for (uint32_t i = 0; i < count; ++i) {
        Variable* local = m_proc.addVariable(toB3Type(type));
        m_locals.uncheckedAppend(local);
        auto val = isSubtype(type, Anyref) ? JSValue::encode(jsNull()) : 0;
        m_currentBlock->appendNew<VariableValue>(m_proc, Set, Origin(), local, constant(toB3Type(type), val, Origin()));
    }
    return { };
}

auto B3IRGenerator::addArguments(const Signature& signature) -> PartialResult
{
    ASSERT(!m_locals.size());
    WASM_COMPILE_FAIL_IF(!m_locals.tryReserveCapacity(signature.argumentCount()), "can't allocate memory for ", signature.argumentCount(), " arguments");

    m_locals.grow(signature.argumentCount());
    wasmCallingConvention().loadArguments(signature, m_proc, m_currentBlock, Origin(),
        [=] (ExpressionType argument, unsigned i) {
            Variable* argumentVariable = m_proc.addVariable(argument->type());
            m_locals[i] = argumentVariable;
            m_currentBlock->appendNew<VariableValue>(m_proc, Set, Origin(), argumentVariable, argument);
        });
    return { };
}

auto B3IRGenerator::addRefIsNull(ExpressionType& value, ExpressionType& result) -> PartialResult
{
    result = m_currentBlock->appendNew<Value>(m_proc, B3::Equal, origin(), value, m_currentBlock->appendNew<Const64Value>(m_proc, origin(), JSValue::encode(jsNull())));
    return { };
}

auto B3IRGenerator::addTableGet(unsigned tableIndex, ExpressionType& index, ExpressionType& result) -> PartialResult
{
    // FIXME: Emit this inline <https://bugs.webkit.org/show_bug.cgi?id=198506>.
    result = m_currentBlock->appendNew<CCallValue>(m_proc, toB3Type(Anyref), origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(&getWasmTableElement, B3CCallPtrTag)),
        instanceValue(), m_currentBlock->appendNew<Const32Value>(m_proc, origin(), tableIndex), index);

    {
        CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
            m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), result, m_currentBlock->appendNew<Const64Value>(m_proc, origin(), 0)));

        check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTableAccess);
        });
    }

    return { };
}

auto B3IRGenerator::addTableSet(unsigned tableIndex, ExpressionType& index, ExpressionType& value) -> PartialResult
{
    // FIXME: Emit this inline <https://bugs.webkit.org/show_bug.cgi?id=198506>.
    auto shouldThrow = m_currentBlock->appendNew<CCallValue>(m_proc, B3::Int32, origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(&setWasmTableElement, B3CCallPtrTag)),
        instanceValue(), m_currentBlock->appendNew<Const32Value>(m_proc, origin(), tableIndex), index, value);

    {
        CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
            m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), shouldThrow, m_currentBlock->appendNew<Const32Value>(m_proc, origin(), 0)));

        check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTableAccess);
        });
    }

    return { };
}

auto B3IRGenerator::addRefFunc(uint32_t index, ExpressionType& result) -> PartialResult
{
    // FIXME: Emit this inline <https://bugs.webkit.org/show_bug.cgi?id=198506>.

    result = m_currentBlock->appendNew<CCallValue>(m_proc, B3::Int64, origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(&doWasmRefFunc, B3CCallPtrTag)),
        instanceValue(), addConstant(Type::I32, index));

    return { };
}

auto B3IRGenerator::addTableSize(unsigned tableIndex, ExpressionType& result) -> PartialResult
{
    // FIXME: Emit this inline <https://bugs.webkit.org/show_bug.cgi?id=198506>.
    uint32_t (*doSize)(Instance*, unsigned) = [] (Instance* instance, unsigned tableIndex) -> uint32_t {
        return instance->table(tableIndex)->length();
    };

    result = m_currentBlock->appendNew<CCallValue>(m_proc, toB3Type(I32), origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(doSize, B3CCallPtrTag)),
        instanceValue(), m_currentBlock->appendNew<Const32Value>(m_proc, origin(), tableIndex));

    return { };
}

auto B3IRGenerator::addTableGrow(unsigned tableIndex, ExpressionType& fill, ExpressionType& delta, ExpressionType& result) -> PartialResult
{
    result = m_currentBlock->appendNew<CCallValue>(m_proc, toB3Type(I32), origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(&doWasmTableGrow, B3CCallPtrTag)),
        instanceValue(), m_currentBlock->appendNew<Const32Value>(m_proc, origin(), tableIndex), fill, delta);

    return { };
}

auto B3IRGenerator::addTableFill(unsigned tableIndex, ExpressionType& offset, ExpressionType& fill, ExpressionType& count) -> PartialResult
{
    auto result = m_currentBlock->appendNew<CCallValue>(m_proc, toB3Type(I32), origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(&doWasmTableFill, B3CCallPtrTag)),
        instanceValue(), m_currentBlock->appendNew<Const32Value>(m_proc, origin(), tableIndex), offset, fill, count);

    {
        CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
            m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), result, m_currentBlock->appendNew<Const32Value>(m_proc, origin(), 0)));

        check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTableAccess);
        });
    }

    return { };
}

auto B3IRGenerator::getLocal(uint32_t index, ExpressionType& result) -> PartialResult
{
    ASSERT(m_locals[index]);
    result = m_currentBlock->appendNew<VariableValue>(m_proc, B3::Get, origin(), m_locals[index]);
    return { };
}

auto B3IRGenerator::addUnreachable() -> PartialResult
{
    B3::PatchpointValue* unreachable = m_currentBlock->appendNew<B3::PatchpointValue>(m_proc, B3::Void, origin());
    unreachable->setGenerator([this] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::Unreachable);
    });
    unreachable->effects.terminal = true;
    return { };
}

auto B3IRGenerator::addGrowMemory(ExpressionType delta, ExpressionType& result) -> PartialResult
{
    int32_t (*growMemory)(void*, Instance*, int32_t) = [] (void* callFrame, Instance* instance, int32_t delta) -> int32_t {
        instance->storeTopCallFrame(callFrame);

        if (delta < 0)
            return -1;

        auto grown = instance->memory()->grow(PageCount(delta));
        if (!grown) {
            switch (grown.error()) {
            case Memory::GrowFailReason::InvalidDelta:
            case Memory::GrowFailReason::InvalidGrowSize:
            case Memory::GrowFailReason::WouldExceedMaximum:
            case Memory::GrowFailReason::OutOfMemory:
                return -1;
            }
            RELEASE_ASSERT_NOT_REACHED();
        }

        return grown.value().pageCount();
    };

    result = m_currentBlock->appendNew<CCallValue>(m_proc, Int32, origin(),
        m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(growMemory, B3CCallPtrTag)),
        m_currentBlock->appendNew<B3::Value>(m_proc, B3::FramePointer, origin()), instanceValue(), delta);

    restoreWebAssemblyGlobalState(RestoreCachedStackLimit::No, m_info.memory, instanceValue(), m_proc, m_currentBlock);

    return { };
}

auto B3IRGenerator::addCurrentMemory(ExpressionType& result) -> PartialResult
{
    static_assert(sizeof(decltype(static_cast<Memory*>(nullptr)->size())) == sizeof(uint64_t), "codegen relies on this size");
    Value* size = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, Int64, origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfCachedMemorySize()));

    constexpr uint32_t shiftValue = 16;
    static_assert(PageCount::pageSize == 1ull << shiftValue, "This must hold for the code below to be correct.");
    Value* numPages = m_currentBlock->appendNew<Value>(m_proc, ZShr, origin(),
        size, m_currentBlock->appendNew<Const32Value>(m_proc, origin(), shiftValue));

    result = m_currentBlock->appendNew<Value>(m_proc, Trunc, origin(), numPages);

    return { };
}

auto B3IRGenerator::setLocal(uint32_t index, ExpressionType value) -> PartialResult
{
    ASSERT(m_locals[index]);
    m_currentBlock->appendNew<VariableValue>(m_proc, B3::Set, origin(), m_locals[index], value);
    return { };
}

auto B3IRGenerator::getGlobal(uint32_t index, ExpressionType& result) -> PartialResult
{
    Value* globalsArray = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfGlobals()));
    result = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, toB3Type(m_info.globals[index].type), origin(), globalsArray, safeCast<int32_t>(index * sizeof(Register)));
    return { };
}

auto B3IRGenerator::setGlobal(uint32_t index, ExpressionType value) -> PartialResult
{
    ASSERT(toB3Type(m_info.globals[index].type) == value->type());
    Value* globalsArray = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfGlobals()));
    m_currentBlock->appendNew<MemoryValue>(m_proc, Store, origin(), value, globalsArray, safeCast<int32_t>(index * sizeof(Register)));

    if (isSubtype(m_info.globals[index].type, Anyref))
        emitWriteBarrierForJSWrapper();

    return { };
}

inline void B3IRGenerator::emitWriteBarrierForJSWrapper()
{
    Value* cell = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfOwner()));
    Value* cellState = m_currentBlock->appendNew<MemoryValue>(m_proc, Load8Z, Int32, origin(), cell, safeCast<int32_t>(JSCell::cellStateOffset()));
    Value* vm = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), cell, safeCast<int32_t>(JSWebAssemblyInstance::offsetOfVM()));
    Value* threshold = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, Int32, origin(), vm, safeCast<int32_t>(VM::offsetOfHeapBarrierThreshold()));

    BasicBlock* fenceCheckPath = m_proc.addBlock();
    BasicBlock* fencePath = m_proc.addBlock();
    BasicBlock* doSlowPath = m_proc.addBlock();
    BasicBlock* continuation = m_proc.addBlock();

    m_currentBlock->appendNewControlValue(m_proc, B3::Branch, origin(),
        m_currentBlock->appendNew<Value>(m_proc, Above, origin(), cellState, threshold),
        FrequentedBlock(continuation), FrequentedBlock(fenceCheckPath, FrequencyClass::Rare));
    fenceCheckPath->addPredecessor(m_currentBlock);
    continuation->addPredecessor(m_currentBlock);
    m_currentBlock = fenceCheckPath;

    Value* shouldFence = m_currentBlock->appendNew<MemoryValue>(m_proc, Load8Z, Int32, origin(), vm, safeCast<int32_t>(VM::offsetOfHeapMutatorShouldBeFenced()));
    m_currentBlock->appendNewControlValue(m_proc, B3::Branch, origin(),
        shouldFence,
        FrequentedBlock(fencePath), FrequentedBlock(doSlowPath));
    fencePath->addPredecessor(m_currentBlock);
    doSlowPath->addPredecessor(m_currentBlock);
    m_currentBlock = fencePath;

    B3::PatchpointValue* doFence = m_currentBlock->appendNew<B3::PatchpointValue>(m_proc, B3::Void, origin());
    doFence->setGenerator([] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
        jit.memoryFence();
    });

    Value* cellStateLoadAfterFence = m_currentBlock->appendNew<MemoryValue>(m_proc, Load8Z, Int32, origin(), cell, safeCast<int32_t>(JSCell::cellStateOffset()));
    m_currentBlock->appendNewControlValue(m_proc, B3::Branch, origin(),
        m_currentBlock->appendNew<Value>(m_proc, Above, origin(), cellStateLoadAfterFence, m_currentBlock->appendNew<Const32Value>(m_proc, origin(), blackThreshold)),
        FrequentedBlock(continuation), FrequentedBlock(doSlowPath, FrequencyClass::Rare));
    doSlowPath->addPredecessor(m_currentBlock);
    continuation->addPredecessor(m_currentBlock);
    m_currentBlock = doSlowPath;

    void (*writeBarrier)(JSWebAssemblyInstance*, VM*) = [] (JSWebAssemblyInstance* cell, VM* vm) -> void {
        vm->heap.writeBarrierSlowPath(cell);
    };

    Value* writeBarrierAddress = m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(writeBarrier, B3CCallPtrTag));
    m_currentBlock->appendNew<CCallValue>(m_proc, B3::Void, origin(), writeBarrierAddress, cell, vm);
    m_currentBlock->appendNewControlValue(m_proc, Jump, origin(), continuation);

    continuation->addPredecessor(m_currentBlock);
    m_currentBlock = continuation;
}

inline Value* B3IRGenerator::emitCheckAndPreparePointer(ExpressionType pointer, uint32_t offset, uint32_t sizeOfOperation)
{
    ASSERT(m_memoryBaseGPR);

    switch (m_mode) {
    case MemoryMode::BoundsChecking: {
        // We're not using signal handling at all, we must therefore check that no memory access exceeds the current memory size.
        ASSERT(m_memorySizeGPR);
        ASSERT(sizeOfOperation + offset > offset);
        m_currentBlock->appendNew<WasmBoundsCheckValue>(m_proc, origin(), m_memorySizeGPR, pointer, sizeOfOperation + offset - 1);
        break;
    }

    case MemoryMode::Signaling: {
        // We've virtually mapped 4GiB+redzone for this memory. Only the user-allocated pages are addressable, contiguously in range [0, current],
        // and everything above is mapped PROT_NONE. We don't need to perform any explicit bounds check in the 4GiB range because WebAssembly register
        // memory accesses are 32-bit. However WebAssembly register + offset accesses perform the addition in 64-bit which can push an access above
        // the 32-bit limit (the offset is unsigned 32-bit). The redzone will catch most small offsets, and we'll explicitly bounds check any
        // register + large offset access. We don't think this will be generated frequently.
        //
        // We could check that register + large offset doesn't exceed 4GiB+redzone since that's technically the limit we need to avoid overflowing the
        // PROT_NONE region, but it's better if we use a smaller immediate because it can codegens better. We know that anything equal to or greater
        // than the declared 'maximum' will trap, so we can compare against that number. If there was no declared 'maximum' then we still know that
        // any access equal to or greater than 4GiB will trap, no need to add the redzone.
        if (offset >= Memory::fastMappedRedzoneBytes()) {
            size_t maximum = m_info.memory.maximum() ? m_info.memory.maximum().bytes() : std::numeric_limits<uint32_t>::max();
            m_currentBlock->appendNew<WasmBoundsCheckValue>(m_proc, origin(), pointer, sizeOfOperation + offset - 1, maximum);
        }
        break;
    }
    }

    pointer = m_currentBlock->appendNew<Value>(m_proc, ZExt32, origin(), pointer);
    return m_currentBlock->appendNew<WasmAddressValue>(m_proc, origin(), pointer, m_memoryBaseGPR);
}

inline uint32_t sizeOfLoadOp(LoadOpType op)
{
    switch (op) {
    case LoadOpType::I32Load8S:
    case LoadOpType::I32Load8U:
    case LoadOpType::I64Load8S:
    case LoadOpType::I64Load8U:
        return 1;
    case LoadOpType::I32Load16S:
    case LoadOpType::I64Load16S:
    case LoadOpType::I32Load16U:
    case LoadOpType::I64Load16U:
        return 2;
    case LoadOpType::I32Load:
    case LoadOpType::I64Load32S:
    case LoadOpType::I64Load32U:
    case LoadOpType::F32Load:
        return 4;
    case LoadOpType::I64Load:
    case LoadOpType::F64Load:
        return 8;
    }
    RELEASE_ASSERT_NOT_REACHED();
}

inline B3::Kind B3IRGenerator::memoryKind(B3::Opcode memoryOp)
{
    if (m_mode == MemoryMode::Signaling)
        return trapping(memoryOp);
    return memoryOp;
}

inline Value* B3IRGenerator::emitLoadOp(LoadOpType op, ExpressionType pointer, uint32_t uoffset)
{
    int32_t offset = fixupPointerPlusOffset(pointer, uoffset);

    switch (op) {
    case LoadOpType::I32Load8S: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load8S), origin(), pointer, offset);
    }

    case LoadOpType::I64Load8S: {
        Value* value = m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load8S), origin(), pointer, offset);
        return m_currentBlock->appendNew<Value>(m_proc, SExt32, origin(), value);
    }

    case LoadOpType::I32Load8U: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load8Z), origin(), pointer, offset);
    }

    case LoadOpType::I64Load8U: {
        Value* value = m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load8Z), origin(), pointer, offset);
        return m_currentBlock->appendNew<Value>(m_proc, ZExt32, origin(), value);
    }

    case LoadOpType::I32Load16S: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load16S), origin(), pointer, offset);
    }

    case LoadOpType::I64Load16S: {
        Value* value = m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load16S), origin(), pointer, offset);
        return m_currentBlock->appendNew<Value>(m_proc, SExt32, origin(), value);
    }

    case LoadOpType::I32Load16U: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load16Z), origin(), pointer, offset);
    }

    case LoadOpType::I64Load16U: {
        Value* value = m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load16Z), origin(), pointer, offset);
        return m_currentBlock->appendNew<Value>(m_proc, ZExt32, origin(), value);
    }

    case LoadOpType::I32Load: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load), Int32, origin(), pointer, offset);
    }

    case LoadOpType::I64Load32U: {
        Value* value = m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load), Int32, origin(), pointer, offset);
        return m_currentBlock->appendNew<Value>(m_proc, ZExt32, origin(), value);
    }

    case LoadOpType::I64Load32S: {
        Value* value = m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load), Int32, origin(), pointer, offset);
        return m_currentBlock->appendNew<Value>(m_proc, SExt32, origin(), value);
    }

    case LoadOpType::I64Load: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load), Int64, origin(), pointer, offset);
    }

    case LoadOpType::F32Load: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load), Float, origin(), pointer, offset);
    }

    case LoadOpType::F64Load: {
        return m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Load), Double, origin(), pointer, offset);
    }
    }
    RELEASE_ASSERT_NOT_REACHED();
}

auto B3IRGenerator::load(LoadOpType op, ExpressionType pointer, ExpressionType& result, uint32_t offset) -> PartialResult
{
    ASSERT(pointer->type() == Int32);

    if (UNLIKELY(sumOverflows<uint32_t>(offset, sizeOfLoadOp(op)))) {
        // FIXME: Even though this is provably out of bounds, it's not a validation error, so we have to handle it
        // as a runtime exception. However, this may change: https://bugs.webkit.org/show_bug.cgi?id=166435
        B3::PatchpointValue* throwException = m_currentBlock->appendNew<B3::PatchpointValue>(m_proc, B3::Void, origin());
        throwException->setGenerator([this] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsMemoryAccess);
        });

        switch (op) {
        case LoadOpType::I32Load8S:
        case LoadOpType::I32Load16S:
        case LoadOpType::I32Load:
        case LoadOpType::I32Load16U:
        case LoadOpType::I32Load8U:
            result = constant(Int32, 0);
            break;
        case LoadOpType::I64Load8S:
        case LoadOpType::I64Load8U:
        case LoadOpType::I64Load16S:
        case LoadOpType::I64Load32U:
        case LoadOpType::I64Load32S:
        case LoadOpType::I64Load:
        case LoadOpType::I64Load16U:
            result = constant(Int64, 0);
            break;
        case LoadOpType::F32Load:
            result = constant(Float, 0);
            break;
        case LoadOpType::F64Load:
            result = constant(Double, 0);
            break;
        }

    } else
        result = emitLoadOp(op, emitCheckAndPreparePointer(pointer, offset, sizeOfLoadOp(op)), offset);

    return { };
}

inline uint32_t sizeOfStoreOp(StoreOpType op)
{
    switch (op) {
    case StoreOpType::I32Store8:
    case StoreOpType::I64Store8:
        return 1;
    case StoreOpType::I32Store16:
    case StoreOpType::I64Store16:
        return 2;
    case StoreOpType::I32Store:
    case StoreOpType::I64Store32:
    case StoreOpType::F32Store:
        return 4;
    case StoreOpType::I64Store:
    case StoreOpType::F64Store:
        return 8;
    }
    RELEASE_ASSERT_NOT_REACHED();
}


inline void B3IRGenerator::emitStoreOp(StoreOpType op, ExpressionType pointer, ExpressionType value, uint32_t uoffset)
{
    int32_t offset = fixupPointerPlusOffset(pointer, uoffset);

    switch (op) {
    case StoreOpType::I64Store8:
        value = m_currentBlock->appendNew<Value>(m_proc, Trunc, origin(), value);
        FALLTHROUGH;

    case StoreOpType::I32Store8:
        m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Store8), origin(), value, pointer, offset);
        return;

    case StoreOpType::I64Store16:
        value = m_currentBlock->appendNew<Value>(m_proc, Trunc, origin(), value);
        FALLTHROUGH;

    case StoreOpType::I32Store16:
        m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Store16), origin(), value, pointer, offset);
        return;

    case StoreOpType::I64Store32:
        value = m_currentBlock->appendNew<Value>(m_proc, Trunc, origin(), value);
        FALLTHROUGH;

    case StoreOpType::I64Store:
    case StoreOpType::I32Store:
    case StoreOpType::F32Store:
    case StoreOpType::F64Store:
        m_currentBlock->appendNew<MemoryValue>(m_proc, memoryKind(Store), origin(), value, pointer, offset);
        return;
    }
    RELEASE_ASSERT_NOT_REACHED();
}

auto B3IRGenerator::store(StoreOpType op, ExpressionType pointer, ExpressionType value, uint32_t offset) -> PartialResult
{
    ASSERT(pointer->type() == Int32);

    if (UNLIKELY(sumOverflows<uint32_t>(offset, sizeOfStoreOp(op)))) {
        // FIXME: Even though this is provably out of bounds, it's not a validation error, so we have to handle it
        // as a runtime exception. However, this may change: https://bugs.webkit.org/show_bug.cgi?id=166435
        B3::PatchpointValue* throwException = m_currentBlock->appendNew<B3::PatchpointValue>(m_proc, B3::Void, origin());
        throwException->setGenerator([this] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsMemoryAccess);
        });
    } else
        emitStoreOp(op, emitCheckAndPreparePointer(pointer, offset, sizeOfStoreOp(op)), value, offset);

    return { };
}

auto B3IRGenerator::addSelect(ExpressionType condition, ExpressionType nonZero, ExpressionType zero, ExpressionType& result) -> PartialResult
{
    result = m_currentBlock->appendNew<Value>(m_proc, B3::Select, origin(), condition, nonZero, zero);
    return { };
}

B3IRGenerator::ExpressionType B3IRGenerator::addConstant(Type type, uint64_t value)
{
    return constant(toB3Type(type), value);
}

void B3IRGenerator::emitEntryTierUpCheck(int32_t incrementCount, Origin origin)
{
    if (!m_tierUp)
        return;

    ASSERT(m_tierUp);
    Value* countDownLocation = constant(pointerType(), reinterpret_cast<uint64_t>(&m_tierUp->m_counter), origin);

    PatchpointValue* patch = m_currentBlock->appendNew<PatchpointValue>(m_proc, B3::Void, origin);
    Effects effects = Effects::none();
    // FIXME: we should have a more precise heap range for the tier up count.
    effects.reads = B3::HeapRange::top();
    effects.writes = B3::HeapRange::top();
    patch->effects = effects;
    patch->clobber(RegisterSet::macroScratchRegisters());

    patch->append(countDownLocation, ValueRep::SomeRegister);
    patch->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        AllowMacroScratchRegisterUsage allowScratch(jit);
        CCallHelpers::Jump tierUp = jit.branchAdd32(CCallHelpers::PositiveOrZero, CCallHelpers::TrustedImm32(incrementCount), CCallHelpers::Address(params[0].gpr()));
        CCallHelpers::Label tierUpResume = jit.label();

        params.addLatePath([=] (CCallHelpers& jit) {
            tierUp.link(&jit);

            const unsigned extraPaddingBytes = 0;
            RegisterSet registersToSpill = { };
            registersToSpill.add(GPRInfo::argumentGPR1);
            unsigned numberOfStackBytesUsedForRegisterPreservation = ScratchRegisterAllocator::preserveRegistersToStackForCall(jit, registersToSpill, extraPaddingBytes);

            jit.move(MacroAssembler::TrustedImm32(m_functionIndex), GPRInfo::argumentGPR1);
            MacroAssembler::Call call = jit.nearCall();

            ScratchRegisterAllocator::restoreRegistersFromStackForCall(jit, registersToSpill, RegisterSet(), numberOfStackBytesUsedForRegisterPreservation, extraPaddingBytes);
            jit.jump(tierUpResume);

            jit.addLinkTask([=] (LinkBuffer& linkBuffer) {
                MacroAssembler::repatchNearCall(linkBuffer.locationOfNearCall<NoPtrTag>(call), CodeLocationLabel<JITThunkPtrTag>(Thunks::singleton().stub(triggerOMGEntryTierUpThunkGenerator).code()));
            });
        });
    });
}

void B3IRGenerator::emitLoopTierUpCheck(int32_t incrementCount, const Stack& expressionStack, uint32_t loopIndex, uint32_t outerLoopIndex, B3::Origin origin)
{
    if (!m_tierUp)
        return;

    ASSERT(m_tierUp);

    ASSERT(m_tierUp->osrEntryTriggers().size() == loopIndex);
    m_tierUp->osrEntryTriggers().append(TierUpCount::TriggerReason::DontTrigger);
    m_tierUp->outerLoops().append(outerLoopIndex);

    Value* countDownLocation = constant(pointerType(), reinterpret_cast<uint64_t>(&m_tierUp->m_counter), origin);

    Vector<ExpressionType> stackmap;
    Vector<B3::Type> types;
    for (auto& local : m_locals) {
        ExpressionType result = m_currentBlock->appendNew<VariableValue>(m_proc, B3::Get, origin, local);
        stackmap.append(result);
        types.append(result->type());
    }
    for (unsigned i = 0; i < expressionStack.size(); ++i) {
        ExpressionType result = expressionStack.at(i);
        stackmap.append(result);
        types.append(result->type());
    }

    PatchpointValue* patch = m_currentBlock->appendNew<PatchpointValue>(m_proc, B3::Void, origin);
    Effects effects = Effects::none();
    // FIXME: we should have a more precise heap range for the tier up count.
    effects.reads = B3::HeapRange::top();
    effects.writes = B3::HeapRange::top();
    effects.exitsSideways = true;
    patch->effects = effects;

    patch->clobber(RegisterSet::macroScratchRegisters());
    RegisterSet clobberLate;
    clobberLate.add(GPRInfo::argumentGPR0);
    patch->clobberLate(clobberLate);

    patch->append(countDownLocation, ValueRep::SomeRegister);
    patch->appendVectorWithRep(stackmap, ValueRep::ColdAny);

    TierUpCount::TriggerReason* forceEntryTrigger = &(m_tierUp->osrEntryTriggers().last());
    static_assert(!static_cast<uint8_t>(TierUpCount::TriggerReason::DontTrigger), "the JIT code assumes non-zero means 'enter'");
    static_assert(sizeof(TierUpCount::TriggerReason) == 1, "branchTest8 assumes this size");
    patch->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        AllowMacroScratchRegisterUsage allowScratch(jit);
        CCallHelpers::Jump forceOSREntry = jit.branchTest8(CCallHelpers::NonZero, CCallHelpers::AbsoluteAddress(forceEntryTrigger));
        CCallHelpers::Jump tierUp = jit.branchAdd32(CCallHelpers::PositiveOrZero, CCallHelpers::TrustedImm32(incrementCount), CCallHelpers::Address(params[0].gpr()));
        MacroAssembler::Label tierUpResume = jit.label();

        OSREntryData& osrEntryData = m_tierUp->addOSREntryData(m_functionIndex, loopIndex);
        for (unsigned index = 0; index < types.size(); ++index)
            osrEntryData.values().constructAndAppend(params[index + 1], types[index]);
        OSREntryData* osrEntryDataPtr = &osrEntryData;

        params.addLatePath([=] (CCallHelpers& jit) {
            AllowMacroScratchRegisterUsage allowScratch(jit);
            forceOSREntry.link(&jit);
            tierUp.link(&jit);

            jit.probe(triggerOSREntryNow, osrEntryDataPtr);
            jit.branchTestPtr(CCallHelpers::Zero, GPRInfo::argumentGPR0).linkTo(tierUpResume, &jit);
            jit.farJump(GPRInfo::argumentGPR1, WasmEntryPtrTag);
        });
    });
}

B3IRGenerator::ControlData B3IRGenerator::addLoop(Type signature, const Stack& stack, uint32_t loopIndex)
{
    BasicBlock* body = m_proc.addBlock();
    BasicBlock* continuation = m_proc.addBlock();

    m_currentBlock->appendNewControlValue(m_proc, Jump, origin(), body);
    if (loopIndex == m_loopIndexForOSREntry) {
        m_currentBlock = m_rootBlock;
        m_osrEntryScratchBufferSize = m_locals.size() + stack.size();
        Value* pointer = m_rootBlock->appendNew<ArgumentRegValue>(m_proc, Origin(), GPRInfo::argumentGPR0);

        auto loadFromScratchBuffer = [&] (B3::Type type, unsigned index) {
            size_t offset = sizeof(uint64_t) * index;
            switch (type.kind()) {
            case B3::Int32:
                return m_currentBlock->appendNew<MemoryValue>(m_proc, Load, Int32, origin(), pointer, offset);
            case B3::Int64:
                return m_currentBlock->appendNew<MemoryValue>(m_proc, Load, B3::Int64, origin(), pointer, offset);
            case B3::Float:
                return m_currentBlock->appendNew<MemoryValue>(m_proc, Load, B3::Float, origin(), pointer, offset);
            case B3::Double:
                return m_currentBlock->appendNew<MemoryValue>(m_proc, Load, B3::Double, origin(), pointer, offset);
            default:
                RELEASE_ASSERT_NOT_REACHED();
                break;
            }
        };

        unsigned indexInBuffer = 0;
        for (auto& local : m_locals)
            m_currentBlock->appendNew<VariableValue>(m_proc, Set, Origin(), local, loadFromScratchBuffer(local->type(), indexInBuffer++));
        for (unsigned i = 0; i < stack.size(); ++i) {
            auto* variable = stack.variableAt(i);
            m_currentBlock->appendNew<VariableValue>(m_proc, Set, Origin(), variable, loadFromScratchBuffer(variable->type(), indexInBuffer++));
        }
        m_currentBlock->appendNewControlValue(m_proc, Jump, origin(), body);
        body->addPredecessor(m_currentBlock);
    }

    uint32_t outerLoopIndex = this->outerLoopIndex();
    m_outerLoops.append(loopIndex);
    m_currentBlock = body;
    emitLoopTierUpCheck(TierUpCount::loopIncrement(), stack, loopIndex, outerLoopIndex, origin());

    return ControlData(m_proc, origin(), signature, BlockType::Loop, continuation, body);
}

B3IRGenerator::ControlData B3IRGenerator::addTopLevel(Type signature)
{
    return ControlData(m_proc, Origin(), signature, BlockType::TopLevel, m_proc.addBlock());
}

B3IRGenerator::ControlData B3IRGenerator::addBlock(Type signature)
{
    return ControlData(m_proc, origin(), signature, BlockType::Block, m_proc.addBlock());
}

auto B3IRGenerator::addIf(ExpressionType condition, Type signature, ControlType& result) -> PartialResult
{
    // FIXME: This needs to do some kind of stack passing.

    BasicBlock* taken = m_proc.addBlock();
    BasicBlock* notTaken = m_proc.addBlock();
    BasicBlock* continuation = m_proc.addBlock();

    m_currentBlock->appendNew<Value>(m_proc, B3::Branch, origin(), condition);
    m_currentBlock->setSuccessors(FrequentedBlock(taken), FrequentedBlock(notTaken));
    taken->addPredecessor(m_currentBlock);
    notTaken->addPredecessor(m_currentBlock);

    m_currentBlock = taken;
    result = ControlData(m_proc, origin(), signature, BlockType::If, continuation, notTaken);
    return { };
}

auto B3IRGenerator::addElse(ControlData& data, const Stack& currentStack) -> PartialResult
{
    unifyValuesWithBlock(currentStack, data.result);
    m_currentBlock->appendNewControlValue(m_proc, Jump, origin(), data.continuation);
    return addElseToUnreachable(data);
}

auto B3IRGenerator::addElseToUnreachable(ControlData& data) -> PartialResult
{
    ASSERT(data.type() == BlockType::If);
    m_currentBlock = data.special;
    data.convertIfToBlock();
    return { };
}

auto B3IRGenerator::addReturn(const ControlData&, const ExpressionList& returnValues) -> PartialResult
{
    ASSERT(returnValues.size() <= 1);
    if (returnValues.size())
        m_currentBlock->appendNewControlValue(m_proc, B3::Return, origin(), returnValues[0]);
    else
        m_currentBlock->appendNewControlValue(m_proc, B3::Return, origin());
    return { };
}

auto B3IRGenerator::addBranch(ControlData& data, ExpressionType condition, const Stack& returnValues) -> PartialResult
{
    unifyValuesWithBlock(returnValues, data.resultForBranch());

    BasicBlock* target = data.targetBlockForBranch();
    if (condition) {
        BasicBlock* continuation = m_proc.addBlock();
        m_currentBlock->appendNew<Value>(m_proc, B3::Branch, origin(), condition);
        m_currentBlock->setSuccessors(FrequentedBlock(target), FrequentedBlock(continuation));
        target->addPredecessor(m_currentBlock);
        continuation->addPredecessor(m_currentBlock);
        m_currentBlock = continuation;
    } else {
        m_currentBlock->appendNewControlValue(m_proc, Jump, origin(), FrequentedBlock(target));
        target->addPredecessor(m_currentBlock);
    }

    return { };
}

auto B3IRGenerator::addSwitch(ExpressionType condition, const Vector<ControlData*>& targets, ControlData& defaultTarget, const Stack& expressionStack) -> PartialResult
{
    for (size_t i = 0; i < targets.size(); ++i)
        unifyValuesWithBlock(expressionStack, targets[i]->resultForBranch());
    unifyValuesWithBlock(expressionStack, defaultTarget.resultForBranch());

    SwitchValue* switchValue = m_currentBlock->appendNew<SwitchValue>(m_proc, origin(), condition);
    switchValue->setFallThrough(FrequentedBlock(defaultTarget.targetBlockForBranch()));
    for (size_t i = 0; i < targets.size(); ++i)
        switchValue->appendCase(SwitchCase(i, FrequentedBlock(targets[i]->targetBlockForBranch())));

    return { };
}

auto B3IRGenerator::endBlock(ControlEntry& entry, Stack& expressionStack) -> PartialResult
{
    ControlData& data = entry.controlData;

    unifyValuesWithBlock(expressionStack, data.result);
    m_currentBlock->appendNewControlValue(m_proc, Jump, origin(), data.continuation);
    data.continuation->addPredecessor(m_currentBlock);

    if (data.type() == BlockType::Loop)
        m_outerLoops.removeLast();

    return addEndToUnreachable(entry);
}


auto B3IRGenerator::addEndToUnreachable(ControlEntry& entry) -> PartialResult
{
    ControlData& data = entry.controlData;
    m_currentBlock = data.continuation;

    if (data.type() == BlockType::If) {
        data.special->appendNewControlValue(m_proc, Jump, origin(), m_currentBlock);
        m_currentBlock->addPredecessor(data.special);
    }

    for (Value* result : data.result) {
        m_currentBlock->append(result);
        entry.enclosedExpressionStack.append(result);
    }

    // TopLevel does not have any code after this so we need to make sure we emit a return here.
    if (data.type() == BlockType::TopLevel)
        return addReturn(entry.controlData, entry.enclosedExpressionStack.convertToExpressionList());

    return { };
}

auto B3IRGenerator::addCall(uint32_t functionIndex, const Signature& signature, Vector<ExpressionType>& args, ExpressionType& result) -> PartialResult
{
    ASSERT(signature.argumentCount() == args.size());

    m_makesCalls = true;

    Type returnType = signature.returnType();
    Vector<UnlinkedWasmToWasmCall>* unlinkedWasmToWasmCalls = &m_unlinkedWasmToWasmCalls;

    if (m_info.isImportedFunctionFromFunctionIndexSpace(functionIndex)) {
        m_maxNumJSCallArguments = std::max(m_maxNumJSCallArguments, static_cast<uint32_t>(args.size()));

        // FIXME imports can be linked here, instead of generating a patchpoint, because all import stubs are generated before B3 compilation starts. https://bugs.webkit.org/show_bug.cgi?id=166462
        Value* targetInstance = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfTargetInstance(functionIndex)));
        // The target instance is 0 unless the call is wasm->wasm.
        Value* isWasmCall = m_currentBlock->appendNew<Value>(m_proc, NotEqual, origin(), targetInstance, m_currentBlock->appendNew<Const64Value>(m_proc, origin(), 0));

        BasicBlock* isWasmBlock = m_proc.addBlock();
        BasicBlock* isEmbedderBlock = m_proc.addBlock();
        BasicBlock* continuation = m_proc.addBlock();
        m_currentBlock->appendNewControlValue(m_proc, B3::Branch, origin(), isWasmCall, FrequentedBlock(isWasmBlock), FrequentedBlock(isEmbedderBlock));

        Value* wasmCallResult = wasmCallingConvention().setupCall(m_proc, isWasmBlock, origin(), args, toB3Type(returnType),
            [=] (PatchpointValue* patchpoint) {
                patchpoint->effects.writesPinned = true;
                patchpoint->effects.readsPinned = true;
                // We need to clobber all potential pinned registers since we might be leaving the instance.
                // We pessimistically assume we could be calling to something that is bounds checking.
                // FIXME: We shouldn't have to do this: https://bugs.webkit.org/show_bug.cgi?id=172181
                patchpoint->clobberLate(PinnedRegisterInfo::get().toSave(MemoryMode::BoundsChecking));
                patchpoint->setGenerator([unlinkedWasmToWasmCalls, functionIndex] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
                    AllowMacroScratchRegisterUsage allowScratch(jit);
                    CCallHelpers::Call call = jit.threadSafePatchableNearCall();
                    jit.addLinkTask([unlinkedWasmToWasmCalls, call, functionIndex] (LinkBuffer& linkBuffer) {
                        unlinkedWasmToWasmCalls->append({ linkBuffer.locationOfNearCall<WasmEntryPtrTag>(call), functionIndex });
                    });
                });
            });
        UpsilonValue* wasmCallResultUpsilon = returnType == Void ? nullptr : isWasmBlock->appendNew<UpsilonValue>(m_proc, origin(), wasmCallResult);
        isWasmBlock->appendNewControlValue(m_proc, Jump, origin(), continuation);

        // FIXME: Let's remove this indirection by creating a PIC friendly IC
        // for calls out to the embedder. This shouldn't be that hard to do. We could probably
        // implement the IC to be over Context*.
        // https://bugs.webkit.org/show_bug.cgi?id=170375
        Value* jumpDestination = isEmbedderBlock->appendNew<MemoryValue>(m_proc,
            Load, pointerType(), origin(), instanceValue(), safeCast<int32_t>(Instance::offsetOfWasmToEmbedderStub(functionIndex)));

        Value* embedderCallResult = wasmCallingConvention().setupCall(m_proc, isEmbedderBlock, origin(), args, toB3Type(returnType),
            [=] (PatchpointValue* patchpoint) {
                patchpoint->effects.writesPinned = true;
                patchpoint->effects.readsPinned = true;
                patchpoint->append(jumpDestination, ValueRep::SomeRegister);
                // We need to clobber all potential pinned registers since we might be leaving the instance.
                // We pessimistically assume we could be calling to something that is bounds checking.
                // FIXME: We shouldn't have to do this: https://bugs.webkit.org/show_bug.cgi?id=172181
                patchpoint->clobberLate(PinnedRegisterInfo::get().toSave(MemoryMode::BoundsChecking));
                patchpoint->setGenerator([returnType] (CCallHelpers& jit, const B3::StackmapGenerationParams& params) {
                    AllowMacroScratchRegisterUsage allowScratch(jit);
                    jit.call(params[returnType == Void ? 0 : 1].gpr(), WasmEntryPtrTag);
                });
            });
        UpsilonValue* embedderCallResultUpsilon = returnType == Void ? nullptr : isEmbedderBlock->appendNew<UpsilonValue>(m_proc, origin(), embedderCallResult);
        isEmbedderBlock->appendNewControlValue(m_proc, Jump, origin(), continuation);

        m_currentBlock = continuation;

        if (returnType == Void)
            result = nullptr;
        else {
            result = continuation->appendNew<Value>(m_proc, Phi, toB3Type(returnType), origin());
            wasmCallResultUpsilon->setPhi(result);
            embedderCallResultUpsilon->setPhi(result);
        }

        // The call could have been to another WebAssembly instance, and / or could have modified our Memory.
        restoreWebAssemblyGlobalState(RestoreCachedStackLimit::Yes, m_info.memory, instanceValue(), m_proc, continuation);
    } else {
        result = wasmCallingConvention().setupCall(m_proc, m_currentBlock, origin(), args, toB3Type(returnType),
            [=] (PatchpointValue* patchpoint) {
                patchpoint->effects.writesPinned = true;
                patchpoint->effects.readsPinned = true;

                patchpoint->setGenerator([unlinkedWasmToWasmCalls, functionIndex] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
                    AllowMacroScratchRegisterUsage allowScratch(jit);
                    CCallHelpers::Call call = jit.threadSafePatchableNearCall();
                    jit.addLinkTask([unlinkedWasmToWasmCalls, call, functionIndex] (LinkBuffer& linkBuffer) {
                        unlinkedWasmToWasmCalls->append({ linkBuffer.locationOfNearCall<WasmEntryPtrTag>(call), functionIndex });
                    });
                });
            });
    }

    return { };
}

auto B3IRGenerator::addCallIndirect(unsigned tableIndex, const Signature& signature, Vector<ExpressionType>& args, ExpressionType& result) -> PartialResult
{
    ExpressionType calleeIndex = args.takeLast();
    ASSERT(signature.argumentCount() == args.size());

    m_makesCalls = true;
    // Note: call indirect can call either WebAssemblyFunction or WebAssemblyWrapperFunction. Because
    // WebAssemblyWrapperFunction is like calling into the embedder, we conservatively assume all call indirects
    // can be to the embedder for our stack check calculation.
    m_maxNumJSCallArguments = std::max(m_maxNumJSCallArguments, static_cast<uint32_t>(args.size()));

    ExpressionType callableFunctionBuffer;
    ExpressionType instancesBuffer;
    ExpressionType callableFunctionBufferLength;
    ExpressionType mask;
    {
        ExpressionType table = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(),
            instanceValue(), safeCast<int32_t>(Instance::offsetOfTablePtr(m_numImportFunctions, tableIndex)));
        callableFunctionBuffer = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(),
            table, safeCast<int32_t>(FuncRefTable::offsetOfFunctions()));
        instancesBuffer = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(),
            table, safeCast<int32_t>(FuncRefTable::offsetOfInstances()));
        callableFunctionBufferLength = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, Int32, origin(),
            table, safeCast<int32_t>(Table::offsetOfLength()));
        mask = m_currentBlock->appendNew<Value>(m_proc, ZExt32, origin(),
            m_currentBlock->appendNew<MemoryValue>(m_proc, Load, Int32, origin(),
                table, safeCast<int32_t>(Table::offsetOfMask())));
    }

    // Check the index we are looking for is valid.
    {
        CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
            m_currentBlock->appendNew<Value>(m_proc, AboveEqual, origin(), calleeIndex, callableFunctionBufferLength));

        check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsCallIndirect);
        });
    }

    calleeIndex = m_currentBlock->appendNew<Value>(m_proc, ZExt32, origin(), calleeIndex);

    if (Options::enableSpectreMitigations())
        calleeIndex = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(), mask, calleeIndex);

    ExpressionType callableFunction;
    {
        // Compute the offset in the table index space we are looking for.
        ExpressionType offset = m_currentBlock->appendNew<Value>(m_proc, Mul, origin(),
            calleeIndex, constant(pointerType(), sizeof(WasmToWasmImportableFunction)));
        callableFunction = m_currentBlock->appendNew<Value>(m_proc, Add, origin(), callableFunctionBuffer, offset);

        // Check that the WasmToWasmImportableFunction is initialized. We trap if it isn't. An "invalid" SignatureIndex indicates it's not initialized.
        // FIXME: when we have trap handlers, we can just let the call fail because Signature::invalidIndex is 0. https://bugs.webkit.org/show_bug.cgi?id=177210
        static_assert(sizeof(WasmToWasmImportableFunction::signatureIndex) == sizeof(uint64_t), "Load codegen assumes i64");
        ExpressionType calleeSignatureIndex = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, Int64, origin(), callableFunction, safeCast<int32_t>(WasmToWasmImportableFunction::offsetOfSignatureIndex()));
        {
            CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
                m_currentBlock->appendNew<Value>(m_proc, Equal, origin(),
                    calleeSignatureIndex,
                    m_currentBlock->appendNew<Const64Value>(m_proc, origin(), Signature::invalidIndex)));

            check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
                this->emitExceptionCheck(jit, ExceptionType::NullTableEntry);
            });
        }

        // Check the signature matches the value we expect.
        {
            ExpressionType expectedSignatureIndex = m_currentBlock->appendNew<Const64Value>(m_proc, origin(), SignatureInformation::get(signature));
            CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
                m_currentBlock->appendNew<Value>(m_proc, NotEqual, origin(), calleeSignatureIndex, expectedSignatureIndex));

            check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
                this->emitExceptionCheck(jit, ExceptionType::BadSignature);
            });
        }
    }

    // Do a context switch if needed.
    {
        Value* offset = m_currentBlock->appendNew<Value>(m_proc, Mul, origin(),
            calleeIndex, constant(pointerType(), sizeof(Instance*)));
        Value* newContextInstance = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(),
            m_currentBlock->appendNew<Value>(m_proc, Add, origin(), instancesBuffer, offset));

        BasicBlock* continuation = m_proc.addBlock();
        BasicBlock* doContextSwitch = m_proc.addBlock();

        Value* isSameContextInstance = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(),
            newContextInstance, instanceValue());
        m_currentBlock->appendNewControlValue(m_proc, B3::Branch, origin(),
            isSameContextInstance, FrequentedBlock(continuation), FrequentedBlock(doContextSwitch));

        PatchpointValue* patchpoint = doContextSwitch->appendNew<PatchpointValue>(m_proc, B3::Void, origin());
        patchpoint->effects.writesPinned = true;
        // We pessimistically assume we're calling something with BoundsChecking memory.
        // FIXME: We shouldn't have to do this: https://bugs.webkit.org/show_bug.cgi?id=172181
        patchpoint->clobber(PinnedRegisterInfo::get().toSave(MemoryMode::BoundsChecking));
        patchpoint->clobber(RegisterSet::macroScratchRegisters());
        patchpoint->append(newContextInstance, ValueRep::SomeRegister);
        patchpoint->append(instanceValue(), ValueRep::SomeRegister);
        patchpoint->numGPScratchRegisters = Gigacage::isEnabled(Gigacage::Primitive) ? 1 : 0;

        patchpoint->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams& params) {
            AllowMacroScratchRegisterUsage allowScratch(jit);
            GPRReg newContextInstance = params[0].gpr();
            GPRReg oldContextInstance = params[1].gpr();
            const PinnedRegisterInfo& pinnedRegs = PinnedRegisterInfo::get();
            GPRReg baseMemory = pinnedRegs.baseMemoryPointer;
            ASSERT(newContextInstance != baseMemory);
            jit.loadPtr(CCallHelpers::Address(oldContextInstance, Instance::offsetOfCachedStackLimit()), baseMemory);
            jit.storePtr(baseMemory, CCallHelpers::Address(newContextInstance, Instance::offsetOfCachedStackLimit()));
            jit.storeWasmContextInstance(newContextInstance);
            ASSERT(pinnedRegs.sizeRegister != baseMemory);
            // FIXME: We should support more than one memory size register
            //   see: https://bugs.webkit.org/show_bug.cgi?id=162952
            ASSERT(pinnedRegs.sizeRegister != newContextInstance);
            GPRReg scratchOrSize = Gigacage::isEnabled(Gigacage::Primitive) ? params.gpScratch(0) : pinnedRegs.sizeRegister;

            jit.loadPtr(CCallHelpers::Address(newContextInstance, Instance::offsetOfCachedMemorySize()), pinnedRegs.sizeRegister); // Memory size.
            jit.loadPtr(CCallHelpers::Address(newContextInstance, Instance::offsetOfCachedMemory()), baseMemory); // Memory::void*.

            jit.cageConditionally(Gigacage::Primitive, baseMemory, pinnedRegs.sizeRegister, scratchOrSize);
        });
        doContextSwitch->appendNewControlValue(m_proc, Jump, origin(), continuation);

        m_currentBlock = continuation;
    }

    ExpressionType calleeCode = m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(),
        m_currentBlock->appendNew<MemoryValue>(m_proc, Load, pointerType(), origin(), callableFunction,
            safeCast<int32_t>(WasmToWasmImportableFunction::offsetOfEntrypointLoadLocation())));

    Type returnType = signature.returnType();
    result = wasmCallingConvention().setupCall(m_proc, m_currentBlock, origin(), args, toB3Type(returnType),
        [=] (PatchpointValue* patchpoint) {
            patchpoint->effects.writesPinned = true;
            patchpoint->effects.readsPinned = true;
            // We need to clobber all potential pinned registers since we might be leaving the instance.
            // We pessimistically assume we're always calling something that is bounds checking so
            // because the wasm->wasm thunk unconditionally overrides the size registers.
            // FIXME: We should not have to do this, but the wasm->wasm stub assumes it can
            // use all the pinned registers as scratch: https://bugs.webkit.org/show_bug.cgi?id=172181
            patchpoint->clobberLate(PinnedRegisterInfo::get().toSave(MemoryMode::BoundsChecking));

            patchpoint->append(calleeCode, ValueRep::SomeRegister);
            patchpoint->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams& params) {
                AllowMacroScratchRegisterUsage allowScratch(jit);
                jit.call(params[returnType == Void ? 0 : 1].gpr(), WasmEntryPtrTag);
            });
        });

    // The call could have been to another WebAssembly instance, and / or could have modified our Memory.
    restoreWebAssemblyGlobalState(RestoreCachedStackLimit::Yes, m_info.memory, instanceValue(), m_proc, m_currentBlock);

    return { };
}

void B3IRGenerator::unify(const ExpressionType phi, const ExpressionType source)
{
    m_currentBlock->appendNew<UpsilonValue>(m_proc, origin(), source, phi);
}

void B3IRGenerator::unifyValuesWithBlock(const Stack& resultStack, const ResultList& result)
{
    ASSERT(result.size() <= resultStack.size());

    for (size_t i = 0; i < result.size(); ++i)
        unify(result[result.size() - 1 - i], resultStack.at(resultStack.size() - 1 - i));
}

void B3IRGenerator::dump(const Vector<ControlEntry>& controlStack, const Stack* expressionStack)
{
    dataLogLn("Constants:");
    for (const auto& constant : m_constantPool)
        dataLogLn(deepDump(m_proc, constant.value));

    dataLogLn("Processing Graph:");
    dataLog(m_proc);
    dataLogLn("With current block:", *m_currentBlock);
    dataLogLn("Control stack:");
    ASSERT(controlStack.size());
    for (size_t i = controlStack.size(); i--;) {
        dataLog("  ", controlStack[i].controlData, ": ");
        expressionStack->dump();
        expressionStack = &controlStack[i].enclosedExpressionStack;
        dataLogLn();
    }
    dataLogLn();
}

auto B3IRGenerator::origin() -> Origin
{
    OpcodeOrigin origin(m_parser->currentOpcode(), m_parser->currentOpcodeStartingOffset());
    ASSERT(isValidOpType(static_cast<uint8_t>(origin.opcode())));
    return bitwise_cast<Origin>(origin);
}

Expected<std::unique_ptr<InternalFunction>, String> parseAndCompile(CompilationContext& compilationContext, const uint8_t* functionStart, size_t functionLength, const Signature& signature, Vector<UnlinkedWasmToWasmCall>& unlinkedWasmToWasmCalls, unsigned& osrEntryScratchBufferSize, const ModuleInformation& info, MemoryMode mode, CompilationMode compilationMode, uint32_t functionIndex, uint32_t loopIndexForOSREntry, TierUpCount* tierUp, ThrowWasmException throwWasmException)
{
    auto result = makeUnique<InternalFunction>();

    compilationContext.embedderEntrypointJIT = makeUnique<CCallHelpers>();
    compilationContext.wasmEntrypointJIT = makeUnique<CCallHelpers>();

    Procedure procedure;

    procedure.setOriginPrinter([] (PrintStream& out, Origin origin) {
        if (origin.data())
            out.print("Wasm: ", bitwise_cast<OpcodeOrigin>(origin));
    });

    // This means we cannot use either StackmapGenerationParams::usedRegisters() or
    // StackmapGenerationParams::unavailableRegisters(). In exchange for this concession, we
    // don't strictly need to run Air::reportUsedRegisters(), which saves a bit of CPU time at
    // optLevel=1.
    procedure.setNeedsUsedRegisters(false);

    procedure.setOptLevel(compilationMode == CompilationMode::BBQMode
        ? Options::webAssemblyBBQB3OptimizationLevel()
        : Options::webAssemblyOMGOptimizationLevel());

    B3IRGenerator irGenerator(info, procedure, result.get(), unlinkedWasmToWasmCalls, osrEntryScratchBufferSize, mode, compilationMode, functionIndex, loopIndexForOSREntry, tierUp, throwWasmException);
    FunctionParser<B3IRGenerator> parser(irGenerator, functionStart, functionLength, signature, info);
    WASM_FAIL_IF_HELPER_FAILS(parser.parse());

    irGenerator.insertConstants();

    procedure.resetReachability();
    if (!ASSERT_DISABLED)
        validate(procedure, "After parsing:\n");

    dataLogIf(WasmB3IRGeneratorInternal::verbose, "Pre SSA: ", procedure);
    fixSSA(procedure);
    dataLogIf(WasmB3IRGeneratorInternal::verbose, "Post SSA: ", procedure);

    {
        B3::prepareForGeneration(procedure);
        B3::generate(procedure, *compilationContext.wasmEntrypointJIT);
        compilationContext.wasmEntrypointByproducts = procedure.releaseByproducts();
        result->entrypoint.calleeSaveRegisters = procedure.calleeSaveRegisterAtOffsetList();
    }

    return result;
}

// Custom wasm ops. These are the ones too messy to do in wasm.json.

void B3IRGenerator::emitChecksForModOrDiv(B3::Opcode operation, ExpressionType left, ExpressionType right)
{
    ASSERT(operation == Div || operation == Mod || operation == UDiv || operation == UMod);
    const B3::Type type = left->type();

    {
        CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
            m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), right, constant(type, 0)));

        check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::DivisionByZero);
        });
    }

    if (operation == Div) {
        int64_t min = type == Int32 ? std::numeric_limits<int32_t>::min() : std::numeric_limits<int64_t>::min();

        CheckValue* check = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(),
            m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
                m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), left, constant(type, min)),
                m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), right, constant(type, -1))));

        check->setGenerator([=] (CCallHelpers& jit, const B3::StackmapGenerationParams&) {
            this->emitExceptionCheck(jit, ExceptionType::IntegerOverflow);
        });
    }
}

template<>
auto B3IRGenerator::addOp<OpType::I32DivS>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = Div;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, op, origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32RemS>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = Mod;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, chill(op), origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32DivU>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = UDiv;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, op, origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32RemU>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = UMod;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, op, origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64DivS>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = Div;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, op, origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64RemS>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = Mod;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, chill(op), origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64DivU>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = UDiv;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, op, origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64RemU>(ExpressionType left, ExpressionType right, ExpressionType& result) -> PartialResult
{
    const B3::Opcode op = UMod;
    emitChecksForModOrDiv(op, left, right);
    result = m_currentBlock->appendNew<Value>(m_proc, op, origin(), left, right);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32Ctz>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int32, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.countTrailingZeros32(params[1].gpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64Ctz>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int64, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.countTrailingZeros64(params[1].gpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32Popcnt>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
#if CPU(X86_64)
    if (MacroAssembler::supportsCountPopulation()) {
        PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int32, origin());
        patchpoint->append(arg, ValueRep::SomeRegister);
        patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
            jit.countPopulation32(params[1].gpr(), params[0].gpr());
        });
        patchpoint->effects = Effects::none();
        result = patchpoint;
        return { };
    }
#endif

    uint32_t (*popcount)(int32_t) = [] (int32_t value) -> uint32_t { return __builtin_popcount(value); };
    Value* funcAddress = m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(popcount, B3CCallPtrTag));
    result = m_currentBlock->appendNew<CCallValue>(m_proc, Int32, origin(), Effects::none(), funcAddress, arg);
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64Popcnt>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
#if CPU(X86_64)
    if (MacroAssembler::supportsCountPopulation()) {
        PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int64, origin());
        patchpoint->append(arg, ValueRep::SomeRegister);
        patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
            jit.countPopulation64(params[1].gpr(), params[0].gpr());
        });
        patchpoint->effects = Effects::none();
        result = patchpoint;
        return { };
    }
#endif

    uint64_t (*popcount)(int64_t) = [] (int64_t value) -> uint64_t { return __builtin_popcountll(value); };
    Value* funcAddress = m_currentBlock->appendNew<ConstPtrValue>(m_proc, origin(), tagCFunctionPtr<void*>(popcount, B3CCallPtrTag));
    result = m_currentBlock->appendNew<CCallValue>(m_proc, Int64, origin(), Effects::none(), funcAddress, arg);
    return { };
}

template<>
auto B3IRGenerator::addOp<F64ConvertUI64>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Double, origin());
    if (isX86())
        patchpoint->numGPScratchRegisters = 1;
    patchpoint->clobber(RegisterSet::macroScratchRegisters());
    patchpoint->append(ConstrainedValue(arg, ValueRep::SomeRegister));
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        AllowMacroScratchRegisterUsage allowScratch(jit);
#if CPU(X86_64)
        jit.convertUInt64ToDouble(params[1].gpr(), params[0].fpr(), params.gpScratch(0));
#else
        jit.convertUInt64ToDouble(params[1].gpr(), params[0].fpr());
#endif
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::F32ConvertUI64>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Float, origin());
    if (isX86())
        patchpoint->numGPScratchRegisters = 1;
    patchpoint->clobber(RegisterSet::macroScratchRegisters());
    patchpoint->append(ConstrainedValue(arg, ValueRep::SomeRegister));
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        AllowMacroScratchRegisterUsage allowScratch(jit);
#if CPU(X86_64)
        jit.convertUInt64ToFloat(params[1].gpr(), params[0].fpr(), params.gpScratch(0));
#else
        jit.convertUInt64ToFloat(params[1].gpr(), params[0].fpr());
#endif
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::F64Nearest>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Double, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.roundTowardNearestIntDouble(params[1].fpr(), params[0].fpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::F32Nearest>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Float, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.roundTowardNearestIntFloat(params[1].fpr(), params[0].fpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::F64Trunc>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Double, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.roundTowardZeroDouble(params[1].fpr(), params[0].fpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::F32Trunc>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Float, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.roundTowardZeroFloat(params[1].fpr(), params[0].fpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32TruncSF64>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Double, bitwise_cast<uint64_t>(-static_cast<double>(std::numeric_limits<int32_t>::min())));
    Value* min = constant(Double, bitwise_cast<uint64_t>(static_cast<double>(std::numeric_limits<int32_t>::min())));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterEqual, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int32, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.truncateDoubleToInt32(params[1].fpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32TruncSF32>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Float, bitwise_cast<uint32_t>(-static_cast<float>(std::numeric_limits<int32_t>::min())));
    Value* min = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(std::numeric_limits<int32_t>::min())));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterEqual, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int32, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.truncateFloatToInt32(params[1].fpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}


template<>
auto B3IRGenerator::addOp<OpType::I32TruncUF64>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Double, bitwise_cast<uint64_t>(static_cast<double>(std::numeric_limits<int32_t>::min()) * -2.0));
    Value* min = constant(Double, bitwise_cast<uint64_t>(-1.0));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterThan, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int32, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.truncateDoubleToUint32(params[1].fpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I32TruncUF32>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(std::numeric_limits<int32_t>::min()) * static_cast<float>(-2.0)));
    Value* min = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(-1.0)));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterThan, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int32, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.truncateFloatToUint32(params[1].fpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64TruncSF64>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Double, bitwise_cast<uint64_t>(-static_cast<double>(std::numeric_limits<int64_t>::min())));
    Value* min = constant(Double, bitwise_cast<uint64_t>(static_cast<double>(std::numeric_limits<int64_t>::min())));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterEqual, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int64, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.truncateDoubleToInt64(params[1].fpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64TruncUF64>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Double, bitwise_cast<uint64_t>(static_cast<double>(std::numeric_limits<int64_t>::min()) * -2.0));
    Value* min = constant(Double, bitwise_cast<uint64_t>(-1.0));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterThan, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });

    Value* signBitConstant;
    if (isX86()) {
        // Since x86 doesn't have an instruction to convert floating points to unsigned integers, we at least try to do the smart thing if
        // the numbers are would be positive anyway as a signed integer. Since we cannot materialize constants into fprs we have b3 do it
        // so we can pool them if needed.
        signBitConstant = constant(Double, bitwise_cast<uint64_t>(static_cast<double>(std::numeric_limits<uint64_t>::max() - std::numeric_limits<int64_t>::max())));
    }
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int64, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    if (isX86()) {
        patchpoint->append(signBitConstant, ValueRep::SomeRegister);
        patchpoint->numFPScratchRegisters = 1;
    }
    patchpoint->clobber(RegisterSet::macroScratchRegisters());
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        AllowMacroScratchRegisterUsage allowScratch(jit);
        FPRReg scratch = InvalidFPRReg;
        FPRReg constant = InvalidFPRReg;
        if (isX86()) {
            scratch = params.fpScratch(0);
            constant = params[2].fpr();
        }
        jit.truncateDoubleToUint64(params[1].fpr(), params[0].gpr(), scratch, constant);
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64TruncSF32>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Float, bitwise_cast<uint32_t>(-static_cast<float>(std::numeric_limits<int64_t>::min())));
    Value* min = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(std::numeric_limits<int64_t>::min())));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterEqual, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int64, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        jit.truncateFloatToInt64(params[1].fpr(), params[0].gpr());
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

template<>
auto B3IRGenerator::addOp<OpType::I64TruncUF32>(ExpressionType arg, ExpressionType& result) -> PartialResult
{
    Value* max = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(std::numeric_limits<int64_t>::min()) * static_cast<float>(-2.0)));
    Value* min = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(-1.0)));
    Value* outOfBounds = m_currentBlock->appendNew<Value>(m_proc, BitAnd, origin(),
        m_currentBlock->appendNew<Value>(m_proc, LessThan, origin(), arg, max),
        m_currentBlock->appendNew<Value>(m_proc, GreaterThan, origin(), arg, min));
    outOfBounds = m_currentBlock->appendNew<Value>(m_proc, Equal, origin(), outOfBounds, constant(Int32, 0));
    CheckValue* trap = m_currentBlock->appendNew<CheckValue>(m_proc, Check, origin(), outOfBounds);
    trap->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams&) {
        this->emitExceptionCheck(jit, ExceptionType::OutOfBoundsTrunc);
    });

    Value* signBitConstant;
    if (isX86()) {
        // Since x86 doesn't have an instruction to convert floating points to unsigned integers, we at least try to do the smart thing if
        // the numbers would be positive anyway as a signed integer. Since we cannot materialize constants into fprs we have b3 do it
        // so we can pool them if needed.
        signBitConstant = constant(Float, bitwise_cast<uint32_t>(static_cast<float>(std::numeric_limits<uint64_t>::max() - std::numeric_limits<int64_t>::max())));
    }
    PatchpointValue* patchpoint = m_currentBlock->appendNew<PatchpointValue>(m_proc, Int64, origin());
    patchpoint->append(arg, ValueRep::SomeRegister);
    if (isX86()) {
        patchpoint->append(signBitConstant, ValueRep::SomeRegister);
        patchpoint->numFPScratchRegisters = 1;
    }
    patchpoint->clobber(RegisterSet::macroScratchRegisters());
    patchpoint->setGenerator([=] (CCallHelpers& jit, const StackmapGenerationParams& params) {
        AllowMacroScratchRegisterUsage allowScratch(jit);
        FPRReg scratch = InvalidFPRReg;
        FPRReg constant = InvalidFPRReg;
        if (isX86()) {
            scratch = params.fpScratch(0);
            constant = params[2].fpr();
        }
        jit.truncateFloatToUint64(params[1].fpr(), params[0].gpr(), scratch, constant);
    });
    patchpoint->effects = Effects::none();
    result = patchpoint;
    return { };
}

} } // namespace JSC::Wasm

#include "WasmB3IRGeneratorInlines.h"

#endif // ENABLE(WEBASSEMBLY)
