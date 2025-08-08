/*
 * Copyright (C) 2011, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1.  Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
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

#include <wtf/TZoneMallocInlines.h>

namespace WebCore {

// Deal with denormals. They can very seriously impact performance on x86.

// Define HAVE_DENORMAL if we support flushing denormals to zero.


class DenormalDisabler final {
    WTF_MAKE_TZONE_ALLOCATED_INLINE(DenormalDisabler);
public:
    DenormalDisabler();
        // Save the current state, and set mode to flush denormals.
        //
        // http://stackoverflow.com/questions/637175/possible-bug-in-controlfp-s-may-not-restore-control-word-correctly

    ~DenormalDisabler();

    // This is a nop if we can flush denormals to zero in hardware.
    static inline float flushDenormalFloatToZero(float f)
    {
#if !HAVE(DENORMAL)
        // For systems using x87 instead of sse, there's no hardware support
        // to flush denormals automatically. Hence, we need to flush
        // denormals to zero manually.
        return (std::abs(f) < FLT_MIN) ? 0.0f : f;
#else
        return f;
#endif
    }
    static constexpr intptr_t kUndefinedStatusWord { -1 };
private:






    const intptr_t m_savedCSR { kUndefinedStatusWord };

// FIXME: add implementations for other architectures and compilers
    const bool m_disablingActivated { false };

    // Assume the worst case that other architectures and compilers
    // need to flush denormals to zero manually.
};


} // WebCore

