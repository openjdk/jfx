/*
 * Copyright (C) 2018 Apple Inc. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <wtf/Optional.h>

namespace WebCore {

struct MediaCapabilitiesInfo {
    bool supported { false };
    bool smooth { false };
    bool powerEfficient { false };

    template<class Encoder> void encode(Encoder&) const;
    template<class Decoder> static Optional<MediaCapabilitiesInfo> decode(Decoder&);
};

template<class Encoder>
void MediaCapabilitiesInfo::encode(Encoder& encoder) const
{
    encoder << supported;
    encoder << smooth;
    encoder << powerEfficient;
}

template<class Decoder>
Optional<MediaCapabilitiesInfo> MediaCapabilitiesInfo::decode(Decoder& decoder)
{
    Optional<bool> supported;
    decoder >> supported;
    if (!supported)
        return WTF::nullopt;

    Optional<bool> smooth;
    decoder >> smooth;
    if (!smooth)
        return WTF::nullopt;

    Optional<bool> powerEfficient;
    decoder >> powerEfficient;
    if (!powerEfficient)
        return WTF::nullopt;

    return {{
        *supported,
        *smooth,
        *powerEfficient,
    }};
}

} // namespace WebCore
