/*
 * Copyright (C) 2019 Apple Inc. All rights reserved.
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

#pragma once

#if ENABLE(LAYOUT_FORMATTING_CONTEXT)

#include "FormattingContext.h"
#include "TableFormattingState.h"
#include <wtf/IsoMalloc.h>

namespace WebCore {
namespace Layout {

// This class implements the layout logic for table formatting contexts.
// https://www.w3.org/TR/CSS22/tables.html
class TableFormattingContext : public FormattingContext {
    WTF_MAKE_ISO_ALLOCATED(TableFormattingContext);
public:
    TableFormattingContext(const Box& formattingContextRoot, TableFormattingState&);
    void layout() const override;

private:
    class Geometry : public FormattingContext::Geometry {
    public:
        static HeightAndMargin tableCellHeightAndMargin(const LayoutState&, const Box&);
    };

    IntrinsicWidthConstraints computedIntrinsicWidthConstraints() const override;
    LayoutUnit computedTableWidth() const;

    void ensureTableGrid() const;
    void computePreferredWidthForColumns() const;
    void distributeAvailableWidth(LayoutUnit extraHorizontalSpace) const;

    TableFormattingState& formattingState() const { return downcast<TableFormattingState>(FormattingContext::formattingState()); }
};

}
}
#endif
