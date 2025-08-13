/*
 * Copyright (C) 2011-2018 Apple Inc. All rights reserved.
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
#include "DFGCFAPhase.h"

#if ENABLE(DFG_JIT)

#include "DFGAbstractInterpreterInlines.h"
#include "DFGBlockSet.h"
#include "DFGClobberSet.h"
#include "DFGClobberize.h"
#include "DFGGraph.h"
#include "DFGInPlaceAbstractState.h"
#include "DFGPhase.h"
#include "DFGSafeToExecute.h"
#include "OperandsInlines.h"
#include "JSCInlines.h"

namespace JSC { namespace DFG {

class CFAPhase : public Phase {
public:
    CFAPhase(Graph& graph)
        : Phase(graph, "control flow analysis"_s)
        , m_state(graph)
        , m_interpreter(graph, m_state)
        , m_verbose(Options::verboseCFA())
    {
    }

    bool run()
    {
        ASSERT(m_graph.m_form == ThreadedCPS || m_graph.m_form == SSA);
        ASSERT(m_graph.m_unificationState == GloballyUnified);
        ASSERT(m_graph.m_refCountState == EverythingIsLive || (Options::validateAbstractInterpreterState() && m_graph.m_refCountState == ExactRefCount));

        m_count = 0;

        dataLogIf((m_verbose && !shouldDumpGraphAtEachPhase(m_graph.m_plan.mode())), "Graph before CFA:\n", m_graph);

        // This implements a pseudo-worklist-based forward CFA, except that the visit order
        // of blocks is the bytecode program order (which is nearly topological), and
        // instead of a worklist we just walk all basic blocks checking if cfaShouldRevisit
        // is set to true. This is likely to balance the efficiency properties of both
        // worklist-based and forward fixpoint-based approaches. Like a worklist-based
        // approach, it won't visit code if it's meaningless to do so (nothing changed at
        // the head of the block or the predecessors have not been visited). Like a forward
        // fixpoint-based approach, it has a high probability of only visiting a block
        // after all predecessors have been visited. Only loops will cause this analysis to
        // revisit blocks, and the amount of revisiting is proportional to loop depth.

        m_state.initialize();

        if (m_graph.m_form != SSA) {
            dataLogLnIf(m_verbose, "   Widening state at OSR entry block.");

            // Widen the abstract values at the block that serves as the must-handle OSR entry.
            for (BlockIndex blockIndex = m_graph.numBlocks(); blockIndex--;) {
                BasicBlock* block = m_graph.block(blockIndex);
                if (!block)
                    continue;

                if (!block->isOSRTarget)
                    continue;
                if (block->bytecodeBegin != m_graph.m_plan.osrEntryBytecodeIndex())
                    continue;

                // We record that the block needs some OSR stuff, but we don't do that yet. We want to
                // handle OSR entry data at the right time in order to get the best compile times. If we
                // simply injected OSR data right now, then we'd potentially cause a loop body to be
                // interpreted with just the constants we feed it, which is more expensive than if we
                // interpreted it with non-constant values. If we always injected this data after the
                // main pass of CFA ran, then we would potentially spend a bunch of time rerunning CFA
                // after convergence. So, we try very hard to inject OSR data for a block when we first
                // naturally come to see it - see the m_blocksWithOSR check in performBlockCFA(). This
                // way, we:
                //
                // - Reduce the likelihood of interpreting the block with constants, since we will inject
                //   the OSR entry constants on top of whatever abstract values we got for that block on
                //   the first pass. The mix of those two things is likely to not be constant.
                //
                // - Reduce the total number of CFA reexecutions since we inject the OSR data as part of
                //   the normal flow of CFA instead of having to do a second fixpoint. We may still have
                //   to do a second fixpoint if we don't even reach the OSR entry block during the main
                //   run of CFA, but in that case at least we're not being redundant.
                m_blocksWithOSR.add(block);
            }
        }

        do {
            m_changed = false;
            performForwardCFA();
        } while (m_changed);

        if (m_graph.m_form != SSA) {
            for (BlockIndex blockIndex = m_graph.numBlocks(); blockIndex--;) {
                BasicBlock* block = m_graph.block(blockIndex);
                if (!block)
                    continue;

                if (m_blocksWithOSR.remove(block))
                    m_changed |= injectOSR(block);
            }

            while (m_changed) {
                m_changed = false;
                performForwardCFA();
            }

            // Make sure we record the intersection of all proofs that we ever allowed the
            // compiler to rely upon.
            for (BlockIndex blockIndex = m_graph.numBlocks(); blockIndex--;) {
                BasicBlock* block = m_graph.block(blockIndex);
                if (!block)
                    continue;

                block->intersectionOfCFAHasVisited &= block->cfaHasVisited;
                for (unsigned i = block->intersectionOfPastValuesAtHead.size(); i--;) {
                    AbstractValue value = block->valuesAtHead[i];
                    // We need to guarantee that when we do an OSR entry, we validate the incoming
                    // value as if it could be live past an invalidation point. Otherwise, we may
                    // OSR enter with a value with the wrong structure, and an InvalidationPoint's
                    // promise of filtering the structure set of certain values is no longer upheld.
                    value.m_structure.observeInvalidationPoint();
                    block->intersectionOfPastValuesAtHead[i].filter(value);
                }
            }
        }

        return true;
    }

private:
    bool injectOSR(BasicBlock* block)
    {
        dataLogLnIf(m_verbose, "   Found must-handle block: ", *block);

        // This merges snapshot of stack values while CFA phase want to have proven types and values. This is somewhat tricky.
        // But this is OK as long as DFG OSR entry validates the inputs with *proven* AbstractValue values. And it turns out that this
        // type widening is critical to navier-stokes. Without it, navier-stokes has more strict constraint on OSR entry and
        // fails OSR entry repeatedly.
        bool changed = false;
        const Operands<std::optional<JSValue>>& mustHandleValues = m_graph.m_plan.mustHandleValues();
        for (size_t i = mustHandleValues.size(); i--;) {
            Operand operand = mustHandleValues.operandForIndex(i);
            std::optional<JSValue> value = mustHandleValues[i];
            if (!value) {
                dataLogLnIf(m_verbose, "   Not live in bytecode: ", operand);
                continue;
            }
            Node* node = block->variablesAtHead.operand(operand);
            if (!node) {
                dataLogLnIf(m_verbose, "   Not live: ", operand);
                continue;
            }

            dataLogLnIf(m_verbose, "   Widening ", operand, " with ", value.value());

            AbstractValue& target = block->valuesAtHead.operand(operand);
            changed |= target.mergeOSREntryValue(m_graph, value.value(), node->variableAccessData(), node);
        }

        if (changed || !block->cfaHasVisited) {
            block->cfaShouldRevisit = true;
            return true;
        }

        return false;
    }

    void performBlockCFA(BasicBlock* block)
    {
        if (!block)
            return;
        if (!block->cfaShouldRevisit)
            return;
        dataLogLnIf(m_verbose, "   Block ", *block, ":");

        if (m_blocksWithOSR.remove(block))
            injectOSR(block);

        m_state.beginBasicBlock(block);
        if (UNLIKELY(m_verbose)) {
            dataLogLn("      head vars: ", block->valuesAtHead);
            if (m_graph.m_form == SSA)
                dataLogLn("      head regs: ", nodeValuePairListDump(block->ssa->valuesAtHead));
        }
        for (unsigned i = 0; i < block->size(); ++i) {
            Node* node = block->at(i);
            if (UNLIKELY(m_verbose)) {
                WTF::dataFile().atomically([&](auto&) {
                    dataLog("      ", Graph::opName(node->op()), " @", node->index(), ": ");
                if (!safeToExecute(m_state, m_graph, node))
                    dataLog("(UNSAFE) ");
                dataLog(m_state.variablesForDebugging(), " ", m_interpreter);
                    dataLogLn();
                });
            }
            if (!m_interpreter.execute(i)) {
                dataLogLnIf(m_verbose, "         Expect OSR exit.");
                break;
            }

            if (ASSERT_ENABLED
                && m_state.didClobberOrFolded() != writesOverlap(m_graph, node, JSCell_structureID))
                DFG_CRASH(m_graph, node, toCString("AI-clobberize disagreement; AI says ", m_state.clobberState(), " while clobberize says ", writeSet(m_graph, node)).data());
        }
        if (m_verbose) {
            WTF::dataFile().atomically([&](auto&) {
                dataLog("      tail regs: ");
            m_interpreter.dump(WTF::dataFile());
                dataLogLn();
            });
        }
        m_changed |= m_state.endBasicBlock();

        if (m_verbose) {
            WTF::dataFile().atomically([&](auto&) {
                dataLogLn("      tail vars: ", block->valuesAtTail);
            if (m_graph.m_form == SSA)
                    dataLogLn("      head regs: ", nodeValuePairListDump(block->ssa->valuesAtTail));
            });
        }
    }

    void performForwardCFA()
    {
        ++m_count;
        dataLogLnIf(m_verbose, "CFA [", m_count, "]");

        for (BlockIndex blockIndex = 0; blockIndex < m_graph.numBlocks(); ++blockIndex)
            performBlockCFA(m_graph.block(blockIndex));
    }

private:
    InPlaceAbstractState m_state;
    AbstractInterpreter<InPlaceAbstractState> m_interpreter;
    BlockSet m_blocksWithOSR;

    const bool m_verbose;

    bool m_changed;
    unsigned m_count;
};

bool performCFA(Graph& graph)
{
    return runPhase<CFAPhase>(graph);
}

} } // namespace JSC::DFG

#endif // ENABLE(DFG_JIT)
