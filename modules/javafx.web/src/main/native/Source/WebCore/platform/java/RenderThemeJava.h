/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef RenderThemeJava_h
#define RenderThemeJava_h

#include "RenderTheme.h"
#include "GraphicsContext.h"
#include "StyleResolver.h"
#if ENABLE(VIDEO)
#include "MediaControlElements.h"
#endif

#include <jni.h>

namespace WebCore {

struct ThemeData {
    ThemeData() : m_part(0), m_state(0) {}

    unsigned m_part;
    unsigned m_state;
};

class RenderThemeJava : public RenderTheme {
public:
    RenderThemeJava(Page* page);

    // A method asking if the theme's controls actually care about redrawing when hovered.
    virtual bool supportsHover(const RenderStyle& style) const { return true; }

    static RefPtr<RenderTheme> sm_defaultInstance;

protected:
    virtual bool paintCheckbox(const RenderObject& o, const PaintInfo& i, const IntRect& r) override;
    virtual void setCheckboxSize(RenderStyle& style) const override;

    virtual bool paintRadio(const RenderObject& o, const PaintInfo& i, const IntRect& r) override;
    virtual void setRadioSize(RenderStyle& style) const override;

    virtual void adjustButtonStyle(StyleResolver&, RenderStyle&, Element*) const override;
    virtual bool paintButton(const RenderObject&, const PaintInfo&, const IntRect&) override;

    virtual void adjustTextFieldStyle(StyleResolver&, RenderStyle&, Element*) const override;
    virtual bool paintTextField(const RenderObject&, const PaintInfo&, const FloatRect&) override;

    virtual void adjustSearchFieldStyle(StyleResolver&, RenderStyle&, Element*) const override;
    virtual bool paintSearchField(const RenderObject&, const PaintInfo&, const IntRect&) override;

    virtual void adjustMenuListStyle(StyleResolver&, RenderStyle&, Element*) const override;
    virtual bool paintMenuList(const RenderObject&, const PaintInfo&, const FloatRect&) override;

    virtual void adjustMenuListButtonStyle(StyleResolver&, RenderStyle&, Element*) const override;
    virtual bool paintMenuListButtonDecorations(const RenderBox&, const PaintInfo&, const FloatRect&) override;

    virtual void adjustTextAreaStyle(StyleResolver&, RenderStyle&, Element* e) const override;
    virtual bool paintTextArea(const RenderObject&, const PaintInfo&, const FloatRect&) override;
    virtual bool supportsFocusRing(const RenderStyle&) const override;

    virtual Color platformActiveSelectionBackgroundColor() const;
    virtual Color platformInactiveSelectionBackgroundColor() const;
    virtual Color platformActiveSelectionForegroundColor() const;
    virtual Color platformInactiveSelectionForegroundColor() const;

#if ENABLE(VIDEO)
    virtual String mediaControlsScript() override;
    virtual String extraMediaControlsStyleSheet() override;

    // Media controls
    //virtual bool supportsClosedCaptioning() const { return false; }
    //virtual bool hasOwnDisabledStateHandlingFor(ControlPart) const { return false; }
    //virtual bool usesMediaControlStatusDisplay() { return false; }
    //virtual bool usesMediaControlVolumeSlider() const { return true; }
    //virtual bool usesVerticalVolumeSlider() const { return true; }
    //virtual double mediaControlsFadeInDuration() { return 0.1; }
    //virtual double mediaControlsFadeOutDuration() { return 0.3; }
    //virtual String formatMediaControlsTime(float time) const;
    virtual String formatMediaControlsCurrentTime(float currentTime, float duration) const;
    virtual String formatMediaControlsRemainingTime(float currentTime, float duration) const;
    // Returns the media volume slider container's offset from the mute button.
    //virtual IntPoint volumeSliderOffsetFromMuteButton(RenderBox*, const IntSize&) const;

    //virtual bool paintMediaFullscreenButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaPlayButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaMuteButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    //virtual bool paintMediaSeekBackButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    //virtual bool paintMediaSeekForwardButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaVolumeSliderContainer(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaVolumeSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaVolumeSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override;
    //virtual bool paintMediaRewindButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    //virtual bool paintMediaReturnToRealtimeButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    //virtual bool paintMediaToggleClosedCaptionsButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaControlsBackground(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaCurrentTime(const RenderObject&, const PaintInfo&, const IntRect&) override;
    virtual bool paintMediaTimeRemaining(const RenderObject&, const PaintInfo&, const IntRect&) override;
    //virtual bool paintMediaFullScreenVolumeSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override { return true; }
    //virtual bool paintMediaFullScreenVolumeSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override { return true; }

#endif

#if ENABLE(PROGRESS_ELEMENT)
    virtual double animationRepeatIntervalForProgressBar(RenderProgress&) const;
    virtual double animationDurationForProgressBar(RenderProgress&) const;
    virtual void adjustProgressBarStyle(StyleResolver&, RenderStyle&, Element*) const;
    virtual bool paintProgressBar(const RenderObject&, const PaintInfo&, const IntRect&) override;
#endif

#if ENABLE(METER_ELEMENT)
    virtual bool supportsMeter(ControlPart) const;
    virtual bool paintMeter(const RenderObject&, const PaintInfo&, const IntRect&) override;
#endif

#if ENABLE(DATALIST_ELEMENT)
    // Returns size of one slider tick mark for a horizontal track.
    // For vertical tracks we rotate it and use it. i.e. Width is always length along the track.
    virtual IntSize sliderTickSize() const {return IntSize(0, 0); }
    // Returns the distance of slider tick origin from the slider track center.
    virtual int sliderTickOffsetFromTrackCenter() const { return 0; };
#endif

    virtual void adjustSliderThumbSize(RenderStyle&, Element*) const override;
    virtual bool paintSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override;

    virtual void adjustSliderTrackStyle(StyleResolver&, RenderStyle&, Element*) const override;
    virtual bool paintSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override;


private:
    virtual void updateCachedSystemFontDescription(CSSValueID, FontCascadeDescription&) const override;

    int createWidgetState(const RenderObject& o);
    bool paintWidget(int widgetIndex, const RenderObject& o,
                     const PaintInfo& i, const IntRect& rect);
    bool paintWidget(int widgetIndex, const RenderObject& o,
                     const PaintInfo& i, const FloatRect& rect);
    Color getSelectionColor(int index) const;
#if ENABLE(VIDEO)
    bool paintMediaControl(jint type, const RenderObject&, const PaintInfo&, const IntRect&);
#endif
    RefPtr<RQRef> m_jTheme;
};

}

#endif
