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
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ContentChange;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

public class HeadingsRTA extends RichTextArea {

    public static final double HEADING_FONT_SIZE = 16.0;
    public static final double BODY_FONT_SIZE = 13.0;
    private static final Pattern HEADING_PATTERN = Pattern.compile("\\s*Header\\s+\\d+\\..*");

    private static final StyleAttributeMap HEADING_ATTRS = StyleAttributeMap.builder().
            setBold(true).
            setFontSize(HEADING_FONT_SIZE).
            setTextColor(Color.FIREBRICK).
            setSpaceAbove(12).
            setSpaceBelow(4).
            build();

    private static final StyleAttributeMap BODY_ATTRS = StyleAttributeMap.builder().
            setBold(false).
            setFontSize(BODY_FONT_SIZE).
            setTextColor(Color.BLACK).
            setSpaceAbove(2).
            setSpaceBelow(2).
            build();

    private final ObservableSet<ParagraphRange> collapsedSections =
            FXCollections.observableSet(new TreeSet<>());
    private final ObservableSet<ParagraphRange> unmodifiableCollapsedSections =
            FXCollections.unmodifiableObservableSet(collapsedSections);

    private final StyledTextModel.Listener modelListener = this::handleModelChange;
    private final HeadingsDecorator headingsDecorator = new HeadingsDecorator(this);

    public HeadingsRTA() {
        super(new RichTextModel());
        setWrapText(true);
        setContentPadding(new Insets(10));
        setHighlightCurrentParagraph(true);
        setLeftDecorator(headingsDecorator);

        getModel().addListener(modelListener);
        modelProperty().addListener((_, oldModel, newModel) -> {
            if (oldModel != null) {
                oldModel.removeListener(modelListener);
            }
            if (newModel != null) {
                newModel.addListener(modelListener);
            }
            expandAllSections();
        });
        collapsedSections.addListener((SetChangeListener<ParagraphRange>) _ -> {
            getHeadingsDecorator().refreshDecorator();
        });
    }

    @Override
    protected RichTextAreaSkin createDefaultSkin() {
        return new HeadingsRTASkin(this);
    }

    public final HeadingsDecorator getHeadingsDecorator() {
        return headingsDecorator;
    }

    public void loadDocument(String text) {
        expandAllSections();
        clear();
        appendText(text);

        int sz = getParagraphCount();
        for (int i = 0; i < sz; i++) {
            String s = getPlainText(i);
            if (s != null) {
                setHeading(i, HEADING_PATTERN.matcher(s).matches());
            }
        }
        clearUndoRedo();
        select(TextPos.ZERO);
    }

    public void setHeading(int index, boolean heading) {
        if (index >= 0 && index < getParagraphCount()) {
            applyStyle(TextPos.ofLeading(index, 0), getParagraphEnd(index), heading ? HEADING_ATTRS : BODY_ATTRS);
        }
    }

    public boolean isHeading(int index) {
        if (index < 0 || index >= getParagraphCount()) {
            return false;
        }
        String text = getPlainText(index);
        if (text == null || text.isBlank()) {
            return false;
        }
        StyleAttributeMap a = getStyleAttributeMap(TextPos.ofLeading(index, 0), false);
        Double size = a.getFontSize();
        return a.isBold() && size != null && size >= HEADING_FONT_SIZE;
    }

    public void collapseAllSections() {
        collapsedSections.addAll(getSections());
    }

    public void expandAllSections() {
        collapsedSections.clear();
    }

    public final ObservableSet<ParagraphRange> getCollapsedSections() {
        return unmodifiableCollapsedSections;
    }

    public boolean isSectionCollapsed(ParagraphRange section) {
        return section != null && findCollapsedSection(section.start()) != null;
    }

    public void collapseSection(ParagraphRange section) {
        if (section != null) {
            ParagraphRange s = getSection(section.start());
            if (s != null) {
                collapsedSections.add(s);
            }
        }
    }

    public void expandSection(ParagraphRange section) {
        if (section != null) {
            ParagraphRange s = findCollapsedSection(section.start());
            if (s != null) {
                collapsedSections.remove(s);
            }
        }
    }

    public void toggleSection(ParagraphRange section) {
        if (isSectionCollapsed(section)) {
            expandSection(section);
        } else {
            collapseSection(section);
        }
    }

    ParagraphRange getSection(int headingIndex) {
        if (!isHeading(headingIndex)) {
            return null;
        }
        int maxCount = getParagraphCount();
        int index = headingIndex + 1;
        while (index < maxCount && !isHeading(index)) {
            index++;
        }
        return new ParagraphRange(headingIndex, index);
    }

    private List<ParagraphRange> getSections() {
        int maxCount = getParagraphCount();
        List<ParagraphRange> list = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < maxCount; i++) {
            if (isHeading(i)) {
                if (start >= 0) {
                    list.add(new ParagraphRange(start, i));
                }
                start = i;
            }
        }
        if (start >= 0) {
            list.add(new ParagraphRange(start, maxCount));
        }
        return list;
    }

    private ParagraphRange findCollapsedSection(int headingIndex) {
        for (ParagraphRange r : collapsedSections) {
            if (r.start() == headingIndex) {
                return r;
            }
            if (r.start() > headingIndex) {
                break;
            }
        }
        return null;
    }

    private void handleModelChange(ContentChange change) {
        if (!change.isEdit()) {
            return;
        }
        if (collapsedSections.isEmpty()) {
            getHeadingsDecorator().refreshDecorator();
            return;
        }

        int start = change.getStart().index();
        int end = change.getEnd().index();
        int delta = change.getLinesAdded() - end + start;
        boolean insertion = start == end;
        boolean atParagraphStart = change.getStart().offset() == 0;
        int expandedStart = change.getLinesAdded() > 0 && !atParagraphStart ? start : -1;

        if (isHeading(start) && change.getLinesAdded() > 0 && start == end &&
                change.getStart().offset() == getParagraphEnd(start).offset()) {
            String text = getPlainText(start + 1);
            if (text != null && text.isBlank()) {
                Platform.runLater(() ->
                        applyStyle(TextPos.ofLeading(start + 1, 0), TextPos.ofLeading(start + 1, 0), BODY_ATTRS));
            }
        }

        TreeSet<ParagraphRange> updatedSections = new TreeSet<>();
        for (ParagraphRange range : collapsedSections) {
            int index = range.start();
            if (index > end) {
                index += delta;
            } else if (index >= start) {
                if (insertion) {
                    index = atParagraphStart ? index + delta : index;
                } else if (index > start) {
                    continue;
                }
            }
            if (index == expandedStart) {
                continue;
            }
            ParagraphRange updatedRange = getSection(index);
            if (updatedRange != null) {
                updatedSections.add(updatedRange);
            }
        }

        collapsedSections.retainAll(updatedSections);
        collapsedSections.addAll(updatedSections);
        getHeadingsDecorator().refreshDecorator();
    }
}
