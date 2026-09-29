/*
 * Copyright (C) 2025 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "ImplicitGrid.h"

#include "GridAreaLines.h"
#include "GridLayout.h"
#include "RenderStyle+GettersInlines.h"
#include "PlacedGridItem.h"
#include "UnplacedGridItem.h"
#include <wtf/Assertions.h>
#include <wtf/Range.h>

namespace WebCore {
namespace Layout {

// The implicit grid is created from the explicit grid + items that are placed outside
// of the explicit grid. Since we know the explicit tracks from style we start the
// implicit grid as exactly the explicit grid and allow placement to add implicit
// tracks and grow the grid.

ImplicitGrid::ImplicitGrid(size_t totalColumnsCount, size_t totalRowsCount)
    : m_gridMatrix(Vector(totalRowsCount, Vector<GridCell>(totalColumnsCount)))
{
}

void ImplicitGrid::insertUnplacedGridItem(const UnplacedGridItem& unplacedGridItem)
{
    // https://drafts.csswg.org/css-grid/#common-uses-numeric
    // Grid positions have already been normalized to non-negative matrix indices.
    auto [columnStart, columnEnd] = unplacedGridItem.normalizedColumnStartEnd();
    auto [rowStart, rowEnd] = unplacedGridItem.normalizedRowStartEnd();

    // Multi-cell items (spanning multiple columns) are not yet supported.
    if (columnEnd - columnStart > 1) {
        ASSERT_NOT_IMPLEMENTED_YET();
        return;
    }

    // Multi-cell items (spanning multiple rows) are not yet supported.
    if (rowEnd - rowStart > 1) {
        ASSERT_NOT_IMPLEMENTED_YET();
        return;
    }

    auto columnsRange = WTF::Range(columnStart, columnEnd);
    auto rowsRange = WTF::Range(rowStart, rowEnd);
    for (auto rowIndex = rowsRange.begin(); rowIndex < rowsRange.end(); ++rowIndex) {
        for (auto columnIndex = columnsRange.begin(); columnIndex < columnsRange.end(); ++columnIndex)
            m_gridMatrix[rowIndex][columnIndex].append(unplacedGridItem);
    }

}

GridAreas ImplicitGrid::gridAreas() const
{
    GridAreas gridAreas;
    gridAreas.reserveInitialCapacity(rowsCount() * columnsCount());

    for (size_t rowIndex = 0; rowIndex < m_gridMatrix.size(); ++rowIndex) {
        for (size_t columnIndex = 0; columnIndex < m_gridMatrix[rowIndex].size(); ++columnIndex) {

            const auto& gridCell = m_gridMatrix[rowIndex][columnIndex];
            for (const auto& unplacedGridItem : gridCell) {
                gridAreas.ensure(unplacedGridItem, [&]() {
                    return GridAreaLines { columnIndex, columnIndex + 1, rowIndex, rowIndex + 1 };
                });
            }
        }
    }
    return gridAreas;
}

void ImplicitGrid::insertDefiniteRowItem(const UnplacedGridItem& unplacedGridItem, GridAutoFlowOptions autoFlowOptions, HashMap<size_t, size_t, DefaultHash<size_t>, WTF::UnsignedWithZeroKeyHashTraits<size_t>>* rowCursors)
{
    // Step 2 of CSS Grid auto-placement algorithm:
    // Process items locked to a given row (definite row position, auto column position)
    // See: https://www.w3.org/TR/css-grid-1/#auto-placement-algo

    auto columnSpan = unplacedGridItem.columnSpanSize();
    // FIXME: Support multi-column spans
    ASSERT(columnSpan == 1);

    ASSERT(unplacedGridItem.hasDefiniteRowPosition() && !unplacedGridItem.hasDefiniteColumnPosition());
    auto [normalizedRowStart, normalizedRowEnd] = unplacedGridItem.normalizedRowStartEnd();
    // FIXME: Support multi-row spans
    ASSERT(normalizedRowEnd - normalizedRowStart == 1);

    auto findColumnPosition = [&]() -> std::optional<size_t> {
        if (autoFlowOptions.strategy == PackingStrategy::Dense) {
            // Dense packing: always start searching from column 0
            return findFirstAvailableColumnPosition(normalizedRowStart, normalizedRowEnd, columnSpan, 0);
        }
        // Sparse packing: use per-row cursors to maintain placement order
        // For multi-row items, use the maximum cursor position across all spanned rows
        ASSERT(autoFlowOptions.strategy == PackingStrategy::Sparse);
        size_t startSearchColumn = 0;
        for (size_t row = normalizedRowStart; row < normalizedRowEnd; ++row)
            startSearchColumn = std::max(startSearchColumn, rowCursors->get(row));
        return findFirstAvailableColumnPosition(normalizedRowStart, normalizedRowEnd, columnSpan, startSearchColumn);
    };

    auto growGridToFit = [&](size_t columnSpan, size_t normalizedRowStart, size_t normalizedRowEnd, size_t currentColumnsCount) {
        // Find the last occupied column in the spanned rows
        size_t lastOccupiedColumn = 0;
        for (size_t row = normalizedRowStart; row < normalizedRowEnd; ++row) {
            for (size_t column = currentColumnsCount; column > 0; --column) {
                if (!m_gridMatrix[row][column - 1].isEmpty()) {
                    lastOccupiedColumn = std::max(lastOccupiedColumn, column - 1);
                    break;
                }
            }
        }

        size_t minimumColumnsNeeded = lastOccupiedColumn + 1 + columnSpan;
        for (auto& row : m_gridMatrix)
            row.resize(minimumColumnsNeeded);
    };

    auto columnPosition = findColumnPosition();

    if (!columnPosition) {
        growGridToFit(columnSpan, normalizedRowStart, normalizedRowEnd, columnsCount());

        // Retry finding position in the grown grid
        columnPosition = findColumnPosition();
#ifndef NDEBUG
        ASSERT(columnPosition); // Must succeed after growing

        // Verify the found position doesn't overlap with existing items
        ASSERT(isCellRangeEmpty(*columnPosition, *columnPosition + columnSpan, normalizedRowStart, normalizedRowEnd),
            "After grid growth, placed item overlaps with occupied cells.");

        auto verifyHasEmptyLastColumn = [&]() {
            for (size_t row = 0; row < m_gridMatrix.size(); ++row) {
                if (!m_gridMatrix[row].last().isEmpty())
                    return false;
            }
            ASSERT_NOT_REACHED();
            return true;
        };
        verifyHasEmptyLastColumn();
#endif
    }

    insertItemInArea(unplacedGridItem, *columnPosition, *columnPosition + columnSpan, normalizedRowStart, normalizedRowEnd);

    if (autoFlowOptions.strategy != PackingStrategy::Dense) {
        for (size_t row = normalizedRowStart; row < normalizedRowEnd; ++row)
            rowCursors->set(row, *columnPosition + columnSpan);
    }
}

std::optional<size_t> ImplicitGrid::findFirstAvailableColumnPosition(size_t rowStart, size_t rowEnd, size_t columnSpan, size_t startSearchColumn) const
{
    auto currentColumnsCount = columnsCount();

    // If we can't fit the span starting from the search position, signal that we need to grow the grid
    if (startSearchColumn + columnSpan > currentColumnsCount)
        return std::nullopt;

    // Search within existing grid bounds
    for (size_t columnStart = startSearchColumn; columnStart <= currentColumnsCount - columnSpan; ++columnStart) {
        if (isCellRangeEmpty(columnStart, columnStart + columnSpan, rowStart, rowEnd))
            return columnStart;
    }
    // If we are unable to find a valid position, signal that we need to grow the grid.
    return std::nullopt;
}

bool ImplicitGrid::isCellRangeEmpty(size_t columnStart, size_t columnEnd, size_t rowStart, size_t rowEnd) const
{
    for (size_t row = rowStart; row < rowEnd; ++row) {
        for (size_t column = columnStart; column < columnEnd; ++column) {
            if (!m_gridMatrix[row][column].isEmpty())
                return false;
        }
    }
    return true;
}

void ImplicitGrid::insertItemInArea(const UnplacedGridItem& unplacedGridItem, size_t columnStart, size_t columnEnd, size_t rowStart, size_t rowEnd)
{
    for (size_t row = rowStart; row < rowEnd; ++row) {
        for (size_t column = columnStart; column < columnEnd; ++column)
            m_gridMatrix[row][column].append(unplacedGridItem);
    }
}

} // namespace Layout
} // namespace WebCore
