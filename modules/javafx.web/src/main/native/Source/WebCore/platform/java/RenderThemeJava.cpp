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

#include "config.h"

#include <cstdio>
#include <wtf/Vector.h>
#include <wtf/text/StringBuilder.h>

#include "CSSPropertyNames.h"
#include "CSSFontSelector.h"
#include "CSSValueKeywords.h"
#include "PlatformJavaClasses.h"
#include "HTMLMediaElement.h"
#include "NotImplemented.h"
#include "PaintInfo.h"
#include "PlatformContextJava.h"
#include "RenderObject.h"
#if ENABLE(PROGRESS_ELEMENT)
#include "RenderProgress.h"
#endif
#if ENABLE(METER_ELEMENT)
#include "HTMLMeterElement.h"
#endif
#include "RenderSlider.h"
#include "RenderThemeJava.h"
#include "ThemeTypes.h"
#include "TimeRanges.h"
#include "UserAgentScripts.h"
#include "UserAgentStyleSheets.h"
#include "MediaControlElementTypes.h"
#include "Page.h"

#include "com_sun_webkit_graphics_RenderTheme.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"
#include "com_sun_webkit_graphics_RenderMediaControls.h"


#define RENDER_MEDIA_CONTROLS_CLASS_NAME    "com/sun/webkit/graphics/RenderMediaControls"

#define JNI_EXPAND(n) com_sun_webkit_graphics_RenderTheme_##n
#define JNI_EXPAND_MEDIA(n) com_sun_webkit_graphics_RenderMediaControls_##n

namespace WebCore {

RenderTheme& RenderTheme::singleton()
{
    static RenderTheme& sm_defaultInstance = *new RenderThemeJava();
    return sm_defaultInstance;
}

RenderThemeJava::RenderThemeJava()
{
}

int RenderThemeJava::createWidgetState(const RenderObject& o)
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
    const RenderObject& object,
    const PaintInfo &paintInfo,
    const FloatRect &rect) {

    return paintWidget(widgetIndex, object, paintInfo, enclosingIntRect(rect));
}

bool RenderThemeJava::paintWidget(
    int widgetIndex,
    const RenderObject& object,
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
    RGBA32 bgColor = object.style().visitedDependentColor(
        widgetIndex == JNI_EXPAND(MENU_LIST_BUTTON)
            ? CSSPropertyColor
            : CSSPropertyBackgroundColor
    ).rgb();

    JNIEnv* env = WTF::GetJavaEnv();

    WTF::Vector<jbyte> extParams;
    if (JNI_EXPAND(SLIDER) == widgetIndex && is<RenderSlider>(object)) {
        HTMLInputElement& input = downcast<RenderSlider>(object).element();

        extParams.grow(sizeof(jint) + 3 * sizeof(jfloat));
        jbyte *data = extParams.data();
        auto isVertical = jint((object.style().appearance() == SliderHorizontalPart)
            ? 0
            : 1);
        memcpy(data, &isVertical, sizeof(isVertical));
        data += sizeof(jint);

        auto maximum = jfloat(input.maximum());
        memcpy(data, &maximum, sizeof(maximum));
        data += sizeof(jfloat);

        auto minimum = jfloat(input.minimum());
        memcpy(data, &minimum, sizeof(minimum));
        data += sizeof(jfloat);

        auto valueAsNumber = jfloat(input.valueAsNumber());
        memcpy(data, &valueAsNumber, sizeof(valueAsNumber));
    } else if (JNI_EXPAND(PROGRESS_BAR) == widgetIndex) {
#if ENABLE(PROGRESS_ELEMENT)
        if (is<RenderProgress>(object)) {
            RenderProgress& renderProgress = downcast<RenderProgress>(object);

            extParams.grow(sizeof(jint) + 3*sizeof(jfloat));
            jbyte *data = extParams.data();
            auto isDeterminate = jint(renderProgress.isDeterminate() ? 1 : 0);
            memcpy(data, &isDeterminate, sizeof(isDeterminate));
            data += sizeof(jint);

            auto position = jfloat(renderProgress.position());
            memcpy(data, &position, sizeof(position));
            data += sizeof(jfloat);

            auto animationProgress = jfloat(renderProgress.animationProgress());
            memcpy(data, &animationProgress, sizeof(animationProgress));
            data += sizeof(jfloat);

            auto animationStartTime = jfloat(renderProgress.animationStartTime());
            memcpy(data, &animationStartTime, sizeof(animationStartTime));
        }
#endif
#if ENABLE(METER_ELEMENT)
    } else if (JNI_EXPAND(METER) == widgetIndex) {
        jfloat value = 0;
        jint region = 0;
        if (object.isMeter()) {
            HTMLMeterElement* meter = static_cast<HTMLMeterElement*>(object.node());
            value = meter->valueRatio();
            region = meter->gaugeRegion();
#if ENABLE(PROGRESS_ELEMENT)
        } else if (is<RenderProgress>(object>)) {
            RenderProgress& renderProgress = downcast<RenderProgress>(object);
            value = jfloat(renderProgress.position());
#endif
        }

        extParams.grow(sizeof(jfloat) + sizeof(jint));
        jbyte *data = extParams.data();
        memcpy(data, &value, sizeof(value));
        data += sizeof(jfloat);

        memcpy(data, &region, sizeof(region));
#endif
    }

    static jmethodID mid = env->GetMethodID(PG_GetRenderThemeClass(env), "createWidget",
            "(JIIIIILjava/nio/ByteBuffer;)Lcom/sun/webkit/graphics/Ref;");
    ASSERT(mid);

    RefPtr<RQRef> widgetRef = RQRef::create(
        env->CallObjectMethod(jobject(*jRenderTheme), mid,
            ptr_to_jlong(&object),
            (jint)widgetIndex,
            (jint)state,
            (jint)rect.width(), (jint)rect.height(),
            (jint)bgColor.value(),
            (jobject)JLObject(extParams.isEmpty()
                ? nullptr
                : env->NewDirectByteBuffer(
                    extParams.data(),
                    extParams.size())))
        );
    if (!widgetRef.get()) {
        //switch to WebKit default render
        return true;
    }
    WTF::CheckAndClearException(env);

    // widgetRef will go into rq's inner refs vector.
    paintInfo.context().platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWWIDGET
    << (jint)*jRenderTheme
    << widgetRef
    << (jint)rect.x() << (jint)rect.y();

    return false;
}

#if ENABLE(PROGRESS_ELEMENT)
void RenderThemeJava::adjustProgressBarStyle(StyleResolver&, RenderStyle& style, const Element*) const
{
    style.setBoxShadow(nullptr);
}

//utatodo: ask Java theme
// These values have been copied from RenderThemeChromiumSkia.cpp
static const int progressActivityBlocks = 5;
static const int progressAnimationFrames = 10;
static const double progressAnimationInterval = 0.125;
double RenderThemeJava::animationRepeatIntervalForProgressBar(RenderProgress&) const
{
    return progressAnimationInterval;
}

double RenderThemeJava::animationDurationForProgressBar(RenderProgress&) const
{
    return progressAnimationInterval * progressAnimationFrames * 2; // "2" for back and forth;
}

bool RenderThemeJava::paintProgressBar(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(PROGRESS_BAR), o, i, rect);
}
#endif

#if ENABLE(METER_ELEMENT)
bool RenderThemeJava::supportsMeter(ControlPart part) const
{
#if ENABLE(PROGRESS_ELEMENT)
    if (part == ProgressBarPart) {
        return true;
    }
#endif
    return (part == MeterPart);
}

bool RenderThemeJava::paintMeter(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(METER), o, i, rect);
}
#endif

void RenderThemeJava::setCheckboxSize(RenderStyle& style) const
{
    setRadioSize(style);
}

bool RenderThemeJava::paintCheckbox(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(CHECK_BOX), o, i, rect);
}

void RenderThemeJava::setRadioSize(RenderStyle& style) const
{
    // If the width and height are both specified, then we have nothing to do.
    if ((!style.width().isIntrinsicOrAuto() && !style.height().isAuto())) {
        return;
    }

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetRenderThemeClass(env), "getRadioButtonSize", "()I");
    ASSERT(mid);

    // Get from default theme object.
    int radioRadius = env->CallIntMethod((jobject)PG_GetRenderThemeObjectFromPage(env, nullptr), mid);
    WTF::CheckAndClearException(env);

    if (style.width().isIntrinsicOrAuto()) {
        style.setWidth(Length(radioRadius, Fixed));
    }

    if (style.height().isAuto()) {
        style.setHeight(Length(radioRadius, Fixed));
    }
}

bool RenderThemeJava::paintRadio(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(RADIO_BUTTON), o, i, rect);
}

bool RenderThemeJava::paintButton(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(BUTTON), o, i, rect);
}

void RenderThemeJava::adjustTextFieldStyle(RenderStyle&, const Element*) const
{
    notImplemented();
}

bool RenderThemeJava::paintTextField(const RenderObject&o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(TEXT_FIELD), o, i, rect);
}

void RenderThemeJava::adjustSearchFieldStyle(RenderStyle&, const Element*) const
{
    notImplemented();
}

bool RenderThemeJava::paintSearchField(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(TEXT_FIELD), o, i, rect);
}

void RenderThemeJava::adjustTextAreaStyle(RenderStyle& style, const Element*) const
{
    if (style.paddingTop().isIntrinsicOrAuto())
        style.setPaddingTop(Length(1, Fixed));
    if (style.paddingBottom().isIntrinsicOrAuto())
        style.setPaddingBottom(Length(1, Fixed));
}

bool RenderThemeJava::paintTextArea(const RenderObject&o, const PaintInfo& i, const FloatRect& r)
{
    return paintTextField(o, i, r);
}

void RenderThemeJava::adjustButtonStyle(RenderStyle& style, const Element*) const
{
    if (style.appearance() == PushButtonPart) {
        // Ignore line-height.
        style.setLineHeight(RenderStyle::initialLineHeight());
    }
}

enum JavaControlSize {
    JavaRegularControlSize, // The control is sized as regular.
    JavaSmallControlSize,   // The control has a smaller size.
    JavaMiniControlSize     // The control has a smaller size than JavaSmallControlSize.
};

static float systemFontSizeForControlSize(JavaControlSize controlSize)
{
    static float sizes[] = { 16.0f, 13.0f, 10.0f };

    return sizes[controlSize];
}

void RenderThemeJava::updateCachedSystemFontDescription(CSSValueID propId, FontCascadeDescription& fontDescription) const
{
    // This logic owes much to RenderThemeSafari.cpp.
    static FontCascadeDescription systemFont;
    static FontCascadeDescription smallSystemFont;
    static FontCascadeDescription menuFont;
    static FontCascadeDescription labelFont;
    static FontCascadeDescription miniControlFont;
    static FontCascadeDescription smallControlFont;
    static FontCascadeDescription controlFont;

    FontCascadeDescription* cachedDesc;
    float fontSize = 0;
    switch (propId) {
        case CSSValueSmallCaption:
            cachedDesc = &smallSystemFont;
            if (!smallSystemFont.isAbsoluteSize())
                fontSize = systemFontSizeForControlSize(JavaSmallControlSize);
            break;
        case CSSValueMenu:
            cachedDesc = &menuFont;
            if (!menuFont.isAbsoluteSize())
                fontSize = systemFontSizeForControlSize(JavaRegularControlSize);
            break;
        case CSSValueStatusBar:
            cachedDesc = &labelFont;
            if (!labelFont.isAbsoluteSize())
                fontSize = 10.0f;
            break;
        case CSSValueWebkitMiniControl:
            cachedDesc = &miniControlFont;
            if (!miniControlFont.isAbsoluteSize())
                fontSize = systemFontSizeForControlSize(JavaMiniControlSize);
            break;
        case CSSValueWebkitSmallControl:
            cachedDesc = &smallControlFont;
            if (!smallControlFont.isAbsoluteSize())
                fontSize = systemFontSizeForControlSize(JavaSmallControlSize);
            break;
        case CSSValueWebkitControl:
            cachedDesc = &controlFont;
            if (!controlFont.isAbsoluteSize())
                fontSize = systemFontSizeForControlSize(JavaRegularControlSize);
            break;
        default:
            cachedDesc = &systemFont;
            if (!systemFont.isAbsoluteSize())
                fontSize = 13.0f;
    }

    if (fontSize) {
        cachedDesc->setIsAbsoluteSize(true);
        // cachedDesc->setGenericFamily(FontCascadeDescription::NoFamily);
        //cachedDesc->setOneFamily("Lucida Grande");
        cachedDesc->setOneFamily("Tahoma");
        cachedDesc->setSpecifiedSize(fontSize);
        cachedDesc->setWeight(normalWeightValue());
        cachedDesc->setItalic(normalItalicValue());
    }
    fontDescription = *cachedDesc;
}

void RenderThemeJava::adjustSliderTrackStyle(RenderStyle& style, const Element* element) const
{
    //utatodo: we need to measure the control in Java theme.
    RenderTheme::adjustSliderTrackStyle(style, element);
}

bool RenderThemeJava::paintSliderTrack(const RenderObject&object, const PaintInfo& info, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(SLIDER), object, info, rect);
}

void getSliderThumbSize(jint sliderType, int *width, int *height)
{
    JNIEnv* env = WTF::GetJavaEnv();
    JGClass cls = JLClass(env->FindClass(RENDER_MEDIA_CONTROLS_CLASS_NAME));
    ASSERT(cls);

    jmethodID mid = env->GetStaticMethodID(cls, "fwkGetSliderThumbSize", "(I)I");
    ASSERT(mid);

    jint size = env->CallStaticIntMethod(cls, mid, sliderType);
    WTF::CheckAndClearException(env);
    *width = (size >> 16) & 0xFFFF;
    *height = size & 0xFFFF;
}

//utatodo: we need to measure the control in Java theme, do not make it const
const int sliderThumbWidth = 17;
const int sliderThumbHeight = 17;

void RenderThemeJava::adjustSliderThumbSize(RenderStyle& style, const Element*) const
{
    ControlPart part = style.appearance();
#if ENABLE(VIDEO)
    if (part == SliderThumbVerticalPart || part == SliderThumbHorizontalPart)
#endif
    {
        style.setWidth(Length(sliderThumbHeight, Fixed));
        style.setHeight(Length(sliderThumbWidth, Fixed));
    }
#if ENABLE(VIDEO)
    else if (part == MediaSliderThumbPart) {
        static int timeWidth = 0;
        static int timeHeight;
        if (timeWidth == 0) {
            getSliderThumbSize(JNI_EXPAND_MEDIA(SLIDER_TYPE_TIME), &timeWidth, &timeHeight);
        }
        style.setWidth(Length(timeWidth, Fixed));
        style.setHeight(Length(timeHeight, Fixed));
    } else if (part == MediaVolumeSliderThumbPart) {
        static int volumeWidth = 0;
        static int volumeHeight;
        if (volumeWidth == 0) {
            getSliderThumbSize(JNI_EXPAND_MEDIA(SLIDER_TYPE_VOLUME), &volumeWidth, &volumeHeight);
        }
        style.setWidth(Length(volumeWidth, Fixed));
        style.setHeight(Length(volumeHeight, Fixed));
    }
#endif
}

bool RenderThemeJava::paintSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&)
{
    // We've already painted it in paintSliderTrack(), no need to do anything here.
    return false;
}

void RenderThemeJava::adjustMenuListStyle(RenderStyle& style, const Element*) const
{
    // Add in the padding that we'd like to use.
    style.setPaddingRight(Length(20.0f + style.paddingRight().value(), Fixed));
    style.setPaddingLeft(Length(2.0f + style.paddingLeft().value(), Fixed));
}

bool RenderThemeJava::paintMenuList(const RenderObject& o, const PaintInfo& i, const FloatRect& rect)
{
    return paintWidget(JNI_EXPAND(MENU_LIST), o, i, rect);
}

void RenderThemeJava::adjustMenuListButtonStyle(RenderStyle& style, const Element* e) const
{
    style.resetBorderRadius();
    adjustMenuListStyle(style, e);
}

bool RenderThemeJava::paintMenuListButtonDecorations(const RenderBox& o, const PaintInfo& i, const FloatRect& r)
{
    IntRect rect(r.x() + r.width(), r.y(), r.height(), r.height());

    return paintWidget(JNI_EXPAND(MENU_LIST_BUTTON), o, i, rect);
}

bool RenderThemeJava::supportsFocusRing(const RenderStyle& style) const
{
    if (!style.hasAppearance())
        return false;

    switch (style.appearance()) {
    case TextFieldPart:
    case TextAreaPart:
    case ButtonPart:
    case CheckboxPart:
    case RadioPart:
    case MenulistPart:
        return true;
    default:
        return RenderTheme::supportsFocusRing(style);
    }
}

Color RenderThemeJava::getSelectionColor(int index) const
{
    JNIEnv* env = WTF::GetJavaEnv();
    ASSERT(env);

    static jmethodID mid = env->GetMethodID(PG_GetRenderThemeClass(env), "getSelectionColor", "(I)I");
    ASSERT(mid);

    // Get from default theme object.
    jint c = env->CallIntMethod((jobject)PG_GetRenderThemeObjectFromPage(env, nullptr), mid, index);
    WTF::CheckAndClearException(env);

    return Color(c);
}

Color RenderThemeJava::platformActiveSelectionBackgroundColor(OptionSet<StyleColor::Options>) const
{
    return getSelectionColor(JNI_EXPAND(BACKGROUND));
}

Color RenderThemeJava::platformInactiveSelectionBackgroundColor(OptionSet<StyleColor::Options> opt) const
{
    return platformActiveSelectionBackgroundColor(opt);
}

Color RenderThemeJava::platformActiveSelectionForegroundColor(OptionSet<StyleColor::Options>) const
{
    return getSelectionColor(JNI_EXPAND(FOREGROUND));
}

Color RenderThemeJava::platformInactiveSelectionForegroundColor(OptionSet<StyleColor::Options> opt) const
{
    return platformActiveSelectionForegroundColor(opt);
}

#if ENABLE(VIDEO)
String RenderThemeJava::mediaControlsScript()
{
    StringBuilder scriptBuilder;
    scriptBuilder.appendCharacters(mediaControlsLocalizedStringsJavaScript, sizeof(mediaControlsLocalizedStringsJavaScript));
    scriptBuilder.appendCharacters(mediaControlsBaseJavaScript, sizeof(mediaControlsBaseJavaScript));
    scriptBuilder.appendCharacters(mediaControlsGtkJavaScript, sizeof(mediaControlsGtkJavaScript));
    return scriptBuilder.toString();
}

String RenderThemeJava::extraMediaControlsStyleSheet()
{
    return String(mediaControlsGtkUserAgentStyleSheet, sizeof(mediaControlsGtkUserAgentStyleSheet));
}

String RenderThemeJava::formatMediaControlsCurrentTime(float, float) const
{
    return "";
}

String RenderThemeJava::formatMediaControlsRemainingTime(float currentTime, float duration) const
{
    return formatMediaControlsTime(currentTime) + "/" + formatMediaControlsTime(duration);
}

/*
bool RenderThemeJava::paintMediaFullscreenButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect &r);
*/

bool RenderThemeJava::paintMediaPlayButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect& r)
{
    auto mediaElement = parentMediaElement(o);
    if (mediaElement == nullptr)
        return false;

    // readyState can be NETWORK_EMPTY if preload is NONE
    jint type = mediaElement->readyState() == HTMLMediaElementEnums::ReadyState::HAVE_NOTHING
                    ? JNI_EXPAND_MEDIA(DISABLED_PLAY_BUTTON)
                    : mediaElement->paused()
                        ? JNI_EXPAND_MEDIA(PLAY_BUTTON)
                        : JNI_EXPAND_MEDIA(PAUSE_BUTTON);
    return paintMediaControl(type, o, paintInfo, r);
}

bool RenderThemeJava::paintMediaMuteButton(const RenderObject&o, const PaintInfo& paintInfo, const IntRect& r)
{
    auto mediaElement = parentMediaElement(o);
    if (mediaElement == nullptr)
        return false;

    jint type = !mediaElement->hasAudio()
                    ? JNI_EXPAND_MEDIA(DISABLED_MUTE_BUTTON)
                    : mediaElement->muted()
                        ? JNI_EXPAND_MEDIA(UNMUTE_BUTTON)
                        : JNI_EXPAND_MEDIA(MUTE_BUTTON);
    return paintMediaControl(type, o, paintInfo, r);
}

/*
bool RenderThemeJava::paintMediaSeekBackButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect &r);
bool RenderThemeJava::paintMediaSeekForwardButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect &r);
*/

bool RenderThemeJava::paintMediaSliderTrack(const RenderObject&o, const PaintInfo& paintInfo, const IntRect& r)
{
    auto mediaElement = parentMediaElement(o);
    if (mediaElement == nullptr)
        return false;

    Ref<TimeRanges> timeRanges = mediaElement->buffered();

    paintInfo.context().platformContext()->rq().freeSpace(4
        + 4                 // number of timeRange pairs
        + timeRanges->length() * 4 *2   // timeRange pairs
        + 4 + 4             // duration and currentTime
        + 4 + 4 + 4 + 4     // x, y, w, h
        )
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIA_TIMETRACK
    << (jint)timeRanges->length();

    //utatodo: need [double] support
    for (unsigned i = 0; i < timeRanges->length(); i++) {
        paintInfo.context().platformContext()->rq()
        << (jfloat)timeRanges->start(i).releaseReturnValue() << (jfloat)timeRanges->end(i).releaseReturnValue();
    }

    paintInfo.context().platformContext()->rq()
    << (jfloat)mediaElement->duration()
    << (jfloat)mediaElement->currentTime()
    << (jint)r.x() <<  (jint)r.y() << (jint)r.width() << (jint)r.height();
    return true;
}

bool RenderThemeJava::paintMediaSliderThumb(const RenderObject& o, const PaintInfo& paintInfo, const IntRect& r)
{
    return paintMediaControl(JNI_EXPAND_MEDIA(TIME_SLIDER_THUMB), o, paintInfo, r);
}

bool RenderThemeJava::paintMediaVolumeSliderContainer(const RenderObject& o, const PaintInfo& paintInfo, const IntRect& r)
{
    return paintMediaControl(JNI_EXPAND_MEDIA(VOLUME_CONTAINER), o, paintInfo, r);
}

bool RenderThemeJava::paintMediaVolumeSliderTrack(const RenderObject& o, const PaintInfo& paintInfo, const IntRect& r)
{
    auto mediaElement = parentMediaElement(o);
    if (mediaElement == nullptr)
        return false;

    paintInfo.context().platformContext()->rq().freeSpace(28)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIA_VOLUMETRACK
    << (jfloat)mediaElement->volume()
    << (jint)(mediaElement->hasAudio() && !mediaElement->muted() ? 0 : 1)   // muted
    << (jint)r.x() <<  (jint)r.y() << (jint)r.width() << (jint)r.height();
    return true;

}

bool RenderThemeJava::paintMediaVolumeSliderThumb(const RenderObject& object, const PaintInfo& paintInfo, const IntRect& rect)
{
    return paintMediaControl(JNI_EXPAND_MEDIA(VOLUME_THUMB), object, paintInfo, rect);
}

/*
bool RenderThemeJava::paintMediaRewindButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect &r);
bool RenderThemeJava::paintMediaReturnToRealtimeButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect &r);
bool RenderThemeJava::paintMediaToggleClosedCaptionsButton(const RenderObject& o, const PaintInfo& paintInfo, const IntRect &r);
*/

bool RenderThemeJava::paintMediaControlsBackground(const RenderObject&, const PaintInfo&, const IntRect&)
{
//    return paintMediaControl(JNI_EXPAND_MEDIA(BACKGROUND), o, paintInfo, r);
    return true;
}

bool RenderThemeJava::paintMediaCurrentTime(const RenderObject&, const PaintInfo&, const IntRect&)
{
//    return paintMediaControl(JNI_EXPAND_MEDIA(CURRENT_TIME), o, paintInfo, r);
    return true;
}

bool RenderThemeJava::paintMediaTimeRemaining(const RenderObject&, const PaintInfo&, const IntRect&)
{
//    return paintMediaControl(JNI_EXPAND_MEDIA(REMAINING_TIME), o, paintInfo, r);
    return true;
}

bool RenderThemeJava::paintMediaControl(jint type, const RenderObject&, const PaintInfo& paintInfo, const IntRect& r)
{
    paintInfo.context().platformContext()->rq().freeSpace(24)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIACONTROL
    << type << (jint)r.x() <<  (jint)r.y()
    << (jint)r.width() << (jint)r.height();

    return true;
}


#undef JNI_EXPAND_MEDIA

#endif  // ENABLE(VIDEO)

}

#undef JNI_EXPAND

