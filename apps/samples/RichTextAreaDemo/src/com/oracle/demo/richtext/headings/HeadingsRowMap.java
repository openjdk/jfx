/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.headings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.collections.SetChangeListener;
import jfx.incubator.scene.control.richtext.skin.RowMap;

public class HeadingsRowMap extends RowMap {
    private final HeadingsRTA control;
    private final List<ParagraphRange> hiddenRanges = new ArrayList<>();
    private int[] starts = new int[0];
    private int[] ends = new int[0];
    private int[] hiddenBefore = new int[0];
    private final SetChangeListener<ParagraphRange> collapsedSectionsListener = this::handleCollapsedSectionsChange;

    public HeadingsRowMap(HeadingsRTA control) {
        this.control = control;
        control.getCollapsedSections().addListener(collapsedSectionsListener);
    }

    @Override
    protected int getRowCountImpl(int modelParagraphCount) {
        return modelParagraphCount - countHiddenParagraphsBefore(modelParagraphCount);
    }

    @Override
    protected int getViewRowImpl(int modelIndex) {
        return modelIndex - countHiddenParagraphsBefore(modelIndex);
    }

    @Override
    protected int getModelIndexImpl(int row) {
        int modelIndex = row;
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] - hiddenBefore[i] <= row) {
                modelIndex = row + hiddenBefore[i] + ends[i] - starts[i];
            } else {
                break;
            }
        }
        return modelIndex;
    }

    @Override
    protected boolean isHiddenImpl(int modelIndex) {
        int index = indexOfRange(modelIndex);
        return index >= 0 && modelIndex < ends[index];
    }

    @Override
    protected void refreshMap() {
        hiddenRanges.clear();
        int maxCount = control.getParagraphCount();
        for (ParagraphRange range : control.getCollapsedSections()) {
            ParagraphRange.ofNextParagraph(range, maxCount).ifPresent(this::addHiddenRange);
        }

        int size = hiddenRanges.size();
        starts = new int[size];
        ends = new int[size];
        hiddenBefore = new int[size];
        int hiddenCount = 0;
        for (int i = 0; i < size; i++) {
            ParagraphRange range = hiddenRanges.get(i);
            starts[i] = range.start();
            ends[i] = range.end();
            hiddenBefore[i] = hiddenCount;
            hiddenCount += range.length();
        }
    }

    @Override
    protected void dispose() {
        control.getCollapsedSections().removeListener(collapsedSectionsListener);
        super.dispose();
    }

    private void addHiddenRange(ParagraphRange newRange) {
        int index = Collections.binarySearch(hiddenRanges, newRange);
        if (index < 0) {
            index = -index - 1;
        }
        hiddenRanges.add(index, newRange);
    }

    private int countHiddenParagraphsBefore(int modelIndex) {
        int index = indexOfRange(modelIndex);
        return index >= 0 ? hiddenBefore[index] + Math.min(modelIndex, ends[index]) - starts[index] : 0;
    }

    private int indexOfRange(int modelIndex) {
        int index = Arrays.binarySearch(starts, modelIndex);
        return index >= 0 ? index : -index - 2;
    }

    private void handleCollapsedSectionsChange(SetChangeListener.Change<? extends ParagraphRange> change) {
        notifyChange(true);
    }

}