/*
 * Copyright (C) 2021 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "FloatRect.h"
#include "FloatRoundedRect.h"
#include "InlineIteratorInlineBox.h"
#include "InlineIteratorTextBox.h"
#include "RenderObject.h"
#include "TextBoxSelectableRange.h"
#include "TextDecorationPainter.h"
#include "TextRun.h"

namespace WebCore {

class Color;
class Document;
class RenderCombineText;
class RenderStyle;
class RenderText;
class ShadowData;
struct CompositionUnderline;
struct MarkedText;
struct PaintInfo;
struct StyledMarkedText;

class TextBoxPainter {
public:
    TextBoxPainter(const LayoutIntegration::InlineContent&, const InlineDisplay::Box&, const RenderStyle&, PaintInfo&, const LayoutPoint& paintOffset);
    ~TextBoxPainter();

    void paint();

    static inline FloatSize rotateShadowOffset(const SpaceSeparatedPoint<Style::Length<>>& offset, WritingMode);

protected:
    auto& textBox() const { return m_textBox; }
    InlineIterator::TextBoxIterator makeIterator() const;

    void paintBackground();
    void paintForegroundAndDecorations();
    void paintCompositionBackground();
    void paintCompositionUnderlines();
    void paintCompositionForeground(const StyledMarkedText&);
    void paintPlatformDocumentMarkers();

    enum class BackgroundStyle { Normal, Rounded };
    void paintBackground(unsigned startOffset, unsigned endOffset, const Color&, BackgroundStyle = BackgroundStyle::Normal);
    void paintBackground(const StyledMarkedText&);
    void paintForeground(const StyledMarkedText&);
    TextDecorationPainter createDecorationPainter(const StyledMarkedText&, const FloatRect&);
    void paintBackgroundDecorations(TextDecorationPainter&, const StyledMarkedText&, const FloatRect&);
    void paintForegroundDecorations(TextDecorationPainter&, const StyledMarkedText&, const FloatRect&);
    void paintCompositionUnderline(const CompositionUnderline&, const FloatRoundedRect::Radii&, bool hasLiveConversion);
    void fillCompositionUnderline(float start, float width, const CompositionUnderline&, const FloatRoundedRect::Radii&, bool hasLiveConversion) const;
    void paintPlatformDocumentMarker(const MarkedText&);

    float textPosition();
    FloatRect computePaintRect(const LayoutPoint& paintOffset);
    bool computeHaveSelection() const;
    std::pair<unsigned, unsigned> selectionStartEnd() const;
    MarkedText createMarkedTextFromSelectionInBox();
    const FontCascade& fontCascade() const;
    WritingMode writingMode() const { return m_style.writingMode(); }
    FloatPoint textOriginFromPaintRect(const FloatRect&) const;

    struct DecoratingBox {
        InlineIterator::InlineBoxIterator inlineBox;
        const RenderStyle& style;
        TextDecorationPainter::Styles textDecorationStyles;
        FloatPoint location;
    };
    using DecoratingBoxList = Vector<DecoratingBox>;
    void collectDecoratingBoxesForBackgroundPainting(DecoratingBoxList&, const InlineIterator::TextBoxIterator&, FloatPoint textBoxLocation, const TextDecorationPainter::Styles&);

    // FIXME: We could just talk to the display box directly.
    const InlineIterator::BoxModernPath m_textBox;
    const RenderText& m_renderer;
    const Document& m_document;
    const RenderStyle& m_style;
    const FloatRect m_logicalRect;
    const TextRun m_paintTextRun;
    PaintInfo& m_paintInfo;
    const TextBoxSelectableRange m_selectableRange;
    const LayoutPoint m_paintOffset;
    const FloatRect m_paintRect;
    const bool m_isFirstLine;
    const bool m_isCombinedText;
    const bool m_isPrinting;
    const bool m_haveSelection;
    bool m_containsComposition { false };
    bool m_useCustomUnderlines { false };
    std::optional<bool> m_emphasisMarkExistsAndIsAbove { };
};

inline FloatSize TextBoxPainter::rotateShadowOffset(const SpaceSeparatedPoint<Style::Length<>>& offset, WritingMode writingMode)
{
    if (writingMode.isHorizontal())
        return { offset.x().value, offset.y().value };
    if (writingMode.isLineOverLeft()) // sideways-lr
        return { -offset.y().value, offset.x().value };
    return { offset.y().value, -offset.x().value };
}
}
