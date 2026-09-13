/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableCell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.control.skin.LabeledSkinBase;
import javafx.scene.control.skin.LabeledSkinBaseShim;
import javafx.scene.control.skin.TableCellSkin;
import javafx.scene.control.skin.TreeTableCellSkin;
import javafx.scene.control.skin.MenuButtonSkin;
import javafx.scene.control.skin.SplitMenuButtonSkin;
import javafx.scene.layout.RegionShim;
import javafx.stage.Stage;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;

/**
 * Tests textTruncated property of Labeled, using Label, TableCell, and TreeTableCell controls
 * (the last two contain conditional code that redirects the execution of computePrefWidth())
 * in their skins to different code paths.
 */
public class LabeledTruncatedTest {
    private static final String TEXT = "testing.truncated";

    private void firePulse() {
        Toolkit.getToolkit().firePulse();
    }

    @Test
    public void testTruncatedLabel() {
        Supplier<Label> fun = () -> {
            Label control = new Label();
            control.setSkin(new LabelSkin(control));
            control.setText(TEXT);
            return control;
        };

        testWithSkin(fun.get());
        testTextProperty(fun.get());
    }

    @Test
    public void testTruncatedButton() {
        Supplier<Labeled> fun = () -> {
            Button control = new Button();
            control.setSkin(new ButtonSkin(control));
            control.setText(TEXT);
            return control;
        };
        testWithSkin(fun.get());
        testTextProperty(fun.get());
    }

    @Test
    public void testTruncatedMenuButton() {
        Supplier<Labeled> fun = () -> {
            MenuButton control = new MenuButton();
            control.setSkin(new MenuButtonSkin(control));
            control.setText(TEXT);
            return control;
        };
        testTextProperty(fun.get());
    }

    @Test
    public void testTruncatedSplitMenuButton() {
        Supplier<Labeled> fun = () -> {
            SplitMenuButton control = new SplitMenuButton();
            control.setSkin(new SplitMenuButtonSkin(control));
            control.setText(TEXT);
            return control;
        };
        testTextProperty(fun.get());
    }

    @Test
    public void testTruncatedTableCellSkin() {
        Supplier<Labeled> fun = () -> {
            TableCell<String, String> control = new TableCell<>();
            control.setSkin(new TableCellSkin<>(control));
            control.setText(TEXT);
            return control;
        };
        testWithSkin(fun.get());
        testTextProperty(fun.get());
    }

    @Test
    public void testTruncatedTreeTableCellSkin() {
        Supplier<Labeled> fun = () -> {
            TreeTableCell<String, String> control = new TreeTableCell<>();
            control.setSkin(new TreeTableCellSkin<>(control));
            control.setText(TEXT);
            return control;
        };
        testWithSkin(fun.get());
    }

    private void testWithSkin(Labeled control) {
        RegionShim.setWidth(control, 1000);
        LabeledSkinBaseShim.updateDisplayedText((LabeledSkinBase)control.getSkin());
        firePulse();
        double h = control.prefHeight(-1);

        assertFalse(control.isTextTruncated());

        RegionShim.setWidth(control, 10);
        RegionShim.setHeight(control, h);
        LabeledSkinBaseShim.updateDisplayedText((LabeledSkinBase)control.getSkin());
        firePulse();

        assertTrue(control.isTextTruncated());

        control.setWrapText(true);
        RegionShim.setWidth(control, 40);
        RegionShim.setHeight(control, 10000);
        LabeledSkinBaseShim.updateDisplayedText((LabeledSkinBase)control.getSkin());
        firePulse();

        assertFalse(control.isTextTruncated());

        RegionShim.setHeight(control, 2);
        LabeledSkinBaseShim.updateDisplayedText((LabeledSkinBase)control.getSkin());
        firePulse();

        assertTrue(control.isTextTruncated());
    }

    private void testTextProperty(Labeled control) {
        Scene scene = new Scene(control, 100, 100);
        Stage s = new Stage();
        s.setScene(scene);
        s.show();
        control.setText("A");
        firePulse();
        double h = control.prefHeight(-1);

        assertFalse(control.isTextTruncated());

        control.setText("very long text with a lots of characters");
        control.layout();
        firePulse();

        assertTrue(control.isTextTruncated());

        control.setText("A");
        control.layout();
        firePulse();
        assertFalse(control.isTextTruncated());
    }
}
