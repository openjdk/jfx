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
#include "MediaPlayerPrivateWirelessPlayback.h"

#if ENABLE(WIRELESS_PLAYBACK_MEDIA_PLAYER)

#include "Logging.h"
#include <wtf/TZoneMallocInlines.h>

namespace WebCore {

WTF_MAKE_TZONE_ALLOCATED_IMPL(MediaPlayerPrivateWirelessPlayback);

class MediaPlayerFactoryWirelessPlayback final : public MediaPlayerFactory {
    WTF_MAKE_TZONE_ALLOCATED_INLINE(MediaPlayerFactoryWirelessPlayback);
    WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR(MediaPlayerFactoryWirelessPlayback);
private:
    MediaPlayerEnums::MediaEngineIdentifier identifier() const final
    {
        return MediaPlayerEnums::MediaEngineIdentifier::WirelessPlayback;
    }

    Ref<MediaPlayerPrivateInterface> createMediaEnginePlayer(MediaPlayer& player) const final
    {
        return adoptRef(*new MediaPlayerPrivateWirelessPlayback(player));
    }

    void getSupportedTypes(HashSet<String>&) const final
    {
    }

    MediaPlayer::SupportsType supportsTypeAndCodecs(const MediaEngineSupportParameters&) const final
    {
        return MediaPlayer::SupportsType::IsNotSupported;
    }
};

void MediaPlayerPrivateWirelessPlayback::registerMediaEngine(MediaEngineRegistrar registrar)
{
    registrar(makeUnique<MediaPlayerFactoryWirelessPlayback>());
}

MediaPlayerPrivateWirelessPlayback::MediaPlayerPrivateWirelessPlayback(MediaPlayer& player)
    : m_player { player }
#if !RELEASE_LOG_DISABLED
    , m_logger { player.mediaPlayerLogger() }
    , m_logIdentifier { player.mediaPlayerLogIdentifier() }
#endif
{
}

MediaPlayerPrivateWirelessPlayback::~MediaPlayerPrivateWirelessPlayback() = default;

#if !RELEASE_LOG_DISABLED
WTFLogChannel& MediaPlayerPrivateWirelessPlayback::logChannel() const
{
    return LogMedia;
}
#endif

} // namespace WebCore

#endif // ENABLE(WIRELESS_PLAYBACK_MEDIA_PLAYER)
