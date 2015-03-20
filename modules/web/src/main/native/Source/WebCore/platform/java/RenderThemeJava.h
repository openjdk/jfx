/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
    virtual bool supportsHover(const RenderStyle* style) const { return true; }

    // System fonts.
    virtual void systemFont(CSSValueID, FontDescription&) const;

    static RefPtr<RenderTheme> sm_defaultInstance;

protected:
    virtual bool paintCheckbox(RenderObject* o, const PaintInfo& i, const IntRect& r);
    virtual void setCheckboxSize(RenderStyle* style) const;

    virtual bool paintRadio(RenderObject* o, const PaintInfo& i, const IntRect& r);
    virtual void setRadioSize(RenderStyle* style) const;

    virtual void adjustButtonStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintButton(RenderObject*, const PaintInfo&, const IntRect&);

    virtual void adjustTextFieldStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintTextField(RenderObject*, const PaintInfo&, const IntRect&);

    virtual void adjustSearchFieldStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintSearchField(RenderObject*, const PaintInfo&, const IntRect&);

    virtual void adjustMenuListStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintMenuList(RenderObject*, const PaintInfo&, const IntRect&);

    virtual void adjustMenuListButtonStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintMenuListButton(RenderObject*, const PaintInfo&, const IntRect&);

    virtual void adjustTextAreaStyle(StyleResolver*, RenderStyle*, Element* e) const;
    virtual bool paintTextArea(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool supportsFocusRing(const RenderStyle*) const;

    virtual Color platformActiveSelectionBackgroundColor() const;
    virtual Color platformInactiveSelectionBackgroundColor() const;
    virtual Color platformActiveSelectionForegroundColor() const;
    virtual Color platformInactiveSelectionForegroundColor() const;

#if ENABLE(VIDEO)
    virtual String extraMediaControlsStyleSheet();

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

    //virtual bool paintMediaFullscreenButton(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaPlayButton(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaMuteButton(RenderObject*, const PaintInfo&, const IntRect&);
    //virtual bool paintMediaSeekBackButton(RenderObject*, const PaintInfo&, const IntRect&);
    //virtual bool paintMediaSeekForwardButton(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaSliderTrack(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaSliderThumb(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaVolumeSliderContainer(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaVolumeSliderTrack(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaVolumeSliderThumb(RenderObject*, const PaintInfo&, const IntRect&);
    //virtual bool paintMediaRewindButton(RenderObject*, const PaintInfo&, const IntRect&);
    //virtual bool paintMediaReturnToRealtimeButton(RenderObject*, const PaintInfo&, const IntRect&);
    //virtual bool paintMediaToggleClosedCaptionsButton(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaControlsBackground(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaCurrentTime(RenderObject*, const PaintInfo&, const IntRect&);
    virtual bool paintMediaTimeRemaining(RenderObject*, const PaintInfo&, const IntRect&);
    //virtual bool paintMediaFullScreenVolumeSliderTrack(RenderObject*, const PaintInfo&, const IntRect&) { return true; }
    //virtual bool paintMediaFullScreenVolumeSliderThumb(RenderObject*, const PaintInfo&, const IntRect&) { return true; }

#endif

#if ENABLE(PROGRESS_ELEMENT)
    virtual double animationRepeatIntervalForProgressBar(RenderProgress*) const;
    virtual double animationDurationForProgressBar(RenderProgress*) const;
    virtual void adjustProgressBarStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintProgressBar(RenderObject*, const PaintInfo&, const IntRect&);
#endif

#if ENABLE(METER_ELEMENT)
    virtual bool supportsMeter(ControlPart) const;
    virtual bool paintMeter(RenderObject*, const PaintInfo&, const IntRect&);
#endif

#if ENABLE(DATALIST_ELEMENT)
    // Returns size of one slider tick mark for a horizontal track.
    // For vertical tracks we rotate it and use it. i.e. Width is always length along the track.
    virtual IntSize sliderTickSize() const {return IntSize(0, 0); }
    // Returns the distance of slider tick origin from the slider track center.
    virtual int sliderTickOffsetFromTrackCenter() const { return 0; };
#endif

    virtual void adjustSliderThumbSize(RenderStyle*, Element*) const;
    virtual bool paintSliderThumb(RenderObject*, const PaintInfo&, const IntRect&);

    virtual void adjustSliderTrackStyle(StyleResolver*, RenderStyle*, Element*) const;
    virtual bool paintSliderTrack(RenderObject*, const PaintInfo&, const IntRect&);


private:
    int createWidgetState(RenderObject* o);
    bool paintWidget(int widgetIndex, RenderObject* o,
                     const PaintInfo& i, const IntRect& rect);
    Color getSelectionColor(int index) const;
#if ENABLE(VIDEO)
    bool paintMediaControl(jint type, RenderObject*, const PaintInfo&, const IntRect&);
#endif
    RefPtr<RQRef> m_jTheme;
};

}

#endif
