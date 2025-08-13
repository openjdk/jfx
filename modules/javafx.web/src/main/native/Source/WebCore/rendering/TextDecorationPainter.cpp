/*
 * (C) 1999 Lars Knoll (knoll@kde.org)
 * (C) 2000 Dirk Mueller (mueller@kde.org)
 * Copyright (C) 2004-2017 Apple Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 */

#include "config.h"
#include "TextDecorationPainter.h"

#include "FilterOperations.h"
#include "FontCascade.h"
#include "GraphicsContext.h"
#include "HTMLAnchorElement.h"
#include "InlineIteratorLineBox.h"
#include "InlineTextBoxStyle.h"
#include "RenderBlock.h"
#include "RenderStyleInlines.h"
#include "RenderText.h"
#include "ShadowData.h"
#include "TextBoxPainter.h"
#include "TextRun.h"

namespace WebCore {

/*
 * Draw one cubic Bezier curve and repeat the same pattern along the the decoration's axis.
 * The start point (p1), controlPoint1, controlPoint2 and end point (p2) of the Bezier curve
 * form a diamond shape:
 *
 *                              step
 *                         |-----------|
 *
 *                   controlPoint1
 *                         +
 *
 *
 *                  . .
 *                .     .
 *              .         .
 * (x1, y1) p1 +           .            + p2 (x2, y2) - <--- Decoration's axis
 *                          .         .               |
 *                            .     .                 |
 *                              . .                   | controlPointDistance
 *                                                    |
 *                                                    |
 *                         +                          -
 *                   controlPoint2
 *
 *             |-----------|
 *                 step
 */
static void strokeWavyTextDecoration(GraphicsContext& context, const FloatRect& rect, WavyStrokeParameters wavyStrokeParameters)
{
    if (rect.isEmpty() || !wavyStrokeParameters.step)
        return;

    FloatPoint p1 = rect.minXMinYCorner();
    FloatPoint p2 = rect.maxXMinYCorner();

    // Extent the wavy line before and after the text so it can cover the whole length.
    p1.setX(p1.x() - 2 * wavyStrokeParameters.step);
    p2.setX(p2.x() + 2 * wavyStrokeParameters.step);

    auto bounds = rect;
    // Offset the bounds and set extra height to ensure the whole wavy line is covered.
    bounds.setY(bounds.y() - wavyStrokeParameters.controlPointDistance);
    bounds.setHeight(bounds.height() + 2 * wavyStrokeParameters.controlPointDistance);
    // Clip the extra wavy line added before
    GraphicsContextStateSaver stateSaver(context);
    context.clip(bounds);

    context.adjustLineToPixelBoundaries(p1, p2, rect.height(), context.strokeStyle());

    Path path;
    path.moveTo(p1);

    ASSERT(p1.y() == p2.y());

    float yAxis = p1.y();
    float x1 = std::min(p1.x(), p2.x());
    float x2 = std::max(p1.x(), p2.x());

    FloatPoint controlPoint1(0, yAxis + wavyStrokeParameters.controlPointDistance);
    FloatPoint controlPoint2(0, yAxis - wavyStrokeParameters.controlPointDistance);

    for (double x = x1; x + 2 * wavyStrokeParameters.step <= x2;) {
        controlPoint1.setX(x + wavyStrokeParameters.step);
        controlPoint2.setX(x + wavyStrokeParameters.step);
        x += 2 * wavyStrokeParameters.step;
        path.addBezierCurveTo(controlPoint1, controlPoint2, FloatPoint(x, yAxis));
    }

    context.setShouldAntialias(true);
    context.setStrokeThickness(rect.height());
    context.strokePath(path);
}

static StrokeStyle textDecorationStyleToStrokeStyle(TextDecorationStyle decorationStyle)
{
    StrokeStyle strokeStyle = StrokeStyle::SolidStroke;
    switch (decorationStyle) {
    case TextDecorationStyle::Solid:
        strokeStyle = StrokeStyle::SolidStroke;
        break;
    case TextDecorationStyle::Double:
        strokeStyle = StrokeStyle::DoubleStroke;
        break;
    case TextDecorationStyle::Dotted:
        strokeStyle = StrokeStyle::DottedStroke;
        break;
    case TextDecorationStyle::Dashed:
        strokeStyle = StrokeStyle::DashedStroke;
        break;
    case TextDecorationStyle::Wavy:
        strokeStyle = StrokeStyle::WavyStroke;
        break;
    }

    return strokeStyle;
}

bool TextDecorationPainter::Styles::operator==(const Styles& other) const
{
    return underline.color == other.underline.color && overline.color == other.overline.color && linethrough.color == other.linethrough.color
        && underline.decorationStyle == other.underline.decorationStyle && overline.decorationStyle == other.overline.decorationStyle && linethrough.decorationStyle == other.linethrough.decorationStyle;
}

TextDecorationPainter::TextDecorationPainter(GraphicsContext& context, const FontCascade& font, const ShadowData* shadow, const FilterOperations* colorFilter, bool isPrinting, WritingMode writingMode)
    : m_context(context)
    , m_isPrinting(isPrinting)
    , m_writingMode(writingMode)
    , m_shadow(shadow)
    , m_shadowColorFilter(colorFilter)
    , m_font(font)
{
}

// Paint text-shadow, underline, overline
void TextDecorationPainter::paintBackgroundDecorations(const RenderStyle& style, const TextRun& textRun, const BackgroundDecorationGeometry& decorationGeometry, OptionSet<TextDecorationLine> decorationType, const Styles& decorationStyle)
{
    auto paintDecoration = [&] (auto decoration, auto underlineStyle, auto& color, auto& rect) {
        m_context.setStrokeColor(color);

        auto strokeStyle = textDecorationStyleToStrokeStyle(underlineStyle);

        if (underlineStyle == TextDecorationStyle::Wavy)
            strokeWavyTextDecoration(m_context, rect, decorationGeometry.wavyStrokeParameters);
        else if (decoration == TextDecorationLine::Underline || decoration == TextDecorationLine::Overline) {
            if ((style.textDecorationSkipInk() == TextDecorationSkipInk::Auto
                || style.textDecorationSkipInk() == TextDecorationSkipInk::All)
                && !m_writingMode.isVerticalTypographic()) {
                if (!m_context.paintingDisabled()) {
                    auto underlineBoundingBox = m_context.computeUnderlineBoundsForText(rect, m_isPrinting);
                    auto intersections = m_font.lineSegmentsForIntersectionsWithRect(textRun, decorationGeometry.textOrigin, underlineBoundingBox);
                    if (!intersections.isEmpty()) {
                        auto dilationAmount = std::min(underlineBoundingBox.height(), style.metricsOfPrimaryFont().height() / 5);
                        auto boundaries = differenceWithDilation({ 0, rect.width() }, WTFMove(intersections), dilationAmount);
                    // We don't use underlineBoundingBox here because drawLinesForText() will run computeUnderlineBoundsForText() internally.
                        m_context.drawLinesForText(rect.location(), rect.height(), boundaries.span(), m_isPrinting, underlineStyle == TextDecorationStyle::Double, strokeStyle);
                    } else
                    m_context.drawLineForText(rect, m_isPrinting, underlineStyle == TextDecorationStyle::Double, strokeStyle);
                }
            } else {
                // FIXME: Need to support text-decoration-skip: none.
                m_context.drawLineForText(rect, m_isPrinting, underlineStyle == TextDecorationStyle::Double, strokeStyle);
            }
        } else
            ASSERT_NOT_REACHED();
    };

    auto areLinesOpaque = !m_isPrinting && (!decorationType.contains(TextDecorationLine::Underline) || decorationStyle.underline.color.isOpaque())
        && (!decorationType.contains(TextDecorationLine::Overline) || decorationStyle.overline.color.isOpaque())
        && (!decorationType.contains(TextDecorationLine::LineThrough) || decorationStyle.linethrough.color.isOpaque());

    float extraOffset = 0.f;
    auto boxOrigin = decorationGeometry.boxOrigin;
    bool clipping = m_shadow && m_shadow->next() && !areLinesOpaque;
    if (clipping) {
        auto clipRect = FloatRect { boxOrigin, FloatSize { decorationGeometry.textBoxWidth, decorationGeometry.clippingOffset } };
        for (const ShadowData* shadow = m_shadow; shadow; shadow = shadow->next()) {
            auto shadowExtent = shadow->paintingExtent();
            auto shadowRect = clipRect;
            shadowRect.inflate(shadowExtent);
            auto shadowOffset = TextBoxPainter::rotateShadowOffset(shadow->location(), m_writingMode);
            shadowRect.move(shadowOffset);
            clipRect.unite(shadowRect);
            extraOffset = std::max(extraOffset, std::max(0.f, shadowOffset.height()) + shadowExtent);
        }
        m_context.save();
        m_context.clip(clipRect);
        extraOffset += decorationGeometry.clippingOffset;
        boxOrigin.move(0.f, extraOffset);
    }

    // These decorations should match the visual overflows computed in visualOverflowForDecorations().
    auto underlineRect = FloatRect { boxOrigin, FloatSize { decorationGeometry.textBoxWidth, decorationGeometry.textDecorationThickness } };
    auto overlineRect = underlineRect;
    if (decorationType.contains(TextDecorationLine::Underline))
        underlineRect.move(0.f, decorationGeometry.underlineOffset);
    if (decorationType.contains(TextDecorationLine::Overline))
        overlineRect.move(0.f, decorationGeometry.overlineOffset);

    auto* shadow = m_shadow;
    do {
        auto applyShadowIfNeeded = [&] {
            if (!shadow)
                return;
            if (!shadow->next()) {
                // The last set of lines paints normally inside the clip.
                boxOrigin.move(0, -extraOffset);
                extraOffset = 0;
            }
            auto shadowColor = style.colorResolvingCurrentColor(shadow->color());
            if (m_shadowColorFilter)
                m_shadowColorFilter->transformColor(shadowColor);

            auto shadowOffset = TextBoxPainter::rotateShadowOffset(shadow->location(), m_writingMode);
            shadowOffset.expand(0, -extraOffset);
            m_context.setDropShadow({ shadowOffset, shadow->radius().value, shadowColor, ShadowRadiusMode::Default });
            shadow = shadow->next();
        };
        applyShadowIfNeeded();

        if (decorationType.contains(TextDecorationLine::Underline) && !underlineRect.isEmpty())
            paintDecoration(TextDecorationLine::Underline, decorationStyle.underline.decorationStyle, decorationStyle.underline.color, underlineRect);
        if (decorationType.contains(TextDecorationLine::Overline) && !overlineRect.isEmpty())
            paintDecoration(TextDecorationLine::Overline, decorationStyle.overline.decorationStyle, decorationStyle.overline.color, overlineRect);
        // We only want to paint the shadow, hence the transparent color, not the actual line-through,
        // which will be painted in paintForegroundDecorations().
        if (shadow && decorationType.contains(TextDecorationLine::LineThrough))
            paintLineThrough({ boxOrigin, decorationGeometry.textBoxWidth, decorationGeometry.textDecorationThickness, decorationGeometry.linethroughCenter, decorationGeometry.wavyStrokeParameters }, Color::transparentBlack, decorationStyle);
    } while (shadow);

    if (clipping)
        m_context.restore();
    else if (m_shadow)
        m_context.clearDropShadow();
}

void TextDecorationPainter::paintForegroundDecorations(const ForegroundDecorationGeometry& foregroundDecorationGeometry, const Styles& decorationStyle)
{
    paintLineThrough(foregroundDecorationGeometry, decorationStyle.linethrough.color, decorationStyle);
}

void TextDecorationPainter::paintLineThrough(const ForegroundDecorationGeometry& foregroundDecorationGeometry, const Color& color, const Styles& decorationStyle)
{
    auto rect = FloatRect { foregroundDecorationGeometry.boxOrigin, FloatSize { foregroundDecorationGeometry.textBoxWidth, foregroundDecorationGeometry.textDecorationThickness } };
    rect.move(0.f, foregroundDecorationGeometry.linethroughCenter);

    m_context.setStrokeColor(color);

    TextDecorationStyle style = decorationStyle.linethrough.decorationStyle;
    auto strokeStyle = textDecorationStyleToStrokeStyle(style);

    if (style == TextDecorationStyle::Wavy)
        strokeWavyTextDecoration(m_context, rect, foregroundDecorationGeometry.wavyStrokeParameters);
    else
        m_context.drawLineForText(rect, m_isPrinting, style == TextDecorationStyle::Double, strokeStyle);
}

static void collectStylesForRenderer(TextDecorationPainter::Styles& result, const RenderObject& renderer, OptionSet<TextDecorationLine> remainingDecorations, bool firstLineStyle, OptionSet<PaintBehavior> paintBehavior, PseudoId pseudoId)
{
    auto extractDecorations = [&] (const RenderStyle& style, OptionSet<TextDecorationLine> decorations) {
        if (decorations.isEmpty())
            return;

        auto color = TextDecorationPainter::decorationColor(style, paintBehavior);
        auto decorationStyle = style.textDecorationStyle();

        if (decorations.contains(TextDecorationLine::Underline)) {
            remainingDecorations.remove(TextDecorationLine::Underline);
            result.underline.color = color;
            result.underline.decorationStyle = decorationStyle;
        }
        if (decorations.contains(TextDecorationLine::Overline)) {
            remainingDecorations.remove(TextDecorationLine::Overline);
            result.overline.color = color;
            result.overline.decorationStyle = decorationStyle;
        }
        if (decorations.contains(TextDecorationLine::LineThrough)) {
            remainingDecorations.remove(TextDecorationLine::LineThrough);
            result.linethrough.color = color;
            result.linethrough.decorationStyle = decorationStyle;
        }
    };

    auto styleForRenderer = [&] (const RenderObject& renderer) -> const RenderStyle& {
        if (pseudoId != PseudoId::None && renderer.style().hasPseudoStyle(pseudoId)) {
            if (auto textRenderer = dynamicDowncast<RenderText>(renderer))
                return *textRenderer->getCachedPseudoStyle({ pseudoId });
            return *downcast<RenderElement>(renderer).getCachedPseudoStyle({ pseudoId });
        }
        return firstLineStyle ? renderer.firstLineStyle() : renderer.style();
    };

    auto* current = &renderer;
    do {
        const auto& style = styleForRenderer(*current);
        extractDecorations(style, style.textDecorationLine());

        if (current->style().display() == DisplayType::RubyAnnotation)
            return;

        current = current->parent();
        if (current && current->isAnonymousBlock()) {
            auto& currentBlock = downcast<RenderBlock>(*current);
            if (auto* continuation = currentBlock.continuation())
                current = continuation;
        }

        if (remainingDecorations.isEmpty())
            break;

    } while (current && !is<HTMLAnchorElement>(current->node()));

    // If we bailed out, use the element we bailed out at (typically a <font> or <a> element).
    if (!remainingDecorations.isEmpty() && current)
        extractDecorations(styleForRenderer(*current), remainingDecorations);
}

Color TextDecorationPainter::decorationColor(const RenderStyle& style, OptionSet<PaintBehavior> paintBehavior)
{
    if (paintBehavior.contains(PaintBehavior::ForceBlackText))
        return Color::black;

    if (paintBehavior.contains(PaintBehavior::ForceWhiteText))
        return Color::white;

    return style.visitedDependentColorWithColorFilter(CSSPropertyTextDecorationColor, paintBehavior);
}

auto TextDecorationPainter::stylesForRenderer(const RenderObject& renderer, OptionSet<TextDecorationLine> requestedDecorations, bool firstLineStyle, OptionSet<PaintBehavior> paintBehavior, PseudoId pseudoId) -> Styles
{
    if (requestedDecorations.isEmpty())
        return { };

    Styles result;
    collectStylesForRenderer(result, renderer, requestedDecorations, false, paintBehavior, pseudoId);
    if (firstLineStyle)
        collectStylesForRenderer(result, renderer, requestedDecorations, true, paintBehavior, pseudoId);
    return result;
}

OptionSet<TextDecorationLine> TextDecorationPainter::textDecorationsInEffectForStyle(const TextDecorationPainter::Styles& style)
{
    OptionSet<TextDecorationLine> decorations;
    if (style.underline.color.isValid())
        decorations.add(TextDecorationLine::Underline);
    if (style.overline.color.isValid())
        decorations.add(TextDecorationLine::Overline);
    if (style.linethrough.color.isValid())
        decorations.add(TextDecorationLine::LineThrough);
    return decorations;
}

} // namespace WebCore
