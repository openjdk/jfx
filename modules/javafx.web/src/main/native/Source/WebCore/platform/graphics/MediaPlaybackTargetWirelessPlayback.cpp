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

#include "config.h"
#include "MediaPlaybackTargetWirelessPlayback.h"

#if ENABLE(WIRELESS_PLAYBACK_MEDIA_PLAYER)

#include "MediaDeviceRoute.h"
#include "MediaDeviceRouteController.h"
#include <wtf/UUID.h>

namespace WebCore {

Ref<MediaPlaybackTargetWirelessPlayback> MediaPlaybackTargetWirelessPlayback::create(std::optional<WTF::UUID> identifier)
{
#if HAVE(AVROUTING_FRAMEWORK)
    return adoptRef(*new MediaPlaybackTargetWirelessPlayback(MediaDeviceRouteController::singleton().routeForIdentifier(identifier)));
#else
    return adoptRef(*new MediaPlaybackTargetWirelessPlayback(WTF::move(identifier)));
#endif
}

#if HAVE(AVROUTING_FRAMEWORK)

Ref<MediaPlaybackTargetWirelessPlayback> MediaPlaybackTargetWirelessPlayback::create(MediaDeviceRoute& route)
{
    return adoptRef(*new MediaPlaybackTargetWirelessPlayback(route));
}

MediaPlaybackTargetWirelessPlayback::MediaPlaybackTargetWirelessPlayback(RefPtr<MediaDeviceRoute>&& route)
    : MediaPlaybackTarget { Type::WirelessPlayback }
    , m_route { WTF::move(route) }
{
}

#else

MediaPlaybackTargetWirelessPlayback::MediaPlaybackTargetWirelessPlayback(std::optional<WTF::UUID> identifier)
    : MediaPlaybackTarget { Type::WirelessPlayback }
    , m_identifier { WTF::move(identifier) }
{
}

#endif // HAVE(AVROUTING_FRAMEWORK)

MediaPlaybackTargetWirelessPlayback::~MediaPlaybackTargetWirelessPlayback() = default;

std::optional<WTF::UUID> MediaPlaybackTargetWirelessPlayback::identifier() const
{
#if HAVE(AVROUTING_FRAMEWORK)
    if (RefPtr route = m_route)
        return m_route->identifier();
    return std::nullopt;
#else
    return m_identifier;
#endif
}

String MediaPlaybackTargetWirelessPlayback::deviceName() const
{
    // FIXME: provide a real device name
    if (auto identifier = this->identifier())
        return identifier->toString();
    return { };
}

bool MediaPlaybackTargetWirelessPlayback::hasActiveRoute() const
{
#if HAVE(AVROUTING_FRAMEWORK)
    return !!m_route;
#else
    return !!m_identifier;
#endif
}

} // namespace WebCore

#endif // ENABLE(WIRELESS_PLAYBACK_MEDIA_PLAYER)
