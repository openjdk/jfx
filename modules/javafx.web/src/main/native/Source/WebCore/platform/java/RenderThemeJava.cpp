/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "HTMLInputElement.h"
#include "HTMLMediaElement.h"
#include "NotImplemented.h"
#include "PaintInfo.h"
#include "PlatformContextJava.h"
#include "RenderObject.h"
#include "RenderProgress.h"
#include "HTMLMeterElement.h"
#include "RenderSlider.h"
#include "RenderThemeJava.h"
#include "ThemeTypes.h"
#include "TimeRanges.h"
#include "UserAgentScripts.h"
#include "UserAgentStyleSheets.h"
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

RenderThemeJava::RenderThemeJava() : mediaResource(MediaControlResourceFactory::createResource())
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
    Color bgColor = object.style().visitedDependentColor(
        widgetIndex == JNI_EXPAND(MENU_LIST_BUTTON)
            ? CSSPropertyColor
            : CSSPropertyBackgroundColor
    );

    JNIEnv* env = WTF::GetJavaEnv();

    WTF::Vector<jbyte> extParams;
    if (JNI_EXPAND(SLIDER) == widgetIndex && is<RenderSlider>(object)) {
        HTMLInputElement& input = downcast<RenderSlider>(object).element();

        extParams.grow(sizeof(jint) + 3 * sizeof(jfloat));
        jbyte *data = extParams.data();
        auto isVertical = jint((object.style().appearance() == StyleAppearance::SliderHorizontal)
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
        if (is<RenderProgress>(object)) {
            const RenderProgress& renderProgress = downcast<RenderProgress>(object);

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
        }
    } else if (JNI_EXPAND(METER) == widgetIndex) {
        jfloat value = 0;
        jint region = 0;
        if (object.isRenderMeter()) {
            HTMLMeterElement* meter = static_cast<HTMLMeterElement*>(object.node());
            value = meter->valueRatio();
            region = meter->gaugeRegion();
        } else if (is<RenderProgress>(object)) {
            const RenderProgress& renderProgress = downcast<RenderProgress>(object);
            value = jfloat(renderProgress.position());
        }

        extParams.grow(sizeof(jfloat) + sizeof(jint));
        jbyte *data = extParams.data();
        memcpy(data, &value, sizeof(value));
        data += sizeof(jfloat);

        memcpy(data, &region, sizeof(region));
    }

    static jmethodID mid = env->GetMethodID(PG_GetRenderThemeClass(env), "createWidget",
            "(JIIIIILjava/nio/ByteBuffer;)Lcom/sun/webkit/graphics/Ref;");
    ASSERT(mid);

    auto [r, g, b, a] = bgColor.toColorTypeLossy<SRGBA<uint8_t>>().resolved();
    RefPtr<RQRef> widgetRef = RQRef::create(
        env->CallObjectMethod(jobject(*jRenderTheme), mid,
            ptr_to_jlong(&object),
            (jint)widgetIndex,
            (jint)state,
            (jint)rect.width(), (jint)rect.height(),
            (jint)(a << 24 | r << 16 | g << 8 | b),
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

void RenderThemeJava::adjustProgressBarStyle(RenderStyle& style, const Element*) const
{
    style.setBoxShadow(nullptr);
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

bool RenderThemeJava::paintProgressBar(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
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

bool RenderThemeJava::paintMeter(const RenderObject&o, const PaintInfo& i, const IntRect& rect)
{
    return paintWidget(JNI_EXPAND(METER), o, i, rect);
}

void RenderThemeJava::setCheckboxSize(RenderStyle& style) const
{
    setRadioSize(style);
}
bool RenderThemeJava::paintCheckbox(const RenderObject&o, const PaintInfo& i, const FloatRect& rect)
{

    return paintWidget(JNI_EXPAND(CHECK_BOX), o, i, rect);
}

void RenderThemeJava::setRadioSize(RenderStyle& style) const
{
    if ((!style.width().isIntrinsicOrAuto() && !style.height().isAuto())) {
        return;
    }
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(PG_GetRenderThemeClass(env), "getRadioButtonSize", "()I");
    ASSERT(mid);
    int radioRadius = env->CallIntMethod((jobject)PG_GetRenderThemeObjectFromPage(env, nullptr), mid);
    WTF::CheckAndClearException(env);
    if (style.width().isIntrinsicOrAuto()) {
        style.setWidth(Length(radioRadius, LengthType::Fixed));
    }
    if (style.height().isAuto()) {
        style.setHeight(Length(radioRadius, LengthType::Fixed));
    }
}
bool RenderThemeJava::paintRadio(const RenderObject&o, const PaintInfo& i, const FloatRect& rect)
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

void RenderThemeJava::adjustSwitchStyle(RenderStyle& style, const Element*) const
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
        style.setPaddingTop(Length(1, LengthType::Fixed));
    if (style.paddingBottom().isIntrinsicOrAuto())
        style.setPaddingBottom(Length(1, LengthType::Fixed));
}

bool RenderThemeJava::paintTextArea(const RenderObject&o, const PaintInfo& i, const FloatRect& r)
{
    return paintTextField(o, i, r);
}

void RenderThemeJava::adjustButtonStyle(RenderStyle& style, const Element*) const
{
    if (style.appearance() == StyleAppearance::PushButton) {
        // Ignore line-height.
        style.setLineHeight(RenderStyle::initialLineHeight());
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
    StyleAppearance part = style.appearance();
#if ENABLE(VIDEO)
    if (part == StyleAppearance::SliderThumbVertical || part == StyleAppearance::SliderThumbHorizontal)
#endif
    {
        style.setWidth(Length(sliderThumbHeight, LengthType::Fixed));
        style.setHeight(Length(sliderThumbWidth, LengthType::Fixed));
    }
}

bool RenderThemeJava::paintSliderThumb(const RenderObject&, const PaintInfo&, const IntRect&)
{
    // We've already painted it in paintSliderTrack(), no need to do anything here.
    return false;
}

void RenderThemeJava::adjustMenuListStyle(RenderStyle& style, const Element*) const
{
    // Add in the padding that we'd like to use.
    style.setPaddingRight(Length(20.0f + style.paddingRight().value(), LengthType::Fixed));
    style.setPaddingLeft(Length(2.0f + style.paddingLeft().value(), LengthType::Fixed));
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

void RenderThemeJava::paintMenuListButtonDecorations(const RenderBox& o, const PaintInfo& i, const FloatRect& r)
{
    IntRect rect(r.x() + r.width(), r.y(), r.height(), r.height());
    paintWidget(JNI_EXPAND(MENU_LIST_BUTTON), o, i, rect);
}

bool RenderThemeJava::supportsFocusRing(const RenderStyle& style) const
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
    uint32_t color = env->CallIntMethod((jobject)PG_GetRenderThemeObjectFromPage(env, nullptr), mid, index);
    WTF::CheckAndClearException(env);

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

    return { String(ModernMediaControlsJavaScript, sizeof(ModernMediaControlsJavaScript)) };

}

String RenderThemeJava::extraMediaControlsStyleSheet()
{
    return emptyString();

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
bool RenderThemeJava::paintMediaSliderTrack(const RenderObject& renderObject, const PaintInfo& paintInfo, const IntRect& r)
{
    auto mediaElement = parentMediaElement(renderObject.node());
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
bool RenderThemeJava::paintMediaSliderThumb(const RenderObject& renderObject, const PaintInfo& paintInfo, const IntRect& r)
{
    return paintMediaControl(JNI_EXPAND_MEDIA(TIME_SLIDER_THUMB), renderObject, paintInfo, r);
}
bool RenderThemeJava::paintMediaControl(jint type, const RenderObject&, const PaintInfo& paintInfo, const IntRect& r)
{
    paintInfo.context().platformContext()->rq().freeSpace(24)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RENDERMEDIACONTROL
    << type << (jint)r.x() <<  (jint)r.y()
    << (jint)r.width() << (jint)r.height();

    return true;
}

String RenderThemeJava::mediaControlsStyleSheet()
{
    return String(ModernMediaControlsUserAgentStyleSheet, sizeof(ModernMediaControlsUserAgentStyleSheet));
}

String RenderThemeJava::mediaControlsBase64StringForIconNameAndType(const String& iconName, const String& iconType)
{
    return mediaResource->getValue(iconName);
}
#undef JNI_EXPAND_MEDIA

#endif  // ENABLE(VIDEO)

}

#undef JNI_EXPAND

