/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"
#include <wkj_constants.h>

#include <cstdio>
#include <wtf/Vector.h>
#include <wtf/text/StringBuilder.h>

#include "CSSPropertyNames.h"
#include "CSSFontSelector.h"
#include "CSSValueKeywords.h"
#include "PlatformJavaClasses.h"
#include "HTMLInputElement.h"
#include "HTMLMediaElement.h"
#include "NotImplemented.h"
#include "PaintInfo.h"
#include "PlatformContextJava.h"
#include "RenderObject.h"
#include "RenderElementInlines.h"
#include "RenderProgress.h"
#include "HTMLMeterElement.h"
#include "RenderSlider.h"
#include "RenderThemeJava.h"
#include "ThemeTypes.h"
#include "TimeRanges.h"
#include "UserAgentScripts.h"
#include "UserAgentStyleSheets.h"
#include "Page.h"
#include "RenderStyle+SettersInlines.h"

#include <stdint.h>



/*
 * The constant names are unchanged: they now come from the generated wkj_constants.h instead
 * of from javac -h output, and these two macros compose exactly the same spellings.
 */
#define JNI_EXPAND(n) com_sun_webkit_graphics_RenderTheme_##n
#define JNI_EXPAND_MEDIA(n) com_sun_webkit_graphics_RenderMediaControls_##n

namespace WebCore {

RenderTheme& RenderTheme::singleton()
{
    static RenderTheme& sm_defaultInstance = *new RenderThemeJava();
    return sm_defaultInstance;
}

RenderThemeJava::RenderThemeJava() : mediaResource(MediaControlResourceFactory::createResource())
{
}

int RenderThemeJava::createWidgetState(const RenderElement& o)
{
    int state = 0;
    if (isChecked(o))
        state |= JNI_EXPAND(CHECKED);
    if (isIndeterminate(o))
        state |= JNI_EXPAND(INDETERMINATE);
    if (isEnabled(o))
        state |= JNI_EXPAND(ENABLED);
    if (isFocused(o))
        state |= JNI_EXPAND(FOCUSED);
    if (isPressed(o))
        state |= JNI_EXPAND(PRESSED);
    if (isHovered(o))
        state |= JNI_EXPAND(HOVERED);
    if (isReadOnlyControl(o))
        state |= JNI_EXPAND(READ_ONLY);
    return state;
}

bool RenderThemeJava::paintWidget(
    int widgetIndex,
    const RenderElement& object,
    const PaintInfo &paintInfo,
    const FloatRect &rect) {

    return paintWidget(widgetIndex, object, paintInfo, enclosingIntRect(rect));
}

bool RenderThemeJava::paintWidget(
    int widgetIndex,
    const RenderElement& object,
    const PaintInfo &paintInfo,
    const IntRect &rect)
{
    // platformContext() returns 0 when printing
    if (paintInfo.context().paintingDisabled() || !paintInfo.context().platformContext()) {
        return false;
    }

    auto jRenderTheme = paintInfo.context().platformContext()->jRenderTheme();
    if (!jRenderTheme) {
        return false;
    }

    int state = createWidgetState(object);
    Color bgColor = widgetIndex == JNI_EXPAND(MENU_LIST_BUTTON)
        ? object.style().visitedDependentColor()
        : object.style().visitedDependentBackgroundColor();

    /*
     * The extParams block is the same native-endian scratch buffer the JNI code built; only
     * the element type changes, from a signed byte to uint8_t. The Java side still reads
     * it as a ByteBuffer, and it stays alive for exactly the duration of the createWidget call.
     */
    WTF::Vector<uint8_t> extParams;
    if (JNI_EXPAND(SLIDER) == widgetIndex && is<RenderSlider>(object)) {
        HTMLInputElement& input = downcast<RenderSlider>(object).element();

        extParams.grow(sizeof(int32_t) + 3 * sizeof(float));
        uint8_t *data = const_cast<uint8_t*>(extParams.span().data());
        auto isVertical = int32_t((object.style().appearance() == StyleAppearance::SliderHorizontal)
            ? 0
            : 1);
        memcpy(data, &isVertical, sizeof(isVertical));
        data += sizeof(int32_t);

        auto maximum = float(input.maximum());
        memcpy(data, &maximum, sizeof(maximum));
        data += sizeof(float);

        auto minimum = float(input.minimum());
        memcpy(data, &minimum, sizeof(minimum));
        data += sizeof(float);

        auto valueAsNumber = float(input.valueAsNumber());
        memcpy(data, &valueAsNumber, sizeof(valueAsNumber));
    } else if (JNI_EXPAND(PROGRESS_BAR) == widgetIndex) {
        if (is<RenderProgress>(object)) {
            const RenderProgress& renderProgress = downcast<RenderProgress>(object);

            extParams.grow(sizeof(int32_t) + 3*sizeof(float));
            uint8_t *data = const_cast<uint8_t*>(extParams.span().data());
            auto isDeterminate = int32_t(renderProgress.isDeterminate() ? 1 : 0);
            memcpy(data, &isDeterminate, sizeof(isDeterminate));
            data += sizeof(int32_t);

            auto position = float(renderProgress.position());
            memcpy(data, &position, sizeof(position));
            data += sizeof(float);

            auto animationProgress = float(renderProgress.animationProgress());
            memcpy(data, &animationProgress, sizeof(animationProgress));
            data += sizeof(float);
        }
    } else if (JNI_EXPAND(METER) == widgetIndex) {
        float value = 0;
        int32_t region = 0;
        if (object.isRenderMeter()) {
            HTMLMeterElement* meter = static_cast<HTMLMeterElement*>(object.element());
            value = meter->valueRatio();
            region = meter->gaugeRegion();
        } else if (is<RenderProgress>(object)) {
            const RenderProgress& renderProgress = downcast<RenderProgress>(object);
            value = float(renderProgress.position());
        }

        extParams.grow(sizeof(float) + sizeof(int32_t));
        uint8_t *data = const_cast<uint8_t*>(extParams.span().data());
        memcpy(data, &value, sizeof(value));
        data += sizeof(float);

        memcpy(data, &region, sizeof(region));
    }

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->create_widget) {
        // No theme callback: fall through to the WebKit default render, as a null Ref did.
        return true;
    }

    auto [r, g, b, a] = bgColor.toColorTypeLossy<SRGBA<uint8_t>>().resolved();

    /*
     * The id create_widget returns is owned by this frame, so it is adopted into a WKJHandle
     * and RQRef::create adds its own reference - the same two steps the JNI code took with
     * the local reference the JNI call returned.
     */
    WKJHandle widget { cb->create_widget(
        wkj_ref(*jRenderTheme),
        wkj_from_ptr(&object),
        (int32_t)widgetIndex,
        (int32_t)state,
        (int32_t)rect.width(), (int32_t)rect.height(),
        (int32_t)(a << 24 | r << 16 | g << 8 | b),
        extParams.isEmpty() ? nullptr : extParams.span().data(),
        (int32_t)extParams.size()) };
    RefPtr<RQRef> widgetRef = RQRef::create(widget.get());
    if (!widgetRef.get()) {
        //switch to WebKit default render
        return true;
    }
    wkjCheckAndClearException();

    // widgetRef will go into rq's inner refs vector.
    paintInfo.context().platformContext()->rq().freeSpace(20)
    << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_DRAWWIDGET
    << (int32_t)*jRenderTheme
    << widgetRef
    << (int32_t)rect.x() << (int32_t)rect.y();

    return false;
}

void RenderThemeJava::adjustProgressBarStyle(RenderStyle& style, const Element* element) const
{
     RenderTheme::adjustProgressBarStyle(style, element);;
}

//utatodo: ask Java theme
// These values have been copied from RenderThemeAdwaita.cpp
static const int progressActivityBlocks = 5;
static const int progressAnimationFrames = 75;
static const Seconds progressAnimationInterval = 33_ms;
Seconds RenderThemeJava::animationRepeatIntervalForProgressBar(const RenderProgress&) const
{
    return progressAnimationInterval;
}

Seconds RenderThemeJava::animationDurationForProgressBar() const
{
    return progressAnimationInterval * progressAnimationFrames;
}

bool RenderThemeJava::paintProgressBar(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(PROGRESS_BAR), o, i, rect);
}

bool RenderThemeJava::supportsMeter(StyleAppearance part) const
{
    if (part == StyleAppearance::ProgressBar) {
        return true;
    }
    return (part == StyleAppearance::Meter);
}

bool RenderThemeJava::paintMeter(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(METER), o, i, rect);
}

void RenderThemeJava::setCheckboxSize(RenderStyle& style) const
{
    setRadioSize(style);
}
bool RenderThemeJava::paintCheckbox(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{

    return paintWidget(JNI_EXPAND(CHECK_BOX), o, i, rect);
}

void RenderThemeJava::setRadioSize(RenderStyle& style) const
{
    using namespace Style;

    if (!style.width().isAuto() && !style.height().isAuto())
        return;

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->get_radio_button_size)
        return;

    // The default theme, which is what a null page asked for before.
    WKJHandle theme = wkjRenderThemeForPage(0);
    int radioSize = cb->get_radio_button_size(theme.get());

    wkjCheckAndClearException();

    if (style.width().isAuto())
        style.setWidth(Style::MinimumSize::Fixed(radioSize));

    if (style.height().isAuto())
        style.setHeight(Style::MinimumSize::Fixed(radioSize));
}

bool RenderThemeJava::paintRadio(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(RADIO_BUTTON), o, i, rect);
}

bool RenderThemeJava::paintButton(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(BUTTON), o, i, rect);
}

void RenderThemeJava::adjustTextFieldStyle(RenderStyle&, const Element*) const
{
    notImplemented();
}

bool RenderThemeJava::paintTextField(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(TEXT_FIELD), o, i, rect);
}

void RenderThemeJava::adjustSearchFieldStyle(RenderStyle&, const Element*) const
{
    notImplemented();
}

void RenderThemeJava::adjustSwitchStyle(RenderStyle& style, const Element*) const
{
    notImplemented();
}
bool RenderThemeJava::paintSearchField(const RenderElement&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(TEXT_FIELD), o, i, rect);
}

void RenderThemeJava::adjustTextAreaStyle(RenderStyle& style, const Element* element) const
{
    RenderTheme::adjustTextAreaStyle(style, element);
}

bool RenderThemeJava::paintTextArea(const RenderElement&o, const PaintInfo& i, const FloatRect& r)
{
    return paintTextField(o, i, r);
}

void RenderThemeJava::adjustButtonStyle(RenderStyle& style, const Element*) const
{
    if (style.appearance() == StyleAppearance::PushButton) {
        // Ignore line-height.
        style.setLineHeight(CSS::Keyword::Normal { });// recheck
    }
}

enum JavaControlSize {
    JavaRegularControlSize, // The control is sized as regular.
    JavaSmallControlSize,   // The control has a smaller size.
    JavaMiniControlSize     // The control has a smaller size than JavaSmallControlSize.
};

#if !PLATFORM(JAVA)
static float systemFontSizeForControlSize(JavaControlSize controlSize)
{
    static float sizes[] = { 16.0f, 13.0f, 10.0f };

    return sizes[controlSize];
}
#endif

void RenderThemeJava::adjustSliderTrackStyle(RenderStyle& style, const Element* element) const
{
    //utatodo: we need to measure the control in Java theme.
    RenderTheme::adjustSliderTrackStyle(style, element);
}

bool RenderThemeJava::paintSliderTrack(const RenderElement&object, const PaintInfo& info, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(SLIDER), object, info, rect);
}

void getSliderThumbSize(int32_t sliderType, int *width, int *height)
{
    const WKJHostTheme* cb = wkjTheme();
    int32_t size = 0;
    if (cb && cb->get_slider_thumb_size) {
        size = cb->get_slider_thumb_size(sliderType);
        wkjCheckAndClearException();
    }
    // The width is the high 16 bits and the height the low 16, as the packed int always was.
    *width = (size >> 16) & 0xFFFF;
    *height = size & 0xFFFF;
}

int sliderThumbSize = 20;

void RenderThemeJava::adjustSliderThumbSize(RenderStyle& style, const Element* element) const
{
    auto appearance = style.usedAppearance();
    if (appearance != StyleAppearance::SliderThumbHorizontal && appearance != StyleAppearance::SliderThumbVertical)
        return;

    style.setWidth(Style::PreferredSize::Fixed { static_cast<float>(sliderThumbSize) });
    style.setHeight(Style::PreferredSize::Fixed { static_cast<float>(sliderThumbSize) });
}

bool RenderThemeJava::paintSliderThumb(const RenderElement&, const PaintInfo&, const FloatRect&)
{
    // We've already painted it in paintSliderTrack(), no need to do anything here.
    return false;
}

void RenderThemeJava::adjustMenuListStyle(RenderStyle& style, const Element*) const
{

}

bool RenderThemeJava::paintMenuList(const RenderElement& o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(MENU_LIST), o, i, rect);
}

void RenderThemeJava::adjustMenuListButtonStyle(RenderStyle& style, const Element* e) const
{
    style.resetBorderRadius();
    adjustMenuListStyle(style, e);
}

void RenderThemeJava::paintMenuListButtonDecorations(const RenderBox& o, const PaintInfo& i, const FloatRect& r)
{
    IntRect rect(r.x() + r.width(), r.y(), r.height(), r.height());
    paintWidget(JNI_EXPAND(MENU_LIST_BUTTON), o, i, rect);
}

bool RenderThemeJava::supportsFocusRing(const RenderElement& obj, const RenderStyle& style) const
{
    if (!style.hasAppearance())
        return false;

    switch (style.appearance()) {
    case StyleAppearance::TextField:
    case StyleAppearance::TextArea:
    case StyleAppearance::Button:
    case StyleAppearance::Checkbox:
    case StyleAppearance::Radio:
    case StyleAppearance::Menulist:
        return true;
    default:
        return RenderTheme::supportsFocusRing(obj,style);
    }
}

Color RenderThemeJava::getSelectionColor(int index) const
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->get_selection_color)
        return SRGBA<uint8_t> { 0, 0, 0, 0 };

    // Get from default theme object.
    WKJHandle theme = wkjRenderThemeForPage(0);
    uint32_t color = static_cast<uint32_t>(cb->get_selection_color(theme.get(), index));
    wkjCheckAndClearException();

    return SRGBA<uint8_t> { static_cast<uint8_t>(color >> 16), static_cast<uint8_t>(color >> 8),
        static_cast<uint8_t>(color), static_cast<uint8_t>(color >> 24) };
}

Color RenderThemeJava::platformActiveSelectionBackgroundColor(OptionSet<StyleColorOptions>) const
{
    return getSelectionColor(JNI_EXPAND(BACKGROUND));
}

Color RenderThemeJava::platformInactiveSelectionBackgroundColor(OptionSet<StyleColorOptions> opt) const
{
    return platformActiveSelectionBackgroundColor(opt);
}

Color RenderThemeJava::platformActiveSelectionForegroundColor(OptionSet<StyleColorOptions>) const
{
    return getSelectionColor(JNI_EXPAND(FOREGROUND));
}

Color RenderThemeJava::platformInactiveSelectionForegroundColor(OptionSet<StyleColorOptions> opt) const
{
    return platformActiveSelectionForegroundColor(opt);
}

#if ENABLE(VIDEO)
Vector<String, 2> RenderThemeJava::mediaControlsScripts()
{
    if (m_mediaControlsScript.isEmpty())
        m_mediaControlsScript = StringImpl::createWithoutCopying(ModernMediaControlsJavaScript);
    return { m_mediaControlsScript };
}

static RefPtr<HTMLMediaElement> parentMediaElement(const Node* node)
{
    if (!node)
        return nullptr;
    RefPtr<Node> mediaNode = node->shadowHost();
    if (!mediaNode)
        mediaNode = const_cast<Node*>(node);
    if (!is<HTMLMediaElement>(*mediaNode))
        return nullptr;
    return downcast<HTMLMediaElement>(mediaNode.get());
}
bool RenderThemeJava::paintMediaSliderTrack(const RenderElement& renderObject, const PaintInfo& paintInfo, const IntRect& r)
{
    auto mediaElement = parentMediaElement(renderObject.element());
    if (mediaElement == nullptr)
        return false;

    Ref<TimeRanges> timeRanges = mediaElement->buffered();

    paintInfo.context().platformContext()->rq().freeSpace(4
        + 4                 // number of timeRange pairs
        + timeRanges->length() * 4 *2   // timeRange pairs
        + 4 + 4             // duration and currentTime
        + 4 + 4 + 4 + 4     // x, y, w, h
        )
    << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIA_TIMETRACK
    << (int32_t)timeRanges->length();

    //utatodo: need [double] support
    for (unsigned i = 0; i < timeRanges->length(); i++) {
        paintInfo.context().platformContext()->rq()
        << (float)timeRanges->start(i).releaseReturnValue() << (float)timeRanges->end(i).releaseReturnValue();
    }

    paintInfo.context().platformContext()->rq()
    << (float)mediaElement->duration()
    << (float)mediaElement->currentTime()
    << (int32_t)r.x() <<  (int32_t)r.y() << (int32_t)r.width() << (int32_t)r.height();
    return true;
}
bool RenderThemeJava::paintMediaSliderThumb(const RenderElement& renderObject, const PaintInfo& paintInfo, const IntRect& r)
{
    return paintMediaControl(JNI_EXPAND_MEDIA(TIME_SLIDER_THUMB), renderObject, paintInfo, r);
}
bool RenderThemeJava::paintMediaControl(int32_t type, const RenderElement&, const PaintInfo& paintInfo,
                                        const IntRect& r)
{
    paintInfo.context().platformContext()->rq().freeSpace(24)
    << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIACONTROL
    << type << (int32_t)r.x() <<  (int32_t)r.y()
    << (int32_t)r.width() << (int32_t)r.height();

    return true;
}

Vector<String, 2> RenderThemeJava::mediaControlsStyleSheets(const HTMLMediaElement& media_elem)
{
    if (m_mediaControlsStyleSheet.isEmpty())
        m_mediaControlsStyleSheet = StringImpl::createWithoutCopying(ModernMediaControlsUserAgentStyleSheet);
    return { m_mediaControlsStyleSheet };
}

RefPtr<FragmentedSharedBuffer> RenderThemeJava::mediaControlsImageDataForIconNameAndType(const String& iconName, const String& iconType)
{
     if (iconType != "svg") {
             return nullptr;
     }

     auto it = mediaResource->getImageMap().find(iconName);
     if (it == mediaResource->getImageMap().end()) {
             return nullptr;
     }
     return utf8Buffer(it->value);
}

String RenderThemeJava::mediaControlsBase64StringForIconNameAndType(const String& iconName, const String& iconType)
{
    return mediaResource->getValue(iconName);
}

String RenderThemeJava::mediaControlsFormattedStringForDuration(double durationInSeconds)
{
    // FIXME: Format this somehow, maybe through GDateTime?
    return makeString(durationInSeconds);
}

#undef JNI_EXPAND_MEDIA

#endif  // ENABLE(VIDEO)

}

#undef JNI_EXPAND
