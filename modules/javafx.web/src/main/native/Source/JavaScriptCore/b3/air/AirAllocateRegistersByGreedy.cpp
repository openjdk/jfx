/*
 * Copyright (C) 2024-2025 Apple Inc. All rights reserved.
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
#include "AirAllocateRegistersByGreedy.h"

#if ENABLE(B3_JIT)

#include "AirArgInlines.h"
#include "AirCode.h"
#include "AirFixSpillsAfterTerminals.h"
#include "AirPhaseInsertionSet.h"
#include "AirInstInlines.h"
#include "AirLiveness.h"
#include "AirPadInterference.h"
#include "AirPhaseScope.h"
#include "AirRegLiveness.h"
#include "AirTmpMap.h"
#include "AirTmpWidthInlines.h"
#include "AirUseCounts.h"
#include <wtf/IterationStatus.h>
#include <wtf/ListDump.h>
#include <wtf/PriorityQueue.h>
#include <wtf/Range.h>

WTF_ALLOW_UNSAFE_BUFFER_USAGE_BEGIN

namespace JSC { namespace B3 { namespace Air {

namespace Greedy {

// Experiments
static constexpr bool eagerGroups = true;
static constexpr bool eagerGroupsSplitFully = false;
static constexpr bool eagerGroupsExhaustiveSearch = false;

// Quickly filters out short ranges from live range splitting consideration.
static constexpr size_t splitMinRangeSize = 8;

static constexpr float unspillableCost = std::numeric_limits<float>::infinity();
static constexpr float fastTmpSpillCost = std::numeric_limits<float>::max();
static_assert(unspillableCost > fastTmpSpillCost);

// Phase constants used for the PhaseInsertionSet. Ensures that the fixup and spill/fill instructions
// inserted in a particular gap ends up in the correct order.
static constexpr unsigned spillStore = 0;
static constexpr unsigned splitMoveTo = 1;
static constexpr unsigned splitMoveFrom = 2;
static constexpr unsigned spillLoad = 3;

static bool verbose() { return Options::airGreedyRegAllocVerbose(); }

// Terminology / core data-structures:
//
// Point: a position within the IR stream. There are two points associated with each
// instruction: early and late. The early point for an instruction occurs immediately
// before the instruction and the late occurs immediately following. Early use/defs
// for that instruction are modeled as occurring at the early point whereas late use/defs
// are modeled as occurring at the late point. The late point for an instruction and
// early point for the subsequent instruction are distinct in order to avoid false
// conflicts, e.g. when a late use is followed by an early def.
//
// Interval: contiguous set of points, represented by a half open interval [begin, end).
//
// LiveRange: a set of intervals, usually used to model the liveness of a particular Tmp.
// In this implementation, Intervals of a LiveRange must be non-overlapping and
// non-contigous (if contigous, they should have been merged), and stored in sorted order.
//
// RegisterRange: a set of intervals, usually used to model assignments to a particular
// register at each Point. Each Interval of a RegisterRange is associated with a single
// Tmp, which may be a register Tmp (to represent fixed register lifetime and clobbers) or
// a Tmp that is currently assigned to this register. This structure models which Tmp occupies
// the register at each Point. Tmps and their associated Intervals can be assigned and
// evicted to/from the RegisterRange as the register allocation algorithm progresses.
//
// Algorithm:
//
// 1. Initialization:
//   a. Define where Points are located in the IR.
//   b. Run liveness analysis and build a LiveRange for each Tmp (including fixed registers).
//   c. Run analysis to determine the cost to spill each Tmp.
//   d. Build metadata related to Tmp coalescing. Eagerly group Tmps that can be coalesced.
// 2. Register allocation:
//   a. Process each Tmp in order of priority. Priority is mostly related to the "stage" of the
//      Tmp, whether a Tmp has a preferred register, and the size of a Tmp's LiveRange. The idea
//      is to fit the most constrained Tmps first, and let the smaller and less constrained ranges
//      fit into the remaining gaps.
//   b. Each Tmp is driven through a state machine (see Stage enum), where the further a Tmp
//      progresses, the more aggressive we try to fit it somewhere.
//     - First, simply try to find space where the LiveRange can fit in a RegisterRange.
//     - If that's not successful, we may evict LiveRanges in favor of a LiveRange with higher
//       spill cost. The evicted LiveRanges will requeued for further processing.
//     - If that's not successful, we may split up eagerly coalesced Tmps and process each
//       subgroup individually. We also try other forms of splitting LiveRanges, which will
//       produce new Tmps/LiveRanges, some of which may be assignable to registers.
//     - Finally, if all else fails, the Tmp is spilled and new Tmps for the spill/fill fixups
//       are queued for processing.
// 3. Finalization: fixup IR code is inserted to handle Tmps' split ranges and spills.
//
// Note that stack slots are allocated during a subsequent compiler phase.

using Point = size_t;
using Interval = WTF::Range<Point>;

class LiveRange {
public:
    LiveRange() = default;

    inline void validate()
    {
#if ASSERT_ENABLED
        size_t size = 0;
        Interval* prevInterval = nullptr;
        for (auto& interval : m_intervals) {
            ASSERT(interval.begin() < interval.end());
            ASSERT(!prevInterval || prevInterval->end() < interval.begin());
            size += interval.distance();
            prevInterval = &interval;
        }
        ASSERT(size == m_size);
#endif
    }

    // interval must come before all the intervals already in this LiveRange.
    void prepend(Interval interval)
    {
        ASSERT(interval);
        if (m_intervals.isEmpty() || interval.end() < m_intervals.first().begin())
            m_intervals.prepend(interval);
        else {
            ASSERT(interval.end() == m_intervals.first().begin());
            m_intervals.first() |= interval;
        }
        m_size += interval.distance();
        validate();
    }

    // interval must come after all the intervals already in this LiveRange.
    void append(Interval interval)
    {
        ASSERT(interval);
        if (m_intervals.isEmpty() || m_intervals.last().end() < interval.begin())
            m_intervals.append(interval);
        else {
            ASSERT(m_intervals.last().end() == interval.begin());
            m_intervals.last() |= interval;
        }
        m_size += interval.distance();
        validate();
    }

    void crossBasicBlockBoundary()
    {
        m_crossesBasicBlockBoundary = true;
    }

    bool isLocal() const
    {
        return !m_crossesBasicBlockBoundary && m_intervals.size() <= 1;
    }

    const Deque<Interval>& intervals() const
    {
        return m_intervals;
    }

    size_t size()
    {
        return m_size;
    }

    bool overlaps(LiveRange& other)
    {
        auto otherIter = other.intervals().begin();
        auto otherEnd = other.intervals().end();
        for (auto interval : intervals()) {
            while (otherIter != otherEnd && otherIter->end() <= interval.begin())
                ++otherIter; // otherIter was entirely before interval
            if (otherIter == otherEnd)
                return false;
            // Either otherIter overlaps interval or otherIter is entirely after interval.
            if (otherIter->begin() < interval.end())
                return true;
        }
        return false;
    }

    static LiveRange merge(const LiveRange& a, const LiveRange& b)
    {
        auto getEarliestAndAdvance = [](auto& aIter, const auto aEnd, auto& bIter, const auto bEnd) {
            auto postInc = [](auto& iter) {
                Interval interval = *iter;
                ++iter;
                return interval;
            };
            ASSERT(aIter != aEnd || bIter != bEnd);
            if (aIter == aEnd)
                return postInc(bIter);
            if (bIter == bEnd)
                return postInc(aIter);
            if (aIter->begin() < bIter->begin())
                return postInc(aIter);
            return postInc(bIter);
        };

        LiveRange result;
        auto aIter = a.intervals().begin();
        auto aEnd = a.intervals().end();
        auto bIter = b.intervals().begin();
        auto bEnd = b.intervals().end();

        Interval current = { };
        while (aIter != aEnd || bIter != bEnd) {
            Interval next = getEarliestAndAdvance(aIter, aEnd, bIter, bEnd);
            if (current.overlaps(next)) {
                current |= next;
                continue;
            }
            if (current)
                result.append(current);
            current = next;
        }
        if (current)
            result.append(current);
        result.validate();
        return result;
    }

    static LiveRange subtract(const LiveRange& a, const LiveRange& b)
    {
        LiveRange result;
        auto aIter = a.intervals().begin();
        auto bIter = b.intervals().begin();

        if (aIter == a.intervals().end())
            return result;
        Interval interval = *aIter;
        ++aIter;

        while (true) {
            // Skip over intervals in b that come before the current interval of a.
            while (bIter != b.intervals().end() && bIter->end() <= interval.begin())
                ++bIter;

            if (bIter != b.intervals().end() && bIter->overlaps(interval)) {
                // Overlap: Split the interval into 0, 1 or 2 intervals.
                if (interval.begin() < bIter->begin())
                    result.append({ interval.begin(), bIter->begin() });
                if (bIter->end() < interval.end()) {
                    // Process remaining portion of the interval.
                    interval = { bIter->end(), interval.end() };
                    continue;
                }
            } else {
                // No overlap: include entire interval in result.
                result.append(interval);
            }
            // Finished processing interval of a; move on to the next.
            if (aIter == a.intervals().end())
                break;
            interval = *aIter;
            ++aIter;
        }
        result.validate();
        return result;
    }

    void dump(PrintStream& out) const
    {
        WTF::CommaPrinter comma;
        out.print("{ ");
        for (auto& interval : intervals())
            out.print(comma, interval);
        out.print(" }[", m_size, "]");
        if (isLocal())
            out.print("(local)");
    }

private:
    Deque<Interval> m_intervals;
    size_t m_size { 0 }; // Sum of the distances over m_intervals
    bool m_crossesBasicBlockBoundary { false };
};

enum class Stage {
    New,
    Unspillable,
    TryAllocate,
    TrySplit,
    Spill,
    MaxInQueue = Spill,
    Assigned,
    Coalesced,
    Spilled,
    Replaced,
};

class TmpPriority {
private:
    // Priority is encoded into a 64-bit unsigned integer for fast comparison.
    static constexpr size_t stageBits = 3;
    static_assert(static_cast<uint64_t>(Stage::MaxInQueue) < (1ULL << stageBits));
    static constexpr size_t stageShift = 64 - stageBits;

    static constexpr size_t maybeCoalescableBits = 1;
    static constexpr size_t maybeCoalescableShift = stageShift - maybeCoalescableBits;

    static constexpr size_t isGlobalBits = 1;
    static constexpr size_t isGlobalShift = maybeCoalescableShift - isGlobalBits;

    static constexpr size_t rangeSizeBits = 39;
    static constexpr size_t rangeSizeShift = isGlobalShift - rangeSizeBits;

    static constexpr size_t tmpIndexBits = 20;
    static constexpr size_t tmpIndexShift = rangeSizeShift - tmpIndexBits;
    static_assert(!tmpIndexShift);

    inline void packPriority(uint64_t val, size_t numBits, size_t shift, bool reverse)
    {
        const uint64_t mask = (1ull << numBits) - 1;
        val &= mask;
        if (reverse)
            val = mask - val;
        m_priority |= val << shift;
    }

public:
    TmpPriority(Tmp tmp, Stage stage, size_t rangeSizeOrStart, bool maybeCoalescable, bool isGlobal)
        : m_tmp(tmp)
    {
        ASSERT(!tmp.isReg());
        ASSERT(stage <= Stage::MaxInQueue);
        // Earlier stages are higher priority.
        packPriority(static_cast<uint64_t>(stage), stageBits, stageShift, true);
        packPriority(maybeCoalescable, maybeCoalescableBits, maybeCoalescableShift, false);
        packPriority(isGlobal, isGlobalBits, isGlobalShift, false);
        // If range is global, then rangeSizeOrStart is size, and larger ranges are higher priority.
        // If range is local, then rangeSizeOrStart is start, and earlier ranges are higher priority.
        packPriority(rangeSizeOrStart, rangeSizeBits, rangeSizeShift, !isGlobal);
        // Make a strict total order for determinism.
        packPriority(tmp.tmpIndex(), tmpIndexBits, tmpIndexShift, true);
    }

    Tmp tmp() { return m_tmp; }

    Stage stage() const
    {
        const uint64_t mask = (1 << stageBits) - 1;
        uint64_t stageReversed = m_priority >> stageShift;
        stageReversed &= mask;
        return static_cast<Stage>(mask - stageReversed);
    }

    void dump(PrintStream& out) const
    {
        out.print("<", m_tmp, ", ", WTF::RawHex(m_priority), ">");
    }

    static bool isHigherPriority(const TmpPriority& left, const TmpPriority& right)
    {
        return left.m_priority > right.m_priority;
    }

private:
    Tmp m_tmp;
    uint64_t m_priority { 0 };
};

class RegisterRange {
public:
    RegisterRange() = default;

    struct AllocatedInterval {
        Tmp tmp;
        Interval interval;

        bool operator<(const AllocatedInterval& other) const
        {
            return this->interval.end() < other.interval.end();
        }

        void dump(PrintStream& out) const
        {
            out.print("{ ", tmp, " ", interval, " }");
        }
    };

    using AllocatedIntervalSet = StdSet<AllocatedInterval>;

    void add(Tmp tmp, LiveRange& range)
    {
        ASSERT(!hasConflict(range)); // Can't add overlapping LiveRanges
        for (auto& interval : range.intervals()) {
            ASSERT(interval != Interval()); // Strict ordering requires no empty intervals.
            m_allocations.insert({ tmp, interval });
        }
    }

    void evict(Tmp tmp, LiveRange& range)
    {
        for (auto& interval : range.intervals()) {
            auto r = m_allocations.erase({ tmp, interval });
            ASSERT_UNUSED(r, r == 1);
        }
    }

    bool hasConflict(LiveRange& range)
    {
        bool hasConflict = false;
        forEachConflict(range, [&](auto&) -> IterationStatus {
            hasConflict = true;
            return IterationStatus::Done;
        });
        return hasConflict;
    }

    // func is called with each (Tmp, Interval) pair (i.e. AllocatedInterval) of this
    // RegisterRange that overlaps with the given LiveRange 'range'.
    //
    // func is allowed to modify this RegisterRange, e.g. by calling evict().
    // func must not modify 'range' for the duration of this forEachConflict invocation.
    template<typename Func>
    void forEachConflict(const LiveRange& range, const Func& func)
    {
        auto rangeIter = range.intervals().begin();
        auto rangeEnd = range.intervals().end();

        if (rangeIter == rangeEnd)
            return;
        Point nextSearch = rangeIter->begin();

        while (true) {
            AllocatedInterval conflict;
            {
                auto conflictIter = findFirstIntervalEndingAfter(nextSearch);
                if (conflictIter == m_allocations.end())
                    return; // End of 'm_allocations', so no more potential conflicts
                if (rangeIter->end() <= conflictIter->interval.begin()) {
                    // No more conflicts with this 'range' interval. Move on to the next interval in 'range'.
                    if (++rangeIter == rangeEnd)
                        return; // End of 'range', so no more potential conflicts
                    // Start searching for conflicts of the next 'range' interval.
                    nextSearch = rangeIter->begin();
                    continue;
                }
                // Found a conflict. There may be additional conflicts of this 'range' interval, so advance
                // the search position beyond this conflict but don't advance the 'range' interval.
                conflict = *conflictIter;
                nextSearch = conflictIter->interval.end();
            }
            // 'func' can invalidate iterators of 'm_allocations'.
            if (func(conflict) == IterationStatus::Done)
                return;
        }
    }

    void dump(PrintStream& out) const
    {
        CommaPrinter comma;
        out.print("[");
        for (auto& alloc : m_allocations) {
            out.print(comma);
            out.print(alloc);
        }
        out.print("]");
    }

private:
    AllocatedIntervalSet::iterator findFirstIntervalEndingAfter(Point point)
    {
        Interval query(point);
        // pos can be 0, yet we can't express a non-empty interval with end==0, so instead of looking
        // for the first interval ending after pos we find the first interval ending at or after pos+1.
        ASSERT(query.end() == point + 1);
        auto iter = m_allocations.lower_bound({ Tmp(), query });
        ASSERT(iter == m_allocations.end() || iter->interval.end() > point);
        return iter;
    }

    AllocatedIntervalSet m_allocations;
};

// Auxillary register allocator data per Tmp.
struct TmpData {
    struct CoalescableWith {
        void dump(PrintStream& out) const
        {
            out.print("(", tmp, ", ", weight, ")");
        }

        Tmp tmp;
        float weight;
    };

    void dump(PrintStream& out) const
    {
        out.print("{stage = ", stage, " liveRange = ", liveRange, ", preferredReg = ", preferredReg,
            ", coalescables = ", listDump(coalescables), ", subGroup0 = ", subGroup0, ", subGroup1 = ", subGroup1,
            ", spillCost = ", spillCost, ", assigned = ", assigned, ", spilled = ", pointerDump(spillSlot), ", splitMetadataIndex = ", splitMetadataIndex, "}");
    }

    bool isGroup()
    {
        ASSERT(!subGroup0 == !subGroup1);
        return !!subGroup0;
    }

    void validate()
    {
        ASSERT(!(spillSlot && assigned));
        ASSERT(!!assigned == (stage == Stage::Assigned));
        ASSERT_IMPLIES(spillSlot, stage == Stage::Spilled);
        ASSERT_IMPLIES(spillSlot, spillCost != unspillableCost);
        ASSERT_IMPLIES(spillSlot, !isGroup()); // Should have been split
        ASSERT_IMPLIES(!spillCost, !liveRange.size());
        ASSERT_IMPLIES(assigned, !parentGroup); // Only top-most should be assigned
        ASSERT_IMPLIES(coalescables.size(), !isGroup()); // Only bottom-most should have coalescables
    }

    Stage stage { Stage::New };
    LiveRange liveRange;
    float spillCost { 0.0f };
    Reg preferredReg;
    Vector<CoalescableWith> coalescables;
    Tmp parentGroup;
    Tmp subGroup0, subGroup1;
    size_t splitMetadataIndex { 0 };

    Reg assigned;
    StackSlot* spillSlot { nullptr };
};

// SplitMetadata tracks a Tmp that is split and the new Tmps that are used to carry the value
// across the "gaps".
struct SplitMetadata {
    void dump(PrintStream& out) const
    {
        out.print(originalTmp, " : { ", listDump(gapTmps), " } ");
    }

    Tmp originalTmp;
    Vector<Tmp> gapTmps;
};

class GreedyAllocator {
public:
    GreedyAllocator(Code& code)
        : m_code(code)
        , m_blockToHeadPoint(code.size())
        , m_tailPoints(code.size())
        , m_map(code)
        , m_splitMetadata(1) // Sacrifice index 0.
        , m_regRanges(Reg::maxIndex() + 1)
        , m_insertionSets(code.size())
        , m_useCounts(m_code)
        , m_tmpWidth(m_code)
    {
    }

    void run()
    {
        // FIXME: reconsider use of padIntereference, https://bugs.webkit.org/show_bug.cgi?id=288122
        padInterference(m_code);
        buildRegisterSets();
        buildIndices();
        buildLiveRanges();
        initSpillCosts<GP>();
        initSpillCosts<FP>();
        finalizeGroups<GP>();
        finalizeGroups<FP>();

        allocateRegisters<GP>();
        allocateRegisters<FP>();

        insertFixupCode();

        validateAssignments<GP>();
        validateAssignments<FP>();

        assignRegisters();
        fixSpillsAfterTerminals(m_code);
    }

private:
    static constexpr Point pointsPerInst = 2;

    void buildRegisterSets()
    {
        forEachBank([&] (Bank bank) {
            m_allowedRegistersInPriorityOrder[bank] = m_code.regsInPriorityOrder(bank);
            for (Reg r : m_allowedRegistersInPriorityOrder[bank])
                m_allAllowedRegisters.add(r, IgnoreVectors);
        });
        m_allAllowedRegistersWholeWidth = m_allAllowedRegisters.toRegisterSet().includeWholeRegisterWidth();
    }

    void buildIndices()
    {
        Point headPosition = 0;
        Point tailPosition = 0;
        for (size_t i = 0; i < m_code.size(); i++) {
            BasicBlock* block = m_code[i];
            if (!block) {
                m_tailPoints[i] = tailPosition;
                continue;
            }
            // Two points per instruction: early and late.
            tailPosition = headPosition + 2 * block->size() - 1;
            m_blockToHeadPoint[block] = headPosition;
            m_tailPoints[i] = tailPosition;
            headPosition += block->size() * pointsPerInst;
        }
    }

    BasicBlock* findBlockContainingPoint(Point point)
    {
        auto iter = std::lower_bound(m_tailPoints.begin(), m_tailPoints.end(), point);
        ASSERT(iter != m_tailPoints.end()); // Should ask only about legal instruction boundaries.
        size_t blockIndex = std::distance(m_tailPoints.begin(), iter);
        BasicBlock* block = m_code[blockIndex];
        ASSERT(positionOfHead(block) <= point && point <= positionOfTail(block));
        return block;
    }

    Point positionOfHead(BasicBlock* block)
    {
        return m_blockToHeadPoint[block];
    }

    Point positionOfTail(BasicBlock* block)
    {
        return positionOfHead(block) + block->size() * pointsPerInst - 1;
    }

    static size_t instIndex(Point positionOfHead, Point point)
    {
        return (point - positionOfHead) / pointsPerInst;
    }

    static Point positionOfEarly(Interval interval)
    {
        return interval.begin() & ~static_cast<Point>(1);
    }

    static Interval earlyInterval(Point positionOfEarly)
    {
        ASSERT(!(positionOfEarly & 1));
        return Interval(positionOfEarly);
    }

    static Interval lateInterval(Point positionOfEarly)
    {
        ASSERT(!(positionOfEarly & 1));
        return Interval(positionOfEarly + 1);
    }

    static Interval earlyAndLateInterval(Point positionOfEarly)
    {
        return earlyInterval(positionOfEarly) | lateInterval(positionOfEarly);
    }

    static Interval interval(Point positionOfEarly, Arg::Timing timing)
    {
        switch (timing) {
        case Arg::OnlyEarly:
            return earlyInterval(positionOfEarly);
        case Arg::OnlyLate:
            return lateInterval(positionOfEarly);
        case Arg::EarlyAndLate:
            return earlyAndLateInterval(positionOfEarly);
        }
        ASSERT_NOT_REACHED();
        return Interval();
    }

    static Interval intervalForSpill(Point positionOfEarly, Arg::Role role)
    {
        Arg::Timing timing = Arg::timing(role);
        switch (timing) {
        case Arg::OnlyEarly:
            if (Arg::isAnyDef(role))
                return earlyAndLateInterval(positionOfEarly); // We have a spill store after this insn.
            return earlyInterval(positionOfEarly);
        case Arg::OnlyLate:
            if (Arg::isAnyUse(role))
                return earlyAndLateInterval(positionOfEarly); // We had a spill load before this insn.
            return lateInterval(positionOfEarly);
        case Arg::EarlyAndLate:
            return earlyAndLateInterval(positionOfEarly);
        }
        ASSERT_NOT_REACHED();
        return Interval();
    }

    Tmp groupForTmp(Tmp tmp)
    {
        while (Tmp parent = m_map[tmp].parentGroup)
            tmp = parent;
        return tmp;
    }

    Reg assignedReg(Tmp tmp)
    {
        return m_map[groupForTmp(tmp)].assigned;
    }

    StackSlot* spillSlot(Tmp tmp)
    {
        return m_map[groupForTmp(tmp)].spillSlot;
    }

    float adjustedBlockFrequency(BasicBlock* block)
    {
        float freq = block->frequency();
        if (UNLIKELY(!m_fastBlocks.saw(block)))
            freq *= Options::rareBlockPenalty();
        return freq;
    }

    // Debug code to verify that the results of register allocation and finalization fixup is valid.
    // That is, no two Tmps simultaneously alive share the same register (unless they were coalesced).
    template<Bank bank>
    void validateAssignments()
    {
        if (!Options::airValidateGreedRegAlloc())
            return;
        bool anyFailures = false;

        auto fail = [&](BasicBlock* block, Tmp a, Tmp b) {
            dataLogLn("AIR GREEDY REGISTER ALLOCATION VALIDATION FAILURE");
            dataLogLn("   In BB", *block);
            dataLogLn("     tmp = ", a, " : ", m_map[a]);
            dataLogLn("     tmp = ", b, " : ", m_map[b]);
            anyFailures = true;
        };

        auto checkConflicts = [&](BasicBlock* block, const typename TmpLiveness<bank>::LocalCalc& localCalc) {
            for (Tmp a : localCalc.live()) {
                Tmp aGrp = groupForTmp(a);
                Reg aReg = assignedReg(a);
                if (!aReg)
                    continue;
                for (Tmp b : localCalc.live()) {
                    Tmp bGrp = groupForTmp(b);
                    Reg bReg = assignedReg(b);
                    if (aGrp == bGrp) {
                        // Coalesced a & b so they better have the same register.
                        if (aReg != bReg)
                            fail(block, a, b);
                    } else {
                        // a & b interfere so b must either have been spilled or assigned a different register.
                        if (!bReg)
                            continue;
                        if (aReg == bReg)
                            fail(block, a, b);
                    }
                }
            }
        };

        TmpLiveness<bank> liveness(m_code);
        for (BasicBlock* block : m_code) {
            typename TmpLiveness<bank>::LocalCalc localCalc(liveness, block);
            for (unsigned instIndex = block->size(); instIndex--;) {
                checkConflicts(block, localCalc);
                localCalc.execute(instIndex);
            }
            checkConflicts(block, localCalc);
        }
        if (anyFailures) {
            dataLogLn("IR:");
            dataLogLn(m_code);
            RELEASE_ASSERT_NOT_REACHED();
        }
    }

    void buildLiveRanges()
    {
        CompilerTimingScope timingScope("Air"_s, "GreedyRegAlloc::buildLiveRanges"_s);
        UnifiedTmpLiveness liveness(m_code);
        TmpMap<Interval> activeIntervals(m_code);

        // Find non-rare blocks.
        m_fastBlocks.push(m_code[0]);
        while (BasicBlock* block = m_fastBlocks.pop()) {
            for (FrequentedBlock& successor : block->successors()) {
                if (!successor.isRare())
                    m_fastBlocks.push(successor.block());
            }
        }

        // addMaybeCoalescable is used during the first pass to collect all potentially
        // coalescables pairs. i.e. pairs of Tmps 'a' and 'b' such that there exists a
        // 'Move a, b' instruction. These pairs will be pruned after liveness analysis
        // based on conflicting defs. We do this rather than simply requiring that the
        // LiveRanges of coalescable tmps do not overlap so that we can handle Tmp copies, e.g.:
        //
        //   Move a, b
        //   Use a
        //   Use b
        //
        // The LiveRanges of a and b overlap, but the move is still coalescable (unless there
        // is a non-coalescable-move def of 'a' or 'b' during the lifetime of the other).
        auto addMaybeCoalescable = [&](Tmp a, Tmp b, BasicBlock* block) {
            if (a == b)
                return;
            TmpData& tmpData = m_map[a];
            float freq = adjustedBlockFrequency(block);
            for (auto& with : tmpData.coalescables) {
                if (with.tmp == b) {
                    with.weight += freq;
                    return;
                }
            }
            tmpData.coalescables.append({ b, freq });
        };

        auto coalescableMoveSrc = [&](Inst& inst) {
            return mayBeCoalescable(inst) ? inst.args[0].tmp() : Tmp();
        };

        auto isLiveAt = [&](Tmp tmp, Point point) {
            if (activeIntervals[tmp])
                return true;
            // Tmp may have had a dead def at point (e.g. clobber).
            auto& intervals = m_map[tmp].liveRange.intervals();
            if (intervals.isEmpty())
                return false;
            return intervals.first().contains(point);
        };

        // Remove def from any coalescable pair of a live tmp. We now know from liveness analysis
        // that these pairs are not coalescable.
        auto pruneCoalescable = [&](Inst& inst, Tmp def, Point point) {
            TmpData& defData = m_map[def];
            if (!defData.coalescables.size())
                return;
            Tmp movSrc = coalescableMoveSrc(inst);
            dataLogLnIf(verbose(), "Checking affinity ", inst, " def=", def, " movSrc=", movSrc);
            defData.coalescables.removeAllMatching([&](TmpData::CoalescableWith& with) {
                ASSERT(with.tmp != def);
                if (with.tmp != movSrc && isLiveAt(with.tmp, point)) {
                    dataLogLnIf(verbose(), "Pruning affinity ", def, " ", with.tmp);
                    m_map[with.tmp].coalescables.removeAllMatching([def](TmpData::CoalescableWith& with) {
                        return with.tmp == def;
                    });
                    return true;
                }
                return false;
            });
        };

        auto closeInterval = [&](Tmp tmp) {
            ASSERT(activeIntervals[tmp] != Interval());
            m_map[tmp].liveRange.prepend(activeIntervals[tmp]);
            activeIntervals[tmp] = Interval();
        };

        // First pass: collect all the potential coalescable pairs of Tmps.
        for (BasicBlock* block : m_code) {
            if (!block)
                continue;
            for (Inst& inst : block->insts()) {
                if (mayBeCoalescable(inst)) {
                    ASSERT(inst.args.size() == 2);
                    if (inst.args[0].isReg() || inst.args[1].isReg()) {
                        unsigned regIdx = inst.args[0].isReg() ? 0 : 1;
                        Reg reg = inst.args[regIdx].reg();
                        if (m_allAllowedRegisters.contains(reg, IgnoreVectors)) {
                            Tmp other = inst.args[regIdx ^ 1].tmp();
                            if (!m_map[other].preferredReg)
                                m_map[other].preferredReg = inst.args[regIdx].reg();
                        }
                    } else {
                        ASSERT(inst.args[0].isTmp() && inst.args[1].isTmp());
                        addMaybeCoalescable(inst.args[0].tmp(), inst.args[1].tmp(), block);
                        addMaybeCoalescable(inst.args[1].tmp(), inst.args[0].tmp(), block);
                    }
                }
            }
        }

        // Second pass: Run liveness analysis and build the LiveRange for each Tmp. Also,
        // prune conflicts from the coalescables.
        BasicBlock* blockAfter = nullptr;
        Vector<Tmp, 8> earlyUses, earlyDefs, lateUses, lateDefs;
        for (size_t blockIndex = m_code.size(); blockIndex--;) {
            BasicBlock* block = m_code[blockIndex];
            if (!block)
                continue;

            Point positionOfHead = this->positionOfHead(block);
            Point positionOfTail = this->positionOfTail(block);
            if (verbose()) {
                dataLog("At BB", pointerDump(block), "\n");
                dataLog("  positionOfHead = ", positionOfHead, "\n");
                dataLog("  positionOfTail = ", positionOfTail, "\n");
            }

            for (Tmp tmp : liveness.liveAtTail(block))
                activeIntervals[tmp] |= Interval(positionOfTail); // FIXME: could just set interval start

            if (blockAfter) {
                for (Tmp tmp : liveness.liveAtHead(blockAfter)) {
                    if (activeIntervals[tmp].contains(positionOfTail))
                        m_map[tmp].liveRange.crossBasicBlockBoundary();
                    else {
                        // If tmp was live at the head of the next block but no longer live, close
                        // the current interval.
                        ASSERT(activeIntervals[tmp].begin() == this->positionOfHead(blockAfter));
                        closeInterval(tmp);
                    }
                }
            }

            for (unsigned instIndex = block->size(); instIndex--;) {
                Inst& inst = block->at(instIndex);
                Point positionOfEarly = positionOfHead + instIndex * pointsPerInst;

                lateUses.shrink(0);
                lateDefs.shrink(0);
                earlyUses.shrink(0);
                earlyDefs.shrink(0);
                inst.forEachTmp([&](Tmp& tmp, Arg::Role role, Bank, Width) {
                    if (Arg::isLateUse(role))
                        lateUses.append(tmp);
                    if (Arg::isLateDef(role))
                        lateDefs.append(tmp);
                    if (Arg::isEarlyUse(role))
                        earlyUses.append(tmp);
                    if (Arg::isEarlyDef(role))
                        earlyDefs.append(tmp);
                });
                for (Tmp tmp : lateUses)
                    activeIntervals[tmp] |= lateInterval(positionOfEarly);
                for (Tmp tmp : lateDefs) {
                    activeIntervals[tmp] |= lateInterval(positionOfEarly);
                    closeInterval(tmp);
                    pruneCoalescable(inst, tmp, positionOfEarly + 1);
                }
                for (Tmp tmp : earlyUses)
                    activeIntervals[tmp] |= earlyInterval(positionOfEarly);
                for (Tmp tmp : earlyDefs) {
                    activeIntervals[tmp] |= earlyInterval(positionOfEarly);
                    closeInterval(tmp);
                    pruneCoalescable(inst, tmp, positionOfEarly);
                }
                if (inst.kind.opcode == Patch) {
                    auto clobberReg = [&](Reg reg, Interval interval) {
                        Tmp tmp = Tmp(reg);
                        bool isAlive = !!activeIntervals[tmp];
                        activeIntervals[tmp] |= interval;
                        if (!isAlive)
                            closeInterval(tmp);
                    };
                    inst.extraClobberedRegs().forEachWithWidthAndPreserved(
                        [&](Reg reg, Width, PreservedWidth) {
                            clobberReg(reg, lateInterval(positionOfEarly));
                        });
                    inst.extraEarlyClobberedRegs().forEachWithWidthAndPreserved(
                        [&](Reg reg, Width, PreservedWidth) {
                            clobberReg(reg, earlyInterval(positionOfEarly));
                        });
                }

            }
            for (Tmp tmp : liveness.liveAtHead(block))
                activeIntervals[tmp] |= Interval(positionOfHead);

            blockAfter = block;
        }
        if (blockAfter) {
            for (Tmp tmp : liveness.liveAtHead(blockAfter)) {
                ASSERT(activeIntervals[tmp].begin() == this->positionOfHead(blockAfter));
                closeInterval(tmp);
            }
        }

#if ASSERT_ENABLED
        m_code.forEachTmp([&](Tmp tmp) {
            ASSERT(!activeIntervals[tmp]);
        });
#endif
        if (verbose()) {
            dataLog("Intervals:\n");
            auto dumpRegTmpData = [&](Reg r) {
                TmpData& tmpData = m_map[Tmp(r)];
                if (tmpData.liveRange.size())
                    dataLog("    ", r, ": ", m_map[Tmp(r)], "\n");
            };
            for (Reg r : m_allowedRegistersInPriorityOrder[GP])
                dumpRegTmpData(r);
            for (Reg r : m_allowedRegistersInPriorityOrder[FP])
                dumpRegTmpData(r);
            m_code.forEachTmp([&](Tmp tmp) {
                dataLog("    ", tmp, ": ", m_map[tmp], "\n");
            });
        }
    }

    template<typename Func>
    IterationStatus forEachTmpInGroup(Tmp grp, const Func& func)
    {
        TmpData& data = m_map[grp];
        if (!data.isGroup())
            return func(grp);
        ASSERT(data.subGroup0 && data.subGroup1);
        if (forEachTmpInGroup(data.subGroup0, func) == IterationStatus::Done)
            return IterationStatus::Done;
        return forEachTmpInGroup(data.subGroup1, func);
    }

    template <Bank bank>
    void finalizeGroups()
    {
        CompilerTimingScope timingScope("Air"_s, "GreedyRegAlloc::finalizeGroups"_s);

        struct Move {
            Tmp tmp0, tmp1;
            float weight;

            void dump(PrintStream& out) const
            {
                out.print(tmp0, ", ", tmp1, " ", weight);
            }
        };
        Vector<Move> moves;

        m_code.forEachTmp<bank>([&](Tmp tmp) {
            ASSERT(!tmp.isReg());
            TmpData& data = m_map[tmp];
            std::sort(data.coalescables.begin(), data.coalescables.end(),
                [this] (const auto& a, const auto& b) -> bool {
                    if (a.weight != b.weight)
                        return a.weight > b.weight;
                    // Favor coalescing shorter live ranges.
                    auto aSize = m_map[a.tmp].liveRange.size();
                    auto bSize = m_map[b.tmp].liveRange.size();
                    if (aSize != bSize)
                        return aSize < bSize;
                    return a.tmp.tmpIndex(bank) < b.tmp.tmpIndex(bank);
            });

            if (!eagerGroups)
                return;

            for (auto& with : m_map[tmp].coalescables) {
                if (tmp.tmpIndex(bank) < with.tmp.tmpIndex(bank))
                    moves.append({ tmp, with.tmp, with.weight });
            }
        });

        ASSERT_IMPLIES(!eagerGroups, moves.isEmpty());
        std::sort(moves.begin(), moves.end(),
            [](Move& a, Move& b) -> bool {
                if (a.weight != b.weight)
                    return a.weight > b.weight;
                if (a.tmp0.tmpIndex(bank) != b.tmp1.tmpIndex(bank))
                    return a.tmp0.tmpIndex(bank) < a.tmp0.tmpIndex(bank);
                ASSERT(a.tmp1.tmpIndex(bank) != b.tmp1.tmpIndex(bank));
                return a.tmp1.tmpIndex(bank) < b.tmp1.tmpIndex(bank);
            });

        auto hasConflict = [this](Tmp group0, Tmp group1) {
            bool conflicts = false;
            forEachTmpInGroup(group0, [&](Tmp tmp0) {
                ASSERT(!conflicts);
                TmpData& data0 = m_map[tmp0];
                ASSERT(!data0.subGroup0 && !data0.subGroup1);
                forEachTmpInGroup(group1, [&](Tmp tmp1) {
                    ASSERT(!conflicts);
                    ASSERT(tmp0 != tmp1);
                    TmpData& data1 = m_map[tmp1];
                    if (!data0.coalescables.containsIf([tmp1](auto& with) { return with.tmp == tmp1; })
                        && data0.liveRange.overlaps(data1.liveRange)) {
                        conflicts = true;
                        return IterationStatus::Done;
                    }
                    return IterationStatus::Continue;
                });
                return conflicts ? IterationStatus::Done : IterationStatus::Continue;
            });
            return conflicts;
        };

        auto addSubGroup = [this](Tmp group, TmpData& groupData, Tmp& subGroupField, Tmp subGroup) {
            TmpData& subGroupData = m_map[subGroup];
            subGroupField = subGroup;
            subGroupData.parentGroup = group;
            subGroupData.stage = Stage::Coalesced;

            groupData.liveRange = LiveRange::merge(groupData.liveRange, subGroupData.liveRange);
            groupData.spillCost += subGroupData.spillCost;
            if (!groupData.preferredReg)
                groupData.preferredReg = subGroupData.preferredReg;

            Width defWidth, useWidth;
            defWidth = std::max(m_tmpWidth.defWidth(group), m_tmpWidth.defWidth(subGroup));
            useWidth = std::max(m_tmpWidth.useWidth(group), m_tmpWidth.useWidth(subGroup));
            m_tmpWidth.setWidths(group, useWidth, defWidth);
        };

        for (Move& move : moves) {
            dataLogLnIf(verbose(), "Processing move: ", move);
            Tmp group0 = groupForTmp(move.tmp0);
            Tmp group1 = groupForTmp(move.tmp1);
            if (group0 == group1) {
                dataLogLnIf(verbose(), "Already grouped transitively into ", group0);
                continue;
            }
            if (!hasConflict(group0, group1)) {
                Tmp newGrp = m_code.newTmp(bank);
                TmpData newGrpData;
                m_tmpWidth.setWidths(newGrp, Width8, Width8);

                addSubGroup(newGrp, newGrpData, newGrpData.subGroup0, group0);
                addSubGroup(newGrp, newGrpData, newGrpData.subGroup1, group1);
                newGrpData.validate();
                m_map.append(newGrp, newGrpData);
                dataLogLnIf(verbose(), "Created group ", newGrp, ": ", m_map[newGrp]);
            }
        }
        if (verbose()) {
            m_code.forEachTmp<bank>([&](Tmp tmp) {
                TmpData& data = m_map[tmp];
                if (!data.parentGroup && data.isGroup()) {
                    dataLog("Group: ", tmp, " = { ");
                    CommaPrinter comma;
                    forEachTmpInGroup(tmp, [&comma](Tmp member) {
                        dataLog(comma, member);
                        return IterationStatus::Continue;
                    });
                    dataLogLn(" }");
                }
            });
        }
    }

    template<Bank bank>
    void initSpillCosts()
    {
        CompilerTimingScope timingScope("Air"_s, "GreedyRegAlloc::initSpillCosts"_s);

        for (Reg reg : m_allowedRegistersInPriorityOrder[bank])
            m_map[Tmp(reg)].spillCost = unspillableCost;

        // FIXME: tmps alive only in one gap should be unspillable.
        m_code.forEachTmp<bank>([&](Tmp tmp) {
            ASSERT(tmp.bank() == bank);
            ASSERT(!tmp.isReg());
            auto index = AbsoluteTmpMapper<bank>::absoluteIndex(tmp);
            float spillCost = m_useCounts.numWarmUsesAndDefs<bank>(index);
            if (bank == GP && m_useCounts.isConstDef<GP>(index))
                spillCost /= 2; // Can rematerialize rather than spill in many cases.
            ASSERT(m_map[tmp].spillCost == 0.0f);
            m_map[tmp].spillCost = spillCost;
        });
        m_code.forEachFastTmp([&](Tmp tmp) {
            if (tmp.bank() != bank)
                return;
            m_map[tmp].spillCost = fastTmpSpillCost;
            dataLogLnIf(verbose(), "FastTmp: ", tmp);
        });
    }

    // newTmp creates and returns a new tmp that can hold the values of 'from'.
    // Note that all TmpData references invalidated since it may expand/realloc the TmpData map.
    Tmp newTmp(Tmp from, float spillCost, Interval interval)
    {
        Tmp tmp = m_code.newTmp(from.bank());
        m_tmpWidth.setWidths(tmp, m_tmpWidth.useWidth(from), m_tmpWidth.defWidth(from));

        m_map.append(tmp, TmpData());
        TmpData& tmpData = m_map[tmp];
        tmpData.liveRange.prepend(interval);
        tmpData.spillCost = spillCost;
        tmpData.validate();
        return tmp;
    }

    Tmp addSpillTmpWithInterval(Tmp spilledTmp, Interval interval)
    {
        Tmp tmp = newTmp(spilledTmp, unspillableCost, interval);
        dataLogLnIf(verbose(), "New spill for ", spilledTmp, " tmp: ", tmp, ": ", m_map[tmp]);
        setStageAndEnqueue(tmp, m_map[tmp], Stage::Unspillable);
        return tmp;
    }

    void dumpRegRanges(Bank bank)
    {
        for (Reg r : m_allowedRegistersInPriorityOrder[bank])
            dataLogLn("   regRanges[", r, "]: ", m_regRanges[r]);
    }

    void setStageAndEnqueue(Tmp tmp, TmpData& tmpData, Stage stage)
    {
        ASSERT(!tmp.isReg());
        ASSERT(stage == Stage::Unspillable || stage == Stage::TryAllocate || stage == Stage::TrySplit || stage == Stage::Spill);
        ASSERT_IMPLIES(!tmpData.spillCost, !tmpData.liveRange.size());
        ASSERT(!tmpData.parentGroup); // Group member should not be enquened
        ASSERT_IMPLIES(!eagerGroups, !tmpData.isGroup());
        tmpData.validate();

        tmpData.stage = stage;
        size_t rangeSizeOrStart = tmpData.liveRange.size();
        if (tmpData.liveRange.isLocal())
            rangeSizeOrStart = tmpData.liveRange.intervals().first().begin();

        m_queue.enqueue({ tmp, stage, rangeSizeOrStart, tmpData.preferredReg || tmpData.coalescables.size(), !tmpData.liveRange.isLocal() });
        dataLogLnIf(verbose(), "Enqueued (", stage, ") ", tmp);
    }

    template <Bank bank>
    void allocateRegisters()
    {
        CompilerTimingScope timingScope("Air"_s, "GreedyRegAlloc::allocateRegisters"_s);

        for (Reg reg : m_allowedRegistersInPriorityOrder[bank])
            assign(Tmp(reg), m_map[Tmp(reg)], reg);

        m_code.forEachTmp<bank>([&](Tmp tmp) {
            ASSERT(!tmp.isReg());
            TmpData& tmpData = m_map[tmp];
            if (tmpData.parentGroup) {
                ASSERT(eagerGroups);
                return;
            }
            if (tmpData.liveRange.intervals().isEmpty())
                return;
            setStageAndEnqueue(tmp, tmpData, Stage::TryAllocate);
        });

        do {
            while (!m_queue.isEmpty()) {
                auto entry = m_queue.dequeue();
                Tmp tmp = entry.tmp();
                ASSERT(tmp && tmp.bank() == bank);
                TmpData& tmpData = m_map[tmp];
                if (verbose()) {
                    dataLogLn("Pop: ", entry, " tmp: ", tmpData);
                    dumpRegRanges(bank);
                }
                if (tmpData.stage == Stage::Replaced)
                    continue; // Tmp no longer relevant
                if (tryAllocate<bank>(tmp, tmpData))
                    continue;
                if (tmpData.stage != Stage::TrySplit && tryEvict<bank>(tmp, tmpData))
                    continue;

                ASSERT(&tmpData == &m_map[tmp]); // Verify m_map hasn't been resized on this path
                switch (tmpData.stage) {
                case Stage::TryAllocate: {
                    // If we couldn't allocate tmp, allow it to split next time.
                    Stage nextStage = Stage::TrySplit;
                    // If we already know splitting won't be profitable, skip it.
                    if (!tmpData.isGroup() && tmpData.liveRange.size() < splitMinRangeSize)
                        nextStage = Stage::Spill;
                    setStageAndEnqueue(tmp, tmpData, nextStage);
                    continue;
                }
                case Stage::TrySplit:
                    if (!trySplit<bank>(tmp, tmpData))
                        setStageAndEnqueue(tmp, tmpData, Stage::Spill);
                    continue;
                case Stage::Spill:
                    ASSERT(queueContainsOnlySpills()); // FIXME: remove
                    spill(tmp, tmpData);
                    continue;
                case Stage::Unspillable:
                    // Unspillables must have been allocated during tryAllocate or tryEvict.
                case Stage::New:
                case Stage::Assigned:
                case Stage::Spilled:
                case Stage::Coalesced:
                case Stage::Replaced:
                    dataLogLn("Invalid stage tmp = ", tmp, " tmpData = ", tmpData);
                    // Tmps in these stages should not have been enqueued.
                    RELEASE_ASSERT_NOT_REACHED();
                }
                RELEASE_ASSERT_NOT_REACHED();
            }
            if (m_didSpill) {
                emitSpillCodeAndEnqueueNewTmps<bank>();
                m_didSpill = false;
            }
            // Process the spill/fill tmps,
        } while (!m_queue.isEmpty());
    }

    template <Bank bank>
    bool tryAllocate(Tmp tmp, TmpData& tmpData)
    {
        ASSERT(&m_map[tmp] == &tmpData);
        ASSERT(!assignedReg(tmp));
        ASSERT(!tmpData.parentGroup);

        auto tryAllocateToReg = [&](Reg r) {
            LiveRange& liveRange = tmpData.liveRange;
            RegisterRange& regRanges = m_regRanges[r];
            if (!regRanges.hasConflict(liveRange)) {
                assign(tmp, tmpData, r);
                return true;
            }
            return false;
        };

        ScalarRegisterSet alreadyAttempted;
        if (eagerGroupsExhaustiveSearch) {
            // FIXME: this will check coalescables within the group, which is wasteful and common.
            // But without doing this, we won't try to coalescables between partially split groups.
            IterationStatus status = forEachTmpInGroup(tmp, [&](Tmp member) {
                for (auto& with : m_map[member].coalescables) {
                    Reg r = assignedReg(with.tmp);
                    if (r) {
                        if (tryAllocateToReg(r))
                            return IterationStatus::Done;
                        alreadyAttempted.add(r, IgnoreVectors);
                    }
                }
                return IterationStatus::Continue;
            });
            if (status == IterationStatus::Done) {
                ASSERT(tmpData.assigned);
                return true;
            }
        } else {
            for (auto& with : tmpData.coalescables) {
                Reg r = m_map[with.tmp].assigned;
                if (r) {
                    if (tryAllocateToReg(r))
                        return true;
                    alreadyAttempted.add(r, IgnoreVectors);
                }
            }
        }
        ASSERT(!tmpData.assigned);

        if (tmpData.preferredReg) {
            if (tryAllocateToReg(tmpData.preferredReg))
                return true;
            alreadyAttempted.add(tmpData.preferredReg, IgnoreVectors);
        }
        for (Reg r : m_allowedRegistersInPriorityOrder[bank]) {
            if (alreadyAttempted.contains(r, IgnoreVectors))
                continue;
            if (tryAllocateToReg(r))
                return true;
        }
        return false;
    }

    template <Bank bank>
    bool tryEvict(Tmp tmp, TmpData& tmpData)
    {
        ASSERT(&m_map[tmp] == &tmpData);
        ASSERT(tmp.bank() == bank);

        Reg bestEvictReg;
        float minSpillCost = unspillableCost;
        BitVector visited(m_code.numTmps(bank));
        LiveRange& liveRange = tmpData.liveRange;
        for (Reg r : m_allowedRegistersInPriorityOrder[bank]) {
            float conflictsSpillCost = 0.0f;
            visited.clearAll();
            m_regRanges[r].forEachConflict(liveRange,
                [&](auto& conflict) -> IterationStatus {
                    if (conflict.tmp.isReg()) {
                        // Conflicts with a fixed register use/def, cannot evict.
                        conflictsSpillCost = unspillableCost;
                        return IterationStatus::Done;
                    }
                    unsigned conflictTmpIndex = conflict.tmp.tmpIndex(bank);
                    if (visited.quickGet(conflictTmpIndex))
                        return IterationStatus::Continue;
                    visited.quickSet(conflictTmpIndex);
                    auto cost = m_map[conflict.tmp].spillCost;
                    if (cost == unspillableCost) {
                        conflictsSpillCost = unspillableCost;
                        return IterationStatus::Done;
                    }
                    conflictsSpillCost += cost;
                    return conflictsSpillCost >= minSpillCost ? IterationStatus::Done : IterationStatus::Continue;
            });
            if (conflictsSpillCost < minSpillCost) {
                minSpillCost = conflictsSpillCost;
                bestEvictReg = r;
            }
        }
        if (minSpillCost >= tmpData.spillCost) {
            // If 'tmp' was unspillable, we better have found at least one suitable register.
            RELEASE_ASSERT(tmpData.spillCost != unspillableCost);
            return false;
        }
        // It's cheaper to spill all the already-assigned conflicting tmps, so evict them in favor of assigning 'tmp'.
        m_regRanges[bestEvictReg].forEachConflict(liveRange,
            [&](auto& conflict) -> IterationStatus {
                TmpData& conflictData = m_map[conflict.tmp];
                evict(conflict.tmp, conflictData, bestEvictReg);
                setStageAndEnqueue(conflict.tmp, conflictData, Stage::TryAllocate);
                return IterationStatus::Continue;
            });
        assign(tmp, tmpData, bestEvictReg);
        return true;
    }

    void assign(Tmp tmp, TmpData& tmpData, Reg reg)
    {
        m_regRanges[reg].add(tmp, tmpData.liveRange);
        ASSERT(tmpData.stage != Stage::Assigned && tmpData.stage != Stage::Spilled);
        tmpData.stage = Stage::Assigned;
        tmpData.assigned = reg;
        dataLogLnIf(verbose(), "Assigned ", tmp, " to ", reg);
        tmpData.validate();
    }

    void evict(Tmp tmp, TmpData& tmpData, Reg reg)
    {
        ASSERT(tmpData.stage == Stage::Assigned);
        ASSERT(tmpData.spillCost != unspillableCost);
        ASSERT(tmpData.assigned == reg);
        m_regRanges[reg].evict(tmp, tmpData.liveRange);
        tmpData.stage = Stage::New;
        tmpData.assigned = Reg();
        dataLogLnIf(verbose(), "Evicted ", tmp, " from ", reg);
        tmpData.validate();
    }

    template<Bank bank>
    bool trySplit(Tmp tmp, TmpData& tmpData)
    {
        ASSERT(tmpData.spillCost != unspillableCost); // Should have evicted.
        if (trySplitGroup(tmp, tmpData))
            return true;
        return trySplitAroundClobbers<bank>(tmp, tmpData);
    }

    bool trySplitGroup(Tmp tmp, TmpData& tmpData)
    {
        if (!tmpData.isGroup())
            return false;
        ASSERT(eagerGroups);
        auto enqueueSubgroup = [&](Tmp subGrp) {
            m_map[subGrp].parentGroup = Tmp();
            setStageAndEnqueue(subGrp, m_map[subGrp], Stage::TryAllocate);
        };
        if (eagerGroupsSplitFully) {
            forEachTmpInGroup(tmp, [&](Tmp member) {
                enqueueSubgroup(member);
                return IterationStatus::Continue;
            });
        } else {
            enqueueSubgroup(tmpData.subGroup0);
            enqueueSubgroup(tmpData.subGroup1);
        }
        tmpData.stage = Stage::Replaced;
        dataLogLnIf(verbose(), "Split (group) ", tmp);
        tmpData.validate();
        return true;
    }

    template<Bank bank>
    bool trySplitAroundClobbers(Tmp tmp, TmpData& tmpData)
    {
        if (tmpData.splitMetadataIndex)
            return false; // Already split around clobbers
        if (tmpData.liveRange.size() < splitMinRangeSize)
            return false; // Not enough instructions to be worthwhile

        auto instUsesOrDefsTmp = [](Inst& inst, Tmp tmp) {
            bool result = false;
            inst.forEachTmpFast([&](Tmp useOrDef) {
                result |= useOrDef == tmp;
            });
            return result;
        };

        Reg bestSplitReg;
        float minSplitCost = unspillableCost;
        for (Reg r : m_allowedRegistersInPriorityOrder[bank]) {
            float splitCost = 0.0f;
            m_regRanges[r].forEachConflict(tmpData.liveRange,
                [&](auto& conflict) -> IterationStatus {
                    if (conflict.tmp.isReg() && conflict.interval.distance() == 1) {
                        // Block freq * rare block penalty
                        BasicBlock* block = findBlockContainingPoint(conflict.interval.begin());
                        unsigned instIndex = this->instIndex(positionOfHead(block), conflict.interval.begin());
                        Inst& inst = block->at(instIndex);
                        if (instUsesOrDefsTmp(inst, tmp)) {
                            // If the inst that clobbers regs also use/def the tmp trying to be split, then
                            // can't split the tmp around this clobber.
                            // FIXME: could allow uses, but then we'd have to make split tmp conflict with any
                            // spill tmps used by this instruction, so unclear if that's better.
                            splitCost = unspillableCost;
                            return IterationStatus::Done;
                        }
                        // Times 2 for 'MOV tmp, gapTmp' and 'MOV gapTmp, tmp'
                        splitCost += adjustedBlockFrequency(block) * 2;
                        return IterationStatus::Continue;
                    }
                    // Conflicts with another Tmp already assigned to this register so splitting around the clobbers won't help.
                    splitCost = unspillableCost;
                    return IterationStatus::Done;
                });
            if (splitCost < minSplitCost) {
                minSplitCost = splitCost;
                bestSplitReg = r;
            }
        }
        ASSERT(tmpData.spillCost != unspillableCost); // Should have evicted.
        if (minSplitCost >= unspillableCost)
            return false; // Other conflicts exist, so splitting is not productive
        // Multiplier of 0 means split around clobbers at every opportunity. The higher the multiplier,
        // the less often the split will be applied (i.e. treats splitting as more costly).
        if (minSplitCost * Options::airGreedyRegAllocSplitMultiplier() >= tmpData.spillCost)
            return false; // Better to spill than to split

        LiveRange holeRange;
        m_regRanges[bestSplitReg].forEachConflict(tmpData.liveRange,
            [&](auto& conflict) -> IterationStatus {
                ASSERT(conflict.tmp.isReg() && conflict.interval.distance() == 1);
                // Punched hole should always include the instructions late interval so the
                // split tmp won't be modeled as conflicting with late defs.
                Interval hole = conflict.interval | lateInterval(positionOfEarly(conflict.interval));
                ASSERT(hole.distance() == 1 || hole.distance() == 2);
                holeRange.append(hole);
                return IterationStatus::Continue;
            });

        tmpData.liveRange = LiveRange::subtract(tmpData.liveRange, holeRange);
        tmpData.splitMetadataIndex = m_splitMetadata.size();
        setStageAndEnqueue(tmp, tmpData, Stage::TryAllocate);

        SplitMetadata metadata;
        metadata.originalTmp = tmp;
        // Create tmps to carry the value across register clobbering instructions. These tmps
        // might spill or be assigned another register.
        for (Interval hole : holeRange.intervals()) {
            BasicBlock* block = findBlockContainingPoint(hole.begin());
            float freq = 2 * adjustedBlockFrequency(block);
            // padInterference() ensures this.
            // FIXME: reconsider that, see https://bugs.webkit.org/show_bug.cgi?id=288122
            ASSERT(hole.begin() > positionOfHead(block));
            // Model gapTmp interference with any other tmp split at this location by starting
            // the gapTmp's range one position before the hole. Otherwise, the same register
            // may be chosen for the gapTmp and another splitTmp, which wouldn't be valid
            // since there could be a cycle among the set of fixup Move instructions.
            // An alternative would be to use the Shuffle opcode (which can handle that
            // rotation of register assignments) but that would trigger an extra liveness
            // analysis (see lowerAfterRegAlloc()), and that's unlikely to be worth it.
            Interval gapInterval = hole | Interval(hole.begin() - 1);
            Tmp gapTmp = newTmp(tmp, freq, gapInterval);
            metadata.gapTmps.append(gapTmp);
            setStageAndEnqueue(gapTmp, m_map[gapTmp], Stage::TryAllocate);
        }
        dataLogLnIf(verbose(), "Split (clobbers): reg = ", bestSplitReg, " spillCost = ", m_map[tmp].spillCost, " splitCost = ", minSplitCost, " split tmp = ", metadata);
        m_splitMetadata.append(WTFMove(metadata));
        return true;
    }

    static unsigned stackSlotMinimumWidth(Width width)
    {
        if (width <= Width32)
            return 4;
        if (width <= Width64)
            return 8;
        ASSERT(width == Width128);
        return 16;
    }

    void spill(Tmp tmp, TmpData& tmpData)
    {
        RELEASE_ASSERT(tmpData.spillCost != unspillableCost);
        ASSERT(tmpData.assigned == Reg());
        ASSERT(!tmpData.isGroup()); // Should have been split
        tmpData.stage = Stage::Spilled;

        dataLogLnIf(verbose(), "Spilled ", tmp);
        if (tmpData.splitMetadataIndex) {
            // Splitting didn't prevent originalTmp from spilling after all, so no point assigning
            // registers or stack slots to the gap tmps for this split.
            dataLogLnIf(verbose(), "   evicting tmps created during split");
            auto& metadata = m_splitMetadata[tmpData.splitMetadataIndex];
            ASSERT(metadata.originalTmp == tmp);
            for (Tmp gapTmp : metadata.gapTmps) {
                Reg reg = m_map[gapTmp].assigned;
                if (reg)
                    evict(gapTmp, m_map[gapTmp], reg);
                m_map[gapTmp].stage = Stage::Replaced;
            }
        }
        // Batch the generation of spill/fill tmps so that we can limit traversals of the code while
        // not tracking each tmp's use/defs explicitly.
        m_didSpill = true;
        tmpData.validate();
    }

    bool queueContainsOnlySpills()
    {
        for (auto& elem : m_queue) {
            if (elem.stage() != Stage::Spill)
                return false;
        }
        return true;
    }

    Opcode moveOpcode(Tmp tmp)
    {
        Opcode move = Oops;
        Width width = m_tmpWidth.requiredWidth(tmp);
        switch (stackSlotMinimumWidth(width)) {
        case 4:
            move = tmp.bank() == GP ? Move32 : MoveFloat;
            break;
        case 8:
            move = tmp.bank() == GP ? Move : MoveDouble;
            break;
        case 16:
            ASSERT(tmp.bank() == FP);
            move = MoveVector;
            break;
        default:
            RELEASE_ASSERT_NOT_REACHED();
            break;
        }
        return move;
    }

    template <Bank bank>
    void emitSpillCodeAndEnqueueNewTmps()
    {
        m_code.forEachTmp<bank>([&](Tmp tmp) {
            TmpData& tmpData = m_map[tmp];
            if (tmpData.stage == Stage::Spilled && !tmpData.spillSlot)
                tmpData.spillSlot = m_code.addStackSlot(stackSlotMinimumWidth(m_tmpWidth.requiredWidth(tmp)), StackSlotKind::Spill);
        });
        for (BasicBlock* block : m_code) {
            Point positionOfHead = this->positionOfHead(block);
            for (unsigned instIndex = 0; instIndex < block->size(); ++instIndex) {
                Inst& inst = block->at(instIndex);
                unsigned indexOfEarly = positionOfHead + instIndex * 2;

                // The TmpWidth analysis will say that a Move only stores 32 bits into the destination,
                // if the source only had 32 bits worth of non-zero bits. Same for the source: it will
                // only claim to read 32 bits from the source if only 32 bits of the destination are
                // read. Note that we only apply this logic if this turns into a load or store, since
                // Move is the canonical way to move data between GPRs.
                bool canUseMove32IfDidSpill = false;
                bool didSpill = false;
                bool needScratch = false;
                Tmp scratchForTmp;
                if (bank == GP && inst.kind.opcode == Move) {
                    if ((inst.args[0].isTmp() && m_tmpWidth.width(inst.args[0].tmp()) <= Width32)
                        || (inst.args[1].isTmp() && m_tmpWidth.width(inst.args[1].tmp()) <= Width32))
                        canUseMove32IfDidSpill = true;
                }

                // Try to replace the register use by memory use when possible.
                inst.forEachArg(
                    [&] (Arg& arg, Arg::Role role, Bank argBank, Width width) {
                        if (!arg.isTmp())
                            return;
                        if (argBank != bank)
                            return;
                        if (arg.isReg())
                            return;

                        StackSlot* spilled = spillSlot(arg.tmp());
                        if (!spilled)
                            return;
                        bool needScratchIfSpilledInPlace = false;
                        if (!inst.admitsStack(arg)) {
                            switch (inst.kind.opcode) {
                            case Move:
                            case MoveDouble:
                            case MoveFloat:
                            case Move32: {
                                unsigned argIndex = &arg - &inst.args[0];
                                unsigned otherArgIndex = argIndex ^ 1;
                                Arg otherArg = inst.args[otherArgIndex];
                                if (inst.args.size() == 2
                                    && otherArg.isStack()
                                    && otherArg.stackSlot()->isSpill()) {
                                    needScratchIfSpilledInPlace = true;
                                    break;
                                }
                                return;
                            }
                            default:
                                return;
                            }
                        }
                        // If the Tmp holds a constant then we want to rematerialize its
                        // value rather than loading it from the stack. In order for that
                        // optimization to kick in, we need to avoid placing the Tmp's stack
                        // address into the instruction.
                        if (!Arg::isColdUse(role) && m_useCounts.isConstDef<bank>(AbsoluteTmpMapper<bank>::absoluteIndex(arg.tmp())))
                            return;

                        Width spillWidth = m_tmpWidth.requiredWidth(arg.tmp());
                        if (Arg::isAnyDef(role) && width < spillWidth) {
                            // Either there are users of this tmp who will use more than width,
                            // or there are producers who will produce more than width non-zero
                            // bits.
                            // FIXME: It's not clear why we should have to return here. We have
                            // a ZDef fixup in allocateStack. And if this isn't a ZDef, then it
                            // doesn't seem like it matters what happens to the high bits. Note
                            // that this isn't the case where we're storing more than what the
                            // spill slot can hold - we already got that covered because we
                            // stretch the spill slot on demand. One possibility is that it's ZDefs of
                            // smaller width than 32-bit.
                            // https://bugs.webkit.org/show_bug.cgi?id=169823
                            return;
                        }
                        ASSERT(inst.kind.opcode == Move || !(Arg::isAnyUse(role) && width > spillWidth));
                        if (spillWidth != Width32)
                            canUseMove32IfDidSpill = false;

                        spilled->ensureSize(canUseMove32IfDidSpill ? 4 : bytesForWidth(width));
                        didSpill = true;
                        if (needScratchIfSpilledInPlace) {
                            needScratch = true;
                            scratchForTmp = arg.tmp();
                        }
                        arg = Arg::stack(spilled);
                    });

                if (didSpill && canUseMove32IfDidSpill)
                    inst.kind.opcode = Move32;

                if (needScratch) {
                    ASSERT(scratchForTmp != Tmp());
                    Tmp tmp = addSpillTmpWithInterval(scratchForTmp, intervalForSpill(indexOfEarly, Arg::Scratch));
                    inst.args.append(tmp);
                    RELEASE_ASSERT(inst.args.size() == 3);
                    // WTF::Liveness and Air::LivenessAdapter do not handle a late-def/use followed by early-def
                    // correctly. While this register allocator does handle it correctly (since it models distinct
                    // late and early points between instructions (i.e. intervalForSpill() won't overlap for different
                    // scratch Tmps)), insert a Nop so that subsequent liveness analysis correctly compute lifetime interference
                    // when there are back-to-back Move spill-spill-scratch instructions (scratch is early-def, late-use).
                    // See https://bugs.webkit.org/show_bug.cgi?id=163548#c2 for more info.
                    // FIXME: reconsider this, https://bugs.webkit.org/show_bug.cgi?id=288122
                    m_insertionSets[block].insert(instIndex, spillLoad, Nop, inst.origin);
                    continue;
                }

                // For every other case, add Load/Store as needed.
                inst.forEachTmp([&] (Tmp& tmp, Arg::Role role, Bank argBank, Width) {
                    if (tmp.isReg() || argBank != bank)
                        return;
                    StackSlot* spilled = spillSlot(tmp);
                    if (!spilled)
                        return;

                    Opcode move = moveOpcode(tmp);
                    auto oldTmp = tmp;
                    tmp = addSpillTmpWithInterval(tmp, intervalForSpill(indexOfEarly, role));
                    if (role == Arg::Scratch)
                        return;

                    Arg arg = Arg::stack(spilled);
                    if (Arg::isAnyUse(role)) {
                        auto tryRematerialize = [&]() {
                            if constexpr (bank == GP) {
                                auto oldIndex = AbsoluteTmpMapper<bank>::absoluteIndex(oldTmp);
                                if (m_useCounts.isConstDef<bank>(oldIndex)) {
                                    int64_t value = m_useCounts.constant<bank>(oldIndex);
                                    if (Arg::isValidImmForm(value) && isValidForm(Move, Arg::Imm, Arg::Tmp)) {
                                        m_insertionSets[block].insert(instIndex, spillLoad, Move, inst.origin, Arg::imm(value), tmp);
                                        dataLogLnIf(verbose(), "Rematerialized (imm) ", oldTmp, ": ", tmp, " <- ", WTF::RawHex(value));
                                        return true;
                                    }
                                    if (isValidForm(Move, Arg::BigImm, Arg::Tmp)) {
                                        m_insertionSets[block].insert(instIndex, spillLoad, Move, inst.origin, Arg::bigImm(value), tmp);
                                        dataLogLnIf(verbose(), "Rematerialized (bigImm) ", oldTmp, ": ", tmp, " <- ", WTF::RawHex(value));
                                        return true;
                                    }
                                }
                            }
                            return false;
                        };

                        if (!tryRematerialize())
                            m_insertionSets[block].insert(instIndex, spillLoad, move, inst.origin, arg, tmp);
                    }
                    if (Arg::isAnyDef(role))
                        m_insertionSets[block].insert(instIndex + 1, spillStore, move, inst.origin, tmp, arg);
                });
                ASSERT(inst.isValidForm());
            }
        }
    }

    void insertFixupCode()
    {
        for (auto& metadata : m_splitMetadata) {
            if (!metadata.originalTmp)
                continue;
            if (spillSlot(metadata.originalTmp))
                continue; // If spilled, better to not split after all. See spill().
            ASSERT(assignedReg(metadata.originalTmp));
            // Emit moves to and from the gapTmps (or stack stot) that fill the split holes.
            for (Tmp gapTmp : metadata.gapTmps) {
                TmpData& gapData = m_map[gapTmp];
                for (auto& interval : gapData.liveRange.intervals()) {
                    ASSERT(interval.distance() == 2);
                    Point lastPoint = interval.end() - 1;
                    BasicBlock* block = findBlockContainingPoint(lastPoint);
                    unsigned instIndex = this->instIndex(positionOfHead(block), lastPoint);
                    Inst& inst = block->at(instIndex);

                    Arg arg = gapTmp;
                    StackSlot* spilled = spillSlot(gapTmp);
                    if (spilled)
                        arg = Arg::stack(spilled);
                    Opcode move = moveOpcode(gapTmp);
                    m_insertionSets[block].insert(instIndex, splitMoveFrom, move, inst.origin, metadata.originalTmp, arg);
                    m_insertionSets[block].insert(instIndex + 1, splitMoveTo, move, inst.origin, arg, metadata.originalTmp);
                    dataLogLnIf(verbose(), "Inserted Moves around clobber tmp=", metadata.originalTmp, " gapTmp=", gapTmp, " gapReg = ", assignedReg(gapTmp), " block=", *block, " index=", instIndex, " inst = ", inst);
                }
            }
        }

        for (BasicBlock* block : m_code)
            m_insertionSets[block].execute(block);
    }

    bool mayBeCoalescable(Inst& inst)
    {
        switch (inst.kind.opcode) {
        case Move:
        case Move32:
        case MoveFloat:
        case MoveDouble:
        case MoveVector:
            break;
        default:
            return false;
        }

        // Avoid the three-argument coalescable spill moves.
        if (inst.args.size() != 2)
            return false;

        if (!inst.args[0].isTmp() || !inst.args[1].isTmp())
            return false;

        // We can coalesce a Move32 so long as either of the following holds:
        // - The input is already zero-filled.
        // - The output only cares about the low 32 bits.
        //
        // Note that the input property requires an analysis over ZDef's, so it's only valid so long
        // as the input gets a register. We don't know if the input gets a register, but we do know
        // that if it doesn't get a register then we will still emit this Move32.
        if (inst.kind.opcode == Move32 && !is32Bit() && m_tmpWidth.defWidth(inst.args[0].tmp()) > Width32)
            return false;
        return true;
    }

    void assignRegisters()
    {
        CompilerTimingScope timingScope("Air"_s, "GreedyRegAlloc::assignRegisters"_s);

        if (verbose()) {
            dataLog("About to assign registers. State of all tmps:\n");
            m_code.forEachTmp(
                [&] (Tmp tmp) {
                    dataLog("    ", tmp, ": ", m_map[tmp], "\n");
                });
            dataLog("IR:\n");
            dataLog(m_code);
        }

        for (BasicBlock* block : m_code) {
            for (Inst& inst : *block) {
                bool mayBeCoalescable = this->mayBeCoalescable(inst);
                dataLogLnIf(verbose(), "At: ", inst, mayBeCoalescable ? " [coalescable]" : "");

                if constexpr (isX86_64()) {
                    // Move32 is cheaper if we know that it's equivalent to a Move in x86_64. It's
                    // equivalent if the destination's high bits are not observable or if the source's high
                    // bits are all zero.
                    if (inst.kind.opcode == Move && inst.args[0].isTmp() && inst.args[1].isTmp()) {
                        if (m_tmpWidth.useWidth(inst.args[1].tmp()) <= Width32 || m_tmpWidth.defWidth(inst.args[0].tmp()) <= Width32)
                            inst.kind.opcode = Move32;
                    }
                }
                if constexpr (isARM64()) {
                    // On the other hand, on ARM64, Move is cheaper than Move32. We would like to use Move instead of Move32.
                    // Move32 on ARM64 is explicitly selected in B3LowerToAir for ZExt32 for example. But using ZDef information
                    // here can optimize it from Move32 to Move.
                    if (inst.kind.opcode == Move32 && inst.args[0].isTmp() && inst.args[1].isTmp()) {
                        if (m_tmpWidth.defWidth(inst.args[0].tmp()) <= Width32)
                            inst.kind.opcode = Move;
                    }
                }

                inst.forEachTmpFast([&](Tmp& tmp) {
                    if (tmp.isReg())
                        return;
                    Reg reg = assignedReg(tmp);
                    if (!reg) {
                        dataLog("Failed to allocate reg for: ", tmp, "\n");
                        RELEASE_ASSERT_NOT_REACHED();
                    }
                    tmp = Tmp(reg);
                });
                ASSERT(inst.isValidForm());

                if (mayBeCoalescable && inst.args[0].isTmp() && inst.args[1].isTmp()
                    && inst.args[0].tmp() == inst.args[1].tmp())
                    inst = Inst();
            }
            // Remove all the useless moves we created in this block.
            block->insts().removeAllMatching([&] (const Inst& inst) {
                return !inst;
            });
        }
    }

    Code& m_code;
    Vector<Reg> m_allowedRegistersInPriorityOrder[numBanks];
    ScalarRegisterSet m_allAllowedRegisters;
    RegisterSet m_allAllowedRegistersWholeWidth;
    IndexMap<BasicBlock*, Point> m_blockToHeadPoint;
    Vector<Point> m_tailPoints;
    TmpMap<TmpData> m_map;
    Vector<SplitMetadata> m_splitMetadata;
    IndexMap<Reg, RegisterRange> m_regRanges;
    PriorityQueue<TmpPriority, TmpPriority::isHigherPriority> m_queue;
    IndexMap<BasicBlock*, PhaseInsertionSet> m_insertionSets;
    BlockWorklist m_fastBlocks;
    UseCounts m_useCounts;
    TmpWidth m_tmpWidth;
    bool m_didSpill { false };
};

} // namespace JSC::B3::Air::Greedy

void allocateRegistersByGreedy(Code& code)
{
    RELEASE_ASSERT(!code.usesSIMD());
    PhaseScope phaseScope(code, "allocateRegistersAndStackByGreedy"_s);
    dataLogIf(Greedy::verbose(), "Air before greedy register allocation:\n", code);
    Greedy::GreedyAllocator allocator(code);
    allocator.run();
    dataLogIf(Greedy::verbose(), "Air after greedy register allocation:\n", code);
}

} } } // namespace JSC::B3::Air

WTF_ALLOW_UNSAFE_BUFFER_USAGE_END

#endif // ENABLE(B3_JIT)
