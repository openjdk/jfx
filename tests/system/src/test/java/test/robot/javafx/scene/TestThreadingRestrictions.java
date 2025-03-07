/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.javafx.scene;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.ColorPickerSkin;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import test.robot.testharness.RobotTestBase;
import test.util.Util;

/**
 * Most JavaFX objects, such as Node and Scene, can be constructed and modified on any thread
 * unless and until that object is attached to a visible Window.
 * Showing or hiding a Window is an operation that must always be done on the JavaFX application thread
 * as documented in the Window and Stage classes.
 *
 * This test ensures that the threading restrictions are in place where required.
 */
public class TestThreadingRestrictions extends RobotTestBase {
    @Test
    public void choiceBox() {
        test(ChoiceBox::new, ChoiceBox::show);
        test(ChoiceBox::new, ChoiceBox::hide);
        test(ChoiceBox::new, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void colorPicker() {
        test(ColorPicker::new, ColorPicker::show);
        test(ColorPicker::new, ColorPicker::hide);
        test(ColorPicker::new, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void colorPickerSkin() {
        Supplier<ColorPickerSkin> gen = () -> {
            return new ColorPickerSkin(new ColorPicker());
        };

        test(gen, ColorPickerSkin::show);
        test(gen, ColorPickerSkin::hide);
        test(gen, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void comboBox() {
        test(ComboBox::new, ComboBox::show);
        test(ComboBox::new, ComboBox::hide);
        test(ComboBox::new, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void comboBoxSkinListeners() {
        ComboBox<Object> c = new ComboBox<>();
        c.setSkin(new ComboBoxListViewSkin(c));
        Scene s = new Scene(c);
        c.getScene().setRoot(new Label());
    }

    @Test
    public void comboBoxSkin() {
        Supplier<ComboBoxListViewSkin> gen = () -> {
            return new ComboBoxListViewSkin(new ComboBox());
        };

        test(gen, ComboBoxListViewSkin::show);
        test(gen, ComboBoxListViewSkin::hide);
        test(gen, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void contextMenu() {
        Supplier<ContextMenu> gen = () -> {
            ContextMenu m = new ContextMenu();
            m.getItems().add(new MenuItem());
            return m;
        };

        test(gen, (p) -> p.show(stage));
        test(gen, (p) -> p.show(stage, 0, 0));
        test(gen, (p) -> p.show(contentPane, 0, 0));
        test(gen, (p) -> p.show(contentPane, Side.BOTTOM, 0, 0));
        test(gen, ContextMenu::hide);
        test(gen, (p) -> {
            Util.runAndWait(() -> {
                p.show(stage);
            });
            p.hide();
        });
    }

    @Test
    public void datePicker() {
        test(DatePicker::new, DatePicker::show);
        test(DatePicker::new, DatePicker::hide);
        test(DatePicker::new, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void datePickerSkin() {
        Supplier<DatePickerSkin> gen = () -> {
            return new DatePickerSkin(new DatePicker());
        };

        test(gen, DatePickerSkin::show);
        test(gen, DatePickerSkin::hide);
        test(gen, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void dialog() {
        test(inFxThread(Dialog::new), Dialog::close);
        test(inFxThread(Dialog::new), Dialog::show);
        test(inFxThread(Dialog::new), Dialog::hide);
        test(inFxThread(Dialog::new), (dialog) -> {
            Util.runAndWait(() -> {
                dialog.show();
            });
            dialog.hide();
        });
        test(inFxThread(Dialog::new), Dialog::showAndWait);
    }

    @Test
    public void menu() {
        Supplier<Menu> gen = () -> {
            Menu m = new Menu();
            m.getItems().add(new MenuItem());
            return m;
        };

        test(gen, Menu::show);
        test(gen, Menu::hide);
        test(gen, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void menuButton() {
        Supplier<MenuButton> gen = () -> {
            MenuButton m = new MenuButton("MenuButton");
            m.getItems().add(new MenuItem());
            return m;
        };

        test(gen, MenuButton::show);
        test(gen, MenuButton::hide);
        test(gen, (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
    }

    @Test
    public void popupWindow() {
        class TPopupWindow extends PopupWindow {
        }

        test(TPopupWindow::new, (p) -> p.hide());
        test(TPopupWindow::new, (p) -> p.show(stage));
        test(TPopupWindow::new, (p) -> p.show(stage, 0, 0));
        test(TPopupWindow::new, (p) -> p.show(contentPane, 0, 0));
        test(TPopupWindow::new, (p) -> {
            Util.runAndWait(() -> {
                p.show(stage);
            });
            p.hide();
        });
    }

    @Test
    public void stage() {
        test(inFxThread(Stage::new), (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.close();
        });
        test(inFxThread(Stage::new), Stage::show);
        test(inFxThread(Stage::new), Stage::hide);
        test(inFxThread(Stage::new), (p) -> {
            Util.runAndWait(() -> {
                p.show();
            });
            p.hide();
        });
        test(inFxThread(Stage::new), Stage::showAndWait);
    }

    @Test
    public void window() {
        class TWindow extends Window {
            public TWindow() {
            }

            @Override
            public void show() {
                super.show();
            }
        }

        test(inFxThread(TWindow::new), TWindow::show);
        test(inFxThread(TWindow::new), TWindow::hide);
    }

    private static <T> void test(Supplier<T> generator, Consumer<T> method) {
        T item = generator.get();
        assertThrows(IllegalStateException.class, () -> {
            method.accept(item);
        });
    }

    private static <T> Supplier<T> inFxThread(Supplier<T> generator) {
        return () -> {
            AtomicReference<T> ref = new AtomicReference<>();
            Util.runAndWait(() -> {
                T item = generator.get();
                ref.set(item);
            });
            return ref.get();
        };
    }
}
