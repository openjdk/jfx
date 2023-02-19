/*
 * Copyright (C) 2022 Apple Inc. All rights reserved.
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
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "BufferSource.h"
#include "ExceptionOr.h"
#include "Formats.h"
#include <JavaScriptCore/Forward.h>
#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>
#include <wtf/Vector.h>
#if !PLATFORM(JAVA)
    #include <zlib.h>
#endif

namespace WebCore {

class CompressionStreamEncoder : public RefCounted<CompressionStreamEncoder> {
public:
    static Ref<CompressionStreamEncoder> create(unsigned char format)
    {
        return adoptRef(*new CompressionStreamEncoder(format));
    }

    ExceptionOr<RefPtr<Uint8Array>> encode(const BufferSource&& input);
    ExceptionOr<RefPtr<Uint8Array>> flush();

    ~CompressionStreamEncoder()
    {
/* removing zlib dependency , as newly added module compression requires zlib */
#if !PLATFORM(JAVA)
        if (initailized)
            deflateEnd(&zstream);
#endif
    }

private:
    // If the user provides too small of an input size we will automatically allocate a page worth of memory instead.
    // Very small input sizes can result in a larger output than their input. This would require an additional
    // encode call then, which is not desired.
    const size_t startingAllocationSize = 16384; // 16KB

    bool initailized { false };
    bool finish { false };
#if !PLATFORM(JAVA)
    z_stream zstream;
#endif

    Formats::CompressionFormat m_format;

    ExceptionOr<Vector<uint8_t>> compress(const uint8_t* input, const size_t inputLength);
    ExceptionOr<bool> initialize();

    explicit CompressionStreamEncoder(unsigned char format)
        : m_format(static_cast<Formats::CompressionFormat>(format))
    {
    }
};
} // namespace WebCore
