/*
 * Copyright (C) 2024 Apple Inc. All rights reserved.
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

#include <wtf/StringPrintStream.h>

namespace WTF {

// This class is intended for when you want to easily buffer and print a bunch of information
// at the end of some scope/function.
class ScopedPrintStream final : public PrintStream {
public:
    ScopedPrintStream(PrintStream& out = WTF::dataFile())
        : m_out(out)
    { }

    ~ScopedPrintStream() final
    {
        m_out.print(m_buffer.toCString());
        m_out.flush();
    }

WTF_ALLOW_UNSAFE_BUFFER_USAGE_BEGIN
    void vprintf(const char* format, va_list argList) final WTF_ATTRIBUTE_PRINTF(2, 0)
    {
        m_buffer.vprintf(format, argList);
    }
WTF_ALLOW_UNSAFE_BUFFER_USAGE_END

    void reset() { m_buffer.reset(); }

private:
    StringPrintStream m_buffer;
    PrintStream& m_out;
};

} // namespace WTF

using WTF::ScopedPrintStream;
