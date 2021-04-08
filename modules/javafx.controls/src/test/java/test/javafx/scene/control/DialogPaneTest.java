/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

import static org.junit.Assert.assertEquals;

public class DialogPaneTest {

    private StageLoader sl;
    private DialogPane dialogPane;

    @Before
    public void setup() {
        dialogPane = new DialogPane();
        sl = new StageLoader(dialogPane);
    }

    @After
    public void after() {
        sl.dispose();
    }

    @Test
    public void test_graphic_padding_noHeader() {
        // Since DialogPane is not set in a Dialog, PseudoClass is activated manually
        dialogPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("no-header"), true);

        final ImageView graphic = new ImageView(new Image(ContextMenuTest.class.getResource("icon.png").toExternalForm()));
        dialogPane.setGraphic(graphic);
        dialogPane.applyCss();

        final StackPane graphicContainer = (StackPane) graphic.getParent();
        final Insets padding = graphicContainer.getPadding();
        final double fontSize = Font.getDefault().getSize();

        // -fx-padding: 0.833em 0 0 0.833em;
        assertEquals(0.833 * fontSize, padding.getTop(), 0.01);
        assertEquals(0, padding.getRight(), 0.0);
        assertEquals(0, padding.getBottom(), 0.0);
        assertEquals(0.833 * fontSize, padding.getLeft(), 0.01);
    }

    @Test
    public void testLookupButtonIsReturningCorrectButton() {
        String id1 = "Test";

        dialogPane.getButtonTypes().setAll(ButtonType.OK);
        assertEquals(1, dialogPane.getButtonTypes().size());

        Node button = dialogPane.lookupButton(ButtonType.OK);
        button.setId(id1);

        verifyIdOfButtonInButtonBar(id1);

        String id2 = "Test2";

        dialogPane.getButtonTypes().setAll(ButtonType.OK);
        assertEquals(1, dialogPane.getButtonTypes().size());

        button = dialogPane.lookupButton(ButtonType.OK);
        button.setId(id2);

        verifyIdOfButtonInButtonBar(id2);
    }

    private void verifyIdOfButtonInButtonBar(String id) {
        for (Node children : dialogPane.getChildren()) {
            if (children instanceof ButtonBar) {
                ObservableList<Node> buttons = ((ButtonBar) children).getButtons();

                assertEquals(1, buttons.size());

                Node button = buttons.get(0);
                assertEquals(id, button.getId());
            }
        }
    }
}
