/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.css.PseudoClass;
import javafx.css.Selector;
import javafx.css.StyleConverter;
import javafx.geometry.NodeOrientation;

import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.geometry.NodeOrientation.RIGHT_TO_LEFT;

/**
 * Class which can read and write selectors in a binary format.
 */
public class BinarySerializer {
    private static final int TYPE_SIMPLE = 1;
    private static final int TYPE_COMPOUND = 2;

    public static Selector read(DataInputStream is, String[] strings) throws IOException {
        int type = is.readByte();

        if (type == TYPE_SIMPLE) {
            return readSimpleSelector(is, strings);
        }

        // Backwards compatible, if it is not TYPE_SIMPLE any other value is considered TYPE_COMPOUND
        return readCompoundSelector(is, strings);
    }

    public static void write(Selector selector, DataOutputStream os, StyleConverter.StringStore stringStore) throws IOException {
        if (selector instanceof SimpleSelector s) {
            writeSimpleSelector(s, os, stringStore);
        }
        else if(selector instanceof CompoundSelector s) {
            writeCompoundSelector(s, os, stringStore);
        }
        else {
            throw new IllegalStateException("support missing for selector type: " + (selector == null ? "null" : selector.getClass()));
        }
    }

    private static SimpleSelector readSimpleSelector(DataInputStream is, String[] strings) throws IOException {
        String name = strings[is.readShort()];
        int nStyleClasses = is.readShort();
        List<String> styleClasses = new ArrayList<>();

        for (int n = 0; n < nStyleClasses; n++) {
            styleClasses.add(strings[is.readShort()]);
        }

        String id = strings[is.readShort()];
        int nPseudoclasses = is.readShort();
        List<String> pseudoclasses = new ArrayList<>();

        for (int n = 0; n < nPseudoclasses; n++) {
            pseudoclasses.add(strings[is.readShort()]);
        }

        return new SimpleSelector(name, styleClasses, pseudoclasses, id);
    }

    private static CompoundSelector readCompoundSelector(DataInputStream is, String[] strings) throws IOException {
        int nSelectors = is.readShort();
        List<SimpleSelector> selectors = new ArrayList<>();

        for (int n = 0; n < nSelectors; n++) {

            /*
             * An extra byte is part of the binary format containing TYPE_SIMPLE that
             * isn't strictly needed. However, to remain backwards compatible with the
             * first (and only) supported binary format, this byte must be skipped over
             * before attempting to read what is known to be a simple selector.
             */

            byte type = is.readByte();

            if (type != TYPE_SIMPLE) {
                throw new IllegalStateException("Expected compound selector to consist of simple selectors only, but found type: " + type);
            }

            selectors.add(readSimpleSelector(is, strings));
        }

        int nRelationships = is.readShort();

        List<Combinator> relationships = new ArrayList<>();

        for (int n = 0; n < nRelationships; n++) {
            int ordinal = is.readByte();

            if (ordinal == Combinator.CHILD.ordinal()) {
                relationships.add(Combinator.CHILD);
            }
            else if (ordinal == Combinator.DESCENDANT.ordinal()) {
                relationships.add(Combinator.DESCENDANT);
            }
            else {
                assert false : "error deserializing CompoundSelector: Combinator = " + ordinal;
                relationships.add(Combinator.DESCENDANT);
            }
        }

        return new CompoundSelector(selectors, relationships);
    }

    private static void writeCompoundSelector(CompoundSelector selector, DataOutputStream os, StyleConverter.StringStore stringStore) throws IOException {
        os.writeByte(TYPE_COMPOUND);

        List<SimpleSelector> selectors = selector.getSelectors();

        os.writeShort(selectors.size());

        for (int n = 0; n < selectors.size(); n++) {
            writeSimpleSelector(selectors.get(n), os, stringStore);
        }

        List<Combinator> relationships = selector.getRelationships();

        os.writeShort(relationships.size());

        for (int n = 0; n < relationships.size(); n++) {
            os.writeByte(relationships.get(n).ordinal());
        }
    }

    private static void writeSimpleSelector(SimpleSelector selector, DataOutputStream os, StyleConverter.StringStore stringStore) throws IOException {
        os.writeByte(TYPE_SIMPLE);

        List<String> selectorStyleClassNames = selector.getStyleClasses();

        os.writeShort(stringStore.addString(selector.getName()));
        os.writeShort(selectorStyleClassNames.size());

        for (String sc : selectorStyleClassNames) {
            os.writeShort(stringStore.addString(sc));
        }

        os.writeShort(stringStore.addString(selector.getId()));

        Set<PseudoClass> pseudoClassStates = selector.getPseudoClassStates();
        NodeOrientation nodeOrientation = selector.getNodeOrientation();

        int pclassSize = pseudoClassStates.size()
                + (nodeOrientation == RIGHT_TO_LEFT || nodeOrientation == LEFT_TO_RIGHT ? 1 : 0);

        os.writeShort(pclassSize);

        for (PseudoClass pc : pseudoClassStates) {
            os.writeShort(stringStore.addString(pc.getPseudoClassName()));
        }

        if (nodeOrientation == RIGHT_TO_LEFT) {
            os.writeShort(stringStore.addString("dir(rtl)"));
        }
        else if (nodeOrientation == LEFT_TO_RIGHT) {
            os.writeShort(stringStore.addString("dir(ltr)"));
        }
    }
}
