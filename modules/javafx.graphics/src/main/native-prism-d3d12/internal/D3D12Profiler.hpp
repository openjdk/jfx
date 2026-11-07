/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#pragma once

#include <cstdint>
#include <string>
#include <array>
#include <vector>


namespace D3D12 {
namespace Internal {

class Profiler
{
public:
    enum class Event: uint32_t
    {
        Event = 0, // generic event for any situations we might want to profile
        Signal, // event for signalling command queue
        Wait, // event for waiting for command queue to finish a batch
        Count
    };

private:
    static constexpr uint32_t EVENT_COUNT = static_cast<uint32_t>(Event::Count);

    struct EventSource
    {
        uint32_t id;
        std::string name;
        uint64_t totalHits;
        std::array<uint64_t, EVENT_COUNT> hits;
        uint64_t totalTime;
        uint64_t timingCount;
        uint64_t timerStart;
        bool freed;

        EventSource(uint32_t id, const std::string& name)
            : id(id)
            , name(name)
            , totalHits(0)
            , hits()
            , totalTime(0)
            , timingCount(0)
            , timerStart(0)
            , freed(false)
        {
            for (uint64_t& h: hits)
            {
                h = 0;
            }
        }
    };

    std::vector<EventSource> mEventSources;
    uint32_t mSourceCount;
    uint64_t mFrameCount;
    uint64_t mTimerFreq;

    Profiler();
    ~Profiler();

    Profiler(const Profiler&) = delete;
    Profiler(Profiler&&) = delete;
    Profiler& operator=(const Profiler&) = delete;
    Profiler& operator=(Profiler&&) = delete;

public:
    static Profiler& Instance();

    uint32_t RegisterSource(const std::string& sourceName);
    void RemoveSource(uint32_t sourceID);
    void RenameSource(uint32_t sourceID, const std::string& sourceName);
    void MarkEvent(uint32_t sourceID, Event event);
    void MarkFrameEnd();
    void TimingStart(uint32_t sourceID);
    void TimingEnd(uint32_t sourceID);

    void PrintSummary();
};

} // namespace Internal
} // namespace D3D12
