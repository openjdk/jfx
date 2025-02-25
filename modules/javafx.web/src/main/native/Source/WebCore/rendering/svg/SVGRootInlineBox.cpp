/*
 * Copyright (C) 2006 Oliver Hunt <ojh16@student.canterbury.ac.nz>
 * Copyright (C) 2006-2023 Apple Inc. All rights reserved.
 * Copyright (C) 2014 Google Inc. All rights reserved.
 * Copyright (C) 2007 Nikolas Zimmermann <zimmermann@kde.org>
 * Copyright (C) Research In Motion Limited 2010. All rights reserved.
 * Copyright (C) 2011 Torch Mobile (Beijing) CO. Ltd. All rights reserved.
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
 */

#include "config.h"
#include "SVGRootInlineBox.h"

#include "GraphicsContext.h"
#include "InlineIteratorLogicalOrderTraversal.h"
#include "RenderSVGText.h"
#include "RenderSVGTextPath.h"
#include "SVGElementTypeHelpers.h"
#include "SVGInlineFlowBox.h"
#include "SVGInlineTextBoxInlines.h"
#include "SVGNames.h"
#include "SVGRenderingContext.h"
#include "SVGTextPositioningElement.h"
#include <wtf/TZoneMallocInlines.h>

namespace WebCore {

WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(SVGRootInlineBox);

SVGRootInlineBox::SVGRootInlineBox(RenderSVGText& renderSVGText)
    : LegacyRootInlineBox(renderSVGText)
    , m_logicalHeight(0)
{
}

RenderSVGText& SVGRootInlineBox::renderSVGText() const
{
    return downcast<RenderSVGText>(blockFlow());
}

void SVGRootInlineBox::paint(PaintInfo& paintInfo, const LayoutPoint& paintOffset, LayoutUnit lineTop, LayoutUnit lineBottom)
{
    ASSERT(paintInfo.phase == PaintPhase::Foreground || paintInfo.phase == PaintPhase::Selection);
    ASSERT(!paintInfo.context().paintingDisabled());

    if (renderer().document().settings().layerBasedSVGEngineEnabled()) {
        auto overflowRect(visualOverflowRect(lineTop, lineBottom));
        flipForWritingMode(overflowRect);
        overflowRect.moveBy(paintOffset);

        if (!paintInfo.rect.intersects(overflowRect))
            return;
    }

    bool isPrinting = renderSVGText().document().printing();
    bool hasSelection = !isPrinting && selectionState() != RenderObject::HighlightState::None;
    bool shouldPaintSelectionHighlight = !(paintInfo.paintBehavior.contains(PaintBehavior::SkipSelectionHighlight));

    PaintInfo childPaintInfo(paintInfo);
    childPaintInfo.updateSubtreePaintRootForChildren(&renderer());

    if (hasSelection && shouldPaintSelectionHighlight) {
        for (auto* child = firstChild(); child; child = child->nextOnLine()) {
            if (auto* textBox = dynamicDowncast<SVGInlineTextBox>(*child))
                textBox->paintSelectionBackground(childPaintInfo);
            else if (auto* flowBox = dynamicDowncast<SVGInlineFlowBox>(*child))
                flowBox->paintSelectionBackground(childPaintInfo);
        }
    }

    if (renderer().document().settings().layerBasedSVGEngineEnabled()) {
        for (auto* child = firstChild(); child; child = child->nextOnLine()) {
            if (child->renderer().isRenderText() || !child->boxModelObject()->hasSelfPaintingLayer())
                child->paint(childPaintInfo, paintOffset, lineTop, lineBottom);
        }

        return;
    }

    SVGRenderingContext renderingContext(renderSVGText(), paintInfo, SVGRenderingContext::SaveGraphicsContext);
    if (renderingContext.isRenderingPrepared()) {
        for (auto* child = firstChild(); child; child = child->nextOnLine())
            child->paint(paintInfo, paintOffset, 0, 0);
    }
}

void SVGRootInlineBox::computePerCharacterLayoutInformation()
{
    auto& textRoot = downcast<RenderSVGText>(blockFlow());

    Vector<SVGTextLayoutAttributes*>& layoutAttributes = textRoot.layoutAttributes();
    if (layoutAttributes.isEmpty())
        return;

    if (textRoot.needsReordering())
        reorderValueListsToLogicalOrder(layoutAttributes);

    // Perform SVG text layout phase two (see SVGTextLayoutEngine for details).
    SVGTextLayoutEngine characterLayout(layoutAttributes);
    layoutCharactersInTextBoxes(this, characterLayout);

    // Perform SVG text layout phase three (see SVGTextChunkBuilder for details).
    characterLayout.finishLayout();

    // Perform SVG text layout phase four
    // Position & resize all SVGInlineText/FlowBoxes in the inline box tree, resize the root box as well as the RenderSVGText parent block.
    FloatRect childRect;
    layoutChildBoxes(this, &childRect);
    layoutRootBox(childRect);
}

void SVGRootInlineBox::layoutCharactersInTextBoxes(LegacyInlineFlowBox* start, SVGTextLayoutEngine& characterLayout)
{
    for (auto* child = start->firstChild(); child; child = child->nextOnLine()) {
        if (auto* textBox = dynamicDowncast<SVGInlineTextBox>(*child)) {
            ASSERT(is<RenderSVGInlineText>(textBox->renderer()));
            characterLayout.layoutInlineTextBox(*textBox);
        } else {
            // Skip generated content.
            RefPtr node = child->renderer().node();
            if (!node)
                continue;

            auto& flowBox = downcast<SVGInlineFlowBox>(*child);
            bool isTextPath = node->hasTagName(SVGNames::textPathTag);
            if (isTextPath) {
                // Build text chunks for all <textPath> children, using the line layout algorithm.
                // This is needeed as text-anchor is just an additional startOffset for text paths.
                SVGTextLayoutEngine lineLayout(characterLayout.layoutAttributes());
                layoutCharactersInTextBoxes(&flowBox, lineLayout);

                characterLayout.beginTextPathLayout(downcast<RenderSVGTextPath>(child->renderer()), lineLayout);
            }

            layoutCharactersInTextBoxes(&flowBox, characterLayout);

            if (isTextPath)
                characterLayout.endTextPathLayout();
        }
    }
}

void SVGRootInlineBox::layoutChildBoxes(LegacyInlineFlowBox* start, FloatRect* childRect)
{
    for (auto* child = start->firstChild(); child; child = child->nextOnLine()) {
        FloatRect boxRect;
        if (auto* textBox = dynamicDowncast<SVGInlineTextBox>(*child)) {
            ASSERT(is<RenderSVGInlineText>(textBox->renderer()));

            boxRect = textBox->calculateBoundaries();
            textBox->setX(boxRect.x());
            textBox->setY(boxRect.y());
            textBox->setLogicalWidth(boxRect.width());
            textBox->setLogicalHeight(boxRect.height());
        } else {
            // Skip generated content.
            if (!child->renderer().node())
                continue;

            auto& flowBox = downcast<SVGInlineFlowBox>(*child);
            layoutChildBoxes(&flowBox);

            boxRect = flowBox.calculateBoundaries();
            flowBox.setX(boxRect.x());
            flowBox.setY(boxRect.y());
            flowBox.setLogicalWidth(boxRect.width());
            flowBox.setLogicalHeight(boxRect.height());
        }
        if (childRect)
            childRect->unite(boxRect);
    }
}

void SVGRootInlineBox::layoutRootBox(const FloatRect& childRect)
{
    RenderSVGText& parentBlock = renderSVGText();

    // Finally, assign the root block position, now that all content is laid out.
    parentBlock.updatePositionAndOverflow(childRect);

    // Position all children relative to the parent block.
    for (auto* child = firstChild(); child; child = child->nextOnLine()) {
        // Skip generated content.
        if (!child->renderer().node())
            continue;
        child->adjustPosition(-childRect.x(), -childRect.y());
    }

    // Position ourselves.
    setX(0);
    setY(0);
    setLogicalWidth(childRect.width());
    setLogicalHeight(childRect.height());

    auto boundingRect = enclosingLayoutRect(childRect);
    setLineTopBottomPositions(0, boundingRect.height(), 0, boundingRect.height());
}

LegacyInlineBox* SVGRootInlineBox::closestLeafChildForPosition(const LayoutPoint& point)
{
    LegacyInlineBox* firstLeaf = firstLeafDescendant();
    LegacyInlineBox* lastLeaf = lastLeafDescendant();
    if (firstLeaf == lastLeaf)
        return firstLeaf;

    // FIXME: Check for vertical text!
    LegacyInlineBox* closestLeaf = nullptr;
    for (auto* leaf = firstLeaf; leaf; leaf = leaf->nextLeafOnLine()) {
        if (!leaf->isSVGInlineTextBox())
            continue;
        if (point.y() < leaf->y())
            continue;
        if (point.y() > leaf->y() + leaf->virtualLogicalHeight())
            continue;

        closestLeaf = leaf;
        if (point.x() < leaf->left() + leaf->logicalWidth())
            return leaf;
    }

    return closestLeaf ? closestLeaf : lastLeaf;
}

bool SVGRootInlineBox::nodeAtPoint(const HitTestRequest& request, HitTestResult& result, const HitTestLocation& locationInContainer, const LayoutPoint& accumulatedOffset, LayoutUnit lineTop, LayoutUnit lineBottom, HitTestAction hitTestAction)
{
    for (auto* leaf = firstLeafDescendant(); leaf; leaf = leaf->nextLeafOnLine()) {
        if (!leaf->isSVGInlineTextBox())
            continue;
        if (leaf->nodeAtPoint(request, result, locationInContainer, accumulatedOffset, lineTop, lineBottom, hitTestAction))
            return true;
    }

    return false;
}

static inline void swapItemsInLayoutAttributes(SVGTextLayoutAttributes* firstAttributes, SVGTextLayoutAttributes* lastAttributes, unsigned firstPosition, unsigned lastPosition)
{
    SVGCharacterDataMap::iterator itFirst = firstAttributes->characterDataMap().find(firstPosition + 1);
    SVGCharacterDataMap::iterator itLast = lastAttributes->characterDataMap().find(lastPosition + 1);
    bool firstPresent = itFirst != firstAttributes->characterDataMap().end();
    bool lastPresent = itLast != lastAttributes->characterDataMap().end();
    // We only want to perform the swap if both inline boxes are absolutely positioned.
    if (!firstPresent || !lastPresent)
        return;

        std::swap(itFirst->value, itLast->value);
}

static inline void findFirstAndLastAttributesInVector(Vector<SVGTextLayoutAttributes*>& attributes, RenderSVGInlineText* firstContext, RenderSVGInlineText* lastContext,
                                                      SVGTextLayoutAttributes*& first, SVGTextLayoutAttributes*& last)
{
    first = nullptr;
    last = nullptr;

    unsigned attributesSize = attributes.size();
    for (unsigned i = 0; i < attributesSize; ++i) {
        SVGTextLayoutAttributes* current = attributes[i];
        if (!first && firstContext == &current->context())
            first = current;
        if (!last && lastContext == &current->context())
            last = current;
        if (first && last)
            break;
    }

    ASSERT(first);
    ASSERT(last);
}

static inline void reverseInlineBoxRangeAndValueListsIfNeeded(Vector<SVGTextLayoutAttributes*>& attributes, Vector<InlineIterator::LeafBoxIterator>::iterator first, Vector<InlineIterator::LeafBoxIterator>::iterator last)
{
    // This is a copy of std::reverse(first, last). It additionally assures that the metrics map within the renderers belonging to the InlineBoxes are reordered as well.
    while (true)  {
        if (first == last || first == --last)
            return;
        auto* legacyFirst = (*first)->legacyInlineBox();
        auto* legacyLast = (*last)->legacyInlineBox();
        if (!is<SVGInlineTextBox>(legacyFirst) || !is<SVGInlineTextBox>(legacyLast)) {
            auto temp = *first;
            *first = *last;
            *last = temp;
            ++first;
            continue;
        }

        auto& firstTextBox = downcast<SVGInlineTextBox>(*legacyFirst);
        auto& lastTextBox = downcast<SVGInlineTextBox>(*legacyLast);

        // Reordering is only necessary for BiDi text that is _absolutely_ positioned.
        if (firstTextBox.len() == 1 && firstTextBox.len() == lastTextBox.len()) {
            RenderSVGInlineText& firstContext = firstTextBox.renderer();
            RenderSVGInlineText& lastContext = lastTextBox.renderer();

            SVGTextLayoutAttributes* firstAttributes = nullptr;
            SVGTextLayoutAttributes* lastAttributes = nullptr;
            findFirstAndLastAttributesInVector(attributes, &firstContext, &lastContext, firstAttributes, lastAttributes);
            swapItemsInLayoutAttributes(firstAttributes, lastAttributes, firstTextBox.start(), lastTextBox.start());
        }

        auto temp = *first;
        *first = *last;
        *last = temp;

        ++first;
    }
}

void SVGRootInlineBox::reorderValueListsToLogicalOrder(Vector<SVGTextLayoutAttributes*>& attributes)
{
    auto lineBox = InlineIterator::LineBoxIterator(this);

    InlineIterator::leafBoxesInLogicalOrder(lineBox, [&](auto first, auto last) {
        reverseInlineBoxRangeAndValueListsIfNeeded(attributes, first, last);
    });

}

} // namespace WebCore
