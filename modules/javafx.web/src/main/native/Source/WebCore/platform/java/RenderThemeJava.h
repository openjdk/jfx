/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "RenderTheme.h"
#include "GraphicsContext.h"
#include "StyleResolver.h"

#include <jni.h>

namespace WebCore {

struct ThemeData {
    ThemeData() : m_part(0), m_state(0) {}

    unsigned m_part;
    unsigned m_state;
};

class RenderThemeJava final : public RenderTheme {
public:
    RenderThemeJava();

    bool canPaint(const PaintInfo&) const final { return true; }

    // A method asking if the theme's controls actually care about redrawing when hovered.
    bool supportsHover(const RenderStyle&) const override { return true; }

protected:
    bool paintCheckbox(const RenderObject& o, const PaintInfo& i, const IntRect& r) override;
    void setCheckboxSize(RenderStyle& style) const override;

    bool paintRadio(const RenderObject& o, const PaintInfo& i, const IntRect& r) override;
    void setRadioSize(RenderStyle& style) const override;

    void adjustButtonStyle(RenderStyle&, const Element*) const override;
    bool paintButton(const RenderObject&, const PaintInfo&, const IntRect&) override;

    void adjustTextFieldStyle(RenderStyle&, const Element*) const override;
    bool paintTextField(const RenderObject&, const PaintInfo&, const FloatRect&) override;

    void adjustSearchFieldStyle(RenderStyle&, const Element*) const override;
    bool paintSearchField(const RenderObject&, const PaintInfo&, const IntRect&) override;

    void adjustMenuListStyle(RenderStyle&, const Element*) const override;
    bool paintMenuList(const RenderObject&, const PaintInfo&, const FloatRect&) override;

    void adjustMenuListButtonStyle(RenderStyle&, const Element*) const override;
    bool paintMenuListButtonDecorations(const RenderBox&, const PaintInfo&, const FloatRect&) override;

    void adjustTextAreaStyle(RenderStyle&, const Element* e) const override;
    bool paintTextArea(const RenderObject&, const PaintInfo&, const FloatRect&) override;
    bool supportsFocusRing(const RenderStyle&) const override;

    Color platformActiveSelectionBackgroundColor(OptionSet<StyleColor::Options>) const override;
    Color platformInactiveSelectionBackgroundColor(OptionSet<StyleColor::Options>) const override;
    Color platformActiveSelectionForegroundColor(OptionSet<StyleColor::Options>) const override;
    Color platformInactiveSelectionForegroundColor(OptionSet<StyleColor::Options>) const override;

#if ENABLE(VIDEO)
    String mediaControlsScript() override;
    String extraMediaControlsStyleSheet() override;

    String formatMediaControlsCurrentTime(float currentTime, float duration) const override;
    String formatMediaControlsRemainingTime(float currentTime, float duration) const override;

    bool paintMediaPlayButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaMuteButton(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaVolumeSliderContainer(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaVolumeSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaVolumeSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaControlsBackground(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaCurrentTime(const RenderObject&, const PaintInfo&, const IntRect&) override;
    bool paintMediaTimeRemaining(const RenderObject&, const PaintInfo&, const IntRect&) override;

#endif

#if ENABLE(PROGRESS_ELEMENT)
    double animationRepeatIntervalForProgressBar(RenderProgress&) const override;
    double animationDurationForProgressBar(RenderProgress&) const override;
    void adjustProgressBarStyle(StyleResolver&, RenderStyle&, Element*) const override;
    bool paintProgressBar(const RenderObject&, const PaintInfo&, const IntRect&) override;
#endif

#if ENABLE(METER_ELEMENT)
    bool supportsMeter(ControlPart) const override;
    bool paintMeter(const RenderObject&, const PaintInfo&, const IntRect&) override;
#endif

#if ENABLE(DATALIST_ELEMENT)
    // Returns size of one slider tick mark for a horizontal track.
    // For vertical tracks we rotate it and use it. i.e. Width is always length along the track.
    IntSize sliderTickSize() const override { return IntSize(0, 0); }
    // Returns the distance of slider tick origin from the slider track center.
    int sliderTickOffsetFromTrackCenter() const override { return 0; };
#endif

    void adjustSliderThumbSize(RenderStyle&, const Element*) const override;
    bool paintSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&) override;

    void adjustSliderTrackStyle(RenderStyle&, const Element*) const override;
    bool paintSliderTrack(const RenderObject&, const PaintInfo&, const IntRect&) override;


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
};

} // namespace WebCore
