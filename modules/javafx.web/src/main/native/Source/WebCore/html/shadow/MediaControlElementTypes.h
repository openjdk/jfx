/*
 * Copyright (C) 2008, 2009, 2010, 2011 Apple Inc. All rights reserved.
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#if ENABLE(VIDEO)

#include "HTMLDivElement.h"
#include "HTMLInputElement.h"
#include "HTMLMediaElement.h"
#include "MediaControllerInterface.h"
#include "RenderObject.h"

namespace WebCore {

enum MediaControlElementType {
    MediaEnterFullscreenButton = 0,
    MediaMuteButton,
    MediaPlayButton,
    MediaSeekBackButton,
    MediaSeekForwardButton,
    MediaSlider,
    MediaSliderThumb,
    MediaRewindButton,
    MediaReturnToRealtimeButton,
    MediaShowClosedCaptionsButton,
    MediaHideClosedCaptionsButton,
    MediaUnMuteButton,
    MediaPauseButton,
    MediaTimelineContainer,
    MediaCurrentTimeDisplay,
    MediaTimeRemainingDisplay,
    MediaStatusDisplay,
    MediaControlsPanel,
    MediaVolumeSliderContainer,
    MediaVolumeSlider,
    MediaVolumeSliderThumb,
    MediaFullScreenVolumeSlider,
    MediaFullScreenVolumeSliderThumb,
    MediaVolumeSliderMuteButton,
    MediaTextTrackDisplayContainer,
    MediaTextTrackDisplay,
    MediaExitFullscreenButton,
    MediaOverlayPlayButton,
    MediaClosedCaptionsContainer,
    MediaClosedCaptionsTrackList,
};

RefPtr<HTMLMediaElement> parentMediaElement(const Node*);
inline RefPtr<HTMLMediaElement> parentMediaElement(const RenderObject& renderer) { return parentMediaElement(renderer.node()); }

MediaControlElementType mediaControlElementType(Node*);

// ----------------------------

class MediaControlElement {
public:
    virtual void hide();
    virtual void show();
    virtual bool isShowing() const;

    virtual MediaControlElementType displayType() { return m_displayType; }

    virtual void setMediaController(MediaControllerInterface* controller) { m_mediaController = controller; }
    virtual MediaControllerInterface* mediaController() const { return m_mediaController; }

protected:
    explicit MediaControlElement(MediaControlElementType, HTMLElement*);
    ~MediaControlElement() = default;

    virtual void setDisplayType(MediaControlElementType);
    virtual bool isMediaControlElement() const { return true; }

private:
    MediaControllerInterface* m_mediaController;
    MediaControlElementType m_displayType;
    HTMLElement* m_element;
};

// ----------------------------

class MediaControlDivElement : public HTMLDivElement, public MediaControlElement {
    WTF_MAKE_ISO_ALLOCATED(MediaControlDivElement);
protected:
    explicit MediaControlDivElement(Document&, MediaControlElementType);

private:
    bool isMediaControlElement() const final { return MediaControlElement::isMediaControlElement(); }
};

// ----------------------------

class MediaControlInputElement : public HTMLInputElement, public MediaControlElement {
    WTF_MAKE_ISO_ALLOCATED(MediaControlInputElement);
protected:
    explicit MediaControlInputElement(Document&, MediaControlElementType);

private:
    bool isMediaControlElement() const final { return MediaControlElement::isMediaControlElement(); }
    virtual void updateDisplayType() { }
};

// ----------------------------

class MediaControlTimeDisplayElement : public MediaControlDivElement {
    WTF_MAKE_ISO_ALLOCATED(MediaControlTimeDisplayElement);
public:
    void setCurrentValue(double);
    double currentValue() const { return m_currentValue; }

protected:
    explicit MediaControlTimeDisplayElement(Document&, MediaControlElementType);

private:
    double m_currentValue;
};

// ----------------------------

class MediaControlMuteButtonElement : public MediaControlInputElement {
    WTF_MAKE_ISO_ALLOCATED(MediaControlMuteButtonElement);
public:
    void changedMute();

    bool willRespondToMouseClickEvents() override { return true; }

protected:
    explicit MediaControlMuteButtonElement(Document&, MediaControlElementType);

    void defaultEventHandler(Event&) override;

private:
    void updateDisplayType() override;
};

// ----------------------------

class MediaControlSeekButtonElement : public MediaControlInputElement {
    WTF_MAKE_ISO_ALLOCATED(MediaControlSeekButtonElement);
public:
    bool willRespondToMouseClickEvents() override { return true; }

protected:
    explicit MediaControlSeekButtonElement(Document&, MediaControlElementType);

    void defaultEventHandler(Event&) override;
    virtual bool isForwardButton() const = 0;

private:
    void setActive(bool /*flag*/ = true, bool /*pause*/ = false) final;
};

// ----------------------------

class MediaControlVolumeSliderElement : public MediaControlInputElement {
    WTF_MAKE_ISO_ALLOCATED(MediaControlVolumeSliderElement);
public:
    bool willRespondToMouseMoveEvents() override;
    bool willRespondToMouseClickEvents() override;
    void setVolume(double);
    void setClearMutedOnUserInteraction(bool);

protected:
    explicit MediaControlVolumeSliderElement(Document&);

    void defaultEventHandler(Event&) override;

private:
    bool m_clearMutedOnUserInteraction;
};

} // namespace WebCore

#endif // ENABLE(VIDEO)
