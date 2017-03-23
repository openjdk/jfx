/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import javafx.beans.NamedArg;
import javafx.event.ActionEvent;
import javafx.scene.Node;

/**
 * This class is used when creating a Mnemonic.
 * The Mnemonic is constructed with a {@link javafx.scene.Node Node} and a {@link KeyCombination}.
 * When a Mnemonic is registered on a {@link javafx.scene.Scene Scene}, and the KeyCombination reaches the Scene unconsumed,
 * then the target Node will be sent an {@link javafx.event.ActionEvent ActionEvent}.
 * <p>
 * Controls should use their MnemonicParsing property when adding Mnemonics.
 * </p>
 * <p>
 * Mnemonics will not be displayed on all platforms, but the api
 * will still be present.
 * </p>
 * @since JavaFX 2.0
 */

public class Mnemonic {

    /**
     * Constructs a {@code Mnemonic} with the specified target {@link javafx.scene.Node Node}
     * and trigger {@link KeyCombination}.
     *
     * @param node the {@link javafx.scene.Node Node} that will receive the {@link javafx.event.ActionEvent ActionEvent}.
     * @param keyCombination the {@link KeyCombination} that will trigger the Mnemonic.
     */
    public Mnemonic(@NamedArg("node") Node node, @NamedArg("keyCombination") KeyCombination keyCombination) {
        this.node = node;
        this.keyCombination = keyCombination;
    }

    private KeyCombination keyCombination;
    /**
     * Returns the {@link KeyCombination}
     * @return the {@code KeyCombination}
     */
    public KeyCombination getKeyCombination() { return keyCombination; }

    /**
     * Sets the {@link KeyCombination}
     * @param keyCombination the {@code KeyCombination}
     */
    public void setKeyCombination(KeyCombination keyCombination) {
        this.keyCombination = keyCombination;
    }

    private Node node;

    /**
     * Returns the {@link javafx.scene.Node Node}
     * @return the {@code Node}
     */
    public Node getNode() { return node; }

    /**
     * Sets the {@link javafx.scene.Node Node}
     * @param node the {@code Node}
     * @since JavaFX 2.2
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * Fire the {@link javafx.event.ActionEvent ActionEvent}
     */
    public void fire() {
        if (node != null)
            node.fireEvent(new ActionEvent());
    }
}
