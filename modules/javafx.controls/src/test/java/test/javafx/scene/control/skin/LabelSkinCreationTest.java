/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.Collection;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBaseShim;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.control.skin.LabeledSkinBaseShim;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A parameterized test suite for testing that a LabelSkin,
 * created with various different configurations, is setup
 * correctly.
 */
public class LabelSkinCreationTest {

    private Label label;
    private LabelSkin skin;
    private Text text;

    private static Collection<Parameter> parameters() {
        Rectangle rect = new Rectangle();
        rect.setWidth(25);
        rect.setHeight(25);
        return List.of(
            // standard configuration
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.LEFT,   null),
            // specify only the fill
            new Parameter(Color.RED,   Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.LEFT,   null),
            // specify only the font
            new Parameter(Color.BLACK, Font.font("Arial", 64), TextAlignment.LEFT,    false, false, ContentDisplay.LEFT,   null),
            // specify only the align
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.JUSTIFY, false, false, ContentDisplay.LEFT,   null),
            // specify only the underline
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    true,  false, ContentDisplay.LEFT,   null),
            // specify only the wrapText
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, true,  ContentDisplay.LEFT,   null),
            // specify only the contentDisplay
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.BOTTOM, null),
            // specify only the graphic
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.LEFT,   rect),
            // specify every type of content display with null graphic
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.BOTTOM,       null),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.CENTER,       null),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.GRAPHIC_ONLY, null),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.LEFT,         null),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.RIGHT,        null),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.TEXT_ONLY,    null),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.TOP,          null),
            // specify every type of content display with non-null graphic
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.BOTTOM,       rect),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.CENTER,       rect),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.GRAPHIC_ONLY, rect),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.LEFT,         rect),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.RIGHT,        rect),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.TEXT_ONLY,    rect),
            new Parameter(Color.BLACK, Font.getDefault(),      TextAlignment.LEFT,    false, false, ContentDisplay.TOP,          rect)
        );
    }

    private record Parameter(
            Paint fill,
            Font font,
            TextAlignment align,
            boolean underline,
            boolean wrapText,
            ContentDisplay contentDisplay,
            Node graphic) { }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Parameter p) {
        label = new Label();
        label.setTextFill(p.fill);
        label.setFont(p.font);
        label.setTextAlignment(p.align);
        label.setUnderline(p.underline);
        label.setWrapText(p.wrapText);
        label.setContentDisplay(p.contentDisplay);
        label.setGraphic(p.graphic);
        label.setText("*");
        label.resize(30, 30);
        skin = new LabelSkin(label);
        label.setSkin(skin);
        text = LabeledSkinBaseShim.get_text(skin);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void labelWasInitializedCorrectly(Parameter p) {
        setup(p);
        assertSame(label.getTextFill(), text.getFill());
        assertSame(label.getFont(), text.getFont());
        assertSame(label.getTextAlignment(), text.getTextAlignment());
        assertEquals(label.isUnderline(), text.isUnderline());

        // The wrapping width is based on layout after updateDisplayedText() is called from layoutChildren().
        //assertTrue(label.isWrapText() ? text.getWrappingWidth() > 0 : text.getWrappingWidth() == 0);

        // Now test the children
        if (label.getContentDisplay() == ContentDisplay.GRAPHIC_ONLY) {
            // 1 child, graphic, if it is not null, otherwise 0
            if (label.getGraphic() == null) {
                assertEquals(0, SkinBaseShim.getChildren(skin).size());
            } else {
                assertEquals(1, SkinBaseShim.getChildren(skin).size());
                assertEquals(label.getGraphic(), SkinBaseShim.getChildren(skin).get(0));
            }
        } else if (label.getContentDisplay() == ContentDisplay.TEXT_ONLY) {
            // 1 child, text
            assertEquals(1, SkinBaseShim.getChildren(skin).size());
            assertEquals(text, SkinBaseShim.getChildren(skin).get(0));
        } else {
            if (label.getGraphic() == null) {
                // 1 child, text
                assertEquals(1, SkinBaseShim.getChildren(skin).size());
                assertEquals(text, SkinBaseShim.getChildren(skin).get(0));
            } else {
                // 2 children, graphic + text
                assertEquals(2, SkinBaseShim.getChildren(skin).size());
                assertEquals(label.getGraphic(), SkinBaseShim.getChildren(skin).get(0));
                assertEquals(text, SkinBaseShim.getChildren(skin).get(1));
            }
        }

        // TODO test that if there is a graphic, that the appropriate listeners are added
    }
}
