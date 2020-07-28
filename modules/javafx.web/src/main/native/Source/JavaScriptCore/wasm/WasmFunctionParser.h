/*
 * Copyright (C) 2016-2017 Apple Inc. All rights reserved.
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

#if ENABLE(WEBASSEMBLY)

#include "WasmParser.h"
#include "WasmSignatureInlines.h"
#include <wtf/DataLog.h>

namespace JSC { namespace Wasm {

enum class BlockType {
    If,
    Block,
    Loop,
    TopLevel
};

template<typename Context>
class FunctionParser : public Parser<void> {
public:
    using ExpressionType = typename Context::ExpressionType;
    using ControlType = typename Context::ControlType;
    using ExpressionList = typename Context::ExpressionList;
    using Stack = typename Context::Stack;

    FunctionParser(Context&, const uint8_t* functionStart, size_t functionLength, const Signature&, const ModuleInformation&);

    Result WARN_UNUSED_RETURN parse();

    struct ControlEntry {
        Stack enclosedExpressionStack;
        ControlType controlData;
    };

    OpType currentOpcode() const { return m_currentOpcode; }
    size_t currentOpcodeStartingOffset() const { return m_currentOpcodeStartingOffset; }

private:
    static const bool verbose = false;

    PartialResult WARN_UNUSED_RETURN parseBody();
    PartialResult WARN_UNUSED_RETURN parseExpression();
    PartialResult WARN_UNUSED_RETURN parseUnreachableExpression();
    PartialResult WARN_UNUSED_RETURN unifyControl(Vector<ExpressionType>&, unsigned level);

#define WASM_TRY_POP_EXPRESSION_STACK_INTO(result, what) do {                               \
        WASM_PARSER_FAIL_IF(m_expressionStack.isEmpty(), "can't pop empty stack in " what); \
        result = m_expressionStack.takeLast();                                              \
        m_toKillAfterExpression.append(result);                                             \
    } while (0)

    template<OpType>
    PartialResult WARN_UNUSED_RETURN unaryCase();

    template<OpType>
    PartialResult WARN_UNUSED_RETURN binaryCase();

#define WASM_TRY_ADD_TO_CONTEXT(add_expression) WASM_FAIL_IF_HELPER_FAILS(m_context.add_expression)

    // FIXME add a macro as above for WASM_TRY_APPEND_TO_CONTROL_STACK https://bugs.webkit.org/show_bug.cgi?id=165862

    Context& m_context;
    Stack m_expressionStack;
    Vector<ControlEntry> m_controlStack;
    const Signature& m_signature;
    const ModuleInformation& m_info;

    OpType m_currentOpcode;
    size_t m_currentOpcodeStartingOffset { 0 };

    Vector<ExpressionType, 8> m_toKillAfterExpression;

    unsigned m_unreachableBlocks { 0 };
    unsigned m_loopIndex { 0 };
};

template<typename Context>
FunctionParser<Context>::FunctionParser(Context& context, const uint8_t* functionStart, size_t functionLength, const Signature& signature, const ModuleInformation& info)
    : Parser(functionStart, functionLength)
    , m_context(context)
    , m_expressionStack(context.createStack())
    , m_signature(signature)
    , m_info(info)
{
    if (verbose)
        dataLogLn("Parsing function starting at: ", (uintptr_t)functionStart, " of length: ", functionLength, " with signature: ", signature);
    m_context.setParser(this);
}

template<typename Context>
auto FunctionParser<Context>::parse() -> Result
{
    uint32_t localGroupsCount;

    WASM_PARSER_FAIL_IF(!m_context.addArguments(m_signature), "can't add ", m_signature.argumentCount(), " arguments to Function");
    WASM_PARSER_FAIL_IF(!parseVarUInt32(localGroupsCount), "can't get local groups count");

    uint64_t totalNumberOfLocals = m_signature.argumentCount();
    for (uint32_t i = 0; i < localGroupsCount; ++i) {
        uint32_t numberOfLocals;
        Type typeOfLocal;

        WASM_PARSER_FAIL_IF(!parseVarUInt32(numberOfLocals), "can't get Function's number of locals in group ", i);
        totalNumberOfLocals += numberOfLocals;
        WASM_PARSER_FAIL_IF(totalNumberOfLocals > maxFunctionLocals, "Function's number of locals is too big ", totalNumberOfLocals, " maximum ", maxFunctionLocals);
        WASM_PARSER_FAIL_IF(!parseValueType(typeOfLocal), "can't get Function local's type in group ", i);
        WASM_TRY_ADD_TO_CONTEXT(addLocal(typeOfLocal, numberOfLocals));
    }

    WASM_FAIL_IF_HELPER_FAILS(parseBody());

    return { };
}

template<typename Context>
auto FunctionParser<Context>::parseBody() -> PartialResult
{
    m_controlStack.append({ m_context.createStack(), m_context.addTopLevel(m_signature.returnType()) });
    uint8_t op;
    while (m_controlStack.size()) {
        ASSERT(m_toKillAfterExpression.isEmpty());

        m_currentOpcodeStartingOffset = m_offset;
        WASM_PARSER_FAIL_IF(!parseUInt8(op), "can't decode opcode");
        WASM_PARSER_FAIL_IF(!isValidOpType(op), "invalid opcode ", op);

        m_currentOpcode = static_cast<OpType>(op);

        if (verbose) {
            dataLogLn("processing op (", m_unreachableBlocks, "): ",  RawPointer(reinterpret_cast<void*>(op)), ", ", makeString(static_cast<OpType>(op)), " at offset: ", RawPointer(reinterpret_cast<void*>(m_offset)));
            m_context.dump(m_controlStack, &m_expressionStack);
        }

        if (m_unreachableBlocks)
            WASM_FAIL_IF_HELPER_FAILS(parseUnreachableExpression());
        else {
            WASM_FAIL_IF_HELPER_FAILS(parseExpression());
            while (m_toKillAfterExpression.size())
                m_context.didKill(m_toKillAfterExpression.takeLast());
        }
    }

    ASSERT(op == OpType::End);
    return { };
}

template<typename Context>
template<OpType op>
auto FunctionParser<Context>::binaryCase() -> PartialResult
{
    ExpressionType right;
    ExpressionType left;
    ExpressionType result;

    WASM_TRY_POP_EXPRESSION_STACK_INTO(right, "binary right");
    WASM_TRY_POP_EXPRESSION_STACK_INTO(left, "binary left");
    WASM_TRY_ADD_TO_CONTEXT(template addOp<op>(left, right, result));

    m_expressionStack.append(result);
    return { };
}

template<typename Context>
template<OpType op>
auto FunctionParser<Context>::unaryCase() -> PartialResult
{
    ExpressionType value;
    ExpressionType result;

    WASM_TRY_POP_EXPRESSION_STACK_INTO(value, "unary");
    WASM_TRY_ADD_TO_CONTEXT(template addOp<op>(value, result));

    m_expressionStack.append(result);
    return { };
}

template<typename Context>
auto FunctionParser<Context>::parseExpression() -> PartialResult
{
    switch (m_currentOpcode) {
#define CREATE_CASE(name, id, b3op, inc) case OpType::name: return binaryCase<OpType::name>();
    FOR_EACH_WASM_BINARY_OP(CREATE_CASE)
#undef CREATE_CASE

#define CREATE_CASE(name, id, b3op, inc) case OpType::name: return unaryCase<OpType::name>();
    FOR_EACH_WASM_UNARY_OP(CREATE_CASE)
#undef CREATE_CASE

    case Select: {
        ExpressionType condition;
        ExpressionType zero;
        ExpressionType nonZero;

        WASM_TRY_POP_EXPRESSION_STACK_INTO(condition, "select condition");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(zero, "select zero");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(nonZero, "select non-zero");

        ExpressionType result;
        WASM_TRY_ADD_TO_CONTEXT(addSelect(condition, nonZero, zero, result));

        m_expressionStack.append(result);
        return { };
    }

#define CREATE_CASE(name, id, b3op, inc) case OpType::name:
    FOR_EACH_WASM_MEMORY_LOAD_OP(CREATE_CASE) {
        uint32_t alignment;
        uint32_t offset;
        ExpressionType pointer;
        ExpressionType result;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(alignment), "can't get load alignment");
        WASM_PARSER_FAIL_IF(alignment > memoryLog2Alignment(m_currentOpcode), "byte alignment ", 1ull << alignment, " exceeds load's natural alignment ", 1ull << memoryLog2Alignment(m_currentOpcode));
        WASM_PARSER_FAIL_IF(!parseVarUInt32(offset), "can't get load offset");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(pointer, "load pointer");
        WASM_TRY_ADD_TO_CONTEXT(load(static_cast<LoadOpType>(m_currentOpcode), pointer, result, offset));
        m_expressionStack.append(result);
        return { };
    }

    FOR_EACH_WASM_MEMORY_STORE_OP(CREATE_CASE) {
        uint32_t alignment;
        uint32_t offset;
        ExpressionType value;
        ExpressionType pointer;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(alignment), "can't get store alignment");
        WASM_PARSER_FAIL_IF(alignment > memoryLog2Alignment(m_currentOpcode), "byte alignment ", 1ull << alignment, " exceeds store's natural alignment ", 1ull << memoryLog2Alignment(m_currentOpcode));
        WASM_PARSER_FAIL_IF(!parseVarUInt32(offset), "can't get store offset");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(value, "store value");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(pointer, "store pointer");
        WASM_TRY_ADD_TO_CONTEXT(store(static_cast<StoreOpType>(m_currentOpcode), pointer, value, offset));
        return { };
    }
#undef CREATE_CASE

    case F32Const: {
        uint32_t constant;
        WASM_PARSER_FAIL_IF(!parseUInt32(constant), "can't parse 32-bit floating-point constant");
        m_expressionStack.append(m_context.addConstant(F32, constant));
        return { };
    }

    case I32Const: {
        int32_t constant;
        WASM_PARSER_FAIL_IF(!parseVarInt32(constant), "can't parse 32-bit constant");
        m_expressionStack.append(m_context.addConstant(I32, constant));
        return { };
    }

    case F64Const: {
        uint64_t constant;
        WASM_PARSER_FAIL_IF(!parseUInt64(constant), "can't parse 64-bit floating-point constant");
        m_expressionStack.append(m_context.addConstant(F64, constant));
        return { };
    }

    case I64Const: {
        int64_t constant;
        WASM_PARSER_FAIL_IF(!parseVarInt64(constant), "can't parse 64-bit constant");
        m_expressionStack.append(m_context.addConstant(I64, constant));
        return { };
    }

    case TableGet: {
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        unsigned tableIndex;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(tableIndex), "can't parse table index");
        ExpressionType result, index;
        WASM_TRY_POP_EXPRESSION_STACK_INTO(index, "table.get");
        WASM_TRY_ADD_TO_CONTEXT(addTableGet(tableIndex, index, result));
        m_expressionStack.append(result);
        return { };
    }

    case TableSet: {
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        unsigned tableIndex;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(tableIndex), "can't parse table index");
        ExpressionType val, index;
        WASM_TRY_POP_EXPRESSION_STACK_INTO(val, "table.set");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(index, "table.set");
        WASM_TRY_ADD_TO_CONTEXT(addTableSet(tableIndex, index, val));
        return { };
    }

    case ExtTable: {
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        uint8_t extOp;
        WASM_PARSER_FAIL_IF(!parseUInt8(extOp), "can't parse table extended opcode");
        unsigned tableIndex;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(tableIndex), "can't parse table index");

        switch (static_cast<ExtTableOpType>(extOp)) {
        case ExtTableOpType::TableSize: {
            ExpressionType result;
            WASM_TRY_ADD_TO_CONTEXT(addTableSize(tableIndex, result));
            m_expressionStack.append(result);
            break;
        }
        case ExtTableOpType::TableGrow: {
            ExpressionType fill, delta, result;
            WASM_TRY_POP_EXPRESSION_STACK_INTO(delta, "table.grow");
            WASM_TRY_POP_EXPRESSION_STACK_INTO(fill, "table.grow");
            WASM_TRY_ADD_TO_CONTEXT(addTableGrow(tableIndex, fill, delta, result));
            m_expressionStack.append(result);
            break;
        }
        case ExtTableOpType::TableFill: {
            ExpressionType offset, fill, count;
            WASM_TRY_POP_EXPRESSION_STACK_INTO(count, "table.fill");
            WASM_TRY_POP_EXPRESSION_STACK_INTO(fill, "table.fill");
            WASM_TRY_POP_EXPRESSION_STACK_INTO(offset, "table.fill");
            WASM_TRY_ADD_TO_CONTEXT(addTableFill(tableIndex, offset, fill, count));
            break;
        }
        default:
            WASM_PARSER_FAIL_IF(true, "invalid extended table op ", extOp);
            break;
        }
        return { };
    }

    case RefNull: {
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        m_expressionStack.append(m_context.addConstant(Funcref, JSValue::encode(jsNull())));
        return { };
    }

    case RefIsNull: {
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        ExpressionType result, value;
        WASM_TRY_POP_EXPRESSION_STACK_INTO(value, "ref.is_null");
        WASM_TRY_ADD_TO_CONTEXT(addRefIsNull(value, result));
        m_expressionStack.append(result);
        return { };
    }

    case RefFunc: {
        uint32_t index;
        ExpressionType result;
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        WASM_PARSER_FAIL_IF(!parseVarUInt32(index), "can't get index for ref.func");

        WASM_TRY_ADD_TO_CONTEXT(addRefFunc(index, result));
        m_expressionStack.append(result);
        return { };
    }

    case GetLocal: {
        uint32_t index;
        ExpressionType result;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(index), "can't get index for get_local");
        WASM_TRY_ADD_TO_CONTEXT(getLocal(index, result));
        m_expressionStack.append(result);
        return { };
    }

    case SetLocal: {
        uint32_t index;
        ExpressionType value;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(index), "can't get index for set_local");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(value, "set_local");
        WASM_TRY_ADD_TO_CONTEXT(setLocal(index, value));
        return { };
    }

    case TeeLocal: {
        uint32_t index;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(index), "can't get index for tee_local");
        WASM_PARSER_FAIL_IF(m_expressionStack.isEmpty(), "can't tee_local on empty expression stack");
        WASM_TRY_ADD_TO_CONTEXT(setLocal(index, m_expressionStack.last()));
        return { };
    }

    case GetGlobal: {
        uint32_t index;
        ExpressionType result;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(index), "can't get get_global's index");
        WASM_TRY_ADD_TO_CONTEXT(getGlobal(index, result));
        m_expressionStack.append(result);
        return { };
    }

    case SetGlobal: {
        uint32_t index;
        ExpressionType value;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(index), "can't get set_global's index");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(value, "set_global value");
        WASM_TRY_ADD_TO_CONTEXT(setGlobal(index, value));
        return { };
    }

    case Call: {
        uint32_t functionIndex;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(functionIndex), "can't parse call's function index");
        WASM_PARSER_FAIL_IF(functionIndex >= m_info.functionIndexSpaceSize(), "call function index ", functionIndex, " exceeds function index space ", m_info.functionIndexSpaceSize());

        SignatureIndex calleeSignatureIndex = m_info.signatureIndexFromFunctionIndexSpace(functionIndex);
        const Signature& calleeSignature = SignatureInformation::get(calleeSignatureIndex);
        WASM_PARSER_FAIL_IF(calleeSignature.argumentCount() > m_expressionStack.size(), "call function index ", functionIndex, " has ", calleeSignature.argumentCount(), " arguments, but the expression stack currently holds ", m_expressionStack.size(), " values");

        size_t firstArgumentIndex = m_expressionStack.size() - calleeSignature.argumentCount();
        Vector<ExpressionType> args;
        WASM_PARSER_FAIL_IF(!args.tryReserveCapacity(calleeSignature.argumentCount()), "can't allocate enough memory for call's ", calleeSignature.argumentCount(), " arguments");
        for (size_t i = firstArgumentIndex; i < m_expressionStack.size(); ++i)
            args.uncheckedAppend(m_expressionStack.at(i));
        m_expressionStack.shrink(firstArgumentIndex);

        ExpressionType result = Context::emptyExpression();
        WASM_TRY_ADD_TO_CONTEXT(addCall(functionIndex, calleeSignature, args, result));

        if (result != Context::emptyExpression())
            m_expressionStack.append(result);

        return { };
    }

    case CallIndirect: {
        uint32_t signatureIndex;
        uint32_t tableIndex;
        WASM_PARSER_FAIL_IF(!m_info.tableCount(), "call_indirect is only valid when a table is defined or imported");
        WASM_PARSER_FAIL_IF(!parseVarUInt32(signatureIndex), "can't get call_indirect's signature index");
        WASM_PARSER_FAIL_IF(!parseVarUInt32(tableIndex), "can't get call_indirect's table index");
        WASM_PARSER_FAIL_IF(tableIndex >= m_info.tableCount(), "call_indirect's table index ", tableIndex, " invalid, limit is ", m_info.tableCount());
        WASM_PARSER_FAIL_IF(m_info.usedSignatures.size() <= signatureIndex, "call_indirect's signature index ", signatureIndex, " exceeds known signatures ", m_info.usedSignatures.size());
        WASM_PARSER_FAIL_IF(m_info.tables[tableIndex].type() != TableElementType::Funcref, "call_indirect is only valid when a table has type funcref");

        const Signature& calleeSignature = m_info.usedSignatures[signatureIndex].get();
        size_t argumentCount = calleeSignature.argumentCount() + 1; // Add the callee's index.
        WASM_PARSER_FAIL_IF(argumentCount > m_expressionStack.size(), "call_indirect expects ", argumentCount, " arguments, but the expression stack currently holds ", m_expressionStack.size(), " values");

        Vector<ExpressionType> args;
        WASM_PARSER_FAIL_IF(!args.tryReserveCapacity(argumentCount), "can't allocate enough memory for ", argumentCount, " call_indirect arguments");
        size_t firstArgumentIndex = m_expressionStack.size() - argumentCount;
        for (size_t i = firstArgumentIndex; i < m_expressionStack.size(); ++i)
            args.uncheckedAppend(m_expressionStack.at(i));
        m_expressionStack.shrink(firstArgumentIndex);

        ExpressionType result = Context::emptyExpression();
        WASM_TRY_ADD_TO_CONTEXT(addCallIndirect(tableIndex, calleeSignature, args, result));

        if (result != Context::emptyExpression())
            m_expressionStack.append(result);

        return { };
    }

    case Block: {
        Type inlineSignature;
        WASM_PARSER_FAIL_IF(!parseResultType(inlineSignature), "can't get block's inline signature");
        m_controlStack.append({ WTFMove(m_expressionStack), m_context.addBlock(inlineSignature) });
        m_expressionStack = m_context.createStack();
        return { };
    }

    case Loop: {
        Type inlineSignature;
        WASM_PARSER_FAIL_IF(!parseResultType(inlineSignature), "can't get loop's inline signature");
        auto expressionStack = WTFMove(m_expressionStack);
        auto loop = m_context.addLoop(inlineSignature, expressionStack, m_loopIndex++);
        m_controlStack.append({ expressionStack, loop });
        m_expressionStack = m_context.createStack();
        return { };
    }

    case If: {
        Type inlineSignature;
        ExpressionType condition;
        ControlType control;
        WASM_PARSER_FAIL_IF(!parseResultType(inlineSignature), "can't get if's inline signature");
        WASM_TRY_POP_EXPRESSION_STACK_INTO(condition, "if condition");
        WASM_TRY_ADD_TO_CONTEXT(addIf(condition, inlineSignature, control));
        m_controlStack.append({ WTFMove(m_expressionStack), control });
        m_expressionStack = m_context.createStack();
        return { };
    }

    case Else: {
        WASM_PARSER_FAIL_IF(m_controlStack.size() == 1, "can't use else block at the top-level of a function");
        WASM_TRY_ADD_TO_CONTEXT(addElse(m_controlStack.last().controlData, m_expressionStack));
        m_expressionStack.shrink(0);
        return { };
    }

    case Br:
    case BrIf: {
        uint32_t target;
        ExpressionType condition = Context::emptyExpression();
        WASM_PARSER_FAIL_IF(!parseVarUInt32(target), "can't get br / br_if's target");
        WASM_PARSER_FAIL_IF(target >= m_controlStack.size(), "br / br_if's target ", target, " exceeds control stack size ", m_controlStack.size());
        if (m_currentOpcode == BrIf)
            WASM_TRY_POP_EXPRESSION_STACK_INTO(condition, "br / br_if condition");
        else
            m_unreachableBlocks = 1;

        ControlType& data = m_controlStack[m_controlStack.size() - 1 - target].controlData;

        WASM_TRY_ADD_TO_CONTEXT(addBranch(data, condition, m_expressionStack));
        return { };
    }

    case BrTable: {
        uint32_t numberOfTargets;
        uint32_t defaultTarget;
        ExpressionType condition;
        Vector<ControlType*> targets;

        WASM_PARSER_FAIL_IF(!parseVarUInt32(numberOfTargets), "can't get the number of targets for br_table");
        WASM_PARSER_FAIL_IF(numberOfTargets == std::numeric_limits<uint32_t>::max(), "br_table's number of targets is too big ", numberOfTargets);

        WASM_PARSER_FAIL_IF(!targets.tryReserveCapacity(numberOfTargets), "can't allocate memory for ", numberOfTargets, " br_table targets");
        for (uint32_t i = 0; i < numberOfTargets; ++i) {
            uint32_t target;
            WASM_PARSER_FAIL_IF(!parseVarUInt32(target), "can't get ", i, "th target for br_table");
            WASM_PARSER_FAIL_IF(target >= m_controlStack.size(), "br_table's ", i, "th target ", target, " exceeds control stack size ", m_controlStack.size());
            targets.uncheckedAppend(&m_controlStack[m_controlStack.size() - 1 - target].controlData);
        }

        WASM_PARSER_FAIL_IF(!parseVarUInt32(defaultTarget), "can't get default target for br_table");
        WASM_PARSER_FAIL_IF(defaultTarget >= m_controlStack.size(), "br_table's default target ", defaultTarget, " exceeds control stack size ", m_controlStack.size());

        WASM_TRY_POP_EXPRESSION_STACK_INTO(condition, "br_table condition");
        WASM_TRY_ADD_TO_CONTEXT(addSwitch(condition, targets, m_controlStack[m_controlStack.size() - 1 - defaultTarget].controlData, m_expressionStack));

        m_unreachableBlocks = 1;
        return { };
    }

    case Return: {
        ExpressionList returnValues;
        if (m_signature.returnType() != Void) {
            ExpressionType returnValue;
            WASM_TRY_POP_EXPRESSION_STACK_INTO(returnValue, "return");
            returnValues.append(returnValue);
        }

        WASM_TRY_ADD_TO_CONTEXT(addReturn(m_controlStack[0].controlData, returnValues));
        m_unreachableBlocks = 1;
        return { };
    }

    case End: {
        ControlEntry data = m_controlStack.takeLast();
        // FIXME: This is a little weird in that it will modify the expressionStack for the result of the block.
        // That's a little too effectful for me but I don't have a better API right now.
        // see: https://bugs.webkit.org/show_bug.cgi?id=164353
        WASM_TRY_ADD_TO_CONTEXT(endBlock(data, m_expressionStack));
        m_expressionStack.swap(data.enclosedExpressionStack);
        return { };
    }

    case Unreachable: {
        WASM_TRY_ADD_TO_CONTEXT(addUnreachable());
        m_unreachableBlocks = 1;
        return { };
    }

    case Drop: {
        WASM_PARSER_FAIL_IF(!m_expressionStack.size(), "can't drop on empty stack");
        auto expression = m_expressionStack.takeLast();
        m_toKillAfterExpression.append(expression);
        return { };
    }

    case Nop: {
        return { };
    }

    case GrowMemory: {
        WASM_PARSER_FAIL_IF(!m_info.memory, "grow_memory is only valid if a memory is defined or imported");

        uint8_t reserved;
        WASM_PARSER_FAIL_IF(!parseVarUInt1(reserved), "can't parse reserved varUint1 for grow_memory");
        WASM_PARSER_FAIL_IF(reserved != 0, "reserved varUint1 for grow_memory must be zero");

        ExpressionType delta;
        WASM_TRY_POP_EXPRESSION_STACK_INTO(delta, "expect an i32 argument to grow_memory on the stack");

        ExpressionType result;
        WASM_TRY_ADD_TO_CONTEXT(addGrowMemory(delta, result));
        m_expressionStack.append(result);

        return { };
    }

    case CurrentMemory: {
        WASM_PARSER_FAIL_IF(!m_info.memory, "current_memory is only valid if a memory is defined or imported");

        uint8_t reserved;
        WASM_PARSER_FAIL_IF(!parseVarUInt1(reserved), "can't parse reserved varUint1 for current_memory");
        WASM_PARSER_FAIL_IF(reserved != 0, "reserved varUint1 for current_memory must be zero");

        ExpressionType result;
        WASM_TRY_ADD_TO_CONTEXT(addCurrentMemory(result));
        m_expressionStack.append(result);

        return { };
    }
    }

    ASSERT_NOT_REACHED();
    return { };
}

// FIXME: We should try to use the same decoder function for both unreachable and reachable code. https://bugs.webkit.org/show_bug.cgi?id=165965
template<typename Context>
auto FunctionParser<Context>::parseUnreachableExpression() -> PartialResult
{
    ASSERT(m_unreachableBlocks);
#define CREATE_CASE(name, id, b3op, inc) case OpType::name:
    switch (m_currentOpcode) {
    case Else: {
        if (m_unreachableBlocks > 1)
            return { };

        ControlEntry& data = m_controlStack.last();
        m_unreachableBlocks = 0;
        WASM_TRY_ADD_TO_CONTEXT(addElseToUnreachable(data.controlData));
        m_expressionStack.shrink(0);
        return { };
    }

    case End: {
        if (m_unreachableBlocks == 1) {
            ControlEntry data = m_controlStack.takeLast();
            WASM_TRY_ADD_TO_CONTEXT(addEndToUnreachable(data));
            m_expressionStack.swap(data.enclosedExpressionStack);
        }
        m_unreachableBlocks--;
        return { };
    }

    case Loop:
    case If:
    case Block: {
        m_unreachableBlocks++;
        Type unused;
        WASM_PARSER_FAIL_IF(!parseResultType(unused), "can't get inline type for ", m_currentOpcode, " in unreachable context");
        return { };
    }

    case BrTable: {
        uint32_t numberOfTargets;
        uint32_t unused;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(numberOfTargets), "can't get the number of targets for br_table in unreachable context");
        WASM_PARSER_FAIL_IF(numberOfTargets == std::numeric_limits<uint32_t>::max(), "br_table's number of targets is too big ", numberOfTargets);

        for (uint32_t i = 0; i < numberOfTargets; ++i)
            WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get ", i, "th target for br_table in unreachable context");

        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get default target for br_table in unreachable context");
        return { };
    }

    case CallIndirect: {
        uint32_t unused;
        uint32_t unused2;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get call_indirect's signature index in unreachable context");
        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused2), "can't get call_indirect's reserved byte in unreachable context");
        return { };
    }

    case F32Const: {
        uint32_t unused;
        WASM_PARSER_FAIL_IF(!parseUInt32(unused), "can't parse 32-bit floating-point constant");
        return { };
    }

    case F64Const: {
        uint64_t constant;
        WASM_PARSER_FAIL_IF(!parseUInt64(constant), "can't parse 64-bit floating-point constant");
        return { };
    }

    // two immediate cases
    FOR_EACH_WASM_MEMORY_LOAD_OP(CREATE_CASE)
    FOR_EACH_WASM_MEMORY_STORE_OP(CREATE_CASE) {
        uint32_t unused;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get first immediate for ", m_currentOpcode, " in unreachable context");
        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get second immediate for ", m_currentOpcode, " in unreachable context");
        return { };
    }

    // one immediate cases
    case SetLocal:
    case GetLocal:
    case TeeLocal:
    case GetGlobal:
    case SetGlobal:
    case Br:
    case BrIf:
    case Call: {
        uint32_t unused;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get immediate for ", m_currentOpcode, " in unreachable context");
        return { };
    }

    case I32Const: {
        int32_t unused;
        WASM_PARSER_FAIL_IF(!parseVarInt32(unused), "can't get immediate for ", m_currentOpcode, " in unreachable context");
        return { };
    }

    case I64Const: {
        int64_t unused;
        WASM_PARSER_FAIL_IF(!parseVarInt64(unused), "can't get immediate for ", m_currentOpcode, " in unreachable context");
        return { };
    }

    case ExtTable:
    case TableGet:
    case TableSet: {
        unsigned tableIndex;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(tableIndex), "can't parse table index");
        FALLTHROUGH;
    }
    case RefIsNull:
    case RefNull: {
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        return { };
    }

    case RefFunc: {
        uint32_t unused;
        WASM_PARSER_FAIL_IF(!parseVarUInt32(unused), "can't get immediate for ", m_currentOpcode, " in unreachable context");
        WASM_PARSER_FAIL_IF(!Options::useWebAssemblyReferences(), "references are not enabled");
        return { };
    }

    case GrowMemory:
    case CurrentMemory: {
        uint8_t reserved;
        WASM_PARSER_FAIL_IF(!parseVarUInt1(reserved), "can't parse reserved varUint1 for grow_memory/current_memory");
        return { };
    }

    // no immediate cases
    FOR_EACH_WASM_BINARY_OP(CREATE_CASE)
    FOR_EACH_WASM_UNARY_OP(CREATE_CASE)
    case Unreachable:
    case Nop:
    case Return:
    case Select:
    case Drop: {
        return { };
    }
    }
#undef CREATE_CASE
    RELEASE_ASSERT_NOT_REACHED();
}

} } // namespace JSC::Wasm

#endif // ENABLE(WEBASSEMBLY)
