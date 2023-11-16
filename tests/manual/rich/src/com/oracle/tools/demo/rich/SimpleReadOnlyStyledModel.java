/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package com.oracle.tools.demo.rich;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledTextModelReadOnlyBase;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A simple, read-only, in-memory, styled text model.
 */
public class SimpleReadOnlyStyledModel extends StyledTextModelReadOnlyBase {
    private final ArrayList<RichParagraph> paragraphs = new ArrayList<>();

    public SimpleReadOnlyStyledModel() {
    }

    public static SimpleReadOnlyStyledModel from(String text) {
        SimpleReadOnlyStyledModel m = new SimpleReadOnlyStyledModel();
        BufferedReader rd = new BufferedReader(new StringReader(text));
        try {
            String s;
            while ((s = rd.readLine()) != null) {
                m.addSegment(s);
                m.nl();
            }
        } catch (Exception ignore) {
        } finally {
            try {
                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return m;
    }

    @Override
    public int size() {
        return paragraphs.size();
    }

    @Override
    public String getPlainText(int index) {
        return paragraphs.get(index).getPlainText();
    }

    @Override
    public RichParagraph getParagraph(int index) {
        return paragraphs.get(index);
    }

    public SimpleReadOnlyStyledModel addSegment(String text) {
        return addSegment(text, StyleAttrs.EMPTY);
    }

    public SimpleReadOnlyStyledModel addSegment(String text, String style, String... css) {
        RichParagraph p = lastParagraph();
        p.addSegment(text, style, css);
        return this;
    }

    public SimpleReadOnlyStyledModel addSegment(String text, StyleAttrs a) {
        Objects.requireNonNull(a);
        RichParagraph p = lastParagraph();
        p.addSegment(text, a);
        return this;
    }

    public SimpleReadOnlyStyledModel highlight(int start, int length, Color c) {
        RichParagraph p = lastParagraph();
        p.addHighlight(start, length, c);
        return this;
    }

    public SimpleReadOnlyStyledModel squiggly(int start, int length, Color c) {
        RichParagraph p = lastParagraph();
        p.addSquiggly(start, length, c);
        return this;
    }

    protected RichParagraph lastParagraph() {
        int sz = paragraphs.size();
        if (sz == 0) {
            RichParagraph p = new RichParagraph();
            paragraphs.add(p);
            return p;
        }
        return paragraphs.get(sz - 1);
    }

    /** adds a paragraph containing an image */
    public SimpleReadOnlyStyledModel addImage(InputStream in) {
        Image im = new Image(in);
        RichParagraph p = RichParagraph.of(() -> {
            return new ImageCellPane(im);
        });
        paragraphs.add(p);
        return this;
    }

    public SimpleReadOnlyStyledModel addParagraph(Supplier<Region> generator) {
        RichParagraph p = RichParagraph.of(() -> {
            return generator.get();
        });
        paragraphs.add(p);
        return this;
    }

    /** adds inline node segment */
    public SimpleReadOnlyStyledModel addNodeSegment(Supplier<Node> generator) {
        RichParagraph p = lastParagraph();
        p.addInlineNode(generator);
        return this;
    }

    public SimpleReadOnlyStyledModel nl() {
        return nl(1);
    }

    public SimpleReadOnlyStyledModel nl(int count) {
        for (int i = 0; i < count; i++) {
            int ix = paragraphs.size();
            paragraphs.add(new RichParagraph());
        }
        return this;
    }

    @Override
    public StyleAttrs getStyleAttrs(TextPos pos) {
        // TODO use segments
        return StyleAttrs.EMPTY;
    }

    public void setParagraphAttributes(StyleAttrs a) {
        RichParagraph p = lastParagraph();
        p.setParagraphAttributes(a);
    }
}
